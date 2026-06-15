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
    port: 1025          # port the local SMTP relay listens on
    max: 10             # max messages before rate-limiting kicks in
    smtp-host: smtp.example.com
    smtp-port: 587
    smtp-sender: sender@example.com
    smtp-password: ${SMTP_PASSWORD}
    key: ${EMAIL_KEY}
```

The following environment variables must be set at runtime:

| Variable | Description |
|----------|-------------|
| `SMTP_PASSWORD` | Password for the outbound SMTP host |
| `EMAIL_KEY` | AES key used for password decryption |

## GitHub Actions Runner

The self-hosted runner is set up using Docker Compose from `src/main/resources/github/docker-compose.yml`.

Two environment files are required in the same directory as the compose file (both are gitignored):

**`.env`** — machine-specific, stable values:
```
RUNNER_NAME=home-prod        # or home-dev on the dev machine
LABELS=self-hosted,home-prod # or self-hosted,home-dev on the dev machine
```

**`.env.secrets`** — the GitHub access token (updated frequently):
```
ACCESS_TOKEN=your-token-here
```

To start the runner:
```bash
cd src/main/resources/github
docker compose up -d
```

The access token can be regenerated from GitHub → Settings → Actions → Runners. Update `.env.secrets` and restart the container (`docker compose restart`) when it changes.

## CI/CD

Pushing to the `Release` branch triggers a GitHub Actions workflow that:

1. Builds the project with Maven
2. Builds a Docker image and pushes it to the Nexus Docker registry tagged with the commit SHA and `latest`
3. Writes a `.env` file with runtime secrets (sourced from GitHub secrets) and deploys via `docker compose`

Required GitHub secrets: `NEXUS_PASSWORD`, `SMTP_PASSWORD`, `EMAIL_KEY`.

To roll back to a previous release, update `docker-compose.yml` to reference the desired commit SHA tag and run `docker compose up -d`.

## Building

```bash
mvn package
```

The build produces a self-contained executable JAR.

## Docker

The Dockerfile is at `src/main/resources/docker/Dockerfile` and runs on port 12036. Logs go to stdout and are accessible via `docker logs`.

To run locally using Docker Compose, create a `.env` file in the project root:

```
SMTP_PASSWORD=your-smtp-password
EMAIL_KEY=your-aes-key
```

Then:

```bash
docker compose up -d
```

Logs are written to stdout and can be viewed with `docker logs home`.
