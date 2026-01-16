package fr.flwrian.Config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void testLoadValidConfig(@TempDir Path tempDir) throws IOException {
        // Create a minimal valid config
        String yaml = """
            tournament:
              name: "Test Tournament"
              mode: "pairs"
              engines:
                - "engine1"
                - "engine2"
              concurrency: 2
              pairsPerMatch: 10
              timeControls:
                - baseTimeMs: 60000
                  incrementMs: 1000
              openings:
                enabled: false
            server:
              webSocket:
                enabled: true
                host: "localhost"
                port: 8080
              http:
                enabled: true
                host: "localhost"
                port: 8080
              shutdown:
                gracefulTimeoutSeconds: 30
            paths:
              engineDir: "./engines"
              outputDir: "./output"
              logDir: "./logs"
              resourcesDir: "./resources"
            logging:
              level: "INFO"
              logToFile: false
              logToConsole: true
              gameProgress: true
              gameProgressInterval: 20
              engineOutput: false
              webSocketEvents: false
            performance:
              engineStartupTimeoutSeconds: 10
              engineResponseTimeoutSeconds: 60
              maxThreadPoolSize: 10
              recommendedHeapSizeMB: 512
            deployment:
              environment: "development"
              healthCheck:
                enabled: true
                endpoint: "/health"
                interval: 30
            stats:
              persistenceEnabled: true
              statsDirectory: "./stats"
            """;

        Path configFile = tempDir.resolve("test-config.yml");
        Files.writeString(configFile, yaml);

        Config config = Config.load(configFile.toString());

        assertEquals("Test Tournament", config.getTournament().getName());
        assertEquals("pairs", config.getTournament().getMode());
        assertEquals(2, config.getTournament().getEngines().size());
        assertEquals(2, config.getTournament().getConcurrency());
        assertEquals(10, config.getTournament().getPairsPerMatch());

        List<Config.TimeControl> timeControls = config.getTournament().getTimeControls();
        assertEquals(1, timeControls.size());
        assertEquals(60000, timeControls.get(0).getBaseTimeMs());
        assertEquals(1000, timeControls.get(0).getIncrementMs());
    }

    @Test
    void testMultipleTimeControls(@TempDir Path tempDir) throws IOException {
        String yaml = """
            tournament:
              name: "Test"
              mode: "pairs"
              engines: ["engine1", "engine2"]
              concurrency: 1
              pairsPerMatch: 5
              timeControls:
                - baseTimeMs: 5000
                  incrementMs: 100
                - baseTimeMs: 10000
                  incrementMs: 200
                - baseTimeMs: 60000
                  incrementMs: 1000
              openings:
                enabled: false
            server:
              webSocket:
                enabled: true
                host: "localhost"
                port: 8080
              http:
                enabled: true
                host: "localhost"
                port: 8080
              shutdown:
                gracefulTimeoutSeconds: 30
            paths:
              engineDir: "./engines"
              outputDir: "./output"
              logDir: "./logs"
              resourcesDir: "./resources"
            logging:
              level: "INFO"
              logToFile: false
              logToConsole: true
              gameProgress: true
              gameProgressInterval: 20
              engineOutput: false
              webSocketEvents: false
            performance:
              engineStartupTimeoutSeconds: 10
              engineResponseTimeoutSeconds: 60
              maxThreadPoolSize: 10
              recommendedHeapSizeMB: 512
            deployment:
              environment: "development"
              healthCheck:
                enabled: true
                endpoint: "/health"
                interval: 30
            stats:
              persistenceEnabled: true
              statsDirectory: "./stats"
            """;

        Path configFile = tempDir.resolve("multi-tc.yml");
        Files.writeString(configFile, yaml);

        Config config = Config.load(configFile.toString());
        List<Config.TimeControl> timeControls = config.getTournament().getTimeControls();

        assertEquals(3, timeControls.size());
        assertEquals(5000, timeControls.get(0).getBaseTimeMs());
        assertEquals(10000, timeControls.get(1).getBaseTimeMs());
        assertEquals(60000, timeControls.get(2).getBaseTimeMs());
    }

    @Test
    void testMissingTimeControls(@TempDir Path tempDir) throws IOException {
        String yaml = """
            tournament:
              name: "Test"
              mode: "pairs"
              engines: ["engine1", "engine2"]
              concurrency: 1
              pairsPerMatch: 5
              openings:
                enabled: false
            server:
              webSocket:
                enabled: true
                host: "localhost"
                port: 8080
              http:
                enabled: true
                host: "localhost"
                port: 8080
              shutdown:
                gracefulTimeoutSeconds: 30
            paths:
              engineDir: "./engines"
              outputDir: "./output"
              logDir: "./logs"
              resourcesDir: "./resources"
            logging:
              level: "INFO"
              logToFile: false
              logToConsole: true
              gameProgress: true
              gameProgressInterval: 20
              engineOutput: false
              webSocketEvents: false
            performance:
              engineStartupTimeoutSeconds: 10
              engineResponseTimeoutSeconds: 60
              maxThreadPoolSize: 10
              recommendedHeapSizeMB: 512
            deployment:
              environment: "development"
              healthCheck:
                enabled: true
                endpoint: "/health"
                interval: 30
            stats:
              persistenceEnabled: true
              statsDirectory: "./stats"
            """;

        Path configFile = tempDir.resolve("no-tc.yml");
        Files.writeString(configFile, yaml);

        IOException exception = assertThrows(IOException.class, () -> {
            Config.load(configFile.toString());
        });

        // Check that error mentions missing time controls
        String msg = exception.getMessage().toLowerCase();
        assertTrue(msg.contains("timecontrol") || msg.contains("missing"), 
            "Expected error about missing timeControls, got: " + exception.getMessage());
    }

    @Test
    void testInvalidTimeControl(@TempDir Path tempDir) throws IOException {
        String yaml = """
            tournament:
              name: "Test"
              mode: "pairs"
              engines: ["engine1", "engine2"]
              concurrency: 1
              pairsPerMatch: 5
              timeControls:
                - baseTimeMs: -1000
                  incrementMs: 100
              openings:
                enabled: false
            server:
              webSocket:
                enabled: true
                host: "localhost"
                port: 8080
              http:
                enabled: true
                host: "localhost"
                port: 8080
              shutdown:
                gracefulTimeoutSeconds: 30
            paths:
              engineDir: "./engines"
              outputDir: "./output"
              logDir: "./logs"
              resourcesDir: "./resources"
            logging:
              level: "INFO"
              logToFile: false
              logToConsole: true
              gameProgress: true
              gameProgressInterval: 20
              engineOutput: false
              webSocketEvents: false
            performance:
              engineStartupTimeoutSeconds: 10
              engineResponseTimeoutSeconds: 60
              maxThreadPoolSize: 10
              recommendedHeapSizeMB: 512
            deployment:
              environment: "development"
              healthCheck:
                enabled: true
                endpoint: "/health"
                interval: 30
            stats:
              persistenceEnabled: true
              statsDirectory: "./stats"
            """;

        Path configFile = tempDir.resolve("invalid-tc.yml");
        Files.writeString(configFile, yaml);

        IOException exception = assertThrows(IOException.class, () -> {
            Config.load(configFile.toString());
        });

        assertTrue(exception.getMessage().toLowerCase().contains("basetimems") || 
                   exception.getMessage().toLowerCase().contains("positive"),
            "Expected error about invalid baseTimeMs, got: " + exception.getMessage());
    }
}
