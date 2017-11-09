package com.google.cloud.android.speech.retrofit.LongRunning;

/**
 * Created by USER on 2017-11-09.
 */

public class Results
{
    private Alternatives[] alternatives;

    public Alternatives[] getAlternatives ()
    {
        return alternatives;
    }

    public void setAlternatives (Alternatives[] alternatives)
    {
        this.alternatives = alternatives;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [alternatives = "+alternatives+"]";
    }
}
