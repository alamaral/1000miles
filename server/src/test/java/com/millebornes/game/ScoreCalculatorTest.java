package com.millebornes.game;

import com.millebornes.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculatorTest {

    private GameState state;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        state = new GameState("TEST");
        state.setPhase(GamePhase.PLAYING);

        player1 = new Player("p1", "Alice", "s1");
        player2 = new Player("p2", "Bob", "s2");

        state.getPlayers().add(player1);
        state.getPlayers().add(player2);
    }

    @Test
    void distancePoints() {
        player1.getDistancePile().add(Card.MILES_100);
        player1.getDistancePile().add(Card.MILES_75);
        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        assertEquals(175, sheet.getDistancePoints());
    }

    @Test
    void safetyPoints() {
        player1.getSafetyArea().add(Card.DRIVING_ACE);
        player1.getSafetyArea().add(Card.EXTRA_TANK);
        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        assertEquals(200, sheet.getSafetyPoints());
    }

    @Test
    void allSafetiesBonus() {
        player1.getSafetyArea().add(Card.DRIVING_ACE);
        player1.getSafetyArea().add(Card.EXTRA_TANK);
        player1.getSafetyArea().add(Card.PUNCTURE_PROOF);
        player1.getSafetyArea().add(Card.RIGHT_OF_WAY);
        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        assertEquals(400, sheet.getSafetyPoints());  // 4 * 100
        assertEquals(300, sheet.getAllSafetiesBonus());
    }

    @Test
    void coupFourrePoints() {
        player1.setCoupFourreCount(2);
        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        assertEquals(600, sheet.getCoupFourrePoints());
    }

    @Test
    void tripCompletedBonus() {
        // Give player exactly 700 miles (the default mile target)
        for (int i = 0; i < 7; i++) {
            player1.getDistancePile().add(Card.MILES_100);
        }
        assertEquals(700, player1.getTotalMiles());
        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        assertEquals(400, sheet.getTripCompletedBonus());
    }

    @Test
    void safeTripBonus_noTwoHundreds() {
        // 700 miles without any 200-mile cards
        for (int i = 0; i < 7; i++) {
            player1.getDistancePile().add(Card.MILES_100);
        }
        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        assertEquals(300, sheet.getSafeTripBonus());
    }

    @Test
    void shutoutBonus() {
        for (int i = 0; i < 7; i++) {
            player1.getDistancePile().add(Card.MILES_100);
        }
        // player2 at 0 miles
        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        assertEquals(500, sheet.getShutoutBonus());
    }

    @Test
    void totalCalculation() {
        for (int i = 0; i < 7; i++) {
            player1.getDistancePile().add(Card.MILES_100);
        }
        player1.getSafetyArea().add(Card.DRIVING_ACE);
        player1.setCoupFourreCount(1);

        // Put some cards in deck so delayed action bonus doesn't apply
        state.getDeck().add(Card.MILES_25);

        ScoreSheet sheet = ScoreCalculator.calculateHandScore(player1, state);
        // 700 (distance) + 100 (safety) + 300 (coup fourre)
        // + 400 (trip) + 300 (safe trip) + 500 (shutout)
        assertEquals(2300, sheet.getTotalHandScore());
    }
}
