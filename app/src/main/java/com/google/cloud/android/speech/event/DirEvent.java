package com.google.cloud.android.speech.event;

/**
 * Created by USER on 2017-11-18.
 */

public class DirEvent {
    public DirEvent(int id) {
        this.id = id;
    }

    int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
