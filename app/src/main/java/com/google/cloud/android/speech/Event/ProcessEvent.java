package com.google.cloud.android.speech.Event;

/**
 * Created by USER on 2017-10-18.
 */

public class ProcessEvent {
    int id;
    int type;
    public static final int RECORD=0;
    public static final int FILE=1;

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public ProcessEvent(int id,int type){
        this.id=id;
        this.type=type;
    }
}
