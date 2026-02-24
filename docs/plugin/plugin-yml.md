# Plugin Metadata (plugin.yml)

The loader accepts `redstonecloud.yml` or `plugin.yml` inside the JAR.

Required fields:

- `name`: plugin name
- `main`: fully-qualified plugin main class

Optional fields:

- `version`
- `author`
- `depends`: list of plugin names

Example:

```yml
name: ExamplePlugin
main: com.example.cloud.ExamplePlugin
version: 1.0.0
author: YourName
depends:
  - SomeDependency
```
