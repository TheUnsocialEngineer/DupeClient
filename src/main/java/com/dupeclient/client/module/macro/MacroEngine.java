package com.dupeclient.client.module.macro;

import net.minecraft.client.MinecraftClient;
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

    public void start(MinecraftClient client, String id) {
        MacroRuntime.INSTANCE.start(client, id);
    }

    public void stop(MinecraftClient client) {
        MacroRuntime.INSTANCE.stop(client);
    }

    public void tick(MinecraftClient client) {
        MacroRuntime.INSTANCE.tick(client);
    }
}
