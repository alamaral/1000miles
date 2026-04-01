// Copyright © 2026 Alan Amaral
// All rights reserved.
//
// Unauthorized copying, modification, distribution, or use of this software,
// via any medium, is strictly prohibited without prior written permission.
//
// Description:
// DTO for a coup fourre response containing the index of the safety card to play.

package com.millebornes.dto;

public class CoupFourreResponse {
    private int cardIndex;

    public CoupFourreResponse() {}

    public int getCardIndex() { return cardIndex; }
    public void setCardIndex(int cardIndex) { this.cardIndex = cardIndex; }
}
