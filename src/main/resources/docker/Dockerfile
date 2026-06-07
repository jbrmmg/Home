FROM eclipse-temurin:17-jre
WORKDIR /app

RUN mkdir -p /var/log/jbr

COPY target/MiddleTier-Home-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=pdn

VOLUME /var/log/jbr

EXPOSE 12036

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
