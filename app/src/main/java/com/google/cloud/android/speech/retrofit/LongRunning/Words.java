package com.google.cloud.android.speech.retrofit.LongRunning;

/**
 * Created by USER on 2017-11-09.
 */

public class Words
{
    private StartTime startTime;

    private String word;

    private EndTime endTime;

    public StartTime getStartTime ()
    {
        return startTime;
    }

    public void setStartTime (StartTime startTime)
    {
        this.startTime = startTime;
    }

    public String getWord ()
    {
        return word;
    }

    public void setWord (String word)
    {
        this.word = word;
    }

    public EndTime getEndTime ()
    {
        return endTime;
    }

    public void setEndTime (EndTime endTime)
    {
        this.endTime = endTime;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [startTime = "+startTime+", word = "+word+", endTime = "+endTime+"]";
    }
}
