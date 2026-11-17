# Build from the repository root:  docker build -f task-service/Dockerfile -t task-service .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY task-service/pom.xml task-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -q -B -pl task-service -am dependency:go-offline
COPY task-service/src task-service/src
RUN mvn -q -B -pl task-service -am package -DskipTests

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 app
USER app
WORKDIR /app
COPY --from=build /src/task-service/target/task-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
