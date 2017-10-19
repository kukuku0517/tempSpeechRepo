package com.google.cloud.android.speech.Data.DTO;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableField;
import android.view.View;
import android.widget.TextView;

import com.google.cloud.android.speech.BR;
import com.google.cloud.android.speech.Util.DateUtil;

import java.util.Observable;

/**
 * Created by USER on 2017-10-12.
 */

public class MediaTimeDTO extends BaseObservable {
    int total;
    int now;

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
        String timeString = DateUtil.durationToDate(value);
        v.setText(timeString);
    }
}
