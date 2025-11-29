package de.redstonecloud.shared.server;

public record ServerType(String name,
                         String[] startCommand,
                         boolean isProxy,
                         String logsPath,
                         String portSettingFile,
                         String portSettingPlaceholder,
                         String stopCommand,
                         String raw) {
    public ServerType merge(ServerType other) {
        return new ServerType(
                other.name != null ? other.name : this.name,
                other.startCommand != null ? other.startCommand : this.startCommand,
                other.isProxy,
                other.logsPath != null ? other.logsPath : this.logsPath,
                other.portSettingFile != null ? other.portSettingFile : this.portSettingFile,
                other.portSettingPlaceholder != null ? other.portSettingPlaceholder : this.portSettingPlaceholder,
                other.stopCommand != null ? other.stopCommand : this.stopCommand,
                other.raw != null ? other.raw : this.raw
        );
    }
}
