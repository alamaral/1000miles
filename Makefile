# Copyright © 2026 Alan Amaral
# All rights reserved.
#
# Unauthorized copying, modification, distribution, or use of this software,
# via any medium, is strictly prohibited without prior written permission.
#
# Description:
# Top-level Makefile for building the server and client.

.PHONY: all client server clean test debug

JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/

all: server client

server:
	cd server && ./gradlew build

client:
	cd client && npm install && npx vite build

test:
	cd server && ./gradlew test

clean:
	cd server && ./gradlew clean
	rm -rf client/dist
