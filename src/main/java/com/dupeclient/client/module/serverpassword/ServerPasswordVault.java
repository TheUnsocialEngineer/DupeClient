package com.dupeclient.client.module.serverpassword;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Vault cryptography (v2): PBKDF2-HMAC-SHA256 key derivation with a mod-bound pepper,
 * AES-256-GCM payload encryption, SHA-256 master verifier, and per-row HMAC-SHA256 integrity tags.
 */
public final class ServerPasswordVault {
    public static final int CRYPTO_VERSION = 2;

    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final String MAC = "HmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int INSTALL_ID_BYTES = 16;
    private static final int NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int ITERATIONS_V1 = 210_000;
    private static final int ITERATIONS_V2 = 600_000;
    private static final int KEY_BITS = 256;
    private static final int MAGIC_BYTES = 8;
    private static final byte[] VERIFIER_INFO = "dupeclient-server-vault-v2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ENTRY_MAC_INFO = "dupeclient-entry-mac-v2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] META_MAC_INFO = "dupeclient-meta-mac-v2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MOD_PEPPER = sha256("dupeclient|server-password-vault|v2|fabric".getBytes(StandardCharsets.UTF_8));
    private static final byte[] EXPECTED_VAULT_MAGIC = Arrays.copyOf(
            sha256("dupeclient-vault-magic-v2".getBytes(StandardCharsets.UTF_8)),
            MAGIC_BYTES
    );

    private final SecureRandom random = new SecureRandom();

    public byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    public byte[] newInstallId() {
        byte[] installId = new byte[INSTALL_ID_BYTES];
        random.nextBytes(installId);
        return installId;
    }

    public byte[] vaultMagic() {
        return EXPECTED_VAULT_MAGIC.clone();
    }

    public boolean validateVaultMagic(byte[] stored) {
        return constantTimeEquals(stored, EXPECTED_VAULT_MAGIC);
    }

    public SecretKey deriveKeyV1(char[] masterPassword, byte[] salt) throws GeneralSecurityException {
        return deriveKey(masterPassword, salt, ITERATIONS_V1, false);
    }

    public SecretKey deriveKeyV2(char[] masterPassword, byte[] salt) throws GeneralSecurityException {
        return deriveKey(masterPassword, salt, ITERATIONS_V2, true);
    }

    public SecretKey deriveKey(char[] masterPassword, byte[] salt, int cryptoVersion) throws GeneralSecurityException {
        if (cryptoVersion >= CRYPTO_VERSION) {
            return deriveKeyV2(masterPassword, salt);
        }
        return deriveKeyV1(masterPassword, salt);
    }

    private SecretKey deriveKey(char[] masterPassword, byte[] salt, int iterations, boolean usePepper)
            throws GeneralSecurityException {
        byte[] kdfSalt = salt;
        if (usePepper) {
            kdfSalt = concat(salt, MOD_PEPPER);
        }
        KeySpec spec = new PBEKeySpec(masterPassword, kdfSalt, iterations, KEY_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF);
        byte[] encoded = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(encoded, "AES");
    }

    public byte[] computeVerifierV1(SecretKey key) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC);
        mac.init(key);
        return mac.doFinal("dupeclient-server-vault-v1".getBytes(StandardCharsets.UTF_8));
    }

    /** SHA-256 verifier bound to install id and mod pepper (v2). */
    public byte[] computeVerifierV2(SecretKey key, byte[] installId) throws GeneralSecurityException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(key.getEncoded());
        sha.update(VERIFIER_INFO);
        sha.update(installId);
        sha.update(MOD_PEPPER);
        return sha.digest();
    }

    public boolean verifyMasterPassword(char[] masterPassword, byte[] salt, byte[] expectedVerifier, int cryptoVersion,
                                        byte[] installId) {
        if (salt == null || expectedVerifier == null || masterPassword == null || masterPassword.length == 0) {
            return false;
        }
        try {
            SecretKey key = deriveKey(masterPassword, salt, cryptoVersion);
            byte[] actual = cryptoVersion >= CRYPTO_VERSION
                    ? computeVerifierV2(key, installId)
                    : computeVerifierV1(key);
            return constantTimeEquals(actual, expectedVerifier);
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    public byte[] computeMetaMac(SecretKey key, byte[] salt, byte[] verifier, byte[] installId, int cryptoVersion)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC);
        mac.init(new SecretKeySpec(deriveSubkey(key, META_MAC_INFO), "HmacSHA256"));
        mac.update(salt);
        mac.update(verifier);
        mac.update(installId);
        mac.update(intToBytes(cryptoVersion));
        mac.update(EXPECTED_VAULT_MAGIC);
        return mac.doFinal();
    }

    public boolean verifyMetaMac(SecretKey key, byte[] salt, byte[] verifier, byte[] installId, int cryptoVersion,
                                 byte[] expectedMac) {
        if (expectedMac == null) {
            return cryptoVersion < CRYPTO_VERSION;
        }
        try {
            byte[] actual = computeMetaMac(key, salt, verifier, installId, cryptoVersion);
            return constantTimeEquals(actual, expectedMac);
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    public EncryptedSecret encrypt(String plaintext, SecretKey key, String hostKey) throws GeneralSecurityException {
        return encryptBytes(plaintext == null ? new byte[0] : plaintext.getBytes(StandardCharsets.UTF_8), key, hostKey);
    }

    public String decrypt(byte[] nonce, byte[] cipherBytes, SecretKey key, String hostKey) throws GeneralSecurityException {
        if (cipherBytes == null || cipherBytes.length == 0) {
            return "";
        }
        byte[] plain = decryptBytes(nonce, cipherBytes, key, hostKey);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** Decrypt payloads written before host-key AAD binding was added. */
    public String decryptLegacy(byte[] nonce, byte[] cipherBytes, SecretKey key) throws GeneralSecurityException {
        if (cipherBytes == null || cipherBytes.length == 0) {
            return "";
        }
        byte[] plain = decryptBytes(nonce, cipherBytes, key, null);
        return new String(plain, StandardCharsets.UTF_8);
    }

    public String decryptWithLegacyFallback(byte[] nonce, byte[] cipherBytes, SecretKey key, String hostKey)
            throws GeneralSecurityException {
        try {
            return decrypt(nonce, cipherBytes, key, hostKey);
        } catch (GeneralSecurityException ex) {
            return decryptLegacy(nonce, cipherBytes, key);
        }
    }

    public EncryptedSecret encryptNullable(String plaintext, SecretKey key, String hostKey) throws GeneralSecurityException {
        if (plaintext == null || plaintext.isBlank()) {
            return EncryptedSecret.empty();
        }
        return encrypt(plaintext, key, hostKey);
    }

    public String decryptNullable(byte[] nonce, byte[] cipherBytes, SecretKey key, String hostKey)
            throws GeneralSecurityException {
        if (nonce == null || cipherBytes == null || cipherBytes.length == 0) {
            return null;
        }
        String value = decryptWithLegacyFallback(nonce, cipherBytes, key, hostKey);
        return value.isBlank() ? null : value;
    }

    public byte[] computeEntryMac(SecretKey key, String hostKey, String profileName, EncryptedSecret password,
                                  EncryptedSecret username, EncryptedSecret notes, String loginCommand,
                                  String registerCommand, boolean autoLogin, boolean autoRegister, long updatedAt)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC);
        mac.init(new SecretKeySpec(deriveSubkey(key, ENTRY_MAC_INFO), "HmacSHA256"));
        mac.update(hostKey.getBytes(StandardCharsets.UTF_8));
        mac.update(profileName == null ? new byte[0] : profileName.getBytes(StandardCharsets.UTF_8));
        mac.update(password.nonce());
        mac.update(password.cipherBytes());
        mac.update(username.nonce());
        mac.update(username.cipherBytes());
        mac.update(notes.nonce());
        mac.update(notes.cipherBytes());
        mac.update(loginCommand.getBytes(StandardCharsets.UTF_8));
        mac.update(registerCommand.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) (autoLogin ? 1 : 0));
        mac.update((byte) (autoRegister ? 1 : 0));
        mac.update(longToBytes(updatedAt));
        return mac.doFinal();
    }

    public byte[] computeEntryMacLegacy(SecretKey key, String hostKey, EncryptedSecret password, EncryptedSecret username,
                                        EncryptedSecret notes, String loginCommand, String registerCommand,
                                        boolean autoLogin, boolean autoRegister, long updatedAt)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC);
        mac.init(new SecretKeySpec(deriveSubkey(key, ENTRY_MAC_INFO), "HmacSHA256"));
        mac.update(hostKey.getBytes(StandardCharsets.UTF_8));
        mac.update(password.nonce());
        mac.update(password.cipherBytes());
        mac.update(username.nonce());
        mac.update(username.cipherBytes());
        mac.update(notes.nonce());
        mac.update(notes.cipherBytes());
        mac.update(loginCommand.getBytes(StandardCharsets.UTF_8));
        mac.update(registerCommand.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) (autoLogin ? 1 : 0));
        mac.update((byte) (autoRegister ? 1 : 0));
        mac.update(longToBytes(updatedAt));
        return mac.doFinal();
    }

    public boolean verifyEntryMac(SecretKey key, String hostKey, String profileName, EncryptedSecret password,
                                  EncryptedSecret username, EncryptedSecret notes, String loginCommand,
                                  String registerCommand, boolean autoLogin, boolean autoRegister, long updatedAt,
                                  byte[] expectedMac) {
        if (expectedMac == null || expectedMac.length == 0) {
            return false;
        }
        try {
            byte[] actual = computeEntryMac(key, hostKey, profileName, password, username, notes, loginCommand,
                    registerCommand, autoLogin, autoRegister, updatedAt);
            if (constantTimeEquals(actual, expectedMac)) {
                return true;
            }
            byte[] legacy = computeEntryMacLegacy(key, hostKey, password, username, notes, loginCommand, registerCommand,
                    autoLogin, autoRegister, updatedAt);
            return constantTimeEquals(legacy, expectedMac);
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    private EncryptedSecret encryptBytes(byte[] plaintext, SecretKey key, String hostKey) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        if (hostKey != null) {
            cipher.updateAAD(hostKey.getBytes(StandardCharsets.UTF_8));
        }
        byte[] cipherBytes = cipher.doFinal(plaintext);
        return new EncryptedSecret(nonce, cipherBytes);
    }

    private byte[] decryptBytes(byte[] nonce, byte[] cipherBytes, SecretKey key, String hostKey)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        if (hostKey != null) {
            cipher.updateAAD(hostKey.getBytes(StandardCharsets.UTF_8));
        }
        return cipher.doFinal(cipherBytes);
    }

    private static byte[] deriveSubkey(SecretKey key, byte[] info) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC);
        mac.init(key);
        return mac.doFinal(info);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    private static byte[] longToBytes(long value) {
        return new byte[]{
                (byte) (value >>> 56),
                (byte) (value >>> 48),
                (byte) (value >>> 40),
                (byte) (value >>> 32),
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    public static void wipe(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    public record EncryptedSecret(byte[] nonce, byte[] cipherBytes) {
        public static EncryptedSecret empty() {
            return new EncryptedSecret(new byte[0], new byte[0]);
        }

        public boolean isEmpty() {
            return cipherBytes.length == 0;
        }
    }
}
