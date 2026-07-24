package com.dupeclient.client.multiplayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public final class SessionAPI {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private SessionAPI() {
    }

    public static String[] getProfileInfo(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json == null || !json.has("name") || !json.has("id")) {
                return null;
            }
            return new String[] {json.get("name").getAsString(), json.get("id").getAsString()};
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean validateSession(String token) {
        try {
            String[] profileInfo = getProfileInfo(token);
            if (profileInfo == null || profileInfo.length < 2) {
                return false;
            }
            UUID uuid = profileInfo[1].contains("-")
                    ? UUID.fromString(profileInfo[1])
                    : UndashedUuid.fromString(profileInfo[1]);
            var session = MinecraftClient.getInstance().getSession();
            return profileInfo[0].equalsIgnoreCase(session.getUsername())
                    && uuid.equals(session.getUuidOrNull());
        } catch (Exception ignored) {
            return false;
        }
    }
}
