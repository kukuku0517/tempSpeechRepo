package com.google.cloud.android.speech.diarization;

import android.util.Log;

import com.google.cloud.android.speech.diarization.formular.Delta;
import com.google.cloud.android.speech.diarization.formular.Energy;
import com.google.cloud.android.speech.diarization.formular.MFCC;
import com.google.cloud.android.speech.util.AudioUtil;

import java.util.Arrays;


/**
 * 特征提取
 *
 * @author wanggang
 */
@SuppressWarnings("unused")
public class FeatureExtract {

    private float[][] framedSignal;
    private int noOfFrames;

    /**
     * how many mfcc coefficients per frame
     */
    private int numCepstra = 13;

//    private double[][] featureVector;
    private double[][] mfccFeature;
    private double[][] deltaMfcc;
    private double[][] deltaDeltaMfcc;
    private double[] energyVal;
    private double[] deltaEnergy;
    private double[] deltaDeltaEnergy;
    private int sampleRate;
    private FeatureVector fv;
    private MFCC mfcc;
    //	private Delta delta;
//	private Energy en;
    int featureDimension;
    // FeatureVector fv;

    /**
     * constructor of feature extract
     *
     * @param framedSignal   2-D audio signal obtained after framing

     */
    public FeatureExtract(float[][] framedSignal, int samplingRate, int featureDimension) {
        this.featureDimension = featureDimension;
        this.framedSignal = framedSignal;
        this.noOfFrames = framedSignal.length;
        this.sampleRate = samplingRate;
        mfcc = new MFCC(samplingRate, numCepstra,featureDimension);
        fv = new FeatureVector();
        mfccFeature = new double[noOfFrames][featureDimension];

    }

    public void setSilence() {
        fv.setSilence(AudioUtil.getSilenceFrames(framedSignal));
    }

    public void setSilence(float[] origin) {
        fv.setSilence(AudioUtil.getSilenceFrames(origin, sampleRate));
    }

    public FeatureVector getFeatureVector() {
        return fv;
    }

    /**
     * generates feature vector by combining mfcc, and its delta and delta
     * deltas also contains energy and its deltas
     */
    public void makeMfccFeatureVector() {
        calculateMFCC();
//		doCepstralMeanNormalization();
        // delta
//		delta.setRegressionWindow(2);// 2 for delta
//		deltaMfcc = delta.performDelta2D(mfccFeature);
//		// delta delta
//		delta.setRegressionWindow(1);// 1 for delta delta
//		deltaDeltaMfcc = delta.performDelta2D(deltaMfcc);
//		// energy
//		energyVal = en.calcEnergy(framedSignal);
//
//		delta.setRegressionWindow(1);
//		// energy delta
//		deltaEnergy = delta.performDelta1D(energyVal);
//		delta.setRegressionWindow(1);
//		// energy delta delta
//		deltaDeltaEnergy = delta.performDelta1D(deltaEnergy);
//        for (int i = 0; i < framedSignal.length; i++) {
//            for (int j = 0; j < numCepstra; j++) {
//                featureVector[i][j] = mfccFeature[i][j];
//            }
//			for (int j = numCepstra; j < 2 * numCepstra; j++) {
//				featureVector[i][j] = deltaMfcc[i][j - numCepstra];
//			}
//			for (int j = 2 * numCepstra; j < 3 * numCepstra; j++) {
//				featureVector[i][j] = deltaDeltaMfcc[i][j - 2 * numCepstra];
//			}
//			featureVector[i][numCepstra] = energyVal[i];
//			featureVector[i][numCepstra + 1] = deltaEnergy[i];
//			featureVector[i][numCepstra + 2] = deltaDeltaEnergy[i];
//        }
//        fv.setMfccFeature(mfccFeature);
        fv.setFeatureVector(mfccFeature);
        System.gc();
    }

    /**
     * calculates MFCC coefficients of each frame
     */
    private void calculateMFCC() {
        Log.d("cepc", String.valueOf(noOfFrames));
        for (int i = 0; i < noOfFrames; i++) {
            // for each frame i, make mfcc from current framed signal

            mfccFeature[i] = mfcc.doMFCC(framedSignal[i], i);// 2D data
            mfccFeature[i][0] = zeroCrossingRate(framedSignal[i])*100;
            mfccFeature[i][1] = energy(framedSignal[i])*10;
            mfccFeature[i][2] = energyEntropy(framedSignal[i])*10;


//			LogUtil.maxInArray(mfccFeature[i],"after dct");
        }
//		LogUtil.writeToFileAsWhole(mfccFeature,"afterdccccc");
    }

    /**
     * performs cepstral mean substraction. <br>
     * it removes channel effect...
     */
    private void doCepstralMeanNormalization() {
        double sum;
        double mean;
        double mCeps[][] = new double[noOfFrames][numCepstra - 1];// same size
        // as mfcc
        // 1.loop through each mfcc coeff
        for (int i = 0; i < numCepstra - 1; i++) {
            // calculate mean
            sum = 0.0;
            for (int j = 0; j < noOfFrames; j++) {
                sum += mfccFeature[j][i];// ith coeff of all frame
            }
            mean = sum / noOfFrames;
            // subtract
            for (int j = 0; j < noOfFrames; j++) {
                mCeps[j][i] = mfccFeature[j][i] - mean;
            }
        }
    }


    //1. zeroCrossingRate (+.- 변동수)
    private double zeroCrossingRate(float[] frame) {
        int len = frame.length;
        double total = 0;
        float prev, now;
        for (int i = 1; i < len; i++) {
            prev = frame[i - 1];
            now = frame[i];
            if ((prev >= 0 && now < 0) || (prev < 0 && now >= 0)) {
                total++;
            }
        }
        return total / 2 / (len - 1);
    }

    //2. energy (magnitude 제곱합)
    private double energy(float[] frame) {
        float total = 0;
        for (float i : frame) {
            total += i * i;
        }
//        return total / frame.length;

        return 0;
    }

    private final double EPS = 0.0000001f;

    private double energyEntropy(float[] frame) {
        int numOfBlock = 10;

        float energy = 0;
        for (float f : frame)
            energy += f*f;
        int len = frame.length;
        int subLen = len / 10;
        float[] subSum = new float[numOfBlock];
        for (int i = 0; i < numOfBlock; i++) {
            subSum[i] = 0;
            for (int j = 0; j < subLen; j++) {
                subSum[i] += Math.pow(frame[i * subLen + j], 2);
            }
            subSum[i] /= (energy + EPS);
        }

        float total = 0;
        for (float f : subSum) {
            total += -f * logBase(f + EPS, 2);
        }

        return total;
    }

    public static double logBase(double x, double base) {
        return Math.log(x) / Math.log(base);
    }


}
