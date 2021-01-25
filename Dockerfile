FROM alpine/git
WORKDIR /app
RUN git clone --depth 1 --branch "master" https://github.com/maqroll/vertx.git

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY --from=0 /app/vertx /app
RUN mvn install

FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=1 /app/target/starter-1.0.0-SNAPSHOT-fat.jar /app
CMD ["java -jar starter-1.0.0-SNAPSHOT-fat.jar"]
