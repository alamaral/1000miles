package com.millebornes.game;

import com.millebornes.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void createDeck_has106Cards() {
        List<Card> deck = Deck.createDeck();
        assertEquals(106, deck.size());
    }

    @Test
    void createDeck_hasCorrectDistribution() {
        List<Card> deck = Deck.createDeck();

        assertEquals(10, deck.stream().filter(c -> c == Card.MILES_25).count());
        assertEquals(10, deck.stream().filter(c -> c == Card.MILES_50).count());
        assertEquals(10, deck.stream().filter(c -> c == Card.MILES_75).count());
        assertEquals(12, deck.stream().filter(c -> c == Card.MILES_100).count());
        assertEquals(4, deck.stream().filter(c -> c == Card.MILES_200).count());

        assertEquals(3, deck.stream().filter(c -> c == Card.ACCIDENT).count());
        assertEquals(3, deck.stream().filter(c -> c == Card.OUT_OF_GAS).count());
        assertEquals(3, deck.stream().filter(c -> c == Card.FLAT_TIRE).count());
        assertEquals(5, deck.stream().filter(c -> c == Card.STOP).count());
        assertEquals(4, deck.stream().filter(c -> c == Card.SPEED_LIMIT).count());

        assertEquals(14, deck.stream().filter(c -> c == Card.ROLL).count());
        assertEquals(6, deck.stream().filter(c -> c == Card.REPAIRS).count());
        assertEquals(6, deck.stream().filter(c -> c == Card.GASOLINE).count());
        assertEquals(6, deck.stream().filter(c -> c == Card.SPARE_TIRE).count());
        assertEquals(6, deck.stream().filter(c -> c == Card.END_OF_LIMIT).count());

        assertEquals(1, deck.stream().filter(c -> c == Card.DRIVING_ACE).count());
        assertEquals(1, deck.stream().filter(c -> c == Card.EXTRA_TANK).count());
        assertEquals(1, deck.stream().filter(c -> c == Card.PUNCTURE_PROOF).count());
        assertEquals(1, deck.stream().filter(c -> c == Card.RIGHT_OF_WAY).count());
    }

    @Test
    void draw_removesTopCard() {
        List<Card> deck = Deck.createShuffledDeck();
        int originalSize = deck.size();
        Card drawn = Deck.draw(deck);
        assertNotNull(drawn);
        assertEquals(originalSize - 1, deck.size());
    }

    @Test
    void draw_emptyDeckReturnsNull() {
        List<Card> deck = Deck.createDeck();
        deck.clear();
        assertNull(Deck.draw(deck));
    }
}
