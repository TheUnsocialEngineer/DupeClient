package com.dupeclient.client.module.serverpassword;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses common AuthMe-style /login and /register chat commands. */
public final class AuthCommandDetector {
    private static final Pattern LOGIN = Pattern.compile("^(?:/)?(?:login|log|l)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGISTER = Pattern.compile("^(?:/)?(?:register|reg|r)\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private AuthCommandDetector() {
    }

    public static Optional<ParsedAuthCommand> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        Optional<ParsedAuthCommand> login = parseLogin(trimmed);
        if (login.isPresent()) {
            return login;
        }
        return parseRegister(trimmed);
    }

    private static Optional<ParsedAuthCommand> parseLogin(String trimmed) {
        Matcher m = LOGIN.matcher(trimmed);
        if (!m.matches()) {
            return Optional.empty();
        }
        String[] parts = splitArgs(m.group(1));
        if (parts.length == 1) {
            return Optional.of(new ParsedAuthCommand(AuthCommandType.LOGIN, "", parts[0], RegisterFormat.PASSWORD_REPEAT));
        }
        if (parts.length >= 2) {
            return Optional.of(new ParsedAuthCommand(
                    AuthCommandType.LOGIN, parts[0], parts[parts.length - 1], RegisterFormat.PASSWORD_REPEAT));
        }
        return Optional.empty();
    }

    private static Optional<ParsedAuthCommand> parseRegister(String trimmed) {
        Matcher m = REGISTER.matcher(trimmed);
        if (!m.matches()) {
            return Optional.empty();
        }
        String[] parts = splitArgs(m.group(1));
        if (parts.length == 1) {
            return Optional.of(new ParsedAuthCommand(
                    AuthCommandType.REGISTER, "", parts[0], RegisterFormat.PASSWORD_ONLY));
        }
        if (parts.length == 2) {
            if (VaultInputValidator.looksLikeRegisterEmail(parts[0])
                    && !VaultInputValidator.looksLikeRegisterEmail(parts[1])) {
                return Optional.of(new ParsedAuthCommand(
                        AuthCommandType.REGISTER, parts[0], parts[1], RegisterFormat.EMAIL_THEN_PASSWORD));
            }
            if (!VaultInputValidator.looksLikeRegisterEmail(parts[0])
                    && VaultInputValidator.looksLikeRegisterEmail(parts[1])) {
                return Optional.of(new ParsedAuthCommand(
                        AuthCommandType.REGISTER, parts[1], parts[0], RegisterFormat.PASSWORD_THEN_EMAIL));
            }
            return Optional.of(new ParsedAuthCommand(
                    AuthCommandType.REGISTER, "", parts[0], RegisterFormat.PASSWORD_REPEAT));
        }
        if (parts.length >= 3) {
            if (VaultInputValidator.looksLikeRegisterEmail(parts[0])) {
                return Optional.of(new ParsedAuthCommand(
                        AuthCommandType.REGISTER, parts[0], parts[1], RegisterFormat.EMAIL_THEN_PASSWORD));
            }
            if (VaultInputValidator.looksLikeRegisterEmail(parts[1])) {
                return Optional.of(new ParsedAuthCommand(
                        AuthCommandType.REGISTER, parts[1], parts[0], RegisterFormat.PASSWORD_THEN_EMAIL));
            }
            return Optional.of(new ParsedAuthCommand(
                    AuthCommandType.REGISTER, parts[0], parts[1], RegisterFormat.PASSWORD_REPEAT));
        }
        return Optional.empty();
    }

    private static String[] splitArgs(String tail) {
        if (tail == null || tail.isBlank()) {
            return new String[0];
        }
        return tail.trim().split("\\s+");
    }

    public enum AuthCommandType {
        LOGIN,
        REGISTER
    }

    public record ParsedAuthCommand(
            AuthCommandType type,
            String username,
            String password,
            RegisterFormat registerFormat
    ) {
        public String commandLabel() {
            return type == AuthCommandType.LOGIN ? "login" : "register";
        }
    }
}
