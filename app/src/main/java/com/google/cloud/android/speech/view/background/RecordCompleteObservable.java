package com.google.cloud.android.speech.view.background;

import java.util.Observable;

/**
 * Created by USER on 2017-11-21.
 */


public class RecordCompleteObservable extends Observable {
    int featureSize = 0;
    int requestSize = 0;

    public int getFeatureSize() {
        return featureSize;
    }

    public int getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(int requestSize) {
        this.requestSize = requestSize;
        measurementsChanged();
    }

    public void setFeatureSize(int featureSize) {
        this.featureSize = featureSize;
        measurementsChanged();
    }

    boolean recordComplete = false;
    boolean init = false;

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
        measurementsChanged();
    }

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
