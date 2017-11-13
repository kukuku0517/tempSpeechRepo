package com.google.cloud.android.speech.diarization.formular;
import android.util.Log;

import com.google.cloud.android.speech.diarization.EndPointDetection;

/**
 * 预处理过程
 *
 * @author wanggang
 */
public class PreProcess {

    float[] originalSignal;// initial extracted PCM,

    public int noOfFrames;// calculated total no of frames
    int samplePerFrame;// how many samples in one frame
    public float[][] framedSignal = null;
    float[] hammingWindow;
    EndPointDetection epd;
    int samplingRate;
    private static final float windowSize = 0.025f;
    private static  final float windowStep = 0.010f;

    private int samplePerStep;

    float preEmphasisAlpha = 0.97f;

    /**
     * constructor, all steps are called frm here
     * <p>
     * <p>
     * 30; samplingFreq, typically 22Khz
     */


    public PreProcess(float[] originalSignal, int samplingRate) {
        this.originalSignal = originalSignal;
        this.samplePerFrame = (int) (samplingRate * windowSize);
        this.samplePerStep = (int) (samplingRate * windowStep);
        this.samplingRate = samplingRate;
        originalSignal = preEmphasis(originalSignal);
        doFraming();
        //        normalizePCM();
        //        epd = new EndPointDetection(this.originalSignal, this.samplingRate);
        //        afterEndPtDetection = epd.doEndPointDetection();
        //        Log.d("preprocess epd", String.valueOf(afterEndPtDetection.length));
        // ArrayWriter.printFloatArrayToFile(afterEndPtDetection, "endPt.txt");
        //		if(afterEndPtDetection.length>=samplePerFrame){
        //        doWindowing();
        //		}
    }

    private float[] preEmphasis(float inputSignal[]) {
        float outputSignal[] = new float[inputSignal.length];
        for (int n = 1; n < inputSignal.length; n++) {
            outputSignal[n] = (inputSignal[n] - preEmphasisAlpha * inputSignal[n - 1]);
        }
        return outputSignal;
    }

    private void normalizePCM() {
        float max = originalSignal[0];
        for (int i = 1; i < originalSignal.length; i++) {
            if (max < Math.abs(originalSignal[i])) {
                max = Math.abs(originalSignal[i]);
            }
        }
        // System.out.println("max PCM =  " + max);
        for (int i = 0; i < originalSignal.length; i++) {
            originalSignal[i] = originalSignal[i] / max;
        }
    }

    /**
     * divides the whole signal into frames of samplerPerFrame
     */


    private void doFraming() {
        if (originalSignal.length < samplePerFrame) {
            noOfFrames = 1;
        } else {
            noOfFrames = 1 + (int) (Math.ceil((originalSignal.length - samplePerFrame) / samplePerStep));
        }


        framedSignal = new float[noOfFrames][samplePerFrame];


        for (int i = 0; i < noOfFrames - 1; i++) {
            int startIndex = i * samplePerStep;
            for (int j = 0; j < samplePerFrame; j++) {
                framedSignal[i][j] = originalSignal[startIndex + j];
            }
        }

        for (int j = 0; j < samplePerFrame; j++) {
            if (j < originalSignal.length) {
                framedSignal[noOfFrames - 1][j] = originalSignal[(noOfFrames - 1) * samplePerStep + j];
            } else {
                framedSignal[noOfFrames - 1][j] = 0;
            }
        }
    }

    /**
     * does hamming window on each frame
     */
    private void doWindowing() {
        // prepare hammingWindow
        hammingWindow = new float[samplePerFrame + 1];
        // prepare for through out the data
        for (int i = 1; i <= samplePerFrame; i++) {
            hammingWindow[i] = (float) (0.54 - 0.46 * (Math.cos(2 * Math.PI * i / samplePerFrame)));
        }
        // do windowing
        for (int i = 0; i < noOfFrames; i++) {
            for (int j = 0; j < framedSignal[i].length; j++) {
                framedSignal[i][j] = framedSignal[i][j] * hammingWindow[j + 1];
            }
        }
    }
}
