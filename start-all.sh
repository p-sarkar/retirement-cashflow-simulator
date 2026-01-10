#!/bin/bash

# Function to kill all background processes when the script exits
cleanup() {
    echo "Stopping all services..."
    kill $(jobs -p)
}

trap cleanup EXIT

# Start frontend
echo "Starting frontend..."
(cd frontend && npm install && npm run dev > ./frontend.log) &

# Start backend
echo "Starting backend..."
(cd backend && deno task dev > ./backend.log) &

# Start api-server
echo "Starting api-server..."
(cd api-server && ./gradlew run > ./apiserver.log) &

# Wait for all background processes to finish
wait
