package ru.ascii.wallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.content.Context;

final class AsciiPainter {
    final AsciiScene scene = new AsciiScene();
    private final GridGeometry grid = new GridGeometry();
    private final GlyphRenderer glyphs;
    private double time;
    private long last;
    AsciiPainter(Context context) { glyphs = new GlyphRenderer(context); }
    void pause() { last = 0; }
    void draw(Canvas canvas, int width, int height, float density, Settings settings, boolean preview) {
        long now = android.os.SystemClock.uptimeMillis();
        if (last != 0) time += Math.min(.1, (now - last) / 1000.0) * settings.speed() / 30.0;
        last = now;
        canvas.drawColor(Color.BLACK);
        grid.configure(width, height, settings.columns(), settings.rowSpacing());
        scene.resize(grid.cols, grid.rows);
        scene.render(settings.mode(), time, grid.cellW / grid.cellH, settings.wireframe(), settings.objectScale() / 100.0);
        glyphs.configure(settings.font(), settings.color(), grid.cellW * settings.glyphScale() / 100f);
        float alphaScale = 255 * settings.brightness() / 100f;
        for (int y = 0; y < grid.rows; y++) for (int x = 0; x < grid.cols; x++) {
            int i = y * grid.cols + x;
            if (scene.glyphs[i] == ' ') continue;
            glyphs.draw(canvas, scene.glyphs, i, grid.left + (x + .5f) * grid.cellW,
                    grid.top + (y + .5f) * grid.cellH, (int)(scene.light[i] * alphaScale));
        }
    }
}
