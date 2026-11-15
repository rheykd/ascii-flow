import ru.ascii.wallpaper.LockClockPolicy;
import java.util.TimeZone;

public final class LockClockTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        for(int sdk:new int[]{26,27,33,34,37})for(int flags:new int[]{0,1,2,3})for(boolean locked:new boolean[]{false,true}){
            check(!LockClockPolicy.show(false,false,sdk,flags,locked));
            check(!LockClockPolicy.show(true,true,sdk,flags,locked));
            check(LockClockPolicy.show(true,false,sdk,flags,locked)==(locked||(sdk>=34&&flags==2)));
        }
        check(!LockClockPolicy.show(true,false,37,3,false));
        check(LockClockPolicy.time(0,TimeZone.getTimeZone("UTC")).equals("0000"));
        check(LockClockPolicy.time(0,TimeZone.getTimeZone("GMT+03:00")).equals("0300"));
        check(LockClockPolicy.time(86399000,TimeZone.getTimeZone("UTC")).equals("2359"));
        check(LockClockPolicy.time(86400000,TimeZone.getTimeZone("UTC")).equals("0000"));
        // Vertical position: default keeps the old center, extremes stay fully on screen, monotonic.
        check(Math.abs(LockClockPolicy.center(50)-.45f)<1e-4);
        check(Math.abs(LockClockPolicy.center(0)-.27f)<1e-4);
        check(Math.abs(LockClockPolicy.center(100)-.73f)<1e-4);
        check(Math.abs(LockClockPolicy.center(-5)-LockClockPolicy.center(0))<1e-6);
        check(Math.abs(LockClockPolicy.center(105)-LockClockPolicy.center(100))<1e-6);
        float prev=-1;
        for(int p=0;p<=100;p++){float c=LockClockPolicy.center(p);check(c>prev&&c>=.27f&&c<=.73f);prev=c;}
        System.out.println("PASS: clock off/preview/home/lock routing across API 26-37, timezone and midnight rollover, vertical position.");
    }
}
