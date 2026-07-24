package com.dupeclient.client.module.serverpassword;

import java.util.Locale;

public final class ServerPasswordKeys {
    private ServerPasswordKeys() {
    }

    public static String normalize(String address) {
        if (address == null) {
            return "";
        }
        String s = address.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("minecraft://")) {
            s = s.substring("minecraft://".length());
        }
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        return s;
    }
}
