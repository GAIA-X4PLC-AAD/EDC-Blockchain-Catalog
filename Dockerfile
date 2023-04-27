FROM eclipse-temurin:17-jre-alpine

COPY ./BlockchainCatalog/blockchain-catalog-prosumer/build/libs/consumer.jar app.jar
COPY ./BlockchainCatalog/blockchain-catalog-prosumer/config.properties config.properties

# Use "exec" for Kubernetes graceful termination (SIGINT) to reach JVM.
ENTRYPOINT [ "sh", "-c", "exec java -Dedc.fs.config=config.properties $JVM_ARGS -jar app.jar"]

#ENTRYPOINT java -jar app.jar

#ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -jar app.jar"]
