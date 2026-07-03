# Multi-stage build for SoulMate AI
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY soulmate-common/pom.xml soulmate-common/
COPY soulmate-domain/pom.xml soulmate-domain/
COPY soulmate-mapper/pom.xml soulmate-mapper/
COPY soulmate-service/pom.xml soulmate-service/
COPY soulmate-ai/pom.xml soulmate-ai/
COPY soulmate-web/pom.xml soulmate-web/
COPY soulmate-app/pom.xml soulmate-app/
RUN mvn dependency:go-offline -B || true
COPY . .
RUN mvn clean package -DskipTests -pl soulmate-app -am -B

# Runtime stage
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/soulmate-app/target/*.jar app.jar
RUN mkdir -p /app/data /app/logs
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
