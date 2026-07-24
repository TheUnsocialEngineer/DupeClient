package com.dupeclient.client.multiplayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProxyHealthChecker {
    private static final long CACHE_MS = 90_000L;
    private static final Map<String, ProxyHealth> HEALTH = new ConcurrentHashMap<>();

    private ProxyHealthChecker() {
    }

    public static ProxyHealth healthFor(ProxyProfile proxy) {
        return HEALTH.computeIfAbsent(proxy.id(), id -> new ProxyHealth());
    }

    public static void requestCheck(ProxyProfile proxy, boolean force) {
        ProxyHealth health = healthFor(proxy);
        if (health.inFlight()) {
            return;
        }
        if (!force && health.checkedAt() > 0 && System.currentTimeMillis() - health.checkedAt() < CACHE_MS) {
            return;
        }
        health.markChecking();
        Thread.startVirtualThread(() -> runCheck(proxy, health));
    }

    public static void requestCheckAll(Iterable<ProxyProfile> proxies, boolean force) {
        for (ProxyProfile proxy : proxies) {
            requestCheck(proxy, force);
        }
    }

    public static void invalidate(String proxyId) {
        HEALTH.remove(proxyId);
    }

    private static void runCheck(ProxyProfile proxy, ProxyHealth health) {
        Authenticator previous = Authenticator.getDefault();
        try {
            long tcpStart = System.nanoTime();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(proxy.host(), proxy.port()), 4_000);
            }
            long tcpMs = (System.nanoTime() - tcpStart) / 1_000_000L;

            if (!proxy.username().isEmpty()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                            proxy.username(),
                            proxy.password().toCharArray()
                        );
                    }
                });
            }

            Proxy javaProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxy.host(), proxy.port()));
            HttpURLConnection conn = (HttpURLConnection) URI
                .create("http://ip-api.com/json/?fields=status,query,country,countryCode")
                .toURL()
                .openConnection(javaProxy);
            conn.setConnectTimeout(6_000);
            conn.setReadTimeout(6_000);
            conn.setRequestMethod("GET");

            long socksStart = System.nanoTime();
            int code = conn.getResponseCode();
            long socksMs = (System.nanoTime() - socksStart) / 1_000_000L;
            if (code != 200) {
                health.markFailed("HTTP " + code);
                return;
            }

            String body;
            try (InputStream in = conn.getInputStream()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!"success".equalsIgnoreCase(json.has("status") ? json.get("status").getAsString() : "")) {
                health.markFailed("Geo lookup failed");
                return;
            }

            String ip = json.has("query") ? json.get("query").getAsString() : "";
            String country = json.has("country") ? json.get("country").getAsString() : "";
            String codeCc = json.has("countryCode") ? json.get("countryCode").getAsString() : "";
            long ping = Math.max(socksMs, tcpMs);
            health.markOk(ping, codeCc, country, ip);
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = ex.getClass().getSimpleName();
            }
            health.markFailed(msg);
        } finally {
            Authenticator.setDefault(previous);
        }
    }
}
