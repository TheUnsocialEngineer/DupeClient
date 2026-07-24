package com.dupeclient.client.multiplayer;

public enum ProxyType {
    SOCKS5("SOCKS5"),
    SOCKS4("SOCKS4");

    public final String label;

    ProxyType(String label) {
        this.label = label;
    }

    public ProxyType next() {
        ProxyType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
