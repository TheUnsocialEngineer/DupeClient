package com.dupeclient.client.module.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import com.dupeclient.client.mixin.KeyboardInvoker;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Simulates a one-shot keyboard press for {@link MacroStepType#PRESS_BUTTON}. */
public final class MacroKeyPress {
    public static final int UNKNOWN = GLFW.GLFW_KEY_UNKNOWN;

    private MacroKeyPress() {
    }

    public static int normalizeKeyCode(int code) {
        return code < 0 ? UNKNOWN : code;
    }

    public static int normalizeModifiers(int modifiers) {
        return Math.max(0, modifiers);
    }

    public static String keyLabel(int keyCode, int modifiers) {
        if (keyCode < 0 || keyCode == UNKNOWN) {
            return "UNBOUND";
        }
        String base = keyName(keyCode);
        if (modifiers == 0) {
            return base;
        }
        StringBuilder sb = new StringBuilder();
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            sb.append("CTRL+");
        }
        if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            sb.append("ALT+");
        }
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            sb.append("SHIFT+");
        }
        if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0) {
            sb.append("SUPER+");
        }
        sb.append(base);
        return sb.toString();
    }

    public static String keyName(int keyCode) {
        if (keyCode < 0 || keyCode == UNKNOWN) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfw != null && !glfw.isEmpty()) {
            if (glfw.length() == 1) {
                return glfw.toUpperCase(Locale.ROOT);
            }
            return glfw;
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PAGE_UP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PAGE_DOWN";
            case GLFW.GLFW_KEY_INSERT -> "INSERT";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS_LOCK";
            case GLFW.GLFW_KEY_NUM_LOCK -> "NUM_LOCK";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "PRINT_SCREEN";
            case GLFW.GLFW_KEY_SCROLL_LOCK -> "SCROLL_LOCK";
            case GLFW.GLFW_KEY_PAUSE -> "PAUSE";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_SUPER -> "LSUPER";
            case GLFW.GLFW_KEY_RIGHT_SUPER -> "RSUPER";
            case GLFW.GLFW_KEY_MENU -> "MENU";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "`";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_BACKSLASH -> "\\";
            case GLFW.GLFW_KEY_SEMICOLON -> ";";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_SLASH -> "/";
            default -> {
                if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
                    yield "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
                }
                if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
                    yield "NUMPAD_" + (keyCode - GLFW.GLFW_KEY_KP_0);
                }
                yield "KEY_" + keyCode;
            }
        };
    }

    public static void simulatePress(MinecraftClient client, int keyCode, int modifiers) {
        if (client == null || client.getWindow() == null || client.keyboard == null) {
            return;
        }
        if (keyCode < 0 || keyCode == UNKNOWN) {
            return;
        }
        long window = client.getWindow().getHandle();
        int scancode = GLFW.glfwGetKeyScancode(keyCode);
        KeyInput input = new KeyInput(keyCode, scancode, modifiers);
        KeyboardInvoker keyboard = (KeyboardInvoker) client.keyboard;
        keyboard.dupeclient$invokeOnKey(window, GLFW.GLFW_PRESS, input);
        Integer codepoint = codepointForKey(keyCode, modifiers);
        if (codepoint != null) {
            keyboard.dupeclient$invokeOnChar(window, new CharInput(codepoint, modifiers));
        }
        keyboard.dupeclient$invokeOnKey(window, GLFW.GLFW_RELEASE, input);
    }

    @Nullable
    static Integer codepointForKey(int keyCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            int base = 'a' + (keyCode - GLFW.GLFW_KEY_A);
            return shift ? Character.toUpperCase(base) : base;
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            if (shift) {
                return switch (keyCode) {
                    case GLFW.GLFW_KEY_1 -> (int) '!';
                    case GLFW.GLFW_KEY_2 -> (int) '@';
                    case GLFW.GLFW_KEY_3 -> (int) '#';
                    case GLFW.GLFW_KEY_4 -> (int) '$';
                    case GLFW.GLFW_KEY_5 -> (int) '%';
                    case GLFW.GLFW_KEY_6 -> (int) '^';
                    case GLFW.GLFW_KEY_7 -> (int) '&';
                    case GLFW.GLFW_KEY_8 -> (int) '*';
                    case GLFW.GLFW_KEY_9 -> (int) '(';
                    default -> (int) ')';
                };
            }
            return '0' + (keyCode - GLFW.GLFW_KEY_0);
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return '0' + (keyCode - GLFW.GLFW_KEY_KP_0);
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            return (int) ' ';
        }
        if (keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT) {
            return shift ? (int) '~' : (int) '`';
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS) {
            return shift ? (int) '_' : (int) '-';
        }
        if (keyCode == GLFW.GLFW_KEY_EQUAL) {
            return shift ? (int) '+' : (int) '=';
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET) {
            return shift ? (int) '{' : (int) '[';
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            return shift ? (int) '}' : (int) ']';
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSLASH) {
            return shift ? (int) '|' : (int) '\\';
        }
        if (keyCode == GLFW.GLFW_KEY_SEMICOLON) {
            return shift ? (int) ':' : (int) ';';
        }
        if (keyCode == GLFW.GLFW_KEY_APOSTROPHE) {
            return shift ? (int) '"' : (int) '\'';
        }
        if (keyCode == GLFW.GLFW_KEY_COMMA) {
            return shift ? (int) '<' : (int) ',';
        }
        if (keyCode == GLFW.GLFW_KEY_PERIOD) {
            return shift ? (int) '>' : (int) '.';
        }
        if (keyCode == GLFW.GLFW_KEY_SLASH) {
            return shift ? (int) '?' : (int) '/';
        }
        String glfw = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfw != null && glfw.length() == 1) {
            return (int) glfw.charAt(0);
        }
        return null;
    }
}
