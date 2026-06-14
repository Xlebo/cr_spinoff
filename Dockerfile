# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Cache the Gradle distribution separately from source so wrapper changes
# don't invalidate the source layer and vice versa.
COPY gradlew .
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew --version --no-daemon

# Build the fat JAR. local.properties is absent → Android module is skipped.
COPY . .
RUN ./gradlew :server:shadowJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/server/build/libs/server-all.jar server-all.jar
EXPOSE 8080
CMD ["java", "-jar", "server-all.jar"]
