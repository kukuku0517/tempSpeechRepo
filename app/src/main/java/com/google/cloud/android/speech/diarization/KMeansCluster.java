package com.google.cloud.android.speech.diarization;

import com.google.cloud.android.speech.data.realm.ClusterDataRealm;
import com.google.cloud.android.speech.data.realm.ClusterRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.VectorRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
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
    private int featureDimension=21;
    private int n[];
    private double classifiedData[][];
    private double centroids[][]; //initial value of centroids
    private double newCentroids[][];

    int size;
    int[] clusterNumber;
    int[] silenceNumber;
    SpeakerDiaryClickListener mListener;

    /**
     * @param noOfCluster       - number of clusters
     * @param v       - vector dimension
     * @param data    - list of featureVectors
     * @param silence - index of silence buffers
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

    public void setListener(SpeakerDiaryClickListener mListener){
        this.mListener=mListener;
    }
    public KMeansCluster(int noOfCluster, int featureDimension, RealmList<VectorRealm> data, RealmList<IntegerRealm> silence) //noOfCluster = the number of centroids
    {
        this.featureDimension=featureDimension;
        this.noOfCluster = noOfCluster;
        this.n = new int[noOfCluster];
        this.size = data.size();

        centroids = new double[noOfCluster][featureDimension];
        newCentroids = new double[noOfCluster][featureDimension];

        classifiedData = new double[size][featureDimension];
        clusterNumber = new int[size];
        silenceNumber = new int[size];


        for (int i = 0; i < size; i++) {
            for (int j = 0; j < data.get(i).getFeatureVector().size(); j++) {
                classifiedData[i][j] = data.get(i).getFeatureVector().get(j).get();
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
        this.featureDimension=featureDimension;
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
        ArrayList<double[]> cents = new ArrayList<>();
        cents.add(classifiedData[random.nextInt(classifiedData.length)]); //첫 centroids는 랜덤
        double[] d = new double[classifiedData.length];

        while (cents.size() < noOfCluster) { // 이후 가장 거리가 먼 점을 centroid로 선정
            for (int i = 0; i < classifiedData.length; i++) {
                double dist = Math.pow(getFurthestCentroidDistance(classifiedData[i], cents), 2);
                d[i] = dist;
            }
            int i = getByPossibility(d);
            cents.add(classifiedData[i]);
        }

        for (int i = 0; i < noOfCluster; i++) { //선정된 centroid 저장
            for (int j = 0; j < featureDimension; j++) {
                centroids[i][j] = cents.get(i)[j];
            }
        }

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


    /**
     * @param time iteration times
     * time 횟수 만큼 kcluster를 반복한후
     * 가장 최적의 cluster를 return
     */
    private HashMap<double[][], Double> map = new HashMap<>();

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
            filteredResult[i] = filter[realsize/2];
        }

        return filteredResult;
    }


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
            map.put(cents, err);
        }

        double min = Integer.MAX_VALUE;
        for (Map.Entry<double[][], Double> entry : this.map.entrySet()) {
            min = Math.min(min, entry.getValue());
        }

        double[][] cents = new double[0][];
        for (Map.Entry<double[][], Double> entry : this.map.entrySet()) {
            if (entry.getValue() == min) {
                cents = entry.getKey();
                break;
            }
        }

        return getClassification(classifiedData, cents);
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
        return centroids;
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

        //TODO
//        getSilenceCluster();
//        LogUtil.print(clusterNumber, "kcluster");
        clusterNumber = medianFilter(clusterNumber);

        LogUtil.print(clusterNumber, "kcluster");

        return clusterNumber;
    }

    private void getSilenceCluster() {
        int[] count = new int[noOfCluster];
        for (int i = 0; i < clusterNumber.length; i++) {
            if (silenceNumber[i] == 1) {
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

    private  int getClusterForWord(WordRealm word, int[] clusters, int err, float unit) {
        int UNIT = (int) (1000 * unit);
        int noOfCluster = 3;
//        err=0;
        int start = (int) ((word.getStartMillis() + err) / UNIT);
        int end = (int) ((word.getEndMillis() + err) / UNIT);
        if (end > clusters.length) end = clusters.length;
        int clusterCount[] = new int[noOfCluster];
        for (int i = 0; i < noOfCluster; i++) {
            clusterCount[i] = 0;
        }
        for (int i = start; i < end; i++) {
            clusterCount[clusters[i]]++;
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
                    WordRealm word = sentence.getWordList().get(i);
                    clusterIndx = getClusterForWord(word, results, err,unit);
                    realm.beginTransaction();
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
                    } else if (sentence.getCluster() == null || sentence.getCluster().getClusterNo() == 0) { // 현재 문장 cluster 미지정 : 현재 clusterNumber등록후 pass
                        realm.beginTransaction();
                        sentence.setCluster(cluster);
                        realm.commitTransaction();
                        continue;
                    } else if (sentence.getCluster().getClusterNo() == clusterIndx) {//현재 cluster와 문장 cluster가 동일 : pas
                        continue;
                    } else { //기존, 현재 cluster가 다를경우 문장 분리

                        realm.beginTransaction();
                        SentenceRealm origin = sentence;
                        SentenceRealm add = RealmUtil.createObject(realm, SentenceRealm.class);

                        add.setEndMillis(origin.getEndMillis());
                        origin.setEndMillis(word.getStartMillis());
                        add.setStartMillis(word.getStartMillis());
                        add.setCluster(cluster);

                        RealmList<WordRealm> originWords = new RealmList<>();
                        RealmList<WordRealm> addWords = new RealmList<>();

                        RealmList<WordRealm> words = sentence.getWordList();
                        for (int j = 0; j < words.size(); j++) {
                            if (j < i) {
                                originWords.add(words.get(j));
                            } else {
                                addWords.add(words.get(j));
                            }
                        }

                        origin.setWordList(originWords);
                        add.setWordList(addWords);
                        origin.setSentence();
                        add.setSentence();
                        sentences.add(sentenceIndex + 1, add);
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
        mListener.onSpeakerDiaryComplete();

        /**************************************************************/

    }




}