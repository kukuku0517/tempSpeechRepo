package com.google.cloud.android.speech.Data.Realm;

import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.TextView;

import com.google.cloud.android.speech.Util.DateUtil;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by samsung on 2017-10-07.
 */

public class SentenceRealm extends RealmObject implements RecordRealmObject {

    @PrimaryKey
    private int id;

    private   RealmList<WordRealm> wordList;
    private int startMillis;

    public RealmList<WordRealm> getWordList() {
        return wordList;
    }

    public void setWordList(RealmList<WordRealm> wordList) {
        this.wordList = wordList;
    }

    public int getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(int startMillis) {
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

    @BindingAdapter("android:text")
    public static void setText(TextView v, int value){
        v.setText(DateUtil.durationToDate(value));
    }
}
