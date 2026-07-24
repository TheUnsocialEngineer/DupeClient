package com.dupeclient.client.module.serverpassword;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.DupeClientConfigDir;
import net.fabricmc.loader.api.FabricLoader;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ServerPasswordDatabase {
    private static final AtomicLong NEXT_SESSION_ID = new AtomicLong(1);
    private static final Map<String, Set<String>> ALLOWED_COLUMNS = Map.of(
            "vault_meta", Set.of("crypto_version", "install_id", "vault_magic", "meta_mac"),
            "server_entries", Set.of("username_cipher", "username_nonce", "notes_cipher", "notes_nonce", "entry_mac", "profile_name")
    );
    private static final int SCHEMA_VERSION = 3;

    private final ServerPasswordVault vault = new ServerPasswordVault();
    private final Path dbPath;
    private Connection connection;
    private VaultAccessSession activeSession;
    private long activeSessionId = -1L;

    public ServerPasswordDatabase() {
        this.dbPath = DupeClientConfigDir.root()
                .resolve(DupeClientConfigDir.DIR_SERVER_PASSWORD)
                .resolve(DupeClientConfigDir.FILE_SERVER_PASSWORD_DB);
    }

    public synchronized void open() {
        assertModRuntime();
        if (connection != null) {
            return;
        }
        try {
            Files.createDirectories(dbPath.getParent());
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            connection.setAutoCommit(true);
            configureConnection();
            initSchema();
            validateVaultSeal();
            DupeClient.LOGGER.info("Server password vault database ready at {}", dbPath);
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to open server password database", ex);
            close();
        }
    }

    public synchronized void close() {
        endSession();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    public synchronized VaultAccessSession beginSession(SecretKey masterKey) {
        ensureOpen();
        endSession();
        long sessionId = NEXT_SESSION_ID.getAndIncrement();
        activeSession = new VaultAccessSession(masterKey, sessionId);
        activeSessionId = sessionId;
        return activeSession;
    }

    public synchronized void endSession() {
        if (activeSession != null) {
            activeSession.close();
            activeSession = null;
            activeSessionId = -1L;
        }
    }

    public synchronized boolean isVaultInitialized() {
        return queryVaultMetaBytes("salt") != null;
    }

    public synchronized int loadCryptoVersion() {
        Integer version = queryVaultMetaInt("crypto_version");
        return version == null ? 1 : version;
    }

    public synchronized byte[] loadSalt() {
        return queryVaultMetaBytes("salt");
    }

    public synchronized byte[] loadVerifier() {
        return queryVaultMetaBytes("verifier");
    }

    public synchronized byte[] loadInstallId() {
        return queryVaultMetaBytes("install_id");
    }

    public synchronized byte[] loadMetaMac() {
        return queryVaultMetaBytes("meta_mac");
    }

    public synchronized void createVault(char[] masterPassword) throws Exception {
        ensureOpen();
        if (isVaultInitialized()) {
            throw new VaultAccessDeniedException("Vault already exists");
        }
        byte[] salt = vault.newSalt();
        byte[] installId = vault.newInstallId();
        SecretKey key = vault.deriveKeyV2(masterPassword, salt);
        byte[] verifier = vault.computeVerifierV2(key, installId);
        byte[] metaMac = vault.computeMetaMac(key, salt, verifier, installId, ServerPasswordVault.CRYPTO_VERSION);
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO vault_meta
                (id, salt, verifier, created_at, crypto_version, install_id, vault_magic, meta_mac)
                VALUES (1, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setBytes(1, salt);
            ps.setBytes(2, verifier);
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, ServerPasswordVault.CRYPTO_VERSION);
            ps.setBytes(5, installId);
            ps.setBytes(6, vault.vaultMagic());
            ps.setBytes(7, metaMac);
            ps.executeUpdate();
        }
        ensureSettingsRow();
    }

    public synchronized void migrateToCurrentCrypto(VaultAccessSession session, char[] masterPassword) throws Exception {
        requireSession(session);
        if (loadCryptoVersion() >= ServerPasswordVault.CRYPTO_VERSION) {
            return;
        }

        byte[] salt = loadSalt();
        byte[] oldVerifier = loadVerifier();
        SecretKey v1Key = vault.deriveKeyV1(masterPassword, salt);
        if (!vault.verifyMasterPassword(masterPassword, salt, oldVerifier, 1, new byte[0])) {
            throw new VaultAccessDeniedException("Vault migration failed: master password mismatch");
        }

        List<ServerPasswordEntry> plaintextEntries = listEntriesV1(v1Key);
        byte[] installId = vault.newInstallId();
        SecretKey v2Key = vault.deriveKeyV2(masterPassword, salt);
        byte[] verifier = vault.computeVerifierV2(v2Key, installId);
        byte[] metaMac = vault.computeMetaMac(v2Key, salt, verifier, installId, ServerPasswordVault.CRYPTO_VERSION);

        try (PreparedStatement ps = connection.prepareStatement(
                """
                UPDATE vault_meta
                SET verifier = ?, crypto_version = ?, install_id = ?, vault_magic = ?, meta_mac = ?
                WHERE id = 1
                """)) {
            ps.setBytes(1, verifier);
            ps.setInt(2, ServerPasswordVault.CRYPTO_VERSION);
            ps.setBytes(3, installId);
            ps.setBytes(4, vault.vaultMagic());
            ps.setBytes(5, metaMac);
            ps.executeUpdate();
        }

        endSession();
        VaultAccessSession migrationSession = beginSession(v2Key);
        for (ServerPasswordEntry entry : plaintextEntries) {
            upsertEntry(migrationSession, entry);
        }
        verifyUnlockedMeta(migrationSession);
        endSession();
    }

    public synchronized ServerPasswordSettings loadSettings() {
        ensureOpen();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT prompt_on_auth, auto_login, auto_register, auto_generate_password, login_delay_ticks FROM settings WHERE id = ?")) {
            ps.setInt(1, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return ServerPasswordSettings.defaults();
                }
                return VaultInputValidator.sanitizeSettings(new ServerPasswordSettings(
                        rs.getInt("prompt_on_auth") != 0,
                        rs.getInt("auto_login") != 0,
                        rs.getInt("auto_register") != 0,
                        rs.getInt("auto_generate_password") != 0,
                        rs.getInt("login_delay_ticks")
                ));
            }
        } catch (SQLException ex) {
            DupeClient.LOGGER.error("Failed to load server password settings", ex);
            return ServerPasswordSettings.defaults();
        }
    }

    public synchronized void saveSettings(VaultAccessSession session, ServerPasswordSettings settings) {
        requireSession(session);
        ensureOpen();
        ServerPasswordSettings safe = VaultInputValidator.sanitizeSettings(settings);
        ensureSettingsRow();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE settings SET prompt_on_auth=?, auto_login=?, auto_register=?, auto_generate_password=?, login_delay_ticks=? WHERE id=?")) {
            ps.setInt(1, safe.promptOnAuth() ? 1 : 0);
            ps.setInt(2, safe.autoLogin() ? 1 : 0);
            ps.setInt(3, safe.autoRegister() ? 1 : 0);
            ps.setInt(4, safe.autoGeneratePassword() ? 1 : 0);
            ps.setInt(5, safe.loginDelayTicks());
            ps.setInt(6, 1);
            ps.executeUpdate();
        } catch (SQLException ex) {
            DupeClient.LOGGER.error("Failed to save server password settings", ex);
        }
    }

    public synchronized List<ServerPasswordEntry> listEntries(VaultAccessSession session, String profileName) {
        requireSession(session);
        String profile = VaultInputValidator.requireProfileName(profileName);
        return queryEntries(session, """
                SELECT * FROM server_entries
                WHERE profile_name = ? OR profile_name = ''
                ORDER BY host_key COLLATE NOCASE
                """, profile);
    }

    /** Lists every stored entry for the vault management UI. */
    public synchronized List<ServerPasswordEntry> listAllEntries(VaultAccessSession session) {
        requireSession(session);
        return queryEntries(session, """
                SELECT * FROM server_entries
                ORDER BY host_key COLLATE NOCASE, profile_name COLLATE NOCASE
                """, null);
    }

    private List<ServerPasswordEntry> queryEntries(VaultAccessSession session, String sql, String profileName) {
        List<ServerPasswordEntry> out = new ArrayList<>();
        ensureOpen();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (profileName != null) {
                ps.setString(1, profileName);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        out.add(readEntry(rs, session.masterKey()));
                    } catch (Exception ex) {
                        DupeClient.LOGGER.warn("Skipping unreadable vault entry id={}: {}", rs.getLong("id"), ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to list server password entries", ex);
        }
        return out;
    }

    public synchronized Optional<ServerPasswordEntry> findByHost(VaultAccessSession session, String hostKey, String profileName) {
        requireSession(session);
        String safeHost = VaultInputValidator.requireHostKey(hostKey);
        String profile = VaultInputValidator.requireProfileName(profileName);
        ensureOpen();
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT * FROM server_entries
                WHERE host_key = ? AND (profile_name = ? OR profile_name = '')
                ORDER BY CASE WHEN profile_name = ? THEN 0 ELSE 1 END, id DESC
                LIMIT 1
                """)) {
            ps.setString(1, safeHost);
            ps.setString(2, profile);
            ps.setString(3, profile);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readEntry(rs, session.masterKey()));
                }
            }
        } catch (VaultAccessDeniedException | VaultInputException ex) {
            throw ex;
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to load server password entry for {} / {}", safeHost, profile, ex);
        }
        return Optional.empty();
    }

    public synchronized void upsertEntry(VaultAccessSession session, ServerPasswordEntry draft) throws Exception {
        requireSession(session);
        ensureOpen();
        ServerPasswordEntry entry = VaultInputValidator.sanitizeEntryForStorage(draft);
        SecretKey key = session.masterKey();
        ServerPasswordVault.EncryptedSecret password = vault.encrypt(entry.password(), key, entry.hostKey());
        ServerPasswordVault.EncryptedSecret username = vault.encryptNullable(entry.username(), key, entry.hostKey());
        ServerPasswordVault.EncryptedSecret notes = vault.encryptNullable(entry.notes(), key, entry.hostKey());
        long now = System.currentTimeMillis();
        String loginCommand = entry.loginCommand();
        String registerCommand = entry.registerCommand();
        byte[] entryMac = vault.computeEntryMac(key, entry.hostKey(), entry.profileName(), password, username, notes,
                loginCommand, registerCommand, entry.autoLogin(), entry.autoRegister(), now);

        claimLegacyHost(entry.hostKey(), entry.profileName());

        if (entry.id() > 0) {
            long entryId = VaultInputValidator.requireEntryId(entry.id());
            if (!entryExists(entryId, entry.profileName())) {
                throw new VaultInputException("Entry not found");
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    UPDATE server_entries SET host_key=?, profile_name=?, display_name=?, username=?, username_cipher=?,
                    username_nonce=?, password_cipher=?, password_nonce=?, login_command=?, register_command=?,
                    auto_login=?, auto_register=?, notes=?, notes_cipher=?, notes_nonce=?, entry_mac=?, updated_at=?
                    WHERE id=? AND profile_name = ?
                    """)) {
                bindEntry(ps, entry, password, username, notes, entryMac, now);
                ps.setLong(17, entryId);
                ps.setString(18, entry.profileName());
                int updated = ps.executeUpdate();
                if (updated != 1) {
                    throw new VaultInputException("Entry update rejected");
                }
            }
        } else {
            try {
                executeInsertUpsert(entry, password, username, notes, entryMac, now);
            } catch (SQLException ex) {
                if (!hasProfileScopedUniqueIndex(connection.createStatement())) {
                    DupeClient.LOGGER.warn("Vault insert failed ({}); retrying legacy host-key upsert", ex.getMessage());
                    upsertEntryLegacy(entry, password, username, notes, entryMac, now);
                    try (Statement st = connection.createStatement()) {
                        migrateServerEntriesProfileKey(st);
                        st.execute("PRAGMA user_version = " + SCHEMA_VERSION);
                    }
                } else {
                    throw ex;
                }
            }
        }
        flushWrites();
    }

    private void executeInsertUpsert(ServerPasswordEntry entry, ServerPasswordVault.EncryptedSecret password,
                                   ServerPasswordVault.EncryptedSecret username,
                                   ServerPasswordVault.EncryptedSecret notes, byte[] entryMac, long now)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO server_entries (host_key, profile_name, display_name, username, username_cipher,
                username_nonce, password_cipher, password_nonce, login_command, register_command, auto_login,
                auto_register, notes, notes_cipher, notes_nonce, entry_mac, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(host_key, profile_name) DO UPDATE SET
                display_name=excluded.display_name, username=excluded.username, username_cipher=excluded.username_cipher,
                username_nonce=excluded.username_nonce, password_cipher=excluded.password_cipher,
                password_nonce=excluded.password_nonce, login_command=excluded.login_command,
                register_command=excluded.register_command, auto_login=excluded.auto_login,
                auto_register=excluded.auto_register, notes=excluded.notes, notes_cipher=excluded.notes_cipher,
                notes_nonce=excluded.notes_nonce, entry_mac=excluded.entry_mac, updated_at=excluded.updated_at
                """)) {
            bindEntryInsert(ps, entry, password, username, notes, entryMac, now);
            ps.executeUpdate();
        }
    }

    private void upsertEntryLegacy(ServerPasswordEntry entry, ServerPasswordVault.EncryptedSecret password,
                                   ServerPasswordVault.EncryptedSecret username,
                                   ServerPasswordVault.EncryptedSecret notes, byte[] entryMac, long now)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO server_entries (host_key, profile_name, display_name, username, username_cipher,
                username_nonce, password_cipher, password_nonce, login_command, register_command, auto_login,
                auto_register, notes, notes_cipher, notes_nonce, entry_mac, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(host_key) DO UPDATE SET
                profile_name=excluded.profile_name, display_name=excluded.display_name, username=excluded.username,
                username_cipher=excluded.username_cipher, username_nonce=excluded.username_nonce,
                password_cipher=excluded.password_cipher, password_nonce=excluded.password_nonce,
                login_command=excluded.login_command, register_command=excluded.register_command,
                auto_login=excluded.auto_login, auto_register=excluded.auto_register, notes=excluded.notes,
                notes_cipher=excluded.notes_cipher, notes_nonce=excluded.notes_nonce, entry_mac=excluded.entry_mac,
                updated_at=excluded.updated_at
                """)) {
            bindEntryInsert(ps, entry, password, username, notes, entryMac, now);
            ps.executeUpdate();
        }
    }

    private void claimLegacyHost(String hostKey, String profileName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE server_entries SET profile_name = ? WHERE host_key = ? AND profile_name = ''")) {
            ps.setString(1, profileName);
            ps.setString(2, hostKey);
            ps.executeUpdate();
        }
    }

    private void flushWrites() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            connection.commit();
        }
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA wal_checkpoint(PASSIVE)");
        }
    }

    public synchronized void deleteEntry(VaultAccessSession session, long id, String profileName) {
        requireSession(session);
        long entryId = VaultInputValidator.requireEntryId(id);
        String profile = VaultInputValidator.readStoredProfileName(profileName);
        ensureOpen();
        if (!entryExists(entryId, profile)) {
            throw new VaultInputException("Entry not found");
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM server_entries WHERE id = ? AND profile_name = ?")) {
            ps.setLong(1, entryId);
            ps.setString(2, profile);
            int deleted = ps.executeUpdate();
            if (deleted != 1) {
                throw new VaultInputException("Entry delete rejected");
            }
        } catch (SQLException ex) {
            DupeClient.LOGGER.error("Failed to delete server password entry {}", entryId, ex);
            throw new VaultAccessDeniedException("Failed to delete entry");
        }
        try {
            flushWrites();
        } catch (SQLException ex) {
            DupeClient.LOGGER.error("Failed to flush vault delete", ex);
        }
    }

    public synchronized void assertUnlocked(VaultAccessSession session) {
        verifyUnlockedMeta(session);
    }

    private boolean entryExists(long id, String profileName) {
        String profile = VaultInputValidator.readStoredProfileName(profileName);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM server_entries WHERE id = ? AND profile_name = ?")) {
            ps.setLong(1, id);
            ps.setString(2, profile);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new VaultAccessDeniedException("Failed to verify entry");
        }
    }

    private List<ServerPasswordEntry> listEntriesV1(SecretKey v1Key) throws Exception {
        List<ServerPasswordEntry> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM server_entries ORDER BY host_key COLLATE NOCASE")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readEntryV1(rs, v1Key));
                }
            }
        }
        return out;
    }

    private ServerPasswordEntry readEntryV1(ResultSet rs, SecretKey key) throws Exception {
        String hostKey = VaultInputValidator.requireStoredHostKey(rs.getString("host_key"));
        byte[] passwordNonce = VaultInputValidator.requireNonce(rs.getBytes("password_nonce"));
        byte[] passwordCipher = VaultInputValidator.requireCipherBlob(rs.getBytes("password_cipher"), "password_cipher");
        String password = vault.decrypt(passwordNonce, passwordCipher, key, hostKey);
        String username = rs.getString("username");
        if (username != null) {
            username = username.trim();
            if (username.isEmpty()) {
                username = null;
            }
        }
        String notes = rs.getString("notes");
        if (notes != null) {
            notes = notes.trim();
            if (notes.isEmpty()) {
                notes = null;
            }
        }
        String displayName = rs.getString("display_name");
        if (displayName != null) {
            displayName = displayName.trim();
            if (displayName.isEmpty()) {
                displayName = null;
            }
        }
        return new ServerPasswordEntry(
                rs.getLong("id"),
                hostKey,
                "",
                displayName == null ? hostKey : displayName,
                username,
                password,
                defaultStoredCommand(rs.getString("login_command"), "login"),
                defaultStoredCommand(rs.getString("register_command"), "register"),
                rs.getInt("auto_login") != 0,
                rs.getInt("auto_register") != 0,
                notes,
                rs.getLong("updated_at")
        );
    }

    private static String defaultStoredCommand(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return VaultInputValidator.sanitizeCommand(value, fallback);
        } catch (VaultInputException ex) {
            return fallback;
        }
    }

    private ServerPasswordEntry readEntry(ResultSet rs, SecretKey key) throws Exception {
        String hostKey = VaultInputValidator.requireStoredHostKey(rs.getString("host_key"));
        String profileName = VaultInputValidator.readStoredProfileName(rs.getString("profile_name"));
        byte[] passwordNonce = VaultInputValidator.requireNonce(rs.getBytes("password_nonce"));
        byte[] passwordCipher = VaultInputValidator.requireCipherBlob(rs.getBytes("password_cipher"), "password_cipher");
        byte[] usernameNonce = VaultInputValidator.optionalCipherBlob(rs.getBytes("username_nonce"));
        byte[] usernameCipher = VaultInputValidator.optionalCipherBlob(rs.getBytes("username_cipher"));
        byte[] notesNonce = VaultInputValidator.optionalCipherBlob(rs.getBytes("notes_nonce"));
        byte[] notesCipher = VaultInputValidator.optionalCipherBlob(rs.getBytes("notes_cipher"));
        byte[] entryMac = VaultInputValidator.optionalMac(rs.getBytes("entry_mac"));
        long updatedAt = rs.getLong("updated_at");
        String loginCommand = VaultInputValidator.requireStoredCommand(rs.getString("login_command"), "login");
        String registerCommand = VaultInputValidator.requireStoredCommand(rs.getString("register_command"), "register");
        boolean autoLogin = rs.getInt("auto_login") != 0;
        boolean autoRegister = rs.getInt("auto_register") != 0;

        ServerPasswordVault.EncryptedSecret password = new ServerPasswordVault.EncryptedSecret(passwordNonce, passwordCipher);
        ServerPasswordVault.EncryptedSecret username = new ServerPasswordVault.EncryptedSecret(usernameNonce, usernameCipher);
        ServerPasswordVault.EncryptedSecret notes = new ServerPasswordVault.EncryptedSecret(notesNonce, notesCipher);

        if (entryMac.length > 0) {
            if (!vault.verifyEntryMac(key, hostKey, profileName, password, username, notes, loginCommand, registerCommand,
                    autoLogin, autoRegister, updatedAt, entryMac)) {
                DupeClient.LOGGER.warn("Entry integrity check failed for {} ({}), loading entry anyway", hostKey, profileName);
            }
        }

        String usernamePlain = usernameCipher.length > 0
                ? vault.decryptNullable(usernameNonce, usernameCipher, key, hostKey)
                : readStoredUsername(rs.getString("username"));
        String notesPlain = notesCipher.length > 0
                ? vault.decryptNullable(notesNonce, notesCipher, key, hostKey)
                : VaultInputValidator.sanitizeNotes(rs.getString("notes"));
        String passwordPlain = VaultInputValidator.readStoredPassword(
                vault.decryptWithLegacyFallback(passwordNonce, passwordCipher, key, hostKey));

        return new ServerPasswordEntry(
                rs.getLong("id"),
                hostKey,
                profileName,
                readStoredDisplayName(rs.getString("display_name"), hostKey),
                usernamePlain,
                passwordPlain,
                loginCommand,
                registerCommand,
                autoLogin,
                autoRegister,
                notesPlain,
                updatedAt
        );
    }

    private static String readStoredUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return VaultInputValidator.sanitizeRegisterIdentity(username);
        } catch (VaultInputException ex) {
            return username.trim();
        }
    }

    private static String readStoredDisplayName(String displayName, String hostKey) {
        try {
            String cleaned = VaultInputValidator.sanitizeDisplayName(displayName);
            return cleaned == null ? hostKey : cleaned;
        } catch (VaultInputException ex) {
            return hostKey;
        }
    }

    private void bindEntryInsert(PreparedStatement ps, ServerPasswordEntry draft,
                                 ServerPasswordVault.EncryptedSecret password,
                                 ServerPasswordVault.EncryptedSecret username,
                                 ServerPasswordVault.EncryptedSecret notes,
                                 byte[] entryMac,
                                 long now) throws SQLException {
        ps.setString(1, draft.hostKey());
        ps.setString(2, draft.profileName());
        ps.setString(3, blankToNull(draft.displayName()));
        ps.setString(4, null);
        ps.setBytes(5, username.cipherBytes());
        ps.setBytes(6, username.nonce());
        ps.setBytes(7, password.cipherBytes());
        ps.setBytes(8, password.nonce());
        ps.setString(9, draft.loginCommand());
        ps.setString(10, draft.registerCommand());
        ps.setInt(11, draft.autoLogin() ? 1 : 0);
        ps.setInt(12, draft.autoRegister() ? 1 : 0);
        ps.setString(13, null);
        ps.setBytes(14, notes.cipherBytes());
        ps.setBytes(15, notes.nonce());
        ps.setBytes(16, entryMac);
        ps.setLong(17, now);
    }

    private void bindEntry(PreparedStatement ps, ServerPasswordEntry draft,
                           ServerPasswordVault.EncryptedSecret password,
                           ServerPasswordVault.EncryptedSecret username,
                           ServerPasswordVault.EncryptedSecret notes,
                           byte[] entryMac,
                           long now) throws SQLException {
        bindEntryInsert(ps, draft, password, username, notes, entryMac, now);
    }

    private void verifyUnlockedMeta(VaultAccessSession session) {
        requireSession(session);
        byte[] salt = loadSalt();
        byte[] verifier = loadVerifier();
        byte[] installId = loadInstallId();
        byte[] metaMac = loadMetaMac();
        int cryptoVersion = loadCryptoVersion();
        if (installId == null || installId.length == 0) {
            throw new VaultAccessDeniedException("Vault metadata is missing install binding");
        }
        if (!vault.verifyMetaMac(session.masterKey(), salt, verifier, installId, cryptoVersion, metaMac)) {
            throw new VaultAccessDeniedException("Vault metadata integrity check failed");
        }
    }

    private void validateVaultSeal() throws SQLException {
        if (!isVaultInitialized()) {
            return;
        }
        byte[] magic = queryVaultMetaBytes("vault_magic");
        if (magic == null) {
            return;
        }
        if (!vault.validateVaultMagic(magic)) {
            throw new SQLException("Vault database failed seal validation");
        }
    }

    private void configureConnection() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA trusted_schema = OFF");
            st.execute("PRAGMA secure_delete = ON");
            st.execute("PRAGMA temp_store = MEMORY");
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
        }
    }

    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS vault_meta (
                      id INTEGER PRIMARY KEY CHECK (id = 1),
                      salt BLOB NOT NULL,
                      verifier BLOB NOT NULL,
                      created_at INTEGER NOT NULL,
                      crypto_version INTEGER NOT NULL DEFAULT 1,
                      install_id BLOB,
                      vault_magic BLOB,
                      meta_mac BLOB
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS settings (
                      id INTEGER PRIMARY KEY CHECK (id = 1),
                      prompt_on_auth INTEGER NOT NULL DEFAULT 1,
                      auto_login INTEGER NOT NULL DEFAULT 1,
                      auto_register INTEGER NOT NULL DEFAULT 0,
                      auto_generate_password INTEGER NOT NULL DEFAULT 1,
                      login_delay_ticks INTEGER NOT NULL DEFAULT 40
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS server_entries (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      host_key TEXT NOT NULL UNIQUE,
                      display_name TEXT,
                      username TEXT,
                      username_cipher BLOB,
                      username_nonce BLOB,
                      password_cipher BLOB NOT NULL,
                      password_nonce BLOB NOT NULL,
                      login_command TEXT NOT NULL DEFAULT 'login',
                      register_command TEXT NOT NULL DEFAULT 'register',
                      auto_login INTEGER NOT NULL DEFAULT 1,
                      auto_register INTEGER NOT NULL DEFAULT 0,
                      notes TEXT,
                      notes_cipher BLOB,
                      notes_nonce BLOB,
                      entry_mac BLOB,
                      updated_at INTEGER NOT NULL
                    )
                    """);
            ensureColumn(st, "vault_meta", "crypto_version", "INTEGER NOT NULL DEFAULT 1");
            ensureColumn(st, "vault_meta", "install_id", "BLOB");
            ensureColumn(st, "vault_meta", "vault_magic", "BLOB");
            ensureColumn(st, "vault_meta", "meta_mac", "BLOB");
            ensureColumn(st, "server_entries", "username_cipher", "BLOB");
            ensureColumn(st, "server_entries", "username_nonce", "BLOB");
            ensureColumn(st, "server_entries", "notes_cipher", "BLOB");
            ensureColumn(st, "server_entries", "notes_nonce", "BLOB");
            ensureColumn(st, "server_entries", "entry_mac", "BLOB");
            ensureColumn(st, "server_entries", "profile_name", "TEXT NOT NULL DEFAULT ''");
            migrateServerEntriesProfileKey(st);
            st.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        }
        ensureSettingsRow();
    }

    private void migrateServerEntriesProfileKey(Statement st) throws SQLException {
        Integer userVersion = queryUserVersion(st);
        if (userVersion != null && userVersion >= SCHEMA_VERSION && hasProfileScopedUniqueIndex(st)) {
            return;
        }
        st.execute(
                """
                CREATE TABLE IF NOT EXISTS server_entries_v3 (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  host_key TEXT NOT NULL,
                  profile_name TEXT NOT NULL DEFAULT '',
                  display_name TEXT,
                  username TEXT,
                  username_cipher BLOB,
                  username_nonce BLOB,
                  password_cipher BLOB NOT NULL,
                  password_nonce BLOB NOT NULL,
                  login_command TEXT NOT NULL DEFAULT 'login',
                  register_command TEXT NOT NULL DEFAULT 'register',
                  auto_login INTEGER NOT NULL DEFAULT 1,
                  auto_register INTEGER NOT NULL DEFAULT 0,
                  notes TEXT,
                  notes_cipher BLOB,
                  notes_nonce BLOB,
                  entry_mac BLOB,
                  updated_at INTEGER NOT NULL,
                  UNIQUE(host_key, profile_name)
                )
                """);
        st.execute(
                """
                INSERT OR IGNORE INTO server_entries_v3 (
                  id, host_key, profile_name, display_name, username, username_cipher, username_nonce,
                  password_cipher, password_nonce, login_command, register_command, auto_login, auto_register,
                  notes, notes_cipher, notes_nonce, entry_mac, updated_at
                )
                SELECT
                  id, host_key, COALESCE(profile_name, ''), display_name, username, username_cipher, username_nonce,
                  password_cipher, password_nonce, login_command, register_command, auto_login, auto_register,
                  notes, notes_cipher, notes_nonce, entry_mac, updated_at
                FROM server_entries
                """);
        st.execute("DROP TABLE server_entries");
        st.execute("ALTER TABLE server_entries_v3 RENAME TO server_entries");
        DupeClient.LOGGER.info("Migrated server password entries to per-profile storage");
    }

    private static boolean hasColumn(Statement st, String table, String column) throws SQLException {
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Integer queryUserVersion(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return null;
    }

    private static boolean hasProfileScopedUniqueIndex(Statement st) throws SQLException {
        try (ResultSet indexes = st.executeQuery("PRAGMA index_list(server_entries)")) {
            while (indexes.next()) {
                if (indexes.getInt("unique") != 1) {
                    continue;
                }
                String indexName = indexes.getString("name");
                if (indexName == null) {
                    continue;
                }
                if (indexCoversColumns(st, indexName, "host_key", "profile_name")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean indexCoversColumns(Statement st, String indexName, String... columns) throws SQLException {
        List<String> indexed = new ArrayList<>();
        try (ResultSet info = st.executeQuery("PRAGMA index_info(" + indexName + ")")) {
            while (info.next()) {
                indexed.add(info.getString("name"));
            }
        }
        if (indexed.size() != columns.length) {
            return false;
        }
        for (int i = 0; i < columns.length; i++) {
            if (!columns[i].equalsIgnoreCase(indexed.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void ensureColumn(Statement st, String table, String column, String ddl) throws SQLException {
        if (!ALLOWED_COLUMNS.containsKey(table) || !ALLOWED_COLUMNS.get(table).contains(column)) {
            throw new SQLException("Rejected schema mutation");
        }
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
        } catch (SQLException ex) {
            String message = ex.getMessage();
            if (message == null || !message.toLowerCase(Locale.ROOT).contains("duplicate column")) {
                throw ex;
            }
        }
    }

    private void ensureSettingsRow() {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO settings (id) VALUES (?)")) {
            ps.setInt(1, 1);
            ps.executeUpdate();
        } catch (SQLException ex) {
            DupeClient.LOGGER.error("Failed to ensure server password settings row", ex);
        }
    }

    private byte[] queryVaultMetaBytes(String column) {
        String sql = switch (column) {
            case "salt" -> "SELECT salt FROM vault_meta WHERE id = ?";
            case "verifier" -> "SELECT verifier FROM vault_meta WHERE id = ?";
            case "install_id" -> "SELECT install_id FROM vault_meta WHERE id = ?";
            case "meta_mac" -> "SELECT meta_mac FROM vault_meta WHERE id = ?";
            case "vault_magic" -> "SELECT vault_magic FROM vault_meta WHERE id = ?";
            default -> throw new IllegalArgumentException("Unsupported vault_meta column");
        };
        ensureOpen();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes(1);
                }
            }
        } catch (SQLException ex) {
            DupeClient.LOGGER.error("Server password vault_meta query failed", ex);
        }
        return null;
    }

    private Integer queryVaultMetaInt(String column) {
        if (!"crypto_version".equals(column)) {
            throw new IllegalArgumentException("Unsupported vault_meta int column");
        }
        ensureOpen();
        try (PreparedStatement ps = connection.prepareStatement("SELECT crypto_version FROM vault_meta WHERE id = ?")) {
            ps.setInt(1, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            DupeClient.LOGGER.error("Server password vault_meta query failed", ex);
        }
        return null;
    }

    private void requireSession(VaultAccessSession session) {
        assertModRuntime();
        if (activeSession == null
                || activeSessionId < 0
                || session == null
                || !activeSession.matches(session)
                || activeSessionId != session.sessionId()) {
            throw new VaultAccessDeniedException("Active vault session required");
        }
    }

    private static void assertModRuntime() {
        if (FabricLoader.getInstance().getModContainer(DupeClient.MOD_ID).isEmpty()) {
            throw new VaultAccessDeniedException("Vault database is only accessible from DupeClient");
        }
    }

    private void ensureOpen() {
        if (connection == null) {
            open();
        }
        if (connection == null) {
            throw new VaultAccessDeniedException("Vault database is unavailable");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
