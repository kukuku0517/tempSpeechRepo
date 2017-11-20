package com.google.cloud.android.speech.event;

/**
 * Created by USER on 2017-11-20.
 */

public class PartialFileEvent {
    String message;

    public PartialFileEvent(String message) {
        this.message = message;
    }

    public String getMessage() {

        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
