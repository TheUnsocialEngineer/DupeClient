package com.dupeclient.client.module.utility.crashes;

public final class CrashesSettings {
    /** @deprecated migrated to {@link #chestChatFeedback} / {@link #armorChatFeedback} on load */
    @Deprecated
    public boolean moduleChatFeedback = true;

    public boolean chestCrashEnabled = false;
    public boolean chestChatFeedback = true;
    public int chestToggleKey = -1;
    public int chestRange = 5;
    /** Total open packets to send; 0 = indefinitely. */
    public int chestPackets = 0;
    public boolean chestOnlyWithWrittenBook = false;
    /** When true, chest crash turns off after leaving a world or disconnecting from a server. */
    public boolean chestDisableOnDisconnect = true;

    public boolean armorPlaceEnabled = false;
    public boolean armorChatFeedback = true;
    public int armorToggleKey = -1;
    public int armorDelay = 0;
    public int armorPacketsPerTick = 50;
    public int armorLength = 5;
    public int armorVerticality = 3;
    public boolean armorDisableOnEmpty = true;
    /** When true, armor stand placer turns off after leaving a world or disconnecting from a server. */
    public boolean armorDisableOnLeave = true;
}
