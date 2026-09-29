# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Abhaengigkeiten zuerst laden, damit sie bei Code-Aenderungen gecacht bleiben
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package

# ---- Laufzeit ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/hypixeltracker-server.jar app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
