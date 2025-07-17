package de.redstonecloud.cloud.server;

import de.redstonecloud.cloud.logger.Logger;
import lombok.Builder;
import lombok.Getter;

import java.io.*;

@Builder
public class DummyErrorReader extends Thread {
    @Getter
    private ServerLogger logger;
    @lombok.Builder.Default
    private boolean running = true;

    public void run() {
        while (running && logger.getServer().getProcess() != null && logger.getServer().getProcess().getErrorStream() != null) {
            BufferedReader out = new BufferedReader(new InputStreamReader(logger.getServer().getProcess().getErrorStream()));
            String line = "";
            try {
                while (running && (line = out.readLine()) != null) {
                    if (logger.isConsoleLogging()) Logger.getInstance().server(logger.getServer().getName(), line);
                    if (logger.getWriter() != null) {
                        try {
                            logger.getWriter().write(line);
                            logger.getWriter().newLine();

                            logger.addMessage(line);
                        } catch (IOException ignore) {
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    public void cancel() {
        running = false;
        this.interrupt();
    }
}