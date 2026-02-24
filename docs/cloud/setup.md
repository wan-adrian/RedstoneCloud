# Cloud Setup (Single Instance)

This guide covers cloud-only deployment with no node clustering.

## Prerequisites

- Java 21+
- network access for downloading optional default software during setup

## First Start

Run:

```bash
java -jar redstonecloud.jar
```

On first boot (if `config.yml` does not exist), interactive setup runs and asks for:

- Redis mode:
  - internal Redis instance
  - external Redis server (`ip` + `port`)
- optional proxy template bootstrap
- optional default lobby template bootstrap

After confirmation, setup writes `config.yml` and optional default templates/types.

## Cloud-only Recommendation

- Keep clustering disabled (default current flow).
- Run a single cloud process with either internal Redis or external dedicated Redis.
- Bind optional REST API to localhost unless explicitly exposing via reverse proxy.
