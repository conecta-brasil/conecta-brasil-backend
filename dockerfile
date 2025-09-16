# Build do JAR dentro da imagem (pode manter assim p/ simplificar)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -DskipTests package

# Runtime leve
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/target/*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Dserver.port=8080"
EXPOSE 8080
CMD ["java","-jar","/app/app.jar"]