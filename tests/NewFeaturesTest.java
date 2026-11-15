import ru.ascii.wallpaper.*;
import java.util.Arrays;

public final class NewFeaturesTest {
    static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
    static int occupied(AsciiScene s) { int n=0;for(char c:s.glyphs)if(c!=' ')n++;return n; }
    static double radius(AsciiScene s,double x,double y,double aspect) {
        double total=0,weight=0;
        for(int j=0;j<s.rows;j++)for(int i=0;i<s.cols;i++){float l=s.light[j*s.cols+i];total+=l*Math.hypot(i-x*s.cols,(j-y*s.rows)/aspect);weight+=l;}
        return total/Math.max(.001,weight);
    }
    public static void main(String[] args) {
        AsciiScene scene=new AsciiScene(); RippleField ripples=new RippleField(); scene.resize(100,120);
        scene.render(6,0,.5); ripples.render(scene,0,.5,1,4); require(occupied(scene)==0,"Untouched wallpaper must be black");
        ripples.touch(.25,.35,0);
        scene.render(6,0,.5); ripples.render(scene,.5,.5,1,4); double r1=radius(scene,.25,.35,.5); require(occupied(scene)>0,"Touch must create wave");
        scene.render(6,0,.5); ripples.render(scene,1,.5,1,4); double r2=radius(scene,.25,.35,.5); require(r2>r1+5,"Wave must expand from touch");
        scene.render(6,0,.5); ripples.render(scene,4.1,.5,1,4); require(occupied(scene)==0,"Wave must fade to black");
        for(int i=0;i<100;i++)ripples.touch(i%10/10.0,i%7/7.0,5);
        scene.render(6,0,.5);ripples.render(scene,5.5,.5,1,4);require(occupied(scene)>0,"Multi touch bounded buffer");
        ripples.clear();scene.render(6,0,.5);ripples.render(scene,6,.5,1,4);require(occupied(scene)==0,"Clear removes waves");
        scene.symbols("01");
        for(int mode=0;mode<6;mode++){scene.render(mode,3,.5);for(char c:scene.glyphs)require(c==' '||c=='0'||c=='1',"Custom alphabet "+mode);}
        scene.symbols("");
        for(int mode=0;mode<6;mode++) {
            scene.render(mode,3,.5); char[] first=scene.glyphs.clone();
            switch(mode){case 0:scene.options.rotationX=0;scene.options.rotationY=0;break;case 1:scene.options.waveFrequency=230;break;
                case 2:scene.options.torusThickness=40;break;case 3:scene.options.spherePulse=250;break;
                case 4:scene.options.galaxyArms=7;break;case 5:scene.options.rainLength=250;scene.options.rainSpacing=5;break;}
            scene.render(mode,3,.5);require(!Arrays.equals(first,scene.glyphs),"Effect control must change mode "+mode);
        }
        require(MediaTone.luminance(0xff000000,1,false)==0,"Black");
        require(Math.abs(MediaTone.luminance(0xffffffff,1,false)-1)<.001,"White");
        require(MediaTone.luminance(0xff000000,1,true)==1,"Invert");
        for(int gray=0;gray<256;gray++)for(float contrast:new float[]{.25f,1,2.5f}) {
            float v=MediaTone.luminance(0xff000000|gray*0x010101,contrast,false);require(v>=0&&v<=1&&Float.isFinite(v),"Bounded luminance");
        }
        System.out.println("PASS: black idle, expanding/fading touch waves, bounded multitouch, custom symbols, controls for all six effects, media luminance.");
    }
}
