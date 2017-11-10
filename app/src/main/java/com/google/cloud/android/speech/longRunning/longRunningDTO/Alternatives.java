package com.google.cloud.android.speech.longRunning.longRunningDTO;

/**
 * Created by USER on 2017-11-09.
 */

public class Alternatives
{
    private String transcript;

    private Words[] words;

    private String confidence;

    public String getTranscript ()
    {
        return transcript;
    }

    public void setTranscript (String transcript)
    {
        this.transcript = transcript;
    }

    public Words[] getWords ()
    {
        return words;
    }

    public void setWords (Words[] words)
    {
        this.words = words;
    }

    public String getConfidence ()
    {
        return confidence;
    }

    public void setConfidence (String confidence)
    {
        this.confidence = confidence;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [transcript = "+transcript+", words = "+words+", confidence = "+confidence+"]";
    }
}
