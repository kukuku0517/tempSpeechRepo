package com.google.cloud.android.speech.data.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by samsung on 2017-10-07.
 */

public class WordRealm  extends RealmObject implements PrimaryRealm {

    @PrimaryKey
    private int id;
    private String word;
    private boolean highlight=false;
    private long startMillis;
    private long endMillis;
    private int sentenceId;
    private float ratioForOne=0;

    public float getRatioForOne() {
        return ratioForOne;
    }

    public void setRatioForOne(float ratioForOne) {
        this.ratioForOne = ratioForOne;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public void setEndMillis(long endMillis) {
        this.endMillis = endMillis;
    }

    public int getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(int sentenceId) {
        this.sentenceId = sentenceId;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
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
