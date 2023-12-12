# Dockerfile for using dynamic allocation of config and vault
FROM gradle:8-jdk17-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle BlockchainCatalog:blockchain-catalog-prosumer:build

FROM eclipse-temurin:17-jre-alpine

COPY --from=build ./home/gradle/src/BlockchainCatalog/blockchain-catalog-prosumer/build/libs/blockchain-catalog-prosumer-all.jar app.jar
COPY --from=build ./home/gradle/src/transfer/transfer-07-provider-push-http/certs/cert.pfx cert.pfx

ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -jar app.jar"]
