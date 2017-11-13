package com.google.cloud.android.speech.longRunning;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by USER on 2017-11-09.
 */

public abstract class RecognizeObserver implements Observer {
    @Override
    public void update(Observable o, Object arg) {
        if(o instanceof recognizeObservable){
            recognizeObservable obs = (recognizeObservable) o;
            if(obs.isInit() && !obs.isSpeakerDiary() && !obs.isRecognize()){
                end();
            }
        }
    }

    public abstract void end();
}
