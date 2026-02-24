# REST API Endpoint Reference

Base path: `/api/v1`

All responses are JSON.

## Health and Identity

### `GET /health`

- Auth: none
- Purpose: health check

Response:

```json
{
  "status": "ok",
  "running": true
}
```

### `GET /me`

- Auth: required
- Required permissions: none
- Purpose: inspect current token identity

Response:

```json
{
  "tokenName": "admin",
  "permissions": ["*"]
}
```

## Servers

### `GET /servers`

- Required permissions: `cloud.read`
- Purpose: list servers

### `GET /servers/{name}`

- Required permissions: `cloud.read`
- Purpose: get server by name

Returns `404` if server does not exist.

### `POST /servers/start`

- Required permissions: `cloud.server.manage`
- Body:

```json
{
  "template": "Lobby",
  "id": 1
}
```

- `template` is required
- `id` is optional
- Returns `201` with created server payload
- Returns `404` if template not found
- Returns `409` if server could not be started

### `POST /servers/{name}/stop`

- Required permissions: `cloud.server.manage`
- Purpose: graceful stop signal

### `POST /servers/{name}/kill`

- Required permissions: `cloud.server.manage`
- Purpose: force kill workflow

### `POST /servers/{name}/execute`

- Required permissions: `cloud.server.execute`
- Body:

```json
{
  "command": "say hello from api"
}
```

- `command` is required

## Players

### `GET /players`

- Required permissions: `cloud.player.read`
- Purpose: list connected players

## Templates

### `GET /templates`

- Required permissions: `cloud.read`
- Purpose: list template metadata (`name`, type, min/max/running servers, static flag)

## Error Format

Error responses use:

```json
{
  "status": 403,
  "error": "Missing permissions: cloud.server.manage"
}
```
