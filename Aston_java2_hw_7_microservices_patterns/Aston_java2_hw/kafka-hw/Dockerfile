# syntax=docker/dockerfile:1
# Один Dockerfile на все 5 модулей: какой модуль собирать — задаёт build-arg MODULE
#   docker build --build-arg MODULE=user-service -t user-service .

# ---------- 1. сборка ----------
FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# кэш ~/.m2 переживает пересборки; locked — чтобы параллельные сборки модулей не портили кэш друг другу
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -f pom.xml -pl ${MODULE} package -DskipTests \
 && cp ${MODULE}/target/*.jar /app.jar

# ---------- 2. запуск ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app.jar app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
