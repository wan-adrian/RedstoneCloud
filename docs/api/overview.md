# API Module Overview

The `api` module is the shared contract/runtime layer used by cloud and node components.

## Main Responsibilities

- shared component interfaces (`ICloudServer`, `ICloudPlayer`)
- shared enums (`ServerStatus`, `ServerActions`)
- Redis cache wrapper and cacheable contract
- Redis pub/sub broker abstraction (`Broker`, `Packet`, `Message`)
- packet registry and default packet registrations
- encryption utilities (`KeyManager`, `KeyCache`)
- common key constants (`Keys`)

## Package Map

- `de.redstonecloud.api.components`: server/player contracts and enums
- `de.redstonecloud.api.components.cache`: JSON payload DTO builders
- `de.redstonecloud.api.redis.cache`: Redis key-value/list convenience wrapper
- `de.redstonecloud.api.redis.broker`: pub/sub broker + response handling
- `de.redstonecloud.api.redis.broker.packet`: typed packet abstraction and registry
- `de.redstonecloud.api.redis.broker.message`: generic message abstraction
- `de.redstonecloud.api.encryption`: RSA key generation and encryption/decryption
- `de.redstonecloud.api.util`: env/system property key constants + helpers

## Build and Tooling

`api/pom.xml` includes:

- Java 21 compilation
- gRPC/protobuf codegen plugins and dependencies
- Gson, Guava, Jedis, FastUtil, Lombok

This module is a dependency of `cloud`, `node`, and `shared` modules.
