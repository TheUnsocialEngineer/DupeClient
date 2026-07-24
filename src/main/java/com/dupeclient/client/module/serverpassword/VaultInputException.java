package com.dupeclient.client.module.serverpassword;

/** Raised when user or database input fails vault validation. */
public final class VaultInputException extends RuntimeException {
    public VaultInputException(String message) {
        super(message);
    }
}
