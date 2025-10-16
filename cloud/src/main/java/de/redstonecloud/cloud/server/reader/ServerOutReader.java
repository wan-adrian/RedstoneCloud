package de.redstonecloud.cloud.server.reader;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.scheduler.task.TaskHandler;
import de.redstonecloud.cloud.server.Server;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads and logs output from a server's stdout stream.
 * Supports both file logging and console logging with automatic flushing.
 */
@Builder
@Log4j2
public class ServerOutReader extends Thread {

    private static final int WRITER_FLUSH_INTERVAL_MS = 5000;
    private static final String BUFFER_LOG_FILENAME = "buffer_console.log";

    @Getter
    private final Server server;

    @lombok.Builder.Default
    private final AtomicBoolean running = new AtomicBoolean(true);

    @lombok.Builder.Default
    private final AtomicBoolean logToConsole = new AtomicBoolean(false);

    @lombok.Builder.Default
    private final Set<String> lastMessages = ConcurrentHashMap.newKeySet();

    private volatile File logFile;
    private volatile BufferedWriter writer;
    private volatile ServerErrorReader errorReader;
    private volatile TaskHandler<?> writerTask;

    @Override
    public void run() {
        try {
            initializeLogFile();
            startErrorReader();
            initializeWriter();
            schedulePeriodicFlush();
            readServerOutput();
        } catch (Exception e) {
            log.error("Error in ServerOutReader for server {}", server.getName(), e);
        } finally {
            cleanupResources();
        }
    }

    private void initializeLogFile() {
        logFile = new File(server.getDirectory(), BUFFER_LOG_FILENAME);
        try {
            Path logPath = logFile.toPath();
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath.getParent());
                Files.createFile(logPath);
            }
            log.debug("Log file initialized for server {}: {}", server.getName(), logFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create log file for server {}", server.getName(), e);
        }
    }

    private void startErrorReader() {
        errorReader = ServerErrorReader.builder()
                .logger(this)
                .build();
        errorReader.start();
    }

    private void initializeWriter() {
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(logFile),
                            StandardCharsets.UTF_8
                    )
            );
            lastMessages.clear();
            log.debug("Writer initialized for server {}", server.getName());
        } catch (IOException e) {
            log.error("Failed to initialize writer for server {}", server.getName(), e);
        }
    }

    private void schedulePeriodicFlush() {
        writerTask = RedstoneCloud.getInstance().getScheduler().scheduleRepeatingTask(() -> {
            flushWriter();
        }, WRITER_FLUSH_INTERVAL_MS);
    }

    private void flushWriter() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                log.warn("Failed to flush writer for server {}", server.getName(), e);
            }
        }
    }

    private void readServerOutput() {
        Process process = server.getProcess();
        if (process == null) {
            log.warn("Cannot read output: process is null for server {}", server.getName());
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                processOutputLine(line);
            }
        } catch (IOException e) {
            if (running.get()) {
                log.debug("Stream closed for server {}", server.getName());
            }
        }
    }

    private void processOutputLine(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }

        // Log to console if enabled
        if (logToConsole.get()) {
            log.info("[{}] {}", server.getName(), line);
        }

        // Write to file
        writeToFile(line);

        // Store in memory
        lastMessages.add(line);
    }

    private void writeToFile(String line) {
        if (writer != null) {
            try {
                writer.write(line);
                writer.newLine();
            } catch (IOException e) {
                log.debug("Failed to write line to file for server {}", server.getName());
            }
        }
    }

    private void cleanupResources() {
        running.set(false);

        if (writerTask != null) {
            writerTask.cancel();
        }

        if (errorReader != null) {
            errorReader.cancel();
        }

        closeWriter();

        if (logToConsole.get()) {
            RedstoneCloud.getInstance().setCurrentLogServer(null);
        }

        server.setLogger(null);
        log.debug("ServerOutReader cleaned up for server {}", server.getName());
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log.error("Failed to close writer for server {}", server.getName());
            }
        }
    }

    /**
     * Stops the reader and cleans up all resources.
     */
    public void cancel() {
        if (!running.getAndSet(false)) {
            return; // Already cancelled
        }

        cleanupResources();
        this.interrupt();
    }

    /**
     * Enables console logging and outputs all previously buffered content.
     */
    public void enableConsoleLogging() {
        if (logToConsole.getAndSet(true)) {
            return; // Already enabled
        }

        outputBufferedContent();
    }

    private void outputBufferedContent() {
        Set<String> outputLines = new LinkedHashSet<>();

        // Read from file first
        if (logFile != null && logFile.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new FileReader(logFile, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[{}] {}", server.getName(), line);
                    outputLines.add(line);
                }
            } catch (IOException e) {
                log.error("Failed to read buffered log for server {}", server.getName(), e);
            }
        }

        // Output any messages not yet in the file
        for (String msg : lastMessages) {
            if (!outputLines.contains(msg)) {
                log.info("[{}] {}", server.getName(), msg);
            }
        }
    }

    /**
     * Disables console logging.
     */
    public void disableConsoleLogging() {
        logToConsole.set(false);
    }

    /**
     * Checks if console logging is enabled.
     *
     * @return true if console logging is enabled
     */
    public boolean isConsoleLogging() {
        return logToConsole.get();
    }

    /**
     * Gets the current writer instance.
     *
     * @return the BufferedWriter, or null if not initialized
     */
    public BufferedWriter getWriter() {
        return writer;
    }

    /**
     * Adds a message to the in-memory buffer.
     *
     * @param msg the message to add
     */
    public void addMessage(String msg) {
        if (msg != null && !msg.isEmpty()) {
            lastMessages.add(msg);
        }
    }

    /**
     * Gets the log file for this reader.
     *
     * @return the log file
     */
    public File getLogFile() {
        return logFile;
    }
}

