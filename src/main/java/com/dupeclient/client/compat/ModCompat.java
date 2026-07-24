package com.dupeclient.client.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Detects common rendering / performance mods so DupeClient can avoid conflicting hooks
 * and tune container scaling for modded GUIs (Axiom, Meteor, etc.).
 */
public final class ModCompat {
    private static volatile boolean resolved;
    private static volatile boolean sodium;
    private static volatile boolean iris;
    private static volatile boolean meteor;
    private static volatile boolean axiom;
    private static volatile boolean voxy;
    private static volatile boolean indigo;
    private static volatile boolean entityCulling;
    private static volatile boolean lithium;

    private ModCompat() {
    }

    public static void resolve() {
        if (resolved) {
            return;
        }
        FabricLoader loader = FabricLoader.getInstance();
        sodium = loader.isModLoaded("sodium");
        iris = loader.isModLoaded("iris");
        meteor = loader.isModLoaded("meteor-client");
        axiom = loader.isModLoaded("axiom");
        voxy = loader.isModLoaded("voxy");
        indigo = loader.isModLoaded("fabric-renderer-indigo");
        entityCulling = loader.isModLoaded("entityculling");
        lithium = loader.isModLoaded("lithium");
        resolved = true;
    }

    public static boolean isSodiumLoaded() {
        resolve();
        return sodium;
    }

    public static boolean isIrisLoaded() {
        resolve();
        return iris;
    }

    public static boolean isMeteorLoaded() {
        resolve();
        return meteor;
    }

    public static boolean isAxiomLoaded() {
        resolve();
        return axiom;
    }

    public static boolean isVoxyLoaded() {
        resolve();
        return voxy;
    }

    public static boolean isIndigoLoaded() {
        resolve();
        return indigo;
    }

    public static boolean isEntityCullingLoaded() {
        resolve();
        return entityCulling;
    }

    public static boolean isLithiumLoaded() {
        resolve();
        return lithium;
    }

    /** True when a third-party renderer may own block/entity batching. */
    public static boolean prefersExternalWorldRenderer() {
        resolve();
        return sodium || iris;
    }

    /** True when modded container screens are likely (large panels, extra widgets). */
    public static boolean expectsModdedContainerWidgets() {
        resolve();
        return axiom || meteor || voxy;
    }
}
