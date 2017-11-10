package com.google.cloud.android.speech.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by USER on 2017-10-12.
 */

public class DateUtil {
    public static String durationToDate(int value){
        int s = value/1000%60;
        int m =value/1000/60%60;
        int h = value/1000/60/60;
        return String.format("%d:%d:%d",h,m,s);
    }

    public static String millisToTime(int value){
        Date date = new Date(value);
        return new SimpleDateFormat("HH:mm").format(date);
    }
    public static String millisToDate(long value){
        Date date = new Date(value);
        return new SimpleDateFormat("yy/MM/dd").format(date);
    }
}
