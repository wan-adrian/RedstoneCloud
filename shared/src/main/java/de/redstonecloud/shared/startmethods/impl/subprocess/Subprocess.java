package de.redstonecloud.shared.startmethods.impl.subprocess;

import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.shared.startmethods.IStartMethod;
import de.redstonecloud.shared.startmethods.impl.subprocess.reader.ServerOutReader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Log4j2
@Getter
public class Subprocess implements IStartMethod {
    private volatile ProcessBuilder processBuilder;
    private Process process;
    @Setter
    private ServerOutReader logger;
    public String directory = "";
    public int port = -1;
    private Runnable onExit;

    @Override
    public void setOnExit(Runnable onExit) {
        this.onExit = onExit;
    }

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        try {
            logger = ServerOutReader.builder().process(this).build();
            process = processBuilder.start();
            process.onExit().thenRun(() -> {
                if (onExit != null) {
                    onExit.run();
                }
            });
            logger.start();
        } catch (IOException e) {
            log.info("Could not start new subprocess: ", e);
        }
    }

    @Override
    public void prepare(String sourceDir, String[] command, Map<String, String> env) {
        processBuilder = new ProcessBuilder(command)
                .directory(new File(directory));

        processBuilder.environment().putAll(env);

        createServerDirectory();
        if (!Files.exists(Path.of(sourceDir))) {
            log.error("Template directory not found: " + sourceDir);
            return;
        }

        try {
            FileUtils.copyDirectory(new File(sourceDir), new File(directory));
        } catch (IOException e) {
            log.error("Cannot copy template", e);
            log.error("Source: " + sourceDir + " | Destination: " + directory);
        }
    }

    @Override
    public void stop(String stopCommand) {
        writeCommand(stopCommand);
    }

    @Override
    public void cleanup() {
            if (logger != null) {
                logger.cancel();
            }
    }

    @Override
    public String getDirectory() {
        return directory;
    }

    @Override
    public void writeCommand(String command) {
        try (PrintWriter stdin = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream())), true)) {
            stdin.println(command);
        } catch (Exception e) {
            log.error("Failed to write console command", e);
        }
    }

    @Override
    public boolean isActive() {
        return process != null && process.isAlive();
    }

    private void createServerDirectory() {
        try {
            Files.createDirectories(Path.of(directory));
        } catch (IOException e) {
            log.error("Cannot create dir: ", directory);
        }
    }

    @Override
    public void kill(int timeout) {
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                        log.warn("Server did not stop gracefully, forcibly terminated");
                }

                timer.cancel();
            }
        }, timeout);
    }

    @Override
    public void enableLogging() {
        if(logger == null)
            return;

        logger.enableConsoleLogging();
    }

    @Override
    public void disableLogging() {
        if(logger == null)
            return;

        logger.disableConsoleLogging();
    }

    @Override
    public boolean isLoggerEnabled() {
        if(logger == null)
            return false;

        return logger.isConsoleLogging();
    }
}
