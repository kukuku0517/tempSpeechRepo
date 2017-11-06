package com.google.cloud.android.speech.event;

/**
 * Created by USER on 2017-10-17.
 */

public class PartialEvent {
    String partial;

    public PartialEvent(String partial){
        this.partial=partial;
    }
    public String getPartial() {
        return partial;
    }
}
