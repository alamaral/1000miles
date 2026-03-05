package com.millebornes.game;

import com.millebornes.model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    /**
     * Creates a standard 106-card Mille Bornes deck.
     * Distribution:
     * Distance: 25mi x10, 50mi x10, 75mi x10, 100mi x12, 200mi x4
     * Hazards: Accident x3, Out of Gas x3, Flat Tire x3, Stop x5, Speed Limit x4
     * Remedies: Repairs x6, Gasoline x6, Spare Tire x6, Roll x14, End of Limit x6
     * Safeties: 1 each (Driving Ace, Extra Tank, Puncture Proof, Right of Way)
     */
    public static List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();

        // Distance cards
        addCards(deck, Card.MILES_25, 10);
        addCards(deck, Card.MILES_50, 10);
        addCards(deck, Card.MILES_75, 10);
        addCards(deck, Card.MILES_100, 12);
        addCards(deck, Card.MILES_200, 4);

        // Hazard cards
        addCards(deck, Card.ACCIDENT, 3);
        addCards(deck, Card.OUT_OF_GAS, 3);
        addCards(deck, Card.FLAT_TIRE, 3);
        addCards(deck, Card.STOP, 5);
        addCards(deck, Card.SPEED_LIMIT, 4);

        // Remedy cards
        addCards(deck, Card.REPAIRS, 6);
        addCards(deck, Card.GASOLINE, 6);
        addCards(deck, Card.SPARE_TIRE, 6);
        addCards(deck, Card.ROLL, 14);
        addCards(deck, Card.END_OF_LIMIT, 6);

        // Safety cards (1 each)
        deck.add(Card.DRIVING_ACE);
        deck.add(Card.EXTRA_TANK);
        deck.add(Card.PUNCTURE_PROOF);
        deck.add(Card.RIGHT_OF_WAY);

        return deck;
    }

    public static List<Card> createShuffledDeck() {
        List<Card> deck = createDeck();
        Collections.shuffle(deck);
        return deck;
    }

    public static Card draw(List<Card> deck) {
        if (deck.isEmpty()) return null;
        return deck.remove(deck.size() - 1);
    }

    private static void addCards(List<Card> deck, Card card, int count) {
        for (int i = 0; i < count; i++) {
            deck.add(card);
        }
    }
}
