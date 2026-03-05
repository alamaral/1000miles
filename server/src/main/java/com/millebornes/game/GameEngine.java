package com.millebornes.game;

import com.millebornes.model.*;

import java.util.List;

public class GameEngine {

    /**
     * Validates whether a player can play a given card.
     * Returns null if valid, or an error message string if invalid.
     */
    public static String validatePlay(GameState state, Player player, Card card, Player target) {
        if (state.getPhase() != GamePhase.PLAYING) {
            return "Game is not in playing phase";
        }
        if (state.getTurnPhase() != TurnPhase.PLAY) {
            return "Not in play phase (draw first)";
        }
        if (!state.getCurrentPlayer().getId().equals(player.getId())) {
            return "It's not your turn";
        }
        if (!player.getHand().contains(card)) {
            return "You don't have that card";
        }

        switch (card.getCardType()) {
            case DISTANCE:
                return validateDistancePlay(state, player, card);
            case HAZARD:
                return validateHazardPlay(state, player, card, target);
            case REMEDY:
                return validateRemedyPlay(player, card);
            case SAFETY:
                return null; // Safeties can always be played
            default:
                return "Unknown card type";
        }
    }

    private static String validateDistancePlay(GameState state, Player player, Card card) {
        if (!player.isRolling()) {
            return "You need a Roll card (or Right of Way) before playing distance";
        }
        if (player.isHazarded()) {
            return "You have an unresolved hazard";
        }
        if (card == Card.MILES_200 && player.getTwoHundredCount() >= 2) {
            return "You can only play two 200-mile cards per hand";
        }
        if (player.hasSpeedLimit() && card.getValue() > 50) {
            return "Speed limit: you can only play 25 or 50 mile cards";
        }
        if (player.getTotalMiles() + card.getValue() > state.getMileTarget()) {
            return "That would exceed the " + state.getMileTarget() + " mile target";
        }
        return null;
    }

    private static String validateHazardPlay(GameState state, Player player, Card card, Player target) {
        if (target == null) {
            return "You must select a target player for hazard cards";
        }
        if (target.getId().equals(player.getId())) {
            return "You cannot play a hazard on yourself";
        }
        // Block hazards against teammates
        if (state.isUseTeams() && player.getTeamIndex() != -1
                && player.getTeamIndex() == target.getTeamIndex()) {
            return "You cannot play a hazard on your teammate";
        }

        HazardType hazard = card.getHazardType();

        // Check if target has the corresponding safety
        if (target.hasSafety(hazard)) {
            return "Target has the safety against this hazard";
        }

        if (hazard == HazardType.SPEED_LIMIT) {
            // Speed limit: Right of Way also protects
            if (target.hasRightOfWay()) {
                return "Target has Right of Way";
            }
            if (target.hasSpeedLimit()) {
                return "Target already has a speed limit";
            }
        } else {
            // All other hazards require target to be rolling
            if (!target.isRolling()) {
                return "Target is not rolling (already stopped or hazarded)";
            }
            // Right of Way protects against Stop
            if (hazard == HazardType.STOP && target.hasRightOfWay()) {
                return "Target has Right of Way";
            }
        }

        return null;
    }

    private static String validateRemedyPlay(Player player, Card card) {
        HazardType remedyFor = card.getHazardType();

        if (card == Card.ROLL) {
            // Roll: can be played if not rolling and not hazarded (except Stop)
            if (player.isRolling()) {
                return "You are already rolling";
            }
            Card battleTop = player.getBattleTop();
            if (battleTop != null && battleTop.getCardType() == CardType.HAZARD
                    && battleTop.getHazardType() != HazardType.STOP) {
                return "You must fix your " + battleTop.getDisplayName() + " first";
            }
            return null;
        }

        if (card == Card.END_OF_LIMIT) {
            if (!player.hasSpeedLimit()) {
                return "You don't have a speed limit to remove";
            }
            return null;
        }

        // Other remedies: must have the matching hazard on battle pile
        Card battleTop = player.getBattleTop();
        if (battleTop == null || battleTop.getCardType() != CardType.HAZARD
                || battleTop.getHazardType() != remedyFor) {
            return "You don't have the matching hazard to fix";
        }
        return null;
    }

    /**
     * Applies a card play to the game state. Assumes validation has passed.
     */
    public static void applyPlay(GameState state, Player player, Card card, Player target) {
        player.getHand().remove(card);

        switch (card.getCardType()) {
            case DISTANCE:
                player.getDistancePile().add(card);
                break;
            case HAZARD:
                if (card.getHazardType() == HazardType.SPEED_LIMIT) {
                    target.getSpeedPile().add(card);
                } else {
                    target.getBattlePile().add(card);
                }
                break;
            case REMEDY:
                if (card == Card.END_OF_LIMIT) {
                    player.getSpeedPile().add(card);
                } else {
                    player.getBattlePile().add(card);
                }
                break;
            case SAFETY:
                player.getSafetyArea().add(card);
                // If the safety counters a current hazard, resolve it
                resolveSafetyEffect(player, card);
                break;
        }
    }

    /**
     * When a safety is played, it may resolve an active hazard or speed limit.
     */
    private static void resolveSafetyEffect(Player player, Card safety) {
        HazardType protects = safety.getHazardType();

        // Check battle pile for matching hazard
        Card battleTop = player.getBattleTop();
        if (battleTop != null && battleTop.getCardType() == CardType.HAZARD
                && battleTop.getHazardType() == protects) {
            // Remove the hazard effect by putting a Roll on the battle pile
            player.getBattlePile().add(Card.ROLL);
        }

        // Right of Way also clears speed limit and acts as perpetual Roll
        if (safety == Card.RIGHT_OF_WAY) {
            if (player.hasSpeedLimit()) {
                player.getSpeedPile().add(Card.END_OF_LIMIT);
            }
            // If not rolling, start rolling
            if (battleTop == null || battleTop == Card.STOP) {
                player.getBattlePile().add(Card.ROLL);
            }
        }

        // If this safety matches a speed limit
        if (protects == HazardType.SPEED_LIMIT) {
            Card speedTop = player.getSpeedTop();
            if (speedTop == Card.SPEED_LIMIT) {
                player.getSpeedPile().add(Card.END_OF_LIMIT);
            }
        }
    }

    /**
     * Applies a discard action.
     */
    public static void applyDiscard(GameState state, Player player, int cardIndex) {
        if (cardIndex < 0 || cardIndex >= player.getHand().size()) return;
        Card card = player.getHand().remove(cardIndex);
        state.getDiscardPile().add(card);
    }

    /**
     * Check if the hand is complete (someone reached the mile target or deck exhausted + all played).
     */
    public static boolean isHandComplete(GameState state) {
        for (Player p : state.getPlayers()) {
            if (p.getTotalMiles() >= state.getMileTarget()) {
                return true;
            }
        }
        // Hand also ends when deck is empty and current player can't draw
        // (handled at draw time in GameService)
        return false;
    }

    /**
     * Check if a player has a coup fourré opportunity.
     * The player must have the corresponding safety card in their hand.
     */
    public static Card getCoupFourreCard(Player target, HazardType hazardType) {
        Card safetyNeeded = getSafetyForHazard(hazardType);
        if (safetyNeeded != null && target.getHand().contains(safetyNeeded)) {
            return safetyNeeded;
        }
        return null;
    }

    public static Card getSafetyForHazard(HazardType hazardType) {
        switch (hazardType) {
            case ACCIDENT: return Card.DRIVING_ACE;
            case OUT_OF_GAS: return Card.EXTRA_TANK;
            case FLAT_TIRE: return Card.PUNCTURE_PROOF;
            case STOP: return Card.RIGHT_OF_WAY;
            case SPEED_LIMIT: return Card.RIGHT_OF_WAY;
            default: return null;
        }
    }

    /**
     * Apply coup fourré. The declarer plays the safety from their hand;
     * the hazard is undone from the victim's pile and discarded.
     * Declarer and victim may be different players.
     */
    public static void applyCoupFourre(GameState state, Player declarer, Player victim, Card safetyCard) {
        // Remove safety from declarer's hand and place in declarer's safety area
        declarer.getHand().remove(safetyCard);
        declarer.getSafetyArea().add(safetyCard);
        declarer.setCoupFourreCount(declarer.getCoupFourreCount() + 1);
        declarer.getCoupFourreSafeties().add(safetyCard);

        // Undo the hazard from the victim's pile and discard it
        HazardType hazardType = safetyCard.getHazardType();
        if (hazardType == HazardType.SPEED_LIMIT) {
            List<Card> speedPile = victim.getSpeedPile();
            if (!speedPile.isEmpty() && speedPile.get(speedPile.size() - 1) == Card.SPEED_LIMIT) {
                Card removed = speedPile.remove(speedPile.size() - 1);
                state.getDiscardPile().add(removed);
            }
        } else {
            List<Card> battlePile = victim.getBattlePile();
            if (!battlePile.isEmpty() && battlePile.get(battlePile.size() - 1).getCardType() == CardType.HAZARD) {
                Card removed = battlePile.remove(battlePile.size() - 1);
                state.getDiscardPile().add(removed);
            }
        }

        // Resolve additional safety effects on the declarer
        resolveSafetyEffect(declarer, safetyCard);
    }
}
