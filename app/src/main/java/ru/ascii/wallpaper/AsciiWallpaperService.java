package ru.ascii.wallpaper;

import android.app.WallpaperColors;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public final class AsciiWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new AsciiEngine(); }

    private final class AsciiEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Settings settings = new Settings(AsciiWallpaperService.this);
        private final AsciiPainter painter = new AsciiPainter(AsciiWallpaperService.this);
        private boolean visible, ready, destroyed;
        private final Runnable frame = this::drawFrame;

        @Override public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(false);
            setOffsetNotificationsEnabled(false);
        }
        @Override public void onVisibilityChanged(boolean value) {
            visible = value;
            handler.removeCallbacks(frame);
            painter.pause();
            if (visible && ready) drawFrame();
        }
        @Override public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder); ready = true;
        }
        @Override public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            ready = true; drawFrame();
        }
        @Override public void onSurfaceDestroyed(SurfaceHolder holder) {
            ready = false; handler.removeCallbacks(frame); painter.pause();
            super.onSurfaceDestroyed(holder);
        }
        @Override public void onDestroy() {
            destroyed = true; handler.removeCallbacks(frame); super.onDestroy();
        }
        @Override public WallpaperColors onComputeColors() {
            if (android.os.Build.VERSION.SDK_INT < 27) return null;
            return new WallpaperColors(Color.valueOf(Color.BLACK), Color.valueOf(settings.color()), null);
        }
        private void drawFrame() {
            handler.removeCallbacks(frame);
            if (!ready || destroyed) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) painter.draw(canvas, canvas.getWidth(), canvas.getHeight(),
                        getResources().getDisplayMetrics().density, settings, false);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                // A launcher may release the surface between the callback and lockCanvas.
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas); }
                    catch (IllegalArgumentException | IllegalStateException ignored) { }
                }
            }
            if (visible && ready && !destroyed) {
                PowerManager power = (PowerManager)getSystemService(POWER_SERVICE);
                int fps = power != null && power.isPowerSaveMode() ? 15 : settings.fps();
                handler.postDelayed(frame, 1000 / fps);
            }
        }
    }
}
