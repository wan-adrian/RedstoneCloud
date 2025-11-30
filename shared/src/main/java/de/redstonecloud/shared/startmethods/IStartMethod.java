package de.redstonecloud.shared.startmethods;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public interface IStartMethod {
    void setDirectory(String dir);
    void setPort(int port);
    void setOnExit(Runnable onExit);
    void start();
    void prepare(String sourceDir, String[] command, Map<String, String> env);
    void stop(String stopCommand);
    void kill(int timeout);
    void cleanup();
    String getDirectory();
    void writeCommand(String command);
    boolean isActive();
}
