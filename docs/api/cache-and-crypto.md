# Cache and Crypto

## Cache Layer

`Cache` wraps Jedis operations and provides:

- string set/get
- bulk set/get (`mset`/`mget`)
- list set/get helpers
- key deletion and existence checks
- prefix key discovery (`keys`)

`Cacheable` entities can define a cache key and use shared cache patterns.

Key prefixes from `Keys`:

- `server:`
- `player:`

## Encryption Utilities

`KeyManager`:

- initializes an RSA 4096-bit keypair (`init()`)
- stores public/private keys statically
- encrypts with provided public key
- decrypts with local private key

`KeyCache`:

- persists public keys per route in Redis list entries
- encodes keys with base64 helper (`B64`)
- reconstructs keys with `KeyFactory` + `X509EncodedKeySpec`

## Operational Guidance

- Initialize keys during process boot before encryption/decryption calls.
- Avoid long-term hard-coded key assumptions; keys are runtime-generated unless persisted externally.
- Protect Redis access because cached encryption material is stored there.
