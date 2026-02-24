# API Module Integration Guide

## Typical Startup Flow

1. Set redis env vars/system props (`REDIS_IP`, `REDIS_PORT`, `REDIS_DB` or property fallbacks).
2. Build packet registry via `BrokerHelper.constructRegistry()`.
3. Create broker with your route and subscribed routes.
4. Register listeners:
   - `broker.listen(route, packetConsumer)`
   - `broker.listenM(route, messageConsumer)`
5. Create `Cache`/`KeyCache` instances as needed.

## Route Design

- Use clear route names (`cloud`, `node-1`, etc.).
- Keep `to` field lowercase-compatible as broker publish normalizes routes.

## Request/Response Pattern

- Use packet/message ids to correlate callbacks.
- Existing implementation applies a timeout fallback for pending responses.

## Reliability Notes

- Subscriber thread reconnects on failure with backoff.
- Publish errors are currently printed; production wrappers should add structured logging and retry policy.

## Current Repository Status

Before integrating into production, resolve current `api` compile regressions in broker/message/packet accessors/constructors.

## Concrete Example

For a concrete production-style extension of the RC packet system, see `custom-packets.md` in this folder.
