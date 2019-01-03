/*
PrayTimes-Java: Prayer Times Java Calculator (ver 0.9)

Copyright (C) 2007-2011 PrayTimes.org (JS Code ver 2.3)
Copyright (C) 2017 Metin Kale (Java Code)

Developer JS: Hamid Zarrabi-Zadeh
Developer Java: Metin Kale

License: GNU LGPL v3.0

TERMS OF USE:
	Permission is granted to use this code, with or
	without modification, in any website or application
	provided that credit is given to the original work
	with a link back to PrayTimes.org.

This program is distributed in the hope that it will
be useful, but WITHOUT ANY WARRANTY.

PLEASE DO NOT REMOVE THIS COPYRIGHT BLOCK.

*/
package org.metinkale.praytimes;

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;


@SuppressWarnings({"WeakerAccess", "unused"})
public class PrayTimes implements Serializable {
    // constants are at the bottom
    
    
    private double lat, lng, elv;
    
    
    //if true double values are in minutes, otherwhise in degrees
    private boolean imsakMin = true;
    private boolean maghribMin = false;
    private boolean ishaMin = false;
    //dhuhr is always in min, fajr is always in degrees
    
    private double imsak;
    private double fajr;
    private double dhuhr;
    private double maghrib;
    private double isha;
    private int highLats;
    private int midnight;
    private TimeZone timeZone = TimeZone.getDefault();
    private int asrJuristic;
    
    
    private double[] tune = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    
    private transient double jdate;
    private transient int year;
    private transient int month;
    private transient int day;
    private transient long timestamp;
    private transient String[] stringTimes;
    private transient double[] times;
    
    public PrayTimes() {
        setMethod(Method.MWL);
    }
    
    
    /**
     * set coordinates
     *
     * @param lat Latitude
     * @param lng Longitute
     * @param elv Elevation
     */
    public void setCoordinates(double lat, double lng, double elv) {
        this.lat = lat;
        this.lng = lng;
        this.elv = elv;
        clearTimes();
    }
    
    /**
     * clears cached times
     */
    private void clearTimes() {
        times = null;
        stringTimes = null;
    }
    
    
    /**
     * set date
     *
     * @param year  Year (e.g. 2017)
     * @param month Month (1-12)
     * @param day   Date/Day of Month
     */
    public void setDate(int year, int month, int day) {
        if (year == this.year && this.month == month && this.day == this.month)
            return;
        this.year = year;
        this.month = month;
        this.day = day;
        Calendar cal = Calendar.getInstance(timeZone);
        cal.set(this.year, this.month - 1, this.day);
        timestamp = cal.getTimeInMillis();
        clearTimes();
    }
    
    /**
     * return prayer time for a given date and time
     *
     * @param time TIME_ from Constants
     * @return array of Times
     */
    public String getTime(int time) {
        return getTimes()[time];
    }
    
    /**
     * return prayer times for a given date
     *
     * @return array of Times
     */
    private String[] getTimes() {
        if (stringTimes != null)
            return stringTimes;
        
        double[] doubles = getTimesAsDouble();
        
        //convert to HH:mm
        stringTimes = new String[doubles.length];
        for (int i = 0; i < stringTimes.length; i++) {
            while (doubles[i] > 24) {
                doubles[i] -= 24;
            }
            while (doubles[i] < 0) {
                doubles[i] += 24;
            }
            stringTimes[i] = toString(doubles[i]);
        }
        return stringTimes;
    }
    
    /**
     * convert double time to HH:MM
     *
     * @param time time in double
     * @return HH:MM
     */
    private String toString(double time) {
        return az((int) Math.floor(time)) + ":" + az((int) (Math.round(time * 60)) % 60);
    }
    
    /**
     * return a two digit String of number
     *
     * @param i number
     * @return two digit number
     */
    private String az(int i) {
        return i >= 10 ? "" + i : "0" + i;
    }
    
    
    /**
     * return prayer times for a given date as double values
     *
     * @return array of Times
     */
    private double[] getTimesAsDouble() {
        if (times != null)
            return times;
        jdate = julian(year, month, day) - lng / (15.0 * 24.0);
        
        computeTimes();
        tuneTimes();
        return times;
    }
    
    /**
     * Calculates the qibla time, if you turn yourself to the sun at that time, you are turned to qibla
     * Note: does not exists everywhere
     *
     * @return Qibla Time
     */
    public QiblaTime getQiblaTime() {
        getTimes();
        Calendar cal = Calendar.getInstance(timeZone);
        //noinspection MagicConstant
        cal.set(year, month - 1, day, 12, 0, 0);
        long[] qibla = new long[4];
        qibla[0] = QiblaTimeCalculator.findQiblaTime(cal.getTimeInMillis(), lat, lng, 0);
        qibla[1] = QiblaTimeCalculator.findQiblaTime(cal.getTimeInMillis(), lat, lng, Math.PI / 2);
        qibla[2] = QiblaTimeCalculator.findQiblaTime(cal.getTimeInMillis(), lat, lng, -Math.PI / 2);
        qibla[3] = QiblaTimeCalculator.findQiblaTime(cal.getTimeInMillis(), lat, lng, Math.PI);
        double[] qiblaD = new double[4];
        String[] qiblaS = new String[4];
        for (int i = 0; i < 4; i++) {
            cal.setTimeInMillis(qibla[i]);
            qiblaD[i] = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60d + cal.get(Calendar.SECOND) / 3600d;
            if (qiblaD[i] < times[TIMES_SUNRISE] || qiblaD[i] > times[TIMES_SUNSET]) {
                qiblaD[i] = 0;
                qiblaS[i] = null;
            } else {
                qiblaS[i] = toString(qiblaD[i]);
            }
            
            
        }
        QiblaTime qt = new QiblaTime();
        qt.front = qiblaS[0];
        qt.right = qiblaS[1];
        qt.left = qiblaS[2];
        qt.back = qiblaS[3];
        
        return qt;
    }
    
    /**
     * Tune times according to user settings
     */
    private void tuneTimes() {
        for (int i = 0; i < times.length; i++) {
            times[i] += tune[i];
        }
    }
    
    
    /**
     * convert Gregorian date to Julian day
     * Ref: Astronomical Algorithms by Jean Meeus
     *
     * @param year  year
     * @param month month
     * @param day   day
     * @return julian day
     */
    private static double julian(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = (2 - a + Math.floor(a / 4.0));
        
        return (Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5);
    }
    
    /**
     * compute prayer times
     */
    private void computeTimes() {
        // default times
        times = new double[]{5, 5, 6, 12, 12, 13, 13, 13, 18, 18, 18, 0};
        
        computePrayerTimes();
        
        adjustTimes();
        
        // add midnight time
        times[TIMES_MIDNIGHT] = (midnight == MIDNIGHT_JAFARI) ? times[TIMES_SUNSET] + this.timeDiff(times[TIMES_SUNSET], times[TIMES_FAJR]) / 2.0 :
                times[TIMES_SUNSET] + this.timeDiff(times[TIMES_SUNSET], times[TIMES_SUNRISE]) / 2.0;
        
    }
    
    /**
     * compute the difference between two times
     *
     * @param time1 Time 1
     * @param time2 Time 2
     * @return timediff
     */
    private double timeDiff(double time1, double time2) {
        return DMath.fixHour(time2 - time1);
    }
    
    /**
     * adjust times
     */
    private void adjustTimes() {
        double offset = getTimeZoneOffset();
        for (int i = 0; i < times.length; i++) {
            times[i] += offset - lng / 15.0;
        }
        
        if (highLats != HIGHLAT_NONE)
            adjustHighLats();
        
        if (imsakMin)
            times[TIMES_IMSAK] = times[TIMES_FAJR] - (imsak) / 60.0;
        if (maghribMin)
            times[TIMES_MAGHRIB] = times[TIMES_SUNSET] + (maghrib) / 60.0;
        if (ishaMin)
            times[TIMES_ISHA] = times[TIMES_MAGHRIB] + (isha) / 60.0;
        times[TIMES_DHUHR] = times[TIMES_ZAWAL] + (dhuhr) / 60.0;
    }
    
    /**
     * adjust times for locations in higher latitudes
     */
    private void adjustHighLats() {
        double nightTime = this.timeDiff(times[TIMES_SUNSET], times[TIMES_SUNRISE]);
        
        times[TIMES_IMSAK] = this.adjustHLTime(times[TIMES_IMSAK], times[TIMES_SUNRISE], (imsak), nightTime, true);
        times[TIMES_FAJR] = this.adjustHLTime(times[TIMES_FAJR], times[TIMES_SUNRISE], (fajr), nightTime, true);
        times[TIMES_ISHA] = this.adjustHLTime(times[TIMES_ISHA], times[TIMES_SUNSET], (isha), nightTime, false);
        times[TIMES_MAGHRIB] = this.adjustHLTime(times[TIMES_MAGHRIB], times[TIMES_SUNSET], (maghrib), nightTime, false);
    }
    
    /**
     * adjust a time for higher latitudes
     *
     * @param time  time
     * @param base  base
     * @param angle angle
     * @param night night time
     * @param ccw   true if clock-counter-wise, false otherwise
     * @return adjusted time
     */
    private double adjustHLTime(double time, double base, double angle, double night, boolean ccw) {
        double portion = this.nightPortion(angle, night);
        double timeDiff = (ccw) ? this.timeDiff(time, base) : this.timeDiff(base, time);
        if (Double.isNaN(time) || timeDiff > portion)
            time = base + (ccw ? -portion : portion);
        return time;
    }
    
    /**
     * the night portion used for adjusting times in higher latitudes
     *
     * @param angle angle
     * @param night night time
     * @return night portion
     */
    private double nightPortion(double angle, double night) {
        double method = highLats;
        double portion = 1.0 / 2.0;// MidNight
        if (method == HIGHLAT_ANGLEBASED)
            portion = 1.0 / 60.0 * angle;
        if (method == HIGHLAT_ONESEVENTH)
            portion = 1.0 / 7.0;
        return portion * night;
    }
    
    /**
     * compute prayer times at given julian date
     */
    private void computePrayerTimes() {
        // convert hours to day portions
        for (int i = 0; i < times.length; i++) {
            times[i] = times[i] / 24.0;
        }
        
        times[TIMES_IMSAK] = this.sunAngleTime((imsak), times[TIMES_IMSAK], true);
        times[TIMES_FAJR] = this.sunAngleTime((fajr), times[TIMES_FAJR], true);
        times[TIMES_SUNRISE] = this.sunAngleTime(this.riseSetAngle(), times[TIMES_SUNRISE], true);
        times[TIMES_ZAWAL] = this.midDay(times[TIMES_ZAWAL]);
        times[TIMES_ASR_SHAFII] = this.asrTime(JURISTIC_STANDARD, times[TIMES_ASR_SHAFII]);
        times[TIMES_ASR_HANAFI] = this.asrTime(JURISTIC_HANAFI, times[TIMES_ASR_HANAFI]);
        times[TIMES_ASR] = asrJuristic != JURISTIC_STANDARD ? times[TIMES_ASR_HANAFI] : times[TIMES_ASR_SHAFII];
        times[TIMES_SUNSET] = this.sunAngleTime(this.riseSetAngle(), times[TIMES_SUNSET], false);
        times[TIMES_MAGHRIB] = this.sunAngleTime((maghrib), times[TIMES_MAGHRIB], false);
        times[TIMES_ISHA] = this.sunAngleTime((isha), times[TIMES_MAGHRIB], false);
    }
    
    /**
     * compute asr time
     *
     * @param factor Shadow Factor
     * @param time   default  time
     * @return asr time
     */
    private double asrTime(int factor, double time) {
        double decl = this.sunPositionDeclination(jdate + time);
        double angle = -DMath.arccot(factor + DMath.tan(Math.abs(lat - decl)));
        return this.sunAngleTime(angle, time, false);
    }
    
    
    /**
     * compute the time at which sun reaches a specific angle below horizon
     *
     * @param angle angle
     * @param time  default time
     * @param ccw   true if counter-clock-wise, false otherwise
     * @return time
     */
    private double sunAngleTime(double angle, double time, boolean ccw) {
        double decl = this.sunPositionDeclination(jdate + time);
        double noon = this.midDay(time);
        double t = 1.0 / 15.0 * DMath.arccos((-DMath.sin(angle) - DMath.sin(decl) * DMath.sin(lat)) / (DMath.cos(decl) * DMath.cos(lat)));
        return noon + (ccw ? -t : t);
    }
    
    /**
     * compute mid-day time
     *
     * @param time default time
     * @return midday time
     */
    private double midDay(double time) {
        double eqt = this.equationOfTime(jdate + time);
        return DMath.fixHour(12 - eqt);
    }
    
    /**
     * compute equation of time
     * Ref: http://aa.usno.navy.mil/faq/docs/SunApprox.php
     *
     * @param jd julian date
     * @return equation of time
     */
    private double equationOfTime(double jd) {
        double d = jd - 2451545.0;
        double g = DMath.fixAngle(357.529 + 0.98560028 * d);
        double q = DMath.fixAngle(280.459 + 0.98564736 * d);
        double l = DMath.fixAngle(q + 1.915 * DMath.sin(g) + 0.020 * DMath.sin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double ra = DMath.arctan2(DMath.cos(e) * DMath.sin(l), DMath.cos(l)) / 15;
        return q / 15.0 - DMath.fixHour(ra);
    }
    
    /**
     * compute  declination angle of sun
     * Ref: http://aa.usno.navy.mil/faq/docs/SunApprox.php
     *
     * @param jd julian date
     * @return declination angle of sun
     */
    private double sunPositionDeclination(double jd) {
        double d = jd - 2451545.0;
        double g = DMath.fixAngle(357.529 + 0.98560028 * d);
        double q = DMath.fixAngle(280.459 + 0.98564736 * d);
        double l = DMath.fixAngle(q + 1.915 * DMath.sin(g) + 0.020 * DMath.sin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        return DMath.arcsin(DMath.sin(e) * DMath.sin(l));
    }
    
    
    /**
     * compute sun angle for sunset/sunrise
     *
     * @return sun angle of sunset/sunrise
     */
    private double riseSetAngle() {
        //double earthRad = 6371009; // in meters
        //double angle = DMath.arccos(earthRad/(earthRad+ elv));
        double angle = 0.0347 * Math.sqrt(elv); // an approximation
        return 0.833 + angle;
    }
    
    
    /**
     * Sets the calculation method
     * Attention: overrides all other parameters, set this as first
     * Default: MWL
     *
     * @param method calculation method
     */
    public void setMethod(Method method) {
        fajr = method.fajr;
        isha = method.isha;
        maghrib = method.maghrib;
        maghribMin = method.maghribMin;
        ishaMin = method.ishaMin;
        midnight = method.midnight;
        clearTimes();
    }
    
    
    /**
     * Sets Imsak time in Degrees/Mins before Fajr
     *
     * @param value degrees/mins
     * @param isMin true if value is in mins, false if it is in degreess
     */
    public void setImsakTime(double value, boolean isMin) {
        imsak = value;
        imsakMin = isMin;
        clearTimes();
    }
    
    /**
     * Sets Fajr time degrees
     *
     * @param degrees degrees
     */
    public void setFajrDegrees(double degrees) {
        fajr = degrees;
        clearTimes();
    }
    
    
    /**
     * Sets Dhuhr time in mins after zawal/solar noon
     *
     * @param mins minutes
     */
    public void setDhuhrMins(double mins) {
        dhuhr = mins;
        clearTimes();
    }
    
    /**
     * Sets Maghrib time in Degrees/Mins after Sunset
     *
     * @param value degrees/mins
     * @param isMin true if value is in mins, false if it is in degreess
     */
    public void setMaghribTime(double value, boolean isMin) {
        maghrib = value;
        maghribMin = isMin;
        clearTimes();
    }
    
    
    /**
     * Sets Isha time in Degrees or Mins after Sunset
     *
     * @param value degrees/mins
     * @param isMin true if value is in mins, false if it is in degreess
     */
    public void setIshaTime(double value, boolean isMin) {
        isha = value;
        ishaMin = isMin;
        clearTimes();
    }
    
    /**
     * In locations at higher latitude, twilight may persist throughout the night during some months of the year.
     * In these abnormal periods, the determination of Fajr and Isha is not possible using the usual formulas mentioned
     * in the previous section. To overcome this problem, several solutions have been proposed,
     * three of which are described below.
     * <p>
     * {@link PrayTimes#HIGHLAT_NONE HIGHLAT_NONE} (Default, see notes)
     * {@link PrayTimes#HIGHLAT_NIGHTMIDDLE HIGHLAT_NIGHTMIDDLE}
     * {@link PrayTimes#HIGHLAT_ONESEVENTH HIGHLAT_ONESEVENTH}
     * {@link PrayTimes#HIGHLAT_ANGLEBASED HIGHLAT_ANGLEBASED}
     *
     * @param method method
     */
    public void setHighLatsAdjustment(int method) {
        highLats = method;
        clearTimes();
    }
    
    /**
     * Midnight is generally calculated as the mean time from Sunset to Sunrise, i.e., Midnight = 1/2(Sunrise - Sunset).
     * In Shia point of view, the juridical midnight (the ending time for performing Isha prayer) is the mean time
     * from Sunset to Fajr, i.e., Midnight = 1/2(Fajr - Sunset).
     * <p>
     * {@link PrayTimes#MIDNIGHT_STANDARD MIDNIGHT_STANDARD} (Default)
     * {@link PrayTimes#MIDNIGHT_JAFARI MIDNIGHT_JAFARI}
     *
     * @param mode mode
     */
    public void setMidnightMode(int mode) {
        midnight = mode;
        clearTimes();
    }
    
    /**
     * TimeZone for times
     * <p>
     * Default: {@link TimeZone#getDefault() TimeZone.getDefault()}
     *
     * @param tz Timezone
     */
    public void setTimezone(TimeZone tz) {
        timeZone = tz;
        clearTimes();
    }
    
    /**
     * There are two main opinions on how to calculate Asr time.
     * <p>
     * {@link PrayTimes#JURISTIC_STANDARD JURISTIC_STANDARD}
     * {@link PrayTimes#JURISTIC_HANAFI JURISTIC_HANAFI}
     * <p>
     * Default: {@link PrayTimes#JURISTIC_STANDARD JURISTIC_STANDARD}
     *
     * @param asr method
     */
    public void setAsrJuristic(int asr) {
        asrJuristic = asr;
        clearTimes();
    }
    
    
    /**
     * tuneTimes single time
     *
     * @param time time
     * @param tune hours
     */
    public void tune(int time, double tune) {
        this.tune[time] = tune;
        clearTimes();
    }
    
    /**
     * get Timezone offset for specific date
     *
     * @return time zone offset
     */
    private double getTimeZoneOffset() {
        return timeZone.getOffset(timestamp) / 1000.0 / 60 / 60;
    }
    
    public TimeZone getTimeZone() {
        return timeZone;
    }
    
    public int getAsrJuristic() {
        return asrJuristic;
    }
    
    
    public int getHighLatsAdjustment() {
        return highLats;
    }
    
    public double getLatitude() {
        return lat;
    }
    
    
    public double getLongitude() {
        return lng;
    }
    
    
    public double getElevation() {
        return elv;
    }
    
    public double getImsakValue() {
        return imsak;
    }
    
    public boolean isImsakTimeInMins() {
        return imsakMin;
    }
    
    public double getFajrDegrees() {
        return fajr;
    }
    
    public boolean isMaghribTimeInMins() {
        return maghribMin;
    }
    
    
    public boolean isIshaTimeInMins() {
        return ishaMin;
    }
    
    public double getDhuhrMins() {
        return dhuhr;
    }
    
    public double getMaghribValue() {
        return maghrib;
    }
    
    public double getIshaValue() {
        return isha;
    }
    
    
    /**
     * Asr Juristic Methods
     * Shafii, Maliki, Jafari and Hanbali (shadow factor = 1)
     */
    public static final int JURISTIC_STANDARD = 1;//
    /**
     * Asr Juristic Methods
     * Hanafi school of tought (shadow factor = 2)
     */
    public static final int JURISTIC_HANAFI = 2;
    //===============================================
    /**
     * Adjust Methods for Higher Latitudes
     * Method: no adjustment
     */
    public static final int HIGHLAT_NONE = 0;
    /**
     * Adjust Methods for Higher Latitudes
     * Method: angle/60th of night
     * <p>
     * This is an intermediate solution, used by some recent prayer time calculators. Let α be the twilight angle for Isha, and let t = α/60. The period between sunset and sunrise is divided into t parts. Isha begins after the first part. For example, if the twilight angle for Isha is 15, then Isha begins at the end of the first quarter (15/60) of the night. Time for Fajr is calculated similarly.
     */
    public static final int HIGHLAT_ANGLEBASED = 1;
    ;
    /**
     * Adjust Methods for Higher Latitudes
     * Method: 1/7th of night
     * <p>
     * In this method, the period between sunset and sunrise is divided into seven parts. Isha begins after the first one-seventh part, and Fajr is at the beginning of the seventh part.
     */
    public static final int HIGHLAT_ONESEVENTH = 2;
    /**
     * Adjust Methods for Higher Latitudes
     * Method: middle of night
     * <p>
     * In this method, the period from sunset to sunrise is divided into two halves. The first half is considered to be the "night" and the other half as "day break". Fajr and Isha in this method are assumed to be at mid-night during the abnormal periods.
     */
    public static final int HIGHLAT_NIGHTMIDDLE = 3;
    
    //===============================================>
    
    /**
     * Midnight mode: Mid Sunset to Sunrise
     */
    public static final int MIDNIGHT_STANDARD = 0;
    /**
     * Midnight mode: Mid Sunset to Fajr
     */
    public static final int MIDNIGHT_JAFARI = 1;
    
    //===============================================>
    /**
     * The time to stop eating Sahur (for fasting), slightly before Fajr.
     */
    public static final int TIMES_IMSAK = 0;
    /**
     * When the sky begins to lighten (dawn).
     */
    public static final int TIMES_FAJR = 1;
    /**
     * The time at which the first part of the Sun appears above the horizon.
     */
    public static final int TIMES_SUNRISE = 2;
    /**
     * Zawal (Solar Noon): When the Sun reaches its highest point in the sky.
     */
    public static final int TIMES_ZAWAL = 3;
    /**
     * When the Sun begins to decline after reaching its highest point in the sky, slightly after solar noon
     */
    public static final int TIMES_DHUHR = 4;
    /**
     * The time when the length of any object's shadow reaches a factor (usually 1 or 2) of the length of the object itself plus the length of that object's shadow at noon.
     */
    public static final int TIMES_ASR = 5;
    /**
     * The time when the length of any object's shadow reaches a factor 1 of the length of the object itself plus the length of that object's shadow at noon.
     */
    public static final int TIMES_ASR_SHAFII = 6;
    /**
     * The time when the length of any object's shadow reaches a factor 2 of the length of the object itself plus the length of that object's shadow at noon.
     */
    public static final int TIMES_ASR_HANAFI = 7;
    /**
     * The time at which the Sun disappears below the horizon.
     */
    public static final int TIMES_SUNSET = 8;
    /**
     * Soon after sunset.
     */
    public static final int TIMES_MAGHRIB = 9;
    /**
     * The time at which darkness falls and there is no scattered light in the sky.
     */
    public static final int TIMES_ISHA = 10;
    /**
     * The mean time from sunset to sunrise (or from Maghrib to Fajr, in some schools of thought).
     */
    public static final int TIMES_MIDNIGHT = 11;
}


