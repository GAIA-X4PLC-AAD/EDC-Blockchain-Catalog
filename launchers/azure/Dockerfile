FROM gradle:8.1.1-jdk17-alpine AS build
COPY --chown=gradle:gradle ../.. /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle launchers:azure:build

FROM eclipse-temurin:17-jre-alpine

ENV JAR_NAME "edc-tu-berlin.jar"
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

COPY --from=build ./home/gradle/src/launchers/azure/build/libs/$JAR_NAME .

RUN find / -name "logging.properties" -print

COPY ./home/gradle/src/resources/logging/logging.properties .

ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -Djava.util.logging.config.file=logging.properties -jar $JAR_NAME"]
