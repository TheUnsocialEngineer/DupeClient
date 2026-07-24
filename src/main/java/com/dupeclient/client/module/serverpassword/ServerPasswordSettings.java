package com.dupeclient.client.module.serverpassword;

public record ServerPasswordSettings(
        boolean promptOnAuth,
        boolean autoLogin,
        boolean autoRegister,
        boolean autoGeneratePassword,
        int loginDelayTicks
) {
    public static ServerPasswordSettings defaults() {
        return new ServerPasswordSettings(true, true, false, true, 40);
    }
}
