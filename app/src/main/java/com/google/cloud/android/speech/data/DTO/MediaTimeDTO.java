package com.google.cloud.android.speech.data.DTO;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.widget.TextView;

import com.google.cloud.android.speech.BR;
import com.google.cloud.android.speech.util.DateUtil;

/**
 * Created by USER on 2017-10-12.
 */

public class MediaTimeDTO extends BaseObservable {
    int total=0;
    int now=0;

    @Bindable
    public int getTotal() {
        return total;
    }

    @Bindable
    public int getNow() {
        return now;
    }

    public void setTotal(int total) {
        this.total = total;
        notifyPropertyChanged(BR.total);
    }


    public void setNow(int now) {

        this.now = now;
        notifyPropertyChanged(BR.now);
    }

    @BindingAdapter("android:text")
    public static void setText(TextView v, int value) {
        String timeString = DateUtil.durationToTextFormat(value);
        v.setText(timeString);
    }
}
