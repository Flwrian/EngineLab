package fr.flwrian;

import fr.flwrian.Runner.MatchRunner;
import fr.flwrian.Util.FENLoader;

import java.util.List;

/**
 * Example showing how to use EngineLab with custom FEN positions.
 * This is useful for testing specific openings or positions.
 */
public class ExampleWithFEN {
    
    public static void main(String[] args) {
        String enginePath = args.length > 0 ? args[0] : "./Aspira_dev";
        
        try {
            // Example 1: Use predefined common openings
            runWithCommonOpenings(enginePath);
            
            // Example 2: Load FEN from file
            // runWithFENFile(enginePath, "openings.fen");
            
        } catch (Exception e) {
            System.err.println("Error:");
            e.printStackTrace();
        }
    }
    
    /**
     * Run a match using common opening positions.
     */
    private static void runWithCommonOpenings(String enginePath) throws Exception {
        System.out.println("=== Running match with common openings ===\n");
        
        // Get predefined openings (e4, d4, c4, Nf3, etc.)
        List<String> openings = FENLoader.getCommonOpenings();
        
        System.out.println("Loaded " + openings.size() + " opening positions:");
        for (int i = 0; i < openings.size() && i < 3; i++) {
            System.out.println("  " + (i+1) + ". " + openings.get(i));
        }
        System.out.println("  ...\n");
        
        // Create runner with:
        // - 2 concurrent games
        // - 3 minutes + 2 seconds
        MatchRunner runner = new MatchRunner(enginePath, 2, 180000, 2000);
        
        // Run 10 pairs using these openings
        // Each opening will be used cyclically
        runner.runPairs(10, openings);
    }
    
    /**
     * Run a match using FEN positions from a file.
     */
    private static void runWithFENFile(String enginePath, String fenFile) throws Exception {
        System.out.println("=== Running match with FEN file: " + fenFile + " ===\n");
        
        // Load FEN positions from file
        List<String> fens = FENLoader.loadFromFile(fenFile);
        
        if (fens.isEmpty()) {
            System.err.println("No valid FEN positions found in " + fenFile);
            return;
        }
        
        System.out.println("Loaded " + fens.size() + " FEN positions:");
        for (int i = 0; i < fens.size() && i < 5; i++) {
            System.out.println("  " + (i+1) + ". " + fens.get(i).substring(0, Math.min(50, fens.get(i).length())) + "...");
        }
        System.out.println();
        
        // Validate all FENs
        int validCount = 0;
        for (String fen : fens) {
            if (FENLoader.isValidFEN(fen)) {
                validCount++;
            } else {
                System.err.println("Warning: Invalid FEN: " + fen);
            }
        }
        System.out.println("Valid FENs: " + validCount + "/" + fens.size() + "\n");
        
        // Create runner with:
        // - 4 concurrent games
        // - 5 minutes + 3 seconds
        MatchRunner runner = new MatchRunner(enginePath, 4, 300000, 3000);
        
        // Run pairs using these positions
        runner.runPairs(fens.size(), fens);
    }
    
    /**
     * Example: Test a specific critical position multiple times.
     */
    @SuppressWarnings("unused")
    private static void runTestPosition(String enginePath) throws Exception {
        System.out.println("=== Testing specific position ===\n");
        
        // Example: Test a sharp Sicilian position
        String testPosition = "rnbqkb1r/pp2pppp/3p1n2/2p5/4P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 0 4";
        
        System.out.println("Testing position: " + testPosition + "\n");
        
        // Create list with this position repeated
        List<String> positions = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            positions.add(testPosition);
        }
        
        // Run 20 pairs from this position (40 games total)
        MatchRunner runner = new MatchRunner(enginePath, 4, 180000, 2000);
        runner.runPairs(20, positions);
    }
}
