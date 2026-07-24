package com.dupeclient.client.multiplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProxyManager {
    public static final ProxyManager INSTANCE = new ProxyManager();

    private List<ProxyProfile> proxies = List.of();
    private String activeId = "";

    private ProxyManager() {
        reload();
    }

    public void reload() {
        ProxyStore.ProxyData data = ProxyStore.load();
        proxies = new ArrayList<>(data.proxies());
        activeId = data.activeId();
    }

    public List<ProxyProfile> getProxies() {
        return proxies;
    }

    public Optional<ProxyProfile> getActive() {
        if (activeId == null || activeId.isBlank()) {
            return Optional.empty();
        }
        for (ProxyProfile proxy : proxies) {
            if (activeId.equals(proxy.id())) {
                return Optional.of(proxy);
            }
        }
        return Optional.empty();
    }

    public String getActiveId() {
        return activeId;
    }

    public void setActiveId(String id) {
        activeId = id == null ? "" : id;
        persist();
    }

    public void clearActive() {
        activeId = "";
        persist();
    }

    public void add(ProxyProfile proxy) {
        proxies = new ArrayList<>(proxies);
        proxies.add(proxy);
        persist();
    }

    public void remove(String id) {
        proxies = new ArrayList<>(proxies);
        proxies.removeIf(proxy -> proxy.id().equals(id));
        if (activeId.equals(id)) {
            activeId = "";
        }
        ProxyHealthChecker.invalidate(id);
        persist();
    }

    public boolean shouldUseProxy() {
        return getActive().isPresent();
    }

    private void persist() {
        ProxyStore.save(new ProxyStore.ProxyData(proxies, activeId));
    }
}
