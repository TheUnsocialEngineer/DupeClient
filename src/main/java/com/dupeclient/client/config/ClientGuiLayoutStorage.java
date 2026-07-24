package com.dupeclient.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists main client GUI scroll when using embedded (non-floating) layout.
 * Used by the client hub scroll + sidebar layout.
 */
public final class ClientGuiLayoutStorage {
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_CLIENT_GUI_LAYOUT);

    private ClientGuiLayoutStorage() {
    }

    @Deprecated
    public static double loadScrollY() {
        if (!Files.exists(CONFIG_PATH)) {
            return 0.0;
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            JsonObject o = GSON.fromJson(raw, JsonObject.class);
            if (o != null && o.has("mainScrollY")) {
                return o.get("mainScrollY").getAsDouble();
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return 0.0;
    }

    public static int loadSelectedModule() {
        if (!Files.exists(CONFIG_PATH)) {
            return 0;
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            JsonObject o = GSON.fromJson(raw, JsonObject.class);
            if (o != null && o.has("selectedModule")) {
                return o.get("selectedModule").getAsInt();
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return 0;
    }

    @Deprecated
    public static void saveScrollY(double mainScrollY) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject o = readOrEmpty();
            o.addProperty("mainScrollY", mainScrollY);
            Files.writeString(CONFIG_PATH, GSON.toJson(o));
        } catch (IOException ignored) {
        }
    }

    public static void saveClientGuiLayout(int selectedModule, double[] moduleScrollY) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject o = readOrEmpty();
            o.addProperty("selectedModule", selectedModule);
            if (moduleScrollY != null && moduleScrollY.length > 0) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                for (double d : moduleScrollY) {
                    arr.add(d);
                }
                o.add("moduleScrollY", arr);
            }
            Files.writeString(CONFIG_PATH, GSON.toJson(o));
        } catch (IOException ignored) {
        }
    }

    public static double[] loadModuleScrollY(int expectedLen) {
        if (expectedLen <= 0) {
            return new double[0];
        }
        if (!Files.exists(CONFIG_PATH)) {
            return new double[expectedLen];
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            JsonObject o = GSON.fromJson(raw, JsonObject.class);
            if (o == null || !o.has("moduleScrollY") || !o.get("moduleScrollY").isJsonArray()) {
                return new double[expectedLen];
            }
            var arr = o.get("moduleScrollY").getAsJsonArray();
            double[] out = new double[expectedLen];
            for (int i = 0; i < expectedLen; i++) {
                if (i < arr.size() && !arr.get(i).isJsonNull()) {
                    out[i] = arr.get(i).getAsDouble();
                }
            }
            return out;
        } catch (IOException | RuntimeException ignored) {
        }
        return new double[expectedLen];
    }

    private static JsonObject readOrEmpty() {
        if (!Files.exists(CONFIG_PATH)) {
            return new JsonObject();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            JsonObject o = GSON.fromJson(raw, JsonObject.class);
            return o != null ? o : new JsonObject();
        } catch (IOException e) {
            return new JsonObject();
        }
    }
}
