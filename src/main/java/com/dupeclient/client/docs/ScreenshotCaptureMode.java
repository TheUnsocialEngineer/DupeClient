package com.dupeclient.client.docs;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ScreenshotCaptureMode {
    private static final boolean ACTIVE = Boolean.getBoolean("dupeclient.captureScreenshots");

    private ScreenshotCaptureMode() {
    }

    public static boolean isActive() {
        return ACTIVE;
    }

    public static Path outputDir() {
        String raw = System.getProperty("dupeclient.captureDir", "docs/assets/screenshots");
        return Paths.get(raw).toAbsolutePath().normalize();
    }
}
