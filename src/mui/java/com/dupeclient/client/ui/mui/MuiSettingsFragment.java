package com.dupeclient.client.ui.mui;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.VisualSettings;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * MUI port of DupeClient settings: same options and cycling behavior as the vanilla screen.
 */
public final class MuiSettingsFragment extends Fragment {
    private Screen returnScreen;
    private Button animatedButton;
    private Button particleButton;
    private Button motionButton;
    private Button twinkleButton;

    void setReturnScreen(Screen returnScreen) {
        this.returnScreen = returnScreen;
    }

    private static int dp(icyllis.modernui.core.Context c, int d) {
        return Math.round(0.5f + c.getResources().getDisplayMetrics().density * d);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet saved) {
        var c = requireContext();
        int pad = dp(c, 16);
        int gap = dp(c, 4);
        var scroll = new ScrollView(c);
        var col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        animatedButton = b(c, animatedLabel());
        animatedButton.setOnClickListener(v -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            settings.animatedBackground = !settings.animatedBackground;
            DupeClient.saveVisualSettings();
            refreshOptionLabels();
        });
        col.addView(animatedButton, fullWidthRow(c, gap));

        particleButton = b(c, particleLabel());
        particleButton.setOnClickListener(v -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            int[] options = {80, 120, 180, 240, 320, 420};
            settings.particleCount = nextIntOption(settings.particleCount, options);
            DupeClient.saveVisualSettings();
            refreshOptionLabels();
        });
        col.addView(particleButton, fullWidthRow(c, gap));

        motionButton = b(c, motionLabel());
        motionButton.setOnClickListener(v -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            float[] options = {0.4F, 0.7F, 1.0F, 1.3F, 1.7F};
            settings.motionIntensity = nextFloatOption(settings.motionIntensity, options);
            DupeClient.saveVisualSettings();
            refreshOptionLabels();
        });
        col.addView(motionButton, fullWidthRow(c, gap));

        twinkleButton = b(c, twinkleLabel());
        twinkleButton.setOnClickListener(v -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            float[] options = {0.6F, 0.9F, 1.0F, 1.3F, 1.6F};
            settings.twinkleSpeed = nextFloatOption(settings.twinkleSpeed, options);
            DupeClient.saveVisualSettings();
            refreshOptionLabels();
        });
        col.addView(twinkleButton, fullWidthRow(c, gap));

        var reset = b(c, "Reset to defaults");
        reset.setOnClickListener(v -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            settings.animatedBackground = true;
            settings.particleCount = 180;
            settings.motionIntensity = 1.0F;
            settings.twinkleSpeed = 1.0F;
            DupeClient.saveVisualSettings();
            refreshOptionLabels();
        });
        col.addView(reset, fullWidthRow(c, gap));

        var back = b(c, "Back");
        back.setOnClickListener(v -> {
            Screen r = returnScreen;
            if (r != null) {
                MinecraftClient.getInstance().setScreen(r);
            } else {
                MinecraftClient.getInstance().setScreen(null);
            }
        });
        col.addView(back, fullWidthRow(c, gap * 2));

        scroll.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }

    private void refreshOptionLabels() {
        if (animatedButton != null) {
            animatedButton.setText(animatedLabel());
        }
        if (particleButton != null) {
            particleButton.setText(particleLabel());
        }
        if (motionButton != null) {
            motionButton.setText(motionLabel());
        }
        if (twinkleButton != null) {
            twinkleButton.setText(twinkleLabel());
        }
    }

    private static Button b(icyllis.modernui.core.Context c, String label) {
        var btn = new Button(c);
        btn.setText(label);
        btn.setMinHeight(dp(c, 40));
        btn.setGravity(Gravity.CENTER);
        return btn;
    }

    private static LinearLayout.LayoutParams fullWidthRow(icyllis.modernui.core.Context c, int bottomMargin) {
        var p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, bottomMargin);
        return p;
    }

    private static String animatedLabel() {
        return "Animated Background: " + (DupeClient.getVisualSettings().animatedBackground ? "ON" : "OFF");
    }

    private static String particleLabel() {
        return "Particle Count: " + DupeClient.getVisualSettings().particleCount;
    }

    private static String motionLabel() {
        int percent = Math.round(DupeClient.getVisualSettings().motionIntensity * 100.0F);
        return "Motion Intensity: " + percent + "%";
    }

    private static String twinkleLabel() {
        int percent = Math.round(DupeClient.getVisualSettings().twinkleSpeed * 100.0F);
        return "Twinkle Speed: " + percent + "%";
    }

    private static int nextIntOption(int current, int[] options) {
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) {
                return options[(i + 1) % options.length];
            }
        }
        return options[0];
    }

    private static float nextFloatOption(float current, float[] options) {
        for (int i = 0; i < options.length; i++) {
            if (Math.abs(options[i] - current) < 0.001F) {
                return options[(i + 1) % options.length];
            }
        }
        return options[0];
    }
}
