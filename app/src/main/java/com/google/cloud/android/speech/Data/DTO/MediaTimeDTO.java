package com.google.cloud.android.speech.Data.DTO;

import android.databinding.BindingAdapter;
import android.databinding.ObservableField;
import android.view.View;
import android.widget.TextView;

import com.google.cloud.android.speech.Util.DateUtil;

import java.util.Observable;

/**
 * Created by USER on 2017-10-12.
 */

public class MediaTimeDTO {
    ObservableField<Integer> total = new ObservableField<>();
    ObservableField<Integer> now = new ObservableField<>();

    public ObservableField<Integer> getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total.set(total);
    }

    public ObservableField<Integer> getNow() {
        return now;
    }

    public void setNow(int now) {
        this.now.set(now);
    }

    @BindingAdapter("android:text")
   public static void setText(TextView v, int value){
        String timeString = DateUtil.durationToDate(value);
        v.setText(timeString);
    }
}
