package com.google.cloud.android.speech.data.realm.primitive;

import io.realm.RealmObject;

/**
 * Created by USER on 2017-11-09.
 */

public class IntegerRealm extends RealmObject {
    int i;

    public int get() {
        return i;
    }

    public void set(int i) {
        this.i = i;
    }
}
