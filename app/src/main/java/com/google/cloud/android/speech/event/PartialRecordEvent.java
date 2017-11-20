package com.google.cloud.android.speech.event;

/**
 * Created by USER on 2017-10-19.
 */

public class PartialRecordEvent {
    int second;

    public PartialRecordEvent(int second){
        this.second=second;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }
}
