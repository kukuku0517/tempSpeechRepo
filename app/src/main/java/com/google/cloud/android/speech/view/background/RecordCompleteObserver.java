package com.google.cloud.android.speech.view.background;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by USER on 2017-11-21.
 */

public abstract class RecordCompleteObserver implements Observer {
    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof RecordCompleteObservable) {
            RecordCompleteObservable obj = (RecordCompleteObservable) o;
            if(obj.featureSize==0&&obj.isRecordComplete()){
                onComplete();
            }
        }
    }

    public abstract void onComplete();
}
