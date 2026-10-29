FROM eclipse-temurin:11-jre

ARG APP
WORKDIR /app

COPY ${APP}/build/dist/${APP}.jar app.jar
COPY ${APP}/build/dist/libs/ libs/

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
