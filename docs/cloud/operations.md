# Cloud Operations (Single Instance)

## Boot Sequence Summary

At startup, cloud process:

1. loads config
2. sets redis env/system properties
3. starts internal redis if enabled
4. initializes cache and broker listeners
5. boots managers (players, servers, commands, plugins, events, scheduler)
6. starts optional REST API if enabled
7. registers shutdown hook

## Shutdown Behavior

Shutdown flow includes:

- cancel scheduled tasks
- stop all running servers
- disable plugins
- shutdown event executor
- flush Redis DB (current implementation)
- shutdown broker
- stop scheduler
- stop internal redis (if used)
- stop optional REST API

## Important Operational Notes

- Current shutdown flushes Redis database used by the cloud process.
- In shared Redis deployments, isolate `dbId` to avoid deleting unrelated keys.
- If REST API is enabled with no valid tokens, startup is skipped for that subsystem.
- Keep `debug: false` in production unless troubleshooting.
