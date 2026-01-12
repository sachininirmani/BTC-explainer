# ---- Build stage (has Maven built-in) ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- Runtime stage (small, fast) ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/btc-explainer-0.1.0.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
