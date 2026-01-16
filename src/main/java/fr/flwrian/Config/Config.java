package fr.flwrian.Config;

import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete configuration loader for EngineLab.
 * Reads config.yml and validates all settings.
 */
public class Config {
    private Tournament tournament;
    private Server server;
    private Paths paths;
    private Logging logging;
    private Stats stats;
    
    // Nested classes for configuration structure
    
    public static class Tournament {
        private String name;
        private String mode;
        private List<String> engines;
        private int concurrency;
        private int pairsPerMatch;
        private List<TimeControl> timeControls;
        private Openings openings;
        
        public String getName() { return name; }
        public String getMode() { return mode; }
        public List<String> getEngines() { return engines; }
        public int getConcurrency() { return concurrency; }
        public int getPairsPerMatch() { return pairsPerMatch; }
        public List<TimeControl> getTimeControls() { return timeControls; }
        public Openings getOpenings() { return openings; }
        
        public void setName(String name) { this.name = name; }
        public void setMode(String mode) { this.mode = mode; }
        public void setEngines(List<String> engines) { this.engines = engines; }
        public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
        public void setPairsPerMatch(int pairsPerMatch) { this.pairsPerMatch = pairsPerMatch; }
        public void setTimeControls(List<TimeControl> timeControls) { this.timeControls = timeControls; }
        public void setOpenings(Openings openings) { this.openings = openings; }
    }
    
    public static class Openings {
        private boolean enabled;
        private String file;
        private String mode;
        
        public boolean isEnabled() { return enabled; }
        public String getFile() { return file; }
        public String getMode() { return mode; }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setFile(String file) { this.file = file; }
        public void setMode(String mode) { this.mode = mode; }
    }
    
    public static class TimeControl {
        private long baseTimeMs;
        private long incrementMs;
        
        public long getBaseTimeMs() { return baseTimeMs; }
        public long getIncrementMs() { return incrementMs; }
        
        public void setBaseTimeMs(long baseTimeMs) { this.baseTimeMs = baseTimeMs; }
        public void setIncrementMs(long incrementMs) { this.incrementMs = incrementMs; }
    }
    
    public static class Server {
        private WebSocket webSocket;
        private Ssl ssl;
        
        public WebSocket getWebSocket() { return webSocket; }
        public Ssl getSsl() { return ssl; }
        
        public void setWebSocket(WebSocket webSocket) { this.webSocket = webSocket; }
        public void setSsl(Ssl ssl) { this.ssl = ssl; }
    }
    
    public static class WebSocket {
        private boolean enabled;
        private int port;
        
        public boolean isEnabled() { return enabled; }
        public int getPort() { return port; }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setPort(int port) { this.port = port; }
    }
    
    public static class Ssl {
        private boolean enabled;
        private int port;
        private String keyStorePath;
        private String keyStorePassword;
        private String keyStoreType;
        
        public boolean isEnabled() { return enabled; }
        public int getPort() { return port; }
        public String getKeyStorePath() { return keyStorePath; }
        public String getKeyStorePassword() { return keyStorePassword; }
        public String getKeyStoreType() { return keyStoreType; }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setPort(int port) { this.port = port; }
        public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }
        public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
        public void setKeyStoreType(String keyStoreType) { this.keyStoreType = keyStoreType; }
    }
    
    public static class Paths {
        private String engineDir;
        private String resourcesDir;
        
        public String getEngineDir() { return engineDir; }
        public String getResourcesDir() { return resourcesDir; }
        
        public void setEngineDir(String engineDir) { this.engineDir = engineDir; }
        public void setResourcesDir(String resourcesDir) { this.resourcesDir = resourcesDir; }
    }
    
    public static class Logging {
        private String level;
        private boolean engineCommunication;
        
        public String getLevel() { return level; }
        public boolean isEngineCommunication() { return engineCommunication; }
        
        public void setLevel(String level) { this.level = level; }
        public void setEngineCommunication(boolean engineCommunication) { this.engineCommunication = engineCommunication; }
    }
    
    public static class Stats {
        private boolean persistenceEnabled;
        private String statsDirectory;
        
        public boolean isPersistenceEnabled() { return persistenceEnabled; }
        public String getStatsDirectory() { return statsDirectory; }
        
        public void setPersistenceEnabled(boolean persistenceEnabled) { 
            this.persistenceEnabled = persistenceEnabled; 
        }
        public void setStatsDirectory(String statsDirectory) { 
            this.statsDirectory = statsDirectory; 
        }
    }
    
    // Getters for main sections
    public Tournament getTournament() { return tournament; }
    public Server getServer() { return server; }
    public Paths getPaths() { return paths; }
    public Logging getLogging() { return logging; }
    public Stats getStats() { return stats; }
    
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    public void setServer(Server server) { this.server = server; }
    public void setPaths(Paths paths) { this.paths = paths; }
    public void setLogging(Logging logging) { this.logging = logging; }
    public void setStats(Stats stats) { this.stats = stats; }
    
    /**
     * Load configuration from YAML file.
     * Validates all required fields and values.
     */
    public static Config load(String configPath) throws IOException {
        Yaml yaml = new Yaml();
        Config config;
        
        try (InputStream input = new FileInputStream(configPath)) {
            config = yaml.loadAs(input, Config.class);
        }
        
        // Validate configuration
        config.validate();
        
        return config;
    }
    
    /**
     * Validate configuration values.
     */
    private void validate() throws IOException {
        // Tournament validation
        if (tournament == null) {
            throw new IOException("Missing 'tournament' section in config.yml");
        }
        
        if (tournament.mode == null) {
            throw new IOException("Tournament mode must be specified");
        }
        List<String> validModes = List.of("round-robin", "pairs");
        if (!validModes.contains(tournament.mode)) {
            throw new IOException("Invalid mode '" + tournament.mode + "'. Must be one of: " + validModes);
        }
        
        if (tournament.engines == null || tournament.engines.isEmpty()) {
            throw new IOException("At least one engine must be specified in 'engines' list");
        }
        
        // Check for minimum 2 different engines (engines can't play against themselves)
        java.util.Set<String> uniqueEngines = new java.util.HashSet<>(tournament.engines);
        if (uniqueEngines.size() < 2) {
            throw new IOException("At least 2 different engines are required (engines cannot play against themselves). Found: " + uniqueEngines.size());
        }
        
        if (tournament.concurrency <= 0) {
            throw new IOException("Concurrency must be positive (got: " + tournament.concurrency + ")");
        }
        if (tournament.concurrency > 16) {
            System.out.println(" Warning: High concurrency (" + tournament.concurrency + ") may cause performance issues");
        }
        
        if (tournament.pairsPerMatch <= 0) {
            throw new IOException("pairsPerMatch must be positive (got: " + tournament.pairsPerMatch + ")");
        }
        
        // Validate timeControls
        if (tournament.timeControls == null || tournament.timeControls.isEmpty()) {
            throw new IOException("Missing 'timeControls' section - at least one time control is required");
        }
        
        // Validate each time control
        for (int i = 0; i < tournament.timeControls.size(); i++) {
            TimeControl tc = tournament.timeControls.get(i);
            if (tc.baseTimeMs <= 0) {
                throw new IOException("timeControls[" + i + "].baseTimeMs must be positive (got: " + tc.baseTimeMs + ")");
            }
            if (tc.incrementMs < 0) {
                throw new IOException("timeControls[" + i + "].incrementMs cannot be negative (got: " + tc.incrementMs + ")");
            }
            if (tc.baseTimeMs < 1000) {
                System.out.println(" Warning: timeControls[" + i + "] has very low base time (" + tc.baseTimeMs + "ms). Games may timeout.");
            }
        }
        
        // Server validation
        if (server == null) {
            throw new IOException("Missing 'server' section");
        }
        
        if (server.webSocket == null) {
            throw new IOException("Missing 'server.webSocket' section");
        }
        if (server.webSocket.port <= 0 || server.webSocket.port > 65535) {
            throw new IOException("Invalid WebSocket port (got: " + server.webSocket.port + "). Must be 1-65535");
        }
        
        // Paths validation
        if (paths == null || paths.engineDir == null || paths.engineDir.isEmpty()) {
            throw new IOException("Missing 'paths.engineDir' in config.yml");
        }
        
        Path engineDirPath = Path.of(paths.engineDir);
        if (!Files.exists(engineDirPath)) {
            throw new IOException("Engine directory does not exist: " + engineDirPath);
        }
        if (!Files.isDirectory(engineDirPath)) {
            throw new IOException("Engine directory path is not a directory: " + engineDirPath);
        }
        
        // Logging validation
        if (logging != null && logging.level != null) {
            List<String> validLevels = List.of("DEBUG", "INFO", "WARN", "ERROR");
            if (!validLevels.contains(logging.level)) {
                throw new IOException("Invalid log level '" + logging.level + "'. Must be one of: " + validLevels);
            }
        }
        
        // Openings validation
        if (tournament.openings != null && tournament.openings.isEnabled()) {
            if (tournament.openings.file == null || tournament.openings.file.isEmpty()) {
                throw new IOException("Openings enabled but no file specified");
            }
            
            Path openingsFile = Path.of(tournament.openings.file);
            if (!Files.exists(openingsFile)) {
                throw new IOException("Openings file not found: " + openingsFile);
            }
            if (!Files.isRegularFile(openingsFile)) {
                throw new IOException("Openings path is not a file: " + openingsFile);
            }
            
            if (tournament.openings.mode != null) {
                List<String> validOpeningModes = List.of("sequential", "random");
                if (!validOpeningModes.contains(tournament.openings.mode)) {
                    throw new IOException("Invalid openings mode '" + tournament.openings.mode + "'. Must be one of: " + validOpeningModes);
                }
            }
        }
    }
    
    /**
     * Get full paths to configured engines.
     * Validates that engines exist in the engine directory.
     */
    public List<String> getEnginePaths() throws IOException {
        List<String> enginePaths = new ArrayList<>();
        String engineDir = paths != null ? paths.getEngineDir() : "./engines";
        
        for (String engineName : tournament.getEngines()) {
            Path enginePath = Path.of(engineDir, engineName);
            
            if (!Files.exists(enginePath)) {
                throw new IOException("Engine not found: " + enginePath + 
                    "\nMake sure the engine exists in the engines/ directory and is executable.");
            }
            
            if (!Files.isExecutable(enginePath)) {
                throw new IOException("Engine is not executable: " + enginePath + 
                    "\nRun: chmod +x " + enginePath);
            }
            
            enginePaths.add(enginePath.toString());
        }
        
        return enginePaths;
    }
    
    /**
     * Load starting positions from EPD/FEN file.
     * Returns empty list if openings are disabled or file is empty.
     */
    public List<String> getStartingPositions() throws IOException {
        List<String> positions = new ArrayList<>();
        
        if (tournament.openings == null || !tournament.openings.isEnabled()) {
            return positions; // Empty list = standard starting position
        }
        
        Path openingsFile = Path.of(tournament.openings.file);
        
        try {
            List<String> lines = Files.readAllLines(openingsFile);
            for (String line : lines) {
                line = line.trim();
                // Skip empty lines and comments
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // EPD format: FEN + optional operations
                    // We only need the FEN part (first 4-6 fields)
                    String[] parts = line.split("\\s+", 7);
                    if (parts.length >= 4) {
                        // Reconstruct FEN (position, side, castling, en passant)
                        StringBuilder fen = new StringBuilder();
                        for (int i = 0; i < Math.min(4, parts.length); i++) {
                            if (i > 0) fen.append(" ");
                            fen.append(parts[i]);
                        }
                        positions.add(fen.toString());
                    }
                }
            }
            
            System.out.println("Loaded " + positions.size() + " opening positions from " + tournament.openings.file);
            
        } catch (IOException e) {
            throw new IOException("Failed to read openings file: " + e.getMessage());
        }
        
        return positions;
    }
    
    /**
     * Get effective list of time controls.
     */
    public List<TimeControl> getTimeControls() {
        if (tournament.getTimeControls() != null && !tournament.getTimeControls().isEmpty()) {
            return tournament.getTimeControls();
        }
        throw new IllegalStateException("No time controls configured");
    }
    
    /**
     * Print configuration summary.
     */
    public void printSummary() {
        System.out.println("=== " + tournament.getName() + " ===");
        System.out.println("Mode:        " + tournament.getMode());
        System.out.println("Engines:     " + String.join(", ", tournament.getEngines()));
        System.out.println("Concurrency: " + tournament.getConcurrency());
        System.out.println("Pairs:       " + tournament.getPairsPerMatch() + " (" + (tournament.getPairsPerMatch() * 2) + " games)");
        
        // Display time controls
        List<TimeControl> timeControls = getTimeControls();
        if (timeControls.size() == 1) {
            TimeControl tc = timeControls.get(0);
            System.out.println("Time:        " + (tc.getBaseTimeMs() / 1000.0) + "s + " + 
                (tc.getIncrementMs() / 1000.0) + "s");
        } else {
            System.out.println("Time:        " + timeControls.size() + " time controls (random selection):");
            for (TimeControl tc : timeControls) {
                System.out.println("             - " + (tc.getBaseTimeMs() / 1000.0) + "s + " + 
                    (tc.getIncrementMs() / 1000.0) + "s");
            }
        }
        
        // Openings info
        if (tournament.getOpenings() != null && tournament.getOpenings().isEnabled()) {
            System.out.println("Openings:    " + tournament.getOpenings().getFile() + " (mode: " + tournament.getOpenings().getMode() + ")");
        } else {
            System.out.println("Openings:    Standard position");
        }
        
        if (server != null && server.getWebSocket() != null && server.getWebSocket().isEnabled()) {
            System.out.println("WebSocket:   localhost:" + server.getWebSocket().getPort());
            System.out.println("Live View: http://localhost:" + server.getWebSocket().getPort() + "/live");
        }
        System.out.println("===============================\n");
    }
}
