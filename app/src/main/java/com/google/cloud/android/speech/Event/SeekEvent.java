package com.google.cloud.android.speech.Event;

/**
 * Created by USER on 2017-10-25.
 */

public class SeekEvent {
    public SeekEvent(long millis) {
        this.millis = millis;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    private long millis;
}
