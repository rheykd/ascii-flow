package ru.ascii.wallpaper;

import java.util.Arrays;

/** Platform independent, allocation-free frame rendering after resize. */
public final class AsciiScene {
    public static final String[] NAMES = {"Куб", "Волны", "Тор", "Сфера", "Галактика", "Дождь", "Касание", "Фото", "Видео"};
    public final EffectOptions options = new EffectOptions();
    private String custom = "";
    private char[] ramp = RAMP;
    public void symbols(String value) {
        if (custom.equals(value)) return;
        custom = value; ramp = value.isEmpty() ? RAMP : value.toCharArray();
    }
    public void pixel(int i, float intensity) {
        light[i] = Math.max(0, Math.min(1, intensity));
        glyphs[i] = light[i] < .035f ? ' ' : ramp[(int)(light[i] * (ramp.length - 1))];
    }
    private static final char[] RAMP = ".,:;irsXA253hMHGS#9B&@".toCharArray();
    private static final char[] RAIN = "0123456789<>/{}[]+*:=ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public int cols, rows;
    public char[] glyphs = new char[0];
    public float[] light = new float[0];
    private float[] depth = new float[0];
    private double ca, sa, cb, sb, scale, aspect;
    private final double[] vx = new double[8], vy = new double[8], vz = new double[8];

    public void resize(int cols, int rows) {
        cols = Math.max(8, cols); rows = Math.max(8, rows);
        if (this.cols == cols && this.rows == rows) return;
        this.cols = cols; this.rows = rows;
        glyphs = new char[cols * rows]; light = new float[glyphs.length]; depth = new float[glyphs.length];
    }

    public void render(int mode, double t, double cellAspect) {
        render(mode, t, cellAspect, false, 1);
    }

    public void render(int mode, double t, double cellAspect, boolean wireframe, double objectScale) {
        Arrays.fill(glyphs, ' '); Arrays.fill(light, 0); Arrays.fill(depth, -Float.MAX_VALUE);
        aspect = cellAspect;
        scale = Math.min(cols, rows / aspect) * .32 * objectScale;
        double a = t * .16 * options.rotationX / 100.0 + .42, b = t * .11 * options.rotationY / 100.0 + .62;
        ca = Math.cos(a); sa = Math.sin(a); cb = Math.cos(b); sb = Math.sin(b);
        switch (mode) {
            case 1: waves(t); break;
            case 2: torus(t); break;
            case 3: sphere(t); break;
            case 4: galaxy(t); break;
            case 5: rain(t); break;
            case 6: case 7: case 8: break;
            default: if (wireframe) wireCube(); else cube();
        }
    }

    private void wireCube() {
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -.72 : .72;
            double y = (i & 2) == 0 ? -.72 : .72;
            double z = (i & 4) == 0 ? -.72 : .72;
            double ry = y * ca - z * sa, rz = y * sa + z * ca;
            double rx = x * cb + rz * sb; rz = -x * sb + rz * cb;
            double p = 4.2 / (4.2 - rz);
            vx[i] = cols * .5 + rx * scale * p;
            vy[i] = rows * .5 + ry * scale * aspect * p;
            vz[i] = rz;
        }
        // Vertices differing by one bit form precisely the twelve cube edges.
        for (int i = 0; i < 8; i++) for (int bit = 1; bit <= 4; bit <<= 1) {
            int j = i ^ bit;
            if (j <= i) continue;
            int x0 = (int)Math.round(vx[i]), y0 = (int)Math.round(vy[i]);
            int x1 = (int)Math.round(vx[j]), y1 = (int)Math.round(vy[j]);
            int dx = x1 - x0, dy = y1 - y0;
            int steps = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
            char ch = Math.abs(dy) < Math.abs(dx) * .28 ? '-' :
                Math.abs(dx) < Math.abs(dy) * .28 ? '|' : dx * dy >= 0 ? '\\' : '/';
            for (int k = 0; k <= steps; k++) {
                double f = (double)k / steps, z = vz[i] + (vz[j] - vz[i]) * f;
                put((int)Math.round(x0 + dx * f), (int)Math.round(y0 + dy * f), z,
                    .72 + z * .16, k == 0 || k == steps ? '+' : ch);
            }
        }
    }

    private void put(int x, int y, double z, double brightness, char glyph) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) return;
        int i = y * cols + x;
        if (z < depth[i]) return;
        depth[i] = (float) z;
        brightness = Math.max(.12, Math.min(1, brightness));
        light[i] = (float) brightness;
        glyphs[i] = glyph == 0 || !custom.isEmpty() ? ramp[(int)(brightness * (ramp.length - 1))] : glyph;
    }

    private void project(double x, double y, double z, double nx, double ny, double nz, boolean edge) {
        double ry = y * ca - z * sa, rz = y * sa + z * ca;
        double rx = x * cb + rz * sb; rz = -x * sb + rz * cb;
        double nry = ny * ca - nz * sa, nrz = ny * sa + nz * ca;
        double nrx = nx * cb + nrz * sb; nrz = -nx * sb + nrz * cb;
        double perspective = 4.2 / (4.2 - rz);
        double brightness = edge ? 1 : .28 + .65 * Math.max(0, -nrx * .35 - nry * .5 + nrz * .79);
        put((int)Math.round(cols * .5 + rx * scale * perspective),
            (int)Math.round(rows * .5 + ry * scale * aspect * perspective), rz, brightness, edge ? '+' : (char)0);
    }

    private void cube() {
        // Fill all six faces with a z-buffer; bright borders define the silhouette.
        int samples = Math.max(26, cols);
        for (int face = 0; face < 6; face++) {
            int axis = face / 2; double sign = face % 2 == 0 ? -1 : 1;
            for (int i = 0; i <= samples; i++) for (int j = 0; j <= samples; j++) {
                double u = (2.0 * i / samples - 1) * .72, v = (2.0 * j / samples - 1) * .72;
                boolean edge = i == 0 || j == 0 || i == samples || j == samples;
                if (axis == 0) project(sign * .72, u, v, sign, 0, 0, edge);
                if (axis == 1) project(u, sign * .72, v, 0, sign, 0, edge);
                if (axis == 2) project(u, v, sign * .72, 0, 0, sign, edge);
            }
        }
    }

    private void torus(double t) {
        for (double u = 0; u < Math.PI * 2; u += .045) for (double v = 0; v < Math.PI * 2; v += .085) {
            double cu = Math.cos(u), su = Math.sin(u), cv = Math.cos(v), sv = Math.sin(v);
            double tube = .31 * options.torusThickness / 100.0;
            double r = .78 + tube * cv;
            project(r * cu, r * su, tube * sv, cv * cu, cv * su, sv, false);
        }
    }

    private void sphere(double t) {
        for (double u = 0; u < Math.PI; u += .045) for (double v = 0; v < 2 * Math.PI; v += .055) {
            double x = Math.sin(u) * Math.cos(v), y = Math.cos(u), z = Math.sin(u) * Math.sin(v);
            double r = .94 + .055 * options.spherePulse / 100.0 * Math.sin(6 * u + 3 * v + t * .6);
            boolean line = Math.abs(Math.sin(u * 9)) < .16 || Math.abs(Math.sin(v * 7)) < .12;
            project(x * r, y * r, z * r, x, y, z, line);
        }
    }

    private void waves(double t) {
        for (int y = 0; y < rows; y++) for (int x = 0; x < cols; x++) {
            double xx = (x - cols * .5) / cols * 9;
            double yy = (y - rows * .5) / cols / aspect * 9;
            xx *= options.waveFrequency / 100.0; yy *= options.waveFrequency / 100.0;
            double d = Math.hypot(xx, yy);
            double wave = Math.sin(xx * 1.6 + t * .6 + Math.sin(yy * .8 - t * .3))
                + Math.sin(yy * 1.5 - t * .5) + .5 * Math.cos(d * 2.3 - t * .7);
            double value = Math.max(0, Math.min(1, (wave * options.waveStrength / 100.0 + 2.5) / 5));
            if (value > .22) put(x, y, 0, value * .85, (char)0);
        }
    }

    private void galaxy(double t) {
        for (int i = 0; i < 2200; i++) {
            double r = Math.sqrt((i + .5) / 2200.0) * 1.25;
            double theta = i * 2.3999632297 + r * 5 + t * .08;
            double arm = Math.pow(.5 + .5 * Math.sin(theta * options.galaxyArms - r * 7 + t * .13), 5);
            if (arm < .12) continue;
            double x = Math.cos(theta) * r, y = Math.sin(theta) * r * .72;
            put((int)(cols * .5 + x * scale), (int)(rows * .5 + y * scale * aspect), arm,
                (.28 + .7 * arm) * (1 - r * .25), (char)0);
        }
        put(cols / 2, rows / 2, 2, 1, '@');
    }

    private void rain(double t) {
        for (int x = 0; x < cols; x += Math.max(1, options.rainSpacing)) {
            int seed = (x * 1103515245 + 12345) & 0x7fffffff;
            double speed = 2.5 + seed % 500 / 90.0;
            double head = (t * speed + seed % 197) % (rows + 25);
            int tail = Math.max(2, (8 + seed % 17) * options.rainLength / 100);
            for (int k = 0; k < tail; k++) {
                int y = (int)head - k;
                int code = Math.floorMod(seed + y * 31 + (int)(t * 2), RAIN.length);
                put(x, y, 0, Math.pow(1.0 - (double)k / tail, 1.5), RAIN[code]);
            }
        }
    }
}
