// Copyright © 2026 Alan Amaral
// All rights reserved.
//
// Unauthorized copying, modification, distribution, or use of this software,
// via any medium, is strictly prohibited without prior written permission.
//
// Description:
// DTO for a discard request containing the index of the card to discard from hand.

package com.millebornes.dto;

public class DiscardRequest {
    private int cardIndex;

    public DiscardRequest() {}

    public int getCardIndex() { return cardIndex; }
    public void setCardIndex(int cardIndex) { this.cardIndex = cardIndex; }
}
