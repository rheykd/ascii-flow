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
    int mode() { return bounded("mode", 0, 0, 11); }
    int parameter(String key, int fallback, int min, int max) { return bounded(key, fallback, min, max); }
    String symbols() { return prefs.getString("symbols", ""); }
    boolean touchEnabled() { return prefs.getBoolean("touch", true); }
    boolean blackHoleWaves() { return prefs.getBoolean("blackhole_waves", true); }
    boolean ripplesActive() { return mode() == 9 ? blackHoleWaves() : mode() == 6 || touchEnabled(); }
    boolean lockClock() { return prefs.getBoolean("lock_clock",false); }
    int lockClockY() { return bounded("lock_clock_y", 50, 0, 100); }
    String mediaUri() { return prefs.getString(mode() == 8 ? "video_uri" : "photo_uri", ""); }
    void effects(EffectOptions o) {
        o.rotationX = bounded("rotation_x",100,0,200); o.rotationY = bounded("rotation_y",100,0,200);
        o.waveFrequency = bounded("wave_frequency",100,25,250); o.waveStrength = bounded("wave_strength",100,10,200);
        o.torusThickness = bounded("torus_thickness",100,30,160); o.spherePulse = bounded("sphere_pulse",100,0,250);
        o.galaxyArms = bounded("galaxy_arms",3,1,8); o.galaxyRotation = bounded("galaxy_rotation",100,0,200);
        o.rainLength = bounded("rain_length",100,25,250); o.rainSpacing = bounded("rain_spacing",2,1,6);
        o.starsAnimation = prefs.getBoolean("stars_animation", false);
        o.galaxySpin = prefs.getBoolean("galaxy_spin", true);
    }
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
