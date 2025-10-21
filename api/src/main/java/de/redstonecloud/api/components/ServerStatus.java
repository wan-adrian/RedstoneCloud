package de.redstonecloud.api.components;

import lombok.Getter;

@Getter
public enum ServerStatus {
    NONE,
    PREPARED,
    STARTING,
    RUNNING,
    WAITING,
    IN_GAME,
    STOPPING,
    STOPPED,
    ERROR;
}
