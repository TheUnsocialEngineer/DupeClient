package com.dupeclient.client.mixin;

import com.dupeclient.client.module.dupedb.search.ServerSearchAuthScreen;
import com.dupeclient.client.module.serverpassword.ServerPasswordScreen;
import com.dupeclient.client.multiplayer.MultiplayerHeaderButtonFilter;
import com.dupeclient.client.multiplayer.OfflineAccountsScreen;
import com.dupeclient.client.multiplayer.ProxiesScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

@Mixin(value = JoinMultiplayerScreen.class, priority = 5000)
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
    private Button dupeClient$vaultButton;
    @Unique
    private Button dupeClient$serverSearchButton;
    @Unique
    private Button dupeClient$proxiesButton;
    @Unique
    private Button dupeClient$accountsButton;

    protected MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void dupeClient$initHeaderButtons(CallbackInfo ci) {
        dupeClient$detachOwnedHeaderButtons();
        dupeClient$attachHeaderButtonsIfNeeded();
        dupeClient$layoutHeaderButtons();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
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

        dupeClient$vaultButton = Button.builder(Component.literal(DUPECLIENT$VAULT_LABEL), button -> {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.setScreen(new ServerPasswordScreen((JoinMultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$VAULT_BTN_W, 20).build();
        this.addRenderableWidget(dupeClient$vaultButton);

        dupeClient$serverSearchButton = Button.builder(Component.literal(DUPECLIENT$SEARCH_LABEL), button -> {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.setScreen(new ServerSearchAuthScreen((JoinMultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$SEARCH_BTN_W, 20).build();
        this.addRenderableWidget(dupeClient$serverSearchButton);

        dupeClient$proxiesButton = Button.builder(Component.literal(DUPECLIENT$PROXIES_LABEL), button -> {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.setScreen(new ProxiesScreen((JoinMultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$HEADER_BTN_W, 20).build();
        this.addRenderableWidget(dupeClient$proxiesButton);

        dupeClient$accountsButton = Button.builder(Component.literal(DUPECLIENT$ACCOUNTS_LABEL), button -> {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.setScreen(new OfflineAccountsScreen((JoinMultiplayerScreen) (Object) this));
            }
        }).size(DUPECLIENT$HEADER_BTN_W, 20).build();
        this.addRenderableWidget(dupeClient$accountsButton);
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
        List<GuiEventListener> toRemove = new ArrayList<>();
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget
                && MultiplayerHeaderButtonFilter.isForeignHeaderButton(
                widget, dupeClient$vaultButton, dupeClient$serverSearchButton, dupeClient$proxiesButton, dupeClient$accountsButton)) {
                toRemove.add(child);
            }
        }
        for (GuiEventListener child : toRemove) {
            this.removeWidget(child);
        }
    }

    @Unique
    private void dupeClient$detachOwnedHeaderButtons() {
        if (dupeClient$vaultButton != null) {
            this.removeWidget(dupeClient$vaultButton);
        }
        if (dupeClient$serverSearchButton != null) {
            this.removeWidget(dupeClient$serverSearchButton);
        }
        if (dupeClient$proxiesButton != null) {
            this.removeWidget(dupeClient$proxiesButton);
        }
        if (dupeClient$accountsButton != null) {
            this.removeWidget(dupeClient$accountsButton);
        }
        dupeClient$vaultButton = null;
        dupeClient$serverSearchButton = null;
        dupeClient$proxiesButton = null;
        dupeClient$accountsButton = null;
    }
}
