package com.millebornes.dto;

public class PlayCardRequest {
    private int cardIndex;
    private String targetPlayerId;

    public PlayCardRequest() {}

    public int getCardIndex() { return cardIndex; }
    public void setCardIndex(int cardIndex) { this.cardIndex = cardIndex; }
    public String getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }
}
