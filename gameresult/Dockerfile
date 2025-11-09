
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/gameresult-1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]