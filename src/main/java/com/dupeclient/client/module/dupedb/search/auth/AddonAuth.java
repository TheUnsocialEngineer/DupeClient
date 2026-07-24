package com.dupeclient.client.module.dupedb.search.auth;

import com.dupeclient.client.DupeClient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.dupeclient.client.config.DupeClientConfigDir;

public final class AddonAuth {
   private static final Path AUTH_DIR = DupeClientConfigDir.root().resolve("minecraft-server-scanner");
   private static final Path AUTH_FILE = AUTH_DIR.resolve("auth.json");
   private static volatile String pendingSignOutMessage;
   private String accessToken;
   private long accessExpiresAtEpochMs;
   private String refreshToken;
   private long refreshExpiresAtEpochMs;

   public static void setSignOutMessage(String message) {
      pendingSignOutMessage = message;
   }

   public static String consumeSignOutMessage() {
      String m = pendingSignOutMessage;
      pendingSignOutMessage = null;
      return m;
   }

   public synchronized void load() {
      try {
         if (!Files.exists(AUTH_FILE)) {
            return;
         }

         String raw = Files.readString(AUTH_FILE, StandardCharsets.UTF_8);
         JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
         this.accessToken = stringOrNull(o, "access_token");
         this.accessExpiresAtEpochMs = longOrZero(o, "access_expires_at_ms");
         this.refreshToken = stringOrNull(o, "refresh_token");
         this.refreshExpiresAtEpochMs = longOrZero(o, "refresh_expires_at_ms");
      } catch (Exception var3) {
         DupeClient.LOGGER.warn("Failed to load addon auth file; starting logged out: {}", var3.toString());
         this.accessToken = null;
         this.accessExpiresAtEpochMs = 0L;
         this.refreshToken = null;
         this.refreshExpiresAtEpochMs = 0L;
      }
   }

   public synchronized void setTokens(String access, long accessExpiresInSec, String refresh, long refreshExpiresInSec) {
      long now = System.currentTimeMillis();
      this.accessToken = access;
      this.accessExpiresAtEpochMs = now + accessExpiresInSec * 1000L;
      this.refreshToken = refresh;
      this.refreshExpiresAtEpochMs = now + refreshExpiresInSec * 1000L;
      this.save();
   }

   public synchronized void updateAccessToken(String access, long accessExpiresInSec) {
      this.accessToken = access;
      this.accessExpiresAtEpochMs = System.currentTimeMillis() + accessExpiresInSec * 1000L;
      this.save();
   }

   public synchronized void clear() {
      this.accessToken = null;
      this.accessExpiresAtEpochMs = 0L;
      this.refreshToken = null;
      this.refreshExpiresAtEpochMs = 0L;

      try {
         Files.deleteIfExists(AUTH_FILE);
      } catch (IOException var2) {
      }
   }

   public synchronized String getAccessToken() {
      return this.accessToken;
   }

   public synchronized String getRefreshToken() {
      return this.refreshToken;
   }

   public synchronized boolean hasRefreshToken() {
      return this.refreshToken != null && !this.refreshToken.isEmpty() && this.refreshExpiresAtEpochMs > System.currentTimeMillis();
   }

   public synchronized boolean isAccessValidWithMargin(long marginSec) {
      return this.accessToken != null && !this.accessToken.isEmpty() && this.accessExpiresAtEpochMs - System.currentTimeMillis() > marginSec * 1000L;
   }

   private synchronized void save() {
      try {
         Files.createDirectories(AUTH_DIR);
         JsonObject o = new JsonObject();
         if (this.accessToken != null) {
            o.addProperty("access_token", this.accessToken);
         }

         o.addProperty("access_expires_at_ms", this.accessExpiresAtEpochMs);
         if (this.refreshToken != null) {
            o.addProperty("refresh_token", this.refreshToken);
         }

         o.addProperty("refresh_expires_at_ms", this.refreshExpiresAtEpochMs);
         Files.writeString(AUTH_FILE, o.toString(), StandardCharsets.UTF_8);
      } catch (IOException var2) {
         DupeClient.LOGGER.warn("Failed to save addon auth file: {}", var2.toString());
      }
   }

   private static String stringOrNull(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
   }

   private static long longOrZero(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : 0L;
   }
}
