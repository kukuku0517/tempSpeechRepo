package com.google.cloud.android.speech.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
    } public static void print(float [] array,String tag){
        StringBuffer buffer = new StringBuffer();

        for(float i:array){
            buffer.append(i+"\t");
        }
        Log.d(tag,buffer.toString());
    }

    public static void print(double [] array,String tag){
        StringBuffer buffer = new StringBuffer();

        for(double i:array){
            buffer.append(i+"\t");
        }
        Log.d(tag,buffer.toString());
    }

    public static void writeToFile(double[] a, String filename) {
        // Get the directory for the user's public pictures directory.
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, "Music");


        file = new File(file.getAbsolutePath(), filename + System.currentTimeMillis() + ".txt");


        // Save your stream, don't forget to flush() it before closing it.

        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append("java+=[");
            int count = 0;
            for (double aa : a) {
                if (count == 0) {
                    myOutWriter.append(String.valueOf(aa));
                    count++;
                } else {
                    myOutWriter.append("," + aa);
                }
            }
            myOutWriter.append("]");
            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }


    }

    public static void writeToFile(float[] a, String filename) {
        // Get the directory for the user's public pictures directory.
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, "Music");


        file = new File(file.getAbsolutePath(), filename + System.currentTimeMillis() + ".txt");


        // Save your stream, don't forget to flush() it before closing it.

        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append("java+=[");
            int count = 0;
            for (float aa : a) {
                if (count == 0) {
                    myOutWriter.append(String.valueOf(aa));
                    count++;
                } else {
                    myOutWriter.append("," + aa);
                }
            }
            myOutWriter.append("]");
            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }


    }

    public static void writeToFileAsWhole(double[][] a, String filename) {
        // Get the directory for the user's public pictures directory.
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, "Music");


        file = new File(file.getAbsolutePath(), filename + System.currentTimeMillis()+ ".txt");


        // Save your stream, don't forget to flush() it before closing it.
        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append("java=[");
//                int count = 0;
//                for (double aa : a) {
//                    if (count == 0) {
//                        myOutWriter.append(String.valueOf(aa));
//                        count++;
//                    } else {
//                        myOutWriter.append("," + aa);
//                    }
//                }
            int count=0;
            for (double[] dd : a) {
                if(count==0){
                    myOutWriter.append("[");
                    count++;
                }else{
                    myOutWriter.append(",[");
                }
                int count2=0;
                for (double d : dd) {
                    if(count2==0){
                        myOutWriter.append(String.valueOf(d));
                        count2++;
                    }else{
                        myOutWriter.append(","+d);
                    }
                }
                myOutWriter.append("]");
            }
            myOutWriter.append("]");
            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

}
