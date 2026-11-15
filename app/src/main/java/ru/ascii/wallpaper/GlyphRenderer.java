package ru.ascii.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.io.IOException;
import java.io.InputStream;

final class GlyphRenderer {
    private static Typeface classic;
    private static Bitmap terminal;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private int font = -1, color;
    private float width, height, baseline;
    private final Context context;

    GlyphRenderer(Context context) { this.context = context.getApplicationContext(); }
    static Typeface classic(Context context) {
        if (classic == null) classic = Typeface.createFromAsset(context.getAssets(), "fonts/Cousine-Regular.ttf");
        return classic;
    }
    private Bitmap terminal() {
        if (terminal != null) return terminal;
        byte[] data = new byte[1024];
        try (InputStream stream = context.getAssets().open("fonts/terminal8x8.bin")) {
            int n = 0, count;
            while (n < data.length && (count = stream.read(data, n, data.length - n)) != -1) n += count;
            if (n != data.length) throw new IOException("Incomplete terminal font");
        } catch (IOException e) { throw new IllegalStateException("Missing bundled font", e); }
        terminal = Bitmap.createBitmap(128, 64, Bitmap.Config.ARGB_8888);
        for (int c = 0; c < 128; c++) for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            if ((data[c * 8 + y] & (1 << x)) != 0)
                terminal.setPixel(c % 16 * 8 + x, c / 16 * 8 + y, Color.WHITE);
        }
        return terminal;
    }
    void configure(int font, int color, float targetWidth) {
        if (this.font != font) {
            this.font = font;
            paint.setTypeface(font == 1 ? classic(context) : Typeface.create(Typeface.MONOSPACE, font == 3 ? Typeface.BOLD : Typeface.NORMAL));
            paint.setAntiAlias(font != 0); paint.setFilterBitmap(false);
            if (font == 0) terminal();
            this.color = 0;
        }
        if (this.color != color) {
            this.color = color;
            paint.setColor(color);
            paint.setColorFilter(font == 0 ? new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN) : null);
        }
        width = targetWidth;
        if (font == 0) { height = width * 2; baseline = 0; }
        else {
            paint.setTextSize(100);
            paint.setTextSize(100 * width / paint.measureText("M"));
            height = paint.descent() - paint.ascent();
            baseline = -paint.ascent();
        }
    }
    void draw(Canvas canvas, char[] glyphs, int index, float centerX, float centerY, int alpha) {
        paint.setAlpha(alpha);
        float x = centerX - width * .5f, y = centerY - height * .5f;
        if (font == 0) {
            int c = glyphs[index] & 127;
            src.set(c % 16 * 8, c / 16 * 8, c % 16 * 8 + 8, c / 16 * 8 + 8);
            dst.set(x, y, x + width, y + height);
            canvas.drawBitmap(terminal, src, dst, paint);
        } else canvas.drawText(glyphs, index, 1, x, y + baseline, paint);
    }
}
