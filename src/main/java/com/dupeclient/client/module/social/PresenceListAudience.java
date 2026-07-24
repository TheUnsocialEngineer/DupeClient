package com.dupeclient.client.module.social;

import org.jetbrains.annotations.Nullable;

/**
 * Who may see your presence row on others' Social screens. Stored server-side and echoed on {@code GET …/list}.
 */
public enum PresenceListAudience {
    /** Visible to all DupeClient users fetching the list. */
    PUBLIC,
    /**
     * Only clients that have your UUID in {@code config/dupeclient/social_friends.json} will show your row
     * (others filter client-side).
     */
    FRIENDS_ONLY;

    public static PresenceListAudience fromWire(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return PUBLIC;
        }
        if ("friends_only".equalsIgnoreCase(raw.trim())) {
            return FRIENDS_ONLY;
        }
        return PUBLIC;
    }

    public String wireValue() {
        return this == FRIENDS_ONLY ? "friends_only" : "public";
    }
}
