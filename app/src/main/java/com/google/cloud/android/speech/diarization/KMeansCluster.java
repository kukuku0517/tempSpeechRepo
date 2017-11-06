package com.google.cloud.android.speech.diarization;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Kmeans聚类算法的实现类，将newsgroups文档集聚成10类、20类、30类
 * 算法结束条件:当每个点最近的聚类中心点就是它所属的聚类中心点时，算法结束
 *
 * @author yangliu
 * @qq 772330184
 * @mail yang.liu@pku.edu.cn
 */

/**
 * The "KMeansClustering" class applies the k-means clustering learning algorithm on the data read in the
 * "Data" class. This involves initiating a number of centroids, setting their positions and assigning
 * them to the nearest clumps of training (classified) data. Then, the clumped test (unclassified) data are
 * assigned to the nearest centroid and so assigning them with the training data of that clump. This then
 * allows for a classification to be derived for every test datum.
 *
 * @author Karim Tabet, modified from "Toy K-Means" code by Chris Thornton
 * @version 1.0 24/11/2010
 */

public class KMeansCluster {
    private int k;
    private int v;
    private int n[];
    private ArrayList<double[][]> data = new ArrayList<>();
    private double classifiedData[][];
    private double centroids[][]; //initial value of centroids
    private double newCentroids[][];
    ArrayList<Integer> silence;
    int[] frameSize;
    //    int[][] clusterIndex;
    int[][] clusterIndex2;

    /**
     * @param k       - number of clusters
     * @param v       - vector dimension
     * @param data    - list of featureVectors
     * @param silence - index of silence buffers
     */
    public KMeansCluster(int k, int v, ArrayList<double[][]> data, ArrayList<Integer> silence) //k = the number of centroids
    {
        this.k = k;
        this.v = v;
        this.n = new int[k];
        this.data = data;
        this.silence = silence;
        centroids = new double[k][v];
        newCentroids = new double[k][v];
        int size = 0;
        int max = -1;

        for (int i = 0; i < silence.size(); i++) {
            int s = silence.get(i);
            size += s;
            if (max < s) {
                max = s;
            }
        }

        classifiedData = new double[size][39];
        frameSize = new int[silence.size()];
        int c = 0;

        for (double[][] dd : data) {
            System.arraycopy(dd, 0, classifiedData, c, dd.length);
            c += dd.length;
        }


        /**
         * silence인 구간을 표시해주기 위함.
         * 각 클러스터의 array크기를 가장큰 buffer의 크기로 맞춰주고
         * silence인 부분은 -1로 채워진다.
         */
         clusterIndex2 = new int[silence.size()][max];
        for (int[] i : clusterIndex2) {
            for (int j = 0; j < i.length; j++) {
                i[j] = -1;
            }
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

        while (cents.size() < k) { // 이후 가장 거리가 먼 점을 centroid로 선정
            for (int i = 0; i < classifiedData.length; i++) {
                double dist = Math.pow(getFurthestCentroidDistance(classifiedData[i], cents), 2);
                d[i] = dist;
            }
            int i = getByPossibility(d);
            cents.add(classifiedData[i]);
        }

        for (int i = 0; i < k; i++) { //선정된 centroid 저장
            for (int j = 0; j < v; j++) {
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
                closestIndex = i; //k is now the location of the closest centroid
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

    public int[][] iterRun(int time) throws IOException {

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
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < v; j++) {
                    newCentroids[i][j] = 0;
                }
            }
            for (int i = 0; i < k; i++) {
                n[i] = 0;
            }

            //모든 데이터에 대해서 가까운 centroid와의 거리를 구하고 평균을 계산
            for (int i = 0; i < classifiedData.length; i++) {
                int count = getClosestCentroid(classifiedData[i]);
                for (int j = 0; j < v; j++) {
                    newCentroids[count][j] += classifiedData[i][j];
                }
                n[count]++;
            }
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < v; j++) {
                    newCentroids[i][j] = newCentroids[i][j] / n[i];
                }
            }


            //centroids가 수렴햇거나, threshold를 넘어섰을 경우
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < v; j++) {
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

            for (int i = 0; i < k; i++) {
                System.arraycopy(newCentroids[i], 0, centroids[i], 0, v);
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
    public int[][] getClassification(double[][] unclassifiedData, double[][] centroid) throws IOException {

        int count = 0;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < clusterIndex2.length; i++) {
            if (silence.get(i) == 0) {

            } else {
                for (int j = 0; j < silence.get(i); j++) {
                    int closest = getClosestCentroid(unclassifiedData[count]);
                    clusterIndex2[i][j] = 5 + closest * 5;
                    count++;
                }
            }
        }

        for (int a[] : clusterIndex2) {
            for (int i = 0; i < a.length; i += 1) {
                buffer.append("\t" + String.valueOf(a[i]));
            }
        }
        Log.d("kcluster", buffer.toString());

        return clusterIndex2;
    }
}