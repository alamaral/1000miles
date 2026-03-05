.PHONY: all client server clean test

JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/

all: debug server client

debug:
	./debug
server:
	cd server && ./gradlew build

client:
	cd client && npm install && npx vite build

test:
	cd server && ./gradlew test

clean:
	cd server && ./gradlew clean
	rm -rf client/dist
