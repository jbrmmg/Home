FROM eclipse-temurin:17-jre
WORKDIR /app

COPY target/MiddleTier-Home-*.jar app.jar

VOLUME /var/log/jbr

EXPOSE 12036
EXPOSE 1025

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
