package com.dupeclient.client.multiplayer;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProxyStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_PROXIES);
    private static final Type FILE_TYPE = new TypeToken<ProxyFile>() {}.getType();

    private ProxyStore() {
    }

    public static ProxyData load() {
        if (!Files.exists(PATH)) {
            return new ProxyData(List.of(), "");
        }
        try {
            String raw = Files.readString(PATH);
            ProxyFile file = GSON.fromJson(raw, FILE_TYPE);
            if (file == null) {
                return new ProxyData(List.of(), "");
            }
            List<ProxyProfile> proxies = new ArrayList<>();
            if (file.proxies != null) {
                for (StoredProxy stored : file.proxies) {
                    if (stored == null || stored.host == null || stored.host.isBlank()) continue;
                    ProxyType type;
                    try {
                        type = stored.type == null ? ProxyType.SOCKS5 : ProxyType.valueOf(stored.type);
                    } catch (Exception ex) {
                        type = ProxyType.SOCKS5;
                    }
                    proxies.add(new ProxyProfile(
                        stored.id == null || stored.id.isBlank() ? java.util.UUID.randomUUID().toString() : stored.id,
                        stored.name == null ? stored.host : stored.name,
                        stored.host,
                        stored.port <= 0 ? 1080 : stored.port,
                        type,
                        stored.username == null ? "" : stored.username,
                        stored.password == null ? "" : stored.password
                    ));
                }
            }
            return new ProxyData(proxies, file.activeId == null ? "" : file.activeId);
        } catch (Exception ignored) {
            return new ProxyData(List.of(), "");
        }
    }

    public static void save(ProxyData data) {
        try {
            Files.createDirectories(PATH.getParent());
            ProxyFile file = new ProxyFile();
            file.activeId = data.activeId();
            file.proxies = new ArrayList<>();
            for (ProxyProfile proxy : data.proxies()) {
                StoredProxy stored = new StoredProxy();
                stored.id = proxy.id();
                stored.name = proxy.name();
                stored.host = proxy.host();
                stored.port = proxy.port();
                stored.type = proxy.type().name();
                stored.username = proxy.username();
                stored.password = proxy.password();
                file.proxies.add(stored);
            }
            Files.writeString(PATH, GSON.toJson(file));
        } catch (IOException ignored) {
        }
    }

    public record ProxyData(List<ProxyProfile> proxies, String activeId) {
    }

    private static final class ProxyFile {
        String activeId;
        List<StoredProxy> proxies;
    }

    private static final class StoredProxy {
        String id;
        String name;
        String host;
        int port;
        String type;
        String username;
        String password;
    }
}
