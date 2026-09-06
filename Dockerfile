FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates git \
    && rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
