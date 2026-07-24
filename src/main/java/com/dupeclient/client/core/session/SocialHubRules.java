package com.dupeclient.client.core.session;

public final class SocialHubRules {
    private SocialHubRules() {
    }

    public static boolean socialUiAllowed() {
        return HubModuleRules.socialFeaturesAllowed();
    }

    public static boolean presenceBroadcastAllowed() {
        return HubModuleRules.socialFeaturesAllowed();
    }

    public static boolean socialListFetchAllowed() {
        return HubModuleRules.socialFeaturesAllowed();
    }

    public static boolean friendsFeatureAllowed() {
        return HubModuleRules.socialFeaturesAllowed();
    }

    public static boolean sessionOk() {
        return SessionBootstrap.INSTANCE.isHealthy();
    }

    public static String blockReason() {
        return HubModuleRules.blockReason();
    }
}
