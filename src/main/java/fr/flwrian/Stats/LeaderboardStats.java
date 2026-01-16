package fr.flwrian.Stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leaderboard statistics by engine and time control
 */
public class LeaderboardStats {
    
    /**
     * Statistics for a specific engine
     */
    public static class EngineStats {
        private String engineName;
        private int totalGames;
        private int wins;
        private int draws;
        private int losses;
        private double points; // 1 for win, 0.5 for draw, 0 for loss
        private int elo; // Elo rating
        private int peakElo; // Highest Elo reached
        
        // Stats par time control
        private Map<String, TimeControlStats> timeControlStats;
        
        public EngineStats(String engineName) {
            this.engineName = engineName;
            this.totalGames = 0;
            this.wins = 0;
            this.draws = 0;
            this.losses = 0;
            this.points = 0.0;
            this.elo = EloCalculator.DEFAULT_ELO;
            this.peakElo = EloCalculator.DEFAULT_ELO;
            this.timeControlStats = new ConcurrentHashMap<>();
        }
        
        public void addWin(String timeControl) {
            wins++;
            totalGames++;
            points += 1.0;
            if (timeControl != null) {
                getOrCreateTimeControlStats(timeControl).addWin();
            }
        }
        
        public void addDraw(String timeControl) {
            draws++;
            totalGames++;
            points += 0.5;
            if (timeControl != null) {
                getOrCreateTimeControlStats(timeControl).addDraw();
            }
        }
        
        public void addLoss(String timeControl) {
            losses++;
            totalGames++;
            if (timeControl != null) {
                getOrCreateTimeControlStats(timeControl).addLoss();
            }
        }
        
        public void updateElo(int newElo) {
            this.elo = newElo;
            if (newElo > peakElo) {
                this.peakElo = newElo;
            }
        }
        
        private TimeControlStats getOrCreateTimeControlStats(String timeControl) {
            // Use "Unknown" as fallback if timeControl is null
            String tc = (timeControl != null) ? timeControl : "Unknown";
            return timeControlStats.computeIfAbsent(tc, t -> new TimeControlStats(t));
        }
        
        public double getWinRate() {
            return totalGames > 0 ? (double) wins / totalGames * 100 : 0.0;
        }
        
        public double getDrawRate() {
            return totalGames > 0 ? (double) draws / totalGames * 100 : 0.0;
        }
        
        public double getLossRate() {
            return totalGames > 0 ? (double) losses / totalGames * 100 : 0.0;
        }
        
        public double getPointsPercentage() {
            return totalGames > 0 ? points / totalGames * 100 : 0.0;
        }
        
        // Getters
        public String getEngineName() { return engineName; }
        public int getTotalGames() { return totalGames; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
        public double getPoints() { return points; }
        public int getElo() { return elo; }
        public int getPeakElo() { return peakElo; }
        public Map<String, TimeControlStats> getTimeControlStats() { return timeControlStats; }
    }
    
    /**
     * Statistics for a specific time control
     */
    public static class TimeControlStats {
        private String timeControl;
        private int games;
        private int wins;
        private int draws;
        private int losses;
        
        public TimeControlStats(String timeControl) {
            this.timeControl = timeControl;
            this.games = 0;
            this.wins = 0;
            this.draws = 0;
            this.losses = 0;
        }
        
        public void addWin() {
            wins++;
            games++;
        }
        
        public void addDraw() {
            draws++;
            games++;
        }
        
        public void addLoss() {
            losses++;
            games++;
        }
        
        public double getWinRate() {
            return games > 0 ? (double) wins / games * 100 : 0.0;
        }
        
        // Getters
        public String getTimeControl() { return timeControl; }
        public int getGames() { return games; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
    }
    
    private Map<String, EngineStats> engineStats;
    
    public LeaderboardStats() {
        this.engineStats = new ConcurrentHashMap<>();
    }
    
    /**
     * Records game result and updates Elo ratings
     */
    public void recordGameResult(String whiteEngine, String blackEngine, String result, String timeControl) {
        EngineStats whiteStats = getOrCreateEngineStats(whiteEngine);
        EngineStats blackStats = getOrCreateEngineStats(blackEngine);
        
        // Update Elo ratings
        int[] newElos = EloCalculator.updateEloRatings(whiteStats.getElo(), blackStats.getElo(), result);
        whiteStats.updateElo(newElos[0]);
        blackStats.updateElo(newElos[1]);
        
        // Update game statistics
        switch (result) {
            case "1-0":
                whiteStats.addWin(timeControl);
                blackStats.addLoss(timeControl);
                break;
            case "0-1":
                whiteStats.addLoss(timeControl);
                blackStats.addWin(timeControl);
                break;
            case "1/2-1/2":
                whiteStats.addDraw(timeControl);
                blackStats.addDraw(timeControl);
                break;
        }
    }
    
    private EngineStats getOrCreateEngineStats(String engineName) {
        return engineStats.computeIfAbsent(engineName, name -> new EngineStats(name));
    }
    
    /**
     * Returns statistics sorted by Elo rating
     */
    public List<EngineStats> getLeaderboard() {
        List<EngineStats> leaderboard = new ArrayList<>(engineStats.values());
        leaderboard.sort((a, b) -> {
            // Sort by Elo rating (higher first)
            int eloCompare = Integer.compare(b.getElo(), a.getElo());
            if (eloCompare != 0) return eloCompare;
            // If same Elo, sort by points
            int pointsCompare = Double.compare(b.getPoints(), a.getPoints());
            if (pointsCompare != 0) return pointsCompare;
            // If same points, sort by wins
            int winsCompare = Integer.compare(b.getWins(), a.getWins());
            if (winsCompare != 0) return winsCompare;
            // Finally, sort by name
            return a.getEngineName().compareTo(b.getEngineName());
        });
        return leaderboard;
    }
    
    /**
     * Retourne tous les time controls utilisés
     */
    public Set<String> getAllTimeControls() {
        Set<String> timeControls = new HashSet<>();
        for (EngineStats stats : engineStats.values()) {
            timeControls.addAll(stats.getTimeControlStats().keySet());
        }
        return timeControls;
    }
    
    public Map<String, EngineStats> getEngineStats() {
        return engineStats;
    }
    
    /**
     * Merges statistics from another instance
     */
    public void merge(LeaderboardStats other) {
        for (Map.Entry<String, EngineStats> entry : other.engineStats.entrySet()) {
            String engineName = entry.getKey();
            EngineStats otherStats = entry.getValue();
            EngineStats thisStats = getOrCreateEngineStats(engineName);
            
            // Fusionner les stats globales
            thisStats.wins += otherStats.wins;
            thisStats.draws += otherStats.draws;
            thisStats.losses += otherStats.losses;
            thisStats.totalGames += otherStats.totalGames;
            thisStats.points += otherStats.points;
            
            // Restore Elo from saved data (don't add, just restore)
            if (otherStats.elo > 0) {
                thisStats.elo = otherStats.elo;
            }
            if (otherStats.peakElo > thisStats.peakElo) {
                thisStats.peakElo = otherStats.peakElo;
            }
            
            // Fusionner les stats par time control
            for (Map.Entry<String, TimeControlStats> tcEntry : otherStats.timeControlStats.entrySet()) {
                String tc = tcEntry.getKey();
                TimeControlStats otherTcStats = tcEntry.getValue();
                TimeControlStats thisTcStats = thisStats.getOrCreateTimeControlStats(tc);
                
                thisTcStats.wins += otherTcStats.wins;
                thisTcStats.draws += otherTcStats.draws;
                thisTcStats.losses += otherTcStats.losses;
                thisTcStats.games += otherTcStats.games;
            }
        }
    }
}
