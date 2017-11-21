package com.google.cloud.android.speech.view.background;

import java.util.Observable;

/**
 * Created by USER on 2017-11-21.
 */


public class RecordCompleteObservable extends Observable {
    int featureSize = 0;
    boolean recordComplete = false;

    public int isFeatureComplete() {
        return featureSize;
    }

    public void setFeatureComplete(int featureSize) {
        this.featureSize = featureSize;
        measurementsChanged();
    }

    public boolean isRecordComplete() {
        return recordComplete;
    }

    public void setRecordComplete(boolean recordComplete) {
        this.recordComplete = recordComplete;
        measurementsChanged();
    }

    public void measurementsChanged() {
        setChanged();
        notifyObservers();
    }


}
