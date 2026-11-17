# Build from the repository root:  docker build -f api-gateway/Dockerfile -t api-gateway .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY task-service/pom.xml task-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -q -B -pl api-gateway -am dependency:go-offline
COPY api-gateway/src api-gateway/src
RUN mvn -q -B -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 app
USER app
WORKDIR /app
COPY --from=build /src/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
