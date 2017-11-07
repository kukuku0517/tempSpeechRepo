package com.google.cloud.android.speech.diarization.main;

import android.util.Log;


import com.google.cloud.android.speech.diarization.FeatureExtract;
import com.google.cloud.android.speech.diarization.formular.PreProcess;
import com.google.cloud.android.speech.diarization.FeatureVector;
import com.google.cloud.android.speech.util.AudioUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SpeechDiary {

    //	private static FormatControlConf fc = new FormatControlConf();
    private static final int SAMPLING_RATE = 44100; // (int) fc.getRate();
    // int samplePerFrame = 256; // 16ms for 8 khz
    private static final int SAMPLE_PER_FRAME = 1024; // 512,23.22ms
    private static final int FEATURE_DIMENSION = 39;
    private FeatureExtract featureExtract;
    //	private WaveData waveData;
    private PreProcess prp;
    private List<double[]> allFeaturesList = new ArrayList<double[]>();

    private static final String BASE_DIR = "data";

    public FeatureVector extractFeatureFromFile(float[] arrAmp) {
//		float[] arrAmp;
//		arrAmp = waveData.extractAmplitudeFromFile(speechFile);

        prp = new PreProcess(arrAmp, SAMPLING_RATE);
        Log.d("preprocess origin", String.valueOf(arrAmp.length));
//        Log.d("preprocess prp", String.valueOf(prp.framedSignal.length));

        if (prp.framedSignal[0].length >= SAMPLE_PER_FRAME && prp.framedSignal.length > 1) {
            featureExtract = new FeatureExtract(prp.framedSignal, SAMPLING_RATE, SAMPLE_PER_FRAME);
            featureExtract.makeMfccFeatureVector();
            Log.d("preprocess fv", String.valueOf(featureExtract.getFeatureVector().getFeatureVector().length));
            featureExtract.setSilence(arrAmp);
            return featureExtract.getFeatureVector();
        } else {

            return null;
        }
    }

    public FeatureVector extractFeatureFromFile(File speechFile) {
        float[] arrAmp = new float[0];
//		arrAmp = waveData.extractAmplitudeFromFile(speechFile);
        prp = new PreProcess(arrAmp,  SAMPLING_RATE);
        featureExtract = new FeatureExtract(prp.framedSignal, SAMPLING_RATE, SAMPLE_PER_FRAME);
        featureExtract.makeMfccFeatureVector();
        return featureExtract.getFeatureVector();
    }

}
