FROM gradle:8.1.1-jdk17-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle BlockchainCatalog:blockchain-catalog-azure:build

FROM eclipse-temurin:17-jre-alpine

ENV JAR_NAME "edc-blockchain.jar"

COPY --from=build ./home/gradle/src/BlockchainCatalog/blockchain-catalog-azure/build/libs/$JAR_NAME .

ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -jar $JAR_NAME"]
