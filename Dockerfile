# Multi-stage build for SoulMate AI
FROM maven:3.9-eclipse-temurin-21 AS builder

# Configure Alibaba Cloud Maven mirror for faster dependency downloads
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'SETTINGS'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>Alibaba Cloud Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
SETTINGS

WORKDIR /build
COPY pom.xml .
COPY soulmate-common/pom.xml soulmate-common/
COPY soulmate-domain/pom.xml soulmate-domain/
COPY soulmate-mapper/pom.xml soulmate-mapper/
COPY soulmate-service/pom.xml soulmate-service/
COPY soulmate-ai/pom.xml soulmate-ai/
COPY soulmate-web/pom.xml soulmate-web/
COPY soulmate-app/pom.xml soulmate-app/
COPY . .
RUN mvn clean package -DskipTests -pl soulmate-app -am -B -U

# Runtime stage
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/soulmate-app/target/*.jar app.jar
RUN mkdir -p /app/data /app/logs
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:+$JAVA_OPTS} -Dspring.profiles.active=docker -jar app.jar"]
