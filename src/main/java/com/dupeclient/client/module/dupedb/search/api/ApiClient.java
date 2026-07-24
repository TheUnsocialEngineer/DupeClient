package com.dupeclient.client.module.dupedb.search.api;

import com.dupeclient.client.DupeClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.dupeclient.client.module.dupedb.search.auth.AddonAuth;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
   public static final String API_BASE = "https://minecraftserversearch.com";
   private static final long ACCESS_TOKEN_REFRESH_MARGIN_SEC = 30L;
   private static final int CONNECT_TIMEOUT_MS = 10000;
   private static final int READ_TIMEOUT_MS = 15000;
   private final AddonAuth auth;

   public ApiClient(AddonAuth auth) {
      this.auth = auth;
   }

   public AddonAuth auth() {
      return this.auth;
   }

   public boolean isAuthenticated() {
      return this.auth.hasRefreshToken();
   }

   public ApiClient.DeviceStart startDevice() throws ApiException {
      JsonObject body = new JsonObject();
      body.addProperty("platform", "fabric-dupeclient");
      String resp = this.postJson("https://minecraftserversearch.com/api/addon/device/start", body.toString(), false);
      JsonObject o = JsonParser.parseString(resp).getAsJsonObject();
      return new ApiClient.DeviceStart(
         asString(o, "device_code"),
         asString(o, "user_code"),
         asString(o, "verification_uri"),
         asStringOrNull(o, "verification_uri_complete"),
         asLong(o, "expires_in", 600L),
         asLong(o, "interval", 5L)
      );
   }

   public ApiClient.PollResult pollDevice(String deviceCode) throws ApiException {
      JsonObject body = new JsonObject();
      body.addProperty("device_code", deviceCode);

      try {
         HttpURLConnection conn = this.openJsonPost("https://minecraftserversearch.com/api/addon/device/poll", false);
         writeBody(conn, body.toString());
         int code = conn.getResponseCode();
         String responseBody = readBody(conn, code);
         JsonObject parsed = parseObjectOrEmpty(responseBody);
         String status = asStringOrNull(parsed, "status");
         if (code == 200 && "ok".equals(status)) {
            String access = asString(parsed, "access_token");
            long accessIn = asLong(parsed, "expires_in", 300L);
            String refresh = asString(parsed, "refresh_token");
            long refreshIn = asLong(parsed, "refresh_expires_in", 2592000L);
            this.auth.setTokens(access, accessIn, refresh, refreshIn);
            return new ApiClient.PollResult.Approved(access, accessIn);
         } else if (code == 202 || "pending".equals(status)) {
            return new ApiClient.PollResult.Pending();
         } else if (code == 410 && "expired".equals(status)) {
            return new ApiClient.PollResult.Expired();
         } else if (code == 410) {
            return new ApiClient.PollResult.Unknown();
         } else if (code == 403 && "no_access".equals(status)) {
            String reason = asStringOrNull(parsed, "reason");
            return new ApiClient.PollResult.NoAccess(reason != null ? reason : "Subscription required");
         } else {
            throw new ApiException(code, "Unexpected poll response", responseBody);
         }
      } catch (IOException var14) {
         throw new ApiException(-1, "Network error: " + var14.getMessage(), null);
      }
   }

   public boolean refreshAccessToken() {
      String refresh = this.auth.getRefreshToken();
      if (refresh != null && !refresh.isEmpty()) {
         try {
            JsonObject body = new JsonObject();
            body.addProperty("refresh_token", refresh);
            HttpURLConnection conn = this.openJsonPost("https://minecraftserversearch.com/api/addon/token/refresh", false);
            writeBody(conn, body.toString());
            int code = conn.getResponseCode();
            String responseBody = readBody(conn, code);
            if (code == 200) {
               JsonObject parsed = parseObjectOrEmpty(responseBody);
               String access = asString(parsed, "access_token");
               long accessIn = asLong(parsed, "expires_in", 300L);
               this.auth.updateAccessToken(access, accessIn);
               return true;
            } else if (code != 401 && code != 403) {
               DupeClient.LOGGER.warn("Addon refresh failed transiently (HTTP {}); keeping tokens", code);
               return false;
            } else {
               DupeClient.LOGGER.info("Addon refresh rejected (HTTP {}): clearing local tokens", code);
               noteSignOutReason(responseBody);
               this.auth.clear();
               return false;
            }
         } catch (IOException var10) {
            DupeClient.LOGGER.warn("Addon refresh network error: {}", var10.toString());
            return false;
         }
      } else {
         return false;
      }
   }

   public String getAuthed(String url) throws ApiException {
      if (!this.auth.hasRefreshToken()) {
         throw new ApiException(401, "Not signed in", null);
      } else if (!this.auth.isAccessValidWithMargin(30L) && !this.refreshAccessToken()) {
         throw new ApiException(401, "Session expired — please link the addon again", null);
      } else {
         ApiClient.Result first = this.doGetAuthed(url);
         if (first.code == 200) {
            return first.body;
         } else if (first.code == 401) {
            noteSignOutReason(first.body);
            if (this.refreshAccessToken()) {
               ApiClient.Result retry = this.doGetAuthed(url);
               if (retry.code == 200) {
                  return retry.body;
               } else {
                  throw new ApiException(retry.code, errorMessage(retry), retry.body);
               }
            } else {
               throw new ApiException(401, "Session expired — please link the addon again", first.body);
            }
         } else if (first.code == 403) {
            noteSignOutReason(first.body);
            throw new ApiException(403, "Access denied — your subscription may have ended", first.body);
         } else {
            throw new ApiException(first.code, errorMessage(first), first.body);
         }
      }
   }

   private static void noteSignOutReason(String body) {
      JsonObject parsed = parseObjectOrEmpty(body);
      if ("superseded".equals(asStringOrNull(parsed, "status"))) {
         String reason = asStringOrNull(parsed, "reason");
         AddonAuth.setSignOutMessage(reason != null && !reason.isEmpty() ? reason : "Signed out because this account was linked on another Minecraft install.");
      }
   }

   public JsonElement getAuthedJson(String url) throws ApiException {
      return JsonParser.parseString(this.getAuthed(url));
   }

   private ApiClient.Result doGetAuthed(String url) throws ApiException {
      try {
         HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
         conn.setRequestMethod("GET");
         conn.setRequestProperty("Accept", "application/json");
         String access = this.auth.getAccessToken();
         if (access != null && !access.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + access);
         }

         conn.setConnectTimeout(10000);
         conn.setReadTimeout(15000);
         int code = conn.getResponseCode();
         String body = readBody(conn, code);
         return new ApiClient.Result(code, body);
      } catch (IOException var6) {
         throw new ApiException(-1, "Network error: " + var6.getMessage(), null);
      }
   }

   private String postJson(String url, String body, boolean authed) throws ApiException {
      try {
         HttpURLConnection conn = this.openJsonPost(url, authed);
         writeBody(conn, body);
         int code = conn.getResponseCode();
         String responseBody = readBody(conn, code);
         if (code >= 200 && code < 300) {
            return responseBody;
         } else {
            throw new ApiException(code, "POST failed (HTTP " + code + ")", responseBody);
         }
      } catch (IOException var7) {
         throw new ApiException(-1, "Network error: " + var7.getMessage(), null);
      }
   }

   private HttpURLConnection openJsonPost(String url, boolean authed) throws IOException {
      HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Accept", "application/json");
      conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(15000);
      conn.setDoOutput(true);
      if (authed) {
         String access = this.auth.getAccessToken();
         if (access != null) {
            conn.setRequestProperty("Authorization", "Bearer " + access);
         }
      }

      return conn;
   }

   private static void writeBody(HttpURLConnection conn, String body) throws IOException {
      try (OutputStream os = conn.getOutputStream()) {
         os.write(body.getBytes(StandardCharsets.UTF_8));
      }
   }

   private static String readBody(HttpURLConnection conn, int code) throws IOException {
      InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
      if (stream == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         String line;
         try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            while ((line = r.readLine()) != null) {
               sb.append(line);
            }
         }

         return sb.toString();
      }
   }

   private static JsonObject parseObjectOrEmpty(String body) {
      if (body != null && !body.isEmpty()) {
         try {
            JsonElement el = JsonParser.parseString(body);
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
         } catch (Exception var2) {
            return new JsonObject();
         }
      } else {
         return new JsonObject();
      }
   }

   private static String asString(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
   }

   private static String asStringOrNull(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
   }

   private static long asLong(JsonObject o, String key, long fallback) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : fallback;
   }

   private static String errorMessage(ApiClient.Result r) {
      return "HTTP " + r.code + (r.body != null && !r.body.isEmpty() ? ": " + truncate(r.body) : "");
   }

   private static String truncate(String s) {
      return s.length() <= 200 ? s : s.substring(0, 197) + "...";
   }

   public record DeviceStart(
      String deviceCode, String userCode, String verificationUri, String verificationUriComplete, long expiresInSec, long pollIntervalSec
   ) {
      public String openableUrl() {
         return this.verificationUriComplete != null && !this.verificationUriComplete.isEmpty() ? this.verificationUriComplete : this.verificationUri;
      }
   }

   public sealed interface PollResult
      permits ApiClient.PollResult.Pending,
      ApiClient.PollResult.Approved,
      ApiClient.PollResult.NoAccess,
      ApiClient.PollResult.Expired,
      ApiClient.PollResult.Unknown {
      public record Approved(String accessToken, long accessExpiresInSec) implements ApiClient.PollResult {
      }

      public record Expired() implements ApiClient.PollResult {
      }

      public record NoAccess(String reason) implements ApiClient.PollResult {
      }

      public record Pending() implements ApiClient.PollResult {
      }

      public record Unknown() implements ApiClient.PollResult {
      }
   }

   private record Result(int code, String body) {
   }
}
