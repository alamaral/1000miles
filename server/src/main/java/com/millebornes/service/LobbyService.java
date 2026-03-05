package com.millebornes.service;

import com.millebornes.dto.LobbyDTO;
import com.millebornes.model.GameRoom;
import com.millebornes.model.Player;
import com.millebornes.model.Team;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public GameRoom createRoom(String playerName, String sessionId) {
        String roomCode = generateRoomCode();
        String playerId = UUID.randomUUID().toString();

        Player host = new Player(playerId, playerName, sessionId);
        GameRoom room = new GameRoom(roomCode, playerId);
        room.getPlayers().add(host);

        rooms.put(roomCode, room);
        return room;
    }

    public GameRoom joinRoom(String roomCode, String playerName, String sessionId) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomCode);
        }
        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new IllegalStateException("Room is full");
        }

        // Check if player is reconnecting
        for (Player p : room.getPlayers()) {
            if (p.getName().equals(playerName)) {
                p.setSessionId(sessionId);
                return room;
            }
        }

        String playerId = UUID.randomUUID().toString();
        Player player = new Player(playerId, playerName, sessionId);
        room.getPlayers().add(player);

        // Clear team assignments if player count is no longer 4 or 6
        int count = room.getPlayers().size();
        if (count != 4 && count != 6) {
            clearAllTeamAssignments(room);
        }

        return room;
    }

    public GameRoom getRoom(String roomCode) {
        return rooms.get(roomCode);
    }

    public Collection<GameRoom> getAllRooms() {
        return rooms.values();
    }

    public List<LobbyDTO> listRooms() {
        List<LobbyDTO> result = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            result.add(toLobbyDTO(room, false));
        }
        return result;
    }

    public LobbyDTO leaveRoom(String roomCode, String playerId) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) throw new IllegalArgumentException("Room not found");
        if (playerId.equals(room.getHostPlayerId())) {
            throw new IllegalStateException("Host cannot leave the room");
        }

        Player departing = room.getPlayerById(playerId);
        if (departing == null) throw new IllegalArgumentException("Player not found in room");

        int departingTeam = departing.getTeamIndex();
        room.getPlayers().remove(departing);

        // If departing player was on a team, unpair that team and clear auto-partners
        if (departingTeam != -1) {
            unpairTeam(room, departingTeam);
            room.getTeams().removeIf(t -> t.getIndex() == departingTeam);
            clearAutoPartners(room);
        }

        // Clear all team assignments if player count is no longer 4 or 6
        int count = room.getPlayers().size();
        if (count != 4 && count != 6) {
            clearAllTeamAssignments(room);
        }

        return toLobbyDTO(room, false);
    }

    public void removeRoom(String roomCode) {
        rooms.remove(roomCode);
    }

    public void configureTeams(String roomCode, boolean useTeams) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) throw new IllegalArgumentException("Room not found");
        room.setUseTeams(useTeams);

        if (useTeams) {
            room.setMaxPlayers(6);
            if (room.getTeams().isEmpty()) {
                room.getTeams().add(new Team(0, "Team 1"));
                room.getTeams().add(new Team(1, "Team 2"));
            }
        } else {
            room.setMaxPlayers(4);
            room.getTeams().clear();
        }
    }

    public LobbyDTO toLobbyDTO(GameRoom room, boolean gameStarted) {
        LobbyDTO dto = new LobbyDTO();
        dto.setRoomCode(room.getRoomCode());
        dto.setMaxPlayers(room.getMaxPlayers());
        dto.setUseTeams(room.isUseTeams());
        dto.setGameStarted(gameStarted);

        Player host = room.getPlayerById(room.getHostPlayerId());
        dto.setHostPlayerName(host != null ? host.getName() : "Unknown");

        List<LobbyDTO.PlayerInfo> playerInfos = new ArrayList<>();
        for (Player p : room.getPlayers()) {
            playerInfos.add(new LobbyDTO.PlayerInfo(p.getId(), p.getName(), p.getTeamIndex(), p.isAutoPartnered()));
        }
        dto.setPlayers(playerInfos);
        dto.setSpectators(new ArrayList<>(room.getSpectators().values()));

        // Populate team names map
        Map<Integer, String> teamNames = new HashMap<>();
        for (Team t : room.getTeams()) {
            teamNames.put(t.getIndex(), t.getName());
        }
        dto.setTeamNames(teamNames);

        return dto;
    }

    public LobbyDTO choosePartner(String roomCode, String requestingPlayerId, String targetPlayerId, String teamName) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) throw new IllegalArgumentException("Room not found");

        int playerCount = room.getPlayers().size();
        if (playerCount != 4 && playerCount != 6) {
            throw new IllegalStateException("Partner selection requires 4 or 6 players");
        }

        if (requestingPlayerId.equals(targetPlayerId)) {
            throw new IllegalArgumentException("Cannot partner with yourself");
        }

        Player requester = room.getPlayerById(requestingPlayerId);
        Player target = room.getPlayerById(targetPlayerId);
        if (requester == null || target == null) {
            throw new IllegalArgumentException("Player not found");
        }

        if (target.isAutoPartnered()) {
            throw new IllegalStateException("Cannot modify an auto-assigned partnership");
        }

        // If already partners → unpair
        if (requester.getTeamIndex() != -1 && requester.getTeamIndex() == target.getTeamIndex()) {
            int teamIdx = requester.getTeamIndex();
            // Remove the team object
            room.getTeams().removeIf(t -> t.getIndex() == teamIdx);
            unpairTeam(room, teamIdx);
            // In 6p mode, also clear any auto-partners since team structure changed
            if (playerCount == 6) {
                clearAutoPartners(room);
                tryAutoCompleteLastTeam(room, playerCount);
            }
            return toLobbyDTO(room, false);
        }

        // If either already has a different partner → error
        if (requester.getTeamIndex() != -1) {
            throw new IllegalStateException("You already have a partner. Unpair first.");
        }
        if (target.getTeamIndex() != -1) {
            throw new IllegalStateException("That player already has a partner.");
        }

        // Pair them
        int teamIdx = nextAvailableTeamIndex(room);
        requester.setTeamIndex(teamIdx);
        target.setTeamIndex(teamIdx);

        // Create a Team object with the given name
        String resolvedName = (teamName != null && !teamName.trim().isEmpty())
                ? teamName.trim() : "Team " + (teamIdx + 1);
        Team team = new Team(teamIdx, resolvedName);
        team.getPlayerIds().add(requester.getId());
        team.getPlayerIds().add(target.getId());
        room.getTeams().add(team);

        // In 6p mode, check if we can auto-complete the last team
        if (playerCount == 6) {
            tryAutoCompleteLastTeam(room, playerCount);
        }

        return toLobbyDTO(room, false);
    }

    private void unpairTeam(GameRoom room, int teamIndex) {
        for (Player p : room.getPlayers()) {
            if (p.getTeamIndex() == teamIndex) {
                p.setTeamIndex(-1);
                p.setAutoPartnered(false);
            }
        }
    }

    private void clearAutoPartners(GameRoom room) {
        Set<Integer> autoTeamIndices = new HashSet<>();
        for (Player p : room.getPlayers()) {
            if (p.isAutoPartnered()) {
                autoTeamIndices.add(p.getTeamIndex());
                p.setTeamIndex(-1);
                p.setAutoPartnered(false);
            }
        }
        room.getTeams().removeIf(t -> autoTeamIndices.contains(t.getIndex()));
    }

    private void clearAllTeamAssignments(GameRoom room) {
        for (Player p : room.getPlayers()) {
            p.setTeamIndex(-1);
            p.setAutoPartnered(false);
        }
        room.getTeams().clear();
    }

    private int nextAvailableTeamIndex(GameRoom room) {
        Set<Integer> used = new HashSet<>();
        for (Player p : room.getPlayers()) {
            if (p.getTeamIndex() != -1) {
                used.add(p.getTeamIndex());
            }
        }
        int idx = 0;
        while (used.contains(idx)) idx++;
        return idx;
    }

    private void tryAutoCompleteLastTeam(GameRoom room, int playerCount) {
        if (playerCount != 6) return;

        // Count manual teams (teams where no player is auto-partnered)
        Set<Integer> manualTeams = new HashSet<>();
        for (Player p : room.getPlayers()) {
            if (p.getTeamIndex() != -1 && !p.isAutoPartnered()) {
                manualTeams.add(p.getTeamIndex());
            }
        }

        if (manualTeams.size() < 2) return;

        // Find unpaired players
        List<Player> unpaired = new ArrayList<>();
        for (Player p : room.getPlayers()) {
            if (p.getTeamIndex() == -1) {
                unpaired.add(p);
            }
        }

        if (unpaired.size() == 2) {
            int teamIdx = nextAvailableTeamIndex(room);
            for (Player p : unpaired) {
                p.setTeamIndex(teamIdx);
                p.setAutoPartnered(true);
            }
            // Create auto team with default name
            Team autoTeam = new Team(teamIdx, "Team " + (teamIdx + 1));
            autoTeam.getPlayerIds().add(unpaired.get(0).getId());
            autoTeam.getPlayerIds().add(unpaired.get(1).getId());
            room.getTeams().add(autoTeam);
        }
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        // Ensure uniqueness
        if (rooms.containsKey(code.toString())) {
            return generateRoomCode();
        }
        return code.toString();
    }
}
