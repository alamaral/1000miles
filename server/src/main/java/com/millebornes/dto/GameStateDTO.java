package com.millebornes.dto;

import com.millebornes.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameStateDTO {
    private String roomCode;
    private GamePhase phase;
    private TurnPhase turnPhase;
    private String currentPlayerId;
    private int deckSize;
    private Card discardTop;
    private List<PlayerViewDTO> players;
    private int handNumber;
    private Map<String, Integer> cumulativeScores;
    private int mileTarget;
    private boolean extensionDeclared;
    private String extensionPromptPlayerId;
    private boolean useTeams;
    private Map<Integer, String> teamNames;

    // Coup fourré info
    private String coupFourreTargetPlayerId;
    private HazardType coupFourreHazardType;
    private String coupFourreSourcePlayerId;

    // Last-action info (for card play animation)
    private Card lastPlayedCard;
    private String lastPlayedByPlayerId;
    private String lastPlayedTargetPlayerId;
    private String lastActionType;

    public static class PlayerViewDTO {
        private String id;
        private String name;
        private int handSize;
        private Card battleTop;
        private Card speedTop;
        private List<Card> safetyArea;
        private List<Card> coupFourreSafeties;
        private List<Card> distancePile;
        private int totalMiles;
        private int teamIndex;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getHandSize() { return handSize; }
        public void setHandSize(int handSize) { this.handSize = handSize; }
        public Card getBattleTop() { return battleTop; }
        public void setBattleTop(Card battleTop) { this.battleTop = battleTop; }
        public Card getSpeedTop() { return speedTop; }
        public void setSpeedTop(Card speedTop) { this.speedTop = speedTop; }
        public List<Card> getSafetyArea() { return safetyArea; }
        public void setSafetyArea(List<Card> safetyArea) { this.safetyArea = safetyArea; }
        public List<Card> getCoupFourreSafeties() { return coupFourreSafeties; }
        public void setCoupFourreSafeties(List<Card> coupFourreSafeties) { this.coupFourreSafeties = coupFourreSafeties; }
        public List<Card> getDistancePile() { return distancePile; }
        public void setDistancePile(List<Card> distancePile) { this.distancePile = distancePile; }
        public int getTotalMiles() { return totalMiles; }
        public void setTotalMiles(int totalMiles) { this.totalMiles = totalMiles; }
        public int getTeamIndex() { return teamIndex; }
        public void setTeamIndex(int teamIndex) { this.teamIndex = teamIndex; }
    }

    /**
     * Build a GameStateDTO from a GameState, hiding private information.
     */
    public static GameStateDTO fromGameState(GameState state) {
        GameStateDTO dto = new GameStateDTO();
        dto.roomCode = state.getRoomCode();
        dto.phase = state.getPhase();
        dto.turnPhase = state.getTurnPhase();
        dto.currentPlayerId = state.getCurrentPlayer() != null ? state.getCurrentPlayer().getId() : null;
        dto.deckSize = state.getDeck().size();
        dto.discardTop = state.getDiscardPile().isEmpty() ? null
                : state.getDiscardPile().get(state.getDiscardPile().size() - 1);
        dto.handNumber = state.getHandNumber();
        dto.cumulativeScores = state.getCumulativeScores();
        dto.mileTarget = state.getMileTarget();
        dto.extensionDeclared = state.isExtensionDeclared();
        dto.extensionPromptPlayerId = state.getExtensionPromptPlayerId();
        dto.useTeams = state.isUseTeams();

        // Build team names map
        dto.teamNames = new HashMap<>();
        for (Team t : state.getTeams()) {
            dto.teamNames.put(t.getIndex(), t.getName());
        }

        // Coup fourré info
        dto.coupFourreTargetPlayerId = state.getCoupFourreTargetPlayerId();
        dto.coupFourreHazardType = state.getCoupFourreHazardType();
        dto.coupFourreSourcePlayerId = state.getCoupFourreSourcePlayerId();

        // Last-action info
        dto.lastPlayedCard = state.getLastPlayedCard();
        dto.lastPlayedByPlayerId = state.getLastPlayedByPlayerId();
        dto.lastPlayedTargetPlayerId = state.getLastPlayedTargetPlayerId();
        dto.lastActionType = state.getLastActionType();

        // Build player views (hiding hands)
        dto.players = new ArrayList<>();
        for (Player p : state.getPlayers()) {
            PlayerViewDTO pv = new PlayerViewDTO();
            pv.setId(p.getId());
            pv.setName(p.getName());
            pv.setHandSize(p.getHand().size());
            pv.setBattleTop(p.getBattleTop());
            pv.setSpeedTop(p.getSpeedTop());
            pv.setSafetyArea(new ArrayList<>(p.getSafetyArea()));
            pv.setCoupFourreSafeties(new ArrayList<>(p.getCoupFourreSafeties()));
            pv.setDistancePile(new ArrayList<>(p.getDistancePile()));
            pv.setTotalMiles(p.getTotalMiles());
            pv.setTeamIndex(p.getTeamIndex());
            dto.players.add(pv);
        }

        return dto;
    }

    // Getters
    public String getRoomCode() { return roomCode; }
    public GamePhase getPhase() { return phase; }
    public TurnPhase getTurnPhase() { return turnPhase; }
    public String getCurrentPlayerId() { return currentPlayerId; }
    public int getDeckSize() { return deckSize; }
    public Card getDiscardTop() { return discardTop; }
    public List<PlayerViewDTO> getPlayers() { return players; }
    public int getHandNumber() { return handNumber; }
    public Map<String, Integer> getCumulativeScores() { return cumulativeScores; }
    public int getMileTarget() { return mileTarget; }
    public boolean isExtensionDeclared() { return extensionDeclared; }
    public String getExtensionPromptPlayerId() { return extensionPromptPlayerId; }
    public boolean isUseTeams() { return useTeams; }
    public Map<Integer, String> getTeamNames() { return teamNames; }
    public String getCoupFourreTargetPlayerId() { return coupFourreTargetPlayerId; }
    public HazardType getCoupFourreHazardType() { return coupFourreHazardType; }
    public String getCoupFourreSourcePlayerId() { return coupFourreSourcePlayerId; }
    public Card getLastPlayedCard() { return lastPlayedCard; }
    public String getLastPlayedByPlayerId() { return lastPlayedByPlayerId; }
    public String getLastPlayedTargetPlayerId() { return lastPlayedTargetPlayerId; }
    public String getLastActionType() { return lastActionType; }
}
