FROM gradle:8.1.1-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ls -la
RUN /home/gradle/src/gradlew extensions:transfer:http-push:provider-push-http-backend-service:build

FROM eclipse-temurin:17-jre

COPY --from=build ./home/gradle/src/extensions/transfer/http-push/provider-push-http-backend-service/build/libs/provider-push-http-backend-service.jar app.jar

ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -jar app.jar"]
