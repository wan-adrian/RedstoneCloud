# Listeners

Plugins can subscribe to cloud events via the `EventManager`. Subscriptions are lightweight and can be registered during plugin enable.

## Basics

Events live under `de.redstonecloud.cloud.events` and are dispatched through the cloud `EventManager`.

Subscription uses Java consumers:

```java
getCloud().getEventManager().subscribe(ServerStartEvent.class, event -> {
    // handle event
});
```

You can also specify a priority:

```java
getCloud().getEventManager().subscribe(PlayerConnectEvent.class, event -> {
    // handle event early
}, EventPriority.HIGHEST);
```

## Cancellable Events

Some events are cancellable and implement `CancellableEvent`. These can be cancelled by calling `event.setCancelled(true)`.

```java
getCloud().getEventManager().subscribe(ServerCreateEvent.class, event -> {
    if (event.getTemplate().getName().startsWith("test")) {
        event.setCancelled(true);
    }
});
```

## Built-in Events

Common events include:

- `ServerCreateEvent` (cancellable)
- `ServerStartEvent`
- `ServerReadyEvent`
- `ServerExitEvent`
- `PlayerConnectEvent`
- `PlayerDisconnectEvent`
- `PlayerTransferEvent`

Plugins can define and dispatch their own events via `EventManager.callEvent`.

