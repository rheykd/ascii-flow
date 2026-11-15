package ru.ascii.wallpaper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

/** Capabilities, not a hardcoded list of phone models, determine defaults. */
final class DeviceProfile {
    private final Context context;
    final boolean modest;
    DeviceProfile(Context context) {
        this.context = context.getApplicationContext();
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        modest = am != null && (am.isLowRamDevice() || am.getMemoryClass() <= 128);
    }
    int columns() {
        int dp = context.getResources().getConfiguration().smallestScreenWidthDp;
        if (dp <= 0) dp = 360;
        return Math.max(36, Math.min(modest ? 64 : 100, Math.round(dp / 6f)));
    }
    int fps() {
        PowerManager power = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return modest || (power != null && power.isPowerSaveMode()) ? 15 : 30;
    }
    boolean wallpaperSupported() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER);
    }
    String description() {
        return Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
            + "\nАвтопрофиль: " + (modest ? "экономный" : "стандартный")
            + " · до " + fps() + " FPS";
    }
}
