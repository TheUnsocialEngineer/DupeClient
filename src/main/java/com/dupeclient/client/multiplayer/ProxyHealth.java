package com.dupeclient.client.multiplayer;

public final class ProxyHealth {
    public enum State {
        UNKNOWN,
        CHECKING,
        OK,
        FAILED
    }

    private volatile State state = State.UNKNOWN;
    private volatile long pingMs = -1L;
    private volatile String region = "";
    private volatile String egressIp = "";
    private volatile String error = "";
    private volatile long checkedAt;
    private volatile boolean inFlight;

    public State state() {
        return state;
    }

    public long pingMs() {
        return pingMs;
    }

    public String region() {
        return region;
    }

    public String egressIp() {
        return egressIp;
    }

    public String error() {
        return error;
    }

    public long checkedAt() {
        return checkedAt;
    }

    public boolean inFlight() {
        return inFlight;
    }

    void markChecking() {
        inFlight = true;
        state = State.CHECKING;
        error = "";
    }

    void markOk(long ping, String countryCode, String country, String ip) {
        inFlight = false;
        state = State.OK;
        pingMs = ping;
        egressIp = ip == null ? "" : ip;
        if (countryCode != null && !countryCode.isBlank()) {
            region = countryCode.toUpperCase();
            if (country != null && !country.isBlank()) {
                region = region + " · " + country;
            }
        } else if (country != null && !country.isBlank()) {
            region = country;
        } else {
            region = "—";
        }
        checkedAt = System.currentTimeMillis();
    }

    void markFailed(String message) {
        inFlight = false;
        state = State.FAILED;
        pingMs = -1L;
        region = "";
        egressIp = "";
        error = message == null ? "Failed" : message;
        checkedAt = System.currentTimeMillis();
    }

    public int pingColor() {
        if (state != State.OK || pingMs < 0) {
            return 0xFF8FA3B8;
        }
        if (pingMs < 100) {
            return 0xFF4ADE80;
        }
        if (pingMs < 250) {
            return 0xFFFACC15;
        }
        if (pingMs < 500) {
            return 0xFFFB923C;
        }
        return 0xFFF87171;
    }

    public int statusColor() {
        return switch (state) {
            case OK -> 0xFF4ADE80;
            case CHECKING -> 0xFFFACC15;
            case FAILED -> 0xFFF87171;
            default -> 0xFF64748B;
        };
    }

    public String statusLabel() {
        return switch (state) {
            case OK -> "OK";
            case CHECKING -> "...";
            case FAILED -> "Down";
            default -> "—";
        };
    }

    public String pingLabel() {
        if (state == State.CHECKING) {
            return "...";
        }
        if (state != State.OK || pingMs < 0) {
            return "—";
        }
        return pingMs + "ms";
    }
}
