package fr.flwrian.Stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.flwrian.Result.GameResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Statistics manager with optional JSON persistence
 */
public class StatsManager {
    private static final Logger logger = LoggerFactory.getLogger(StatsManager.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final LeaderboardStats stats;
    private final boolean persistenceEnabled;
    private final String statsFilePath;
    
    public StatsManager(boolean persistenceEnabled, String statsDirectory) {
        this.stats = new LeaderboardStats();
        this.persistenceEnabled = persistenceEnabled;
        
        if (persistenceEnabled) {
            // Create directory if necessary
            Path statsDir = Paths.get(statsDirectory);
            try {
                Files.createDirectories(statsDir);
            } catch (IOException e) {
                logger.error("Failed to create stats directory: {}", statsDirectory, e);
            }
            
            this.statsFilePath = statsDirectory + "/leaderboard_stats.json";
            loadStats();
        } else {
            this.statsFilePath = null;
        }
    }
    
    /**
     * Records game result
     */
    public synchronized void recordGame(GameResult result, String whiteEngine, String blackEngine, String timeControl) {
        stats.recordGameResult(whiteEngine, blackEngine, result.getResult(), timeControl);
        
        if (persistenceEnabled) {
            saveStats();
        }
    }
    
    /**
     * Returns current statistics
     */
    public LeaderboardStats getStats() {
        return stats;
    }
    
    /**
     * Saves statistics to JSON file
     */
    private void saveStats() {
        if (!persistenceEnabled || statsFilePath == null) {
            return;
        }
        
        try {
            String json = gson.toJson(stats);
            Files.writeString(Paths.get(statsFilePath), json);
            logger.debug("Stats saved to {}", statsFilePath);
        } catch (IOException e) {
            logger.error("Failed to save stats to {}", statsFilePath, e);
        }
    }
    
    /**
     * Loads statistics from JSON file
     */
    private void loadStats() {
        if (!persistenceEnabled || statsFilePath == null) {
            return;
        }
        
        Path path = Paths.get(statsFilePath);
        if (!Files.exists(path)) {
            logger.info("No existing stats file found at {}, starting fresh", statsFilePath);
            return;
        }
        
        try {
            String json = Files.readString(path);
            LeaderboardStats loadedStats = gson.fromJson(json, LeaderboardStats.class);
            if (loadedStats != null) {
                stats.merge(loadedStats);
                logger.info("Stats loaded from {}", statsFilePath);
            }
        } catch (IOException e) {
            logger.error("Failed to load stats from {}", statsFilePath, e);
        }
    }
    
    /**
     * Exports statistics to file with timestamp
     */
    public void exportStats(String directory) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = directory + "/leaderboard_export_" + timestamp + ".json";
        
        try {
            Files.createDirectories(Paths.get(directory));
            String json = gson.toJson(stats);
            Files.writeString(Paths.get(filename), json);
            logger.info("Stats exported to {}", filename);
        } catch (IOException e) {
            logger.error("Failed to export stats to {}", filename, e);
        }
    }
    
    /**
     * Resets all statistics
     */
    public synchronized void reset() {
        stats.getEngineStats().clear();
        if (persistenceEnabled) {
            saveStats();
        }
        logger.info("Stats reset");
    }
    
    /**
     * Displays statistics summary in logs
     */
    public void printSummary() {
        logger.info("════════════════════════════════════════");
        logger.info("LEADERBOARD SUMMARY");
        logger.info("════════════════════════════════════════");
        
        var leaderboard = stats.getLeaderboard();
        for (int i = 0; i < leaderboard.size(); i++) {
            var engineStats = leaderboard.get(i);
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "  ";
            logger.info("{} {}. {} - {} pts ({} games: +{} ={} -{})",
                medal,
                i + 1,
                engineStats.getEngineName(),
                String.format("%.1f", engineStats.getPoints()),
                engineStats.getTotalGames(),
                engineStats.getWins(),
                engineStats.getDraws(),
                engineStats.getLosses()
            );
        }
        
        logger.info("════════════════════════════════════════");
    }
}
