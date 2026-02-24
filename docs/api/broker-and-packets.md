# Broker and Packet System

## Broker Lifecycle

`Broker` is a singleton-style runtime (`Broker.get()`) that:

- opens a Jedis pool using env/system redis values
- runs a dedicated Redis subscriber thread
- publishes packets/messages asynchronously via a fixed thread pool
- dispatches incoming payloads to registered listeners
- tracks pending responses by message/packet id with timeout fallback

Redis connection values are read from:

- env: `REDIS_IP`, `REDIS_PORT`, `REDIS_DB`
- properties fallback: `redis.bind`, `redis.port`, `redis.db`

## Payload Types

Incoming pub/sub payloads are JSON arrays with first entry type discriminator:

- `"packet"`
- `"message"`

## Packets

`Packet` is an abstract typed transport with:

- `packetId()` network id
- `serialize(JsonArray)`
- `deserialize(JsonArray)`
- metadata: `sessionId`, `from`, `to`

Sending:

- `send()` fire-and-forget
- `send(packetType, callback)` request/response pattern

## Packet Registry

`PacketRegistry` maps network ids to suppliers and can reconstruct packets from incoming payload arrays.

Default registrations are in `BrokerHelper.constructRegistry()`, including:

- communication auth packet
- template lookup/start packets
- player connect/disconnect packets
- server action/status/remove packets

## Generic Messages

`Message` provides a string-argument message envelope with builder helpers and optional callback responses.

### Minimal Message Example

```java
// sender side
new Message.Builder()
        .setTo("service-b")
        .append("PING")
        .send(response -> {
            if (response != null) {
                String[] args = response.getArguments();
                // handle response args
            }
        });
```

```java
// receiver side
Broker.get().listenM("service-b", message -> {
    String[] args = message.getArguments();
    if (args.length > 0 && "PING".equals(args[0])) {
        message.respond("PONG").send();
    }
});
```

## Current Caveat

At the current repository state, the `api` module has compile issues around generated/expected accessor methods in broker/message/packet classes. Treat this documentation as architectural reference; verify implementation state before publishing binaries.
