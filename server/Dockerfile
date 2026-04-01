# Copyright © 2026 Alan Amaral
# All rights reserved.
#
# Unauthorized copying, modification, distribution, or use of this software,
# via any medium, is strictly prohibited without prior written permission.
#
# Description:
# Multi-stage Docker build for the Spring Boot server.

FROM eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew build -x test

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
