package com.google.cloud.android.speech.data.realm.primitive;

import io.realm.RealmObject;

/**
 * Created by USER on 2017-11-13.
 */

public class DoubleRealm extends RealmObject {
  double i;

    public double get() {
        return i;
    }

    public void set(double i) {
        this.i = i;
    }
}
