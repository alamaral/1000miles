.PHONY: all client server clean test

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
