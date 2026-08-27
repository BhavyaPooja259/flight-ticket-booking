# Flight Ticket Booking API

A small Spring Boot service that books seats on pre-populated flights.

## How to run

Requires Java 17+.

```bash
./mvnw spring-boot:run          # starts on http://localhost:8080
./mvnw test                     # run the tests
./mvnw clean package            # build the jar
```

Flights are loaded into memory at startup (`FlightDataInitializer`):

| Flight number | Capacity |
|---------------|----------|
| AI101         | 150      |
| AI202         | 60       |
| AI303         | 2        |

## Example request / response

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H 'Content-Type: application/json' \
  -d '{"flightNumber":"AI101","passengerName":"Ada Lovelace","numberOfSeats":2}'
```

`201 Created`

```json
{
  "bookingId": "0f1b0c9b-6a26-4f0e-9e1a-2b0f3a7c8d11",
  "flightNumber": "AI101",
  "passengerName": "Ada Lovelace",
  "numberOfSeats": 2
}
```

## Errors

Every failure — validation, unknown flight, full flight, or an unexpected one —
comes back in the same shape:

```json
{ "status": 409, "error": "Conflict", "message": "Insufficient seats on flight AI303 for 3 seat(s)" }
```

Validation failures add a `fieldErrors` map so a client can attach messages to
the right input. `fieldErrors` is omitted from every other response.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "flightNumber must not be blank, numberOfSeats must be at least 1",
  "fieldErrors": {
    "flightNumber": "must not be blank",
    "numberOfSeats": "must be at least 1"
  }
}
```

No response carries a stack trace, exception class, or parser message. An
unexpected failure is logged server-side and returns a bare
`{"status":500,"error":"Internal Server Error","message":"Unexpected error"}`.

| Status | When |
|--------|------|
| 201 Created | booking confirmed |
| 400 Bad Request | a field failed validation, or the body is missing / not valid JSON |
| 404 Not Found | the flight number does not exist |
| 405 Method Not Allowed | wrong HTTP method for the endpoint |
| 409 Conflict | the flight does not have enough seats left |
| 415 Unsupported Media Type | `Content-Type` is not `application/json` |
| 500 Internal Server Error | anything unexpected — details are logged, not returned |

### Validation rules

| Field | Rule | Message |
|-------|------|---------|
| `flightNumber` | required, not null, not blank after trimming | `must not be blank` |
| `passengerName` | required, not null, not blank after trimming | `must not be blank` |
| `numberOfSeats` | required, not null | `is required` |
| `numberOfSeats` | at least 1 | `must be at least 1` |

Missing and `null` are treated the same as each other; `numberOfSeats` is an
`Integer` rather than an `int` precisely so a missing value reports "is
required" instead of silently defaulting to `0`. Strings are trimmed before
validation, so `"   "` is rejected as blank and `" AI101 "` still books flight
`AI101`. A non-numeric `numberOfSeats` is a malformed body, so it is a 400 too.
All failing fields are reported in one response rather than one at a time.

## Design

`BookingController` -> `BookingService` -> `FlightRepository` / `BookingRepository`,
with both repositories backed by a `ConcurrentHashMap`.

**Preventing overbooking.** The availability check and the seat decrement live
together in `Flight.reserveSeats(int)`, which is `synchronized` on the flight
instance. Every request for a flight number resolves to the same `Flight`
object in the repository, so concurrent bookings for one flight serialise on
that object's monitor while bookings for different flights stay independent.
The service never reads availability and then writes it back, so there is no
window for a lost update. The app is a single instance, so this in-process lock
is sufficient — no distributed locking involved.

**Failing after the reservation.** Reserving seats and recording the booking
are two separate steps. If saving the booking fails, the service releases the
seats it just reserved before letting the failure propagate, so seats are never
held by a booking that does not exist. With an in-memory map this is close to
theoretical; against a real datastore it is the difference between a
recoverable error and seats that can never be sold again.

## Assumptions

- Flight creation and search are out of scope, so flights are seeded in memory
  and never change capacity.
- A booking is all-or-nothing: a request for more seats than are left is
  rejected rather than partially filled.
- No persistence — all state is lost on restart.
- No authentication; the passenger name is taken at face value beyond being
  non-blank — no length limit or character rules are imposed.
- `bookingId` is a server-generated UUID; the client does not supply one.
- Requests are not idempotent — a retried request creates a second booking.

## What I would improve with more time

- Persist flights and bookings, and move the concurrency guard to the database
  (an optimistic-locking version column or a conditional `UPDATE ... WHERE
  available_seats >= ?`), which is what makes this correct across more than one
  instance.
- Add the rest of the lifecycle: retrieve a booking, cancel one and release its
  seats.
- Make booking idempotent with a client-supplied idempotency key, so a retry
  after a timeout cannot double-book.
- Add an error code per failure type (e.g. `INSUFFICIENT_SEATS`) so clients can
  branch on something other than the HTTP status and message text.
- Cap `numberOfSeats` at a sane per-booking maximum, and reject unknown JSON
  properties instead of ignoring them.
