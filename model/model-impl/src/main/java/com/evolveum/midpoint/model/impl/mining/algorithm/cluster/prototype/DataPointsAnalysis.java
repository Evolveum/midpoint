/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.prototype;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;

/**
 * This class is part of ongoing research and is intended for research purposes only.
 * It contains experimental functionalities and methods developed for investigative purposes.
 * Please note that the methods and behaviors within this class may not follow conventional
 * implementation standards and should not be used in production environments.
 *
 * <p>This class serves as a platform for testing and exploring novel ideas and algorithms.
 */

// TODO delete after finish research
public class DataPointsAnalysis {

    RangeType rangeSimilarity;
    int chunkSuccess = 0;
    double averageObjects = 0;
    double averageProperties = 0;
    double averageDensity = 0;
    double averageSimilarity = 0;
    double averageIntersection = 0;
    AssociationAnalyseObject associationAnalyseObject = new AssociationAnalyseObject();

    public DataPointsAnalysis(RangeType rangeSimilarity) {
        this.rangeSimilarity = rangeSimilarity;
    }

    public void iterateChunkSuccess(double processedPropertiesCount,
            double processedObjectsCount,
            double processedSimilarity,
            double processedIntersection,
            double density) {
        int chunkSuccessOld = chunkSuccess;
        int chunkSuccessNew = chunkSuccess + 1;

        averageProperties = calculateAverage(chunkSuccessOld, chunkSuccessNew, averageProperties, processedPropertiesCount);

        averageObjects = calculateAverage(chunkSuccessOld, chunkSuccessNew, averageObjects, processedObjectsCount);

        averageSimilarity = calculateAverage(chunkSuccessOld, chunkSuccessNew, averageSimilarity, processedSimilarity);

        averageIntersection = calculateAverage(chunkSuccessOld, chunkSuccessNew, averageIntersection, processedIntersection);

        averageDensity = calculateAverage(chunkSuccessOld, chunkSuccessNew, averageDensity, density);

        this.chunkSuccess++;

    }

    private double calculateAverage(int chunkSuccessOld,
            int chunkSuccessNew,
            double averageForUpgrade,
            double newValueForAverage) {
        double sumOfObjects = averageForUpgrade * chunkSuccessOld;
        sumOfObjects += newValueForAverage;
        return sumOfObjects / chunkSuccessNew;
    }

    public int getChunkSuccess() {
        return chunkSuccess;
    }

    public void setChunkSuccess(int chunkSuccess) {
        this.chunkSuccess = chunkSuccess;
    }

    public double getAverageObjects() {
        return averageObjects;
    }

    public void setAverageObjects(double averageObjects) {
        this.averageObjects = averageObjects;
    }

    public double getAverageProperties() {
        return averageProperties;
    }

    public void setAverageProperties(double averageProperties) {
        this.averageProperties = averageProperties;
    }

    public void prepareAverageProperties(double averageProperties) {
        this.averageProperties = averageProperties;
    }

    public double getAverageDensity() {
        return averageDensity;
    }

    public void setAverageDensity(double averageDensity) {
        this.averageDensity = averageDensity;
    }

    public double getAverageSimilarity() {
        return averageSimilarity;
    }

    public void setAverageSimilarity(double averageSimilarity) {
        this.averageSimilarity = averageSimilarity;
    }

    public double getAverageIntersection() {
        return averageIntersection;
    }

    public void setAverageIntersection(double averageIntersection) {
        this.averageIntersection = averageIntersection;
    }

    public AssociationAnalyseObject getMinMaxAverage() {
        return associationAnalyseObject;
    }

    public void setMinMaxAverage(AssociationAnalyseObject associationAnalyseObject) {
        this.associationAnalyseObject = associationAnalyseObject;
    }

}
