# REST API Overview

RedstoneCloud includes an **optional embedded REST API** in the cloud module.

It is designed for:

- external dashboards
- automation scripts
- CI/CD workflows that need to control server lifecycle
- read-only monitoring tools

The API is disabled by default and must be enabled in `config.yml`.

## Runtime Characteristics

- Implementation: JDK `HttpServer` (no external web framework dependency)
- Base path: `/api/v1`
- Binding: configurable host + port
- Auth: token-based (`Bearer` or `X-Api-Token`)
- Access control: permission-based per endpoint

## Lifecycle

- Starts during cloud boot when `restApi.enabled: true`
- Skips startup if no valid tokens are configured
- Stops during cloud shutdown
- Token/permission updates can be applied live with the cloud console `restapi` command

## Health and Identity Routes

- `GET /api/v1/health`: liveness + cloud running flag
- `GET /api/v1/me`: validates token and returns token name + effective permissions

## Security Notes

- Keep the API bound to `127.0.0.1` unless remote access is required.
- If exposed remotely, place it behind a reverse proxy/firewall.
- Replace default placeholder token values before enabling.
- Use scoped tokens (least privilege) for integrations.
