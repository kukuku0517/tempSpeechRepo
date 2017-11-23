package com.google.cloud.android.speech.diarization;

import android.util.Log;

import com.google.cloud.android.speech.data.realm.ClusterDataRealm;
import com.google.cloud.android.speech.data.realm.ClusterRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.VectorRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.data.realm.primitive.DoubleRealm;
import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;
import com.google.cloud.android.speech.util.LogUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.recordResult.handler.SpeakerDiaryClickListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.realm.Realm;
import io.realm.RealmList;

public class KMeansCluster {
    private int noOfCluster;
    private int featureDimension = 21;
    private int n[];
    private double classifiedData[][];
    private double centroids[][]; //initial value of centroids
    private double newCentroids[][];

    int size;
    int[] clusterNumber;
    int[] silenceNumber;
    SpeakerDiaryClickListener mListener;

    /**
     * @param noOfCluster - number of clusters
     * @param v           - vector dimension
     * @param data        - list of featureVectors
     * @param silence     - index of silence buffers
     */
    public KMeansCluster(int noOfCluster, int v, ArrayList<double[][]> data, ArrayList<int[]> silence) //noOfCluster = the number of centroids
    {
        this.noOfCluster = noOfCluster;
        this.featureDimension = v;
        this.n = new int[noOfCluster];
        this.size = 0;

        centroids = new double[noOfCluster][v];
        newCentroids = new double[noOfCluster][v];
        for (int i = 0; i < data.size(); i++) {
            size += data.get(i).length;
        }

        classifiedData = new double[size][v];
        clusterNumber = new int[size];
        silenceNumber = new int[size];

        int c = 0;
        for (double[][] dd : data) {
            System.arraycopy(dd, 0, classifiedData, c, dd.length);
            c += dd.length;
        }
        c = 0;
        for (int[] i : silence) {
            System.arraycopy(i, 0, silenceNumber, c, i.length);
            c += i.length;
        }


    }

    public void setListener(SpeakerDiaryClickListener mListener) {
        this.mListener = mListener;
    }

    public KMeansCluster(int noOfCluster, int featureDimension, RealmList<VectorRealm> data, RealmList<IntegerRealm> silence) //noOfCluster = the number of centroids
    {
        this.featureDimension = featureDimension;
        this.noOfCluster = noOfCluster;
        this.n = new int[noOfCluster];
        this.size = data.size();

        centroids = new double[noOfCluster][featureDimension];
        newCentroids = new double[noOfCluster][featureDimension];

        classifiedData = new double[size][featureDimension];
        clusterNumber = new int[size];
        silenceNumber = new int[size];


        for (int i = 0; i < size; i++) {

            RealmList<DoubleRealm> fv = data.get(i).getFeatureVector();
            int size = fv.size();
            for (int j = 0; j < size; j++) {
                classifiedData[i][j] = fv.get(j).get();
            }
        }

        int c = 0;
        for (IntegerRealm i : silence) {
            silenceNumber[c] = i.get();
            c++;
        }


    }

    public KMeansCluster(int noOfCluster, int featureDimension, double[][] data, RealmList<IntegerRealm> silence) //noOfCluster = the number of centroids
    {
        this.featureDimension = featureDimension;
        this.noOfCluster = noOfCluster;
        this.n = new int[noOfCluster];
        this.size = data.length;

        centroids = new double[noOfCluster][featureDimension];
        newCentroids = new double[noOfCluster][featureDimension];

        classifiedData = data;
        clusterNumber = new int[size];
        silenceNumber = new int[silence.size()];

        int c = 0;
        for (IntegerRealm i : silence) {
            silenceNumber[c] = i.get();
            c++;
        }


    }


    /**
     * K-Means++의 방식으로 초기 centroids값을 선정
     */
    private void initKmeansPlus() {
        Random random = new Random();
        String s = "";
        ArrayList<double[]> cents = new ArrayList<>();
        int randomIndex = random.nextInt(classifiedData.length);
        cents.add(classifiedData[randomIndex]); //첫 centroids는 랜덤
        s += randomIndex + ",";
        double[] d = new double[classifiedData.length];


        while (cents.size() < noOfCluster) { // 이후 가장 거리가 먼 점을 centroid로 선정
            for (int i = 0; i < classifiedData.length; i++) {
                double dist = Math.pow(getFurthestCentroidDistance(classifiedData[i], cents), 2);
                d[i] = dist;
            }
            int i = getByPossibility(d);
            cents.add(classifiedData[i]);
            s += i + ",";
        }
        Log.d("kcluster22", "index: " + s);

        for (int i = 0; i < noOfCluster; i++) { //선정된 centroid 저장
            for (int j = 0; j < featureDimension; j++) {
                centroids[i][j] = cents.get(i)[j];
            }
        }

    }

    /**
     * The run method essentially creates new centroids. Firstly, it resets the value of n as this counts
     * how many data objects belong to a centroid - it needs to be 0 as the centroids modify themelf at
     * every iteration. The closestCentroid variable holds the index of the closest centroid of certain data,
     * it does this using Euclidean Distance. It sums up all datum sharing the same closest centroid in order
     * to get the mean of all the data belonging to that centroid.
     * It calls the terminator method to check for stability between old and new centroids, stability will
     * cause the run method to terminate.
     * It then calls the getClassification method to assign centroids to a classication value, then print
     * output to file.
     */


    public int[] iterRun(int time) throws IOException {

        /**
         * centroids와 전체 클러스터의 error값을 map에 저장
         */

        for (int i = 0; i < time; i++) {
            double cents[][];
            double err;
            initKmeansPlus();
            cents = run();
            err = getErr();
            mListener.onSpeakerDiaryComplete(i);
            map.put(err, cents);

        }

        double min = Integer.MAX_VALUE;
        for (Map.Entry<Double, double[][]> entry : this.map.entrySet()) {
            min = Math.min(min, entry.getKey());
        }
        Log.d("kcluster2", "size" + map.size());
        String s = String.valueOf(min) + "of";
        double[][] cents = new double[0][];
        for (Map.Entry<Double, double[][]> entry : this.map.entrySet()) {
            if (entry.getKey() == min) {
                cents = entry.getValue();
//                break;
            } else {
                s += entry.getKey() + ",";
            }
        }
        Log.d("kcluster222", "details: " + s);
        return getClassification(classifiedData, cents);
    }

    /**
     * @param p
     * @return 각 data의 확률에 따라 계산하여 random하게 선택된 data의 index
     */
    private int getByPossibility(double[] p) {
        double total = 0;
        for (double i : p) {
            total += i;
        }
        double index = Math.random() * total;
        double sum = 0;
        int i = 0;
        while (sum < index) {
            sum = sum + p[i++];
        }
        return i - 1;
    }

    /**
     * @param datum
     * @param centroid
     * @return 특정 data와 특정 centroid의 거리
     */
    public double getDistance(double[] datum, double[] centroid) {
        double d = 0.0;

        for (int i = 0; i < datum.length; i++) { //calculate distance for each row of data
            d += Math.pow(datum[i] - centroid[i], 2); //Euclidean distance
        }

        return (Math.sqrt(d)); //return distance (note: only returns distances for one row of centroid)
    }

    private double min;
    private int closestIndex;

    private double d;

    public int getClosestCentroid(double[] datum) {
        min = Double.MAX_VALUE; //starts with minimum = centroids[0]
        closestIndex = -1; //-1 because 0 could be a result value
        d = Double.MAX_VALUE;

        for (int i = 0; i < centroids.length; i++) { //for each centroid
            d = getDistance(datum, centroids[i]); // between cluster centroid and object
            if (d < min) { //current distance is less than the minimum distance
                closestIndex = i; //noOfCluster is now the location of the closest centroid
                min = d;
            }
        }
        return (closestIndex); //returns index of the closest centroid for current datum
    }

    public double getClosestCentroidDistance(double[] datum) {
        double min = Double.MAX_VALUE; //starts with minimum = centroids[0]
        for (int i = 0; i < centroids.length; i++) { //for each centroid
            double d = getDistance(datum, centroids[i]); // between cluster centroid and object
            if (d < min) { //current distance is less than the minimum distance
                min = d;
            }
        }
        return min;
    }

    /**
     * @param datum
     * @param cents
     * @return get distance between furthest data and the centroids
     */
    public double getFurthestCentroidDistance(double[] datum, ArrayList<double[]> cents) {
        double max = -1;
        for (int i = 0; i < cents.size(); i++) {
            double d = getDistance(datum, cents.get(i));
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    /**
     * @param time iteration times
     * time 횟수 만큼 kcluster를 반복한후
     * 가장 최적의 cluster를 return
     */
    private HashMap<Double, double[][]> map = new HashMap<>();


    private int[] medianFilter(int[] results) {
        int filterSize = 30;
        int size = results.length;
        int filteredResult[] = new int[size];
        int filter[];

        for (int i = 0; i < size; i++) {
            int startIndex = i - filterSize / 2;
            int endIndex = i + filterSize / 2 + 1;
            if (startIndex < 0) startIndex = 0;
            if (endIndex > size) endIndex = size;
            int realsize = endIndex - startIndex;
            filter = Arrays.copyOfRange(results, startIndex, endIndex);
            Arrays.sort(filter);
            filteredResult[i] = filter[realsize / 2];
        }

        return filteredResult;
    }

    public double[][] run() {
        boolean check = false;
        int c = 0;
        int threshHold = 50;

        //수렴 할때 까지 반복(최대 : threshHold)
        while (check == false) {
            boolean result = true;
            for (int i = 0; i < noOfCluster; i++) {
                for (int j = 0; j < featureDimension; j++) {
                    newCentroids[i][j] = 0;
                }
            }
            for (int i = 0; i < noOfCluster; i++) {
                n[i] = 0;
            }

            //모든 데이터에 대해서 가까운 centroid와의 거리를 구하고 평균을 계산
            for (int i = 0; i < classifiedData.length; i++) {
                int count = getClosestCentroid(classifiedData[i]);
                for (int j = 0; j < featureDimension; j++) {
                    newCentroids[count][j] += classifiedData[i][j];
                }
                n[count]++;
            }
            for (int i = 0; i < noOfCluster; i++) {
                for (int j = 0; j < featureDimension; j++) {
                    newCentroids[i][j] = newCentroids[i][j] / n[i];
                }
            }


            //centroids가 수렴햇거나, threshold를 넘어섰을 경우
            for (int i = 0; i < noOfCluster; i++) {
                for (int j = 0; j < featureDimension; j++) {
                    if (result == true) {
                        if (newCentroids[i][j] == centroids[i][j] || ++c > threshHold) {
                            check = true;
                            result = true;
                        } else {
                            check = false;
                            result = false;
                        }
                    }
                }
            }

            for (int i = 0; i < noOfCluster; i++) {
                System.arraycopy(newCentroids[i], 0, centroids[i], 0, featureDimension);
            }
        }
        return newCentroids;
    }

    /**
     * @return total error of the cluster (by distances)
     */

    public double getErr() {
        double total = 0;
        for (int i = 0; i < classifiedData.length; i++) { //ALL data objects
            double dis = getClosestCentroidDistance(classifiedData[i]);
            total += dis;
        }
        return total;
    }

    /**
     * Prints out classification values for test (unclassified) data in both the terminal window and
     * a new file "output.txt" in the root folder. Throws an input output exception.
     */
    public int[] getClassification(double[][] unclassifiedData, double[][] centroid) throws IOException {

        for (int i = 0; i < clusterNumber.length; i++) {
            int closest = getClosestCentroid(unclassifiedData[i]);
            clusterNumber[i] = closest;
        }

        for (double[] d : centroid) {
            LogUtil.print(d, "centroids");
        }
        //TODO
        getSilenceCluster();
        LogUtil.print(clusterNumber, "kcluster");

//        LogUtil.print(clusterNumber, "kcluster");
        clusterNumber = medianFilter(clusterNumber);

        LogUtil.print(clusterNumber, "kcluster");

        return clusterNumber;
    }

    private void getSilenceCluster() {
        int[] count = new int[noOfCluster];
        for (int i = 0; i < clusterNumber.length; i++) {
            if (silenceNumber[i] == 0) {
                count[clusterNumber[i]]++;
            }
        }

        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < noOfCluster; i++) {
            if (count[i] > max) {
                max = count[i];
                maxIndex = i;
            }
        }
        for (int i = 0; i < clusterNumber.length; i++) {
            if (clusterNumber[i] == 0) {
                clusterNumber[i] = maxIndex;
            } else if (clusterNumber[i] == maxIndex) {
                clusterNumber[i] = 0;
            }
        }
    }

    public int[] getSilenceCluster(int[] results, int recordId) {
        Realm realm = Realm.getDefaultInstance();
        RecordRealm record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();

        final float RATIO = 1000 * 0.01f;
        int silence[] = new int[results.length];
        Arrays.fill(silence, 0);

        for (SentenceRealm sentence : record.getSentenceRealms()) {
            for (WordRealm word : sentence.getWordList()) {
                int start = (int) (word.getStartMillis() / RATIO);
                float end = word.getEndMillis() / RATIO;
                for (int i = start; i < end; i++) {
                    silence[i] = 1;
                }
            }
        }

        LogUtil.print(silence, "silencesilence");

        int[] count = new int[noOfCluster];
        for (int i = 0; i < clusterNumber.length; i++) {
            if (silence[i] == 0) {
                count[clusterNumber[i]]++;
            }
        }

        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < noOfCluster; i++) {
            if (count[i] > max) {
                max = count[i];
                maxIndex = i;
            }
        }
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                results[i] = maxIndex;
            } else if (results[i] == maxIndex) {
                results[i] = 0;
            }
        }

        return results;
    }


    private int getClusterForWord(WordRealm word, int[] clusters, int err, float unit) {
        boolean includeZero = false;
        err = 0;

        int UNIT = (int) (1000 * unit);
        int noOfCluster = 3;

        int start = (int) ((word.getStartMillis() + err) / UNIT);
        int end = (int) ((word.getEndMillis() + err) / UNIT);
        if (end > clusters.length) end = clusters.length;
        int clusterCount[] = new int[noOfCluster];
        Arrays.fill(clusterCount, 0);
        for (int i = start; i < end; i++) {
            int cluster = clusters[i];
            if (cluster == 0) {
                if (includeZero) {
                    clusterCount[cluster]++;
                }
            } else {
                clusterCount[cluster]++;
            }

        }
        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < noOfCluster; i++) {
            if (clusterCount[i] > max) {
                max = clusterCount[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private float getClusterRatioForeWord(WordRealm word, int[] clusters, int err, float unit) {
        boolean includeZero = false;
        err = 0;

        int UNIT = (int) (1000 * unit);
        int noOfCluster = 3;

        int start = (int) ((word.getStartMillis() + err) / UNIT);
        int end = (int) ((word.getEndMillis() + err) / UNIT);
        if (end > clusters.length) end = clusters.length;
        int clusterCount[] = new int[noOfCluster];
        Arrays.fill(clusterCount, 0);
        for (int i = start; i < end; i++) {
            int cluster = clusters[i];
            if (cluster == 0) {
                if (includeZero) {
                    clusterCount[cluster]++;
                }
            } else {
                clusterCount[cluster]++;
            }

        }
        float max = -1;
        int maxIndex = -1;
        for (int i = 0; i < noOfCluster; i++) {
            if (clusterCount[i] > max) {
                max = clusterCount[i];
                maxIndex = i;
            }
        }
        return clusterCount[1] / (float) clusters.length;
    }

    public void applyClusterToRealm(int k, int[] results, final int fileId, float unit) {

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        ClusterDataRealm clusterData = realm.where(ClusterDataRealm.class).equalTo("id", fileId).findFirst();
        if (clusterData == null) {
            clusterData = realm.createObject(ClusterDataRealm.class, fileId);
        }
        clusterData.setClusters(results);
        realm.commitTransaction();

        final RecordRealm[] fileRecord = {realm.where(RecordRealm.class).equalTo("id", fileId).findFirst()};

        /**************************************************************/

        if (fileRecord[0] != null) {
/****
 *     RecordRealm temp = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();
 RealmList<SentenceRealm> tempSentence = temp.getSentenceRealms();
 float[][] clusterRatios = new float[tempSentence.size()][];

 float averageForOne = 0;
 int count=0;
 for (int i = 0; i < tempSentence.size(); i++) {
 RealmList<WordRealm> words = tempSentence.get(i).getWordList();
 float[] sentenceRatios = new float[words.size()];
 for (int j = 0; j < words.size(); j++) {
 float ratios = getClusterRatioForeWord(words.get(j), results, 0, unit);
 sentenceRatios[j] = ratios;
 averageForOne+=ratios;
 count++;
 }
 }
 averageForOne/=(float)count;

 */
            realm.beginTransaction();
            RecordRealm temp = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();
            RealmList<SentenceRealm> tempSentence = temp.getSentenceRealms();
            float averageForOne = 0;
            int count=0;
            for (int i = 0; i < tempSentence.size(); i++) {
                RealmList<WordRealm> words = tempSentence.get(i).getWordList();
//                float[] sentenceRatios = new float[words.size()];
                for (int j = 0; j < words.size(); j++) {
                    float ratios = getClusterRatioForeWord(words.get(j), results, 0, unit);
//                    sentenceRatios[j] = ratios;
                    words.get(j).setRatioForOne(ratios);
                    averageForOne+=ratios;
                    count++;
                }
            }
            averageForOne/=(float)count;
            realm.commitTransaction();

            int sentenceIndex = 0;
            int err = 0;
            for (int i : results) {
                if (i == 0) err += unit * 1000;
                else break;
            }
            int clusterIndx;
            while (true) {
                //clusterIndex는 startSentence~endSentence(UNIT보정)사이
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();
                RealmList<SentenceRealm> sentences = record.getSentenceRealms();
                SentenceRealm sentence = sentences.get(sentenceIndex);
                /***/

                for (int i = 0; i < sentence.getWordList().size(); i++) {
                    realm.beginTransaction();
                    WordRealm word = sentence.getWordList().get(i);
//                    clusterIndx = getClusterForWord(word, results, err, unit);
                    float ratioForOne = word.getRatioForOne();
                    clusterIndx = ratioForOne<averageForOne?1:2;
                    ClusterRealm cluster;
                    if (!record.hasCluster(clusterIndx)) {
                        cluster = RealmUtil.createObject(realm, ClusterRealm.class);
                        cluster.setClusterNo(clusterIndx);
                        record.getClusterMembers().add(cluster);
                    } else {
                        cluster = record.getByClusterNo(clusterIndx);
                    }
                    realm.commitTransaction();

                    if (clusterIndx == 0) {//현재 cluster가 silence : pass
                        continue;
                    } else if (sentence.getCluster() == null || i == 0) { // 현재 문장 cluster 미지정 : 현재 clusterNumber등록후 pass
                        realm.beginTransaction();
                        sentence.setCluster(cluster);
                        realm.commitTransaction();
                        continue;
                    } else if (sentence.getCluster().getClusterNo() == clusterIndx) {//현재 cluster와 문장 cluster가 동일 : pas
                        continue;
                    } else { //기존, 현재 cluster가 다를경우 문장 분리
                        realm.beginTransaction();
                        RealmUtil.splitSentence(realm, record.getId(), sentenceIndex, sentence.getId(), word.getId(), cluster);
                        realm.commitTransaction();
                        break;
                    }
                }
                /***/

                //한문장이 끝나면 다음 문장으로
                sentenceIndex++;
                if (sentenceIndex == sentences.size()) break;

            }
        }


        /**************************************************************/

    }


}