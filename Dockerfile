FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY server/pom.xml server/
COPY server/src server/src

RUN apt-get update && \
    apt-get install -y maven && \
    mvn -f server/pom.xml clean package -DskipTests && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:17-jre

#some notes

WORKDIR /app

COPY --from=build /app/server/target/mergesort-server-app-1.0.0.jar /app/server.jar

EXPOSE 12345

ENTRYPOINT ["java", "-jar", "/app/server.jar"]
