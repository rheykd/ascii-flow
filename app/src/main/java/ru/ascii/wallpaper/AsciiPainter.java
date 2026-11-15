package ru.ascii.wallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.content.Context;

final class AsciiPainter {
    final AsciiScene scene = new AsciiScene();
    private final GridGeometry grid = new GridGeometry();
    private final GlyphRenderer glyphs;
    private final RippleField ripples = new RippleField();
    private final MediaSource media;
    private final android.graphics.Paint messagePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private int previousMode = -1;
    private String lastMedia = "";
    private double realTime;
    private double time;
    private long last;
    AsciiPainter(Context context) { glyphs = new GlyphRenderer(context); media = new MediaSource(context); }
    void pause() { last = 0; media.pause(); }
    void close() { media.close(); }
    void touch(float x, float y, int width, int height, Settings settings) {
        if (previousMode != settings.mode()) { ripples.clear(); previousMode = settings.mode(); time=0; media.pause(); }
        if (settings.mode() == 6 || settings.touchEnabled()) ripples.touch(x/Math.max(1,width), y/Math.max(1,height), realTime);
    }
    void draw(Canvas canvas, int width, int height, float density, Settings settings, boolean preview) {
        long now = android.os.SystemClock.uptimeMillis();
        if (last != 0) {
            double delta = Math.min(.25, (now - last) / 1000.0);
            time += delta * settings.speed() / 30.0; realTime += delta;
        }
        last = now;
        canvas.drawColor(Color.BLACK);
        grid.configure(width, height, settings.columns(), settings.rowSpacing());
        scene.resize(grid.cols, grid.rows);
        if (previousMode != settings.mode()) { ripples.clear(); media.pause(); time=0; previousMode = settings.mode(); }
        settings.effects(scene.options); scene.symbols(settings.symbols());
        scene.render(settings.mode(), time, grid.cellW / grid.cellH, settings.wireframe(), settings.objectScale() / 100.0);
        if (settings.mode() >= 7 && width > 0 && height > 0) {
            int revision=settings.prefs.getInt("media_revision",0);
            String identity=settings.mediaUri()+":"+revision;
            if(!lastMedia.equals(identity)){lastMedia=identity;time=0;}
            MediaSource.Frame frame = media.request(settings.mediaUri(),settings.mode()==8,grid.cols,grid.rows,width,height,time,revision);
            if (frame != null && frame.pixels != null) {
                float contrast = settings.parameter("media_contrast",100,25,250)/100f;
                boolean invert = settings.prefs.getBoolean("media_invert",false);
                for(int i=0;i<scene.glyphs.length;i++) {
                    scene.pixel(i,MediaTone.luminance(frame.pixels[i],contrast,invert));
                }
            } else if(frame!=null) {
                messagePaint.setColor(settings.color()); messagePaint.setTextSize(12*density);
                messagePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                int start=0, line=0;
                while(start<frame.message.length() && line<4) {
                    int count=messagePaint.breakText(frame.message,start,frame.message.length(),true,Math.max(1,width-24*density),null);
                    if(count==0)break;
                    canvas.drawText(frame.message.substring(start,start+count),width*.5f,height*.5f+line*17*density,messagePaint);
                    start+=count;line++;
                }
            }
        }
        if (settings.mode()==6 || settings.touchEnabled()) ripples.render(scene,realTime,grid.cellW/grid.cellH,
                settings.parameter("ripple_speed",100,25,250)/100.0,settings.parameter("ripple_life",4,1,10));
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
