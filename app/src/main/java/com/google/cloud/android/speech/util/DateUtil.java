package com.google.cloud.android.speech.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by USER on 2017-10-12.
 */

public class DateUtil {

    public static String durationToTextFormat(int value) {
        int s = value / 1000 % 60;
        int m = value / 1000 / 60 % 60;
        int h = value / 1000 / 60 / 60;

        if (h > 0) {
            return String.format("%02d h %02d m %02d s", h, m, s);
        } else if (m > 0) {
            return String.format("%02d min %02d sec",  m, s);
        } else {
            return String.format("%02d sec", s);
        }
    }

    public static String durationToNumFormat(int value) {
        int s = value / 1000 % 60;
        int m = value / 1000 / 60 % 60;
        int h = value / 1000 / 60 / 60;

        if (h > 0) {
            return String.format("%02d:%02d:%02d", h, m, s);
        } else if (m > 0) {
            return String.format("%02d:%02d",  m, s);
        } else {
            return String.format("00:%02d", s);
        }
    }

    public static String dateToNumFormat(int value) {
        Date date = new Date(value);
        return new SimpleDateFormat("HH:mm").format(date);
    }

    public static String dateToTextFormat(long value) {
        Date date = new Date(value);
        return new SimpleDateFormat("yy/MM/dd").format(date);
    }
}
