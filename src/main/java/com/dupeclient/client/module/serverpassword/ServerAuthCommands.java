package com.dupeclient.client.module.serverpassword;

/** Builds safe /login and /register chat commands for auth plugins. */
public final class ServerAuthCommands {
    private ServerAuthCommands() {
    }

    public static boolean usesRegisterEmail(RegisterFormat format) {
        return format == RegisterFormat.EMAIL_THEN_PASSWORD || format == RegisterFormat.PASSWORD_THEN_EMAIL;
    }

    public static String buildLogin(String command, String username, String password) {
        String cmd = VaultInputValidator.sanitizeCommand(command, "login");
        VaultInputValidator.requireChatToken(password, "password");
        String user = "";
        if (username != null && !username.isBlank()) {
            String cleaned = VaultInputValidator.sanitizeRegisterIdentity(username);
            if (cleaned != null) {
                VaultInputValidator.requireChatToken(cleaned, "username");
                user = cleaned;
            }
        }
        if (user.isEmpty()) {
            return "/" + cmd + " " + password;
        }
        return "/" + cmd + " " + user + " " + password;
    }

    public static String buildRegister(String command, RegisterFormat format, String identity, String password) {
        String cmd = VaultInputValidator.sanitizeCommand(command, "register");
        VaultInputValidator.requireChatToken(password, "password");
        String args = switch (format) {
            case PASSWORD_ONLY -> password;
            case PASSWORD_REPEAT -> password + " " + password;
            case EMAIL_THEN_PASSWORD -> {
                String email = VaultInputValidator.sanitizeRegisterEmail(identity);
                VaultInputValidator.requireChatToken(email, "email");
                yield email + " " + password;
            }
            case PASSWORD_THEN_EMAIL -> {
                String email = VaultInputValidator.sanitizeRegisterEmail(identity);
                VaultInputValidator.requireChatToken(email, "email");
                yield password + " " + email;
            }
        };
        return "/" + cmd + " " + args;
    }
}
