package fr.flwrian.Runner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.flwrian.Config.Config;
import fr.flwrian.Game.MatchPair;
import fr.flwrian.Game.TimeControl;
import fr.flwrian.Result.GameResult;
import fr.flwrian.Result.PairResult;
import fr.flwrian.Stats.StatsManager;
import fr.flwrian.WebSocket.GameWebSocket;
import fr.flwrian.WebSocket.WebSocketServer;

/**
 * Manages a match between engines with proper time control and pair management.
 * Supports both single games and paired games (with color swapping).
 * Engines are created on-demand when needed and closed after each game to save resources.
 */
public class MatchRunner {
    private final ExecutorService pool;
    private final List<String> enginePaths = new ArrayList<>();
    private final List<String> engineNames = new ArrayList<>();
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
     * @param enginePathsList List of paths to engine executables
     * @param concurrency Number of concurrent games
     * @param configTimeControls List of time controls (one will be randomly selected per pair)
     * @param wsPort WebSocket server port (0 to disable)
     * @param config Full configuration (for SSL settings)
     */
    public MatchRunner(List<String> enginePathsList, int concurrency, 
                      List<fr.flwrian.Config.Config.TimeControl> configTimeControls, 
                      int wsPort, 
                      fr.flwrian.Config.Config config) throws Exception {
        this.pool = Executors.newFixedThreadPool(concurrency);
        
        // Convert Config.TimeControl to Game.TimeControl
        this.timeControls = new ArrayList<>();
        for (fr.flwrian.Config.Config.TimeControl tc : configTimeControls) {
            this.timeControls.add(new TimeControl(tc.getBaseTimeMs(), tc.getIncrementMs()));
        }
        
        // Set baseTimeControl to first one for backward compatibility
        this.baseTimeControl = this.timeControls.get(0);

        // Start WebSocket server with SSL support if configured
        if (wsPort > 0) {
            fr.flwrian.Config.Config.Ssl sslConfig = config.getServer().getSsl();
            if (sslConfig != null && sslConfig.isEnabled()) {
                this.wsServer = new WebSocketServer(
                    wsPort, 
                    true, 
                    sslConfig.getPort(),
                    sslConfig.getKeyStorePath(),
                    sslConfig.getKeyStorePassword(),
                    sslConfig.getKeyStoreType()
                );
            } else {
                this.wsServer = new WebSocketServer(wsPort);
            }
            this.wsServer.start();
        } else {
            this.wsServer = null;
        }

        // Store engine paths and names for on-demand creation
        for (String enginePath : enginePathsList) {
            // Extract engine name from path
            String[] pathParts = enginePath.replace("\\", "/").split("/");
            String engineName = pathParts[pathParts.length - 1];
            
            this.enginePaths.add(enginePath);
            this.engineNames.add(engineName);
            
            System.out.println("[MatchRunner] Registered engine: " + engineName + " (" + enginePath + ")");
        }
        
        System.out.println("[MatchRunner] Engines will be created on-demand (concurrency: " + concurrency + ")");
    }

    /**
     * Create a match runner with multiple engines for round-robin tournament.
     * @param enginePathsList List of paths to engine executables
     * @param concurrency Number of concurrent games
     * @param baseTimeMs Base time in milliseconds (e.g., 60000 for 1 minute)
     * @param incrementMs Increment per move in milliseconds (e.g., 1000 for 1 second)
     * @param wsPort WebSocket server port (0 to disable)
     * @param config Full configuration (for SSL settings)
     */
    public MatchRunner(List<String> enginePathsList, int concurrency, long baseTimeMs, long incrementMs, int wsPort, Config config) throws Exception {
        this.pool = Executors.newFixedThreadPool(concurrency);
        this.baseTimeControl = new TimeControl(baseTimeMs, incrementMs);
        this.timeControls = Arrays.asList(this.baseTimeControl);  // Single time control

        // Start WebSocket server if port specified
        if (wsPort > 0) {
            Config.Ssl sslConfig = config.getServer().getSsl();
            if (sslConfig != null && sslConfig.isEnabled()) {
                // SSL mode: WSS + HTTPS
                this.wsServer = new WebSocketServer(
                    wsPort, 
                    true, 
                    sslConfig.getPort(), 
                    sslConfig.getKeyStorePath(), 
                    sslConfig.getKeyStorePassword(), 
                    sslConfig.getKeyStoreType()
                );
            } else {
                // Standard HTTP WebSocket
                this.wsServer = new WebSocketServer(wsPort);
            }
            this.wsServer.start();
        } else {
            this.wsServer = null;
        }

        // Store engine paths and names for on-demand creation
        for (String enginePath : enginePathsList) {
            // Extract engine name from path
            String[] pathParts = enginePath.replace("\\", "/").split("/");
            String engineName = pathParts[pathParts.length - 1];
            
            this.enginePaths.add(enginePath);
            this.engineNames.add(engineName);
            
            System.out.println("[MatchRunner] Registered engine: " + engineName + " (" + enginePath + ")");
        }
        
        System.out.println("[MatchRunner] Engines will be created on-demand (concurrency: " + concurrency + ")");
    }
    
    /**
     * Create a match runner with a single engine (self-play).
     * @param enginePath Path to the engine executable
     * @param concurrency Number of concurrent games
     * @param baseTimeMs Base time in milliseconds
     * @param incrementMs Increment per move in milliseconds
     * @param wsPort WebSocket server port (0 to disable)
     * @param config Full configuration (for SSL settings)
     */
    public MatchRunner(String enginePath, int concurrency, long baseTimeMs, long incrementMs, int wsPort, Config config) throws Exception {
        this(Arrays.asList(enginePath), concurrency, baseTimeMs, incrementMs, wsPort, config);
    }
    
    /**
     * Create a match runner with default WebSocket port 8080.
     * @param config Full configuration (for SSL settings)
     */
    public MatchRunner(String enginePath, int concurrency, long baseTimeMs, long incrementMs, Config config) throws Exception {
        this(enginePath, concurrency, baseTimeMs, incrementMs, 8080, config);
    }
    
    /**
     * Create a match runner for multiple engines with default WebSocket port 8080.
     * @param config Full configuration (for SSL settings)
     */
    public MatchRunner(List<String> enginePaths, int concurrency, long baseTimeMs, long incrementMs, Config config) throws Exception {
        this(enginePaths, concurrency, baseTimeMs, incrementMs, 8080, config);
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

        // Random number generator for selecting engines
        Random random = new Random();
        
        // Track futures with their engine info
        Map<Future<PairResult>, String[]> activePairs = new HashMap<>();
        Map<Future<PairResult>, String> pairTimeControls = new HashMap<>();
        
        int pairsSubmitted = 0;
        int pairsCompleted = 0;
        
        // Calculate concurrency for pairs (each pair task runs 2 games sequentially)
        int concurrency = ((ThreadPoolExecutor) pool).getCorePoolSize();
        int maxConcurrentPairs = concurrency;
        
        // Submit initial batch of pairs (up to max concurrent pairs)
        while (pairsSubmitted < totalPairs && pairsSubmitted < maxConcurrentPairs) {
            // Pick 2 random DIFFERENT engines
            int[] selectedIndices = selectTwoDifferentEngineIndices(random);
            if (selectedIndices == null) {
                System.err.println("Cannot select 2 different engines (need at least 2 engines)");
                throw new Exception("Tournament requires at least 2 different engines");
            }
            
            int idx1 = selectedIndices[0];
            int idx2 = selectedIndices[1];
            String enginePath1 = enginePaths.get(idx1);
            String enginePath2 = enginePaths.get(idx2);
            String engineName1 = engineNames.get(idx1);
            String engineName2 = engineNames.get(idx2);
            
            String fen = selectStartingPosition(startFens, pairsSubmitted, mode, random);
            TimeControl selectedTC = selectRandomTimeControl(random);
            MatchPair pair = new MatchPair(fen, pairsSubmitted);
            
            Future<PairResult> future = pool.submit(
                new fr.flwrian.Task.OnDemandPairTask(pair, 
                    enginePath1, enginePath2, 
                    selectedTC, 
                    engineName1, engineName2)
            );
            activePairs.put(future, new String[]{engineName1, engineName2});
            pairTimeControls.put(future, formatTimeControl(selectedTC));
            pairsSubmitted++;
        }

        // Track scores per engine name
        Map<String, Double> engineScores = new HashMap<>();
        for (String engineName : engineNames) {
            engineScores.put(engineName, 0.0);
        }
        int totalGames = 0;

        // Wait for pairs to complete and submit new ones
        while (pairsCompleted < totalPairs) {
            // Poll all active futures to find completed ones
            Future<PairResult> completedFuture = null;
            
            for (Future<PairResult> future : activePairs.keySet()) {
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
                
                // Get engine names and time control for this pair
                String[] engineNamesForPair = activePairs.get(completedFuture);
                String timeControl = pairTimeControls.get(completedFuture);
                
                // Record stats if available
                if (statsManager != null && engineNamesForPair != null && engineNamesForPair.length == 2) {
                    String engine1Name = engineNamesForPair[0];
                    String engine2Name = engineNamesForPair[1];
                    
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
                System.out.println("│ Pair " + pr.getPairId() + " Complete [" + (pairsCompleted + 1) + "/" + totalPairs + "]" + " ".repeat(50 - 30 - String.valueOf(pr.getPairId()).length() - String.valueOf(pairsCompleted + 1).length() - String.valueOf(totalPairs).length()) + "│");
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
                
                // Remove completed pair
                activePairs.remove(completedFuture);
                pairTimeControls.remove(completedFuture);
                
                // Submit next pair if available and under concurrency limit
                if (pairsSubmitted < totalPairs) {
                    // Pick 2 random DIFFERENT engines
                    int[] selectedIndices = selectTwoDifferentEngineIndices(random);
                    if (selectedIndices != null) {
                        int idx1 = selectedIndices[0];
                        int idx2 = selectedIndices[1];
                        String enginePath1 = enginePaths.get(idx1);
                        String enginePath2 = enginePaths.get(idx2);
                        String engineName1 = this.engineNames.get(idx1);
                        String engineName2 = this.engineNames.get(idx2);
                        
                        String fen = selectStartingPosition(startFens, pairsSubmitted, mode, random);
                        TimeControl selectedTC = selectRandomTimeControl(random);
                        MatchPair pair = new MatchPair(fen, pairsSubmitted);
                        
                        Future<PairResult> future = pool.submit(
                            new fr.flwrian.Task.OnDemandPairTask(pair, 
                                enginePath1, enginePath2, 
                                selectedTC, 
                                engineName1, engineName2)
                        );
                        activePairs.put(future, new String[]{engineName1, engineName2});
                        pairTimeControls.put(future, formatTimeControl(selectedTC));
                        pairsSubmitted++;
                    }
                }
                
            } catch (TimeoutException e) {
                System.err.println("Pair timed out after " + timeoutSeconds + " seconds - skipping");
                
                // Remove the timed-out future from active pairs
                activePairs.remove(completedFuture);
                pairTimeControls.remove(completedFuture);
                pairsCompleted++;
                
                // Submit next pair if available
                if (pairsSubmitted < totalPairs) {
                    int[] selectedIndices = selectTwoDifferentEngineIndices(random);
                    if (selectedIndices != null) {
                        int idx1 = selectedIndices[0];
                        int idx2 = selectedIndices[1];
                        String enginePath1 = enginePaths.get(idx1);
                        String enginePath2 = enginePaths.get(idx2);
                        String engineName1 = this.engineNames.get(idx1);
                        String engineName2 = this.engineNames.get(idx2);
                        
                        String fen = selectStartingPosition(startFens, pairsSubmitted, mode, random);
                        TimeControl selectedTC = selectRandomTimeControl(random);
                        MatchPair pair = new MatchPair(fen, pairsSubmitted);
                        
                        Future<PairResult> future = pool.submit(
                            new fr.flwrian.Task.OnDemandPairTask(pair, 
                                enginePath1, enginePath2, 
                                selectedTC, 
                                engineName1, engineName2)
                        );
                        activePairs.put(future, new String[]{engineName1, engineName2});
                        pairTimeControls.put(future, formatTimeControl(selectedTC));
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
        List<Map.Entry<String, Double>> sortedScores = new ArrayList<>(engineScores.entrySet());
        sortedScores.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        for (Map.Entry<String, Double> entry : sortedScores) {
            String engineName = entry.getKey();
            double score = entry.getValue();
            double percentage = totalGames > 0 ? (score / totalGames) * 100 : 0.0;
            System.out.println("   " + engineName + ": " + String.format("%.1f", score) + " points (" + String.format("%.1f%%", percentage) + ")");
        }
        
        System.out.println();
        System.out.println("Statistics:");
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
     * Select 2 random DIFFERENT engine indices from registered engines.
     * @param random Random number generator
     * @return Array of 2 different indices, or null if not enough engines
     */
    private int[] selectTwoDifferentEngineIndices(Random random) {
        if (enginePaths.size() < 2) {
            return null;
        }
        
        int idx1 = random.nextInt(enginePaths.size());
        int idx2;
        
        // Make sure we select a different engine
        do {
            idx2 = random.nextInt(enginePaths.size());
        } while (idx2 == idx1);
        
        return new int[]{idx1, idx2};
    }

    /**
     * Select a random time control from the available time controls.
     * @param random Random generator
     * @return TimeControl to use for this pair
     */
    private TimeControl selectRandomTimeControl(Random random) {
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
    private String selectStartingPosition(List<String> startFens, int pairIndex, String mode, Random random) {
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
