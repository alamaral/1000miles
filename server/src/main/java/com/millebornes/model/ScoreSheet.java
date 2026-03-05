package com.millebornes.model;

public class ScoreSheet {
    private String playerId;
    private String teamName;
    private int distancePoints;
    private int safetyPoints;
    private int allSafetiesBonus;
    private int coupFourrePoints;
    private int tripCompletedBonus;
    private int delayedActionBonus;
    private int safeTripBonus;
    private int shutoutBonus;
    private int totalHandScore;

    public ScoreSheet() {}

    public ScoreSheet(String playerId) {
        this.playerId = playerId;
    }

    public int calculateTotal() {
        totalHandScore = distancePoints + safetyPoints + allSafetiesBonus
                + coupFourrePoints + tripCompletedBonus + delayedActionBonus
                + safeTripBonus + shutoutBonus;
        return totalHandScore;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public int getDistancePoints() { return distancePoints; }
    public void setDistancePoints(int distancePoints) { this.distancePoints = distancePoints; }
    public int getSafetyPoints() { return safetyPoints; }
    public void setSafetyPoints(int safetyPoints) { this.safetyPoints = safetyPoints; }
    public int getAllSafetiesBonus() { return allSafetiesBonus; }
    public void setAllSafetiesBonus(int allSafetiesBonus) { this.allSafetiesBonus = allSafetiesBonus; }
    public int getCoupFourrePoints() { return coupFourrePoints; }
    public void setCoupFourrePoints(int coupFourrePoints) { this.coupFourrePoints = coupFourrePoints; }
    public int getTripCompletedBonus() { return tripCompletedBonus; }
    public void setTripCompletedBonus(int tripCompletedBonus) { this.tripCompletedBonus = tripCompletedBonus; }
    public int getDelayedActionBonus() { return delayedActionBonus; }
    public void setDelayedActionBonus(int delayedActionBonus) { this.delayedActionBonus = delayedActionBonus; }
    public int getSafeTripBonus() { return safeTripBonus; }
    public void setSafeTripBonus(int safeTripBonus) { this.safeTripBonus = safeTripBonus; }
    public int getShutoutBonus() { return shutoutBonus; }
    public void setShutoutBonus(int shutoutBonus) { this.shutoutBonus = shutoutBonus; }
    public int getTotalHandScore() { return totalHandScore; }
    public void setTotalHandScore(int totalHandScore) { this.totalHandScore = totalHandScore; }
}
