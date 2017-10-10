package com.google.cloud.android.speech.RealmData;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static android.os.Build.VERSION_CODES.M;

/**
 * Created by samsung on 2017-10-07.
 */

public class SentenceRealm extends RealmObject implements RecordRealmObject {

    @PrimaryKey
    private int id;

    private   RealmList<WordRealm> wordList;
    private long startMillis;

    public RealmList<WordRealm> getWordList() {
        return wordList;
    }

    public void setWordList(RealmList<WordRealm> wordList) {
        this.wordList = wordList;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }


    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

}
