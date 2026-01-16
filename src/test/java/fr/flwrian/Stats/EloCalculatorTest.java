package fr.flwrian.Stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EloCalculatorTest {

    @Test
    void testDefaultElo() {
        assertEquals(1500, EloCalculator.DEFAULT_ELO);
    }

    @Test
    void testExpectedScoreEqual() {
        // Two players with same rating should have 50% expected score
        double expected = EloCalculator.calculateExpectedScore(1500, 1500);
        assertEquals(0.5, expected, 0.001);
    }

    @Test
    void testExpectedScoreHigherPlayer() {
        // Player with 100 points more should have ~64% expected score
        double expected = EloCalculator.calculateExpectedScore(1600, 1500);
        assertTrue(expected > 0.6 && expected < 0.7);
    }

    @Test
    void testExpectedScoreLowerPlayer() {
        // Player with 100 points less should have ~36% expected score
        double expected = EloCalculator.calculateExpectedScore(1400, 1500);
        assertTrue(expected > 0.3 && expected < 0.4);
    }

    @Test
    void testWinIncreasesElo() {
        int currentElo = 1500;
        int opponentElo = 1500;
        int newElo = EloCalculator.calculateNewElo(currentElo, opponentElo, 1.0);
        
        // Winning against equal opponent should increase rating
        assertTrue(newElo > currentElo);
    }

    @Test
    void testLossDecreasesElo() {
        int currentElo = 1500;
        int opponentElo = 1500;
        int newElo = EloCalculator.calculateNewElo(currentElo, opponentElo, 0.0);
        
        // Losing against equal opponent should decrease rating
        assertTrue(newElo < currentElo);
    }

    @Test
    void testDrawMaintainsElo() {
        int currentElo = 1500;
        int opponentElo = 1500;
        int newElo = EloCalculator.calculateNewElo(currentElo, opponentElo, 0.5);
        
        // Drawing against equal opponent should maintain rating
        assertEquals(currentElo, newElo);
    }

    @Test
    void testBigWinAgainstStrongerOpponent() {
        int currentElo = 1400;
        int opponentElo = 1600; // Opponent is 200 points higher
        int newElo = EloCalculator.calculateNewElo(currentElo, opponentElo, 1.0);
        
        // Winning against stronger opponent should give big gain
        int gain = newElo - currentElo;
        assertTrue(gain > 16); // Should gain more than K/2
    }

    @Test
    void testSmallWinAgainstWeakerOpponent() {
        int currentElo = 1600;
        int opponentElo = 1400; // Opponent is 200 points lower
        int newElo = EloCalculator.calculateNewElo(currentElo, opponentElo, 1.0);
        
        // Winning against weaker opponent should give small gain
        int gain = newElo - currentElo;
        assertTrue(gain < 16); // Should gain less than K/2
    }

    @Test
    void testUpdateBothPlayers() {
        int whiteElo = 1500;
        int blackElo = 1500;
        
        // White wins
        int[] newElos = EloCalculator.updateEloRatings(whiteElo, blackElo, "1-0");
        
        // White should gain points, black should lose the same amount
        assertTrue(newElos[0] > whiteElo);
        assertTrue(newElos[1] < blackElo);
        
        // Total Elo should be conserved (approximately, due to rounding)
        int totalBefore = whiteElo + blackElo;
        int totalAfter = newElos[0] + newElos[1];
        assertEquals(totalBefore, totalAfter, 1); // Allow 1 point difference due to rounding
    }

    @Test
    void testBlackWin() {
        int whiteElo = 1500;
        int blackElo = 1500;
        
        int[] newElos = EloCalculator.updateEloRatings(whiteElo, blackElo, "0-1");
        
        // Black should gain points, white should lose
        assertTrue(newElos[1] > blackElo);
        assertTrue(newElos[0] < whiteElo);
    }

    @Test
    void testDraw() {
        int whiteElo = 1500;
        int blackElo = 1500;
        
        int[] newElos = EloCalculator.updateEloRatings(whiteElo, blackElo, "1/2-1/2");
        
        // Both should maintain rating in a draw between equals
        assertEquals(whiteElo, newElos[0]);
        assertEquals(blackElo, newElos[1]);
    }

    @Test
    void testInvalidResult() {
        assertThrows(IllegalArgumentException.class, () -> {
            EloCalculator.updateEloRatings(1500, 1500, "invalid");
        });
    }

    @Test
    void testKFactor() {
        double kFactor = EloCalculator.getKFactor();
        assertEquals(32.0, kFactor);
    }
}
