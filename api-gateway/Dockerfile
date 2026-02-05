FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/api-gateway-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]