package com.google.cloud.android.speech.Data.Realm;

import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by samsung on 2017-10-07.
 */

public interface RecordRealmObject extends RealmModel {
    int getId();
    void setId(int id);
}
