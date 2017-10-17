package com.google.cloud.android.speech.View.Recording;

/**
 * Created by USER on 2017-10-17.
 */

public class PartialEvent {
    String partial;

    PartialEvent(String partial){
        this.partial=partial;
    }
    public String getPartial() {
        return partial;
    }
}
