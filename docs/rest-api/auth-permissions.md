# REST API Authentication and Permissions

## Authentication

Provide one of the following headers:

- `Authorization: Bearer <token>` (recommended)
- `X-Api-Token: <token>`

Missing or invalid token returns HTTP `401`.

## Permission Model

Each endpoint checks for required permissions.

Available permission keys:

- `cloud.read`
- `cloud.player.read`
- `cloud.server.manage`
- `cloud.server.execute`
- `*` (wildcard/full access)

Missing required permission returns HTTP `403`.

## Permission Design Recommendations

- Use dedicated tokens per integration.
- Prefer narrowly scoped permissions over `*`.
- Rotate tokens periodically.
- Keep tokens out of source control and logs.

## Suggested Token Profiles

### Read-only monitoring

- `cloud.read`
- `cloud.player.read`

### Server operator automation

- `cloud.read`
- `cloud.server.manage`
- optionally `cloud.server.execute`

### Full admin

- `*`

## Runtime Token Management (Cloud Console)

When REST API is enabled, use the `restapi` command in the cloud console:

- `restapi token list`
- `restapi token add <name> [token]`
- `restapi token rotate <name>`
- `restapi token remove <name>`
- `restapi token enable <name>`
- `restapi token disable <name>`
- `restapi token show <name>`
- `restapi perm list <name>`
- `restapi perm add <name> <permission>`
- `restapi perm remove <name> <permission>`

Changes are persisted to `config.yml` and token cache is reloaded live.
