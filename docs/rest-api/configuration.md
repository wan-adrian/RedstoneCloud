# REST API Configuration

The REST API is configured via the `restApi` section in `config.yml`.

## Full Example

```yml
restApi:
  enabled: true
  host: "127.0.0.1"
  port: 8080
  tokens:
    - name: "admin"
      token: "REPLACE_WITH_LONG_RANDOM_TOKEN"
      permissions:
        - "*"
      enabled: true
    - name: "monitoring"
      token: "ANOTHER_LONG_RANDOM_TOKEN"
      permissions:
        - "cloud.read"
        - "cloud.player.read"
      enabled: true
```

## Fields

### `restApi.enabled`

- Type: `boolean`
- Default: `false`
- Effect: Enables/disables REST API startup.

### `restApi.host`

- Type: `string`
- Default: `127.0.0.1`
- Effect: Network interface/address used for binding.

### `restApi.port`

- Type: `int`
- Default: `8080`
- Effect: TCP port the API listens on.

### `restApi.tokens`

List of token entries used for authentication and authorization.

Each token object supports:

- `name` (`string`): human-readable identifier for logs/admin
- `token` (`string`): secret token value
- `permissions` (`list<string>`): granted permissions
- `enabled` (`boolean`): whether token can authenticate

## Token Validation Rules

A token entry is ignored if:

- `enabled` is `false`
- `token` is blank
- `token` equals `CHANGE_ME` (default placeholder)

If all tokens are ignored, REST API startup is skipped.
