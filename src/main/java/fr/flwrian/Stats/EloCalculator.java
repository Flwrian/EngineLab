package fr.flwrian.Stats;

/**
 * Elo rating calculator using FIDE formula
 * 
 * Formula: 
 * - Expected score: E = 1 / (1 + 10^((opponentElo - playerElo) / 400))
 * - New rating: newElo = oldElo + K * (actualScore - expectedScore)
 * 
 * Where:
 * - K factor: 32 (standard for online chess, could be adjusted)
 * - Actual score: 1 for win, 0.5 for draw, 0 for loss
 */
public class EloCalculator {
    
    // K-factor: determines how much ratings change after each game
    // Higher K = more volatile ratings
    private static final double K_FACTOR = 32.0;
    
    // Default starting Elo
    public static final int DEFAULT_ELO = 1500;
    
    /**
     * Calculate expected score for a player
     * @param playerElo Current player rating
     * @param opponentElo Current opponent rating
     * @return Expected score (0.0 to 1.0)
     */
    public static double calculateExpectedScore(int playerElo, int opponentElo) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentElo - playerElo) / 400.0));
    }
    
    /**
     * Calculate new Elo rating after a game
     * @param currentElo Current player rating
     * @param opponentElo Current opponent rating
     * @param actualScore Actual game score (1.0 = win, 0.5 = draw, 0.0 = loss)
     * @return New rating
     */
    public static int calculateNewElo(int currentElo, int opponentElo, double actualScore) {
        double expectedScore = calculateExpectedScore(currentElo, opponentElo);
        double ratingChange = K_FACTOR * (actualScore - expectedScore);
        return (int) Math.round(currentElo + ratingChange);
    }
    
    /**
     * Calculate Elo change for a game
     * @param currentElo Current player rating
     * @param opponentElo Current opponent rating
     * @param actualScore Actual game score (1.0 = win, 0.5 = draw, 0.0 = loss)
     * @return Rating change (can be negative)
     */
    public static int calculateEloChange(int currentElo, int opponentElo, double actualScore) {
        int newElo = calculateNewElo(currentElo, opponentElo, actualScore);
        return newElo - currentElo;
    }
    
    /**
     * Update Elo ratings for both players after a game
     * @param whiteElo White player current Elo
     * @param blackElo Black player current Elo
     * @param result Game result ("1-0", "0-1", "1/2-1/2")
     * @return Array with [newWhiteElo, newBlackElo]
     */
    public static int[] updateEloRatings(int whiteElo, int blackElo, String result) {
        double whiteScore;
        double blackScore;
        
        switch (result) {
            case "1-0":
                whiteScore = 1.0;
                blackScore = 0.0;
                break;
            case "0-1":
                whiteScore = 0.0;
                blackScore = 1.0;
                break;
            case "1/2-1/2":
                whiteScore = 0.5;
                blackScore = 0.5;
                break;
            default:
                throw new IllegalArgumentException("Invalid result: " + result);
        }
        
        int newWhiteElo = calculateNewElo(whiteElo, blackElo, whiteScore);
        int newBlackElo = calculateNewElo(blackElo, whiteElo, blackScore);
        
        return new int[]{newWhiteElo, newBlackElo};
    }
    
    /**
     * Get K-factor (for display/info purposes)
     */
    public static double getKFactor() {
        return K_FACTOR;
    }
}
