package com.google.cloud.android.speech.data.realm;

import io.realm.RealmModel;

/**
 * Created by samsung on 2017-10-07.
 */

public interface RecordRealmObject extends RealmModel {
    int getId();
    void setId(int id);
}
