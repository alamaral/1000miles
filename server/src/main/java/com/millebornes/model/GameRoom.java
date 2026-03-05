package com.millebornes.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {
    private String roomCode;
    private String hostPlayerId;
    private List<Player> players = new ArrayList<>();
    private List<Team> teams = new ArrayList<>();
    private boolean useTeams = false;
    private int maxPlayers = 6;
    // Maps WebSocket sessionId -> spectator display name
    private Map<String, String> spectators = new ConcurrentHashMap<>();

    public GameRoom() {}

    public GameRoom(String roomCode, String hostPlayerId) {
        this.roomCode = roomCode;
        this.hostPlayerId = hostPlayerId;
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

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getHostPlayerId() { return hostPlayerId; }
    public void setHostPlayerId(String hostPlayerId) { this.hostPlayerId = hostPlayerId; }
    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
    public List<Team> getTeams() { return teams; }
    public void setTeams(List<Team> teams) { this.teams = teams; }
    public boolean isUseTeams() { return useTeams; }
    public void setUseTeams(boolean useTeams) { this.useTeams = useTeams; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public Map<String, String> getSpectators() { return spectators; }

    public void addSpectator(String sessionId, String name) {
        spectators.put(sessionId, name);
    }

    public void removeSpectator(String sessionId) {
        spectators.remove(sessionId);
    }
}
