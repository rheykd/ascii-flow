package ru.ascii.wallpaper;

public final class LockClockPolicy {
    public static boolean show(boolean enabled, boolean preview, int sdk, int flags, boolean keyguard) {
        if (!enabled || preview) return false;
        // A dedicated lock engine belongs to the lock screen even during its transitions.
        if (sdk >= 34 && flags == 2) return true;
        // A shared engine, older Android, and OEMs reporting system-only flags need keyguard state.
        return keyguard;
    }
    public static String time(long millis, java.util.TimeZone zone) {
        java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("HHmm",java.util.Locale.ROOT);
        f.setTimeZone(zone);return f.format(new java.util.Date(millis));
    }
}
