package de.redstonecloud.shared.startmethods.impl.subprocess.reader;

import de.redstonecloud.shared.startmethods.impl.subprocess.Subprocess;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
    private final Subprocess process;

    @lombok.Builder.Default
    private final AtomicBoolean running = new AtomicBoolean(true);

    @lombok.Builder.Default
    private final AtomicBoolean logToConsole = new AtomicBoolean(false);

    @lombok.Builder.Default
    private final Set<String> lastMessages = ConcurrentHashMap.newKeySet();

    private volatile File logFile;
    private volatile BufferedWriter writer;
    private volatile ServerErrorReader errorReader;
    private volatile Timer writerTask;

    @Override
    public void run() {
        try {
            initializeLogFile();
            startErrorReader();
            initializeWriter();
            schedulePeriodicFlush();
            readServerOutput();
        } catch (Exception e) {
            log.error("Error in ServerOutReader", e);
        } finally {
            cleanupResources();
        }
    }

    private void initializeLogFile() {
        logFile = new File(process.getDirectory(), BUFFER_LOG_FILENAME);
        try {
            Path logPath = logFile.toPath();
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath.getParent());
                Files.createFile(logPath);
            }
        } catch (IOException e) {
            log.error("Failed to create log file", e);
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
        } catch (IOException e) {
            log.error("Failed to initialize writer", e);
        }
    }

    private void schedulePeriodicFlush() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                flushWriter();
            }
        };

        writerTask = new java.util.Timer(true);
        writerTask.scheduleAtFixedRate(task, WRITER_FLUSH_INTERVAL_MS, WRITER_FLUSH_INTERVAL_MS);
    }

    private void flushWriter() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                log.warn("Failed to flush writer", e);
            }
        }
    }

    private void readServerOutput() {
        Process process = getProcess().getProcess();
        if (process == null) {
            log.warn("Cannot read output: process is null");
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
                log.debug("Stream closed");
            }
        }
    }

    private void processOutputLine(String line) {
        if (line == null || line.isEmpty()) {
            return;
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
                log.debug("Failed to write line to file");
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

        process.setLogger(null);
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log.error("Failed to close writer");
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

