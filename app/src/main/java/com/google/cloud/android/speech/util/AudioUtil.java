package com.google.cloud.android.speech.util;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Multipart;

/**
 * Created by USER on 2017-10-20.
 */

public class AudioUtil {

    private static final int AMPLITUDE_THRESHOLD = 1500;

    public static MultipartBody.Part wrap(File file){

        RequestBody surveyBody = RequestBody.create(MediaType.parse("audio/*"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), surveyBody);

       return part;

    }

    public static int getSampleRate(File file){
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(file.getAbsolutePath());
            return extractor.getTrackFormat(0).getInteger(MediaFormat.KEY_SAMPLE_RATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 44100;
    }



    public static long getAudioLength(Context context, String fileName) {
        Uri uri = Uri.parse(fileName);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long millSecond = Integer.parseInt(durationStr);
        return millSecond;
    }

    public static boolean isHearingVoice(byte[] buffer, int size) {
        for (int i = 0; i < size - 1; i += 2) {
            // The buffer has LINEAR16 in little endian.
            int s = buffer[i + 1];
            if (s < 0) s *= -1;
            s <<= 8;
            s += Math.abs(buffer[i]);
            if (s > AMPLITUDE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }


    private static float windowSize = 0.040f;
    private static float windowStep = 0.015f;

    public static int[] getSilenceFrames(float[] originalSignal, int sampleRate) {
        int samplePerFrame = (int) (sampleRate * windowSize);
        int samplePerStep = (int) (sampleRate * windowStep);

        int firstSamples = samplePerFrame * 200;
        firstSamples = originalSignal.length > firstSamples ? firstSamples : originalSignal.length;

        float[] voiced = new float[originalSignal.length];
        float sum = 0;
        double sd;
        double m;

        // 1. calculation of mean
        for (int i = 0; i < firstSamples; i++) {
            sum += originalSignal[i];
        }

        m = sum / firstSamples;// mean
        sum = 0;// reuse var for S.D.
        for (int i = 0; i < firstSamples; i++) {
            sum += Math.pow((originalSignal[i] - m), 2);
        }
        sd = Math.sqrt(sum / firstSamples);

        for (int i = 0; i < originalSignal.length; i++) {
            if ((Math.abs(originalSignal[i] - m) / sd) > 2) {
                voiced[i] = 1;
            } else {
                voiced[i] = 0;
            }
        }

        int noOfFrames;
        if (originalSignal.length < samplePerFrame) {
            noOfFrames = 1;
        } else {
            noOfFrames = 1 + (int) (Math.ceil((originalSignal.length - samplePerFrame) / samplePerStep));
        }


        int count_voiced = 0;
        int count_unvoiced = 0;
        int voicedFrame[] = new int[noOfFrames];


        for (int i = 0; i < noOfFrames - 1; i++) {
            count_voiced = 0;
            count_unvoiced = 0;
            int startIndex = i * samplePerStep;
            for (int j = 0; j < samplePerFrame; j++) {
                if (voiced[startIndex + j] == 1) {
                    count_voiced++;
                } else {
                    count_unvoiced++;
                }
            }
            if (count_voiced > count_unvoiced) {
                voicedFrame[i] = 1;
            } else {
                voicedFrame[i] = 0;
            }
        }

        for (int j = 0; j < samplePerFrame; j++) {
            count_voiced = 0;
            count_unvoiced = 0;
            if (voiced[j + (noOfFrames - 1) * samplePerStep] == 1) {
                count_voiced++;
            } else {
                count_unvoiced++;
            }
        }
        if (count_voiced > count_unvoiced) {
            voicedFrame[noOfFrames - 1] = 1;
        } else {
            voicedFrame[noOfFrames - 1] = 0;
        }


        return voicedFrame;
    }
}
