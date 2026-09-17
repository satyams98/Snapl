# Stage 1 — build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw -q package -DskipTests

# Stage 2 — run
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
USER app
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=3 \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]