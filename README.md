# Home

A Spring Boot middle-tier service that integrates several home utilities:

- **TFL route planner** — fetches Transport for London line data and calculates routes between stations using Dijkstra's algorithm
- **Octopus Energy** — retrieves gas and electricity meter readings via the Octopus Energy API
- **SMTP relay** — a simple local SMTP server that accepts and forwards emails to an external mail host

## Tech Stack

- Java 17
- Spring Boot 3.0 (Undertow, JPA, Liquibase)
- OpenAPI/Swagger UI (`/swagger-ui.html`)

## REST API

Base path: `/jbr/int/home`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/routes` | Refresh TFL line data and recalculate station routes |
| GET | `/query` | Find reachable stations (`?station=`, `?stops=`, `?zones=`) |
| GET | `/gas` | Get gas readings (`?Key=`, `?mprn=`, `?serial=`) |
| GET | `/electricity` | Get electricity readings (`?Key=`, `?mpan=`, `?serial=`) |

## Configuration

Configuration is via Spring Boot properties under the `home` prefix:

```yaml
home:
  allowed-recipients:
    - user@example.com
  email:
    port: 2525          # port the local SMTP relay listens on
    max: 10             # max messages before rate-limiting kicks in
    smtp-host: smtp.example.com
    smtp-port: 587
    smtp-sender: sender@example.com
    smtp-password: <encrypted>
    key: <AES key for password decryption>
```

## Building

```bash
mvn package
```

The build produces a self-contained executable JAR and a deployment zip (via maven-assembly-plugin).

## Running

```bash
java -jar target/MiddleTier-Home-<version>.jar
```

## Docker

The Dockerfile is at `src/main/resources/docker/Dockerfile` and targets the `pdn` (production) Spring profile on port 12036.

**Build** (requires a packaged JAR — run `mvn package` first):

```bash
docker build -f src/main/resources/docker/Dockerfile -t home .
```

**Run:**

```bash
docker run -d \
  --restart unless-stopped \
  -p 12036:12036 \
  -v /var/log/jbr:/var/log/jbr \
  -e HOME_EMAIL_SMTP_PASSWORD=<your-smtp-password> \
  -e HOME_EMAIL_KEY=<your-aes-key> \
  --name home \
  home
```

The two environment variables override the `home.email.smtp-password` and `home.email.key` properties, which are normally substituted by Maven at build time. Logs are written to `/var/log/jbr/MiddleTier-Home-PDN.log` inside the container; mount a host directory there to persist them.
