# ── Stage 1: Build ────────────────────────────────────────────
FROM gradle:8.11-jdk21 AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src/ ./src/

# Build an uber-jar (single file, no directory layout issues)
RUN gradle quarkusBuild -Dquarkus.package.jar.type=uber-jar --no-daemon

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

# Copy the single uber-jar
COPY --from=build /app/build/*-runner.jar ./kortex.jar

ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:+UseZGC -XX:+ZGenerational"
EXPOSE 8080

CMD ["java", "-jar", "kortex.jar"]