package com.evolveum.midpoint.common.mining.utils.values;

import java.io.Serializable;

public class ZScoreData implements Serializable {
    double sum = 0;
    int dataSize = 0;
    double mean = 0;
    double sumSquaredDiff = 0;
    double variance = 0;
    double stdDev = 0;

    public ZScoreData(double sum, int dataSize, double mean, double sumSquaredDiff, double variance, double stdDev) {
        this.sum = sum;
        this.dataSize = dataSize;
        this.mean = mean;
        this.sumSquaredDiff = sumSquaredDiff;
        this.variance = variance;
        this.stdDev = stdDev;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getSumSquaredDiff() {
        return sumSquaredDiff;
    }

    public void setSumSquaredDiff(double sumSquaredDiff) {
        this.sumSquaredDiff = sumSquaredDiff;
    }

    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public double getStdDev() {
        return stdDev;
    }

    public void setStdDev(double stdDev) {
        this.stdDev = stdDev;
    }

}
