# EngineLab

Chess engine tournament system for UCI engines. Run automated tournaments between chess engines with real-time web interface and detailed statistics.

## Features

- **Multi-engine tournaments** - Run round-robin or concurrent matches
- **Real-time web interface** - Watch games live at `/live`
- **Detailed statistics** - Track performance by engine and time control
- **Flexible time controls** - Support for base time + increment
- **Opening books** - Use EPD files for varied starting positions
- **Docker support** - Develop and run in isolated containers
- **Concurrent execution** - Run multiple games in parallel
- **WebSocket streaming** - Real-time updates with no polling

## Quick Start

### Local (Direct)
```bash
# Build and run
./run.sh

# With custom config
./run.sh my-config.yml
```

### Docker (Development)
```bash
# Start interactive container with code mounted
./docker.sh

# Inside container, run the app
./run.sh
```

The code is mounted as a volume, so you can edit files on your host and changes are reflected immediately in the container.

## Requirements

### Local Development
- **Java**: 17 or higher (JDK)
- **Maven**: 3.6+ for building
- **OS**: Linux, macOS, or Windows with WSL
### Configuration (minimal & tunnel-safe)

EngineLab uses a minimal, explicit config. We intentionally avoid "one-size-fits-all" options and keep the file small and honest.

Recommended `config.yml` (dev / tunnel usage):

```yaml
tournament:
  name: "EngineLab Tournament"
  mode: "pairs"
  engines:
    - "Aspira_3"
    - "stockfish"
  concurrency: 1
  pairsPerMatch: 100
  timeControls:
    - baseTimeMs: 5000
      incrementMs: 100
  openings:
    enabled: true
    file: "8moves.epd"
    mode: "random"

server:
  webSocket:
    enabled: true
    host: "127.0.0.1"    # Localhost only - use a tunnel for external access
    port: 8080
  http:
    enabled: true
    host: "127.0.0.1"
    port: 8080
  shutdown:
    gracefulTimeoutSeconds: 30

paths:
  engineDir: "./engines"
  outputDir: "./output"
  resourcesDir: "./src/main/resources"

logging:
  level: "WARN"                    # DEBUG, INFO, WARN, ERROR
  engineCommunication: false       # Log all UCI commands (<-) and responses (->)

performance:
  engineStartupTimeoutSeconds: 10
  engineResponseTimeoutSeconds: 60

stats:
  persistenceEnabled: true
  statsDirectory: "./stats"
```

Notes:
- We purposely bind to `127.0.0.1` so the app is reachable only locally. Use an external tunnel (Cloudflare Tunnel, Tailscale, ngrok, etc.) to expose it securely.
- Internal TLS/keystore options have been removed from the default config. If you want HTTPS/WSS in production, terminate TLS at the edge (reverse proxy / tunnel) and keep the app local.

### Paths
```yaml
paths:
  engineDir: "./engines"
  outputDir: "./output"
  logDir: "./logs"
  resourcesDir: "./src/main/resources"
```

### Logging
```yaml
logging:
  level: "INFO"                   # DEBUG, INFO, WARN, ERROR
  engineCommunication: false      # Log all UCI protocol communication
```

**UCI Communication Logging**

When `engineCommunication: true`, all UCI protocol communication is logged to the console:
- `[UCI <-]` - Commands sent TO the engine
- `[UCI ->]` - Responses received FROM the engine

Example output:
```
[UCI <-] uci
[UCI ->] id name Stockfish 16
[UCI ->] id author the Stockfish developers
[UCI ->] uciok
[UCI <-] isready
[UCI ->] readyok
[UCI <-] position startpos moves e2e4
[UCI <-] go wtime 60000 btime 60000 winc 1000 binc 1000
[UCI ->] info depth 1 seldepth 1 score cp 52 nodes 20 nps 20000 pv e7e5
[UCI ->] info depth 2 seldepth 2 score cp 49 nodes 40 nps 40000 pv e7e5 g1f3
[UCI ->] bestmove e7e5
```

This is useful for debugging engine issues or understanding how engines think.

### Performance
```yaml
performance:
  engineStartupTimeoutSeconds: 10
  engineResponseTimeoutSeconds: 60
  maxThreadPoolSize: 10
  recommendedHeapSizeMB: 512
```

### Stats
```yaml
stats:
  persistenceEnabled: true
  statsDirectory: "./stats"
```

## Web Interface

Once started:
- **Live view**: `http://localhost:8080/live` - Watch games in real-time
- **Leaderboard**: `http://localhost:8080/leaderboard` - View rankings and stats
- **WebSocket**: `ws://localhost:8080/ws` - Direct WebSocket connection

## Adding Engines

1. Download UCI engine binary (e.g., from [Stockfish](https://stockfishchess.org/))
2. Place in `engines/` directory
3. Make executable: `chmod +x engines/stockfish`
4. Add to `config.yml`:
```yaml
engines:
  - "engine"
```

### Supported Engines
Any UCI-compatible chess engine:
- Stockfish
- Leela Chess Zero (lc0)
- Komodo
- Rybka
- Custom engines

## Building

```bash
# Clean build
mvn clean package

gmvn package

# Run directly
mvn exec:java

# Create executable JAR
mvn package
java -jar target/enginelab-1.0-SNAPSHOT.jar
```

## Project Structure

```
enginelab/
├── src/
│   ├── main/
│   │   ├── java/fr/flwrian/
│   │   │   ├── Chess/           # Chess move validation
│   │   │   ├── Config/          # Configuration management
│   │   │   ├── Engine/          # UCI engine communication
│   │   │   ├── Game/            # Game state and management
│   │   │   ├── Result/          # Game/pair results
│   │   │   ├── Runner/          # Tournament execution
│   │   │   ├── Stats/           # Statistics tracking
│   │   │   ├── Task/            # Concurrent game tasks
│   │   │   ├── Util/            # Utilities (FEN loader, etc.)
│   │   │   └── WebSocket/       # WebSocket server
│   │   └── resources/
│   │       ├── live.html        # Live game viewer
│   │       ├── leaderboard.html # Statistics page
│   │       ├── chessground.js   # Board library
│   │       └── pieces/          # SVG chess pieces
│   └── test/java/               # Unit tests
├── engines/                     # UCI engine binaries (not in git)
├── stats/                       # Persistent statistics (not in git)
├── logs/                        # Application logs (not in git)
├── config.yml                   # Main configuration
├── docker-compose.dev.yml       # Docker dev environment
├── docker.sh                    # Docker launcher
├── run.sh                       # Local launcher
└── pom.xml                      # Maven configuration
```

## Tech Stack

- **Java 17** - Core language
- **Maven** - Build and dependency management
- **Jetty 11** - WebSocket server and HTTP endpoints
- **Gson** - JSON serialization
- **SnakeYAML** - YAML parsing
- **SLF4J + Logback** - Structured logging
- **Chessground.js** - Board visualization
- **Vanilla JS** - Frontend (no framework bloat)

## Development

### Running Tests
```bash
mvn test
```

### Code Style
- Follow standard Java conventions
- Use meaningful variable names
- Comment complex algorithms
- Keep methods focused and small

### Hot Reload (with Docker)
```bash
./docker.sh
# Edit files on host
# Inside container:
mvn compile exec:java
```

## Troubleshooting

### Engine Not Found
- Check path in `config.yml`
- Ensure binary is executable: `chmod +x engines/yourengine`
- Verify engine responds to `uci` command

### Port Already in Use
- Change port in `config.yml` under `server.websocket.port`
- Kill process using port: `lsof -ti:8080 | xargs kill`

### Out of Memory
- Increase JVM heap: `JAVA_OPTS=-Xmx4g ./run.sh`
- Reduce concurrency in `config.yml`

## License

MIT License - See LICENSE file for details

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Author

Flwrian
