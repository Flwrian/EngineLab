#!/bin/bash
# EngineLab - Docker Dev Mode

set -e

CONFIG_FILE=${1:-config.yml}

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file $CONFIG_FILE not found"
    exit 1
fi

echo "Starting EngineLab in dev mode..."
echo "You can:"
echo "  - Edit files on your host, they're synced in real-time"
echo "  - Run ./run.sh inside the container"
echo "  - Maven cache is persisted between runs"
echo ""

docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml exec enginelab-dev bash
