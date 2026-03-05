package com.millebornes.game;

import com.millebornes.model.*;

import java.util.*;

public class ScoreCalculator {

    public static ScoreSheet calculateHandScore(Player player, GameState state) {
        ScoreSheet sheet = new ScoreSheet(player.getId());

        // Distance points
        sheet.setDistancePoints(player.getTotalMiles());

        // Safety points: 100 per safety played
        sheet.setSafetyPoints(player.getSafetyArea().size() * 100);

        // All 4 safeties bonus: 300 extra (total 700 for all safeties)
        if (player.getSafetyArea().size() == 4) {
            sheet.setAllSafetiesBonus(300);
        }

        // Coup fourré: 300 per coup fourré
        sheet.setCoupFourrePoints(player.getCoupFourreCount() * 300);

        // Trip completed: +400 if reached mile target
        if (player.getTotalMiles() >= state.getMileTarget()) {
            sheet.setTripCompletedBonus(400);

            // Delayed action: +300 if completed after draw pile empty
            if (state.isDeckEmpty()) {
                sheet.setDelayedActionBonus(300);
            }

            // Safe trip: +300 if no 200-mile cards used
            if (player.getTwoHundredCount() == 0) {
                sheet.setSafeTripBonus(300);
            }
        }

        // Shutout: +500 for each opponent at 0 miles (if this player completed trip)
        if (player.getTotalMiles() >= state.getMileTarget()) {
            int shutouts = 0;
            for (Player opponent : state.getPlayers()) {
                if (!opponent.getId().equals(player.getId()) && opponent.getTotalMiles() == 0) {
                    shutouts++;
                }
            }
            sheet.setShutoutBonus(shutouts * 500);
        }

        sheet.calculateTotal();
        return sheet;
    }

    /**
     * Team-aware scoring: one ScoreSheet per team, using the representative (first member).
     * Coup fourré count sums both teammates' individual counts.
     * Shutout checks opponent teams' shared miles (not individual opponents).
     */
    public static ScoreSheet calculateTeamHandScore(Team team, GameState state) {
        // Find team members
        List<Player> members = new ArrayList<>();
        Player rep = null;
        for (Player p : state.getPlayers()) {
            if (p.getTeamIndex() == team.getIndex()) {
                members.add(p);
                if (rep == null) rep = p;
            }
        }
        if (rep == null) return new ScoreSheet("unknown");

        ScoreSheet sheet = new ScoreSheet(rep.getId());
        sheet.setTeamName(team.getName());

        // Distance points (shared piles, so use rep)
        sheet.setDistancePoints(rep.getTotalMiles());

        // Safety points (shared safety area)
        sheet.setSafetyPoints(rep.getSafetyArea().size() * 100);

        // All 4 safeties bonus
        if (rep.getSafetyArea().size() == 4) {
            sheet.setAllSafetiesBonus(300);
        }

        // Coup fourré: sum individual counts from all team members
        int totalCF = 0;
        for (Player m : members) {
            totalCF += m.getCoupFourreCount();
        }
        sheet.setCoupFourrePoints(totalCF * 300);

        // Trip completed
        if (rep.getTotalMiles() >= state.getMileTarget()) {
            sheet.setTripCompletedBonus(400);

            if (state.isDeckEmpty()) {
                sheet.setDelayedActionBonus(300);
            }

            if (rep.getTwoHundredCount() == 0) {
                sheet.setSafeTripBonus(300);
            }
        }

        // Shutout: check opponent teams' shared miles (use one rep per opponent team)
        if (rep.getTotalMiles() >= state.getMileTarget()) {
            int shutouts = 0;
            Set<Integer> counted = new HashSet<>();
            for (Player opponent : state.getPlayers()) {
                if (opponent.getTeamIndex() == team.getIndex()) continue;
                if (counted.contains(opponent.getTeamIndex())) continue;
                counted.add(opponent.getTeamIndex());
                if (opponent.getTotalMiles() == 0) {
                    shutouts++;
                }
            }
            sheet.setShutoutBonus(shutouts * 500);
        }

        sheet.calculateTotal();
        return sheet;
    }

    public static List<ScoreSheet> calculateAllScores(GameState state) {
        if (state.isUseTeams() && !state.getTeams().isEmpty()) {
            List<ScoreSheet> sheets = new ArrayList<>();
            for (Team team : state.getTeams()) {
                sheets.add(calculateTeamHandScore(team, state));
            }
            return sheets;
        }

        List<ScoreSheet> sheets = new ArrayList<>();
        for (Player player : state.getPlayers()) {
            sheets.add(calculateHandScore(player, state));
        }
        return sheets;
    }
}
