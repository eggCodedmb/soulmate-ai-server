# Runtime stage for SoulMate AI
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY soulmate-app/target/*.jar app.jar
RUN mkdir -p /app/data /app/logs
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:+$JAVA_OPTS} -Dspring.profiles.active=docker -jar app.jar"]

