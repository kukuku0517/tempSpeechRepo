package com.google.cloud.android.speech.data;

import android.databinding.BindingAdapter;
import android.widget.TextView;

import com.google.cloud.android.speech.util.DateUtil;

/**
 * Created by USER on 2017-11-15.
 */

public class BindingAdapters {

    @BindingAdapter("durationText")
    public static void setSentence(TextView v, long value) {
        v.setText(DateUtil.durationToTextFormat((int) value));
    }

    @BindingAdapter("durationNum")
    public static void setDuration(TextView v, long value) {
        v.setText(DateUtil.durationToNumFormat((int) value));
    }

    @BindingAdapter("dateText")
    public static void setDate(TextView v, long value) {
        v.setText(DateUtil.dateToTextFormat(value));
    }

    @BindingAdapter("dateNum")
    public static void setDateTime(TextView v, long value) {
        v.setText(DateUtil.dateToNumFormat((int) value));
    }

}
