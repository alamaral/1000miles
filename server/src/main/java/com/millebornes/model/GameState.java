package com.millebornes.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {
    private String roomCode;
    private GamePhase phase = GamePhase.WAITING;
    private List<Card> deck = new ArrayList<>();
    private List<Card> discardPile = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private List<Team> teams = new ArrayList<>();
    private boolean useTeams = false;
    private int currentPlayerIndex = 0;
    private TurnPhase turnPhase = TurnPhase.DRAW;
    private int handNumber = 1;
    private Map<String, Integer> cumulativeScores = new HashMap<>();
    private boolean extensionDeclared = false;
    private int mileTarget = 700;
    private String extensionPromptPlayerId;

    // Coup fourré tracking
    private String coupFourreTargetPlayerId;
    private HazardType coupFourreHazardType;
    private long coupFourreDeadline;
    private String coupFourreSourcePlayerId;

    // Last-action tracking (for card play animation)
    private Card lastPlayedCard;
    private String lastPlayedByPlayerId;
    private String lastPlayedTargetPlayerId;
    private String lastActionType; // "PLAY" or "DISCARD"

    public GameState() {}

    public GameState(String roomCode) {
        this.roomCode = roomCode;
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public Player getPlayerById(String playerId) {
        for (Player p : players) {
            if (p.getId().equals(playerId)) return p;
        }
        return null;
    }

    public Player getPlayerBySessionId(String sessionId) {
        for (Player p : players) {
            if (p.getSessionId().equals(sessionId)) return p;
        }
        return null;
    }

    public int getPlayerIndex(String playerId) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(playerId)) return i;
        }
        return -1;
    }

    public boolean isDeckEmpty() {
        return deck.isEmpty();
    }

    // Getters and setters
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public List<Card> getDeck() { return deck; }
    public void setDeck(List<Card> deck) { this.deck = deck; }
    public List<Card> getDiscardPile() { return discardPile; }
    public void setDiscardPile(List<Card> discardPile) { this.discardPile = discardPile; }
    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
    public List<Team> getTeams() { return teams; }
    public void setTeams(List<Team> teams) { this.teams = teams; }
    public boolean isUseTeams() { return useTeams; }
    public void setUseTeams(boolean useTeams) { this.useTeams = useTeams; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
    public TurnPhase getTurnPhase() { return turnPhase; }
    public void setTurnPhase(TurnPhase turnPhase) { this.turnPhase = turnPhase; }
    public int getHandNumber() { return handNumber; }
    public void setHandNumber(int handNumber) { this.handNumber = handNumber; }
    public Map<String, Integer> getCumulativeScores() { return cumulativeScores; }
    public void setCumulativeScores(Map<String, Integer> cumulativeScores) { this.cumulativeScores = cumulativeScores; }
    public boolean isExtensionDeclared() { return extensionDeclared; }
    public void setExtensionDeclared(boolean extensionDeclared) { this.extensionDeclared = extensionDeclared; }
    public int getMileTarget() { return mileTarget; }
    public void setMileTarget(int mileTarget) { this.mileTarget = mileTarget; }
    public String getExtensionPromptPlayerId() { return extensionPromptPlayerId; }
    public void setExtensionPromptPlayerId(String extensionPromptPlayerId) { this.extensionPromptPlayerId = extensionPromptPlayerId; }
    public String getCoupFourreTargetPlayerId() { return coupFourreTargetPlayerId; }
    public void setCoupFourreTargetPlayerId(String coupFourreTargetPlayerId) { this.coupFourreTargetPlayerId = coupFourreTargetPlayerId; }
    public HazardType getCoupFourreHazardType() { return coupFourreHazardType; }
    public void setCoupFourreHazardType(HazardType coupFourreHazardType) { this.coupFourreHazardType = coupFourreHazardType; }
    public long getCoupFourreDeadline() { return coupFourreDeadline; }
    public void setCoupFourreDeadline(long coupFourreDeadline) { this.coupFourreDeadline = coupFourreDeadline; }
    public String getCoupFourreSourcePlayerId() { return coupFourreSourcePlayerId; }
    public void setCoupFourreSourcePlayerId(String coupFourreSourcePlayerId) { this.coupFourreSourcePlayerId = coupFourreSourcePlayerId; }

    public Card getLastPlayedCard() { return lastPlayedCard; }
    public void setLastPlayedCard(Card lastPlayedCard) { this.lastPlayedCard = lastPlayedCard; }
    public String getLastPlayedByPlayerId() { return lastPlayedByPlayerId; }
    public void setLastPlayedByPlayerId(String lastPlayedByPlayerId) { this.lastPlayedByPlayerId = lastPlayedByPlayerId; }
    public String getLastPlayedTargetPlayerId() { return lastPlayedTargetPlayerId; }
    public void setLastPlayedTargetPlayerId(String lastPlayedTargetPlayerId) { this.lastPlayedTargetPlayerId = lastPlayedTargetPlayerId; }
    public String getLastActionType() { return lastActionType; }
    public void setLastActionType(String lastActionType) { this.lastActionType = lastActionType; }

    public void clearLastAction() {
        this.lastPlayedCard = null;
        this.lastPlayedByPlayerId = null;
        this.lastPlayedTargetPlayerId = null;
        this.lastActionType = null;
    }
}
