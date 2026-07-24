package com.dupeclient.client.ui.mui;

import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.ScreenCallback;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * mVUS entry points. Title / main menu is not routed here — only in-game and macro/social UIs.
 */
public final class MuiDupeScreens {
    private MuiDupeScreens() {
    }

    /** Hub: MC surface draws the full module; no extra vanilla dim behind the mVUS chrome. */
    private static ScreenCallback clientHubCallback() {
        return new ScreenCallback() {
            @Override
            public boolean isPauseScreen() {
                return false;
            }

            @Override
            public boolean hasDefaultBackground() {
                return false;
            }
        };
    }

    private static ScreenCallback hostedCallback() {
        return new ScreenCallback() {
            @Override
            public boolean isPauseScreen() {
                return false;
            }

            @Override
            public boolean hasDefaultBackground() {
                return true;
            }
        };
    }

    public static Screen createClientGui(Screen parent) {
        MuiClientGuiFragment f = new MuiClientGuiFragment();
        f.setReturnScreen(parent);
        return MuiModApi.get().createScreen(f, clientHubCallback(), parent, "DupeClient");
    }

    public static Screen createSocialScreen(Screen parent) {
        MuiHostedScreenFragment f = MuiHostedScreenFragment.forSocial(parent);
        return MuiModApi.get().createScreen(f, hostedCallback(), parent, "DupeClient Social");
    }

    public static Screen createMacroEditorScreen(Screen parent, @Nullable String loadId) {
        MuiHostedScreenFragment f = MuiHostedScreenFragment.forMacro(parent, loadId);
        return MuiModApi.get().createScreen(f, hostedCallback(), parent, "Macro editor");
    }

    public static Screen createHudEditorScreen(Screen parent) {
        MuiHostedScreenFragment f = MuiHostedScreenFragment.forHud(parent);
        return MuiModApi.get().createScreen(f, hostedCallback(), parent, "HUD Editor");
    }
}
