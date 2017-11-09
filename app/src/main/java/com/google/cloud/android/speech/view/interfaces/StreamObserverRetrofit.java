package com.google.cloud.android.speech.view.interfaces;

/**
 * Created by USER on 2017-11-09.
 */

public interface StreamObserverRetrofit<T> {
    void onNext(T response);
    void onError(Throwable t);
    void onComplete(T response);
}
