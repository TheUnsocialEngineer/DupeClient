package com.dupeclient.client.module.serverpassword;

import java.util.Locale;
import java.util.regex.Pattern;

/** Validates and normalizes all vault inputs before database or network use. */
public final class VaultInputValidator {
    public static final int MAX_HOST_LEN = 253;
    public static final int MAX_USERNAME_LEN = 64;
    public static final int MAX_PASSWORD_LEN = 256;
    public static final int MAX_COMMAND_LEN = 32;
    public static final int MAX_NOTES_LEN = 512;
    public static final int MAX_DISPLAY_LEN = 128;
    public static final int MAX_CIPHER_BYTES = 16_384;
    public static final int MAX_NONCE_BYTES = 32;
    public static final int MAX_MAC_BYTES = 64;
    public static final int MAX_LOGIN_DELAY_TICKS = 20 * 60 * 20;

    private static final Pattern HOST_KEY = Pattern.compile("^[a-z0-9.:\\[\\]_-]{1,253}$");
    private static final Pattern CHAT_COMMAND = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{0,31}$");
    private static final Pattern USERNAME = Pattern.compile("^[\\w.-]{0,64}$");
    private static final Pattern REGISTER_EMAIL = Pattern.compile(
            "^[a-zA-Z0-9._+-]{1,32}@[a-zA-Z0-9.-]{1,24}\\.[a-zA-Z]{2,10}$");
    private static final Pattern SAFE_CHAT_TOKEN = Pattern.compile("^\\S{1,256}$");

    private VaultInputValidator() {
    }

    public static String requireHostKey(String address) {
        String normalized = ServerPasswordKeys.normalize(address);
        if (normalized.isEmpty()) {
            throw new VaultInputException("Server address is required");
        }
        if (normalized.length() > MAX_HOST_LEN || !HOST_KEY.matcher(normalized).matches()) {
            throw new VaultInputException("Invalid server address");
        }
        return normalized;
    }

    public static String optionalHostKey(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        return requireHostKey(address);
    }

    public static String sanitizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String cleaned = stripControls(username.trim());
        if (cleaned.length() > MAX_USERNAME_LEN || !USERNAME.matcher(cleaned).matches()) {
            throw new VaultInputException("Invalid username");
        }
        return cleaned;
    }

    public static boolean looksLikeRegisterEmail(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return REGISTER_EMAIL.matcher(stripControls(value.trim())).matches();
    }

    public static String sanitizeRegisterEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new VaultInputException("Email is required");
        }
        String cleaned = stripControls(email.trim().toLowerCase(Locale.ROOT));
        if (cleaned.length() > MAX_USERNAME_LEN || !REGISTER_EMAIL.matcher(cleaned).matches()) {
            throw new VaultInputException("Invalid email");
        }
        return cleaned;
    }

    /** Username or generated register email stored in the vault identity field. */
    public static String sanitizeRegisterIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            return null;
        }
        if (looksLikeRegisterEmail(identity)) {
            return sanitizeRegisterEmail(identity);
        }
        return sanitizeUsername(identity);
    }

    public static String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new VaultInputException("Password is required");
        }
        String cleaned = stripControls(password);
        if (cleaned.length() > MAX_PASSWORD_LEN || !SAFE_CHAT_TOKEN.matcher(cleaned).matches()) {
            throw new VaultInputException("Invalid password");
        }
        return cleaned;
    }

    public static String sanitizeCommand(String command, String fallback) {
        String value = command == null || command.isBlank() ? fallback : stripControls(command.trim());
        if (value.length() > MAX_COMMAND_LEN || !CHAT_COMMAND.matcher(value).matches()) {
            throw new VaultInputException("Invalid auth command");
        }
        return value;
    }

    public static String sanitizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        String cleaned = stripControls(displayName.trim());
        if (cleaned.length() > MAX_DISPLAY_LEN) {
            throw new VaultInputException("Display name is too long");
        }
        return cleaned;
    }

    public static String sanitizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String cleaned = stripControls(notes.trim());
        if (cleaned.length() > MAX_NOTES_LEN) {
            throw new VaultInputException("Notes are too long");
        }
        return cleaned;
    }

    public static long requireEntryId(long id) {
        if (id <= 0) {
            throw new VaultInputException("Invalid entry id");
        }
        return id;
    }

    public static ServerPasswordSettings sanitizeSettings(ServerPasswordSettings settings) {
        if (settings == null) {
            return ServerPasswordSettings.defaults();
        }
        int delay = Math.max(0, Math.min(MAX_LOGIN_DELAY_TICKS, settings.loginDelayTicks()));
        return new ServerPasswordSettings(
                settings.promptOnAuth(),
                settings.autoLogin(),
                settings.autoRegister(),
                settings.autoGeneratePassword(),
                delay
        );
    }

    public static ServerPasswordEntry sanitizeEntry(ServerPasswordEntry draft) {
        if (draft == null) {
            throw new VaultInputException("Entry is required");
        }
        String hostKey = requireHostKey(draft.hostKey());
        String profileName = requireProfileName(draft.profileName());
        String displayName = sanitizeDisplayName(draft.displayName());
        String username = sanitizeRegisterIdentity(draft.username());
        String password = requirePassword(draft.password());
        String loginCommand = sanitizeCommand(draft.loginCommand(), "login");
        String registerCommand = sanitizeCommand(draft.registerCommand(), "register");
        String notes = sanitizeNotes(draft.notes());
        long id = draft.id() < 0 ? 0L : draft.id();
        if (id > 0) {
            requireEntryId(id);
        }
        return new ServerPasswordEntry(
                id,
                hostKey,
                profileName,
                displayName == null ? hostKey : displayName,
                username,
                password,
                loginCommand,
                registerCommand,
                draft.autoLogin(),
                draft.autoRegister(),
                notes,
                draft.updatedAtEpochMs() > 0 ? draft.updatedAtEpochMs() : System.currentTimeMillis()
        );
    }

    /** Validates entry fields for storage; profile may be empty during legacy/crypto migration. */
    public static ServerPasswordEntry sanitizeEntryForStorage(ServerPasswordEntry draft) {
        if (draft == null) {
            throw new VaultInputException("Entry is required");
        }
        String hostKey = requireHostKey(draft.hostKey());
        String profileName = sanitizeProfileName(draft.profileName());
        String displayName = sanitizeDisplayName(draft.displayName());
        String username = sanitizeRegisterIdentity(draft.username());
        String password = requirePassword(draft.password());
        String loginCommand = sanitizeCommand(draft.loginCommand(), "login");
        String registerCommand = sanitizeCommand(draft.registerCommand(), "register");
        String notes = sanitizeNotes(draft.notes());
        long id = draft.id() < 0 ? 0L : draft.id();
        if (id > 0) {
            requireEntryId(id);
        }
        return new ServerPasswordEntry(
                id,
                hostKey,
                profileName,
                displayName == null ? hostKey : displayName,
                username,
                password,
                loginCommand,
                registerCommand,
                draft.autoLogin(),
                draft.autoRegister(),
                notes,
                draft.updatedAtEpochMs() > 0 ? draft.updatedAtEpochMs() : System.currentTimeMillis()
        );
    }

    public static String requireStoredHostKey(String hostKey) {
        if (hostKey == null || hostKey.isBlank()) {
            throw new VaultInputException("Corrupt entry: missing host");
        }
        String cleaned = stripControls(hostKey.trim().toLowerCase(Locale.ROOT));
        if (cleaned.length() > MAX_HOST_LEN || !HOST_KEY.matcher(cleaned).matches()) {
            throw new VaultInputException("Corrupt entry: invalid host");
        }
        return cleaned;
    }

    public static String requireStoredCommand(String command, String fallback) {
        try {
            return sanitizeCommand(command, fallback);
        } catch (VaultInputException ex) {
            throw new VaultInputException("Corrupt entry: invalid auth command");
        }
    }

    public static byte[] requireCipherBlob(byte[] blob, String field) {
        if (blob == null) {
            throw new VaultInputException("Corrupt entry: missing " + field);
        }
        if (blob.length > MAX_CIPHER_BYTES) {
            throw new VaultInputException("Corrupt entry: " + field + " too large");
        }
        return blob;
    }

    public static byte[] optionalCipherBlob(byte[] blob) {
        if (blob == null || blob.length == 0) {
            return new byte[0];
        }
        if (blob.length > MAX_CIPHER_BYTES) {
            throw new VaultInputException("Corrupt entry: ciphertext too large");
        }
        return blob;
    }

    public static byte[] requireNonce(byte[] nonce) {
        if (nonce == null || nonce.length == 0 || nonce.length > MAX_NONCE_BYTES) {
            throw new VaultInputException("Corrupt entry: invalid nonce");
        }
        return nonce;
    }

    public static byte[] optionalMac(byte[] mac) {
        if (mac == null || mac.length == 0) {
            return new byte[0];
        }
        if (mac.length > MAX_MAC_BYTES) {
            throw new VaultInputException("Corrupt entry: invalid integrity tag");
        }
        return mac;
    }

    public static String sanitizeProfileName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return "";
        }
        String cleaned = stripControls(profileName.trim().toLowerCase(Locale.ROOT));
        if (cleaned.isEmpty() || cleaned.length() > MAX_USERNAME_LEN || !USERNAME.matcher(cleaned).matches()) {
            throw new VaultInputException("Invalid profile name");
        }
        return cleaned;
    }

    public static String requireProfileName(String profileName) {
        String cleaned = sanitizeProfileName(profileName);
        if (cleaned.isEmpty()) {
            throw new VaultInputException("Profile name is required");
        }
        return cleaned;
    }

    public static String readStoredProfileName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return "";
        }
        return profileName.trim().toLowerCase(Locale.ROOT);
    }

    public static String readStoredPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new VaultInputException("Corrupt entry: missing password");
        }
        String cleaned = stripControls(password);
        if (cleaned.isEmpty() || cleaned.length() > MAX_PASSWORD_LEN) {
            throw new VaultInputException("Corrupt entry: invalid password");
        }
        return cleaned;
    }

    public static void requireChatToken(String value, String label) {
        if (value == null || value.isBlank() || !SAFE_CHAT_TOKEN.matcher(value).matches()) {
            throw new VaultInputException("Invalid " + label);
        }
    }

    private static String stripControls(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}
