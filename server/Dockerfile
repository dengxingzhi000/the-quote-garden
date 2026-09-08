# syntax=docker/dockerfile:1.6

# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Cache Gradle wrapper + dependencies first.
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --version --no-daemon

# Copy the rest of the server sources and build the fat jar.
COPY . .
RUN ./gradlew :bootJar --no-daemon

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /src/build/libs/server-*.jar /app/server.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENV SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -jar /app/server.jar"]