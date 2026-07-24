package com.dupeclient.client.module.serverpassword;

import com.dupeclient.client.DupeClient;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class ServerPasswordManager {
    public static final ServerPasswordManager INSTANCE = new ServerPasswordManager();

    private final ServerPasswordDatabase database = new ServerPasswordDatabase();
    private final ServerPasswordVault vault = new ServerPasswordVault();
    private final SecureRandom random = new SecureRandom();

    private VaultAccessSession session;
    private ServerPasswordSettings settings = ServerPasswordSettings.defaults();
    private PendingSave pendingSave;
    private String pendingHostKey;
    private SessionAuthCapture sessionAuth;
    private RegisterFormat sessionRegisterFormat = RegisterFormat.PASSWORD_REPEAT;
    private int loginDelayTicks;
    private boolean registerAttemptedThisSession;

    private ServerPasswordManager() {
    }

    public void initialize() {
        database.open();
        settings = database.loadSettings();
    }

    public void tick(Minecraft client) {
        if (client == null || client.player == null || !isUnlocked()) {
            return;
        }
        if (loginDelayTicks > 0) {
            loginDelayTicks--;
            if (loginDelayTicks == 0) {
                tryAutoLogin(client);
            }
        }
    }

    public void onSessionJoined(Minecraft client) {
        registerAttemptedThisSession = false;
        sessionRegisterFormat = RegisterFormat.PASSWORD_REPEAT;
        loginDelayTicks = 0;
        if (!isUnlocked() || client == null) {
            return;
        }
        String host = currentHostKey(client);
        if (host == null) {
            return;
        }
        Optional<ServerPasswordEntry> entry = database.findByHost(requireSession(), host, requireCurrentProfile());
        if (entry.isEmpty()) {
            return;
        }
        if (entry.get().autoLogin() && settings.autoLogin()) {
            loginDelayTicks = Math.max(1, settings.loginDelayTicks());
        }
    }

    public void onSessionLeave() {
        loginDelayTicks = 0;
        pendingSave = null;
        pendingHostKey = null;
        registerAttemptedThisSession = false;
        sessionAuth = null;
        sessionRegisterFormat = RegisterFormat.PASSWORD_REPEAT;
    }

    public void onOutgoingChat(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        AuthCommandDetector.parse(raw).ifPresent(this::handleDetectedAuthCommand);
    }

    public void onIncomingChatLine(Minecraft client, String line) {
        if (client == null || line == null) {
            return;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        trySaveFromAuthFeedback(client, line, lower);

        if (lower.contains("register") && !ServerAuthPrompts.looksLikeRegisterSuccess(lower)) {
            RegisterFormat detected = RegisterFormatDetector.detectFromPrompt(line);
            if (detected != RegisterFormat.PASSWORD_REPEAT || lower.contains("email") || lower.contains("e-mail")) {
                sessionRegisterFormat = detected;
            }
        }

        if (!settings.autoRegister() || registerAttemptedThisSession) {
            return;
        }
        if (!ServerAuthPrompts.looksLikeRegisterPrompt(lower)) {
            return;
        }
        String host = currentHostKey(client);
        if (host == null) {
            return;
        }
        Optional<ServerPasswordEntry> existing = findEntryIfUnlocked(host);
        if (existing.isPresent() && existing.get().autoRegister()) {
            return;
        }
        if (existing.isPresent()) {
            return;
        }
        attemptAutoRegister(client, host);
    }

    public boolean isVaultInitialized() {
        return database.isVaultInitialized();
    }

    public boolean isUnlocked() {
        return session != null && session.isOpen();
    }

    public ServerPasswordSettings settings() {
        return settings;
    }

    public void saveSettings(ServerPasswordSettings next) {
        ServerPasswordSettings safe = VaultInputValidator.sanitizeSettings(next);
        this.settings = safe;
        if (isUnlocked()) {
            database.saveSettings(requireSession(), safe);
        }
    }

    public List<ServerPasswordEntry> listEntries() {
        return database.listEntries(requireSession(), requireCurrentProfile());
    }

    public List<ServerPasswordEntry> listAllEntriesForVault() {
        return database.listAllEntries(requireSession());
    }

    public Optional<ServerPasswordEntry> findEntry(String hostKey) {
        if (!isUnlocked()) {
            return Optional.empty();
        }
        try {
            return database.findByHost(requireSession(), hostKey, requireCurrentProfile());
        } catch (VaultInputException ex) {
            return Optional.empty();
        }
    }

    public String currentProfileName() {
        return requireCurrentProfile();
    }

    private String requireCurrentProfile() {
        Minecraft client = Minecraft.getInstance();
        String profile = VaultInputValidator.requireProfileName(playerName(client));
        if (profile.isEmpty()) {
            throw new VaultInputException("No logged-in Minecraft profile");
        }
        return profile;
    }

    private ServerPasswordEntry withCurrentProfile(ServerPasswordEntry entry) {
        return new ServerPasswordEntry(
                entry.id(),
                entry.hostKey(),
                requireCurrentProfile(),
                entry.displayName(),
                entry.username(),
                entry.password(),
                entry.loginCommand(),
                entry.registerCommand(),
                entry.autoLogin(),
                entry.autoRegister(),
                entry.notes(),
                entry.updatedAtEpochMs()
        );
    }

    public void createVault(char[] masterPassword) throws Exception {
        if (database.isVaultInitialized()) {
            throw new VaultAccessDeniedException("Vault already exists");
        }
        database.createVault(masterPassword);
        unlock(masterPassword);
    }

    public boolean unlock(char[] masterPassword) {
        if (!database.isVaultInitialized()) {
            return false;
        }
        byte[] salt = database.loadSalt();
        byte[] verifier = database.loadVerifier();
        int cryptoVersion = database.loadCryptoVersion();
        byte[] installId = database.loadInstallId();
        if (installId == null) {
            installId = new byte[0];
        }
        if (!vault.verifyMasterPassword(masterPassword, salt, verifier, cryptoVersion, installId)) {
            return false;
        }
        // Remember session captures across lock() — auto-register credentials lived here when vault was locked.
        SessionAuthCapture preservedAuth = sessionAuth;
        PendingSave preservedPending = pendingSave;
        String preservedPendingHost = pendingHostKey;
        RegisterFormat preservedRegisterFormat = sessionRegisterFormat;
        boolean preservedRegisterAttempted = registerAttemptedThisSession;
        try {
            lock();
            sessionAuth = preservedAuth;
            pendingSave = preservedPending;
            pendingHostKey = preservedPendingHost;
            sessionRegisterFormat = preservedRegisterFormat;
            registerAttemptedThisSession = preservedRegisterAttempted;
            if (cryptoVersion < ServerPasswordVault.CRYPTO_VERSION) {
                VaultAccessSession legacySession = database.beginSession(vault.deriveKeyV1(masterPassword, salt));
                database.migrateToCurrentCrypto(legacySession, masterPassword);
            }
            session = database.beginSession(vault.deriveKeyV2(masterPassword, salt));
            database.assertUnlocked(session);
            settings = database.loadSettings();
            flushSessionAuthCapture();
            return true;
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to unlock server password vault", ex);
            lock();
            return false;
        }
    }

    public void lock() {
        database.endSession();
        session = null;
        pendingSave = null;
        pendingHostKey = null;
        sessionAuth = null;
    }

    public void saveEntry(ServerPasswordEntry entry) throws Exception {
        database.upsertEntry(requireSession(), VaultInputValidator.sanitizeEntryForStorage(withCurrentProfile(entry)));
    }

    public void saveVaultEntry(ServerPasswordEntry entry) throws Exception {
        database.upsertEntry(requireSession(), VaultInputValidator.sanitizeEntryForStorage(entry));
    }

    public void deleteEntry(long id) {
        deleteEntry(id, requireCurrentProfile());
    }

    public void deleteEntry(long id, String profileName) {
        database.deleteEntry(requireSession(), id, profileName);
    }

    public void confirmPendingSave() {
        if (pendingSave == null || !isUnlocked()) {
            return;
        }
        try {
            PendingSave save = pendingSave;
            VaultInputValidator.requireHostKey(save.hostKey());
            VaultInputValidator.requirePassword(save.password());
            if (save.username() != null && !save.username().isBlank()) {
                VaultInputValidator.sanitizeUsername(save.username());
            }
            saveDetectedPassword(save);
            notifyPlayer(Component.literal("Saved password for " + pendingSave.hostKey()).withStyle(ChatFormatting.GREEN));
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to save pending password", ex);
            notifyPlayer(Component.literal("Failed to save password.").withStyle(ChatFormatting.RED));
        } finally {
            pendingSave = null;
            pendingHostKey = null;
        }
    }

    public void dismissPendingSave() {
        pendingSave = null;
        pendingHostKey = null;
        notifyPlayer(Component.literal("Password not saved.").withStyle(ChatFormatting.GRAY));
    }

    public static String currentHostKey(Minecraft client) {
        if (client == null) {
            return null;
        }
        if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            String normalized = ServerPasswordKeys.normalize(client.getCurrentServer().ip);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        if (client.getConnection() != null && client.getConnection().getConnection() != null
                && client.getConnection().getConnection().getRemoteAddress() != null) {
            String raw = client.getConnection().getConnection().getRemoteAddress().toString();
            if (raw != null) {
                raw = raw.replaceFirst("^/", "").trim();
                String normalized = ServerPasswordKeys.normalize(raw);
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
        }
        return null;
    }

    public static String playerName(Minecraft client) {
        if (client == null || client.getUser() == null) {
            return "";
        }
        return client.getUser().getName();
    }

    private void handleDetectedAuthCommand(AuthCommandDetector.ParsedAuthCommand cmd) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        String host = currentHostKey(client);
        if (host == null) {
            return;
        }
        String username = resolveUsername(client, cmd.username());
        rememberAuthCapture(host, username, cmd.password(), cmd.type());
        if (cmd.type() == AuthCommandDetector.AuthCommandType.REGISTER && ServerAuthCommands.usesRegisterEmail(cmd.registerFormat())) {
            sessionRegisterFormat = cmd.registerFormat();
        }
        if (!isUnlocked()) {
            notifyPlayer(Component.literal("Unlock the password vault to save server passwords (credentials remembered for this session).")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        Optional<ServerPasswordEntry> existing = database.findByHost(requireSession(), host, requireCurrentProfile());
        if (existing.isPresent()) {
            try {
                if (cmd.type() == AuthCommandDetector.AuthCommandType.REGISTER && settings.autoRegister()) {
                    commitAutoRegisterCredentials(host, username, cmd.password(),
                            existing.map(ServerPasswordEntry::notes).orElse(null));
                } else {
                    saveDetectedPassword(new PendingSave(host, username, cmd.password(), cmd.type()));
                }
            } catch (Exception ex) {
                DupeClient.LOGGER.error("Failed to update auth password", ex);
            }
            return;
        }
        // Auto-register / silent mode always commits; otherwise prompt.
        if (!settings.promptOnAuth()
                || (settings.autoRegister() && cmd.type() == AuthCommandDetector.AuthCommandType.REGISTER)) {
            try {
                if (cmd.type() == AuthCommandDetector.AuthCommandType.REGISTER && settings.autoRegister()) {
                    commitAutoRegisterCredentials(host, username, cmd.password(), "Auto-generated");
                } else {
                    saveDetectedPassword(new PendingSave(host, username, cmd.password(), cmd.type()));
                }
            } catch (Exception ex) {
                DupeClient.LOGGER.error("Failed to auto-save auth password", ex);
            }
            return;
        }
        pendingHostKey = host;
        pendingSave = new PendingSave(host, username, cmd.password(), cmd.type());
        MutableComponent save = Component.literal("[Save]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/vault save"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Save this password to the vault"))));
        MutableComponent dismiss = Component.literal("[Dismiss]").withStyle(ChatFormatting.RED)
                .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/vault dismiss"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Do not save"))));
        notifyPlayer(Component.literal("Save " + cmd.commandLabel() + " password for " + host + "? ").append(save).append(Component.literal(" ")).append(dismiss));
    }

    private void saveDetectedPassword(PendingSave save) throws Exception {
        Optional<ServerPasswordEntry> existing = database.findByHost(requireSession(), save.hostKey(), requireCurrentProfile());
        ServerPasswordEntry entry = existing.map(e -> new ServerPasswordEntry(
                e.id(),
                save.hostKey(),
                requireCurrentProfile(),
                save.hostKey(),
                save.username(),
                save.password(),
                e.loginCommand(),
                e.registerCommand(),
                e.autoLogin(),
                e.autoRegister(),
                e.notes(),
                System.currentTimeMillis()
        )).orElseGet(() -> new ServerPasswordEntry(
                0L,
                save.hostKey(),
                requireCurrentProfile(),
                save.hostKey(),
                save.username(),
                save.password(),
                "login",
                "register",
                true,
                save.type() == AuthCommandDetector.AuthCommandType.REGISTER,
                null,
                System.currentTimeMillis()
        ));
        database.upsertEntry(requireSession(), entry);
    }

    private void tryAutoLogin(Minecraft client) {
        String host = currentHostKey(client);
        if (host == null || client.player == null || client.getConnection() == null) {
            return;
        }
        database.findByHost(requireSession(), host, requireCurrentProfile()).ifPresent(entry -> {
            if (!entry.autoLogin()) {
                return;
            }
            sendAuthCommand(client, entry.loginCommand(), entry.username(), entry.password());
            notifyPlayer(Component.literal("Auto-login sent for " + host).withStyle(ChatFormatting.DARK_AQUA));
        });
    }

    private void attemptAutoRegister(Minecraft client, String host) {
        if (!settings.autoGeneratePassword()) {
            return;
        }
        registerAttemptedThisSession = true;
        RegisterFormat format = sessionRegisterFormat;
        String password = generatePassword();
        String identity = ServerAuthCommands.usesRegisterEmail(format) ? generateRandomEmail() : playerName(client);
        // Remember for flush-on-unlock / commit-on-success. Persist only after success (or immediately if unlocked
        // so auto-login still works if the success message is nonstandard).
        rememberAuthCapture(host, identity, password, AuthCommandDetector.AuthCommandType.REGISTER);
        boolean saved = false;
        String notes = ServerAuthCommands.usesRegisterEmail(format) ? "Auto-generated (email register)" : "Auto-generated";
        if (isUnlocked()) {
            try {
                commitAutoRegisterCredentials(host, identity, password, notes);
                saved = true;
            } catch (Exception ex) {
                DupeClient.LOGGER.error("Auto-register save failed for {}", host, ex);
            }
        }
        sendRegister(client, "register", format, identity, password);
        if (saved) {
            if (ServerAuthCommands.usesRegisterEmail(format)) {
                notifyPlayer(Component.literal("Auto-register sent for " + host + " with " + identity + " (saved to vault).")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                notifyPlayer(Component.literal("Auto-register sent for " + host + " (password saved to vault).")
                        .withStyle(ChatFormatting.GREEN));
            }
        } else {
            notifyPlayer(Component.literal("Auto-register sent for " + host + ". Unlock the vault to save the password.")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private void commitAutoRegisterCredentials(String host, String identity, String password, String notes)
            throws Exception {
        Optional<ServerPasswordEntry> existing = database.findByHost(requireSession(), host, requireCurrentProfile());
        ServerPasswordEntry entry = existing.map(e -> new ServerPasswordEntry(
                e.id(),
                host,
                requireCurrentProfile(),
                host,
                identity,
                password,
                e.loginCommand() == null || e.loginCommand().isBlank() ? "login" : e.loginCommand(),
                e.registerCommand() == null || e.registerCommand().isBlank() ? "register" : e.registerCommand(),
                true,
                true,
                notes != null ? notes : e.notes(),
                System.currentTimeMillis()
        )).orElseGet(() -> new ServerPasswordEntry(
                0L,
                host,
                requireCurrentProfile(),
                host,
                identity,
                password,
                "login",
                "register",
                true,
                true,
                notes,
                System.currentTimeMillis()
        ));
        database.upsertEntry(requireSession(), entry);
    }

    public void sendAuthCommand(Minecraft client, String command, String username, String password) {
        if (client == null || client.getConnection() == null) {
            return;
        }
        try {
            client.getConnection().sendCommand(
                    ServerAuthCommands.buildLogin(command, username, password).substring(1));
        } catch (VaultInputException ex) {
            DupeClient.LOGGER.warn("Blocked unsafe auth command: {}", ex.getMessage());
        }
    }

    private void sendRegister(Minecraft client, String command, RegisterFormat format, String identity, String password) {
        if (client == null || client.getConnection() == null) {
            return;
        }
        try {
            client.getConnection().sendCommand(
                    ServerAuthCommands.buildRegister(command, format, identity, password).substring(1));
        } catch (VaultInputException ex) {
            DupeClient.LOGGER.warn("Blocked unsafe register command: {}", ex.getMessage());
        }
    }

    private String generateRandomEmail() {
        return VaultInputValidator.sanitizeRegisterEmail(
                randomAlphanumeric(10) + "@" + randomAlphanumeric(8) + ".mail");
    }

    private String randomAlphanumeric(int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private String generatePassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static final Pattern ANNOUNCED_PASSWORD = Pattern.compile(
            "(?i)(?:tried auto registering with password|password(?:\\s+is)?|passwd(?:\\s+is)?)[:\\s]+\\**([\\w!@#$%^&*._-]{4,256})\\**");

    private void trySaveFromAuthFeedback(Minecraft client, String line, String lower) {
        Optional<String> announced = extractAnnouncedPassword(line);
        announced.ifPresent(password -> {
            String host = currentHostKey(client);
            if (host != null) {
                String identity = sessionAuth != null && host.equals(sessionAuth.hostKey()) && sessionAuth.username() != null
                        ? sessionAuth.username()
                        : playerName(client);
                rememberAuthCapture(host, identity, password, AuthCommandDetector.AuthCommandType.REGISTER);
            }
        });

        AuthCommandDetector.AuthCommandType type = null;
        if (ServerAuthPrompts.looksLikeRegisterSuccess(lower)) {
            type = AuthCommandDetector.AuthCommandType.REGISTER;
        } else if (ServerAuthPrompts.looksLikeLoginSuccess(lower)) {
            type = AuthCommandDetector.AuthCommandType.LOGIN;
        }
        if (type == null) {
            if (announced.isEmpty()) {
                return;
            }
            type = AuthCommandDetector.AuthCommandType.REGISTER;
        }
        if (!isUnlocked()) {
            return;
        }
        String host = currentHostKey(client);
        if (host == null) {
            return;
        }
        String username = playerName(client);
        String password = announced.orElse(null);
        if ((password == null || password.isBlank()) && sessionAuth != null && host.equals(sessionAuth.hostKey())) {
            password = sessionAuth.password();
            if (sessionAuth.username() != null && !sessionAuth.username().isBlank()) {
                username = sessionAuth.username();
            }
        }
        if (password == null || password.isBlank()) {
            return;
        }
        try {
            Optional<ServerPasswordEntry> existing = database.findByHost(requireSession(), host, requireCurrentProfile());
            if (existing.isPresent() && password.equals(existing.get().password())) {
                // Still ensure auto-login flags stay on after a successful auto-register.
                if (type == AuthCommandDetector.AuthCommandType.REGISTER
                        && (!existing.get().autoLogin() || !existing.get().autoRegister())) {
                    commitAutoRegisterCredentials(host, username, password, existing.get().notes());
                }
                sessionAuth = null;
                return;
            }
            boolean autoCommit = settings.autoRegister()
                    || (sessionAuth != null && sessionAuth.type() == AuthCommandDetector.AuthCommandType.REGISTER);
            if (existing.isEmpty() && settings.promptOnAuth() && !autoCommit) {
                pendingHostKey = host;
                pendingSave = new PendingSave(host, username, password, type);
                MutableComponent save = Component.literal("[Save]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                        .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/vault save"))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Save this password to the vault"))));
                MutableComponent dismiss = Component.literal("[Dismiss]").withStyle(ChatFormatting.RED)
                        .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/vault dismiss"))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Do not save"))));
                notifyPlayer(Component.literal("Save " + type.name().toLowerCase(Locale.ROOT) + " password for " + host + "? ")
                        .append(save).append(Component.literal(" ")).append(dismiss));
                return;
            }
            if (type == AuthCommandDetector.AuthCommandType.REGISTER && autoCommit) {
                commitAutoRegisterCredentials(host, username, password,
                        ServerAuthCommands.usesRegisterEmail(sessionRegisterFormat)
                                ? "Auto-generated (email register)" : "Auto-generated");
            } else {
                saveDetectedPassword(new PendingSave(host, username, password, type));
            }
            notifyPlayer(Component.literal("Saved " + type.name().toLowerCase(Locale.ROOT) + " credentials for " + host)
                    .withStyle(ChatFormatting.GREEN));
            sessionAuth = null;
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to save credentials from auth feedback", ex);
        }
    }

    private static Optional<String> extractAnnouncedPassword(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = ANNOUNCED_PASSWORD.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(VaultInputValidator.requirePassword(matcher.group(1)));
        } catch (VaultInputException ex) {
            return Optional.empty();
        }
    }

    private void rememberAuthCapture(String hostKey, String username, String password,
                                     AuthCommandDetector.AuthCommandType type) {
        if (hostKey == null || hostKey.isBlank() || password == null || password.isBlank()) {
            return;
        }
        try {
            VaultInputValidator.requireHostKey(hostKey);
            VaultInputValidator.requirePassword(password);
            String user = username == null || username.isBlank() ? null : VaultInputValidator.sanitizeRegisterIdentity(username);
            sessionAuth = new SessionAuthCapture(hostKey, user, password, type);
        } catch (VaultInputException ex) {
            DupeClient.LOGGER.debug("Ignored invalid session auth capture: {}", ex.getMessage());
        }
    }

    private void flushSessionAuthCapture() {
        if (sessionAuth == null || !isUnlocked()) {
            return;
        }
        try {
            Optional<ServerPasswordEntry> existing = database.findByHost(
                    requireSession(), sessionAuth.hostKey(), requireCurrentProfile());
            if (existing.isPresent() && sessionAuth.password().equals(existing.get().password())) {
                if (sessionAuth.type() == AuthCommandDetector.AuthCommandType.REGISTER
                        && (!existing.get().autoLogin() || !existing.get().autoRegister())) {
                    commitAutoRegisterCredentials(
                            sessionAuth.hostKey(),
                            sessionAuth.username(),
                            sessionAuth.password(),
                            existing.get().notes());
                }
                sessionAuth = null;
                return;
            }
            String hostKey = sessionAuth.hostKey();
            if (sessionAuth.type() == AuthCommandDetector.AuthCommandType.REGISTER) {
                commitAutoRegisterCredentials(
                        sessionAuth.hostKey(),
                        sessionAuth.username(),
                        sessionAuth.password(),
                        "Auto-generated");
            } else {
                saveDetectedPassword(new PendingSave(
                        sessionAuth.hostKey(),
                        sessionAuth.username(),
                        sessionAuth.password(),
                        sessionAuth.type()));
            }
            sessionAuth = null;
            notifyPlayer(Component.literal("Saved remembered credentials for " + hostKey).withStyle(ChatFormatting.GREEN));
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to flush remembered auth credentials", ex);
        }
    }

    private Optional<ServerPasswordEntry> findEntryIfUnlocked(String host) {
        if (!isUnlocked()) {
            return Optional.empty();
        }
        try {
            return database.findByHost(requireSession(), host, requireCurrentProfile());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static String resolveUsername(Minecraft client, String parsed) {
        if (parsed != null && !parsed.isBlank()) {
            return parsed.trim();
        }
        return playerName(client);
    }

    private VaultAccessSession requireSession() {
        if (!isUnlocked()) {
            throw new VaultAccessDeniedException("Vault is locked");
        }
        return session;
    }

    private static void notifyPlayer(Component text) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(text);
        }
    }

    private record PendingSave(String hostKey, String username, String password, AuthCommandDetector.AuthCommandType type) {
    }

    private record SessionAuthCapture(String hostKey, String username, String password,
                                      AuthCommandDetector.AuthCommandType type) {
    }
}
