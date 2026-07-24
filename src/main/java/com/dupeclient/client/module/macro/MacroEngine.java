package com.dupeclient.client.module.macro;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/** Thin API surface; logic in {@link MacroRuntime}. */
public final class MacroEngine {
    public static final MacroEngine INSTANCE = new MacroEngine();

    private MacroEngine() {
    }

    public boolean isRunning() {
        return MacroRuntime.INSTANCE.isRunning();
    }

    @Nullable
    public String getActiveMacroId() {
        return MacroRuntime.INSTANCE.getActiveMacroId();
    }

    public String getRunLabel() {
        return MacroRuntime.INSTANCE.getRunLabel();
    }

    public void start(Minecraft client, String id) {
        MacroRuntime.INSTANCE.start(client, id);
    }

    public void stop(Minecraft client) {
        MacroRuntime.INSTANCE.stop(client);
    }

    public void tick(Minecraft client) {
        MacroRuntime.INSTANCE.tick(client);
    }
}
