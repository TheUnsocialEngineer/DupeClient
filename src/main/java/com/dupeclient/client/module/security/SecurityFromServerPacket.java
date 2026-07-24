package com.dupeclient.client.module.security;

/**
 * Implemented via mixin on text content types that can carry server-driven key probes.
 * Only marked instances are spoofed when {@link SecuritySettings#keyResolutionServerMarkedOnly} is true.
 */
public interface SecurityFromServerPacket {
    void dupeclient$setFromServerPacket(boolean fromServer);

    boolean dupeclient$isFromServerPacket();
}
