package com.google.cloud.android.speech.Util;

import com.google.cloud.android.speech.Data.Realm.RecordRealmObject;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by samsung on 2017-10-07.
 */

public class RealmUtil<T extends RecordRealmObject> {

    public  T createObject(Realm realm, Class<T> clas){
//        realm.beginTransaction();
        RealmResults<T> result = realm.where(clas).findAll();
        int lastId;
        if(result.size()==0){
            lastId=0;
        }else{
            lastId=result.last().getId();
        }

       T record = realm.createObject(clas,lastId+1);
//        realm.commitTransaction();
        return record;
    }





}
