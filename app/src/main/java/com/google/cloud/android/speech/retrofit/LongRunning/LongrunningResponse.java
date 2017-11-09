package com.google.cloud.android.speech.retrofit.LongRunning;

/**
 * Created by USER on 2017-11-09.
 */
public class LongrunningResponse
{
    private Results[] results;

    public Results[] getResults ()
    {
        return results;
    }

    public void setResults (Results[] results)
    {
        this.results = results;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [results = "+results+"]";
    }
}