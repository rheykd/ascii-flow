package ru.ascii.wallpaper;

/** Normalized origins and monotonic time keep waves stable across resizes. */
public final class RippleField {
    private final double[] x = new double[16], y = new double[16], born = new double[16];
    private int next;
    public RippleField() { clear(); }
    public void clear() { java.util.Arrays.fill(born, -100000); }
    public void touch(double nx, double ny, double now) {
        x[next] = Math.max(0, Math.min(1, nx)); y[next] = Math.max(0, Math.min(1, ny));
        born[next] = now; next = (next + 1) % born.length;
    }
    public void render(AsciiScene scene, double now, double cellAspect, double speed, double lifetime) {
        for (int j = 0; j < born.length; j++) {
            double age = now - born[j];
            if (age < 0 || age >= lifetime) continue;
            double radius = age * speed * scene.cols * .22;
            double thickness = Math.max(1.2, scene.cols * .025);
            for (int cy = 0; cy < scene.rows; cy++) for (int cx = 0; cx < scene.cols; cx++) {
                double d = Math.hypot(cx - x[j] * scene.cols, (cy - y[j] * scene.rows) / cellAspect);
                double band = Math.abs(d - radius);
                if (band > thickness * 2.5) continue;
                float intensity = (float)(Math.exp(-band * band / (thickness * thickness)) * (1 - age / lifetime));
                int i = cy * scene.cols + cx;
                if (intensity > .035 && intensity > scene.light[i]) scene.pixel(i, intensity);
            }
        }
    }
}
