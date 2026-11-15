package ru.ascii.wallpaper;

/** Centerline lettering drawn at its final physical dimensions, never stretched. */
public final class TallDigits {
    public interface Pen {
        void move(float x,float y); void line(float x,float y);
        void curve(float x1,float y1,float x2,float y2,float x,float y); void close();
    }
    public static float weight(float width) { return width*.24f; }
    private static final float K=.55228475f;
    private static void loop(Pen p,float l,float t,float r,float b) {
        float radius=Math.min((r-l)/2,(b-t)/2), k=radius*K;
        p.move(l+radius,t);p.line(r-radius,t);
        p.curve(r-radius+k,t,r,t+radius-k,r,t+radius);p.line(r,b-radius);
        p.curve(r,b-radius+k,r-radius+k,b,r-radius,b);p.line(l+radius,b);
        p.curve(l+radius-k,b,l,b-radius+k,l,b-radius);p.line(l,t+radius);
        p.curve(l,t+radius-k,l+radius-k,t,l+radius,t);p.close();
    }
    public static void draw(int digit,float width,float height,Pen p) {
        float s=weight(width),l=s/2,r=width-s/2,t=s/2,b=height-s/2,m=height*.5f;
        float radius=(r-l)/2,c=(l+r)/2,k=radius*K;
        switch(digit) {
            case 0: loop(p,l,t,r,b);break;
            case 1:
                p.move(l,t+radius*.45f);p.line(c,t);p.line(c,b);
                p.move(l,b);p.line(r,b);break;
            case 2:
                p.move(l,t+radius);p.curve(l,t+radius-k,c-k,t,c,t);
                p.curve(c+k,t,r,t+radius-k,r,t+radius);p.line(r,m-radius);
                p.curve(r,m,l,m,l,m+radius);p.line(l,b);p.line(r,b);break;
            case 3:
                p.move(l,t+radius);p.curve(l,t+radius-k,c-k,t,c,t);
                p.curve(c+k,t,r,t+radius-k,r,t+radius);p.line(r,m-radius);
                p.curve(r,m-radius+k,c+k,m,c,m);p.line(l+radius*.28f,m);
                p.move(c,m);p.curve(c+k,m,r,m+radius-k,r,m+radius);p.line(r,b-radius);
                p.curve(r,b-radius+k,c+k,b,c,b);p.curve(c-k,b,l,b-radius+k,l,b-radius);break;
            case 4:
                p.move(l,t);p.line(l,height*.56f);p.line(r,height*.56f);
                p.move(r,t);p.line(r,b);break;
            case 5:
                p.move(r,t);p.line(l,t);p.line(l,m);p.line(c,m);
                p.curve(c+k,m,r,m+radius-k,r,m+radius);p.line(r,b-radius);
                p.curve(r,b-radius+k,c+k,b,c,b);p.curve(c-k,b,l,b-radius+k,l,b-radius);break;
            case 6:
                loop(p,l,m,r,b);p.move(l,m+radius);p.line(l,t+radius);
                p.curve(l,t+radius-k,c-k,t,c,t);p.curve(c+k,t,r,t+radius-k,r,t+radius);break;
            case 7:
                p.move(l,t);p.line(r,t);p.line(l+radius*.65f,b);break;
            case 8: loop(p,l,t,r,m);loop(p,l,m,r,b);break;
            case 9:
                loop(p,l,t,r,m);p.move(r,m-radius);p.line(r,b-radius);
                p.curve(r,b-radius+k,c+k,b,c,b);p.curve(c-k,b,l,b-radius+k,l,b-radius);break;
            default:throw new IllegalArgumentException("Digit must be 0..9");
        }
    }
}
