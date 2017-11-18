package com.google.cloud.android.speech.data;

import android.databinding.BindingAdapter;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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


    @BindingAdapter("focus")
    public static void setFocus(TextView v, boolean focus) {
        if (focus) {
            v.setTextColor(v.getContext().getResources().getColor(R.color.text_black));
            v.setTypeface(null, Typeface.BOLD);
        } else {
            v.setTextColor(v.getContext().getResources().getColor(R.color.text_gray));
            v.setTypeface(null, Typeface.NORMAL);
        }
    }

    @BindingAdapter("droppable")
    public static void setFocus(LinearLayout v, int focus) {
        switch (focus) {
            case 0:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.default_background));
                break;
            case 1:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.red));
                break;
            case 2:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.light_gray));
                break;
        }
    }

    @BindingAdapter("droppable")
    public static void setFocus(RelativeLayout v, int focus) {
        switch (focus) {
            case 0:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.default_background));
                break;
            case 1:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.red));
                break;
            case 2:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.light_gray));
                break;
        }
    }

    @BindingAdapter("clusterNumber")
    public static void setSentence(TextView v, String value) {
        v.setText(value);
    }

    @BindingAdapter("clusterColor")
    public static void setClusterColor(ImageView v, int value) {
        switch (value) {
            case 0:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.default_background));
                break;
            case 1:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.cluster_a));
                break;
            case 2:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.cluster_b));
                break;
        }
    }
}
