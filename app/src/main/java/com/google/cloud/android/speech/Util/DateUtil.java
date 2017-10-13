package com.google.cloud.android.speech.Util;

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

    public static String millisToDate(long value){
        Date date = new Date(value);
        return new SimpleDateFormat("yyyy/MM/dd").format(date);
    }
}
