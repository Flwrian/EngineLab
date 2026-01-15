#!/bin/bash
# EngineLab - Local launcher

set -e

CONFIG=${1:-config.yml}

if [ ! -f "target/enginelab-1.0-SNAPSHOT.jar" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

if [ ! -f "$CONFIG" ]; then
    echo "Error: Configuration file not found: $CONFIG"
    exit 1
fi

echo "Starting EngineLab with $CONFIG"
java -jar target/enginelab-1.0-SNAPSHOT.jar "$CONFIG"
