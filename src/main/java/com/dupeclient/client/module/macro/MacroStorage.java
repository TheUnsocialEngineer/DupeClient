package com.dupeclient.client.module.macro;

import com.dupeclient.DupeBuildConstants;
import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.DupeClientConfigDir;
import com.dupeclient.client.module.macro.graph.MacroGraphCompiler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public final class MacroStorage {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EXAMPLE_RESOURCE = "/assets/dupeclient/macros/example_linear.json";
    private static final String SPEEDBRIDGE_RESOURCE = "/assets/dupeclient/macros/speedbridge.json";

    private MacroStorage() {
    }

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return (T)GSON.fromJson(json, type);
    }

    public static Path macrosDirectory() {
        return DupeClientConfigDir.root().resolve("macros");
    }

    private static Path macroEditorPreferencesFile() {
        return DupeClientConfigDir.root().resolve("macro_editor_preferences.json");
    }

    public static MacroEditorPreferences loadMacroEditorPreferences() {
        MacroStorage.prepare();
        Path file = MacroStorage.macroEditorPreferencesFile();
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return new MacroEditorPreferences();
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            MacroEditorPreferences p = GSON.fromJson(reader, MacroEditorPreferences.class);
            return p != null ? p : new MacroEditorPreferences();
        } catch (JsonParseException | IOException e) {
            DupeClient.LOGGER.warn("Could not load macro editor preferences: {}", e.toString());
            return new MacroEditorPreferences();
        }
    }

    public static void saveMacroEditorPreferences(MacroEditorPreferences prefs) {
        if (prefs == null) {
            return;
        }
        try {
            MacroStorage.ensureDirectory();
            Path file = MacroStorage.macroEditorPreferencesFile();
            Files.writeString(file, (CharSequence)GSON.toJson((Object)prefs), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            DupeClient.LOGGER.warn("Could not save macro editor preferences: {}", (Object)e.toString());
        }
    }

    public static void ensureDirectory() throws IOException {
        Files.createDirectories(MacroStorage.macrosDirectory(), new FileAttribute[0]);
    }

    public static void prepare() {
        try {
            MacroStorage.ensureDirectory();
            MacroStorage.ensureBundledExampleIfMissing();
            MacroStorage.copyBundledMacroIfMissing("speedbridge.json", SPEEDBRIDGE_RESOURCE);
        }
        catch (IOException e) {
            DupeClient.LOGGER.warn("Macro storage prepare failed: {}", (Object)e.toString());
        }
    }

    public static List<String> listMacroIds() {
        MacroStorage.prepare();
        Path dir = MacroStorage.macrosDirectory();
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)
                            && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .map(p -> MacroStorage.stripJsonExtension(p.getFileName().toString()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            DupeClient.LOGGER.warn("Failed to list macros: {}", e.toString());
            return List.of();
        }
    }

    public static MacroDefinition load(String id) throws IOException {
        MacroStorage.prepare();
        String safeId = MacroStorage.sanitizeId(id);
        Path file = MacroStorage.macrosDirectory().resolve(safeId + ".json");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Macro not found: " + safeId);
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);){
            MacroDefinition def;
            try {
                def = (MacroDefinition)GSON.fromJson((Reader)reader, MacroDefinition.class);
            }
            catch (JsonParseException e) {
                throw new IOException("Invalid JSON in macro " + safeId + ": " + e.getMessage(), e);
            }
            if (def == null) {
                throw new IOException("Empty macro file: " + safeId);
            }
            def.normalize();
            if (def.formatVersion < 1 || def.formatVersion > 2) {
                throw new IOException("Unsupported formatVersion: " + def.formatVersion + " (supported: 1\u20132)");
            }
            if (def.formatVersion == 2) {
                MacroGraphCompiler.ensureGraphBookends(def);
            }
            if (!safeId.equals(def.id)) {
                DupeClient.LOGGER.warn("Macro file {} declares id '{}'; using filename id '{}'", new Object[]{file, def.id, safeId});
                def.id = safeId;
            }
            MacroDefinition macroDefinition = def;
            return macroDefinition;
        }
    }

    public static void save(MacroDefinition def) throws IOException {
        String safeId;
        MacroStorage.ensureDirectory();
        def.normalize();
        def.id = safeId = MacroStorage.sanitizeId(def.id);
        def.modifiedAt = Instant.now().toString();
        Path file = MacroStorage.macrosDirectory().resolve(safeId + ".json");
        Files.writeString(file, (CharSequence)GSON.toJson((Object)def), StandardCharsets.UTF_8, new OpenOption[0]);
        MacroQuickPlay.markDirty();
    }

    public static String uniqueMacroId(String base) {
        MacroStorage.prepare();
        String safe = MacroStorage.sanitizeId(base);
        if (safe.isBlank()) {
            safe = "imported_macro";
        }
        if (!MacroStorage.macroIdExists(safe)) {
            return safe;
        }
        for (int i = 2; i < 10_000; i++) {
            String candidate = safe + "_" + i;
            if (!MacroStorage.macroIdExists(candidate)) {
                return candidate;
            }
        }
        return safe + "_" + System.currentTimeMillis();
    }

    public static boolean macroIdExists(String id) {
        String safe = MacroStorage.sanitizeId(id);
        for (String existing : MacroStorage.listMacroIds()) {
            if (existing.equalsIgnoreCase(safe)) {
                return true;
            }
        }
        return false;
    }

    public static MacroBundle exportBundle(String id) throws IOException {
        return MacroStorage.exportBundle(MacroStorage.load(id));
    }

    public static MacroBundle exportBundle(MacroDefinition def) {
        MacroDefinition copy = MacroStorage.fromJson(MacroStorage.toJson(def), MacroDefinition.class);
        copy.normalize();
        MacroBundle bundle = new MacroBundle(copy);
        bundle.exportedAt = Instant.now().toString();
        bundle.dupeclientVersion = DupeBuildConstants.MOD_VERSION;
        bundle.description = copy.description == null ? "" : copy.description.trim();
        return bundle;
    }

    public static String exportBundleJson(String id) throws IOException {
        return MacroStorage.toJson(MacroStorage.exportBundle(id));
    }

    public static String exportBundleJson(MacroDefinition def) {
        return MacroStorage.toJson(MacroStorage.exportBundle(def));
    }

    public static MacroDefinition parseImportDefinition(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IOException("JSON is empty");
        }
        JsonElement root;
        try {
            root = GSON.fromJson(json, JsonElement.class);
        } catch (JsonParseException e) {
            throw new IOException("Invalid JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isJsonObject()) {
            throw new IOException("Expected a JSON object");
        }
        JsonObject obj = root.getAsJsonObject();
        MacroDefinition def;
        if (obj.has("definition") && obj.get("definition").isJsonObject()) {
            def = GSON.fromJson(obj.get("definition"), MacroDefinition.class);
        } else if (obj.has("dupeclientMacro")) {
            MacroBundle bundle = GSON.fromJson(obj, MacroBundle.class);
            if (bundle == null || bundle.definition == null) {
                throw new IOException("Export bundle is missing definition");
            }
            def = bundle.definition;
        } else {
            def = GSON.fromJson(obj, MacroDefinition.class);
        }
        if (def == null) {
            throw new IOException("No macro definition found in JSON");
        }
        MacroStorage.finalizeImportedDefinition(def);
        return def;
    }

    public static MacroImportResult importMacro(
            String json, @Nullable String targetId, boolean overwrite, boolean stripHotkey) {
        ArrayList<String> warnings = new ArrayList<>();
        MacroDefinition def;
        try {
            def = MacroStorage.parseImportDefinition(json);
        } catch (IOException e) {
            return MacroImportResult.fail(e.getMessage() == null ? "Import parse failed" : e.getMessage());
        }
        String desiredId = targetId == null || targetId.isBlank()
                ? MacroStorage.sanitizeId(def.id)
                : MacroStorage.sanitizeId(targetId);
        if (desiredId.isBlank()) {
            desiredId = "imported_macro";
        }
        boolean exists = MacroStorage.macroIdExists(desiredId);
        if (exists && !overwrite) {
            String remapped = MacroStorage.uniqueMacroId(desiredId);
            warnings.add("Macro id \"" + desiredId + "\" already exists — saved as \"" + remapped + "\".");
            desiredId = remapped;
        } else if (exists) {
            warnings.add("Overwrote existing macro \"" + desiredId + "\".");
        }
        def.id = desiredId;
        if (def.displayName == null || def.displayName.isBlank()) {
            def.displayName = desiredId;
        }
        if (stripHotkey && def.hotkeyKey >= 0) {
            def.hotkeyKey = -1;
            def.hotkeyMods = 0;
            warnings.add("Run hotkey cleared to avoid conflicts.");
        }
        warnings.addAll(MacroStorage.collectImportWarnings(def));
        try {
            MacroStorage.save(def);
            return MacroImportResult.ok(desiredId, def.displayName, warnings);
        } catch (IOException e) {
            return MacroImportResult.fail(e.getMessage() == null ? "Save failed" : e.getMessage());
        }
    }

    public static MacroImportResult importFromFile(
            Path file, @Nullable String targetId, boolean overwrite, boolean stripHotkey) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("File not found: " + file);
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return MacroStorage.importMacro(json, targetId, overwrite, stripHotkey);
    }

    public static void exportToFile(String id, Path target) throws IOException {
        MacroStorage.ensureDirectory();
        Files.writeString(target, MacroStorage.exportBundleJson(id), StandardCharsets.UTF_8, new OpenOption[0]);
    }

    private static void finalizeImportedDefinition(MacroDefinition def) throws IOException {
        def.normalize();
        if (def.formatVersion < 1 || def.formatVersion > 2) {
            throw new IOException("Unsupported formatVersion: " + def.formatVersion + " (supported: 1–2)");
        }
        if (def.formatVersion == 2) {
            MacroGraphCompiler.ensureGraphBookends(def);
        }
    }

    private static List<String> collectImportWarnings(MacroDefinition def) {
        List<String> diags = MacroGraphCompiler.collectDiagnostics(def);
        if (diags.isEmpty()) {
            return List.of();
        }
        ArrayList<String> warnings = new ArrayList<>(diags.size());
        for (String d : diags) {
            warnings.add("Compile: " + d);
        }
        return warnings;
    }

    public static void deleteMacro(String id) throws IOException {
        MacroStorage.prepare();
        String safeId = MacroStorage.sanitizeId(id);
        Path file = MacroStorage.macrosDirectory().resolve(safeId + ".json");
        if (!Files.isRegularFile(file, new LinkOption[0])) {
            throw new IOException("Macro not found: " + safeId);
        }
        Files.delete(file);
        MacroQuickPlay.markDirty();
    }

    public static String setRunHotkey(String macroId, int glfwKey) {
        try {
            MacroDefinition def = MacroStorage.load(macroId);
            if (glfwKey == -1 || glfwKey == 256) {
                def.hotkeyKey = -1;
                def.hotkeyMods = 0;
            } else {
                def.hotkeyKey = glfwKey;
                def.hotkeyMods = 0;
                long packed = MacroDefinition.packHotkey(def.hotkeyKey, 0);
                String myId = MacroStorage.filenameId(def.id);
                for (String other : MacroStorage.listMacroIds()) {
                    if (other.equalsIgnoreCase(myId)) continue;
                    try {
                        MacroDefinition od = MacroStorage.load(other);
                        if (od.hotkeyKey < 0 || MacroDefinition.packHotkey(od.hotkeyKey, od.hotkeyMods) != packed) continue;
                        return "Macro \"" + other + "\" already uses that key.";
                    }
                    catch (IOException iOException) {
                    }
                }
            }
            MacroStorage.save(def);
            return null;
        }
        catch (IOException e) {
            return e.getMessage();
        }
    }

    private static void ensureBundledExampleIfMissing() {
        try {
            MacroStorage.ensureDirectory();
            Path target = MacroStorage.macrosDirectory().resolve("example_linear.json");
            if (Files.isRegularFile(target, new LinkOption[0])) {
                return;
            }
            InputStream in = DupeClient.class.getResourceAsStream(EXAMPLE_RESOURCE);
            if (in == null) {
                DupeClient.LOGGER.warn("Bundled macro example missing from jar: {}", (Object)EXAMPLE_RESOURCE);
                return;
            }
            Files.copy(in, target, new CopyOption[0]);
            DupeClient.LOGGER.info("Wrote example macro to {}", (Object)target);
        }
        catch (IOException e) {
            DupeClient.LOGGER.warn("Could not write example macro: {}", (Object)e.toString());
        }
    }

    private static void copyBundledMacroIfMissing(String filename, String resourcePath) {
        try {
            MacroStorage.ensureDirectory();
            Path target = MacroStorage.macrosDirectory().resolve(filename);
            if (Files.isRegularFile(target, new LinkOption[0])) {
                return;
            }
            InputStream in = DupeClient.class.getResourceAsStream(resourcePath);
            if (in == null) {
                DupeClient.LOGGER.warn("Bundled macro missing from jar: {}", (Object)resourcePath);
                return;
            }
            Files.copy(in, target, new CopyOption[0]);
            DupeClient.LOGGER.info("Wrote bundled macro to {}", (Object)target);
        }
        catch (IOException e) {
            DupeClient.LOGGER.warn("Could not write bundled macro {}: {}", (Object)filename, (Object)e.toString());
        }
    }

    private static String stripJsonExtension(String name) {
        if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return name.substring(0, name.length() - 5);
        }
        return name;
    }

    public static String filenameId(String id) {
        return MacroStorage.sanitizeId(id);
    }

    private static String sanitizeId(String id) {
        if (id == null) {
            return "unnamed";
        }
        String t = id.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); ++i) {
            char c = t.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-')) continue;
            out.append(c);
        }
        return out.isEmpty() ? "macro" : out.toString();
    }

    public static final class MacroEditorPreferences {
        public boolean autosaveEnabled;

        public MacroEditorPreferences() {
        }

        public MacroEditorPreferences(boolean autosaveEnabled) {
            this.autosaveEnabled = autosaveEnabled;
        }
    }
}

