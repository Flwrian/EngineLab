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
echo ""

mvn clean package -DskipTests

docker compose -f docker-compose.yml up -d
docker compose -f docker-compose.yml exec enginelab-dev bash
