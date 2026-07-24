package com.dupeclient.client.module.packet.sniffer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public final class MappingLabelResolver {
    private static final Pattern CLASS_PATTERN = Pattern.compile("^class_\\d+([$]class_\\d+)*$");
    private static final Pattern COMP_PATTERN = Pattern.compile("^comp_\\d+$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^field_\\d+$");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^method_\\d+$");
    private static final Pattern DOLLAR_RECORD_PATTERN = Pattern.compile("^\\$\\$\\d+$");
    private static final Map<String, String> CLASS_LABELS = new HashMap<String, String>();
    private static final Map<String, String> YARN_CLASS_KEYS = new HashMap<String, String>();
    private static final Map<String, String> FIELD_LABELS = new HashMap<String, String>();
    private static final Map<String, String> RECORD_LABELS = new HashMap<String, String>();
    private static final Map<String, String> METHOD_LABELS = new HashMap<String, String>();
    private static volatile boolean loaded;
    private static volatile boolean loadStarted;

    private MappingLabelResolver() {
    }

    public static void startBackgroundLoad() {
        if (loaded || loadStarted) {
            return;
        }
        loadStarted = true;
        Thread thread = new Thread(() -> {
            synchronized (MappingLabelResolver.class) {
                if (!loaded) {
                    MappingLabelResolver.loadLabels();
                    loaded = true;
                }
            }
        }, "dupeclient-yarn-labels");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static String resolveClassName(String runtimeSimple) {
        if (!loaded) {
            MappingLabelResolver.startBackgroundLoad();
            return runtimeSimple;
        }
        if (!MappingLabelResolver.looksObfuscatedClass(runtimeSimple)) {
            return runtimeSimple;
        }
        return CLASS_LABELS.getOrDefault(runtimeSimple, runtimeSimple);
    }

    public static String resolveFieldName(Class<?> owner, String runtimeName) {
        if (!loaded) {
            MappingLabelResolver.startBackgroundLoad();
            return runtimeName;
        }
        String ownerKey = MappingLabelResolver.ownerIntermediaryKey(owner);
        if (ownerKey != null) {
            String qualified;
            String resolved;
            String dollarResolved;
            if (DOLLAR_RECORD_PATTERN.matcher(runtimeName).matches() && (dollarResolved = RECORD_LABELS.get(ownerKey + "/" + runtimeName)) != null) {
                return dollarResolved;
            }
            if (MappingLabelResolver.looksObfuscatedField(runtimeName) && (resolved = FIELD_LABELS.get(qualified = ownerKey + "/" + runtimeName)) != null) {
                return resolved;
            }
        }
        if (MappingLabelResolver.looksObfuscatedField(runtimeName)) {
            return FIELD_LABELS.getOrDefault(runtimeName, runtimeName);
        }
        return runtimeName;
    }

    public static String resolveFieldName(Class<?> owner, RecordComponent component) {
        String ownerKey;
        String runtimeName = component.getName();
        String resolved = MappingLabelResolver.resolveFieldName(owner, runtimeName);
        if (!resolved.equals(runtimeName)) {
            return resolved;
        }
        int index = MappingLabelResolver.componentIndex(owner, component);
        if (index >= 0 && (ownerKey = MappingLabelResolver.ownerIntermediaryKey(owner)) != null) {
            String byIndex = RECORD_LABELS.get(ownerKey + "/" + index);
            if (byIndex != null) {
                return byIndex;
            }
            String byDollar = RECORD_LABELS.get(ownerKey + "/$$" + index);
            if (byDollar != null) {
                return byDollar;
            }
        }
        return runtimeName;
    }

    public static String resolveMethodName(Class<?> owner, String runtimeName) {
        if (!loaded) {
            MappingLabelResolver.startBackgroundLoad();
            return runtimeName;
        }
        if (!MappingLabelResolver.looksObfuscatedMethod(runtimeName)) {
            return runtimeName;
        }
        String ownerKey = MappingLabelResolver.ownerIntermediaryKey(owner);
        String qualified;
        String resolved;
        if (ownerKey != null && (resolved = METHOD_LABELS.get(qualified = ownerKey + "/" + runtimeName)) != null) {
            return resolved;
        }
        return METHOD_LABELS.getOrDefault(runtimeName, runtimeName);
    }

    public static String fieldValueFromMap(Map<String, String> map, Class<?> recordClass, RecordComponent component, String fallback) {
        for (String key : MappingLabelResolver.fieldLookupKeys(recordClass, component)) {
            if (!map.containsKey(key)) continue;
            return map.get(key);
        }
        return fallback;
    }

    public static List<String> fieldLookupKeys(Class<?> owner, RecordComponent component) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        String runtime = component.getName();
        keys.add(runtime);
        keys.add(MappingLabelResolver.resolveFieldName(owner, component));
        int index = MappingLabelResolver.componentIndex(owner, component);
        if (index >= 0) {
            keys.add("$$" + index);
            String ownerKey = MappingLabelResolver.ownerIntermediaryKey(owner);
            if (ownerKey != null) {
                String qualified;
                String byDollar;
                String byIndex = RECORD_LABELS.get(ownerKey + "/" + index);
                if (byIndex != null) {
                    keys.add(byIndex);
                }
                if ((byDollar = RECORD_LABELS.get(ownerKey + "/$$" + index)) != null) {
                    keys.add(byDollar);
                }
                if ((qualified = FIELD_LABELS.get(ownerKey + "/" + runtime)) != null) {
                    keys.add(qualified);
                }
            }
        }
        return new ArrayList<>(keys);
    }

    private static void loadLabels() {
        try (InputStream in = MappingLabelResolver.class.getResourceAsStream("/dupeclient/yarn_labels.txt");){
            if (in == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));){
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts;
                    if ((line = line.trim()).isEmpty() || line.startsWith("#") || (parts = line.split("\t", -1)).length < 3) continue;
                    switch (parts[0]) {
                        case "C": {
                            CLASS_LABELS.put(parts[1], parts[2]);
                            break;
                        }
                        case "Y": {
                            YARN_CLASS_KEYS.put(parts[1], parts[2]);
                            break;
                        }
                        case "F": {
                            if (parts.length >= 4) {
                                FIELD_LABELS.put(parts[1] + "/" + parts[2], parts[3]);
                                break;
                            }
                            FIELD_LABELS.put(parts[1], parts[2]);
                            break;
                        }
                        case "R": 
                        case "D": {
                            if (parts.length < 4) break;
                            RECORD_LABELS.put(parts[1] + "/" + parts[2], parts[3]);
                            break;
                        }
                        case "M": {
                            if (parts.length >= 4) {
                                METHOD_LABELS.put(parts[1] + "/" + parts[2], parts[3]);
                                break;
                            }
                            METHOD_LABELS.put(parts[1], parts[2]);
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static boolean looksObfuscatedClass(String name) {
        return CLASS_PATTERN.matcher(name).matches();
    }

    private static boolean looksObfuscatedField(String name) {
        return COMP_PATTERN.matcher(name).matches() || FIELD_PATTERN.matcher(name).matches();
    }

    private static boolean looksObfuscatedMethod(String name) {
        return METHOD_PATTERN.matcher(name).matches();
    }

    private static int componentIndex(Class<?> owner, RecordComponent component) {
        if (owner == null || !owner.isRecord()) {
            return -1;
        }
        RecordComponent[] components = owner.getRecordComponents();
        for (int i = 0; i < components.length; ++i) {
            if (!components[i].getName().equals(component.getName())) continue;
            return i;
        }
        return -1;
    }

    @Nullable
    private static String ownerIntermediaryKey(Class<?> owner) {
        if (owner == null) {
            return null;
        }
        String simple = owner.getSimpleName();
        String fromYarn = YARN_CLASS_KEYS.get(simple);
        if (fromYarn != null) {
            return fromYarn;
        }
        String name = owner.getName();
        int classIdx = name.indexOf("class_");
        if (classIdx >= 0) {
            return name.substring(classIdx);
        }
        if (MappingLabelResolver.looksObfuscatedClass(simple)) {
            return simple;
        }
        return null;
    }
}

