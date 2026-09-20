# Credos Settlement

A small Spring Boot service that moves money between accounts and keeps the books consistent.
Every transfer is idempotent, recorded as double-entry ledger lines, and relayed to an external
settlement system through a transactional outbox. Reconciliation checks, both on demand and in
batch, verify that transfers, ledger entries, and the external system agree.

## Stack

- Java 21, Spring Boot 4.1
- Spring Data JPA, Spring Batch (JDBC job repository)
- PostgreSQL 16, Flyway migrations
- Spotless with google-java-format

## How a transfer flows

1. `POST /transfers` with an `Idempotency-Key` header. The key is inserted first, so a replayed
   request returns the original transfer and a replay with different data returns `409`.
2. Both accounts are locked, the balance is moved, and two ledger entries (debit and credit) are
   written in the same transaction.
3. A `SETTLEMENT_REQUESTED` outbox event is saved in that same transaction.
4. A scheduled worker claims the oldest pending event, marks it `PROCESSING`, commits, then calls
   the settlement client outside any transaction, and finally marks it `PROCESSED`.
5. Failures are retried with exponential backoff (up to 5 attempts). Permanent failures, or
   exhausted retries, mark the event `FAILED`.
6. A recovery worker runs every minute. It picks up events stuck in `PROCESSING` for more than
   five minutes, asks the settlement system for their status, and either completes them or
   schedules a retry.

The settlement client is currently an in-memory stub.

## Reconciliation

- **Ledger reconciliation** checks that a transfer has exactly two ledger entries, that the debit
  and credit match the transfer amount, and that the entries sum to zero.
- **External reconciliation** compares the outbox event status with the settlement system and
  reports `MATCHED`, `INTERNAL_AHEAD`, `EXTERNAL_AHEAD`, or `PENDING`.
- **Batch reconciliation** runs the ledger check over every transfer in chunks of 1,000 using
  Spring Batch, and upserts the outcome into `reconciliation_results`.

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/accounts` | Create an account with a name and starting balance |
| `GET` | `/accounts/{id}` | Fetch an account |
| `POST` | `/transfers` | Transfer funds (requires `Idempotency-Key` header) |
| `GET` | `/outbox/failed` | List outbox events that permanently failed |
| `GET` | `/reconciliations/transfers/{transferKey}` | Reconcile one transfer against the ledger |
| `GET` | `/reconciliations/external/transfers/{transferKey}` | Reconcile one transfer against the settlement system |
| `POST` | `/batch/reconciliation/run` | Launch the batch reconciliation job |

Errors are returned as JSON with an HTTP status, an error code such as `INSUFFICIENT_BALANCE` or
`IDEMPOTENCY_CONFLICT`, a message, and a timestamp.

## Running locally

Start PostgreSQL and the application:

```bash
docker compose up -d
./gradlew bootRun
```

Flyway applies the schema on startup. The default datasource points at
`localhost:5432/credos_settlement` with user and password `settlement`.

Example session:

```bash
curl -X POST localhost:8080/accounts \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","balance":100.00}'

curl -X POST localhost:8080/accounts \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bob","balance":0}'

curl -X POST localhost:8080/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-1' \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":25.00}'
```

## Development

```bash
./gradlew spotlessApply   # format sources
./gradlew test            # run tests
```

Compilation runs `spotlessCheck` first, so unformatted code fails the build.

SQL scripts for seeding large data sets and inspecting query plans for the batch job live under
`src/main/java/com/example/credos_settlement/scripts/performance/`.

## Project layout

```
account/          accounts and balance operations
transfer/         idempotent transfer use case
ledger/           double-entry ledger lines
outbox/           outbox events, relay worker, retry policy, stale recovery
settlement/       settlement client interface and stub
reconciliation/   on-demand ledger and external reconciliation
batch/            Spring Batch reconciliation job
common/           API error model and global exception handler
db/migration/     Flyway migrations (V1 through V9)
```
