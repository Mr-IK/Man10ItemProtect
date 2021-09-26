package red.man10.man10itemprotect.util;

/**
 * Created by sho on 2018/06/14.
 */
public class JPYFormat {

    public static String getText(Double val){
        return getText(val.longValue());
    }

    public static String getText(int val){
        return getText((long)val);
    }

    public static String getText(Long val){
        if(val < 10000){
            return String.valueOf(val);
        }
        if(val < 100000000){
            long man = val/10000;
            String left = String.valueOf(val).substring(String.valueOf(val).length() - 4);
            if(Long.parseLong(left) == 0){
                return man + "万";
            }
            return man + "万" + Long.parseLong(left);
        }
        if(val < 100000000000L){
            long oku = val/100000000;
            String man = String.valueOf(val).substring(String.valueOf(val).length() - 8);
            String te = man.substring(0, 4);
            String left = String.valueOf(val).substring(String.valueOf(val).length() - 4);
            if(Long.parseLong(te)  == 0){
                if( Long.parseLong(left) == 0){
                    return oku + "億";
                }else{
                    return oku + "億"+ Long.parseLong(left);
                }
            }else{
                if( Long.parseLong(left) == 0){
                    return oku + "億" + Long.parseLong(te) + "万";
                }
            }
            return oku + "億" + Long.parseLong(te) + "万" + Long.parseLong(left);
        }
        return "Null";
    }

}
