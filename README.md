# Shipping Rate Comparison

A small web app that compares shipping rates with the ShipTime sandbox API.
It is built with Java 21 and Spring Boot 3.5.

## What it does

1. **Package details** – Enter the origin address, the destination address,
   and the package size and weight.
2. **Choose a service** – See the rates from all carriers, sorted by price or by transit time,
   and pick one.
3. **Shipment** – Create the shipment. The app shows the shipment ID,
   the tracking number, and a link to the shipping label (PDF).

The app uses three ShipTime endpoints:

| Step | Endpoint | Purpose |
|---|---|---|
| 1–2 | `POST /rest/rates` | Get rates for the package |
| 3 | `POST /rest/shipments` | Create a shipment with the chosen rate |
| 3 | `GET /rest/shipments/{shipId}/label` | Download the label PDF |

## Requirements

- Java 21
- ShipTime sandbox credentials

## Configuration

The app reads the credentials from environment variables.
Credentials are never stored in the code or in Git.
Use the sandbox credentials from the ShipTime project brief.

| Variable | Required | Description |
|---|---|---|
| `SHIPTIME_USERNAME` | Yes | Sandbox username (Basic Auth) |
| `SHIPTIME_PASSWORD` | Yes | Sandbox password (Basic Auth) |
| `SHIPTIME_BASE_URL` | No | Default: `https://sandboxrestapi.shiptime.com` |

Without credentials, the app still starts. It shows a "service not available" message
when you ask for rates.

## Run

macOS / Linux:

```bash
export SHIPTIME_USERNAME=your-username
export SHIPTIME_PASSWORD=your-password
./gradlew bootRun
```

Windows (PowerShell):

```powershell
$env:SHIPTIME_USERNAME="your-username"
$env:SHIPTIME_PASSWORD="your-password"
.\gradlew.bat bootRun
```

Then open http://localhost:8080.

If port 8080 is busy, use another port:

```bash
./gradlew bootRun --args="--server.port=8089"
```

## Test

```bash
./gradlew test
```

The tests do not call ShipTime. They use a mock server and mock services.

## Project structure

```
src/main/java/com/hyojinahn/shiptime
├── config    How to connect to ShipTime: base URL, Basic Auth, timeouts
├── client    Calls the ShipTime API, and the request/response classes (dto)
├── service   Business logic: build requests, sort rates, create shipments
└── web       Controllers and form classes for the pages

src/main/resources
├── templates  Thymeleaf pages
├── static     CSS and JavaScript
└── application.yml
```

The service has its own input and output classes. It takes `RateInput`, not the form classes,
and it turns ShipTime's responses into `RateOption` and `ShipmentResult`.

## Design decisions

### HTTP client in a config class
`ShipTimeClientConfig` creates the `RestClient` with the base URL, Basic Auth and timeouts.
`ShipTimeClient` only decides what to call. The client has one constructor,
so the tests can pass in a `RestClient` that is bound to a mock server.

### Settings vs. code
Things that change by environment (base URL, credentials, timeouts) are in configuration.
Things that are part of the API contract (endpoint paths, request fields, fixed values
like `PACKAGE` and `METRIC`) are in the code.

### Input validation
The form is checked on the server with Bean Validation before ShipTime is called.
The rules follow the ShipTime API (name length, postal code, 10-digit phone, decimal places).
If a field is wrong, the error is shown next to the field and ShipTime is not called.

For convenience, the page also formats the postal code (`L6J 1H6`) and
the phone number (`(905) 555-1234`) while you type. The server still checks every value.

### Error handling
| ShipTime response | What the user sees |
|---|---|
| 404 on rates | "No rates found" – this is a normal answer, not an error |
| 400 | ShipTime's own messages, so the user can fix the input |
| 401, 5xx, timeout, network error | "Service not available" – details go to the log only |

### Sorting
The user can sort the rates in two ways (`RateSort`):
- **Cheapest** (default): total price with taxes, then transit days.
- **Fastest**: transit days, then price. Rates without transit days come last.

The cheapest rate can be very slow (for example, 98 days), so the user can also sort by speed.
Prices come in cents and are converted with `BigDecimal`, not `double`, to avoid rounding errors.

### No double shipments
Creating a shipment costs money, so it must not happen twice.
- After the form is posted, the app redirects to a result page (Post/Redirect/Get).
  Refreshing the page does not send the form again.
- The quote is removed from the session after the shipment is created.
- The page only sends the quote ID. The carrier, service and rate request
  come from the quote saved on the server.

### Label download
ShipTime's label URL needs our credentials, so the browser cannot open it directly.
The app downloads the label on the server and returns the PDF.
- The URL is built from the configured base URL and the shipment ID.
  The app does not follow the URL in the response, so the credentials only go to ShipTime.
- A user can only download labels for shipments created in their own session.
  Other shipment IDs return 404.

## Limitations

- Canada only. The country is fixed to `CA`, and all prices are in CAD.
  International shipments would need customs details and currency conversion before sorting.
- One package per shipment.
- The ship date is always today.
- Quotes and shipment IDs are kept in the HTTP session. They are lost when the session
  ends or the server restarts. A real service would save shipments in a database
  with the user who created them.
- No login. The session is the only way to tell users apart.
