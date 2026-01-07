package de.redstonecloud.shared.startmethods.impl.screen;

import de.redstonecloud.shared.startmethods.IStartMethod;
import de.redstonecloud.shared.startmethods.impl.screen.reader.ScreenLogReader;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Getter
public class ScreenProcess implements IStartMethod {

    private String directory = "";
    private int port = -1;

    private String screenName;
    private String[] command;
    private Map<String, String> env;

    private Runnable onExit;
    private ScreenLogReader logger;

    private volatile boolean loggingEnabled = false;
    private final AtomicBoolean exited = new AtomicBoolean(false);

    private Thread watchdog;

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void setOnExit(Runnable onExit) {
        this.onExit = onExit;
    }

    @Override
    public void prepare(String sourceDir, String[] command, Map<String, String> env) {
        this.command = command;
        this.env = env;
        this.screenName = "server-" + System.currentTimeMillis();

        createServerDirectory();

        try {
            FileUtils.copyDirectory(new File(sourceDir), new File(directory));
        } catch (IOException e) {
            log.error("Failed copying template", e);
        }
    }

    @Override
    public void start() {
        try {
            String joinedCommand = String.join(" ", command);

            ProcessBuilder pb = new ProcessBuilder(
                    "screen",
                    "-L",
                    "-Logfile", "screen.log",
                    "-dmS", screenName,
                    "bash", "-c", joinedCommand
            );

            pb.directory(new File(directory));
            pb.environment().putAll(env);
            pb.start();

            logger = new ScreenLogReader(this);
            logger.start();

            startWatchdog();

            log.info("Started screen session {}", screenName);

        } catch (IOException e) {
            log.error("Failed to start screen process", e);
        }
    }

    // ================= WATCHDOG =================

    private void startWatchdog() {
        watchdog = new Thread(() -> {
            try {
                while (!exited.get()) {
                    Thread.sleep(1000);

                    if (!isActive()) {
                        handleExit();
                        return;
                    }
                }
            } catch (InterruptedException ignored) {}
        }, "ScreenWatchdog-" + screenName);

        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void handleExit() {
        if (!exited.compareAndSet(false, true)) {
            return;
        }

        log.info("Screen session {} exited", screenName);

        cleanup();

        if (onExit != null) {
            onExit.run();
        }
    }

    // ================= CONTROL =================

    @Override
    public void stop(String stopCommand) {
        writeCommand(stopCommand);
    }

    @Override
    public void writeCommand(String command) {
        try {
            new ProcessBuilder(
                    "screen",
                    "-S", screenName,
                    "-X", "stuff",
                    command + "\n"
            ).start();
        } catch (IOException e) {
            log.error("Failed to send command to screen", e);
        }
    }

    @Override
    public boolean isActive() {
        try {
            Process p = new ProcessBuilder("screen", "-ls").start();
            String output = new String(p.getInputStream().readAllBytes());
            return output.contains("\t" + screenName) || output.contains("." + screenName);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void kill(int timeout) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    new ProcessBuilder(
                            "screen",
                            "-S", screenName,
                            "-X", "quit"
                    ).start();
                } catch (IOException ignored) {}
            }
        }, timeout);
    }

    @Override
    public void cleanup() {
        if (logger != null) {
            logger.cancel();
            logger = null;
        }

        if (watchdog != null) {
            watchdog.interrupt();
            watchdog = null;
        }
    }

    // ================= LOGGING =================

    @Override
    public void enableLogging() {
        if (loggingEnabled) return;

        try {
            new ProcessBuilder(
                    "screen",
                    "-S", screenName,
                    "-X", "log", "on"
            ).start();

            loggingEnabled = true;

            if (logger != null) {
                logger.enableConsoleLogging();
            }

        } catch (IOException e) {
            log.error("Failed to enable logging", e);
        }
    }

    @Override
    public void disableLogging() {
        if (!loggingEnabled) return;

        try {
            new ProcessBuilder(
                    "screen",
                    "-S", screenName,
                    "-X", "log", "off"
            ).start();

            loggingEnabled = false;

            if (logger != null) {
                logger.disableConsoleLogging();
            }

        } catch (IOException e) {
            log.error("Failed to disable logging", e);
        }
    }

    @Override
    public boolean isLoggerEnabled() {
        return loggingEnabled;
    }

    // ================= UTIL =================

    private void createServerDirectory() {
        try {
            Files.createDirectories(Path.of(directory));
        } catch (IOException e) {
            log.error("Failed to create server directory", e);
        }
    }
}
