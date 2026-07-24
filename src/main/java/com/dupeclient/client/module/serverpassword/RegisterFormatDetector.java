package com.dupeclient.client.module.serverpassword;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Infers /register argument order from server usage hints in chat. */
public final class RegisterFormatDetector {
    private static final Pattern EMAIL_BEFORE_PASSWORD = Pattern.compile(
            "<\\s*e?-?mail\\s*>\\s*<\\s*pass(?:word)?\\s*>|\\[e?-?mail\\]\\s*\\[pass(?:word)?\\]|"
                    + "register\\s+<?e?-?mail>?\\s+<?pass(?:word)?>?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_BEFORE_EMAIL = Pattern.compile(
            "<\\s*pass(?:word)?\\s*>\\s*<\\s*e?-?mail\\s*>|\\[pass(?:word)?\\]\\s*\\[e?-?mail\\]|"
                    + "register\\s+<?pass(?:word)?>?\\s+<?e?-?mail>?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_REPEAT = Pattern.compile(
            "<\\s*pass(?:word)?\\s*>\\s*<\\s*pass(?:word)?\\s*>|\\[pass(?:word)?\\]\\s*\\[pass(?:word)?\\]|"
                    + "pass(?:word)?\\s+pass(?:word)?|register\\s+\\S+\\s+\\S+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PLACEHOLDER = Pattern.compile(
            "<\\s*pass(?:word)?\\s*>|\\[pass(?:word)?\\]|\\{pass(?:word)?(?:\\s+repeat)?\\}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PLACEHOLDER = Pattern.compile(
            "<\\s*e?-?mail\\s*>|\\[e?-?mail\\]|\\{e?-?mail\\}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REGISTER_COMMAND = Pattern.compile(
            "/(register|reg)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LOGIN_COMMAND = Pattern.compile(
            "/(?:login|log|l)\\b",
            Pattern.CASE_INSENSITIVE);

    private RegisterFormatDetector() {
    }

    public static RegisterFormat detectFromPrompt(String line) {
        if (line == null || line.isBlank()) {
            return RegisterFormat.PASSWORD_REPEAT;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (!lower.contains("register") && !lower.contains("/reg") && !lower.contains("reg ")) {
            return RegisterFormat.PASSWORD_REPEAT;
        }

        if (EMAIL_BEFORE_PASSWORD.matcher(line).find()) {
            return RegisterFormat.EMAIL_THEN_PASSWORD;
        }
        if (PASSWORD_BEFORE_EMAIL.matcher(line).find()) {
            return RegisterFormat.PASSWORD_THEN_EMAIL;
        }

        int passSlots = countMatches(PASSWORD_PLACEHOLDER, line);
        int emailSlots = countMatches(EMAIL_PLACEHOLDER, line);

        if (emailSlots > 0 && passSlots > 0) {
            int emailIdx = firstIndexOfEmailHint(lower);
            int passIdx = firstIndexOfPasswordHint(lower);
            if (emailIdx >= 0 && passIdx >= 0) {
                return emailIdx < passIdx ? RegisterFormat.EMAIL_THEN_PASSWORD : RegisterFormat.PASSWORD_THEN_EMAIL;
            }
            return RegisterFormat.EMAIL_THEN_PASSWORD;
        }

        if (PASSWORD_REPEAT.matcher(line).find() || passSlots >= 2) {
            return RegisterFormat.PASSWORD_REPEAT;
        }

        if (passSlots == 1 || looksLikeSinglePasswordUsage(lower)) {
            return RegisterFormat.PASSWORD_ONLY;
        }

        if (containsEmailHint(lower)) {
            return RegisterFormat.EMAIL_THEN_PASSWORD;
        }

        return RegisterFormat.PASSWORD_REPEAT;
    }

    /** AuthMe-style command name from a server usage hint (defaults to {@code register}). */
    public static String detectRegisterCommand(String line) {
        if (line == null || line.isBlank()) {
            return "register";
        }
        Matcher matcher = REGISTER_COMMAND.matcher(line);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase(Locale.ROOT);
        }
        return "register";
    }

    /** Login command name from a server usage hint (defaults to {@code login}). */
    public static String detectLoginCommand(String line) {
        if (line == null || line.isBlank()) {
            return "login";
        }
        Matcher matcher = LOGIN_COMMAND.matcher(line);
        if (matcher.find()) {
            String cmd = matcher.group(1).toLowerCase(Locale.ROOT);
            return "log".equals(cmd) ? "login" : cmd;
        }
        return "login";
    }

    /** Next format to try when the server rejects the register command syntax. */
    public static RegisterFormat nextFallback(RegisterFormat current) {
        return switch (current) {
            case PASSWORD_REPEAT -> RegisterFormat.PASSWORD_ONLY;
            case PASSWORD_ONLY -> RegisterFormat.PASSWORD_REPEAT;
            case EMAIL_THEN_PASSWORD, PASSWORD_THEN_EMAIL -> RegisterFormat.PASSWORD_REPEAT;
        };
    }

    private static boolean looksLikeSinglePasswordUsage(String lower) {
        if (looksLikeDoublePasswordUsage(lower)) {
            return false;
        }
        return lower.contains("/register <password>")
                || lower.contains("/register {password}")
                || lower.contains("/register password")
                || lower.contains("register <pass>")
                || lower.contains("register <password>")
                || lower.contains("register {password}")
                || (lower.contains("/register") && !lower.contains("repeat"))
                || (lower.contains("/reg ") && !lower.contains("repeat"));
    }

    private static boolean looksLikeDoublePasswordUsage(String lower) {
        return lower.contains("password password")
                || lower.contains("pass pass")
                || lower.contains("<password> <password>")
                || lower.contains("{password} {password}")
                || lower.contains("<password> <pass>")
                || lower.contains("repeat")
                || countMatches(PASSWORD_PLACEHOLDER, lower) >= 2;
    }

    private static int countMatches(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static boolean containsEmailHint(String lower) {
        return lower.contains("email") || lower.contains("e-mail") || lower.contains("e mail");
    }

    private static boolean containsPasswordHint(String lower) {
        return lower.contains("password") || lower.contains("passwd") || lower.contains("pass ");
    }

    private static int firstIndexOfEmailHint(String lower) {
        int email = lower.indexOf("email");
        if (email >= 0) {
            return email;
        }
        int dash = lower.indexOf("e-mail");
        if (dash >= 0) {
            return dash;
        }
        return lower.indexOf("e mail");
    }

    private static int firstIndexOfPasswordHint(String lower) {
        int password = lower.indexOf("password");
        if (password >= 0) {
            return password;
        }
        int passwd = lower.indexOf("passwd");
        if (passwd >= 0) {
            return passwd;
        }
        return lower.indexOf("pass ");
    }
}
