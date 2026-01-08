package de.redstonecloud.shared.startmethods;

import de.redstonecloud.shared.startmethods.impl.screen.ScreenProcess;
import de.redstonecloud.shared.startmethods.impl.subprocess.Subprocess;

public enum StartMethods {
    SUBPROCESS(Subprocess.class),
    SCREEN(ScreenProcess.class),
    ;

    private final Class<? extends IStartMethod> startMethodClass;

    StartMethods(Class<? extends IStartMethod> startMethodClass) {
        this.startMethodClass = startMethodClass;
    }

    public Class<? extends IStartMethod> getTargetClass() {
        return startMethodClass;
    }
}
