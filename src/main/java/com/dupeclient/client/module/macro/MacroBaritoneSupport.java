package com.dupeclient.client.module.macro;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

/**
 * Optional macro pathing via Meteor's Baritone fork ({@code baritone-meteor}). Uses reflection so DupeClient does not
 * hard-depend on Baritone at runtime (avoids {@code fabric.mod.json} forcing it alongside mods such as Axiom that load
 * {@code Screen} during {@code preLaunch}, which breaks Baritone's {@code MixinScreen} prepare order).
 */
public final class MacroBaritoneSupport {
    private static final String MOD_ID = "baritone-meteor";

    private MacroBaritoneSupport() {
    }

    public static boolean startPathToBlock(MinecraftClient client, BlockPos goal) {
        if (client == null || client.player == null || goal == null) {
            return false;
        }
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return false;
        }
        try {
            Class<?> baritoneApi = Class.forName("baritone.api.BaritoneAPI");
            Method getProvider = baritoneApi.getMethod("getProvider");
            Object provider = getProvider.invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            if (baritone == null) {
                return false;
            }
            Object ctx = baritone.getClass().getMethod("getPlayerContext").invoke(baritone);
            Object ctxPlayer = ctx.getClass().getMethod("player").invoke(ctx);
            if (ctxPlayer != client.player) {
                return false;
            }
            Object pathing = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            pathing.getClass().getMethod("cancelEverything").invoke(pathing);
            Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Object goalBlock = goalBlockClass.getConstructor(BlockPos.class).newInstance(goal);
            Object customGoal = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            Class<?> goalIface = Class.forName("baritone.api.pathing.goals.Goal");
            customGoal.getClass().getMethod("setGoalAndPath", goalIface).invoke(customGoal, goalBlock);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Stop any in-flight Baritone goals when entering a world. Prevents stale pathing / async node spam after reconnects.
     */
    public static void onWorldJoin(MinecraftClient client) {
        if (client == null) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> baritoneApi = Class.forName("baritone.api.BaritoneAPI");
            Object provider = baritoneApi.getMethod("getProvider").invoke(null);
            Iterable<?> all = (Iterable<?>) provider.getClass().getMethod("getAllBaritones").invoke(provider);
            for (Object baritone : all) {
                resetBaritoneInstance(baritone, client);
            }
        } catch (Throwable ignored) {
            cancelPathing(client);
        }
    }

    public static void cancelPathing(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> baritoneApi = Class.forName("baritone.api.BaritoneAPI");
            Object provider = baritoneApi.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            resetBaritoneInstance(baritone, client);
        } catch (Throwable ignored) {
        }
    }

    private static void resetBaritoneInstance(Object baritone, MinecraftClient client) {
        if (baritone == null || client == null || client.player == null) {
            return;
        }
        try {
            Object ctx = baritone.getClass().getMethod("getPlayerContext").invoke(baritone);
            Object ctxPlayer = ctx.getClass().getMethod("player").invoke(ctx);
            if (ctxPlayer != null && ctxPlayer != client.player) {
                return;
            }
            Object pathing = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            pathing.getClass().getMethod("cancelEverything").invoke(pathing);
            invokeOnLostControl(baritone, "getCustomGoalProcess");
            invokeOnLostControl(baritone, "getExploreProcess");
            invokeOnLostControl(baritone, "getMineProcess");
            invokeOnLostControl(baritone, "getFollowProcess");
            invokeOnLostControl(baritone, "getFarmProcess");
            invokeOnLostControl(baritone, "getBuilderProcess");
        } catch (Throwable ignored) {
        }
    }

    private static void invokeOnLostControl(Object baritone, String processGetter) {
        try {
            Object process = baritone.getClass().getMethod(processGetter).invoke(baritone);
            if (process != null) {
                process.getClass().getMethod("onLostControl").invoke(process);
            }
        } catch (Throwable ignored) {
        }
    }
}
