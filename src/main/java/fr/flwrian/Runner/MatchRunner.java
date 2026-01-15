package fr.flwrian.Runner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import fr.flwrian.Engine.Engine;
import fr.flwrian.Game.MatchPair;
import fr.flwrian.Game.TimeControl;
import fr.flwrian.Result.GameResult;
import fr.flwrian.Result.PairResult;
import fr.flwrian.Stats.StatsManager;
import fr.flwrian.Task.PairTask;
import fr.flwrian.WebSocket.GameWebSocket;
import fr.flwrian.WebSocket.WebSocketServer;

/**
 * Manages a match between engines with proper time control and pair management.
 * Supports both single games and paired games (with color swapping).
 */
public class MatchRunner {
    private final ExecutorService pool;
    private final List<EngineInstance> engineInstances = new ArrayList<>();
    private final TimeControl baseTimeControl;  // Legacy: for single time control
    private final List<TimeControl> timeControls;  // New: for multiple time controls
    private final WebSocketServer wsServer;
    private StatsManager statsManager;
    
    /**
     * Set the stats manager for leaderboard tracking
     */
    public void setStatsManager(StatsManager statsManager) {
        this.statsManager = statsManager;
        if (this.statsManager != null) {
            GameWebSocket.setStatsManager(statsManager);
        }
    }

    /**
     * Create a match runner with multiple engines and multiple time controls.
     * @param enginePaths List of paths to engine executables
     * @param concurrency Number of concurrent games
     * @param timeControls List of time controls (one will be randomly selected per pair)
     * @param wsPort WebSocket server port (0 to disable)
     */
    public MatchRunner(List<String> enginePaths, int concurrency, List<fr.flwrian.Config.Config.TimeControl> configTimeControls, int wsPort) throws Exception {
        this.pool = Executors.newFixedThreadPool(concurrency);
        
        // Convert Config.TimeControl to Game.TimeControl
        this.timeControls = new ArrayList<>();
        for (fr.flwrian.Config.Config.TimeControl tc : configTimeControls) {
            this.timeControls.add(new TimeControl(tc.getBaseTimeMs(), tc.getIncrementMs()));
        }
        
        // Set baseTimeControl to first one for backward compatibility
        this.baseTimeControl = this.timeControls.get(0);

        // Start WebSocket server if port specified
        if (wsPort > 0) {
            this.wsServer = new WebSocketServer(wsPort);
            this.wsServer.start();
        } else {
            this.wsServer = null;
        }

        // Create engine pool: concurrency instances of each engine
        for (String enginePath : enginePaths) {
            // Extract engine name from path
            String[] pathParts = enginePath.replace("\\", "/").split("/");
            String engineName = pathParts[pathParts.length - 1];
            
            System.out.println("\n[MatchRunner] Création des instances pour: " + engineName);
            System.out.println("   Chemin: " + enginePath);
            System.out.println("   Instances: " + concurrency);
            
            for (int i = 0; i < concurrency; i++) {
                System.out.println("\n[MatchRunner] Création instance " + (i + 1) + "/" + concurrency + " de " + engineName);
                try {
                    Engine engine = new Engine(enginePath);
                    engineInstances.add(new EngineInstance(engine, engineName));
                    System.out.println("[MatchRunner] Instance " + (i + 1) + " créée avec succès");
                } catch (Exception e) {
                    System.err.println("[MatchRunner] Échec création instance " + (i + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
            System.out.println("[MatchRunner] Toutes les instances de " + engineName + " créées");
        }
    }

    /**
     * Create a match runner with multiple engines for round-robin tournament.
     * @param enginePaths List of paths to engine executables
     * @param concurrency Number of concurrent games
     * @param baseTimeMs Base time in milliseconds (e.g., 60000 for 1 minute)
     * @param incrementMs Increment per move in milliseconds (e.g., 1000 for 1 second)
     * @param wsPort WebSocket server port (0 to disable)
     */
    public MatchRunner(List<String> enginePaths, int concurrency, long baseTimeMs, long incrementMs, int wsPort) throws Exception {
        this.pool = Executors.newFixedThreadPool(concurrency);
        this.baseTimeControl = new TimeControl(baseTimeMs, incrementMs);
        this.timeControls = java.util.Arrays.asList(this.baseTimeControl);  // Single time control

        // Start WebSocket server if port specified
        if (wsPort > 0) {
            this.wsServer = new WebSocketServer(wsPort);
            this.wsServer.start();
        } else {
            this.wsServer = null;
        }

        // Create engine pool: concurrency instances of each engine
        for (String enginePath : enginePaths) {
            // Extract engine name from path
            String[] pathParts = enginePath.replace("\\", "/").split("/");
            String engineName = pathParts[pathParts.length - 1];
            
            System.out.println("\n[MatchRunner] Création des instances pour: " + engineName);
            System.out.println("   Chemin: " + enginePath);
            System.out.println("   Instances: " + concurrency);
            
            for (int i = 0; i < concurrency; i++) {
                System.out.println("\n[MatchRunner] Création instance " + (i + 1) + "/" + concurrency + " de " + engineName);
                try {
                    Engine engine = new Engine(enginePath);
                    engineInstances.add(new EngineInstance(engine, engineName));
                    System.out.println("[MatchRunner] Instance " + (i + 1) + " créée avec succès");
                } catch (Exception e) {
                    System.err.println("[MatchRunner] Échec création instance " + (i + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
            System.out.println("[MatchRunner] Toutes les instances de " + engineName + " créées");
        }
    }
    
    /**
     * Create a match runner with a single engine (self-play).
     * @param enginePath Path to the engine executable
     * @param concurrency Number of concurrent games
     * @param baseTimeMs Base time in milliseconds
     * @param incrementMs Increment per move in milliseconds
     * @param wsPort WebSocket server port (0 to disable)
     */
    public MatchRunner(String enginePath, int concurrency, long baseTimeMs, long incrementMs, int wsPort) throws Exception {
        this(java.util.Arrays.asList(enginePath), concurrency, baseTimeMs, incrementMs, wsPort);
    }
    
    /**
     * Create a match runner with default WebSocket port 8080.
     */
    public MatchRunner(String enginePath, int concurrency, long baseTimeMs, long incrementMs) throws Exception {
        this(enginePath, concurrency, baseTimeMs, incrementMs, 8080);
    }
    
    /**
     * Create a match runner for multiple engines with default WebSocket port 8080.
     */
    public MatchRunner(List<String> enginePaths, int concurrency, long baseTimeMs, long incrementMs) throws Exception {
        this(enginePaths, concurrency, baseTimeMs, incrementMs, 8080);
    }
    /**
     * Run paired games (each pair consists of 2 games with swapped colors).
     * @param totalPairs Number of pairs to run (will result in totalPairs * 2 games)
     */
    public void runPairs(int totalPairs) throws Exception {
        runPairs(totalPairs, new ArrayList<>(), "sequential");
    }

    /**
     * Run paired games with custom starting positions.
     * @param totalPairs Number of pairs to run
     * @param startFens List of FEN positions to use (cycles through them)
     */
    public void runPairs(int totalPairs, List<String> startFens) throws Exception {
        runPairs(totalPairs, startFens, "sequential");
    }

    /**
     * Run paired games with custom starting positions and mode.
     * @param totalPairs Number of pairs to run
     * @param startFens List of FEN positions to use
     * @param mode "sequential" to cycle through positions, "random" to pick randomly
     */
    public void runPairs(int totalPairs, List<String> startFens, String mode) throws Exception {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("   TOURNAMENT START");
        System.out.println("════════════════════════════════════════\n");
        
        // Display time control info
        if (timeControls.size() == 1) {
            System.out.println(" Time Control: " + baseTimeControl);
        } else {
            System.out.println(" Random Time Controls (" + timeControls.size() + " variants):");
            for (TimeControl tc : timeControls) {
                long baseMs = tc.getWhiteTime();
                long incMs = tc.getWhiteIncrement();
                String formatted = (baseMs < 60000) ? (baseMs/1000.0 + "s") : 
                                 ((baseMs/60000) + ":" + String.format("%02d", (baseMs%60000)/1000));
                formatted += " + " + (incMs/1000.0) + "s";
                System.out.println("   • " + formatted);
            }
        }
        
        if (!startFens.isEmpty()) {
            System.out.println("Opening Book: " + startFens.size() + " positions (mode: " + mode + ")");
        }
        
        System.out.println("Target: " + totalPairs + " pairs (" + (totalPairs * 2) + " games)");
        System.out.println();

        // Calculate maximum expected time per pair using the longest time control
        TimeControl longestTC = timeControls.stream()
            .max((tc1, tc2) -> Long.compare(
                tc1.getWhiteTime() + tc1.getWhiteIncrement() * 60,
                tc2.getWhiteTime() + tc2.getWhiteIncrement() * 60
            ))
            .orElse(baseTimeControl);
        
        // 2 games × (base + 60 moves × increment) × 2 players, with 50% safety margin
        long maxTimePerPair = (long) ((2 * (longestTC.getWhiteTime() + 60 * longestTC.getWhiteIncrement()) * 2) * 1.5);
        long timeoutSeconds = maxTimePerPair / 1000;

        // Track which engine instances are available
        java.util.Queue<EngineInstance> availableEngines = new java.util.LinkedList<>(engineInstances);
        
        // Random number generator for selecting engines
        java.util.Random random = new java.util.Random();
        
        // Track futures with their engine pairs
        java.util.Map<java.util.concurrent.Future<PairResult>, EngineInstance[]> activePairs = new java.util.HashMap<>();
        java.util.Map<java.util.concurrent.Future<PairResult>, String> pairTimeControls = new java.util.HashMap<>();
        
        int pairsSubmitted = 0;
        int pairsCompleted = 0;
        
        // Submit initial batch of pairs (up to concurrency / 2)
        while (pairsSubmitted < totalPairs && availableEngines.size() >= 2) {
            // Pick 2 random DIFFERENT engines from available pool
            EngineInstance[] selectedPair = selectTwoDifferentEngines(availableEngines, random);
            if (selectedPair == null) {
                System.err.println("Cannot find 2 different engines in pool, waiting...");
                Thread.sleep(100);
                continue;
            }
            
            EngineInstance engine1 = selectedPair[0];
            EngineInstance engine2 = selectedPair[1];
            availableEngines.remove(engine1);
            availableEngines.remove(engine2);
            
            String fen = selectStartingPosition(startFens, pairsSubmitted, mode, random);
            TimeControl selectedTC = selectRandomTimeControl(random);
            MatchPair pair = new MatchPair(fen, pairsSubmitted);
            
            java.util.concurrent.Future<PairResult> future = pool.submit(
                new PairTask(pair, 
                    engine1.getEngine(), engine2.getEngine(), 
                    selectedTC, 
                    engine1.getName(), engine2.getName())
            );
            activePairs.put(future, new EngineInstance[]{engine1, engine2});
            pairTimeControls.put(future, formatTimeControl(selectedTC));
            pairsSubmitted++;
        }

        // Track scores per engine name
        java.util.Map<String, Double> engineScores = new java.util.HashMap<>();
        for (EngineInstance ei : engineInstances) {
            engineScores.put(ei.getName(), 0.0);
        }
        int totalGames = 0;

        // Wait for pairs to complete and submit new ones
        while (pairsCompleted < totalPairs) {
            // Poll all active futures to find completed ones
            java.util.concurrent.Future<PairResult> completedFuture = null;
            
            for (java.util.concurrent.Future<PairResult> future : activePairs.keySet()) {
                if (future.isDone()) {
                    completedFuture = future;
                    break;
                }
            }
            
            if (completedFuture == null) {
                // No completed pair yet, wait a bit
                Thread.sleep(100);
                continue;
            }
            
            try {
                PairResult pr = completedFuture.get(timeoutSeconds, TimeUnit.SECONDS);
                System.out.println(pr);
                
                // Update total games and scores
                totalGames += pr.getGameResults().size();
                
                // Update engine scores
                engineScores.put(pr.getEngine1Name(), 
                    engineScores.getOrDefault(pr.getEngine1Name(), 0.0) + pr.getEngine1Score());
                engineScores.put(pr.getEngine2Name(), 
                    engineScores.getOrDefault(pr.getEngine2Name(), 0.0) + pr.getEngine2Score());
                
                // Get engine info and time control for this pair
                EngineInstance[] engines = activePairs.get(completedFuture);
                String timeControl = pairTimeControls.get(completedFuture);
                
                // Record stats if available
                if (statsManager != null && engines != null && engines.length == 2) {
                    String engine1Name = engines[0].getName();
                    String engine2Name = engines[1].getName();
                    
                    // Use "Unknown" if timeControl is null (shouldn't happen but defensive)
                    String tc = (timeControl != null) ? timeControl : "Unknown";
                    
                    // Game 1: engine1 is white, engine2 is black
                    if (pr.getGameResults().size() > 0) {
                        GameResult gr1 = pr.getGameResults().get(0);
                        statsManager.recordGame(gr1, engine1Name, engine2Name, tc);
                    }
                    
                    // Game 2: engine2 is white, engine1 is black
                    if (pr.getGameResults().size() > 1) {
                        GameResult gr2 = pr.getGameResults().get(1);
                        statsManager.recordGame(gr2, engine2Name, engine1Name, tc);
                    }
                    
                    // Broadcast updated leaderboard
                    GameWebSocket.broadcastLeaderboard();
                }
                
                // Print pair result with progress
                System.out.println("┌" + "─".repeat(50) + "┐");
                System.out.println("│ Pair " + pr.getPairId() + " Complete [" + pairsCompleted + "/" + totalPairs + "]" + " ".repeat(50 - 30 - String.valueOf(pr.getPairId()).length() - String.valueOf(pairsCompleted).length() - String.valueOf(totalPairs).length()) + "│");
                System.out.println("├" + "─".repeat(50) + "┤");
                
                // Print matchup
                String matchupLine = "│ " + pr.getEngine1Name() + " vs " + pr.getEngine2Name();
                int matchupPadding = 50 - matchupLine.length() + 1;
                if (matchupPadding > 0) {
                    matchupLine += " ".repeat(matchupPadding) + "│";
                } else {
                    matchupLine = "│ " + pr.getEngine1Name() + " vs " + pr.getEngine2Name() + "│";
                }
                System.out.println(matchupLine);
                System.out.println("├" + "─".repeat(50) + "┤");
                
                // Print individual game details
                for (GameResult gr : pr.getGameResults()) {
                    String resultIcon = gr.getResult().equals("1-0") ? "⚪" : 
                                      gr.getResult().equals("0-1") ? "⚫" : "🤝";
                    System.out.println("│ " + resultIcon + " Game " + gr.getId() + ": " + gr.getResult() + " (" + gr.getReason() + ")" + " ".repeat(50 - 17 - String.valueOf(gr.getId()).length() - gr.getResult().length() - gr.getReason().length()) + "│");
                }
                
                // Print score with engine names
                String scoreLine = "│ Score: " + pr.getEngine1Name() + " " + String.format("%.1f", pr.getEngine1Score()) + " - " + String.format("%.1f", pr.getEngine2Score()) + " " + pr.getEngine2Name();
                int scorePadding = 50 - scoreLine.length() + 1;
                if (scorePadding > 0) {
                    scoreLine += " ".repeat(scorePadding) + "│";
                } else {
                    // Truncate names if too long
                    scoreLine = "│ " + String.format("%.1f", pr.getEngine1Score()) + " - " + String.format("%.1f", pr.getEngine2Score()) + " ".repeat(50 - 11 - String.format("%.1f", pr.getEngine1Score()).length() - String.format("%.1f", pr.getEngine2Score()).length()) + "│";
                }
                System.out.println(scoreLine);
                System.out.println("└" + "─".repeat(50) + "┘");
                System.out.println();
                
                pairsCompleted++;
                
                // Get the engine pair that just finished
                EngineInstance[] freedEnginePair = activePairs.remove(completedFuture);
                
                // Submit next pair if available
                if (pairsSubmitted < totalPairs) {
                    // Reset engines before reusing them
                    freedEnginePair[0].getEngine().reset();
                    freedEnginePair[1].getEngine().reset();
                    
                    // Put freed engines back in pool
                    availableEngines.add(freedEnginePair[0]);
                    availableEngines.add(freedEnginePair[1]);
                    
                    // Pick 2 random DIFFERENT engines from available pool
                    EngineInstance[] selectedPair = selectTwoDifferentEngines(availableEngines, random);
                    if (selectedPair == null) {
                        System.err.println(" Cannot find 2 different engines in pool, waiting...");
                        availableEngines.remove(freedEnginePair[0]);
                        availableEngines.remove(freedEnginePair[1]);
                        continue;
                    }
                    
                    EngineInstance engine1 = selectedPair[0];
                    EngineInstance engine2 = selectedPair[1];
                    availableEngines.remove(engine1);
                    availableEngines.remove(engine2);
                    
                    String fen = selectStartingPosition(startFens, pairsSubmitted, mode, random);
                    TimeControl selectedTC = selectRandomTimeControl(random);
                    MatchPair pair = new MatchPair(fen, pairsSubmitted);
                    
                    java.util.concurrent.Future<PairResult> future = pool.submit(
                        new PairTask(pair, 
                            engine1.getEngine(), engine2.getEngine(), 
                            selectedTC, 
                            engine1.getName(), engine2.getName())
                    );
                    activePairs.put(future, new EngineInstance[]{engine1, engine2});
                    pairTimeControls.put(future, formatTimeControl(selectedTC));
                    pairsSubmitted++;
                } else {
                    // No more pairs to submit, put engines back in pool for cleanup
                    availableEngines.add(freedEnginePair[0]);
                    availableEngines.add(freedEnginePair[1]);
                }
                
            } catch (java.util.concurrent.TimeoutException e) {
                System.err.println("Pair " + pairsCompleted + " timed out after " + timeoutSeconds + " seconds - skipping");
                
                // Remove the timed-out future from active pairs
                activePairs.remove(completedFuture);
                pairsCompleted++;
                
                // Task is still running but we move on. The thread will be forcefully stopped during shutdown.
                // Don't reuse these engines as they might still be in use
                
                // Try to submit next pair with fresh engines if available
                if (pairsSubmitted < totalPairs && availableEngines.size() >= 2) {
                    EngineInstance[] selectedPair = selectTwoDifferentEngines(availableEngines, random);
                    if (selectedPair != null) {
                        EngineInstance engine1 = selectedPair[0];
                        EngineInstance engine2 = selectedPair[1];
                        availableEngines.remove(engine1);
                        availableEngines.remove(engine2);
                        
                        String fen = selectStartingPosition(startFens, pairsSubmitted, mode, random);
                        TimeControl selectedTC = selectRandomTimeControl(random);
                        MatchPair pair = new MatchPair(fen, pairsSubmitted);
                        
                        java.util.concurrent.Future<PairResult> future = pool.submit(
                            new PairTask(pair, 
                                engine1.getEngine(), engine2.getEngine(), 
                                selectedTC, 
                                engine1.getName(), engine2.getName())
                        );
                        activePairs.put(future, new EngineInstance[]{engine1, engine2});
                        pairsSubmitted++;
                    }
                }
            }
        }

        System.out.println("\n" + "═".repeat(60));
        System.out.println("TOURNAMENT COMPLETE");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("Final Scores:");
        
        // Sort engines by score (descending)
        java.util.List<java.util.Map.Entry<String, Double>> sortedScores = new java.util.ArrayList<>(engineScores.entrySet());
        sortedScores.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        for (java.util.Map.Entry<String, Double> entry : sortedScores) {
            String engineName = entry.getKey();
            double score = entry.getValue();
            double percentage = totalGames > 0 ? (score / totalGames) * 100 : 0.0;
            System.out.println("   " + engineName + ": " + String.format("%.1f", score) + " points (" + String.format("%.1f%%", percentage) + ")");
        }
        
        System.out.println();
        System.out.println("📈 Statistics:");
        System.out.println("   Total games: " + totalGames);
        System.out.println();
        
        // Determine winner
        if (sortedScores.size() >= 2) {
            double topScore = sortedScores.get(0).getValue();
            double secondScore = sortedScores.get(1).getValue();
            if (topScore > secondScore) {
                double diff = topScore - secondScore;
                System.out.println(" Winner: " + sortedScores.get(0).getKey() + " (+" + String.format("%.1f", diff) + ")");
            } else {
                System.out.println("🤝 Draw!");
            }
        } else if (sortedScores.size() == 1) {
            System.out.println(" Winner: " + sortedScores.get(0).getKey());
        }
        System.out.println("═".repeat(60) + "\n");

        shutdown();
    }
    
    /**
     * Select 2 different engines randomly from the available pool.
     * Ensures the two engines have different names (no self-play).
     * 
     * @param availableEngines Queue of available engine instances
     * @param random Random number generator
     * @return Array of 2 different engines, or null if not possible
     */
    private EngineInstance[] selectTwoDifferentEngines(java.util.Queue<EngineInstance> availableEngines, java.util.Random random) {
        if (availableEngines.size() < 2) {
            return null;
        }
        
        EngineInstance[] availableArray = availableEngines.toArray(new EngineInstance[0]);
        
        // Try to find 2 engines with different names
        // First, pick a random engine
        EngineInstance engine1 = availableArray[random.nextInt(availableArray.length)];
        
        // Then, try to find another engine with a different name
        java.util.List<EngineInstance> differentEngines = new java.util.ArrayList<>();
        for (EngineInstance e : availableArray) {
            if (!e.getName().equals(engine1.getName())) {
                differentEngines.add(e);
            }
        }
        
        if (differentEngines.isEmpty()) {
            // All available engines have the same name, cannot pair
            return null;
        }
        
        // Pick a random engine from those with different names
        EngineInstance engine2 = differentEngines.get(random.nextInt(differentEngines.size()));
        
        return new EngineInstance[]{engine1, engine2};
    }

    /**
     * Select a random time control from the available time controls.
     * @param random Random generator
     * @return TimeControl to use for this pair
     */
    private TimeControl selectRandomTimeControl(java.util.Random random) {
        if (timeControls.size() == 1) {
            return timeControls.get(0);
        }
        return timeControls.get(random.nextInt(timeControls.size()));
    }

    /**
     * Format time control as human-readable string for leaderboard (e.g., "1:00 + 1.0s").
     */
    private String formatTimeControl(TimeControl tc) {
        long baseMs = tc.getWhiteTime();  // Use white time as base (they're equal at start)
        long incMs = tc.getWhiteIncrement();
        
        // Format base time
        String baseStr;
        if (baseMs < 60000) {
            // Less than a minute: show as seconds
            baseStr = String.format("%.1fs", baseMs / 1000.0);
        } else {
            // More than a minute: show as minutes:seconds
            long minutes = baseMs / 60000;
            long seconds = (baseMs % 60000) / 1000;
            if (seconds > 0) {
                baseStr = String.format("%d:%02d", minutes, seconds);
            } else {
                baseStr = minutes + ":00";
            }
        }
        
        // Format increment
        String incStr = String.format("%.1fs", incMs / 1000.0);
        
        return baseStr + " + " + incStr;
    }

    /**
     * Select starting position based on mode.
     * @param startFens List of FEN positions
     * @param pairIndex Current pair index
     * @param mode "sequential" or "random"
     * @param random Random generator
     * @return FEN position to use, or "startpos" if no custom positions
     */
    private String selectStartingPosition(List<String> startFens, int pairIndex, String mode, java.util.Random random) {
        if (startFens.isEmpty()) {
            return "startpos";
        }
        
        if ("random".equalsIgnoreCase(mode)) {
            return startFens.get(random.nextInt(startFens.size()));
        } else {
            // Sequential mode (default)
            return startFens.get(pairIndex % startFens.size());
        }
    }

    private void shutdown() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        
        // Stop WebSocket server
        if (wsServer != null) {
            try {
                wsServer.stop();
            } catch (Exception e) {
                System.err.println("Error stopping WebSocket server: " + e.getMessage());
            }
        }
        
        System.out.println("\nAll games completed. Shutting down.");
    }
    
    /**
     * Force immediate shutdown (for Ctrl+C handling).
     * Stops all running games and closes connections gracefully.
     */
    public void forceShutdown() throws Exception {
        System.out.println("Closing WebSocket connections...");
        
        // Stop WebSocket server first (graceful close for browsers)
        if (wsServer != null) {
            wsServer.stop();
        }
        
        System.out.println("Stopping engine processes...");
        
        // Force shutdown thread pool
        pool.shutdownNow();
        if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("Some tasks did not terminate in time");
        }
        
        System.out.println("🧹 Cleanup complete");
    }
}
