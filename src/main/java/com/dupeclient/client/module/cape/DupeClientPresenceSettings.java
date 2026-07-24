package com.dupeclient.client.module.cape;

/**
 * Persisted in {@code config/dupeclient/presence.json}. Default host is {@value #DEFAULT_API_BASE}; override
 * {@link #apiBase} if you mirror the API elsewhere.
 */
public final class DupeClientPresenceSettings {
    public static final String DEFAULT_API_BASE = "https://dupeclient-presence.vercel.app/api/client/presence";

    /** When false, no HTTP presence calls are made (capes only for yourself locally). Use {@link Boolean} so Gson omits = still enabled. */
    public Boolean enabled = Boolean.TRUE;

    /**
     * When false, your client does not send heartbeats (others stop seeing your cape after TTL), but you still
     * query and can see other DupeClient users.
     */
    public Boolean broadcastPresence = Boolean.TRUE;

    /**
     * When true, heartbeats include {@code server} (current multiplayer address or "Singleplayer") so the social list can show it.
     */
    public Boolean shareCurrentServer = Boolean.FALSE;

    /**
     * When true, heartbeats include player block position as {@code coords} so the social list can show it.
     */
    public Boolean shareCurrentCoords = Boolean.FALSE;

    /**
     * When true, the Social screen shows the {@code server} field for each online user when the API returned it.
     */
    public Boolean showServersInSocial = Boolean.TRUE;

    /**
     * When true, the Social screen shows shared {@code coords} when present.
     */
    public Boolean showCoordsInSocial = Boolean.TRUE;

    /**
     * When true, your own UUID is omitted from the Social list (you still send heartbeats; others see you).
     */
    public Boolean hideSelfInSocial = Boolean.TRUE;

    /**
     * {@code everyone} (default): your Social row is shown to all DupeClient users who fetch the list.
     * {@code friends_only}: only clients that added your UUID to {@code config/dupeclient/social_friends.json} show your row
     * (others filter client-side; requires matching API configuration).
     */
    public String presenceListAudience = "everyone";

    /**
     * When true, Social only lists players whose UUID is in your local friends file (plus your own row rules via
     * {@link #hideSelfInSocial}).
     */
    public Boolean socialListFriendsOnlyView = Boolean.FALSE;

    /**
     * Origin for presence endpoints, without a trailing slash. Heartbeat and query URLs are
     * {@code apiBase + "/heartbeat"} and {@code apiBase + "/query"}.
     */
    public String apiBase = DEFAULT_API_BASE;

    /** Hotkey to open the Social screen, GLFW key code, -1 means none. */
    public int openSocialKey = -1;

    /** Hotkey to open the Waypoints screen. */
    public int openWaypointsKey = -1;

    public Boolean shareWaypoints = Boolean.TRUE;
    public Boolean showSharedWaypointsInWorld = Boolean.TRUE;
    public Boolean waypointsFriendsOnlyView = Boolean.FALSE;
    public int configVersion = 1;
    public static final int CONFIG_VERSION = 2;
}
