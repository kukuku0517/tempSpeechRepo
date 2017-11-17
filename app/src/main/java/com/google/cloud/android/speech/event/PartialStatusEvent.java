package com.google.cloud.android.speech.event;

import android.databinding.BindingAdapter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import java.util.Random;

/**
 * Created by USER on 2017-11-17.
 */

public class PartialStatusEvent {

    public static final int SILENCE = 0;
    public static final int VOICE = 1;
    public static final int END = 2;

    private int status = 0;

    public PartialStatusEvent(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


}
