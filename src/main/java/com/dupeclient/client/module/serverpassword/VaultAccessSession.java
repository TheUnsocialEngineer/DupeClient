package com.dupeclient.client.module.serverpassword;

import javax.crypto.SecretKey;

/**
 * Short-lived unlock session. Only {@link ServerPasswordManager} may create or close sessions.
 * Database crypto operations require a live session so external callers cannot read secrets directly.
 */
public final class VaultAccessSession {
    private final SecretKey masterKey;
    private final long sessionId;
    private volatile boolean closed;

    VaultAccessSession(SecretKey masterKey, long sessionId) {
        this.masterKey = masterKey;
        this.sessionId = sessionId;
    }

    boolean matches(VaultAccessSession other) {
        return other != null && other.sessionId == this.sessionId && other.masterKey == this.masterKey && !closed && !other.closed;
    }

    SecretKey masterKey() {
        ensureOpen();
        return masterKey;
    }

    long sessionId() {
        ensureOpen();
        return sessionId;
    }

    void close() {
        closed = true;
    }

    boolean isOpen() {
        return !closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new VaultAccessDeniedException("Vault session is closed");
        }
    }
}
