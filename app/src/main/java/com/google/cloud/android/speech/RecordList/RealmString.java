package com.google.cloud.android.speech.RecordList;

import io.realm.RealmObject;

/**
 * Created by samsung on 2017-10-07.
 */

public class RealmString extends RealmObject {
    private String string;

    public RealmString(){

    }
    public RealmString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
