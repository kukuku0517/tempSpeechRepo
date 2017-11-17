package com.google.cloud.android.speech.data;

import android.databinding.BindingAdapter;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.event.PartialStatusEvent;
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


    @BindingAdapter("statusColor")
    public static void onPartialStatusChange(ImageView v, int status) {

        switch (status) {
            case PartialStatusEvent.SILENCE:
            case PartialStatusEvent.END:
                v.setColorFilter(ContextCompat.getColor(v.getContext(), R.color.light_gray), PorterDuff.Mode.SRC_ATOP);
                break;
            case PartialStatusEvent.VOICE:
                v.setColorFilter(ContextCompat.getColor(v.getContext(),R.color.red), PorterDuff.Mode.SRC_ATOP);
                break;

        }
    }
}
