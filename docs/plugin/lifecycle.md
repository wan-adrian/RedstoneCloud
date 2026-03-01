# Plugin Lifecycle

## Load Order

- Plugins are discovered and their classloaders are created.
- `onLoad()` is called during plugin loading.
- `onEnable()` is called after all plugins are loaded.

## Dependencies

- Dependencies listed in `depends` are enabled first.
- Missing dependencies prevent a plugin from enabling.
- Circular dependencies are detected and rejected.

## Disable

- `onDisable()` is called on shutdown.
