package ru.ascii.wallpaper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.view.Display;
import android.view.WindowManager;

/** Capabilities, not a hardcoded list of phone models, determine defaults. */
final class DeviceProfile {
    private final Context context;
    final boolean modest;
    final int displayFps;
    DeviceProfile(Context context) {
        this.context = context.getApplicationContext();
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        modest = am != null && (am.isLowRamDevice() || am.getMemoryClass() <= 128);
        displayFps = detectMaxFps();
    }
    int columns() {
        int dp = context.getResources().getConfiguration().smallestScreenWidthDp;
        if (dp <= 0) dp = 360;
        return Math.max(36, Math.min(modest ? 64 : 100, Math.round(dp / 6f)));
    }
    private int detectMaxFps() {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return 60;
        Display display = wm.getDefaultDisplay();
        if (display == null) return 60;
        float max = display.getRefreshRate();
        for (Display.Mode mode : display.getSupportedModes()) max = Math.max(max, mode.getRefreshRate());
        return max > 0 && Float.isFinite(max) ? Math.min(1000, Math.max(1, Math.round(max))) : 60;
    }
    boolean powerSaving() {
        PowerManager power = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isPowerSaveMode();
    }
    boolean wallpaperSupported() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER);
    }
}
