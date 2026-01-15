package fr.flwrian;

import fr.flwrian.Config.Config;
import fr.flwrian.Runner.MatchRunner;
import fr.flwrian.Stats.StatsManager;
import java.util.List;

/**
 * Main entry point using config.yml
 */
public class ConfigMain {
    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "config.yml";
        
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("� EngineLab - Chess Engine Tournament System");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📋 Loading configuration: " + configPath);
        System.out.println();
        
        try {
            // Load config
            Config config = Config.load(configPath);
            config.printSummary();
            
            // Get engine paths
            List<String> enginePaths = config.getEnginePaths();
            System.out.println("Validating " + enginePaths.size() + " engine(s):");
            for (String path : enginePaths) {
                System.out.println("   - " + path);
                java.io.File engineFile = new java.io.File(path);
                System.out.println("     ├─ Exists: " + engineFile.exists());
                System.out.println("     ├─ Size: " + engineFile.length() + " bytes");
                System.out.println("     └─ Executable: " + engineFile.canExecute());
            }
            System.out.println();
            
            // Initialize Stats Manager if configured
            final StatsManager statsManager;
            if (config.getStats() != null) {
                boolean persistenceEnabled = config.getStats().isPersistenceEnabled();
                String statsDir = config.getStats().getStatsDirectory();
                statsManager = new StatsManager(persistenceEnabled, statsDir);
                System.out.println("Stats Manager initialized (persistence: " + (persistenceEnabled ? "enabled" : "disabled") + ")");
                if (persistenceEnabled) {
                    System.out.println("   Stats directory: " + statsDir);
                }
                System.out.println();
            } else {
                statsManager = null;
            }
            
            // Get starting positions
            List<String> startingPositions = config.getStartingPositions();
            
            // Get opening mode
            String openingMode = "sequential"; // default
            if (config.getTournament().getOpenings() != null && 
                config.getTournament().getOpenings().isEnabled()) {
                openingMode = config.getTournament().getOpenings().getMode();
            }
            
            // Get time controls
            List<Config.TimeControl> timeControls = config.getTimeControls();
            
            // Create match runner
            Config.Tournament t = config.getTournament();
            Config.Server s = config.getServer();
            
            int wsPort = s.getWebSocket().isEnabled() ? s.getWebSocket().getPort() : 0;
            
            System.out.println("════════════════════════════════════════════════");
            System.out.println("Initializing MatchRunner");
            System.out.println("════════════════════════════════════════════════");
            System.out.println("Engines: " + enginePaths.size());
            System.out.println("Concurrence: " + t.getConcurrency());
            System.out.println("WebSocket: " + (wsPort > 0 ? "port " + wsPort : "désactivé"));
            System.out.println();
            
            MatchRunner runner = new MatchRunner(
                enginePaths,
                t.getConcurrency(),
                timeControls,
                wsPort
            );
            
            System.out.println("\nMatchRunner initialized successfully!");
            System.out.println("════════════════════════════════════════════════\n");
            
            // Set stats manager if available
            if (statsManager != null) {
                runner.setStatsManager(statsManager);
            }
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n\n🛑 Shutdown signal received (Ctrl+C)...");
                System.out.println("Stopping gracefully...");
                try {
                    // Print final stats summary
                    if (statsManager != null) {
                        System.out.println();
                        statsManager.printSummary();
                    }
                    
                    runner.forceShutdown();
                    System.out.println("Shutdown complete.");
                } catch (Exception e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                }
            }));
            
            // Run tournament
            switch (t.getMode().toLowerCase()) {
                case "pairs":
                case "round-robin":
                    runner.runPairs(t.getPairsPerMatch(), startingPositions, openingMode);
                    break;
                default:
                    System.err.println("Unknown mode: " + t.getMode());
                    System.err.println("Supported modes: pairs, round-robin");
                    System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
