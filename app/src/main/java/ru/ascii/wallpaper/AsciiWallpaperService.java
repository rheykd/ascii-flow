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
        private final LockClockRenderer clock = new LockClockRenderer(AsciiWallpaperService.this);
        private boolean showingClock;
        private boolean wantsClock() {
            android.app.KeyguardManager keyguard=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            int flags=android.os.Build.VERSION.SDK_INT>=34?getWallpaperFlags():0;
            return LockClockPolicy.show(settings.lockClock(),isPreview(),android.os.Build.VERSION.SDK_INT,flags,keyguard!=null&&keyguard.isKeyguardLocked());
        }
        @Override public void onWallpaperFlagsChanged(int which) { if(visible&&ready)drawFrame(); }
        private boolean visible, ready, destroyed;
        private long lastTouch;
        private final Runnable frame = this::drawFrame;

        @Override public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(true);
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
            destroyed = true; handler.removeCallbacks(frame); painter.close(); clock.close(); super.onDestroy();
        }
        @Override public void onTouchEvent(android.view.MotionEvent event) {
            if(wantsClock())return;
            if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN || event.getActionMasked()==android.view.MotionEvent.ACTION_POINTER_DOWN) {
                int i=event.getActionIndex(); android.graphics.Rect bounds=getSurfaceHolder().getSurfaceFrame();
                lastTouch=android.os.SystemClock.uptimeMillis();
                painter.touch(event.getX(i),event.getY(i),bounds.width(),bounds.height(),settings);
                if(visible && ready) drawFrame();
            }
        }
        @Override public android.os.Bundle onCommand(String action,int x,int y,int z,android.os.Bundle extras,boolean resultRequested) {
            if(wantsClock())return null;
            if(android.app.WallpaperManager.COMMAND_TAP.equals(action) && android.os.SystemClock.uptimeMillis()-lastTouch>350) {
                android.graphics.Rect bounds=getSurfaceHolder().getSurfaceFrame();
                painter.touch(x,y,bounds.width(),bounds.height(),settings);
                if(visible&&ready)drawFrame();
            }
            return super.onCommand(action,x,y,z,extras,resultRequested);
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
                if (canvas != null) {
                    boolean clockNow=wantsClock();
                    if(clockNow!=showingClock){painter.pause();showingClock=clockNow;}
                    if(clockNow)clock.draw(canvas,canvas.getWidth(),canvas.getHeight(),settings);
                    else painter.draw(canvas, canvas.getWidth(), canvas.getHeight(),getResources().getDisplayMetrics().density, settings, false);
                }
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
                handler.postDelayed(frame, showingClock?250:1000 / fps);
            }
        }
    }
}
