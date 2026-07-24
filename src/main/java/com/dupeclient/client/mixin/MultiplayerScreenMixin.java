package com.dupeclient.client.mixin;

import com.dupeclient.client.module.dupedb.search.ServerSearchAuthScreen;
import com.dupeclient.client.module.serverpassword.ServerPasswordScreen;
import com.dupeclient.client.multiplayer.MultiplayerHeaderButtonFilter;
import com.dupeclient.client.multiplayer.OfflineAccountsScreen;
import com.dupeclient.client.multiplayer.ProxiesScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MultiplayerScreen.class, priority = 5000)
public abstract class MultiplayerScreenMixin extends Screen {
    @Unique
    private static final int DUPECLIENT$HEADER_BTN_W = 75;
    @Unique
    private static final int DUPECLIENT$SEARCH_BTN_W = 110;
    @Unique
    private static final int DUPECLIENT$HEADER_MARGIN = 3;
    @Unique
    private static final int DUPECLIENT$HEADER_GAP = 2;
    @Unique
    private static final String DUPECLIENT$SEARCH_LABEL = "Server Search";
    @Unique
    private static final String DUPECLIENT$VAULT_LABEL = "Vault";
    @Unique
    private static final int DUPECLIENT$VAULT_BTN_W = 52;
    @Unique
    private static final String DUPECLIENT$PROXIES_LABEL = "Proxies";
    @Unique
    private static final String DUPECLIENT$ACCOUNTS_LABEL = "Accounts";

    @Unique
    private ButtonWidget dupeClient$vaultButton;
    @Unique
    private ButtonWidget dupeClient$serverSearchButton;
    @Unique
    private ButtonWidget dupeClient$proxiesButton;
    @Unique
    private ButtonWidget dupeClient$accountsButton;

    protected MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void dupeClient$initHeaderButtons(CallbackInfo ci) {
        dupeClient$detachOwnedHeaderButtons();
        dupeClient$attachHeaderButtonsIfNeeded();
        dupeClient$layoutHeaderButtons();
    }

    @Inject(method = "refreshWidgetPositions", at = @At("TAIL"))
    private void dupeClient$refreshHeaderButtons(CallbackInfo ci) {
        dupeClient$stripForeignHeaderButtons();
        dupeClient$attachHeaderButtonsIfNeeded();
        dupeClient$layoutHeaderButtons();
    }

    @Unique
    private void dupeClient$attachHeaderButtonsIfNeeded() {
        if (dupeClient$buttonsAlive()) {
            return;
        }
        dupeClient$detachOwnedHeaderButtons();

        dupeClient$vaultButton = ButtonWidget.builder(Text.literal(DUPECLIENT$VAULT_LABEL), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new ServerPasswordScreen((MultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$VAULT_BTN_W, 20).build();
        this.addDrawableChild(dupeClient$vaultButton);

        dupeClient$serverSearchButton = ButtonWidget.builder(Text.literal(DUPECLIENT$SEARCH_LABEL), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new ServerSearchAuthScreen((MultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$SEARCH_BTN_W, 20).build();
        this.addDrawableChild(dupeClient$serverSearchButton);

        dupeClient$proxiesButton = ButtonWidget.builder(Text.literal(DUPECLIENT$PROXIES_LABEL), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new ProxiesScreen((MultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$HEADER_BTN_W, 20).build();
        this.addDrawableChild(dupeClient$proxiesButton);

        dupeClient$accountsButton = ButtonWidget.builder(Text.literal(DUPECLIENT$ACCOUNTS_LABEL), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new OfflineAccountsScreen((MultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$HEADER_BTN_W, 20).build();
        this.addDrawableChild(dupeClient$accountsButton);
    }

    @Unique
    private boolean dupeClient$buttonsAlive() {
        return dupeClient$vaultButton != null
            && dupeClient$serverSearchButton != null
            && dupeClient$proxiesButton != null
            && dupeClient$accountsButton != null
            && this.children().contains(dupeClient$vaultButton)
            && this.children().contains(dupeClient$serverSearchButton)
            && this.children().contains(dupeClient$proxiesButton)
            && this.children().contains(dupeClient$accountsButton);
    }

    @Unique
    private void dupeClient$layoutHeaderButtons() {
        if (!dupeClient$buttonsAlive()) {
            return;
        }
        int accountsX = this.width - DUPECLIENT$HEADER_MARGIN - DUPECLIENT$HEADER_BTN_W;
        int proxiesX = accountsX - DUPECLIENT$HEADER_GAP - DUPECLIENT$HEADER_BTN_W;
        int searchX = Math.max(4, proxiesX - DUPECLIENT$HEADER_GAP - DUPECLIENT$SEARCH_BTN_W);
        int vaultX = Math.max(4, searchX - DUPECLIENT$HEADER_GAP - DUPECLIENT$VAULT_BTN_W);

        dupeClient$accountsButton.setPosition(accountsX, 3);
        dupeClient$proxiesButton.setPosition(proxiesX, 3);
        dupeClient$serverSearchButton.setPosition(searchX, 3);
        dupeClient$vaultButton.setPosition(vaultX, 3);
    }

    @Unique
    private void dupeClient$stripForeignHeaderButtons() {
        List<Element> toRemove = new ArrayList<>();
        for (Element child : this.children()) {
            if (child instanceof ClickableWidget widget
                && MultiplayerHeaderButtonFilter.isForeignHeaderButton(
                widget, dupeClient$vaultButton, dupeClient$serverSearchButton, dupeClient$proxiesButton, dupeClient$accountsButton)) {
                toRemove.add(child);
            }
        }
        for (Element child : toRemove) {
            this.remove(child);
        }
    }

    @Unique
    private void dupeClient$detachOwnedHeaderButtons() {
        if (dupeClient$vaultButton != null) {
            this.remove(dupeClient$vaultButton);
        }
        if (dupeClient$serverSearchButton != null) {
            this.remove(dupeClient$serverSearchButton);
        }
        if (dupeClient$proxiesButton != null) {
            this.remove(dupeClient$proxiesButton);
        }
        if (dupeClient$accountsButton != null) {
            this.remove(dupeClient$accountsButton);
        }
        dupeClient$vaultButton = null;
        dupeClient$serverSearchButton = null;
        dupeClient$proxiesButton = null;
        dupeClient$accountsButton = null;
    }
}
