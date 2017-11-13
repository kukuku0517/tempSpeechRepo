package com.google.cloud.android.speech.data.realm.primitive;

import io.realm.RealmObject;

/**
 * Created by samsung on 2017-10-07.
 */

public class StringRealm extends RealmObject {
    private String string;

    public StringRealm(){

    }
    public StringRealm(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
