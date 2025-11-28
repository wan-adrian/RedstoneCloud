package de.redstonecloud.shared.startmethods.impl.subprocess.reader;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads and logs output from a server's stderr stream.
 * Works in conjunction with ServerOutReader to capture all server output.
 */
@Log4j2
@Builder
public class ServerErrorReader extends Thread {

    @Getter
    private final ServerOutReader logger;

    @lombok.Builder.Default
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void run() {
        try {
            readServerErrors();
        } catch (Exception e) {
            log.error("Error in ServerErrorReader", e);
        }
    }

    private void readServerErrors() {
        Process process = logger.getProcess().getProcess();
        if (process == null) {
            log.warn("Cannot read errors: process is null");
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {

            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                processErrorLine(line);
            }
        } catch (IOException e) {
            if (running.get()) {
                log.debug("Error stream closed");
            }
        }
    }

    private void processErrorLine(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }

        // Write to the same file as stdout
        writeToFile(line);

        // Store in the parent logger's memory buffer
        logger.addMessage(line);
    }

    private void writeToFile(String line) {
        BufferedWriter writer = logger.getWriter();
        if (writer != null) {
            try {
                writer.write("[ERROR] " + line);
                writer.newLine();
            } catch (IOException e) {
                log.debug("Failed to write error line to file");
            }
        }
    }

    /**
     * Stops the error reader and interrupts the thread.
     */
    public void cancel() {
        if (!running.getAndSet(false)) {
            return; // Already cancelled
        }

        this.interrupt();
        log.debug("ServerErrorReader cancelled");
    }
}