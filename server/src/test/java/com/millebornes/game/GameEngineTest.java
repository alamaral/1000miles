package com.millebornes.game;

import com.millebornes.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private GameState state;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        state = new GameState("TEST");
        state.setPhase(GamePhase.PLAYING);
        state.setTurnPhase(TurnPhase.PLAY);

        player1 = new Player("p1", "Alice", "s1");
        player2 = new Player("p2", "Bob", "s2");

        state.getPlayers().add(player1);
        state.getPlayers().add(player2);
        state.setCurrentPlayerIndex(0);
    }

    // === Distance card tests ===

    @Test
    void cannotPlayDistance_withoutRoll() {
        player1.getHand().add(Card.MILES_100);
        String error = GameEngine.validatePlay(state, player1, Card.MILES_100, null);
        assertNotNull(error);
        assertTrue(error.contains("Roll"));
    }

    @Test
    void canPlayDistance_withRoll() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getHand().add(Card.MILES_100);
        assertNull(GameEngine.validatePlay(state, player1, Card.MILES_100, null));
    }

    @Test
    void canPlayDistance_withRightOfWay() {
        player1.getSafetyArea().add(Card.RIGHT_OF_WAY);
        player1.getHand().add(Card.MILES_100);
        assertNull(GameEngine.validatePlay(state, player1, Card.MILES_100, null));
    }

    @Test
    void cannotExceedMileTarget() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getDistancePile().add(Card.MILES_200);
        player1.getDistancePile().add(Card.MILES_200);
        player1.getDistancePile().add(Card.MILES_100);
        player1.getDistancePile().add(Card.MILES_100);
        // Total: 600
        player1.getHand().add(Card.MILES_200);
        // Would be 800 > 700
        assertNotNull(GameEngine.validatePlay(state, player1, Card.MILES_200, null));
    }

    @Test
    void cannotPlayMore_thanTwo200MileCards() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getDistancePile().add(Card.MILES_200);
        player1.getDistancePile().add(Card.MILES_200);
        player1.getHand().add(Card.MILES_200);
        String error = GameEngine.validatePlay(state, player1, Card.MILES_200, null);
        assertNotNull(error);
        assertTrue(error.contains("200"));
    }

    @Test
    void speedLimit_restrictsCards() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getSpeedPile().add(Card.SPEED_LIMIT);
        player1.getHand().add(Card.MILES_75);
        String error = GameEngine.validatePlay(state, player1, Card.MILES_75, null);
        assertNotNull(error);
        assertTrue(error.contains("Speed limit"));
    }

    @Test
    void speedLimit_allows50() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getSpeedPile().add(Card.SPEED_LIMIT);
        player1.getHand().add(Card.MILES_50);
        assertNull(GameEngine.validatePlay(state, player1, Card.MILES_50, null));
    }

    // === Hazard card tests ===

    @Test
    void canPlayHazard_onRollingOpponent() {
        player2.getBattlePile().add(Card.ROLL);
        player1.getHand().add(Card.ACCIDENT);
        assertNull(GameEngine.validatePlay(state, player1, Card.ACCIDENT, player2));
    }

    @Test
    void cannotPlayHazard_onSelf() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getHand().add(Card.ACCIDENT);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.ACCIDENT, player1));
    }

    @Test
    void cannotPlayHazard_onStoppedOpponent() {
        // player2 has no Roll - not rolling
        player1.getHand().add(Card.ACCIDENT);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.ACCIDENT, player2));
    }

    @Test
    void cannotPlayHazard_ifOpponentHasSafety() {
        player2.getBattlePile().add(Card.ROLL);
        player2.getSafetyArea().add(Card.DRIVING_ACE);
        player1.getHand().add(Card.ACCIDENT);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.ACCIDENT, player2));
    }

    @Test
    void speedLimit_canBePlayedOnRollingOrNot() {
        // Speed limit doesn't require target to be rolling
        player1.getHand().add(Card.SPEED_LIMIT);
        assertNull(GameEngine.validatePlay(state, player1, Card.SPEED_LIMIT, player2));
    }

    @Test
    void cannotDoubleSpeedLimit() {
        player2.getSpeedPile().add(Card.SPEED_LIMIT);
        player1.getHand().add(Card.SPEED_LIMIT);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.SPEED_LIMIT, player2));
    }

    @Test
    void rightOfWay_protectsAgainstStop() {
        player2.getBattlePile().add(Card.ROLL);
        player2.getSafetyArea().add(Card.RIGHT_OF_WAY);
        player1.getHand().add(Card.STOP);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.STOP, player2));
    }

    @Test
    void rightOfWay_protectsAgainstSpeedLimit() {
        player2.getSafetyArea().add(Card.RIGHT_OF_WAY);
        player1.getHand().add(Card.SPEED_LIMIT);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.SPEED_LIMIT, player2));
    }

    // === Remedy card tests ===

    @Test
    void canPlayRoll_whenStopped() {
        // No battle top or Stop on top
        player1.getHand().add(Card.ROLL);
        assertNull(GameEngine.validatePlay(state, player1, Card.ROLL, null));
    }

    @Test
    void cannotPlayRoll_whenAlreadyRolling() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getHand().add(Card.ROLL);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.ROLL, null));
    }

    @Test
    void canPlayRepairs_afterAccident() {
        player1.getBattlePile().add(Card.ACCIDENT);
        player1.getHand().add(Card.REPAIRS);
        assertNull(GameEngine.validatePlay(state, player1, Card.REPAIRS, null));
    }

    @Test
    void cannotPlayRepairs_withoutAccident() {
        player1.getBattlePile().add(Card.ROLL);
        player1.getHand().add(Card.REPAIRS);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.REPAIRS, null));
    }

    @Test
    void cannotPlayRoll_beforeFixingHazard() {
        player1.getBattlePile().add(Card.ACCIDENT);
        player1.getHand().add(Card.ROLL);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.ROLL, null));
    }

    @Test
    void endOfLimit_removesSpeedLimit() {
        player1.getSpeedPile().add(Card.SPEED_LIMIT);
        player1.getHand().add(Card.END_OF_LIMIT);
        assertNull(GameEngine.validatePlay(state, player1, Card.END_OF_LIMIT, null));
    }

    @Test
    void cannotPlayEndOfLimit_withoutSpeedLimit() {
        player1.getHand().add(Card.END_OF_LIMIT);
        assertNotNull(GameEngine.validatePlay(state, player1, Card.END_OF_LIMIT, null));
    }

    // === Safety card tests ===

    @Test
    void safety_canAlwaysBePlayed() {
        player1.getHand().add(Card.DRIVING_ACE);
        assertNull(GameEngine.validatePlay(state, player1, Card.DRIVING_ACE, null));
    }

    // === Apply tests ===

    @Test
    void applyDistance_addsToPile() {
        player1.getHand().add(Card.MILES_100);
        GameEngine.applyPlay(state, player1, Card.MILES_100, null);
        assertEquals(100, player1.getTotalMiles());
        assertFalse(player1.getHand().contains(Card.MILES_100));
    }

    @Test
    void applyHazard_goesOnTargetBattlePile() {
        player1.getHand().add(Card.ACCIDENT);
        GameEngine.applyPlay(state, player1, Card.ACCIDENT, player2);
        assertEquals(Card.ACCIDENT, player2.getBattleTop());
    }

    @Test
    void applySpeedLimit_goesOnSpeedPile() {
        player1.getHand().add(Card.SPEED_LIMIT);
        GameEngine.applyPlay(state, player1, Card.SPEED_LIMIT, player2);
        assertEquals(Card.SPEED_LIMIT, player2.getSpeedTop());
    }

    @Test
    void applySafety_resolvesMatchingHazard() {
        player1.getBattlePile().add(Card.ACCIDENT);
        player1.getHand().add(Card.DRIVING_ACE);
        GameEngine.applyPlay(state, player1, Card.DRIVING_ACE, null);
        assertTrue(player1.getSafetyArea().contains(Card.DRIVING_ACE));
        // Should have a Roll on top now
        assertEquals(Card.ROLL, player1.getBattleTop());
    }

    @Test
    void rightOfWay_clearsSpeedLimitAndStartsRolling() {
        player1.getSpeedPile().add(Card.SPEED_LIMIT);
        player1.getHand().add(Card.RIGHT_OF_WAY);
        GameEngine.applyPlay(state, player1, Card.RIGHT_OF_WAY, null);
        assertFalse(player1.hasSpeedLimit());
        assertTrue(player1.isRolling());
    }

    // === Coup fourré tests ===

    @Test
    void coupFourre_detected() {
        player2.getHand().add(Card.DRIVING_ACE);
        Card cf = GameEngine.getCoupFourreCard(player2, HazardType.ACCIDENT);
        assertEquals(Card.DRIVING_ACE, cf);
    }

    @Test
    void coupFourre_notDetectedIfMissing() {
        Card cf = GameEngine.getCoupFourreCard(player2, HazardType.ACCIDENT);
        assertNull(cf);
    }

    @Test
    void applyCoupFourre_removesHazardAndPlacesSafety() {
        player2.getBattlePile().add(Card.ACCIDENT);
        player2.getHand().add(Card.DRIVING_ACE);
        GameEngine.applyCoupFourre(state, player2, player2, Card.DRIVING_ACE);

        assertTrue(player2.getSafetyArea().contains(Card.DRIVING_ACE));
        assertFalse(player2.getHand().contains(Card.DRIVING_ACE));
        assertEquals(1, player2.getCoupFourreCount());
        // Accident should have been removed
        assertNotEquals(Card.ACCIDENT, player2.getBattleTop());
    }

    // === Hand completion ===

    @Test
    void handComplete_whenPlayerReachesMileTarget() {
        player1.getDistancePile().add(Card.MILES_200);
        player1.getDistancePile().add(Card.MILES_200);
        player1.getDistancePile().add(Card.MILES_100);
        player1.getDistancePile().add(Card.MILES_100);
        player1.getDistancePile().add(Card.MILES_100);
        assertEquals(700, player1.getTotalMiles());
        assertTrue(GameEngine.isHandComplete(state));
    }
}
