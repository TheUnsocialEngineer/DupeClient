package com.dupeclient.client.module.serverpassword;

import java.util.Locale;

/** Detects AuthMe / NLogin style login and register prompts from server chat. */
public final class ServerAuthPrompts {
    private ServerAuthPrompts() {
    }

    public static boolean looksLikeRegisterPrompt(String lower) {
        if (lower.isBlank()) {
            return false;
        }
        if (looksLikeAlreadyRegistered(lower) || looksLikeRegisterRejected(lower)) {
            return false;
        }
        return lower.contains("/register")
                || lower.contains("please register")
                || lower.contains("not registered")
                || lower.contains("register first")
                || lower.contains("use /register")
                || lower.contains("register to play")
                || lower.contains("must register")
                || lower.contains("need to register")
                || lower.contains("sign up")
                || lower.contains("/reg ")
                || lower.contains("/registrar")
                || lower.contains("registrarte");
    }

    /** Register command failed — account already exists; use login instead. */
    public static boolean looksLikeRegisterRejected(String lower) {
        return looksLikeAlreadyRegistered(lower)
                || lower.contains("registration failed")
                || lower.contains("failed to register")
                || lower.contains("cannot register")
                || lower.contains("can't register");
    }

    /** Wrong /register syntax — retry with a different argument layout. */
    public static boolean looksLikeRegisterUsageError(String lower) {
        if (lower.isBlank() || looksLikeRegisterSuccess(lower) || looksLikeRegisterRejected(lower)) {
            return false;
        }
        if (!lower.contains("register") && !lower.contains("/reg")) {
            return false;
        }
        return lower.contains("usage:")
                || lower.contains("correct usage")
                || lower.contains("wrong usage")
                || lower.contains("invalid usage")
                || lower.contains("invalid arguments")
                || lower.contains("invalid argument")
                || lower.contains("too many arguments")
                || lower.contains("not enough arguments")
                || lower.contains("incorrect number")
                || lower.contains("wrong number")
                || (lower.contains("unknown") && lower.contains("argument"));
    }

    /** Login failed because the account does not exist yet — register is appropriate. */
    public static boolean looksLikeLoginFailureNotRegistered(String lower) {
        if (lower.isBlank() || looksLikeAuthWelcomeMessage(lower)) {
            return false;
        }
        return lower.contains("account not found")
                || lower.contains("isn't registered")
                || lower.contains("is not registered")
                || lower.contains("never registered")
                || lower.contains("you must register")
                || lower.contains("you need to register")
                || (lower.contains("not registered") && !lower.contains("already registered"))
                || (lower.contains("register first") && !lower.contains("/register <"));
    }

    /** Combined login/register instructions shown on join — not a login failure. */
    public static boolean looksLikeAuthWelcomeMessage(String lower) {
        boolean mentionsLogin = lower.contains("/login")
                || lower.contains("please log in")
                || lower.contains("please login")
                || lower.contains("log in first");
        boolean mentionsRegister = lower.contains("/register")
                || lower.contains("please register")
                || lower.contains("register to");
        return mentionsLogin && mentionsRegister;
    }

    public static boolean looksLikeLoginTimeoutKick(String lower) {
        return lower.contains("login timeout")
                || lower.contains("logged in too long")
                || lower.contains("authentication timeout");
    }

    /** Login failed for other reasons (wrong password, etc.) — do not register. */
    public static boolean looksLikeLoginFailure(String lower) {
        return lower.contains("wrong password")
                || lower.contains("incorrect password")
                || lower.contains("invalid password")
                || lower.contains("bad password")
                || lower.contains("login failed")
                || lower.contains("failed to login")
                || lower.contains("failed to log in");
    }

    public static boolean looksLikeAuthPrompt(String lower) {
        return looksLikeLoginPrompt(lower) || looksLikeRegisterPrompt(lower);
    }

    public static boolean looksLikeLoginPrompt(String lower) {
        if (looksLikeLoginSuccess(lower)) {
            return false;
        }
        if (looksLikeRegisterPrompt(lower) && !lower.contains("log in") && !lower.contains("login")) {
            return false;
        }
        return lower.contains("/login")
                || lower.contains("/l ")
                || lower.contains("please login")
                || lower.contains("please log in")
                || lower.contains("log in first")
                || lower.contains("login first")
                || lower.contains("use /login")
                || lower.contains("not logged in")
                || lower.contains("must log in")
                || lower.contains("must login")
                || lower.contains("need to log in")
                || lower.contains("need to login")
                || lower.contains("authenticate")
                || lower.contains("authentication required")
                || lower.contains("session timed out")
                || lower.contains("logged in yet")
                || lower.contains("you aren't logged in")
                || lower.contains("you are not logged in")
                || lower.contains("inicia sesión")
                || lower.contains("inicia sesion")
                || lower.contains("/registrar");
    }

    public static boolean looksLikeRegisterSuccess(String lower) {
        if (lower.contains("not registered")
                || lower.contains("already registered")
                || lower.contains("failed to register")
                || lower.contains("registration failed")
                || lower.contains("invalid")
                || lower.contains("wrong")) {
            return false;
        }
        return lower.contains("successfully registered")
                || lower.contains("registration successful")
                || lower.contains("registered successfully")
                || lower.contains("you have successfully registered")
                || lower.contains("you are now registered")
                || lower.contains("account registered")
                || lower.contains("registration complete")
                || lower.contains("has been registered")
                || lower.contains("account created")
                || (lower.contains("registered") && (lower.contains("success") || lower.contains("complete")));
    }

    public static boolean looksLikeLoginSuccess(String lower) {
        if (lower.contains("not logged") || lower.contains("must log in") || lower.contains("must login")) {
            return false;
        }
        return lower.contains("successful login")
                || lower.contains("successfully logged in")
                || lower.contains("logged in successfully")
                || lower.contains("login successful")
                || lower.contains("authenticated successfully")
                || lower.contains("you are now logged in")
                || lower.contains("welcome back")
                || (lower.contains("logged in") && lower.contains("success"));
    }

    /** Register instructions without a login alternative — new account path. */
    public static boolean looksLikeRegisterOnlyPrompt(String lower) {
        if (!looksLikeRegisterPrompt(lower)) {
            return false;
        }
        return !lower.contains("/login")
                && !lower.contains("please log in")
                && !lower.contains("please login")
                && !lower.contains("log in first");
    }

    public static boolean looksLikeAlreadyRegistered(String lower) {
        return lower.contains("already registered")
                || lower.contains("account already exists")
                || lower.contains("name is already registered")
                || lower.contains("username is already registered")
                || lower.contains("player already registered");
    }

    public static String normalizeChatLine(String line) {
        if (line == null) {
            return "";
        }
        String cleaned = line.replaceAll("\u001B\\[[0-9;?]*[ -/]*[@-~]", "");
        cleaned = cleaned.replaceAll("§.", "");
        return cleaned.trim();
    }

    public static String lower(String line) {
        return line == null ? "" : line.toLowerCase(Locale.ROOT);
    }
}
