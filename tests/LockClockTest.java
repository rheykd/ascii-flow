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
        System.out.println("PASS: clock off/preview/home/lock routing across API 26-37, timezone and midnight rollover.");
    }
}
