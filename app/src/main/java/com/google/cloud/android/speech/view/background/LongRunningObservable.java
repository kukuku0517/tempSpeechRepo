package com.google.cloud.android.speech.view.background;

import java.util.Observable;

/**
 * Created by USER on 2017-11-09.
 */

public class LongRunningObservable extends Observable {
    boolean speakerDiary=false;
    boolean speechRecognize=false;
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

    public boolean isSpeechRecognize() {
        return speechRecognize;
    }

    public void setSpeechRecognize(boolean speechRecognize) {
        this.speechRecognize = speechRecognize;
        measurementsChanged();
    }


    public void measurementsChanged(){
        setChanged();
        notifyObservers();
    }


}
