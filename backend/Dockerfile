FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/btc-explainer-0.1.0.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
