# EngineLab
Chess engine tournament system for UCI engines. Run automated tournaments between chess engines with real-time web interface and detailed statistics.

<img width="1899" height="1021" alt="image" src="https://github.com/user-attachments/assets/e0853904-c0f6-470a-b8e7-c8617307ffe4" />

<img width="1908" height="1021" alt="image" src="https://github.com/user-attachments/assets/d63ea1cf-b9fd-4bae-b621-e0869960cfc8" />


## Features

- **Multi-engine tournaments** - Run round-robin or concurrent matches
- **Real-time web interface** - Watch games live with interactive chessboard
- **Detailed statistics** - Track performance by engine and time control
- **Flexible time controls** - Support for base time + increment
- **Opening books** - Use EPD files for varied starting positions
- **Concurrent execution** - Run multiple games in parallel
- **WebSocket streaming** - Real-time updates with no polling
- **UCI protocol logging** - Debug engine communication

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

The code is mounted as a volume, so you can edit files on your host and it will reflect inside the container (need to restart the app to pick up changes).

## Requirements

### Local Development
- **Java**: 17 or higher (JDK)
- **Maven**: 3.6+ for building
- **OS**: Linux, macOS, or Windows with WSL

## Configuration

Minimal `config.yml` example:

```yaml
tournament:
  name: "EngineLab Tournament"
  mode: "pairs"                    # "pairs" or "round-robin"
  engines:
    - "Aspira_3"
    - "stockfish"
  concurrency: 1                   # Number of games to run in parallel
  pairsPerMatch: 100               # Each pair = 2 games (colors swapped)
  timeControls:
    - baseTimeMs: 5000             # 5 seconds base time
      incrementMs: 100             # 0.1s increment per move
  openings:
    enabled: true
    file: "8moves.epd"
    mode: "random"                 # "random" or "sequential"

server:
  webSocket:
    enabled: true
    port: 8080

paths:
  engineDir: "./engines"
  resourcesDir: "./src/main/resources"

logging:
  level: "WARN"                    # DEBUG, INFO, WARN, ERROR
  engineCommunication: true        # Log all UCI commands and responses

stats:
  persistenceEnabled: true
  statsDirectory: "./stats"
```

### Tournament Modes

**Pairs Mode** (recommended):
- Each engine pair plays N games with colors swapped
- Example: `pairsPerMatch: 100` = 200 total games (100 white + 100 black)
- Good for comparing two engines head-to-head

**Round-Robin Mode**:
- Every engine plays every other engine
- With N engines: N*(N-1)/2 unique pairs
- Good for tournaments with multiple engines
### UCI Communication Logging

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
[UCI ->] bestmove e7e5
```

This is useful for debugging engine issues or understanding how engines think.

## Web Interface

- **Live view**: `http://localhost:8080/live` - Watch games in real-time with interactive chessboard
- **Leaderboard**: `http://localhost:8080/leaderboard` - View rankings and detailed stats
- **WebSocket**: `ws://localhost:8080/ws` - Direct WebSocket connection


## Adding Engines

1. Download UCI engine binary (e.g., from [Stockfish](https://stockfishchess.org/))
2. Place in `engines/` directory
3. Make executable: `chmod +x engines/stockfish`
4. Add to `config.yml`:
```yaml
engines:
  - "stockfish"
  - "another_engine"
```

### Supported Engines
Any UCI-compatible chess engine:
- Stockfish
- Leela Chess Zero (lc0)
- Komodo
- Custom engines

## Tech Stack

- **Java 17** - Core language
- **Maven** - Build and dependency management
- **Jetty 11** - WebSocket server and HTTP endpoints
- **Gson** - JSON serialization
- **SnakeYAML** - YAML parsing
- **chesslib** - Chess position validation
- **Chessground.js** - Board visualization
- **Vanilla JS** - Frontend (no framework bloat)


## Troubleshooting

### Engine Not Found
- Check path in `config.yml`
- Ensure binary is executable: `chmod +x engines/yourengine`
- Verify engine responds to `uci` command: `./engines/yourengine` then type `uci`

### Port Already in Use
- Change port in `config.yml` under `server.webSocket.port`
- Kill process using port: `lsof -ti:8080 | xargs kill`

### Games Timeout
- Increase base time in `timeControls`
- Check engine is responding with `engineCommunication: true`
- Verify engine binary is correct architecture (x64, ARM, etc.)

### WebSocket Connection Issues
- Check firewall settings
- Verify port is accessible: `netstat -an | grep 8080`
- Use the IP address shown at startup, not localhost

## License

MIT License - See LICENSE file for details

## Author

Flwrian
