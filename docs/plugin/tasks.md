# Tasks

Plugins can schedule tasks through the cloud task scheduler. Tasks run on the scheduler thread pool and can be delayed or repeating. All timings are in milliseconds (ms), not ticks.

## Scheduling Runnables

Use `TaskScheduler` for simple tasks:

```java
TaskScheduler scheduler = getCloud().getScheduler();

// Run once immediately
scheduler.scheduleTask(() -> {
    // work
});

// Run once after a delay
scheduler.scheduleDelayedTask(() -> {
    // work
}, 5_000L);

// Run repeatedly
scheduler.scheduleRepeatingTask(() -> {
    // work
}, 1_000L);
```

## Custom Task Class

Extend `Task` if you need lifecycle hooks:

```java
public class CleanupTask extends Task {
    @Override
    protected void onRun(long currentMillis) {
        // work
    }

    @Override
    public void onCancel() {
        // optional cleanup
    }

    @Override
    public void onError(Throwable throwable) {
        // error handling
    }
}

TaskScheduler.getInstance().scheduleRepeatingTask(new CleanupTask(), 30_000L);
```

## Task Handles

Every scheduled task returns a `TaskHandler`. It can be used to cancel or reconfigure timing.

```java
TaskHandler<Runnable> handler = scheduler.scheduleRepeatingTask(() -> {}, 1000L);
handler.cancel();
```
