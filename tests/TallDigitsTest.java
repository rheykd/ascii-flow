import ru.ascii.wallpaper.TallDigits;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public final class TallDigitsTest {
    static Path2D.Float path(int digit,float width,float height) {
        Path2D.Float path=new Path2D.Float();
        TallDigits.draw(digit,width,height,new TallDigits.Pen(){
            public void move(float x,float y){path.moveTo(x,y);}public void line(float x,float y){path.lineTo(x,y);}
            public void curve(float a,float b,float c,float d,float x,float y){path.curveTo(a,b,c,d,x,y);}public void close(){path.closePath();}
        });return path;
    }
    public static void main(String[] args)throws Exception {
        for(int digit=0;digit<10;digit++)for(int height:new int[]{300,600,1000}) {
            Shape ink=new BasicStroke(TallDigits.weight(140),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(path(digit,140,height));
            Rectangle2D bounds=ink.getBounds2D();
            if(bounds.getMinX()<-.1||bounds.getMaxX()>140.1||bounds.getMinY()<-.1||bounds.getMaxY()>height+.1)throw new AssertionError("Clipped digit "+digit);
        }
        Shape zero=new BasicStroke(TallDigits.weight(140)).createStrokedShape(path(0,140,1000));
        int vertical=0,horizontal=0;
        for(int i=0;i<70;i++){if(zero.contains(i+.5,500))vertical++;if(zero.contains(70,i+.5))horizontal++;}
        if(Math.abs(vertical-horizontal)>1)throw new AssertionError("Unequal horizontal and vertical weight");
        BufferedImage image=new BufferedImage(1080,960,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,1080,960);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        String[] times={"1030","2359","0426"};
        for(int p=0;p<3;p++){
            BufferedImage mask=new BufferedImage(360,900,BufferedImage.TYPE_INT_RGB);Graphics2D mg=mask.createGraphics();
            mg.setColor(Color.BLACK);mg.fillRect(0,0,360,900);mg.setColor(Color.WHITE);mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            float[] xs={.06f,.255f,.585f,.78f};
            for(int d=0;d<4;d++){mg.setStroke(new BasicStroke(TallDigits.weight(57.6f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));mg.translate(xs[d]*360,162);mg.draw(path(times[p].charAt(d)-'0',57.6f,486));mg.translate(-xs[d]*360,-162);}
            for(float y:new float[]{162+486*.27f,162+486*.73f})mg.fill(new Ellipse2D.Float(180-6.48f,y-6.48f,12.96f,12.96f));mg.dispose();
            g.setFont(new Font("Monospaced",Font.PLAIN,6));
            for(int y=0;y<900;y+=7)for(int x=0;x<360;x+=4){int light=mask.getRGB(x,y)&255;if(light<24)continue;g.setColor(new Color(light,light,light));g.drawString(((x/4+y/7)%3==0)?"#":"@",p*360+x,y+5);}
            g.setColor(new Color(0x91f5cc));g.setFont(new Font("Monospaced",Font.PLAIN,14));g.drawString(times[p].substring(0,2)+":"+times[p].substring(2),p*360+145,930);
        }
        g.dispose();ImageIO.write(image,"png",new File("dist/clock-1.4.1-preview.png"));
        System.out.println("PASS: all ten digits fit; horizontal/vertical strokes have equal weight at tall proportions.");
    }
}
