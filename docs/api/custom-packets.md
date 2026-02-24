# Custom Packet Integration

## Pattern Summary

A robust way to extend RC packets uses this 4-part model:

1. Define numeric packet IDs.
2. Implement packet classes extending `de.redstonecloud.api.redis.broker.packet.Packet`.
3. Register packet suppliers in the shared `PacketRegistry`.
4. Register runtime handlers and dispatch incoming packets by packet class.

## 1) Packet ID Namespace

Keep IDs grouped by domain, e.g.:

- `1000-1010`: player actions
- `1100-1111`: server management

This avoids collisions and keeps packet types predictable.

## 2) Custom Packet Class Shape

Each packet:

- extends `Packet`
- returns a fixed `NETWORK_ID` from `packetId()`
- serializes fields to a `JsonArray` in stable order
- deserializes in the same order

Example shape:

- `PlayerDispatchCommandPacket` stores `uuid` + `command`
- response packets (e.g., `BooleanResponse`) carry typed values for callback flows

## 3) Registry Extension

`PacketUtils.registerPackets()` can get the existing registry from `Broker.get().getPacketRegistry()` and register all custom packet suppliers.

This layers custom packet types on top of the default RC registry (`BrokerHelper.constructRegistry()`), instead of replacing it.

## 4) Handler Dispatch

`PacketHandler` keeps `Map<Class<? extends Packet>, Supplier<IPacketHandler>>`.

Flow:

- platform loaders register handlers (`PacketHandler.registerHandler(...)`)
- broker listener forwards packet events to `PacketHandler::handle`
- dispatcher resolves packet class and runs the matching handler

This gives platform-specific handling while sharing packet definitions.

## Recommended Boot Order

Recommended startup order:

1. Register packet classes (`PacketUtils.registerPackets()`).
2. Register packet handlers in each runtime/platform loader.
3. Start packet listener using your route subscription bootstrap.

The sequence ensures incoming packets can always be deserialized and routed.

## Route Subscription Strategy

A route bootstrap can subscribe to:

- incremental wildcard prefixes derived from current directory name
- exact application route
- global wildcard route `*`

That supports broad fan-in communication across related services.

## Request/Response Example

Use RC callback packets directly:

- request: `new GetServerAmountPacket(template).send(NumberResponse.class, callback)`
- handler reply: build response packet, set `to` and `sessionId` from request, then `send()`

This follows RC's built-in correlation via `sessionId`.

## Practical Guidance for New Custom Packets

- Reserve packet IDs in a dedicated range per feature domain.
- Keep packet payloads minimal and version-tolerant.
- Always echo `sessionId` and `from`/`to` correctly in responses.
- Register packet types before listeners start.
- Register handlers before first traffic on that route.
