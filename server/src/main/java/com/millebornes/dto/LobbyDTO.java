package com.millebornes.dto;

import java.util.List;
import java.util.Map;

public class LobbyDTO {
    private String roomCode;
    private String hostPlayerName;
    private List<PlayerInfo> players;
    private int maxPlayers;
    private boolean useTeams;
    private boolean gameStarted;
    private List<String> spectators;
    private Map<Integer, String> teamNames;

    public static class PlayerInfo {
        private String id;
        private String name;
        private int teamIndex;
        private boolean autoPartnered;

        public PlayerInfo() {}

        public PlayerInfo(String id, String name, int teamIndex, boolean autoPartnered) {
            this.id = id;
            this.name = name;
            this.teamIndex = teamIndex;
            this.autoPartnered = autoPartnered;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getTeamIndex() { return teamIndex; }
        public void setTeamIndex(int teamIndex) { this.teamIndex = teamIndex; }
        public boolean isAutoPartnered() { return autoPartnered; }
        public void setAutoPartnered(boolean autoPartnered) { this.autoPartnered = autoPartnered; }
    }

    public LobbyDTO() {}

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getHostPlayerName() { return hostPlayerName; }
    public void setHostPlayerName(String hostPlayerName) { this.hostPlayerName = hostPlayerName; }
    public List<PlayerInfo> getPlayers() { return players; }
    public void setPlayers(List<PlayerInfo> players) { this.players = players; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public boolean isUseTeams() { return useTeams; }
    public void setUseTeams(boolean useTeams) { this.useTeams = useTeams; }
    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }
    public List<String> getSpectators() { return spectators; }
    public void setSpectators(List<String> spectators) { this.spectators = spectators; }
    public Map<Integer, String> getTeamNames() { return teamNames; }
    public void setTeamNames(Map<Integer, String> teamNames) { this.teamNames = teamNames; }
}
