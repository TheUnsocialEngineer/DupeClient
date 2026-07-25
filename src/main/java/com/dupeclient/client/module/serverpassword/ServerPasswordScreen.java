package com.dupeclient.client.module.serverpassword;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import com.dupeclient.client.multiplayer.MultiplayerNavigable;
import com.dupeclient.client.multiplayer.MultiplayerScreens;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ServerPasswordScreen extends Screen implements MultiplayerNavigable {
   private static final int PANEL_TOP = 24;
   private static final int PANEL_BOTTOM_PAD = 24;
   private static final int PANEL_HEADER_H = 30;
   private static final int SETTINGS_SECTION_GAP = 6;
   private static final int SECTION_LABEL_H = 10;
   private static final int GAP = 8;
   private static final int SETTINGS_TABLE_GAP = 5;
   private static final int TABLE_SECTION_LABEL_H = 12;
   private static final int TABLE_HEADER_H = 16;
   private static final int TABLE_ROW_H = 28;
   private static final int ACTION_BTN_H = 20;
   private static final int ROW_BTN_H = 20;
   private static final int ROW_BTN_W = 58;
   private static final int ROW_COPY_BTN_W = 24;
   private static final int ROW_COPY_BTN_H = 18;
   private static final int FIELD_COPY_BTN_W = 22;
   private static final int COL_GAP = 6;
   private static final int AUTH_CARD_MAX_W = 400;
   private static final int AUTH_FIELD_H = 22;
   private static final int AUTH_BTN_H = 22;
   private final Screen parent;
   private final List<ServerPasswordEntry> entries = new ArrayList<>();
   private StylishTextFieldWidget masterField;
   private StylishTextFieldWidget confirmField;
   private boolean creatingVault;
   private long authErrorShakeUntil;
   private int authCardX;
   private int authCardY;
   private int authCardW;
   private int authCardH;
   private int authFieldY;
   private StylishTextFieldWidget hostField;
   private StylishTextFieldWidget userField;
   private StylishTextFieldWidget passField;
   private StylishTextFieldWidget loginCmdField;
   private StylishTextFieldWidget registerCmdField;
   private StylishTextFieldWidget notesField;
   private boolean autoLogin = true;
   private boolean autoRegister;
   private boolean promptOnAuth = true;
   private boolean autoGenerate = true;
   private boolean revealPasswords;
   private int loginDelay = 40;
   private Long editingId;
   private String editingProfile;
   private int scroll;
   private String status = "";
   private int panelX;
   private int panelW;
   private int panelH;
   private int innerX;
   private int innerW;
   private int settingsBottomY;
   private int tableHeaderTop;
   private int listTop;
   private int listBottom;
   private int tableBodyBottom;
   private VaultTable table;

   public ServerPasswordScreen(Screen parent) {
      super(Text.literal("Server Password Vault"));
      this.parent = parent;
   }

   @Override
   public Screen getNavigationParent() {
      return this.parent;
   }

   private void goBack() {
      MultiplayerScreens.returnToMultiplayer(this.client, this.parent);
   }

   public void close() {
      this.goBack();
   }

   protected void init() {
      super.init();
      this.settingsBottomY = 0;
      this.clearChildren();
      this.layoutPanel();
      this.table = VaultTable.compute(this.innerX, this.innerW);
      if (!ServerPasswordManager.INSTANCE.isVaultInitialized()) {
         this.reloadEntries();
         this.initAuthScreen(true);
      } else if (!ServerPasswordManager.INSTANCE.isUnlocked()) {
         this.reloadEntries();
         this.initAuthScreen(false);
      } else {
         this.reloadEntries();
         this.initUnlockedScreen(this.contentTop());
      }
   }

   private int settingsSectionLabelY() {
      return 60;
   }

   private int contentTop() {
      return this.settingsSectionLabelY() + 10 + 8;
   }

   private void initAuthScreen(boolean creating) {
      this.creatingVault = creating;
      this.layoutAuthCard(creating);
      int fieldX = this.authCardX + 20;
      int fieldW = this.authCardW - 40;
      this.authFieldY = this.authCardY + 98;
      this.masterField = this.addStyledSecretField(fieldX, this.authFieldY, fieldW, "Master password");
      this.masterField.setChangedListener(text -> this.status = "");
      if (creating) {
         this.confirmField = this.addStyledSecretField(fieldX, this.authFieldY + 22 + 10, fieldW, "Confirm password");
         this.confirmField.setChangedListener(text -> this.status = "");
      } else {
         this.confirmField = null;
      }

      int primaryY = creating ? this.authFieldY + 64 + 14 : this.authFieldY + 22 + 14;
      this.addDrawableChild(
         new StylishButtonWidget(
            fieldX, primaryY, fieldW, 22, Text.literal(creating ? "Create vault" : "Unlock vault"), creating ? this::createVault : this::unlockVault
         )
      );
      this.addDrawableChild(new StylishButtonWidget(fieldX, primaryY + 22 + 8, fieldW, 22, ScreenTexts.BACK, this::goBack));
      this.setFocused(this.masterField);
   }

   private void initUnlockedScreen(int y) {
      ServerPasswordSettings settings = ServerPasswordManager.INSTANCE.settings();
      this.autoLogin = settings.autoLogin();
      this.autoRegister = settings.autoRegister();
      this.promptOnAuth = settings.promptOnAuth();
      this.autoGenerate = settings.autoGeneratePassword();
      this.loginDelay = settings.loginDelayTicks();
      int toggleW = (this.innerW - 24) / 4;
      this.addDrawableChild(new StylishButtonWidget(this.innerX, y, toggleW, 20, toggleLabel("Auto login", this.autoLogin), () -> {
         this.autoLogin = !this.autoLogin;
         this.saveSettings();
         this.init();
      }));
      this.addDrawableChild(new StylishButtonWidget(this.innerX + toggleW + 8, y, toggleW, 20, toggleLabel("Auto register", this.autoRegister), () -> {
         this.autoRegister = !this.autoRegister;
         this.saveSettings();
         this.init();
      }));
      this.addDrawableChild(new StylishButtonWidget(this.innerX + (toggleW + 8) * 2, y, toggleW, 20, toggleLabel("Save prompt", this.promptOnAuth), () -> {
         this.promptOnAuth = !this.promptOnAuth;
         this.saveSettings();
         this.init();
      }));
      this.addDrawableChild(new StylishButtonWidget(this.innerX + (toggleW + 8) * 3, y, toggleW, 20, toggleLabel("Auto-generate", this.autoGenerate), () -> {
         this.autoGenerate = !this.autoGenerate;
         this.saveSettings();
         this.init();
      }));
      y += 28;
      int half = (this.innerW - 8) / 2;
      int fieldW = half - 22 - 2;
      this.hostField = this.addField(this.innerX, y, fieldW, "play.example.com");
      this.addCopyFieldButton(this.innerX + half - 22, y, () -> this.copyFieldText(this.hostField.getText(), "server IP"));
      this.userField = this.addField(this.innerX + half + 8, y, fieldW, "Username / email (optional)");
      this.addCopyFieldButton(this.innerX + this.innerW - 22, y, () -> this.copyFieldText(this.userField.getText(), identityLabel(this.userField.getText())));
      y += 28;
      this.passField = this.addSecretField(this.innerX, y, fieldW, "Password");
      this.addCopyFieldButton(this.innerX + half - 22, y, () -> this.copyFieldText(this.passField.getText(), "password"));
      this.loginCmdField = this.addField(this.innerX + half + 8, y, 84, "login");
      this.registerCmdField = this.addField(this.innerX + half + 8 + 92, y, 84, "register");
      y += 28;
      this.notesField = this.addField(this.innerX, y, this.innerW, "Notes (optional)");
      y += 28;
      int actionW = (this.innerW - 24) / 4;
      this.addDrawableChild(
         new StylishButtonWidget(this.innerX, y, actionW, 20, Text.literal(this.editingId == null ? "Add Entry" : "Update Entry"), this::saveEntry)
      );
      this.addDrawableChild(new StylishButtonWidget(this.innerX + actionW + 8, y, actionW, 20, Text.literal("Clear"), this::clearForm));
      this.addDrawableChild(new StylishButtonWidget(this.innerX + (actionW + 8) * 2, y, actionW, 20, Text.literal("Lock"), this::lockVault));
      this.addDrawableChild(new StylishButtonWidget(this.innerX + (actionW + 8) * 3, y, actionW, 20, Text.literal("Login now"), this::loginSelectedHost));
      y += 24;
      int halfW = (this.innerW - 8) / 2;
      this.addDrawableChild(
         new StylishButtonWidget(this.innerX, y, halfW, 20, Text.literal(this.revealPasswords ? "Hide Passwords" : "Show Passwords"), () -> {
            this.revealPasswords = !this.revealPasswords;
            this.init();
         })
      );
      this.addDrawableChild(new StylishButtonWidget(this.innerX + halfW + 8, y, halfW, 20, ScreenTexts.BACK, this::goBack));
      this.settingsBottomY = y + 20;
      this.recomputeTableLayout();
      int visibleRows = this.visibleTableRows();
      this.scroll = Math.min(this.scroll, Math.max(0, this.entries.size() - visibleRows));

      for (int i = 0; i < visibleRows; i++) {
         int idx = this.scroll + i;
         if (idx >= this.entries.size()) {
            break;
         }

         ServerPasswordEntry entry = this.entries.get(idx);
         int rowY = this.listTop + i * 28;
         int btnY = rowY + 4;
         int copyY = rowY + 5;
         this.addDrawableChild(new StylishButtonWidget(this.table.copyIpX(), copyY, 24, 18, Text.literal("IP"), () -> this.copyEntryServer(entry)));
         this.addDrawableChild(new StylishButtonWidget(this.table.copyMailX(), copyY, 24, 18, Text.literal("@"), () -> this.copyEntryUser(entry)));
         this.addDrawableChild(new StylishButtonWidget(this.table.copyPwdX(), copyY, 24, 18, Text.literal("Pw"), () -> this.copyEntryPassword(entry)));
         this.addDrawableChild(new StylishButtonWidget(this.table.editX(), btnY, 58, 20, Text.literal("Edit"), () -> this.loadEntry(entry)));
         this.addDrawableChild(new StylishButtonWidget(this.table.delX(), btnY, 58, 20, Text.literal("Delete"), () -> this.deleteEntry(entry)));
      }
   }

   private void recomputeTableLayout() {
      this.layoutPanel();
      this.table = VaultTable.compute(this.innerX, this.innerW);
      if (this.settingsBottomY > 0) {
         this.tableHeaderTop = this.settingsBottomY + 5 + 12;
         this.listTop = this.tableHeaderTop + 16;
         this.tableBodyBottom = this.panelBottom() - 12;
         this.listBottom = this.tableBodyBottom;
         if (this.listBottom < this.listTop + 28) {
            this.listBottom = this.listTop + 84;
            this.tableBodyBottom = this.listBottom;
         }
      }
   }

   private int panelBottom() {
      return 24 + this.panelH;
   }

   private int visibleTableRows() {
      return Math.max(1, (this.listBottom - this.listTop) / 28);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      boolean authMode = this.isAuthMode();
      if (authMode) {
         this.layoutAuthCard(this.creatingVault);
         UiDraw.fillRootGradient(context, this.width, this.height);
         int shake = this.authShakeOffset();
         int cardX = this.authCardX + shake;
         UiDraw.cardElevated(context, cardX, this.authCardY, this.authCardW, this.authCardH, 14);
         this.drawAuthChrome(context, cardX);
      } else {
         this.layoutPanel();
         if (ServerPasswordManager.INSTANCE.isUnlocked()) {
            this.recomputeTableLayout();
         }

         UiDraw.fillMidnightBackground(context, this.width, this.height);
         UiDraw.cardElevated(context, this.panelX, 24, this.panelW, this.panelH, 14);
         this.drawPanelHeader(context);
         if (this.settingsBottomY > 0) {
            this.drawSectionLabel(context, "Settings", this.settingsSectionLabelY());
            this.drawSectionLabel(context, "Saved servers", this.settingsBottomY + 4);
         }
      }

      super.render(context, mouseX, mouseY, deltaTicks);
      if (!authMode && ServerPasswordManager.INSTANCE.isUnlocked() && this.table != null && this.settingsBottomY > 0) {
         this.drawTable(context);
      }

      this.drawStatus(context, authMode);
   }

   private boolean isAuthMode() {
      return !ServerPasswordManager.INSTANCE.isVaultInitialized() || !ServerPasswordManager.INSTANCE.isUnlocked();
   }

   private void drawAuthChrome(DrawContext context, int cardX) {
      int iconX = this.width / 2;
      int iconY = this.authCardY + 28;
      int iconR = 14;
      UiDraw.card(context, iconX - iconR, iconY - iconR, iconR * 2, iconR * 2);
      context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.creatingVault ? "+" : "V"), iconX, iconY - 4, -7934036);
      String headline = this.creatingVault ? "Create your vault" : "Unlock vault";
      context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(headline), this.width / 2, this.authCardY + 52, -460036);
      int accentW = 56;
      context.fill(this.width / 2 - accentW / 2, this.authCardY + 64, this.width / 2 + accentW / 2, this.authCardY + 65, -11870592);
      String subtitle = this.creatingVault
         ? "Pick a master password to encrypt saved server credentials."
         : "Enter your master password to access saved server passwords.";
      this.drawWrappedCentered(context, subtitle, cardX + 20, this.authCardY + 72, this.authCardW - 40, -7035976);
      context.drawText(this.textRenderer, "Master password", cardX + 20, this.authFieldY - 10, -7035976, false);
      if (this.creatingVault && this.confirmField != null) {
         context.drawText(this.textRenderer, "Confirm password", cardX + 20, this.authFieldY + 22 + 1, -7035976, false);
      }

      if (this.creatingVault) {
         context.drawText(
            this.textRenderer, "Minimum 4 characters · never stored in plain text", cardX + 20, this.authCardY + this.authCardH - 14, -10193781, false
         );
      }
   }

   private void drawWrappedCentered(DrawContext context, String text, int x, int y, int maxW, int color) {
      if (text != null && !text.isBlank()) {
         String trimmed = this.textRenderer.trimToWidth(text, maxW);
         context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(trimmed), x + maxW / 2, y, color);
      }
   }

   private void drawStatus(DrawContext context, boolean authMode) {
      if (!this.status.isBlank()) {
         int color = this.statusColor();
         int y = authMode ? this.authCardY + this.authCardH - 28 : this.height - 24;
         context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.status), this.width / 2, y, color);
      }
   }

   private int statusColor() {
      String lower = this.status.toLowerCase();
      if (lower.contains("incorrect")
         || lower.contains("failed")
         || lower.contains("denied")
         || lower.contains("match")
         || lower.contains("invalid")
         || lower.contains("required")) {
         return UiTokens.argb(255, 16281969);
      } else {
         return !lower.contains("created")
               && !lower.contains("unlocked")
               && !lower.contains("added")
               && !lower.contains("updated")
               && !lower.contains("deleted")
               && !lower.contains("sent")
               && !lower.contains("copied")
            ? -3418655
            : -7934036;
      }
   }

   private void layoutAuthCard(boolean creating) {
      this.authCardW = Math.min(400, this.width - 48);
      this.authCardH = creating ? 252 : 214;
      this.authCardX = (this.width - this.authCardW) / 2;
      this.authCardY = (this.height - this.authCardH) / 2 - 12;
   }

   private int authShakeOffset() {
      long remaining = this.authErrorShakeUntil - System.currentTimeMillis();
      return remaining <= 0L ? 0 : (int)(Math.sin(remaining * 0.05) * 5.0);
   }

   private void shakeAuth() {
      this.authErrorShakeUntil = System.currentTimeMillis() + 420L;
   }

   public boolean keyPressed(KeyInput input) {
      if (this.isAuthMode()) {
         int key = input.key();
         if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.creatingVault) {
               this.createVault();
            } else {
               this.unlockVault();
            }

            return true;
         }
      }

      return super.keyPressed(input);
   }

   private void drawPanelHeader(DrawContext context) {
      int headerBottom = 54;
      context.fill(this.panelX + 1, 25, this.panelX + this.panelW - 1, headerBottom, UiTokens.argb(102, -15788246));
      context.fill(this.innerX, headerBottom - 1, this.innerX + this.innerW, headerBottom, UiTokens.argb(136, -15293622));
      context.drawText(this.textRenderer, this.title.getString(), this.innerX, 35, -460036, false);
      String hint = "Profile: " + ServerPasswordManager.INSTANCE.currentProfileName();
      int hintW = this.textRenderer.getWidth(hint);
      context.drawText(this.textRenderer, hint, this.innerX + this.innerW - hintW, 35, -10193781, false);
   }

   private void drawSectionLabel(DrawContext context, String label, int y) {
      context.drawText(this.textRenderer, label, this.innerX, y, -7035976, false);
   }

   private void drawTable(DrawContext context) {
      int tableLeft = this.innerX;
      int tableRight = this.innerX + this.innerW;
      int tableBottom = this.tableBodyBottom;
      UiComponents.drawSurfaceCard(context, tableLeft, this.tableHeaderTop - 2, this.innerW, tableBottom - this.tableHeaderTop + 4);
      int headerTextY = this.tableHeaderTop + 5;
      context.fill(tableLeft, this.tableHeaderTop, tableRight, this.tableHeaderTop + 16, UiTokens.argb(170, -15788246));
      context.fill(tableLeft, this.tableHeaderTop + 16 - 1, tableRight, this.tableHeaderTop + 16, UiTokens.argb(102, -12102295));
      this.drawHeaderCell(context, "Server", this.table.serverX(), this.table.serverW(), headerTextY);
      this.drawHeaderCell(context, "User", this.table.userX(), this.table.userW(), headerTextY);
      this.drawHeaderCell(context, "Flags", this.table.flagsX(), this.table.flagsW(), headerTextY);
      this.drawHeaderCell(context, "Password", this.table.pwdX(), this.table.pwdW(), headerTextY);
      this.drawHeaderCell(context, "Copy", this.table.copyIpX(), this.table.copyGroupW(), headerTextY);
      this.drawHeaderCell(context, "Edit", this.table.editX(), 58, headerTextY);
      this.drawHeaderCell(context, "Delete", this.table.delX(), 58, headerTextY);
      this.drawColumnGuides(context, this.tableHeaderTop + 16, tableBottom);
      int visibleRows = this.visibleTableRows();
      if (this.entries.isEmpty()) {
         int emptyTop = this.listTop;
         int emptyH = Math.max(28, tableBottom - emptyTop);
         context.fill(tableLeft, emptyTop, tableRight, emptyTop + emptyH, UiTokens.argb(51, -14800581));
         context.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("No saved servers yet"), tableLeft + this.innerW / 2, emptyTop + (emptyH - 8) / 2, -7035976
         );
      } else {
         for (int i = 0; i < visibleRows; i++) {
            int idx = this.scroll + i;
            if (idx >= this.entries.size()) {
               break;
            }

            ServerPasswordEntry e = this.entries.get(idx);
            int rowY = this.listTop + i * 28;
            if (rowY + 28 > tableBottom) {
               break;
            }

            int bg = idx % 2 == 0 ? UiTokens.argb(68, -15788246) : UiTokens.argb(51, -14800581);
            context.fill(tableLeft, rowY, tableRight, rowY + 28, bg);
            if (i > 0) {
               context.fill(tableLeft, rowY, tableRight, rowY + 1, UiTokens.argb(68, -13418155));
            }

            int textY = rowY + 10;
            this.drawCellText(context, truncate(e.label(), this.table.serverW()), this.table.serverX(), this.table.serverW(), textY, -460036);
            this.drawCellText(context, truncate(this.formatEntryUser(e), this.table.userW()), this.table.userX(), this.table.userW(), textY, -3418655);
            String flags = (e.autoLogin() ? "L" : "-") + (e.autoRegister() ? "R" : "-");
            this.drawCellText(context, flags, this.table.flagsX(), this.table.flagsW(), textY, -7934036);
            String pwd = this.revealPasswords ? e.password() : "********";
            this.drawCellText(context, this.truncateToWidth(pwd, this.table.pwdW()), this.table.pwdX(), this.table.pwdW(), textY, -10193781);
         }

         if (this.entries.size() > visibleRows) {
            UiDraw.drawScrollbar(context, tableRight - 4, this.listTop, tableBottom, this.scroll, this.entries.size() - visibleRows);
         }
      }
   }

   private void drawColumnGuides(DrawContext context, int top, int bottom) {
      int[] dividers = new int[]{this.table.userX() - 3, this.table.flagsX() - 3, this.table.pwdX() - 3, this.table.copyIpX() - 3, this.table.editX() - 3};

      for (int x : dividers) {
         context.fill(x, top, x + 1, bottom, UiTokens.argb(51, -12102295));
      }
   }

   private void drawHeaderCell(DrawContext context, String label, int x, int w, int y) {
      context.drawText(this.textRenderer, label, x + 4, y, -3418655, false);
   }

   private void drawCellText(DrawContext context, String text, int x, int w, int y, int color) {
      String shown = this.textRenderer.trimToWidth(text == null ? "" : text, Math.max(8, w - 8));
      context.drawText(this.textRenderer, shown, x + 4, y, color, false);
   }

   private void lockVault() {
      ServerPasswordManager.INSTANCE.lock();
      this.status = "Vault locked";
      this.init();
   }

   private void createVault() {
      char[] pass = this.masterField.getText().toCharArray();
      if (pass.length < 4) {
         this.status = "Master password must be at least 4 characters";
         this.shakeAuth();
      } else if (this.confirmField != null && !this.masterField.getText().equals(this.confirmField.getText())) {
         this.status = "Passwords do not match";
         this.shakeAuth();
         ServerPasswordVault.wipe(pass);
      } else {
         try {
            ServerPasswordManager.INSTANCE.createVault(pass);
            this.status = "Vault created";
            this.init();
         } catch (VaultAccessDeniedException var7) {
            this.status = "Vault access denied";
            this.shakeAuth();
         } catch (Exception var8) {
            this.status = "Failed to create vault";
            this.shakeAuth();
         } finally {
            ServerPasswordVault.wipe(pass);
         }
      }
   }

   private void unlockVault() {
      char[] pass = this.masterField.getText().toCharArray();
      if (pass.length == 0) {
         this.status = "Master password required";
         this.shakeAuth();
         ServerPasswordVault.wipe(pass);
      } else {
         if (ServerPasswordManager.INSTANCE.unlock(pass)) {
            this.status = "Vault unlocked";
            this.masterField.setText("");
            this.init();
         } else {
            this.status = "Incorrect master password";
            this.shakeAuth();
         }

         ServerPasswordVault.wipe(pass);
      }
   }

   private void saveEntry() {
      try {
         String host = ServerPasswordKeys.normalize(this.hostField.getText());
         if (host.isBlank()) {
            this.status = "Server address required";
            return;
         }

         if (this.passField.getText().isBlank()) {
            this.status = "Password required";
            return;
         }

         String profile = this.editingId == null
            ? ServerPasswordManager.INSTANCE.currentProfileName()
            : (this.editingProfile == null ? "" : this.editingProfile);
         ServerPasswordEntry entry = new ServerPasswordEntry(
            this.editingId == null ? 0L : this.editingId,
            host,
            profile,
            this.hostField.getText().trim(),
            this.userField.getText().trim(),
            this.passField.getText(),
            this.loginCmdField.getText().trim(),
            this.registerCmdField.getText().trim(),
            this.autoLogin,
            this.autoRegister,
            this.notesField.getText().trim(),
            System.currentTimeMillis()
         );
         ServerPasswordManager.INSTANCE.saveVaultEntry(entry);
         this.status = this.editingId == null ? "Entry added" : "Entry updated";
         this.clearForm();
         this.init();
      } catch (VaultInputException var4) {
         this.status = var4.getMessage();
      } catch (Exception var5) {
         DupeClient.LOGGER.error("Vault save failed", var5);
         String message = var5.getMessage();
         this.status = message != null && !message.isBlank() ? "Save failed: " + message : "Save failed";
      }
   }

   private void deleteEntry(ServerPasswordEntry entry) {
      try {
         ServerPasswordManager.INSTANCE.deleteEntry(entry.id(), entry.profileName());
         if (this.editingId != null && this.editingId == entry.id()) {
            this.clearForm();
         }

         this.status = "Deleted " + entry.hostKey();
         this.init();
      } catch (VaultInputException var3) {
         this.status = var3.getMessage();
      }
   }

   private void loadEntry(ServerPasswordEntry entry) {
      this.editingId = entry.id();
      this.editingProfile = entry.profileName();
      this.hostField.setText(entry.hostKey());
      this.userField.setText(entry.username() == null ? "" : entry.username());
      this.passField.setText(entry.password());
      this.loginCmdField.setText(entry.loginCommand());
      this.registerCmdField.setText(entry.registerCommand());
      this.notesField.setText(entry.notes() == null ? "" : entry.notes());
      this.autoLogin = entry.autoLogin();
      this.autoRegister = entry.autoRegister();
      this.status = "Editing " + entry.hostKey();
   }

   private void clearForm() {
      this.editingId = null;
      this.editingProfile = null;
      if (this.hostField != null) {
         this.hostField.setText("");
      }

      if (this.userField != null) {
         this.userField.setText("");
      }

      if (this.passField != null) {
         this.passField.setText("");
      }

      if (this.loginCmdField != null) {
         this.loginCmdField.setText("login");
      }

      if (this.registerCmdField != null) {
         this.registerCmdField.setText("register");
      }

      if (this.notesField != null) {
         this.notesField.setText("");
      }
   }

   private void loginSelectedHost() {
      String host = ServerPasswordKeys.normalize(this.hostField.getText());
      if (host.isBlank()) {
         this.status = "Enter a server host first";
      } else {
         ServerPasswordManager.INSTANCE.findEntry(host).ifPresentOrElse(entry -> {
            ServerPasswordManager.INSTANCE.sendAuthCommand(this.client, entry.loginCommand(), entry.username(), entry.password());
            this.status = "Login command sent";
         }, () -> this.status = "No saved entry for that host");
      }
   }

   private void saveSettings() {
      ServerPasswordManager.INSTANCE
         .saveSettings(new ServerPasswordSettings(this.promptOnAuth, this.autoLogin, this.autoRegister, this.autoGenerate, this.loginDelay));
   }

   private void reloadEntries() {
      this.entries.clear();
      if (ServerPasswordManager.INSTANCE.isUnlocked()) {
         try {
            this.entries.addAll(ServerPasswordManager.INSTANCE.listAllEntriesForVault());
         } catch (Exception var2) {
            DupeClient.LOGGER.error("Failed to reload vault entries", var2);
            this.status = "Failed to load saved entries";
         }
      }
   }

   private void addCopyFieldButton(int x, int y, Runnable action) {
      this.addDrawableChild(new StylishButtonWidget(x, y, 22, 20, Text.literal("⎘"), action));
   }

   private void copyFieldText(String value, String label) {
      this.copyToClipboard(value == null ? "" : value.trim(), label);
   }

   private void copyEntryServer(ServerPasswordEntry entry) {
      String address = entry.displayName() != null && !entry.displayName().isBlank() ? entry.displayName() : entry.hostKey();
      this.copyToClipboard(address, "server IP");
   }

   private void copyEntryUser(ServerPasswordEntry entry) {
      if (entry.username() != null && !entry.username().isBlank()) {
         this.copyToClipboard(entry.username(), identityLabel(entry.username()));
      } else {
         this.status = "No username or email saved";
      }
   }

   private void copyEntryPassword(ServerPasswordEntry entry) {
      this.copyToClipboard(entry.password(), "password");
   }

   private static String identityLabel(String value) {
      return VaultInputValidator.looksLikeRegisterEmail(value) ? "email" : "username";
   }

   private void copyToClipboard(String value, String label) {
      if (value != null && !value.isBlank()) {
         if (this.client != null && this.client.keyboard != null) {
            this.client.keyboard.setClipboard(value);
            this.status = "Copied " + label;
         }
      } else {
         this.status = "Nothing to copy";
      }
   }

   private void layoutPanel() {
      this.panelW = Math.min(680, this.width - 24);
      this.panelH = this.height - 24 - 24;
      this.panelX = (this.width - this.panelW) / 2;
      this.innerX = this.panelX + 14;
      this.innerW = this.panelW - 28;
   }

   private StylishTextFieldWidget addField(int x, int y, int w, String placeholder) {
      StylishTextFieldWidget field = StylishTextFieldWidget.create(this.textRenderer, x, y, w, Text.literal(placeholder));
      field.setPlaceholder(placeholder);
      field.setMaxLength(256);
      return (StylishTextFieldWidget)this.addDrawableChild(field);
   }

   private StylishTextFieldWidget addStyledSecretField(int x, int y, int w, String placeholder) {
      StylishTextFieldWidget field = StylishTextFieldWidget.create(this.textRenderer, x, y, w, AUTH_FIELD_H, Text.literal(placeholder));
      field.setPlaceholder(placeholder);
      field.setMaxLength(256);
      return this.addDrawableChild(field);
   }

   private StylishTextFieldWidget addSecretField(int x, int y, int w, String placeholder) {
      return this.addField(x, y, w, placeholder);
   }

   private static Text toggleLabel(String label, boolean on) {
      return Text.literal(label + ": " + (on ? "ON" : "OFF"));
   }

   private String formatEntryUser(ServerPasswordEntry entry) {
      String user = entry.username() != null && !entry.username().isBlank() ? entry.username() : "-";
      String profile = entry.profileName();
      if (profile != null && !profile.isBlank()) {
         try {
            String current = ServerPasswordManager.INSTANCE.currentProfileName();
            if (profile.equals(current)) {
               return user;
            }
         } catch (VaultInputException var5) {
         }

         return user + " · " + profile;
      } else {
         return user;
      }
   }

   private static String truncate(String value, int maxChars) {
      if (value == null) {
         return "";
      } else {
         return value.length() <= maxChars ? value : value.substring(0, Math.max(0, maxChars - 3)) + "...";
      }
   }

   private String truncateToWidth(String value, int maxWidth) {
      return value == null ? "" : this.textRenderer.trimToWidth(value, Math.max(8, maxWidth - 8));
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (ServerPasswordManager.INSTANCE.isUnlocked() && this.settingsBottomY > 0 && mouseY >= this.listTop && mouseY <= this.listBottom) {
         int maxScroll = Math.max(0, this.entries.size() - this.visibleTableRows());
         if (verticalAmount > 0.0) {
            this.scroll = Math.max(0, this.scroll - 1);
         } else if (verticalAmount < 0.0) {
            this.scroll = Math.min(maxScroll, this.scroll + 1);
         }

         this.init();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   private record VaultTable(
         int serverX,
         int serverW,
         int userX,
         int userW,
         int flagsX,
         int flagsW,
         int pwdX,
         int pwdW,
         int copyIpX,
         int copyMailX,
         int copyPwdX,
         int copyGroupW,
         int editX,
         int delX) {
      private static VaultTable compute(int left, int width) {
         int copyBtnW = ROW_COPY_BTN_W;
         int copyGroupW = copyBtnW * 3 + 4;
         int editW = ROW_BTN_W;
         int delW = ROW_BTN_W;
         int actionsW = copyGroupW + 6 + editW + 6 + delW;
         int flagsW = 44;
         int pwdW = 92;
         int userW = 104;
         int serverW = width - userW - flagsW - pwdW - actionsW - 30;
         serverW = Math.max(120, serverW);
         int userX = left + serverW + COL_GAP;
         int flagsX = userX + userW + COL_GAP;
         int pwdX = flagsX + flagsW + COL_GAP;
         int copyIpX = pwdX + pwdW + COL_GAP;
         int copyMailX = copyIpX + copyBtnW + 2;
         int copyPwdX = copyMailX + copyBtnW + 2;
         int delX = left + width - delW;
         int editX = delX - 6 - editW;
         return new VaultTable(left, serverW, userX, userW, flagsX, flagsW, pwdX, pwdW,
               copyIpX, copyMailX, copyPwdX, copyGroupW, editX, delX);
      }
   }
}
