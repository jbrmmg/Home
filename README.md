# Home

A Spring Boot middle-tier service that integrates several home utilities:

- **TFL route planner** — fetches Transport for London line data and calculates routes between stations using Dijkstra's algorithm
- **Tube Quiz Helper** — a Flask web UI for narrowing down a mystery station by entering stop/zone clues
- **Octopus Energy** — retrieves gas and electricity meter readings via the Octopus Energy API
- **SMTP relay** — a simple local SMTP server that accepts and forwards emails to an external mail host

## Tech Stack

- Java 17
- Spring Boot 3.x (Undertow, JPA, Liquibase)
- Python 3 + Flask + Gunicorn (web UI)
- OpenAPI/Swagger UI (`/swagger-ui.html`)

## Tube Quiz Helper

The container also runs a Flask web app on port 8080 (mapped to **5002** externally in production). It is a dark-themed single-page application with two tabs:

**Quiz Helper tab**
- Shows TFL route data status and lets you trigger a refresh
- Accepts guesses: a station name plus the number of stops and zones the quiz returned
- Displays the intersected set of possible stations in both the TFL view and the merged (quiz) view, colour-coded by which view(s) each station appears in
- Provides a collapsible per-guess breakdown for debugging

**Route Explorer tab**
- Enter any two stations to see the shortest path between them
- Displays stop count, zone count, and the full station-by-station path in both the TFL and merged views side-by-side

The Flask app proxies `/api/*` calls to the Java service at `http://localhost:12036/api/v1`.

Source: `src/main/resources/web/`

## REST API

### Transport — base path `/api/v1/transport`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/routes` | Fetch TFL line data and trigger route calculation (async — poll `/routes/status`) |
| GET | `/routes/status` | Check whether TFL and merged route calculations have completed (returns `tflReady`, `mergedReady`, `lastUpdated`) |
| GET | `/stations` | List all station names sorted alphabetically |
| GET | `/stops` | Find stops reachable from a station (`?station=`, `?stops=`, `?zones=`) |
| POST | `/guess` | Add a guess — body `{"station":"…","stops":N,"zones":N}` |
| GET | `/guess` | List all guesses for the current session |
| DELETE | `/guess` | Clear all guesses and start a new session |
| GET | `/guess/results` | Stations satisfying all guesses (TFL and merged views) |
| GET | `/guess/breakdown` | Per-guess match lists before intersection |
| GET | `/explain` | Shortest path between two stations (`?from=`, `?to=`) — returns stops, zones, and full path in both TFL and merged views |

### Energy — base path `/api/v1/energy`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/gas` | Get gas readings (`?Key=`, `?mprn=`, `?serial=`) |
| GET | `/electricity` | Get electricity readings (`?Key=`, `?mpan=`, `?serial=`) |

## Ports

| Port (host) | Container port | Service |
|-------------|---------------|---------|
| 12036 | 12036 | Java Spring Boot API |
| 1025 | 1025 | SMTP relay |
| 5002 | 8080 | Flask web UI |

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

The Dockerfile is at `src/main/resources/docker/Dockerfile`. It:

1. Installs Python 3 and creates a virtualenv at `/app/venv`
2. Installs Flask, Gunicorn, and Requests into the venv
3. Copies the Java JAR and the Flask web app
4. Uses `start.sh` to launch both processes: Gunicorn on port 8080 and the Java service on port 12036

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
