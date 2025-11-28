package de.redstonecloud.shared.server;

public record ServerType(String name,
                         String[] startCommand,
                         boolean isProxy,
                         String logsPath,
                         String portSettingFile,
                         String portSettingPlaceholder,
                         String stopCommand,
                         String raw) {
}
