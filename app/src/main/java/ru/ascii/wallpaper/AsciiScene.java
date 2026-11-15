package ru.ascii.wallpaper;

import java.util.Arrays;

/** Platform independent, allocation-free frame rendering after resize. */
public final class AsciiScene {
    public static final String[] NAMES = {"Куб", "Волны", "Тор", "Сфера", "Галактика", "Дождь", "Касание", "Фото", "Видео", "Чёрная дыра", "Пульсар", "Звёзды", "Гроза", "Дерево", "Сакура", "Огонь", "Ландшафт", "Город под дождём", "Северное сияние", "Осенняя ветка", "Зима"};
    public static final int[] NATURE_COLORS = {0, 0xff98c6ff, 0xffffeac3, 0xffbd875c, 0xff85df70, 0xffffa6cd, 0xffff6b31, 0xff8da4bd, 0xff64f6c5, 0xffb493ff, 0xffe0efff, 0xffffb548, 0xff477e81};
    public byte[] tones = new byte[0];
    public final EffectOptions options = new EffectOptions();
    private String custom = "";
    private char[] ramp = RAMP;
    public void symbols(String value) {
        if (custom.equals(value)) return;
        custom = value; ramp = value.isEmpty() ? RAMP : value.toCharArray();
    }
    public void pixel(int i, float intensity) {
        tones[i] = 0;
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
        tones = new byte[glyphs.length];
    }

    public void render(int mode, double t, double cellAspect) {
        render(mode, t, cellAspect, false, 1);
    }

    public void render(int mode, double t, double cellAspect, boolean wireframe, double objectScale) {
        Arrays.fill(glyphs, ' '); Arrays.fill(light, 0); Arrays.fill(depth, -Float.MAX_VALUE);
        Arrays.fill(tones, (byte)0);
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
            case 9: blackHole(t); break;
            case 10: pulsar(t); break;
            case 11: stars(t); break;
            case 12: storm(t); break;
            case 13: tree(t); break;
            case 14: sakura(t); break;
            case 15: fire(t); break;
            case 16: landscape(t); break;
            case 17: rainyCity(t); break;
            case 18: aurora(t); break;
            case 19: autumn(t); break;
            case 20: winter(t); break;
            default: if (wireframe) wireCube(); else cube();
        }
    }

    private static double noise(int seed) {
        seed ^= seed >>> 16;
        seed *= 0x7feb352d;
        seed ^= seed >>> 15;
        seed *= 0x846ca68b;
        return ((seed ^ (seed >>> 16)) & 0xffffff) / 16777216.0;
    }

    private void tint(int x, int y, double brightness, char glyph, int tone) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) return;
        put(x, y, 0, brightness, glyph);
        tones[y * cols + x] = (byte)tone;
    }

    private void stroke(double x, double y, double endX, double endY, double brightness, int tone) {
        double dx = endX - x, dy = endY - y;
        int steps = Math.max(1, (int)Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
        char glyph = Math.abs(dx) < Math.abs(dy) * .25 ? '|' : dx * dy < 0 ? '/' : (char)92;
        for (int i = 0; i <= steps; i++) {
            double progress = (double)i / steps;
            tint((int)Math.round(x + dx * progress), (int)Math.round(y + dy * progress), brightness, glyph, tone);
        }
    }

    private void storm(double t) {
        for (int i = 0; i < Math.min(450, cols * rows / 18); i++) {
            int x = (int)(noise(i * 7 + 1) * cols);
            int y = (int)((noise(i * 7 + 2) + t * (.24 + noise(i * 7 + 3) * .4)) % 1 * rows);
            tint(x, y, .2 + .32 * noise(i * 7 + 4), '/', 1);
        }
        double phase = t % 5;
        if (phase > .24) return;
        int seed = (int)(t / 5);
        double x = cols * (.25 + .5 * noise(seed + 170)), y = rows * .03;
        double flash = 1 - phase * 2.5;
        for (int i = 0; i < 9; i++) {
            double nextX = x + (noise(seed * 31 + i) - .5) * cols * .18;
            double nextY = y + rows * .082;
            stroke(x, y, nextX, nextY, flash, 1);
            if (i == 2 || i == 5) {
                double side = (i == 2 ? -1 : 1) * cols * .12;
                stroke(x, y, x + side, y + rows * .09, flash * .65, 1);
                stroke(x + side, y + rows * .09, x + side * 1.25, y + rows * .18, flash * .45, 1);
            }
            x = nextX;
            y = nextY;
        }
    }

    private void tree(double t) {
        double width = Math.min(cols * .76, rows / aspect * .72);
        double height = Math.min(rows * .64, width * aspect * 1.35);
        double center = (cols - 1) * .5, base = rows * .82;
        double crown = base - height * .7;
        double sway = Math.sin(t * .55) * width * .025;
        for (int i = 0; i < Math.max(2, (int)(width * .055)); i++) {
            stroke(center + i - width * .033, base, center + sway * .3, crown, .75, 3);
        }
        for (int branch = 0; branch < 7; branch++) {
            double offset = (branch - 3) / 3.0;
            double x = center + width * offset * .39 + sway;
            double y = crown - height * (.19 - Math.abs(offset) * .24);
            double forkX = center + (x - center) * .56;
            double forkY = crown + height * .08;
            stroke(center, crown + height * .29, forkX, forkY, .73, 3);
            stroke(forkX, forkY, x, y, .63, 3);
            for (int i = 0; i < Math.max(20, (int)(width * height / 17)); i++) {
                int seed = branch * 4099 + i * 5;
                double radius = Math.sqrt(noise(seed + 1)), angle = noise(seed + 2) * Math.PI * 2;
                tint((int)Math.round(x + Math.cos(angle) * radius * width * .16),
                     (int)Math.round(y + Math.sin(angle) * radius * height * .13),
                     .43 + noise(seed + 3) * .53, '+', 4);
            }
        }
        for (int i = 0; i < Math.max(10, cols / 3); i++) {
            double fall = (noise(i * 13 + 41) + t * (.055 + noise(i * 13 + 42) * .065)) % 1;
            double x = center + (noise(i * 13 + 43) - .5) * width * .88
                    + width * .1 * Math.sin(fall * 5 + i) * fall;
            tint((int)x, (int)(crown + fall * (base - crown + height * .14)),
                    .47 + .45 * (1 - fall), '~', 4);
        }
    }

    // Диагональная ветка входит справа, а тонкие побеги расходятся в обе стороны.
    private static final double[][] SAKURA_BRANCHES = {
        {1.08,.24, .91,.32, .80,.43, .67,.51, .55,.64, .40,.71, .25,.75, -.06,.87},
        {.88,.34, .81,.25, .73,.17, .64,.13},
        {.80,.43, .69,.39, .57,.34, .42,.36, .33,.31},
        {.67,.51, .59,.55, .48,.52, .42,.57},
        {.55,.64, .61,.70, .62,.78, .57,.87},
        {.83,.40, .85,.51, .91,.60, .86,.69, .81,.75},
        {.40,.71, .36,.64, .29,.62, .23,.66},
        {.25,.75, .19,.80, .13,.90}
    };

    // Центр цветка, диаметр и поворот. Несколько цветков образуют каждую гроздь.
    private static final double[][] SAKURA_FLOWERS = {
        {.65,.13,.075,.1}, {.75,.18,.092,.5}, {.82,.24,.063,.8},
        {.36,.32,.084,.2}, {.43,.36,.104,.7}, {.50,.32,.123,.4},
        {.56,.37,.112,.1}, {.62,.40,.085,.9}, {.70,.41,.065,.3},
        {.43,.57,.081,.8}, {.49,.53,.102,.4},
        {.90,.55,.095,.2}, {.92,.62,.078,.4}, {.84,.69,.104,.7}, {.81,.75,.067,.2},
        {.59,.70,.100,.1}, {.62,.77,.126,.6}, {.57,.85,.093,.8},
        {.28,.72,.087,.3}, {.35,.69,.107,.9}, {.32,.76,.097,.4},
        {.03,.85,.114,.4}, {.10,.80,.111,.9}, {.16,.83,.081,.2}
    };
    private static final double[][] SAKURA_BUDS = {
        {.62,.10}, {.77,.12}, {.91,.25}, {.33,.30}, {.37,.27}, {.65,.45},
        {.39,.58}, {.51,.61}, {.93,.67}, {.86,.76}, {.63,.86},
        {.53,.91}, {.26,.64}, {.21,.66}, {.18,.87}, {.12,.91}
    };

    private double sakuraSway(double x, double y, double t) {
        return (1.04 - x) * (.023 * Math.sin(t * 1.15) + .008 * Math.sin(t * 1.7 + y * 3));
    }

    private void sakura(double t) {
        double width = Math.min(cols * .98, rows / aspect * .94);
        double height = width * aspect * .94;
        double left = (cols - width) * .5;
        double top = rows * .43 - height * .5;
        for (int branch = 0; branch < SAKURA_BRANCHES.length; branch++) {
            double[] points = SAKURA_BRANCHES[branch];
            for (int i = 2; i < points.length; i += 2) {
                double start = points[i - 2], end = points[i];
                double y = top + height * (points[i - 1] + sakuraSway(start, points[i - 1], t));
                double endY = top + height * (points[i + 1] + sakuraSway(end, points[i + 1], t));
                double x = left + width * start, endX = left + width * end;
                stroke(x, y, endX, endY, branch == 0 ? .82 : .55, 3);
                if (branch == 0) {
                    int thickness = Math.max(1, (int)(width * .047 * (1 - start + 1.0) * start));
                    for (int j = 1; j <= thickness; j++) {
                        stroke(x, y + j * aspect, endX, endY + j * aspect * .83, .62, 3);
                    }
                }
            }
        }
        for (double[] bud : SAKURA_BUDS) {
            int x = (int)Math.round(left + width * bud[0]);
            int y = (int)Math.round(top + height * (bud[1] + sakuraSway(bud[0], bud[1], t)));
            tint(x, y, .76, 'o', 5);
        }
        for (int i = 0; i < SAKURA_FLOWERS.length; i++) {
            double[] flower = SAKURA_FLOWERS[i];
            double x = left + width * flower[0];
            double y = top + height * (flower[1] + sakuraSway(flower[0], flower[1], t));
            double radius = Math.max(1.35, width * flower[2] * .5);
            double rotation = flower[3] + .14 * Math.sin(t * 1.65 + i * .68);
            sakuraFlower(x, y, radius, rotation);
        }
        for (int i = 0; i < Math.max(14, cols / 3); i++) {
            double[] source = SAKURA_FLOWERS[(i * 11) % SAKURA_FLOWERS.length];
            double lifetime = 5.4 + noise(i * 31 + 8) * 4;
            double fall = (noise(i * 31 + 7) + t / lifetime) % 1;
            double drift = (.09 + .05 * noise(i * 31 + 9)) * fall + Math.sin(fall * 8 + i) * fall * .06;
            double swayAtRelease = sakuraSway(source[0], source[1], t - fall * lifetime);
            int x = (int)Math.round(left + width * (source[0] + drift));
            int y = (int)Math.round(top + height * (source[1] + swayAtRelease) + fall * rows * .47);
            double fade = Math.min(1, (1 - fall) * 5);
            if (fade > .1) tint(x, y, (.79 - fall * .37) * fade, i % 3 == 0 ? ',' : '.', 5);
        }
    }

    private void sakuraFlower(double x, double y, double radius, double rotation) {
        for (int petal = 0; petal < 5; petal++) {
            double angle = rotation + petal * Math.PI * .4;
            double petalX = x + Math.cos(angle) * radius * .55;
            double petalY = y + Math.sin(angle) * radius * .55 * aspect;
            double outer = radius * .48;
            for (int row = (int)Math.floor(petalY - outer * aspect); row <= Math.ceil(petalY + outer * aspect); row++) {
                for (int col = (int)Math.floor(petalX - outer); col <= Math.ceil(petalX + outer); col++) {
                    double dx = col - petalX, dy = (row - petalY) / aspect;
                    double distance = (dx * dx + dy * dy) / (outer * outer);
                    if (distance <= 1) tint(col, row, .97 - distance * .25, distance < .42 ? 'o' : '+', 5);
                }
            }
            tint((int)Math.round(petalX), (int)Math.round(petalY), .91, 'o', 5);
        }
        tint((int)Math.round(x), (int)Math.round(y), .65, '*', 5);
    }

    private void rainyCity(double t) {
        int street = (int)(rows * .72);
        for (int layer = 0; layer < 2; layer++) {
            int x = -layer;
            for (int building = 0; x < cols; building++) {
                int seed = building * 121 + layer * 725;
                int width = Math.max(3, (int)(cols * (.08 + noise(seed) * .11)));
                int roof = street - (int)(rows * (.16 + layer * .07 + noise(seed + 1) * .25));
                for (int y = roof; y < street; y++) for (int j = 0; j < width - 1; j++) {
                    boolean window = layer == 1 && j % 3 == 1 && (y - roof) % 3 == 2
                            && noise(seed + y * 17 + j) > .42;
                    tint(x + j, y, window ? .88 : layer == 0 ? .19 : .34,
                         window ? '+' : y == roof ? '-' : ':', window ? 2 : 7);
                    if (window) {
                        int reflected = street + 1 + (street - y) / 2;
                        int shimmer = (int)Math.round(Math.sin(t * 2.2 + y + j));
                        tint(x + j + shimmer, reflected, .32 + .12 * Math.sin(t + y), '-', 11);
                    }
                }
                x += width;
            }
        }
        for (int x = 0; x < cols; x++) tint(x, street, .32, '_', 1);
        precipitation(t, false);
    }

    private void aurora(double t) {
        int horizon = (int)(rows * .74);
        for (int i = 0; i < cols; i++) {
            int x = (int)(noise(i * 23 + 300) * cols), y = (int)(noise(i * 23 + 301) * horizon);
            tint(x, y, .35 + noise(i * 23 + 302) * .42, '.', 10);
        }
        for (int band = 0; band < 3; band++) for (int x = 0; x < cols; x++) {
            double u = x / (double)cols;
            double wave = Math.sin(u * 7 + t * .47 + band * .8) * .075
                    + Math.sin(u * 15 - t * .28) * .024;
            int edge = (int)(rows * (.30 + band * .082 + wave));
            int length = Math.max(3, (int)(rows * (.10 + .045 * Math.sin(u * 12 + t * .5 + band))));
            double glow = .57 + .24 * Math.sin(u * 10 + t * .8 + band);
            for (int dy = 0; dy < length; dy++) {
                double light = (1 - dy / (double)length) * glow;
                if (light > .15 && noise(x * 181 + dy * 19 + band * 133) < .29 + light) {
                    tint(x, edge - dy, light, (char)0, band == 1 ? 9 : 8);
                }
            }
        }
        for (int x = 0; x < cols; x++) {
            double u = x / (double)cols;
            int ridge = horizon + (int)(rows * (.035 * Math.sin(u * 16) + .024 * Math.cos(u * 28)));
            tint(x, ridge, .37, '^', 12);
        }
    }

    private void autumn(double t) {
        double width = Math.min(cols * .92, rows / aspect * 1.35);
        double height = width * aspect * .51;
        double left = (cols - width) * .5, top = rows * .34 - height * .45;
        double sway = Math.sin(t * .67) * height * .065;
        for (int segment = 0; segment < 8; segment++) {
            double from = segment / 8.0, to = (segment + 1) / 8.0;
            stroke(left + width * from, top + height * (.23 + .6 * from * from) + sway * from,
                   left + width * to, top + height * (.23 + .6 * to * to) + sway * to, .85, 3);
        }
        for (int i = 0; i < 11; i++) {
            double u = .08 + i * .083;
            double x = left + width * u, y = top + height * (.23 + .6 * u * u) + sway * u;
            double side = i % 2 == 0 ? -1 : 1;
            double tipX = x + width * (.025 + noise(i + 199) * .055);
            double tipY = y + side * height * (.15 + noise(i + 201) * .20);
            stroke(x, y, tipX, tipY, .68, 3);
            maple((int)Math.round(tipX), (int)Math.round(tipY), 1, i % 3 == 0 ? 6 : 11, .87);
        }
        for (int i = 0; i < Math.max(7, cols / 6); i++) {
            double fall = (noise(i * 29 + 12) + t * (.065 + noise(i * 29 + 13) * .046)) % 1;
            double u = .1 + .73 * noise(i * 29 + 14);
            double drift = .19 * fall + .045 * Math.sin(fall * 13 + i) * fall;
            int x = (int)(left + width * (u + drift));
            int y = (int)(top + height * (.23 + .6 * u * u) + fall * rows * .65);
            maple(x, y, Math.cos(fall * 13 + i), i % 3 == 0 ? 6 : 11, .76 - fall * .26);
        }
    }

    private void maple(int x, int y, double face, int tone, double brightness) {
        tint(x, y, brightness, '*', tone);
        tint(x, y + 1, brightness * .65, '|', tone);
        if (Math.abs(face) < .4) return;
        tint(x, y - 1, brightness, '^', tone);
        tint(x - 1, y, brightness * .89, (char)92, tone);
        tint(x + 1, y, brightness * .89, '/', tone);
    }

    private void winter(double t) {
        for (int x = 0; x < cols; x++) {
            double u = x / (double)cols;
            int drift = (int)(rows * (.79 + .04 * Math.cos(u * 10) + .02 * Math.sin(u * 23)));
            tint(x, drift, .52, '_', 10);
            for (int y = drift + 1; y < rows; y++) {
                if (noise(y * 139 + x * 271) > .76) tint(x, y, .24, '.', 10);
            }
        }
        for (int layer = 0; layer < 2; layer++) {
            int count = layer == 0 ? 8 : 5;
            for (int i = 0; i < count; i++) {
                int seed = layer * 300 + i * 37;
                int x = (int)((i + .22 + noise(seed + 5) * .52) * cols / count);
                int foot = (int)(rows * (.72 + layer * .075 + noise(seed + 1) * .085));
                int height = Math.max(6, (int)Math.min(rows * (.16 + .092 * layer + noise(seed + 2) * .13),
                        cols * aspect * (.24 + .2 * layer)));
                fir(x, foot, height, layer == 0 ? .65 : 1);
            }
        }
        precipitation(t, true);
    }

    private void fir(int x, int foot, int height, double brightness) {
        for (int tier = 0; tier < 3; tier++) {
            int tip = foot - height + height * tier / 5;
            int length = Math.max(3, height * (4 + tier) / 10);
            for (int row = 0; row < length; row++) {
                int span = (int)(row / aspect * (.30 + tier * .07));
                for (int dx = -span; dx <= span; dx++) {
                    boolean snow = row < 2 || Math.abs(dx) >= span - 1 || row == length - 2;
                    tint(x + dx, tip + row, brightness * (snow ? .77 : .51),
                         snow ? (dx < 0 ? '/' : dx > 0 ? (char)92 : '*') : ':', snow ? 10 : 12);
                }
            }
        }
        stroke(x, foot - height * .12, x, foot, brightness * .61, 3);
    }

    private void precipitation(double t, boolean snow) {
        int count = Math.min(snow ? 150 : 360, Math.max(16, cols * rows / (snow ? 65 : 22)));
        for (int i = 0; i < count; i++) {
            int seed = i * 17 + 393;
            double depth = noise(seed + 1);
            double progress = (noise(seed + 2) + t * (snow ? .045 + depth * .09 : .32 + depth * .58)) % 1;
            double sway = snow ? .028 * Math.sin(t * .8 + i) : -.13 * progress;
            double horizontal = (1 + noise(seed + 3) + sway) % 1;
            int x = (int)(horizontal * cols), y = (int)(progress * rows);
            tint(x, y, .28 + depth * .42, snow ? depth > .79 ? '*' : '.' : '/', snow ? 10 : 1);
        }
    }

    private void fire(double t) {
        for (int x = 0; x < cols; x++) {
            double horizontal = x / (double)cols;
            double flame = .36 + .11 * Math.sin(horizontal * 28 + t * 2.2)
                    + .09 * Math.sin(horizontal * 51 - t * 1.4);
            for (int y = rows - 1; y >= Math.max(0, rows * (1 - flame)); y--) {
                double rise = (rows - 1 - y) / (rows * flame);
                double plume = 1 - rise + .16 * Math.sin(horizontal * 54 + rise * 16 - t * 5);
                if (plume > .16) tint(x, y, plume, (char)0, plume > .7 ? 2 : 6);
            }
        }
    }

    private void landscape(double t) {
        for (int layer = 0; layer < 3; layer++) {
            double distance = layer + 1;
            for (int x = 0; x < cols; x++) {
                double world = x * 9.0 / cols + t * (.09 + layer * .12);
                double ridge = Math.sin(world + layer) * .095 + Math.sin(world * 2.3 + layer) * .045;
                int summit = (int)(rows * (.43 + layer * .13 + ridge));
                for (int y = summit; y < rows; y++) {
                    double slope = (y - summit) / (double)rows;
                    if (y == summit || noise(x * 171 + y * 41 + layer) > .42) {
                        double shade = .63 - slope * .68 + .15 * Math.sin(world * 2 + slope * 25);
                        tint(x, y, shade / (1 + (3 - distance) * .25), (char)0, layer == 2 ? 4 : 7);
                    } else {
                        int index = y * cols + x;
                        glyphs[index] = ' ';
                        light[index] = 0;
                    }
                }
            }
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
        // Выключенное вращение замораживает и поворот, и бег рукавов.
        double spin = options.galaxySpin ? t * options.galaxyRotation / 100.0 : 0;
        double rotation = spin * .08;
        for (int i = 0; i < 2200; i++) {
            double r = Math.sqrt((i + .5) / 2200.0) * 1.25;
            double theta = i * 2.3999632297 + r * 5 + rotation;
            double arm = Math.pow(.5 + .5 * Math.sin(theta * options.galaxyArms - r * 7 + spin * .13), 5);
            if (arm < .12) continue;
            double x = Math.cos(theta) * r, y = Math.sin(theta) * r * .72;
            put((int)(cols * .5 + x * scale), (int)(rows * .5 + y * scale * aspect), arm,
                (.28 + .7 * arm) * (1 - r * .25), (char)0);
        }
        put(cols / 2, rows / 2, 2, 1, '@');
    }

    private void blackHole(double t) {
        int cx = cols / 2, cy = rows / 2;
        // Аккреционный диск — наклонные орбиты вокруг дыры
        for (int i = 0; i < 2000; i++) {
            double r = .15 + Math.sqrt((i + .5) / 2000.0) * 1.1;
            double theta = i * 2.3999632297 + t * .18 * (1.0 / (r + .1));
            // Наклон диска
            double x3d = Math.cos(theta) * r;
            double y3d = Math.sin(theta) * r * .35;
            double z3d = Math.sin(theta) * r * .6;
            // Эффект линзирования вблизи центра
            double dist = Math.hypot(x3d, y3d);
            double lensing = dist > .18 ? 1.0 : (1.0 + (.18 - dist) * 3.0);
            double sx = x3d * lensing, sy = (y3d + z3d * .28) * lensing;
            double brightness = (.35 + .55 * Math.pow(1.0 - Math.min(1, r), 1.5)) * (r > .18 ? 1.0 : 0.0);
            if (brightness > .08)
                put((int)(cx + sx * scale), (int)(cy + sy * scale * aspect), r, brightness, (char)0);
        }
        // Фотонное кольцо
        for (double a = 0; a < Math.PI * 2; a += .08) {
            double rx = Math.cos(a) * .14, ry = Math.sin(a) * .05;
            put((int)(cx + rx * scale), (int)(cy + ry * scale * aspect), 1.5, .9, '+');
        }
        // Ядро — чёрная дыра
        put(cx, cy, 2, 1, '@');
    }

    private void pulsar(double t) {
        int cx = cols / 2, cy = rows / 2;
        // Два вращающихся луча-маяка
        for (int beam = 0; beam < 2; beam++) {
            double beamAngle = t * 1.4 + beam * Math.PI;
            int beamLen = (int)(Math.min(cols, rows / aspect) * .45);
            for (int r = 2; r < beamLen; r++) {
                // Ширина луча: в центре широкий, к краям тонкий
                double spreadR = 1.0 + r * .04;
                for (double off = -spreadR; off <= spreadR; off += .5) {
                    double ang = beamAngle + off * .06 / Math.max(1, r * .1);
                    double bx = Math.cos(ang) * r;
                    double by = Math.sin(ang) * r * .6;
                    double brightness = (.9 - r * .012) * (1.0 - Math.abs(off) / (spreadR + 1));
                    if (brightness > .08)
                        put((int)(cx + bx), (int)(cy + by * aspect), 0, brightness, (char)0);
                }
            }
        }
        // Концентрические кольца — периодические выбросы
        for (int ring = 0; ring < 4; ring++) {
            double ringR = ((t * .6 + ring * .9) % 2.5) * Math.min(cols, rows / aspect) * .22;
            if (ringR < 1.5) continue;
            for (double a = 0; a < Math.PI * 2; a += .07) {
                double bx = Math.cos(a) * ringR, by = Math.sin(a) * ringR * .6;
                double brightness = .55 * (1.0 - ringR / (Math.min(cols, rows / aspect) * .55));
                if (brightness > .1)
                    put((int)(cx + bx), (int)(cy + by * aspect), 0, brightness, (char)0);
            }
        }
        // Пульсирующее ядро
        double corePulse = .7 + .3 * Math.sin(t * Math.PI * 1.4);
        put(cx, cy, 2, corePulse, '*');
        put(cx - 1, cy, 1.9, corePulse * .7, (char)0);
        put(cx + 1, cy, 1.9, corePulse * .7, (char)0);
        put(cx, cy - 1, 1.9, corePulse * .6, (char)0);
        put(cx, cy + 1, 1.9, corePulse * .6, (char)0);
    }

    private void stars(double t) {
        // Статичные звёзды с опциональной анимацией
        int seed = 42;
        int starCount = cols * rows / 12;
        for (int i = 0; i < starCount; i++) {
            seed = (seed * 1103515245 + 12345) & 0x7fffffff;
            int x = seed % cols;
            seed = (seed * 1103515245 + 12345) & 0x7fffffff;
            int y = seed % rows;
            seed = (seed * 1103515245 + 12345) & 0x7fffffff;
            double baseBrightness = .3 + (seed % 100) / 150.0;
            double brightness = baseBrightness;
            if (options.starsAnimation) {
                double phase = (seed % 100) / 30.0;
                brightness = baseBrightness * (.7 + .3 * Math.sin(t * .5 + phase));
            }
            put(x, y, 0, brightness, (char)0);
        }
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
