FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Python and create a venv
RUN apt-get update -q && apt-get install -y -q python3 python3-venv && apt-get clean && rm -rf /var/lib/apt/lists/*
RUN python3 -m venv /app/venv

# Install Python dependencies
COPY src/main/resources/web/requirements.txt web/requirements.txt
RUN /app/venv/bin/pip install --no-cache-dir -r web/requirements.txt

# Copy Java jar
COPY target/MiddleTier-Home-*.jar app.jar

# Copy web app
COPY src/main/resources/web/ web/

# Copy startup script
COPY src/main/resources/docker/start.sh start.sh
RUN chmod +x start.sh

VOLUME /var/log/jbr

EXPOSE 12036
EXPOSE 1025
EXPOSE 8080

ENTRYPOINT ["/app/start.sh"]
