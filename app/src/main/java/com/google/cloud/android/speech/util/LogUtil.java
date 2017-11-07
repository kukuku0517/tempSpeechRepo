package com.google.cloud.android.speech.util;

import android.util.Log;

/**
 * Created by USER on 2017-11-07.
 */

public class LogUtil {
    public static void print(int [] array,String tag){
        StringBuffer buffer = new StringBuffer();

        for(int i:array){
            buffer.append(i+"\t");
        }
        Log.d(tag,buffer.toString());
    }
}
