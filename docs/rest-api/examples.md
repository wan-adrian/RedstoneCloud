# REST API Examples

Set environment variables first:

```bash
export RC_API="http://127.0.0.1:8080/api/v1"
export RC_TOKEN="YOUR_TOKEN"
```

## Check health

```bash
curl -s "$RC_API/health"
```

## Inspect token identity

```bash
curl -s \
  -H "Authorization: Bearer $RC_TOKEN" \
  "$RC_API/me"
```

## List servers

```bash
curl -s \
  -H "Authorization: Bearer $RC_TOKEN" \
  "$RC_API/servers"
```

## Get one server

```bash
curl -s \
  -H "Authorization: Bearer $RC_TOKEN" \
  "$RC_API/servers/Lobby-1"
```

## Start a server from template

```bash
curl -s -X POST \
  -H "Authorization: Bearer $RC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"template":"Lobby"}' \
  "$RC_API/servers/start"
```

Start with a fixed ID:

```bash
curl -s -X POST \
  -H "Authorization: Bearer $RC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"template":"Lobby","id":3}' \
  "$RC_API/servers/start"
```

## Stop a server

```bash
curl -s -X POST \
  -H "Authorization: Bearer $RC_TOKEN" \
  "$RC_API/servers/Lobby-3/stop"
```

## Kill a server

```bash
curl -s -X POST \
  -H "Authorization: Bearer $RC_TOKEN" \
  "$RC_API/servers/Lobby-3/kill"
```

## Execute a command on server

```bash
curl -s -X POST \
  -H "Authorization: Bearer $RC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"command":"say deployed via api"}' \
  "$RC_API/servers/Lobby-3/execute"
```

## List players

```bash
curl -s \
  -H "Authorization: Bearer $RC_TOKEN" \
  "$RC_API/players"
```

## List templates

```bash
curl -s \
  -H "Authorization: Bearer $RC_TOKEN" \
  "$RC_API/templates"
```

## Cloud Console Command Examples

Use these in the cloud process console:

```text
restapi status
restapi token list
restapi token add monitor
restapi perm add monitor cloud.read
restapi perm add monitor cloud.player.read
restapi token rotate monitor
restapi token disable monitor
restapi token enable monitor
restapi token remove monitor
```
