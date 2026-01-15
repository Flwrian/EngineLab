# EngineLab

Chess engine tournament system for UCI engines. Run automated tournaments between chess engines with real-time web interface and detailed statistics.

## Features

- **Multi-engine tournaments** - Run round-robin or concurrent matches
- **Real-time web interface** - Watch games live at `/live`
- **Detailed statistics** - Track performance by engine and time control
- **Flexible time controls** - Support for base time + increment
- **Opening books** - Use EPD files for varied starting positions
- **Docker support** - Develop and run in isolated containers

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

## Configuration

Edit `config.yml` to configure:
- **Engines** - Paths and names of UCI engines
- **Time Control** - Base time and increment (supports multiple)
- **Tournament** - Number of games, concurrency, mode
- **Openings** - EPD file path and selection mode
- **Server** - WebSocket port and settings

Example:
```yaml
tournament:
  games: 100
  concurrency: 4
  mode: "random"

timeControl:
  base: 5000      # 5 seconds
  increment: 100  # 0.1 seconds
```

## Web Interface

Once started:
- **Live view**: `http://localhost:8080/live`
- **Leaderboard**: `http://localhost:8080/leaderboard`

## Adding Engines

1. Place UCI engine binary in `engines/` directory
2. Add to `config.yml`:
```yaml
engines:
  - "MyEngine"
```

## Project Structure

```
enginelab/
├── src/main/java/fr/flwrian/  # Java source code
├── src/main/resources/         # HTML/CSS/JS for web UI
├── engines/                    # UCI engine binaries
├── stats/                      # Persistent statistics (JSON)
├── config.yml                  # Main configuration
└── docker-compose.dev.yml      # Docker dev environment
```

## Requirements

- **Local**: Java 17+, Maven
- **Docker**: Docker + Docker Compose

## Tech Stack

- Java 17 with Maven
- Jetty WebSocket server
- Gson for JSON handling
- Chessground.js for board visualization
