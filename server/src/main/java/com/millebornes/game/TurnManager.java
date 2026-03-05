package com.millebornes.game;

import com.millebornes.model.GameState;
import com.millebornes.model.TurnPhase;

public class TurnManager {

    /**
     * Advance to the next player's turn.
     * Skips straight to PLAY phase when the deck is empty.
     */
    public static void advanceTurn(GameState state) {
        int next = (state.getCurrentPlayerIndex() + 1) % state.getPlayers().size();
        state.setCurrentPlayerIndex(next);
        state.setTurnPhase(state.isDeckEmpty() ? TurnPhase.PLAY : TurnPhase.DRAW);
    }

    /**
     * Set the turn to a specific player (used for coup fourré).
     * Skips straight to PLAY phase when the deck is empty.
     */
    public static void setTurnToPlayer(GameState state, String playerId) {
        int index = state.getPlayerIndex(playerId);
        if (index >= 0) {
            state.setCurrentPlayerIndex(index);
            state.setTurnPhase(state.isDeckEmpty() ? TurnPhase.PLAY : TurnPhase.DRAW);
        }
    }

    /**
     * Transition from DRAW to PLAY phase.
     */
    public static void moveToPlayPhase(GameState state) {
        state.setTurnPhase(TurnPhase.PLAY);
    }

    /**
     * Open coup fourré window.
     */
    public static void openCoupFourreWindow(GameState state) {
        state.setTurnPhase(TurnPhase.COUP_FOURRE_WINDOW);
    }

    /**
     * Close coup fourré window and advance turn.
     */
    public static void closeCoupFourreWindow(GameState state) {
        state.setCoupFourreTargetPlayerId(null);
        state.setCoupFourreHazardType(null);
        state.setCoupFourreDeadline(0);
        state.setCoupFourreSourcePlayerId(null);
        advanceTurn(state);
    }
}
