package com.millebornes.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Player {
    private String id;
    private String name;
    private String sessionId;
    private List<Card> hand = new ArrayList<>();
    private List<Card> battlePile = new ArrayList<>();
    private List<Card> speedPile = new ArrayList<>();
    private List<Card> safetyArea = new ArrayList<>();
    private List<Card> distancePile = new ArrayList<>();
    private int coupFourreCount = 0;
    private Set<Card> coupFourreSafeties = new HashSet<>();
    private int teamIndex = -1;
    private boolean autoPartnered = false;

    public Player() {}

    public Player(String id, String name, String sessionId) {
        this.id = id;
        this.name = name;
        this.sessionId = sessionId;
    }

    public Card getBattleTop() {
        return battlePile.isEmpty() ? null : battlePile.get(battlePile.size() - 1);
    }

    public Card getSpeedTop() {
        return speedPile.isEmpty() ? null : speedPile.get(speedPile.size() - 1);
    }

    public int getTotalMiles() {
        int total = 0;
        for (Card card : distancePile) {
            if (card.getValue() != null) {
                total += card.getValue();
            }
        }
        return total;
    }

    public int getTwoHundredCount() {
        int count = 0;
        for (Card card : distancePile) {
            if (card == Card.MILES_200) {
                count++;
            }
        }
        return count;
    }

    public boolean hasSafety(HazardType hazardType) {
        for (Card card : safetyArea) {
            if (card.getHazardType() == hazardType) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRightOfWay() {
        return hasSafety(HazardType.STOP);
    }

    public boolean isRolling() {
        if (hasRightOfWay()) return true;
        Card top = getBattleTop();
        return top == Card.ROLL;
    }

    public boolean hasSpeedLimit() {
        if (hasSafety(HazardType.SPEED_LIMIT) || hasRightOfWay()) return false;
        Card top = getSpeedTop();
        return top == Card.SPEED_LIMIT;
    }

    public boolean isHazarded() {
        Card top = getBattleTop();
        return top != null && top.getCardType() == CardType.HAZARD;
    }

    public void resetForNewHand() {
        hand.clear();
        battlePile.clear();
        speedPile.clear();
        safetyArea.clear();
        distancePile.clear();
        coupFourreCount = 0;
        coupFourreSafeties.clear();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<Card> getHand() { return hand; }
    public void setHand(List<Card> hand) { this.hand = hand; }
    public List<Card> getBattlePile() { return battlePile; }
    public void setBattlePile(List<Card> battlePile) { this.battlePile = battlePile; }
    public List<Card> getSpeedPile() { return speedPile; }
    public void setSpeedPile(List<Card> speedPile) { this.speedPile = speedPile; }
    public List<Card> getSafetyArea() { return safetyArea; }
    public void setSafetyArea(List<Card> safetyArea) { this.safetyArea = safetyArea; }
    public List<Card> getDistancePile() { return distancePile; }
    public void setDistancePile(List<Card> distancePile) { this.distancePile = distancePile; }
    public int getCoupFourreCount() { return coupFourreCount; }
    public void setCoupFourreCount(int coupFourreCount) { this.coupFourreCount = coupFourreCount; }
    public Set<Card> getCoupFourreSafeties() { return coupFourreSafeties; }
    public void setCoupFourreSafeties(Set<Card> coupFourreSafeties) { this.coupFourreSafeties = coupFourreSafeties; }
    public int getTeamIndex() { return teamIndex; }
    public void setTeamIndex(int teamIndex) { this.teamIndex = teamIndex; }
    public boolean isAutoPartnered() { return autoPartnered; }
    public void setAutoPartnered(boolean autoPartnered) { this.autoPartnered = autoPartnered; }
}
