#!/bin/bash

# Ports used by the services
FRONTEND_PORT=5173
BACKEND_PORT=8000
API_SERVER_PORT=8090

# Function to kill a process on a specific port
kill_port() {
    local port=$1
    local pid=$(lsof -ti:$port)
    if [ ! -z "$pid" ]; then
        echo "Killing process on port $port (PID: $pid)..."
        kill -9 $pid
    fi
}

# Function to run a command with retries on segmentation fault
run_with_retry() {
    local dir="$1"
    local cmd="$2"
    local name="$3"
    local log="$4"
    local count=0
    local max_retries=3

    (
        cd "$dir" || exit 1
        while [ $count -lt $max_retries ]; do
            echo "Starting $name..."
            eval "$cmd >> $log 2>&1"
            local exit_code=$?
            
            # 139 is the standard exit code for Segmentation Fault
            if [ $exit_code -eq 139 ]; then
                count=$((count + 1))
                echo "Segmentation fault detected in $name. Restarting ($count/$max_retries)..."
                sleep 1
            else
                # If it's a normal exit or other error, we stop retrying
                break
            fi
        done
    )
}

# Ensure existing services are stopped before starting
echo "Checking for existing services..."
kill_port $FRONTEND_PORT
kill_port $BACKEND_PORT
kill_port $API_SERVER_PORT

# Start services in the background
echo "Starting services in the background..."
run_with_retry "api-server" "./gradlew run" "api-server" "./apiserver.log" &
run_with_retry "backend" "deno task dev" "backend" "./backend.log" &
run_with_retry "frontend" "npm install && npm run dev" "frontend" "./frontend.log" &

# Function to wait for a port to be ready
wait_for_port() {
    local port=$1
    local name=$2
    local timeout=60
    local count=0
    echo -n "Waiting for $name on port $port..."
    while ! nc -z localhost $port; do
        sleep 1
        count=$((count + 1))
        if [ $count -ge $timeout ]; then
            echo " Timeout reached for $name."
            return 1
        fi
        echo -n "."
    done
    echo " Ready!"
}

# Wait for all services to be ready
wait_for_port $API_SERVER_PORT "api-server"
wait_for_port $BACKEND_PORT "backend"
wait_for_port $FRONTEND_PORT "frontend"

echo "All services have started and are running in the background."
echo "Logs: api-server/apiserver.log, backend/backend.log, frontend/frontend.log"
exit 0