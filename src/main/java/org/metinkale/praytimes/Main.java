package org.metinkale.praytimes;

/**
 * Created by metin on 11.10.2017.
 */

public class Main {
    
    public static void main(String args[]) {
        PrayTimes pt = new PrayTimes();
        pt.setDate(2019, 1, 1);
        pt.setCoordinates(52.24702453613281,10.506549835205078,72);
        pt.setMethod(Method.MWL);
        
        pt.setHighLatsAdjustment(PrayTimes.HIGHLAT_NONE);
        System.out.println("Imsak:" + pt.getTime(PrayTimes.TIMES_IMSAK));
        System.out.println("Fajr:" + pt.getTime(PrayTimes.TIMES_FAJR));
        System.out.println("Sunrise:" + pt.getTime(PrayTimes.TIMES_SUNRISE));
        System.out.println("Dhuhr:" + pt.getTime(PrayTimes.TIMES_DHUHR));
        System.out.println("Asr1:" + pt.getTime(PrayTimes.TIMES_ASR_SHAFII));
        System.out.println("Asr2:" + pt.getTime(PrayTimes.TIMES_ASR_HANAFI));
        System.out.println("Sunset:" + pt.getTime(PrayTimes.TIMES_SUNSET));
        System.out.println("Maghrib:" + pt.getTime(PrayTimes.TIMES_MAGHRIB));
        System.out.println("Ishaa:" + pt.getTime(PrayTimes.TIMES_ISHA));
        System.out.println("Midnight:" + pt.getTime(PrayTimes.TIMES_MIDNIGHT));
        
        
    }
}
