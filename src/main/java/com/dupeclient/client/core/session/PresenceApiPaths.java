package com.dupeclient.client.core.session;

import com.dupeclient.client.module.cape.DupeClientCapePresence;

final class PresenceApiPaths {
    private PresenceApiPaths() {
    }

    static String staffBaseUrl() {
        return DupeClientCapePresence.resolvedPresenceApiBase() + "/staff";
    }
}
