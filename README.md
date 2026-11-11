# Shipping Rate Comparison

A simplified shipping rate comparison module built with Java 21 and Spring Boot.
It uses the ShipTime sandbox API to get rates, lets the user pick a carrier service,
and creates a shipment.

> Work in progress.

## Requirements

- Java 21
- ShipTime sandbox credentials

## Configuration

Credentials are read from environment variables. They are never committed.

| Variable | Description |
|---|---|
| `SHIPTIME_BASE_URL` | Base URL of the ShipTime sandbox REST API |
| `SHIPTIME_USERNAME` | Sandbox username (Basic Auth) |
| `SHIPTIME_PASSWORD` | Sandbox password (Basic Auth) |

## Run

```bash
./gradlew bootRun
```

Then open http://localhost:8080.

## Test

```bash
./gradlew test
```
