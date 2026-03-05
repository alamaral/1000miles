package com.millebornes.model;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private int index;
    private String name;
    private List<String> playerIds = new ArrayList<>();
    private int cumulativeScore = 0;

    public Team() {}

    public Team(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<String> playerIds) { this.playerIds = playerIds; }
    public int getCumulativeScore() { return cumulativeScore; }
    public void setCumulativeScore(int cumulativeScore) { this.cumulativeScore = cumulativeScore; }
}
