FROM gradle:8.1.1-jdk17-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle launchers:azure:build

FROM eclipse-temurin:17-jre-alpine

ENV JAR_NAME "edc-tu-berlin.jar"
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

COPY --from=build ./home/gradle/src/launchers/azure/build/libs/$JAR_NAME .

ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -jar $JAR_NAME"]
