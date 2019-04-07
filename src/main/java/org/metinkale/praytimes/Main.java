package org.metinkale.praytimes;

/**
 * Created by metin on 11.10.2017.
 */

public class Main {
    
    public static void main(String args[]) {
        PrayTimes pt = new PrayTimes();
        pt.setDate(2019, 1, 4);
        pt.setCoordinates(52.24702453613281, 10.506549835205078, 72);
        
        pt.tuneFajr(0, -83);
        pt.tuneSunrise(-3);
        pt.tuneDhuhr(5);
        pt.tuneAsrShafi(5);
        pt.tuneMaghrib(0, 2);
        pt.tuneIshaa(0, 72);
        System.out.println("Fajr:" + pt.getTime(Times.Fajr));
        System.out.println("Sunrise:" + pt.getTime(Times.Sunrise));
        System.out.println("Dhuhr:" + pt.getTime(Times.Dhuhr));
        System.out.println("Asr1:" + pt.getTime(Times.AsrShafi));
        System.out.println("Maghrib:" + pt.getTime(Times.Maghrib));
        System.out.println("Ishaa:" + pt.getTime(Times.Ishaa));
        
        
    }
}
