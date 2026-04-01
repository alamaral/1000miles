// Copyright © 2026 Alan Amaral
// All rights reserved.
//
// Unauthorized copying, modification, distribution, or use of this software,
// via any medium, is strictly prohibited without prior written permission.
//
// Description:
// DTO for a play card request containing the card index and optional target player ID.

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
