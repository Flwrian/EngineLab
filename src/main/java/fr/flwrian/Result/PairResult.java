package fr.flwrian.Result;

import java.util.List;

/**
 * Result of a pair of games (engine1 vs engine2 twice with swapped colors).
 */
public class PairResult {
    private final int pairId;
    private final List<GameResult> gameResults;
    private final String engine1Name;
    private final String engine2Name;

    public PairResult(int pairId, List<GameResult> gameResults) {
        this(pairId, gameResults, "Engine1", "Engine2");
    }

    public PairResult(int pairId, List<GameResult> gameResults, String engine1Name, String engine2Name) {
        this.pairId = pairId;
        this.gameResults = gameResults;
        this.engine1Name = engine1Name;
        this.engine2Name = engine2Name;
    }

    public int getPairId() {
        return pairId;
    }

    public List<GameResult> getGameResults() {
        return gameResults;
    }

    public String getEngine1Name() {
        return engine1Name;
    }

    public String getEngine2Name() {
        return engine2Name;
    }

    /**
     * Calculate score for engine1 from this pair.
     * Engine1 is white in game1, black in game2.
     */
    public double getEngine1Score() {
        double score = 0.0;
        
        // Game 1: engine1 is white
        if (gameResults.size() > 0) {
            score += getScoreForWhite(gameResults.get(0).getResult());
        }
        
        // Game 2: engine1 is black
        if (gameResults.size() > 1) {
            score += getScoreForBlack(gameResults.get(1).getResult());
        }
        
        return score;
    }

    /**
     * Calculate score for engine2 from this pair.
     */
    public double getEngine2Score() {
        double score = 0.0;
        
        // Game 1: engine2 is black
        if (gameResults.size() > 0) {
            score += getScoreForBlack(gameResults.get(0).getResult());
        }
        
        // Game 2: engine2 is white
        if (gameResults.size() > 1) {
            score += getScoreForWhite(gameResults.get(1).getResult());
        }
        
        return score;
    }

    private double getScoreForWhite(String result) {
        switch (result) {
            case "1-0": return 1.0;
            case "1/2-1/2": return 0.5;
            case "0-1": return 0.0;
            default: return 0.0; // Treat unknown results as loss
        }
    }

    private double getScoreForBlack(String result) {
        switch (result) {
            case "0-1": return 1.0;
            case "1/2-1/2": return 0.5;
            case "1-0": return 0.0;
            default: return 0.0;
        }
    }

    @Override
    public String toString() {
        return String.format("Pair %d: %s %.1f - %.1f %s", 
            pairId, engine1Name, getEngine1Score(), getEngine2Score(), engine2Name);
    }
}
