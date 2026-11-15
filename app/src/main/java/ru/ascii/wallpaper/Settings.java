package ru.ascii.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;

final class Settings {
    static final int[] COLORS = {0xff91f5cc, 0xffa7d8ff, 0xffd0b0ff, 0xffffc078, 0xffe6edea, 0xff39ff78};
    static final String[] COLOR_NAMES = {"Мята", "Лёд", "Лаванда", "Янтарь", "Белый", "Matrix"};
    static final String[] FONT_NAMES = {"Terminal / пиксели", "Classic / Cousine", "Mono / системный", "Mono / жирный"};
    final SharedPreferences prefs;
    final DeviceProfile device;
    Settings(Context context) {
        prefs = context.getSharedPreferences("ascii_flow", Context.MODE_PRIVATE);
        device = new DeviceProfile(context);
    }
    int mode() { return Math.max(0, Math.min(5, prefs.getInt("mode", 0))); }
    int colorIndex() { return bounded("color", 5, 0, COLORS.length - 1); }
    int color() { return COLORS[colorIndex()]; }
    int columns() { return bounded("columns", device.columns(), 24, 180); }
    int glyphScale() { return bounded("glyph_scale", 90, 40, 150); }
    int rowSpacing() { return bounded("row_spacing", 100, 70, 180); }
    int objectScale() { return bounded("object_scale", 100, 40, 140); }
    int font() { return bounded("font", 0, 0, FONT_NAMES.length - 1); }
    boolean wireframe() { return bounded("cube_style", 1, 0, 1) == 1; }
    int speed() { return bounded("speed", 30, 0, 180); }
    int brightness() { return bounded("brightness", 80, 5, 100); }
    int fps() { return prefs.getBoolean("eco", false) ? 15 : device.fps(); }
    void set(String key, int value) { prefs.edit().putInt(key, value).apply(); }
    private int bounded(String key, int fallback, int min, int max) {
        return Math.max(min, Math.min(max, prefs.getInt(key, fallback)));
    }
}
