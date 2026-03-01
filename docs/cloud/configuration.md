# Cloud Configuration Reference

Main file: `config.yml`

## Root Fields

### `startMethod`

- Type: enum
- Options: `SUBPROCESS`, `SCREEN`
- Default: `SUBPROCESS`

### `redis`

- `internalInstance` (`boolean`, default `true`)
- `ip` (`string`, default `127.0.0.1`)
- `port` (`int`, default `6379`)
- `dbId` (`int`, default `0`)

### `bridge`

- `hubTemplate` (`string`, default `Lobby`)
- `hubDescription` (`string`)
- `hubNotAvailable` (`string`)

### `cluster`

For now, leave unused in cloud-only mode.

- `port` (`int`, default `6854`)
- `nodes` (`list`, default empty)

### `restApi`

See `docs/rest-api/configuration.md`.

- `enabled`
- `host`
- `port`
- `tokens[]`

### `debug`

- Type: `boolean`
- Default: `false`
- Enables extra debug logging when true.

## Minimal Cloud-only Example

```yml
startMethod: SUBPROCESS
redis:
  internalInstance: true
  ip: "127.0.0.1"
  port: 6379
  dbId: 0
bridge:
  hubTemplate: "Lobby"
  hubDescription: "Go back to the lobby server"
  hubNotAvailable: "There is no hub server available at the moment."
cluster:
  port: 6854
  nodes: []
restApi:
  enabled: false
  host: "127.0.0.1"
  port: 8080
  tokens: []
debug: false
```
