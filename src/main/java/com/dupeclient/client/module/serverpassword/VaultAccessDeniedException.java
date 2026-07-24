package com.dupeclient.client.module.serverpassword;

/** Thrown when vault data is accessed without an active in-mod session. */
public final class VaultAccessDeniedException extends RuntimeException {
    public VaultAccessDeniedException(String message) {
        super(message);
    }
}
