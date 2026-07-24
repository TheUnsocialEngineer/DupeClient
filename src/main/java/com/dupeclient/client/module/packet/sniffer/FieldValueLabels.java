package com.dupeclient.client.module.packet.sniffer;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Human-readable labels for common numeric packet fields (movement keys, mouse buttons, etc.).
 */
public final class FieldValueLabels {
    private static final String[] MOVEMENT_KEY = {"w", "a", "s", "d", "space", "shift", "ctrl"};
    private static final String[] MOVEMENT_DESC = {
            "forward (w)", "left (a)", "back (s)", "right (d)", "jump (space)", "sneak (shift)", "sprint (ctrl)"};

    private static final String[] MOUSE_BUTTON = {"left", "right", "middle"};
    private static final String[] MOUSE_DESC = {"left click", "right click", "middle click"};

    private FieldValueLabels() {
    }

    public static boolean isLabeledField(@Nullable String packetType, String fieldName) {
        return kindFor(packetType, fieldName) != null;
    }

    public static boolean isCyclableField(@Nullable String packetType, String fieldName) {
        LabelKind kind = kindFor(packetType, fieldName);
        return kind == LabelKind.MOVEMENT_KEY || kind == LabelKind.MOUSE_BUTTON;
    }

    public static String formatDisplay(@Nullable String packetType, String fieldName, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue == null ? "" : rawValue;
        }
        String raw = stripDecorated(rawValue.trim());
        LabelKind kind = kindFor(packetType, fieldName);
        if (kind == null) {
            return rawValue.trim();
        }
        if (kind == LabelKind.PLAYER_INPUT) {
            String label = labelFor(kind, fieldName, raw, -1);
            if (label == null) {
                return raw;
            }
            return raw + " (" + label + ")";
        }
        int id = parseIntLoose(raw);
        if (id < 0) {
            return rawValue.trim();
        }
        String label = labelFor(kind, fieldName, raw, id);
        if (label == null) {
            return raw;
        }
        return raw + " (" + label + ")";
    }

    /** Accepts {@code 3}, {@code d}, {@code 3 (d)}, etc. */
    public static String parseRaw(@Nullable String packetType, String fieldName, String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        LabelKind kind = kindFor(packetType, fieldName);
        if (kind == null) {
            return stripDecorated(trimmed);
        }
        String bare = stripDecorated(trimmed);
        int fromName = idFromName(kind, bare);
        if (fromName >= 0) {
            return Integer.toString(fromName);
        }
        int id = parseIntLoose(bare);
        return id >= 0 ? Integer.toString(id) : bare;
    }

    public static String cycle(@Nullable String packetType, String fieldName, String current) {
        LabelKind kind = kindFor(packetType, fieldName);
        if (kind != LabelKind.MOVEMENT_KEY && kind != LabelKind.MOUSE_BUTTON) {
            return current;
        }
        String raw = parseRaw(packetType, fieldName, current == null ? "" : current);
        int id = parseIntLoose(raw);
        if (id < 0) {
            id = 0;
        }
        int max = maxId(kind);
        return Integer.toString((id + 1) % (max + 1));
    }

    @Nullable
    private static LabelKind kindFor(@Nullable String packetType, String fieldName) {
        if (fieldName == null) {
            return null;
        }
        String field = fieldName.toLowerCase(Locale.ROOT);
        String type = packetType == null ? "" : packetType.toLowerCase(Locale.ROOT);

        if (field.equals("buttonid") || field.equals("keyid") || field.equals("inputid")) {
            return LabelKind.MOVEMENT_KEY;
        }
        if (field.equals("button") && type.contains("clickslot")) {
            return LabelKind.MOUSE_BUTTON;
        }
        if (field.equals("mousebutton") || field.equals("mouse_button")) {
            return LabelKind.MOUSE_BUTTON;
        }
        if (field.equals("key") || field.equals("keycode") || field.equals("scancode")) {
            return LabelKind.GLFW_KEY;
        }
        if (type.contains("playerinput")) {
            return switch (field) {
                case "forward", "backward", "left", "right", "jump", "sneak", "sprint" -> LabelKind.PLAYER_INPUT;
                default -> null;
            };
        }
        return null;
    }

    @Nullable
    private static String labelFor(LabelKind kind, String fieldName, String raw, int id) {
        if (kind == LabelKind.PLAYER_INPUT) {
            if (!Boolean.parseBoolean(raw.trim())) {
                return null;
            }
            return switch (fieldName.toLowerCase(Locale.ROOT)) {
                case "forward" -> "w";
                case "backward" -> "s";
                case "left" -> "a";
                case "right" -> "d";
                case "jump" -> "space";
                case "sneak" -> "shift";
                case "sprint" -> "ctrl";
                default -> null;
            };
        }
        return switch (kind) {
            case MOVEMENT_KEY -> id >= 0 && id < MOVEMENT_DESC.length ? MOVEMENT_DESC[id] : null;
            case MOUSE_BUTTON -> id >= 0 && id < MOUSE_DESC.length ? MOUSE_DESC[id] : null;
            case GLFW_KEY -> glfwKeyLabel(id);
            case PLAYER_INPUT -> null;
        };
    }

    private static int maxId(LabelKind kind) {
        return switch (kind) {
            case MOVEMENT_KEY -> MOVEMENT_KEY.length - 1;
            case MOUSE_BUTTON -> MOUSE_BUTTON.length - 1;
            case GLFW_KEY, PLAYER_INPUT -> 0;
        };
    }

    @Nullable
    private static String glfwKeyLabel(int code) {
        return switch (code) {
            case 32 -> "space";
            case 87, 119 -> "w";
            case 65, 97 -> "a";
            case 83, 115 -> "s";
            case 68, 100 -> "d";
            case 340, 344 -> "shift";
            case 341, 345 -> "ctrl";
            default -> null;
        };
    }

    private static int idFromName(LabelKind kind, String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (kind == LabelKind.MOVEMENT_KEY) {
            for (int i = 0; i < MOVEMENT_KEY.length; i++) {
                if (MOVEMENT_KEY[i].equals(n) || MOVEMENT_DESC[i].toLowerCase(Locale.ROOT).startsWith(n)) {
                    return i;
                }
            }
            return switch (n) {
                case "forward", "up" -> 0;
                case "left" -> 1;
                case "backward", "back", "down" -> 2;
                case "right" -> 3;
                case "jump" -> 4;
                case "sneak", "crouch" -> 5;
                case "sprint", "run" -> 6;
                default -> -1;
            };
        }
        if (kind == LabelKind.MOUSE_BUTTON) {
            for (int i = 0; i < MOUSE_BUTTON.length; i++) {
                if (MOUSE_BUTTON[i].equals(n) || MOUSE_DESC[i].replace(' ', '_').contains(n)) {
                    return i;
                }
            }
            return switch (n) {
                case "l", "lmb" -> 0;
                case "r", "rmb" -> 1;
                case "m", "mmb" -> 2;
                default -> -1;
            };
        }
        if (kind == LabelKind.GLFW_KEY) {
            return switch (n) {
                case "space" -> 32;
                case "w" -> 87;
                case "a" -> 65;
                case "s" -> 83;
                case "d" -> 68;
                case "shift" -> 340;
                case "ctrl", "control" -> 341;
                default -> -1;
            };
        }
        return -1;
    }

    private static String stripDecorated(String value) {
        int p = value.indexOf('(');
        if (p > 0) {
            return value.substring(0, p).trim();
        }
        return value.trim();
    }

    private static int parseIntLoose(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private enum LabelKind {
        MOVEMENT_KEY,
        MOUSE_BUTTON,
        GLFW_KEY,
        PLAYER_INPUT
    }
}
