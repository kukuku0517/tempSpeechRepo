package com.google.cloud.android.speech.Util;

import com.google.cloud.android.speech.Data.Realm.RecordRealmObject;
import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by samsung on 2017-10-07.
 */

public class RealmUtil {

    public static <T extends RecordRealmObject> T createObject(Realm realm, Class<T> clas) {
//        realm.beginTransaction();
        RealmResults<T> result = realm.where(clas).findAll();
        int lastId;
        if (result.size() == 0) {
            lastId = 0;
        } else {
            lastId = result.last().getId();
        }

        T record = realm.createObject(clas, lastId + 1);
//        realm.commitTransaction();
        return record;
    }

    public static void updateWordRealm(WordRealm word, SentenceRealm sentence, String newWord) {
        word.setWord(newWord);
        StringBuilder builder = new StringBuilder();
        for (WordRealm wordRealm : sentence.getWordList()) {
            builder.append(wordRealm.getWord() + " ");
        }
        sentence.setSentence(builder.toString());

    }


}
