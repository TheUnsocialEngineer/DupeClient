package com.dupeclient.client.core;

/**
 * Screens that consume keyboard input for binding capture or similar should implement this so
 * global hotkeys (macros, overlays, hub toggles) do not fire on the same keys.
 */
public interface KeyboardConsumingScreen {
    boolean consumesGlobalHotkeys();
}
