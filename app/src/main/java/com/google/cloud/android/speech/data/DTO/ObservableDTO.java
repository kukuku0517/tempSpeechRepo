package com.google.cloud.android.speech.data.DTO;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.android.databinding.library.baseAdapters.BR;

/**
 * Created by samsung on 2017-10-22.
 */

public class ObservableDTO<T> extends BaseObservable {

    T value;

    @Bindable
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        notifyPropertyChanged(BR.value);
    }


}
