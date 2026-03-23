FROM eclipse-temurin:17-jdk
WORKDIR /app

# Копируем сертификат (предварительно скачайте его и положите рядом с Dockerfile)
COPY bdu_fstec.crt /tmp/bdu_fstec.crt

# Импортируем сертификат в хранилище доверенных Java
RUN keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -alias bdu_fstec -file /tmp/bdu_fstec.crt && \
    rm /tmp/bdu_fstec.crt

# Копируем JAR-файл
COPY target/asset-service-1.0.0.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]