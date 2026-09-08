# API-2008: a conflicting order preserves the active ride and succeeds after cancellation

| Field | Value |
|---|---|
| Layer | API |
| Allure ID | `2008` |
| Test | `tests.RideConflictRecoveryTest.testConflictingOrderPreservesActiveRideAndAllowsRetryAfterCancellation` |
| Objective | A rejected second order leaves the active ride unchanged; cancelling that ride allows the same second order to succeed. |
| Preconditions | One authorized sandbox session with no active ride and all simulated states disabled. Use the same session and bearer token for every request. |
| Contract | `fake-api/openapi.yaml`: `POST /rides`, `GET /rides/active`, `POST /rides/{id}/cancel`. |

## Request data

| Order | `from` | `to` | `rideOptionId` |
|---|---|---|---|
| A | `Oak Avenue` | `Market Street` | `1` (Yellow) |
| B | `City Center` | `Airport Terminal` | `2` (Turquoise) |

## Steps and expected results

| Step | Request | Expected result |
|---|---|---|
| 1 | `POST /rides` with order A | HTTP `201`. A positive ride ID, A's route, tariff ID `1`, and status `driver_found`. Retain the created ride's ID. |
| 2 | `POST /rides` with order B while A is active | HTTP `409`. Error body: `{"error":"An active ride already exists","code":"ACTIVE_RIDE_EXISTS"}`. |
| 3 | `GET /rides/active` | HTTP `200`. The same ride ID as step 1, A's route, tariff ID `1`, and status `driver_found`. |
| 4 | `POST /rides/{id}/cancel` using A's ID | HTTP `200`. The response identifies A and has status `cancelled`. |
| 5 | `GET /rides/active` | HTTP `404`. Error body: `{"error":"No active ride"}`. |
| 6 | Repeat `POST /rides` with the same order B data | HTTP `201`. A positive ID different from A's ID, B's route, tariff ID `2`, and status `driver_found`. |
| 7 | `GET /rides/active` | HTTP `200`. The same ride ID as step 6, B's route, tariff ID `2`, and status `driver_found`. |

The existing test base resets the sandbox after execution. There must be no reset, session replacement, or alternate authorization between the scenario steps.
