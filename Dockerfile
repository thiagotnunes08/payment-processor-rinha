FROM amazoncorretto:24.0.1-alpine3.21

WORKDIR /app

COPY target/quarkus-app/ /app/

ENV JAVA_OPTS="\
  -XX:+UseSerialGC \
#  -Xms128m \
#  -Xmx128m \
"


ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
