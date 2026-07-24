package com.dupeclient.client.ui.mui;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.ui.MuiUiRouter;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import org.jetbrains.annotations.Nullable;

public final class MuiMainMenuFragment extends Fragment {
    private static int dp(icyllis.modernui.core.Context c, int d) {
        return Math.round(0.5f + c.getResources().getDisplayMetrics().density * d);
    }

    private static LinearLayout.LayoutParams row(icyllis.modernui.core.Context c) {
        int m = dp(c, 4);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, m);
        return p;
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, DataSet saved) {
        var c = requireContext();
        int pad = dp(c, 16);
        var scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        var col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        var caption = new TextView(c);
        caption.setText("DupeClient " + DupeClient.BUILD_TAG);
        caption.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        col.addView(caption, row(c));

        col.addView(b(c, "Singleplayer", v -> toScreen(new SelectWorldScreen(liveParent()))), row(c));
        col.addView(b(c, "Multiplayer", v -> toScreen(new MultiplayerScreen(liveParent()))), row(c));
        col.addView(b(c, "Minecraft Realms", v -> toScreen(new RealmsMainScreen(liveParent()))), row(c));
        col.addView(b(c, "Mods", v -> openModsScreen()), row(c));
        col.addView(b(c, "DupeClient settings", v -> MuiUiRouter.openSettings(liveParent())), row(c));

        var row2 = new LinearLayout(c);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        int halfGap = dp(c, 2);
        int rowBottom = dp(c, 4);
        var left = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        left.setMargins(0, 0, halfGap, rowBottom);
        var opt = b(c, "Options", v -> toScreen(new OptionsScreen(liveParent(), getClient().options)));
        row2.addView(opt, left);
        var right = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        right.setMargins(halfGap, 0, 0, rowBottom);
        var quit = b(c, "Quit Game", v -> getClient().scheduleStop());
        row2.addView(quit, right);
        col.addView(row2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        scroll.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }

    private static Button b(icyllis.modernui.core.Context c, String label, View.OnClickListener l) {
        var btn = new Button(c);
        btn.setText(label);
        btn.setOnClickListener(l);
        btn.setMinHeight(dp(c, 40));
        btn.setGravity(Gravity.CENTER);
        return btn;
    }

    private MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }

    /**
     * Parent to pass into vanilla sub-screens; uses whatever is current when the click runs.
     */
    private Screen liveParent() {
        return getClient().currentScreen;
    }

    private void toScreen(Screen next) {
        getClient().setScreen(next);
    }

    private void openModsScreen() {
        try {
            Class<?> clazz = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            toScreen((Screen) clazz.getConstructor(Screen.class).newInstance(liveParent()));
        } catch (Exception e) {
            DupeClient.LOGGER.warn("ModMenu not available, Mods button ignored.");
        }
    }
}
