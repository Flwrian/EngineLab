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

### Docker
- **Docker**: 20.10+
- **Docker Compose**: 1.29+

### Runtime
- **Memory**: Minimum 1GB RAM, recommended 2GB+
- **CPU**: Multi-core recommended for concurrent games
- **Disk**: ~50MB for application + space for stats/logs

## Dependencies

All dependencies are managed by Maven and automatically downloaded:

### Core Dependencies
- **Chesslib** (1.3.3) - Chess move validation and checkmate detection (maybe replaced with my own implementation based on Aspria but good for now)
- **Jetty WebSocket** (11.0.18) - WebSocket server
- **Jetty Server** (11.0.18) - HTTP server
- **Jetty Servlet** (11.0.18) - Servlet support
- **Jetty WebApp** (11.0.18) - Web application container
- **Gson** (2.10.1) - JSON serialization for stats and WebSocket messages
- **SnakeYAML** (2.2) - YAML configuration parsing
- **SLF4J Simple** (2.0.9) - Logging implementation

### Build Plugins
- **Maven Shade Plugin** (3.5.1) - Creates executable JAR with all dependencies

### Frontend
- **Chessground.js** - Interactive chess board visualization
- Vanilla JavaScript - No framework dependencies

To see all dependencies:
```bash
mvn dependency:tree
```

## Configuration

All settings in `config.yml`:

### Tournament
```yaml
tournament:
  name: "EngineLab Tournament"
  mode: "pairs"                    # "pairs" or "round-robin"
  engines:
    - Aspira
    - stockfish
  concurrency: 1                   # Concurrent games (1 = sequential)
  pairsPerMatch: 100              # Number of pairs (each pair = 2 games)
  
  # Multiple time controls - randomly selected per pair
  timeControls:
    - baseTimeMs: 60000            # 1min + 1s (bullet)
      incrementMs: 1000
    - baseTimeMs: 180000           # 3min + 2s (blitz)
      incrementMs: 2000
  
  openings:
    enabled: true
    file: "8moves.epd"            # EPD file with positions
    mode: "random"                # "random" or "sequential"
```

### Server
```yaml
server:
  webSocket:
    enabled: true
    host: "0.0.0.0"               # Bind address
    port: 8080
  
  http:
    enabled: true
    host: "0.0.0.0"
    port: 8080
  
  shutdown:
    gracefulTimeoutSeconds: 30
```

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
  logToFile: false
  logToConsole: true
  gameProgress: true
  gameProgressInterval: 20        # Log every N moves
  engineOutput: false             # Raw engine output
  webSocketEvents: false
```

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

# Skip tests
mvn package -DskipTests

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
