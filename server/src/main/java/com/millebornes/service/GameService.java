package com.millebornes.service;

import com.millebornes.dto.GameStateDTO;
import com.millebornes.game.*;
import com.millebornes.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;

    public GameService(SimpMessagingTemplate messagingTemplate, LobbyService lobbyService) {
        this.messagingTemplate = messagingTemplate;
        this.lobbyService = lobbyService;
    }

    public GameState startGame(String roomCode, boolean randomizeStart) {
        GameState state = startGame(roomCode);
        if (randomizeStart && state.getPlayers().size() > 1) {
            int idx = new Random().nextInt(state.getPlayers().size());
            state.setCurrentPlayerIndex(idx);
            broadcastState(state);
        }
        return state;
    }

    public GameState startGame(String roomCode) {
        GameRoom room = lobbyService.getRoom(roomCode);
        if (room == null) throw new IllegalArgumentException("Room not found");
        if (room.getPlayers().size() < 2) throw new IllegalStateException("Need at least 2 players");

        int playerCount = room.getPlayers().size();
        if (playerCount == 5) throw new IllegalStateException("Cannot start with 5 players — one must leave or one more must join");

        // Auto-pair remaining players for 4p mode
        if (playerCount == 4) {
            // Count manual teams
            Set<Integer> existingTeams = new HashSet<>();
            List<Player> unpaired = new ArrayList<>();
            for (Player p : room.getPlayers()) {
                if (p.getTeamIndex() != -1) {
                    existingTeams.add(p.getTeamIndex());
                } else {
                    unpaired.add(p);
                }
            }
            if (existingTeams.size() == 1 && unpaired.size() == 2) {
                // Auto-pair remaining 2
                int teamIdx = 0;
                while (existingTeams.contains(teamIdx)) teamIdx++;
                for (Player p : unpaired) {
                    p.setTeamIndex(teamIdx);
                    p.setAutoPartnered(true);
                }
            }
            // Check if all are now paired
            boolean allPaired = true;
            for (Player p : room.getPlayers()) {
                if (p.getTeamIndex() == -1) { allPaired = false; break; }
            }
            if (allPaired) {
                room.setUseTeams(true);
                buildTeamsFromAssignments(room);
            }
        }

        // Validate 6p: all must be partnered
        if (playerCount == 6) {
            for (Player p : room.getPlayers()) {
                if (p.getTeamIndex() == -1) {
                    throw new IllegalStateException("All players must have partners in 6-player mode");
                }
            }
            room.setUseTeams(true);
            buildTeamsFromAssignments(room);
        }

        GameState state = new GameState(roomCode);
        state.setPlayers(new ArrayList<>(room.getPlayers()));
        state.setUseTeams(room.isUseTeams());
        state.setTeams(new ArrayList<>(room.getTeams()));

        // Initialize cumulative scores
        for (Player p : state.getPlayers()) {
            state.getCumulativeScores().put(p.getId(), 0);
        }

        dealNewHand(state);

        if (state.isUseTeams()) {
            interleavePlayerOrder(state);
            shareTeamPiles(state);
        }

        games.put(roomCode, state);

        log.info("Game started in room {} with {} players", roomCode, state.getPlayers().size());
        for (Player p : state.getPlayers()) {
            log.info("  Player {} ({}) has {} cards", p.getName(), p.getId(), p.getHand().size());
        }

        broadcastState(state);
        sendAllHands(state);
        return state;
    }

    /**
     * Reorder players so turns alternate between teams.
     * For teams [A,B] and [C,D]: order becomes [A, C, B, D].
     * Round 0 → first member of each team, round 1 → second member, etc.
     */
    private void interleavePlayerOrder(GameState state) {
        // Group players by team index, preserving order within each team
        Map<Integer, List<Player>> teamPlayers = new LinkedHashMap<>();
        for (Team team : state.getTeams()) {
            teamPlayers.put(team.getIndex(), new ArrayList<>());
        }
        for (Player p : state.getPlayers()) {
            if (p.getTeamIndex() != -1 && teamPlayers.containsKey(p.getTeamIndex())) {
                teamPlayers.get(p.getTeamIndex()).add(p);
            }
        }

        List<Player> interleaved = new ArrayList<>();
        int maxSize = 0;
        for (List<Player> members : teamPlayers.values()) {
            maxSize = Math.max(maxSize, members.size());
        }
        for (int round = 0; round < maxSize; round++) {
            for (List<Player> members : teamPlayers.values()) {
                if (round < members.size()) {
                    interleaved.add(members.get(round));
                }
            }
        }

        state.setPlayers(interleaved);
        state.setCurrentPlayerIndex(0);
    }

    /**
     * Share pile references between teammates.
     * After calling this, teammates share the same battlePile, speedPile,
     * safetyArea, and distancePile lists. Each player keeps their own hand.
     */
    private void shareTeamPiles(GameState state) {
        // Group players by team
        Map<Integer, List<Player>> teamPlayers = new HashMap<>();
        for (Player p : state.getPlayers()) {
            if (p.getTeamIndex() != -1) {
                teamPlayers.computeIfAbsent(p.getTeamIndex(), k -> new ArrayList<>()).add(p);
            }
        }

        for (List<Player> members : teamPlayers.values()) {
            if (members.size() < 2) continue;
            Player rep = members.get(0);
            for (int i = 1; i < members.size(); i++) {
                Player mate = members.get(i);
                mate.setBattlePile(rep.getBattlePile());
                mate.setSpeedPile(rep.getSpeedPile());
                mate.setSafetyArea(rep.getSafetyArea());
                mate.setDistancePile(rep.getDistancePile());
            }
        }
    }

    private void buildTeamsFromAssignments(GameRoom room) {
        // Build Team objects from player teamIndex assignments
        Map<Integer, List<String>> teamMap = new HashMap<>();
        for (Player p : room.getPlayers()) {
            if (p.getTeamIndex() != -1) {
                teamMap.computeIfAbsent(p.getTeamIndex(), k -> new ArrayList<>()).add(p.getId());
            }
        }
        List<Team> teams = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : teamMap.entrySet()) {
            Team team = new Team(entry.getKey(), "Team " + (entry.getKey() + 1));
            team.setPlayerIds(entry.getValue());
            teams.add(team);
        }
        teams.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        room.setTeams(teams);
    }

    public void dealNewHand(GameState state) {
        for (Player p : state.getPlayers()) {
            p.resetForNewHand();
        }

        state.setDeck(Deck.createShuffledDeck());
        state.getDiscardPile().clear();
        state.setPhase(GamePhase.PLAYING);
        state.setTurnPhase(TurnPhase.DRAW);
        state.setCurrentPlayerIndex(0);
        state.setExtensionDeclared(false);
        state.setExtensionPromptPlayerId(null);
        state.setMileTarget(700);

        // Deal 6 cards to each player
        for (int i = 0; i < 6; i++) {
            for (Player p : state.getPlayers()) {
                Card card = Deck.draw(state.getDeck());
                if (card != null) {
                    p.getHand().add(card);
                }
            }
        }
    }

    public GameState getGame(String roomCode) {
        return games.get(roomCode);
    }

    // === Draw ===
    public void handleDraw(String roomCode, String playerId) {
        GameState state = games.get(roomCode);
        if (state == null) { log.warn("Draw: no game for room {}", roomCode); return; }

        Player player = state.getPlayerById(playerId);
        if (player == null) { log.warn("Draw: no player {} in room {}", playerId, roomCode); return; }

        if (!state.getCurrentPlayer().getId().equals(playerId)) {
            sendError(state, playerId, "It's not your turn");
            return;
        }
        if (state.getTurnPhase() != TurnPhase.DRAW) {
            sendError(state, playerId, "Not in draw phase");
            return;
        }

        // Drawing clears any pending coup fourré opportunity
        clearCoupFourreMetadata(state);

        // Clear last-action so stale animation data isn't re-broadcast
        state.clearLastAction();

        if (state.isDeckEmpty()) {
            TurnManager.moveToPlayPhase(state);
            broadcastState(state);
            sendHand(state, player);
            return;
        }

        Card drawn = Deck.draw(state.getDeck());
        if (drawn != null) {
            player.getHand().add(drawn);
        }

        log.info("Player {} drew a card, now has {} cards", player.getName(), player.getHand().size());

        TurnManager.moveToPlayPhase(state);
        broadcastState(state);
        sendHand(state, player);
    }

    // === Play Card ===
    public void handlePlay(String roomCode, String playerId, int cardIndex, String targetPlayerId) {
        GameState state = games.get(roomCode);
        if (state == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null) return;

        if (cardIndex < 0 || cardIndex >= player.getHand().size()) {
            sendError(state, playerId, "Invalid card index");
            return;
        }

        Card card = player.getHand().get(cardIndex);
        Player target = targetPlayerId != null ? state.getPlayerById(targetPlayerId) : null;

        String error = GameEngine.validatePlay(state, player, card, target);
        if (error != null) {
            sendError(state, playerId, error);
            return;
        }

        // Capture card before applyPlay removes it from hand
        Card playedCard = card;

        GameEngine.applyPlay(state, player, card, target);

        // Set last-action for animation
        state.setLastPlayedCard(playedCard);
        state.setLastPlayedByPlayerId(playerId);
        state.setLastPlayedTargetPlayerId(targetPlayerId);
        state.setLastActionType("PLAY");

        // Safety gives extra turn (draw + play) — no replacement draw;
        // the extra turn's draw is the compensation for the played card.
        if (card.getCardType() == CardType.SAFETY) {
            state.setTurnPhase(state.isDeckEmpty() ? TurnPhase.PLAY : TurnPhase.DRAW);
            broadcastState(state);
            sendAllHands(state);
            checkHandComplete(state);
            return;
        }

        // Check for coup fourré opportunity (any player except the source)
        if (card.getCardType() == CardType.HAZARD && target != null) {
            boolean anyoneCanCF = false;
            for (Player p : state.getPlayers()) {
                if (p.getId().equals(player.getId())) continue; // hazard player can't CF
                if (GameEngine.getCoupFourreCard(p, card.getHazardType()) != null) {
                    anyoneCanCF = true;
                    break;
                }
            }
            if (anyoneCanCF) {
                // Set CF metadata but DON'T block the turn
                state.setCoupFourreTargetPlayerId(target.getId());
                state.setCoupFourreHazardType(card.getHazardType());
                state.setCoupFourreSourcePlayerId(player.getId());
                log.info("Coup fourré opportunity: hazard {} on {}, someone has the safety",
                        card.getHazardType(), target.getName());
            }
        }

        if (GameEngine.isHandComplete(state)) {
            maybePromptExtensionOrEnd(state);
        } else {
            TurnManager.advanceTurn(state);
            checkDeckExhaustion(state);
            broadcastState(state);
            sendAllHands(state);
        }
    }

    // === Discard ===
    public void handleDiscard(String roomCode, String playerId, int cardIndex) {
        GameState state = games.get(roomCode);
        if (state == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null) return;

        if (!state.getCurrentPlayer().getId().equals(playerId)) {
            sendError(state, playerId, "It's not your turn");
            return;
        }
        if (state.getTurnPhase() != TurnPhase.PLAY) {
            sendError(state, playerId, "Not in play phase");
            return;
        }
        if (cardIndex < 0 || cardIndex >= player.getHand().size()) {
            sendError(state, playerId, "Invalid card index");
            return;
        }

        // Capture card before applyDiscard removes it from hand
        Card discardedCard = player.getHand().get(cardIndex);

        GameEngine.applyDiscard(state, player, cardIndex);

        // Set last-action for animation
        state.setLastPlayedCard(discardedCard);
        state.setLastPlayedByPlayerId(playerId);
        state.setLastPlayedTargetPlayerId(null);
        state.setLastActionType("DISCARD");

        if (GameEngine.isHandComplete(state)) {
            maybePromptExtensionOrEnd(state);
        } else {
            TurnManager.advanceTurn(state);
            checkDeckExhaustion(state);
            broadcastState(state);
            sendAllHands(state);
        }
    }

    // === Coup Fourré ===
    public void handleCoupFourre(String roomCode, String playerId, int cardIndex) {
        GameState state = games.get(roomCode);
        if (state == null) return;

        // CF metadata must still be active
        if (state.getCoupFourreHazardType() == null) {
            sendError(state, playerId, "No coup fourré opportunity active");
            return;
        }

        // The hazard source player cannot declare CF
        if (playerId.equals(state.getCoupFourreSourcePlayerId())) {
            sendError(state, playerId, "You played the hazard — you can't declare coup fourré");
            return;
        }

        Player declarer = state.getPlayerById(playerId);
        if (declarer == null) return;

        if (cardIndex < 0 || cardIndex >= declarer.getHand().size()) {
            sendError(state, playerId, "Invalid card index");
            return;
        }

        Card card = declarer.getHand().get(cardIndex);
        if (card.getCardType() != CardType.SAFETY) {
            sendError(state, playerId, "That's not a safety card");
            return;
        }

        Card expectedSafety = GameEngine.getSafetyForHazard(state.getCoupFourreHazardType());
        if (card != expectedSafety) {
            sendError(state, playerId, "Wrong safety card for this hazard");
            return;
        }

        Player victim = state.getPlayerById(state.getCoupFourreTargetPlayerId());
        log.info("Coup fourré declared by {} (victim was {})", declarer.getName(),
                victim != null ? victim.getName() : "unknown");

        GameEngine.applyCoupFourre(state, declarer, victim, card);

        // Replacement draw: declarer played a safety out of turn, so draw
        // one to restore their hand size before their bonus turn.
        if (!state.isDeckEmpty()) {
            Card drawn = Deck.draw(state.getDeck());
            if (drawn != null) {
                declarer.getHand().add(drawn);
            }
        }

        clearCoupFourreMetadata(state);

        // CF declarer gets a full bonus turn (draw + play)
        TurnManager.setTurnToPlayer(state, declarer.getId());

        broadcastState(state);
        sendAllHands(state);
    }

    public void handleCoupFourrePass(String roomCode, String playerId) {
        // Pass is handled entirely client-side (local dismiss).
        // This handler is a no-op to avoid race conditions where a late
        // pass message clears metadata for a newer CF opportunity.
    }

    // === Extension ===
    public void handleExtension(String roomCode, String playerId, boolean declare) {
        GameState state = games.get(roomCode);
        if (state == null) return;
        if (state.getPhase() != GamePhase.EXTENSION_PROMPT) return;

        Player player = state.getPlayerById(playerId);
        if (player == null) return;

        if (!playerId.equals(state.getExtensionPromptPlayerId())) {
            sendError(state, playerId, "It's not your extension decision");
            return;
        }

        state.setExtensionPromptPlayerId(null);

        if (declare) {
            state.setExtensionDeclared(true);
            state.setMileTarget(1000);
            state.setPhase(GamePhase.PLAYING);
            TurnManager.advanceTurn(state);
            broadcastState(state);
            sendAllHands(state);
        } else {
            endHand(state);
        }
    }

    // === Hand/Game End ===
    private void endHand(GameState state) {
        state.setPhase(GamePhase.HAND_OVER);

        List<ScoreSheet> scores = ScoreCalculator.calculateAllScores(state);

        if (state.isUseTeams() && !state.getTeams().isEmpty()) {
            // Team scoring: store cumulative by rep player ID,
            // and set both teammates to the same cumulative score
            for (ScoreSheet sheet : scores) {
                int cumulative = state.getCumulativeScores().getOrDefault(sheet.getPlayerId(), 0);
                cumulative += sheet.getTotalHandScore();
                state.getCumulativeScores().put(sheet.getPlayerId(), cumulative);

                // Find the team and set same cumulative for all members
                for (Team team : state.getTeams()) {
                    if (team.getPlayerIds().contains(sheet.getPlayerId())) {
                        for (String memberId : team.getPlayerIds()) {
                            state.getCumulativeScores().put(memberId, cumulative);
                        }
                        break;
                    }
                }
            }
        } else {
            for (ScoreSheet sheet : scores) {
                int cumulative = state.getCumulativeScores().getOrDefault(sheet.getPlayerId(), 0);
                cumulative += sheet.getTotalHandScore();
                state.getCumulativeScores().put(sheet.getPlayerId(), cumulative);
            }
        }

        boolean anyOver5000 = false;
        for (int score : state.getCumulativeScores().values()) {
            if (score >= 5000) {
                anyOver5000 = true;
                break;
            }
        }

        boolean gameOver = false;
        if (anyOver5000) {
            // Find the highest cumulative score
            int highest = 0;
            String winnerId = null;
            for (Map.Entry<String, Integer> entry : state.getCumulativeScores().entrySet()) {
                if (entry.getValue() > highest) {
                    highest = entry.getValue();
                    winnerId = entry.getKey();
                }
            }
            state.setWinnerId(winnerId);
            state.setPhase(GamePhase.GAME_OVER);
            gameOver = true;
        }

        Map<String, Object> scoreMessage = new HashMap<>();
        scoreMessage.put("scores", scores);
        scoreMessage.put("cumulativeScores", state.getCumulativeScores());
        scoreMessage.put("handComplete", true);
        scoreMessage.put("gameComplete", gameOver);
        if (gameOver) {
            scoreMessage.put("winnerId", state.getWinnerId());
        }
        messagingTemplate.convertAndSend("/topic/game/" + state.getRoomCode() + "/score", scoreMessage);

        broadcastState(state);
    }

    public void resetGame(String roomCode) {
        games.remove(roomCode);
        log.info("Game reset in room {}", roomCode);

        // Broadcast updated lobby (gameStarted=false) so clients show waiting screen
        GameRoom room = lobbyService.getRoom(roomCode);
        if (room != null) {
            messagingTemplate.convertAndSend("/topic/lobby/" + roomCode,
                    lobbyService.toLobbyDTO(room, false));
        }

        // Notify all clients to clear their game state
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/reset",
                java.util.Collections.singletonMap("reset", true));
    }

    public void startNewHand(String roomCode) {
        GameState state = games.get(roomCode);
        if (state == null) return;
        if (state.getPhase() != GamePhase.HAND_OVER) return;

        state.setHandNumber(state.getHandNumber() + 1);
        dealNewHand(state);

        if (state.isUseTeams()) {
            shareTeamPiles(state);
        }

        broadcastState(state);
        sendAllHands(state);
    }

    private void checkHandComplete(GameState state) {
        if (GameEngine.isHandComplete(state)) {
            maybePromptExtensionOrEnd(state);
        }
    }

    private void maybePromptExtensionOrEnd(GameState state) {
        // If extension not yet declared and target is still 700, prompt the player who reached it
        if (!state.isExtensionDeclared() && state.getMileTarget() == 700) {
            for (Player p : state.getPlayers()) {
                if (p.getTotalMiles() >= 700) {
                    state.setPhase(GamePhase.EXTENSION_PROMPT);
                    state.setExtensionPromptPlayerId(p.getId());
                    log.info("Extension prompt for {} (reached {} miles)", p.getName(), p.getTotalMiles());
                    broadcastState(state);
                    sendAllHands(state);
                    return;
                }
            }
        }
        endHand(state);
    }

    private void checkDeckExhaustion(GameState state) {
        if (state.isDeckEmpty()) {
            boolean anyCanPlay = false;
            for (Player p : state.getPlayers()) {
                if (!p.getHand().isEmpty()) {
                    anyCanPlay = true;
                    break;
                }
            }
            if (!anyCanPlay) {
                endHand(state);
            }
        }
    }

    // === Broadcasting ===
    private void broadcastState(GameState state) {
        GameStateDTO dto = GameStateDTO.fromGameState(state);
        messagingTemplate.convertAndSend("/topic/game/" + state.getRoomCode(), dto);
    }

    private void sendHand(GameState state, Player player) {
        Map<String, Object> handMsg = new HashMap<>();
        handMsg.put("hand", player.getHand());
        handMsg.put("playerId", player.getId());
        String dest = "/topic/game/" + state.getRoomCode() + "/hand/" + player.getId();
        log.info("Sending hand to {} at {}, {} cards", player.getName(), dest, player.getHand().size());
        messagingTemplate.convertAndSend(dest, handMsg);
    }

    private void sendAllHands(GameState state) {
        for (Player p : state.getPlayers()) {
            sendHand(state, p);
        }
    }

    private void clearCoupFourreMetadata(GameState state) {
        state.setCoupFourreTargetPlayerId(null);
        state.setCoupFourreHazardType(null);
        state.setCoupFourreDeadline(0);
        state.setCoupFourreSourcePlayerId(null);
    }

    private void sendError(GameState state, String playerId, String message) {
        Map<String, String> errorMsg = new HashMap<>();
        errorMsg.put("message", message);
        messagingTemplate.convertAndSend(
                "/topic/game/" + state.getRoomCode() + "/error/" + playerId, errorMsg);
    }
}
