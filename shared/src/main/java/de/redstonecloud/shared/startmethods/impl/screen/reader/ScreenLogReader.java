package de.redstonecloud.shared.startmethods.impl.screen.reader;

import de.redstonecloud.shared.startmethods.impl.screen.ScreenProcess;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class ScreenLogReader extends Thread {

    private static final String LOG_FILE = "screen.log";

    private final File logFile;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean consoleLogging = new AtomicBoolean(false);

    public ScreenLogReader(ScreenProcess process) {
        this.logFile = new File(process.getDirectory(), LOG_FILE);
        setName("ScreenLogReader-" + process.getDirectory());
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            waitForFile();
            tailFile();
        } catch (Exception e) {
            if (running.get()) {
                log.error("ScreenLogReader error", e);
            }
        }
    }

    private void waitForFile() throws InterruptedException {
        while (!logFile.exists() && running.get()) {
            Thread.sleep(200);
        }
    }

    private void tailFile() throws IOException, InterruptedException {
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long pointer = raf.length(); // start at end

            while (running.get()) {
                long length = raf.length();

                if (length > pointer) {
                    raf.seek(pointer);
                    String line;

                    while ((line = raf.readLine()) != null) {
                        if (consoleLogging.get()) {
                            log.info("[CONSOLE] {}", line);
                        }
                    }

                    pointer = raf.getFilePointer();
                }

                Thread.sleep(200);
            }
        }
    }

    // ========== LOGGING CONTROL ==========

    public void enableConsoleLogging() {
        consoleLogging.set(true);
    }

    public void disableConsoleLogging() {
        consoleLogging.set(false);
    }

    public boolean isConsoleLogging() {
        return consoleLogging.get();
    }

    // ========== LIFECYCLE ==========

    public void cancel() {
        running.set(false);
        interrupt();
    }
}
