package com.dupeclient.client.module.serverpassword;

public record ServerPasswordEntry(
        long id,
        String hostKey,
        String profileName,
        String displayName,
        String username,
        String password,
        String loginCommand,
        String registerCommand,
        boolean autoLogin,
        boolean autoRegister,
        String notes,
        long updatedAtEpochMs
) {
    public String label() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return hostKey;
    }
}
