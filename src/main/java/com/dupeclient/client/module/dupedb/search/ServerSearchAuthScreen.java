package com.dupeclient.client.module.dupedb.search;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.multiplayer.MultiplayerNavigable;
import com.dupeclient.client.multiplayer.MultiplayerScreens;

import com.dupeclient.client.module.dupedb.search.api.ApiClient;
import com.dupeclient.client.module.dupedb.search.api.ApiException;
import com.dupeclient.client.module.dupedb.search.auth.AddonAuth;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class ServerSearchAuthScreen extends Screen implements MultiplayerNavigable {
   private static final long MIN_POLL_INTERVAL_MS = 2000L;
   private final Screen parent;
   private final AddonAuth auth = new AddonAuth();
   private final ApiClient apiClient = new ApiClient(this.auth);
   private ServerSearchAuthScreen.Phase phase = ServerSearchAuthScreen.Phase.IDLE;
   private ApiClient.DeviceStart pendingDevice;
   private long codeExpiresAtEpochMs;
   private long nextPollAtEpochMs;
   private String headlineText = "";
   private String detailText = "";
   private String hintText = "";
   private String noticeText = "";
   private Button primaryButton;
   private Button secondaryButton;
   private Button backButton;

   public ServerSearchAuthScreen(Screen parent) {
      super(Component.literal("Minecraft Server Search — Sign in"));
      this.parent = parent;
   }

   public Screen getNavigationParent() {
      return this.parent;
   }

   protected void init() {
      super.init();
      if (this.phase == ServerSearchAuthScreen.Phase.IDLE) {
         this.auth.load();
         if (this.apiClient.isAuthenticated()) {
            this.phase = ServerSearchAuthScreen.Phase.AUTHED;
         } else {
            String notice = AddonAuth.consumeSignOutMessage();
            if (notice != null && !notice.isEmpty()) {
               this.noticeText = notice;
            }
         }
      }

      if (this.phase == ServerSearchAuthScreen.Phase.AUTHED) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            mc.execute(this::openScanner);
         }
      } else {
         int cx = this.width / 2;
         int cy = this.height / 2;
         this.primaryButton = Button.builder(Component.literal("Sign in with Discord"), b -> this.onPrimary())
            .bounds(cx - 110, cy + 8, 220, 20)
            .build();
         this.addRenderableWidget(this.primaryButton);
         this.secondaryButton = Button.builder(Component.literal("Copy code"), b -> this.onSecondary())
            .bounds(cx - 110, cy + 32, 220, 20)
            .build();
         this.secondaryButton.visible = false;
         this.addRenderableWidget(this.secondaryButton);
         this.backButton = Button.builder(CommonComponents.GUI_BACK, b -> this.onBack()).bounds(cx - 110, cy + 60, 220, 20).build();
         this.addRenderableWidget(this.backButton);
         this.applyPhase();
      }
   }

   public void tick() {
      super.tick();
      if (this.phase == ServerSearchAuthScreen.Phase.AWAITING) {
         this.pollIfDue();
      }
   }

   private void applyPhase() {
      if (this.primaryButton != null) {
         switch (this.phase) {
            case IDLE:
               boolean hasNotice = this.noticeText != null && !this.noticeText.isEmpty();
               this.headlineText = hasNotice ? "You were signed out" : "Sign in to use the Minecraft Server Scanner";
               this.detailText = hasNotice ? this.noticeText : "This addon requires an active subscription or VIP role.";
               this.hintText = hasNotice ? "Only one Minecraft install can be active per account. Sign in again to use it here." : "";
               this.primaryButton.setMessage(Component.literal("Sign in with Discord"));
               this.primaryButton.active = true;
               this.primaryButton.visible = true;
               this.secondaryButton.visible = false;
               break;
            case STARTING:
               this.headlineText = "Requesting a sign-in code…";
               this.detailText = "";
               this.hintText = "";
               this.primaryButton.active = false;
               this.primaryButton.visible = true;
               this.secondaryButton.visible = false;
               break;
            case AWAITING:
               this.headlineText = "Open the browser link, then approve this code";
               this.detailText = this.pendingDevice != null ? this.pendingDevice.userCode() : "";
               this.hintText = "Already logged in on the website? You should be signed in here within a few seconds.";
               this.primaryButton.setMessage(Component.literal("Open link in browser"));
               this.primaryButton.active = true;
               this.primaryButton.visible = true;
               this.secondaryButton.setMessage(Component.literal("Copy code"));
               this.secondaryButton.active = true;
               this.secondaryButton.visible = true;
               break;
            case FAILED:
               this.primaryButton.setMessage(Component.literal("Try again"));
               this.primaryButton.active = true;
               this.primaryButton.visible = true;
               this.secondaryButton.visible = false;
               break;
            case NO_ACCESS:
               this.primaryButton.setMessage(Component.literal("Try again"));
               this.primaryButton.active = true;
               this.primaryButton.visible = true;
               this.secondaryButton.visible = false;
               break;
            case AUTHED:
               this.primaryButton.visible = false;
               this.secondaryButton.visible = false;
         }
      }
   }

   private void onPrimary() {
      switch (this.phase) {
         case IDLE:
         case FAILED:
         case NO_ACCESS:
            this.startDeviceFlow();
         case STARTING:
         default:
            break;
         case AWAITING:
            this.openVerificationUrl();
      }
   }

   private void onSecondary() {
      if (this.phase == ServerSearchAuthScreen.Phase.AWAITING && this.pendingDevice != null) {
         copyToClipboard(this.pendingDevice.userCode());
         this.hintText = "Code copied — paste it on the website page.";
      }
   }

   private void onBack() {
      MultiplayerScreens.returnToMultiplayer(Minecraft.getInstance(), this.parent);
   }

   private void openVerificationUrl() {
      if (this.pendingDevice != null) {
         try {
            Util.getPlatform().openUri(this.pendingDevice.openableUrl());
         } catch (Exception var2) {
            DupeClient.LOGGER.warn("Could not open verification URL: {}", var2.toString());
         }
      }
   }

   private void startDeviceFlow() {
      this.phase = ServerSearchAuthScreen.Phase.STARTING;
      this.pendingDevice = null;
      this.applyPhase();
      Thread.startVirtualThread(() -> {
         try {
            ApiClient.DeviceStart start = this.apiClient.startDevice();
            this.runOnClient(() -> {
               this.pendingDevice = start;
               this.codeExpiresAtEpochMs = System.currentTimeMillis() + start.expiresInSec() * 1000L;
               this.nextPollAtEpochMs = System.currentTimeMillis() + Math.max(2000L, start.pollIntervalSec() * 1000L);
               this.phase = ServerSearchAuthScreen.Phase.AWAITING;
               this.applyPhase();
               this.openVerificationUrl();
            });
         } catch (ApiException var2) {
            this.runOnClient(() -> {
               this.phase = ServerSearchAuthScreen.Phase.FAILED;
               this.headlineText = "Could not start sign-in";
               this.detailText = var2.getMessage() != null ? var2.getMessage() : "Unknown error";
               this.hintText = "Check your internet connection and try again.";
               this.applyPhase();
            });
         }
      });
   }

   private void pollIfDue() {
      long now = System.currentTimeMillis();
      if (this.pendingDevice != null) {
         if (now >= this.nextPollAtEpochMs) {
            if (now > this.codeExpiresAtEpochMs) {
               this.phase = ServerSearchAuthScreen.Phase.FAILED;
               this.headlineText = "Sign-in code expired";
               this.detailText = "Generate a new one and try again.";
               this.hintText = "";
               this.applyPhase();
            } else {
               this.nextPollAtEpochMs = now + Math.max(2000L, this.pendingDevice.pollIntervalSec() * 1000L);
               String code = this.pendingDevice.deviceCode();
               Thread.startVirtualThread(() -> {
                  try {
                     ApiClient.PollResult result = this.apiClient.pollDevice(code);
                     this.runOnClient(() -> this.handlePollResult(result));
                  } catch (ApiException var3x) {
                     this.runOnClient(() -> this.hintText = "Last network attempt failed (" + safeMessage(var3x) + "). Will retry.");
                  }
               });
            }
         }
      }
   }

   private void handlePollResult(ApiClient.PollResult result) {
      if (!(result instanceof ApiClient.PollResult.Pending)) {
         if (result instanceof ApiClient.PollResult.Approved) {
            this.phase = ServerSearchAuthScreen.Phase.AUTHED;
            this.applyPhase();
            this.openScanner();
         } else if (result instanceof ApiClient.PollResult.NoAccess no) {
            this.phase = ServerSearchAuthScreen.Phase.NO_ACCESS;
            this.headlineText = "Account not allowed";
            this.detailText = no.reason();
            this.hintText = "Get a subscription or VIP role on the website, then try again.";
            this.applyPhase();
         } else {
            if (result instanceof ApiClient.PollResult.Expired || result instanceof ApiClient.PollResult.Unknown) {
               this.phase = ServerSearchAuthScreen.Phase.FAILED;
               this.headlineText = "Sign-in code is no longer valid";
               this.detailText = "Generate a new one and try again.";
               this.hintText = "";
               this.applyPhase();
            }
         }
      }
   }

   private void openScanner() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         mc.gui.setScreen(new ServerScannerScreen(this.parent, this.apiClient));
      }
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
      super.extractRenderState(context, mouseX, mouseY, deltaTicks);
      int cx = this.width / 2;
      int cy = this.height / 2;
      context.centeredText(this.font, this.headlineText, cx, cy - 50, -1);
      if (this.phase == ServerSearchAuthScreen.Phase.AWAITING && this.pendingDevice != null) {
         context.centeredText(this.font, this.pendingDevice.userCode(), cx, cy - 26, -11144);
         long secsLeft = Math.max(0L, (this.codeExpiresAtEpochMs - System.currentTimeMillis()) / 1000L);
         context.centeredText(this.font, "Code valid for " + secsLeft + "s", cx, cy - 12, -6250336);
      } else if (!this.detailText.isEmpty()) {
         int color = this.phase != ServerSearchAuthScreen.Phase.NO_ACCESS && this.phase != ServerSearchAuthScreen.Phase.FAILED ? -6250336 : -32640;
         context.centeredText(this.font, this.detailText, cx, cy - 26, color);
      }

      if (!this.hintText.isEmpty()) {
         context.centeredText(this.font, this.hintText, cx, cy + 84, -8748396);
      }
   }

   private void runOnClient(Runnable r) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         mc.execute(r);
      }
   }

   private static String safeMessage(Exception e) {
      String m = e.getMessage();
      return m != null ? m : e.getClass().getSimpleName();
   }

   private static void copyToClipboard(String text) {
      try {
         Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
         cb.setContents(new StringSelection(text), null);
      } catch (Exception var2) {
      }
   }

   private static enum Phase {
      IDLE,
      STARTING,
      AWAITING,
      FAILED,
      NO_ACCESS,
      AUTHED;
   }
}
