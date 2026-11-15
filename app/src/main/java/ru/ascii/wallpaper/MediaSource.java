package ru.ascii.wallpaper;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import java.io.InputStream;

/** Only this worker owns decoder and bitmaps. UI receives immutable small pixel arrays. */
final class MediaSource {
    static final class Frame {
        final String key, message; final int[] pixels;
        Frame(String key, int[] pixels, String message) { this.key=key; this.pixels=pixels; this.message=message; }
    }
    private final Context context;
    private HandlerThread thread; private Handler worker;
    private volatile Frame frame;
    private volatile boolean busy, closed;
    private long requested;
    private MediaMetadataRetriever decoder;
    private Bitmap photo;
    private String opened = "";
    private long duration = 1;
    MediaSource(Context context) { this.context = context.getApplicationContext(); }
    Frame request(String uri, boolean video, int cols, int rows, int width, int height, double seconds, int revision) {
        String key = uri + ":" + video + ":" + cols + ":" + rows + ":" + width + ":" + height + ":" + revision;
        if (uri.isEmpty()) return new Frame(key, null, "Выберите " + (video ? "видео" : "фото") + " в настройках");
        if (closed) return null;
        long now = SystemClock.uptimeMillis();
        Frame current = frame;
        if (!busy && now - requested >= 100 && (current == null || !current.key.equals(key) || (video && current.pixels != null))) {
            if (worker == null) { thread = new HandlerThread("ascii-media"); thread.start(); worker = new Handler(thread.getLooper()); }
            busy = true; requested = now;
            worker.post(() -> {
                Bitmap decoded = null, small = null;
                try {
                    String sourceKey = uri + ":" + video + ":" + revision;
                    if (!opened.equals(sourceKey)) {
                        release();
                        if (video) {
                            decoder = new MediaMetadataRetriever(); decoder.setDataSource(context, Uri.parse(uri));
                            String d = decoder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            duration = Math.max(1, Long.parseLong(d == null ? "1" : d));
                        } else photo = loadPhoto(Uri.parse(uri));
                        opened = sourceKey;
                    }
                    Bitmap source;
                    if (video) {
                        long micros = ((long)(Math.max(0, seconds) * 1000) % duration) * 1000;
                        if (android.os.Build.VERSION.SDK_INT >= 27) {
                            decoded = decoder.getScaledFrameAtTime(micros, MediaMetadataRetriever.OPTION_CLOSEST, 480, 480);
                        } else decoded = decoder.getFrameAtTime(micros, MediaMetadataRetriever.OPTION_CLOSEST);
                        source = decoded;
                    } else source = photo;
                    if (source == null) throw new IllegalArgumentException("No decoded frame");
                    small = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(small);
                    // Center crop in screen coordinates, then compress each ASCII cell to one pixel.
                    float scale = Math.max(width/(float)source.getWidth(), height/(float)source.getHeight());
                    float sw = source.getWidth()*scale, sh = source.getHeight()*scale;
                    RectF dest = new RectF((width-sw)*.5f*cols/width, (height-sh)*.5f*rows/height,
                        (width+sw)*.5f*cols/width, (height+sh)*.5f*rows/height);
                    canvas.drawBitmap(source, null, dest, new Paint(Paint.FILTER_BITMAP_FLAG));
                    int[] pixels = new int[cols*rows]; small.getPixels(pixels,0,cols,0,0,cols,rows);
                    frame = new Frame(key, pixels, "");
                } catch (Exception | OutOfMemoryError e) {
                    frame = new Frame(key, null, "Файл недоступен или не поддерживается. Выберите другой.");
                    release();
                } finally {
                    if (decoded != null) decoded.recycle(); if (small != null) small.recycle(); busy = false;
                }
            });
        }
        return current != null && current.key.equals(key) ? current : new Frame(key,null,"Загрузка…");
    }
    private Bitmap loadPhoto(Uri uri) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
        try (InputStream input=context.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input,null,options); }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw new IllegalArgumentException("Invalid image");
        options.inSampleSize=1;
        while (Math.max(options.outWidth,options.outHeight)/options.inSampleSize > 1024) options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        Bitmap bitmap;
        try (InputStream input=context.getContentResolver().openInputStream(uri)) { bitmap=BitmapFactory.decodeStream(input,null,options); }
        if (bitmap==null) throw new IllegalArgumentException("Invalid image");
        int orientation=1;
        try (InputStream input=context.getContentResolver().openInputStream(uri)) {
            orientation=new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
        } catch (Exception ignored) { }
        Matrix m=new Matrix();
        switch (orientation) {
            case 2: m.setScale(-1,1); break; case 3: m.setRotate(180); break;
            case 4: m.setScale(1,-1); break; case 5: m.setRotate(90); m.postScale(-1,1); break;
            case 6: m.setRotate(90); break; case 7: m.setRotate(-90); m.postScale(-1,1); break;
            case 8: m.setRotate(-90); break;
        }
        if (!m.isIdentity()) { Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true); if(rotated!=bitmap)bitmap.recycle(); bitmap=rotated; }
        return bitmap;
    }
    private void release() {
        if(decoder!=null) { try { decoder.release(); } catch(Exception ignored) { } decoder=null; }
        if(photo!=null) { photo.recycle(); photo=null; } opened="";
    }
    void pause() { if(worker!=null)worker.post(this::release); }
    void close() { closed=true; if(worker!=null)worker.post(() -> { release(); thread.quitSafely(); }); }
}
