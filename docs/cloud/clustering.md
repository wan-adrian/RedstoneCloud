# Clustering (Master + Nodes)

RedstoneCloud supports a single **master** (cloud) instance with multiple **node** instances. All instances must use the **same Redis** server and DB.

## Requirements

- One cloud instance (master).
- One or more node instances (slaves).
- All instances must connect to the same Redis host, port, and DB.
- The cluster gRPC port must be reachable from nodes.

## Data Model

- The master is the source of truth for templates and types.
- Nodes execute server processes and report runtime state back to the master.
- Redis is shared by all instances; it must point to the same DB ID on all hosts.

## Security Model

- Nodes authenticate with a login request and receive a session token.
- All cluster messages are validated against that token.
- The master only accepts nodes listed in `cluster.nodes`.

## Master Setup (Cloud)

1. Configure Redis in `config.yml`.
2. Register nodes in `cluster.nodes` with their IDs and names.
3. Start the cloud instance and ensure the cluster port is open.

Example:

```yml
cluster:
  port: 6854
  nodes:
    - name: "Node-1"
      id: "<node-id>"
    - name: "Node-2"
      id: "<node-id>"
```

## Node Setup

1. Run the node setup wizard.
2. Provide the master IP/port and node ID.
3. Ensure the node Redis settings match the master.

On login, the master will push Redis settings to the node. The node updates its config if needed, so Redis stays consistent across the cluster.

## Runtime Behavior

- Nodes keep running their servers if the master goes offline.
- Nodes buffer cluster events while disconnected and flush them on reconnect.
- On reconnect, nodes send a full server state sync so the master can reconcile.

### Reconciliation Rules

- If a server exists on the node but not on the master, the master creates it.
- If a server exists on the master for a node but is missing from the node sync, the master removes it and clears the cache entry.
- Server status, port, and address are updated from the node snapshot.

## Troubleshooting

- If a node cannot connect, verify `cluster.port` is open and the node ID is in `cluster.nodes`.
- If nodes start but servers don’t appear, check Redis connectivity and DB ID.
- If a node repeatedly reconnects, check token mismatch logs and network stability.

## Notes

- Unknown node IDs are rejected by the master.
- Nodes must authenticate on the cluster stream with the session token issued at login.
- Server lifecycle events are synchronized between master and nodes over gRPC.

## Recovery Behavior

- Nodes keep running their servers if the master goes offline.
- Nodes buffer cluster events while disconnected and flush them on reconnect.
- On reconnect, nodes send a full server state sync so the master can reconcile.
