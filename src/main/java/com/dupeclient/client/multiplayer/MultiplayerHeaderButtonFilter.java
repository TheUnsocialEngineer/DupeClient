package com.dupeclient.client.multiplayer;

import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MultiplayerHeaderButtonFilter {
    private static final Pattern FORMATTING = Pattern.compile("§.");

    private MultiplayerHeaderButtonFilter() {
    }

    public static boolean isForeignHeaderButton(
            ClickableWidget widget,
            ClickableWidget ownedVault,
            ClickableWidget ownedSearch,
            ClickableWidget ownedProxies,
            ClickableWidget ownedAccounts
    ) {
        if (widget == null) {
            return false;
        }
        if (widget == ownedVault || widget == ownedSearch || widget == ownedProxies || widget == ownedAccounts) {
            return false;
        }
        int y = widget.getY();
        if (y < 0 || y > 28) {
            return false;
        }
        String plain = plainLabel(widget.getMessage().getString());
        if (plain.isEmpty()) {
            return false;
        }
        if (isDupeClientOwnedLabel(plain)) {
            return true;
        }
        if (isProxyOrAccountLabel(plain)) {
            return true;
        }
        String className = widget.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("meteor") && (plain.contains("proxy") || plain.contains("account"))) {
            return true;
        }
        return false;
    }

    private static boolean isDupeClientOwnedLabel(String plain) {
        return plain.equalsIgnoreCase("server search")
            || plain.equalsIgnoreCase("vault")
            || plain.equalsIgnoreCase("proxies")
            || plain.equalsIgnoreCase("accounts");
    }

    private static boolean isProxyOrAccountLabel(String plain) {
        if (plain.equals("proxy") || plain.equals("proxies") || plain.equals("account") || plain.equals("accounts")) {
            return true;
        }
        if (plain.equals("alt") || plain.equals("alts") || plain.equals("offline") || plain.equals("offline accounts")) {
            return true;
        }
        return plain.contains("proxy") && plain.length() <= 24
            || plain.contains("account") && plain.length() <= 24;
    }

    private static String plainLabel(String raw) {
        if (raw == null) {
            return "";
        }
        return FORMATTING.matcher(raw.trim()).replaceAll("").trim().toLowerCase(Locale.ROOT);
    }
}
