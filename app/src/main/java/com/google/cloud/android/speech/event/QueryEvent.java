package com.google.cloud.android.speech.event;

/**
 * Created by USER on 2017-11-20.
 */

public class QueryEvent {
    String query;

    public QueryEvent(String s) {
        this.query = s;
    }

    public String getQuery() {

        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
