# Plugin System Overview

RedstoneCloud loads plugins from the `plugins/` directory in the cloud working directory.

## Discovery

- All `*.jar` files under `plugins/` are scanned.
- JARs inside server-type specific folders are skipped.
- Plugin metadata is read from `redstonecloud.yml` or `plugin.yml` in the JAR.

## Server-Type Specific Folders

Folders named after server types (e.g., `Lobby`, `Proxy`) are considered server-specific and are ignored by the cloud plugin loader.
