package com.google.cloud.android.speech.view.background;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by USER on 2017-11-09.
 */

public abstract class LongRunningObserver implements Observer {
    @Override
    public void update(Observable o, Object arg) {
        if(o instanceof LongRunningObservable){
            LongRunningObservable obs = (LongRunningObservable) o;
            if(obs.isInit() && !obs.isSpeakerDiary() && !obs.isSpeechRecognize()){
                end();
            }
        }
    }

    public abstract void end();
}
