package com.dupeclient.client.core.session;

import java.nio.charset.StandardCharsets;

final class SessionSecrets {
    private static final String DEFAULT_STAFF_HMAC_SECRET = "REDACTED";

    private SessionSecrets() {
    }

    static byte[] presenceStaffHmacKey() {
        String env = System.getenv("DUPECLIENT_PRESENCE_STAFF_HMAC");
        if (env != null && !env.isBlank()) {
            return env.trim().getBytes(StandardCharsets.UTF_8);
        }
        String prop = System.getProperty("dupeclient.presence.staff.hmac");
        if (prop != null && !prop.isBlank()) {
            return prop.trim().getBytes(StandardCharsets.UTF_8);
        }
        return DEFAULT_STAFF_HMAC_SECRET.getBytes(StandardCharsets.UTF_8);
    }
}
