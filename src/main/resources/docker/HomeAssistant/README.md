# Home Assistant Docker Setup

## Prerequisites

- Docker installed with the user in the `docker` group
- Docker Compose (v2, via `docker compose`)

## Starting Home Assistant

```bash
docker compose up -d
```

Home Assistant will be available at http://localhost:8123. Config is persisted to `~/homeassistant/config/`.

## Initial Setup

1. Open http://localhost:8123 and create your admin account
2. Set your home name and location
3. Home Assistant will auto-discover devices on your network

## Integrations

### FRITZ!Box

- Go to **Settings → Devices & Services** and find the FRITZ!Box entry
- Create a dedicated user on the FRITZ!Box (**System → FRITZ!Box Users → Add User**) with a strong password and restricted permissions
- Enter those credentials when prompted in Home Assistant
- Useful for device tracking (presence detection) and network stats
- FRITZ!DECT smart home devices (plugs, radiator valves) will appear here if you have any

### Octopus Energy

- Install via **Settings → Marketplace** (search for the BottlecapDave integration)
- Get your API key from octopus.energy: **Account → Personal Details → API Access**
- Provides usage/cost data and account balance
- If on Agile or Intelligent tariff, enables automations based on dynamic pricing

## API Access

Home Assistant has a built-in REST API.

Generate a token: **Profile → Long-Lived Access Tokens → Create Token**

```bash
# Get all entity states
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8123/api/states

# Get a specific entity
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8123/api/states/sensor.some_entity

# Trigger a service
curl -X POST -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8123/api/services/light/turn_on
```

Real-time events are also available via WebSocket.
