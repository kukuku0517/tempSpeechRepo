package com.google.cloud.android.speech.longRunning;

import java.util.Observable;

/**
 * Created by USER on 2017-11-09.
 */

public class recognizeObservable extends Observable {
    boolean speakerDiary=false;
    boolean recognize =false;
    boolean init=false;

    public boolean isInit() {
        return init;

    }

    public void setInit(boolean init) {
        this.init = init;
        measurementsChanged();
    }

    public boolean isSpeakerDiary() {
        return speakerDiary;
    }

    public void setSpeakerDiary(boolean speakerDiary) {
        this.speakerDiary = speakerDiary;
        measurementsChanged();
    }

    public boolean isRecognize() {
        return recognize;
    }

    public void setRecognize(boolean recognize) {
        this.recognize = recognize;
        measurementsChanged();
    }


    public void measurementsChanged(){
        setChanged();
        notifyObservers();
    }


}
