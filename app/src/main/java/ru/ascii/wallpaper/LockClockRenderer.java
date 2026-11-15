package ru.ascii.wallpaper;

import android.content.Context;
import android.graphics.*;

/** Tall, condensed silhouettes filled with ASCII; no date, weather or seconds. */
final class LockClockRenderer {
    private final GlyphRenderer glyphs;
    private final Paint maskPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final TallDigits.Pen pen=new TallDigits.Pen() {
        public void move(float x,float y){path.moveTo(x,y);}
        public void line(float x,float y){path.lineTo(x,y);}
        public void curve(float x1,float y1,float x2,float y2,float x,float y){path.cubicTo(x1,y1,x2,y2,x,y);}
        public void close(){path.close();}
    };
    private final GridGeometry grid=new GridGeometry();
    private Bitmap mask;
    private int[] pixels;
    private char[] chars;
    private String lastTime="";
    private int oldWidth,oldHeight,oldColumns,oldSpacing,oldPosition;
    LockClockRenderer(Context context) {
        glyphs=new GlyphRenderer(context);
        maskPaint.setColor(Color.WHITE);
        maskPaint.setStrokeCap(Paint.Cap.ROUND);maskPaint.setStrokeJoin(Paint.Join.ROUND);
    }
    void draw(Canvas canvas,int width,int height,Settings settings) {
        canvas.drawColor(Color.BLACK);
        if(width<=0||height<=0)return;
        grid.configure(width,height,Math.max(64,settings.columns()),settings.rowSpacing());
        String time=LockClockPolicy.time(System.currentTimeMillis(),java.util.TimeZone.getDefault());
        if(mask==null||width!=oldWidth||height!=oldHeight||grid.cols!=oldColumns||settings.rowSpacing()!=oldSpacing||settings.lockClockY()!=oldPosition||!time.equals(lastTime)) {
            if(mask!=null)mask.recycle();
            mask=Bitmap.createBitmap(grid.cols,grid.rows,Bitmap.Config.ARGB_8888);
            pixels=new int[grid.cols*grid.rows];chars=new char[pixels.length];
            Canvas target=new Canvas(mask);
            // Reference proportions: clock occupies 88% of width and 54% of height; vertical center from settings.
            float digitW=width*.16f;
            float[] positions={.06f,.255f,.585f,.78f};
            float center=LockClockPolicy.center(settings.lockClockY());
            float top=(height*(center-.27f)-grid.top)/grid.cellH, bottom=(height*(center+.27f)-grid.top)/grid.cellH;
            for(int i=0;i<4;i++) {
                path.reset();TallDigits.draw(time.charAt(i)-'0',digitW,height*.54f,pen);
                maskPaint.setStyle(Paint.Style.STROKE);maskPaint.setStrokeWidth(TallDigits.weight(digitW));
                target.save();target.translate(grid.cols*positions[i],top);
                target.scale(1/grid.cellW,1/grid.cellH);target.drawPath(path,maskPaint);target.restore();
            }
            maskPaint.setStyle(Paint.Style.FILL);
            float cx=grid.cols*.5f, rx=grid.cols*.018f, ry=rx*grid.cellW/grid.cellH;
            float cy1=top+(bottom-top)*.27f, cy2=top+(bottom-top)*.73f;
            target.drawOval(cx-rx,cy1-ry,cx+rx,cy1+ry,maskPaint);
            target.drawOval(cx-rx,cy2-ry,cx+rx,cy2+ry,maskPaint);
            mask.getPixels(pixels,0,grid.cols,0,0,grid.cols,grid.rows);
            oldWidth=width;oldHeight=height;oldColumns=grid.cols;oldSpacing=settings.rowSpacing();oldPosition=settings.lockClockY();lastTime=time;
        }
        String alphabet=settings.symbols().isEmpty()?"#%@@":settings.symbols();
        glyphs.configure(settings.font(),Color.WHITE,grid.cellW*Math.min(110,settings.glyphScale())/100f);
        for(int y=0;y<grid.rows;y++)for(int x=0;x<grid.cols;x++) {
            int i=y*grid.cols+x, alpha=pixels[i]>>>24;
            if(alpha<24)continue;
            // Fine variation preserves visible individual symbols inside large digits.
            int pattern=(x*7+y*11)%alphabet.length();chars[i]=alphabet.charAt(pattern);
            glyphs.draw(canvas,chars,i,(x+.5f)*grid.cellW,grid.top+(y+.5f)*grid.cellH,
                    alpha*settings.brightness()/100);
        }
    }
    void close(){if(mask!=null){mask.recycle();mask=null;}}
}
