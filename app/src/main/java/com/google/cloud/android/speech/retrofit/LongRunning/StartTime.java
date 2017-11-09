package com.google.cloud.android.speech.retrofit.LongRunning;

/**
 * Created by USER on 2017-11-09.
 */

public class StartTime
{
    private long nanos;

    private long seconds;

    public long getNanos ()
    {
        return nanos;
    }

    public void setNanos (long nanos)
    {
        this.nanos = nanos;
    }

    public long getSeconds ()
    {
        return seconds;
    }

    public void setSeconds (long seconds)
    {
        this.seconds = seconds;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [nanos = "+nanos+", seconds = "+seconds+"]";
    }
}
