/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.prototype;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.DataPoint;
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
public class ParameterOptimization {

    DataPointsAnalysis zeroToTen = new DataPointsAnalysis(new RangeType().min(0.0).max(10.0));
    DataPointsAnalysis tenToTwenty = new DataPointsAnalysis(new RangeType().min(10.0).max(20.0));
    DataPointsAnalysis twentyToThirty = new DataPointsAnalysis(new RangeType().min(20.0).max(30.0));
    DataPointsAnalysis thirtyToForty = new DataPointsAnalysis(new RangeType().min(30.0).max(40.0));
    DataPointsAnalysis fortyToFifty = new DataPointsAnalysis(new RangeType().min(40.0).max(50.0));
    DataPointsAnalysis fiftyToSixty = new DataPointsAnalysis(new RangeType().min(50.0).max(60.0));
    DataPointsAnalysis sixtyToSeventy = new DataPointsAnalysis(new RangeType().min(60.0).max(70.0));
    DataPointsAnalysis seventyToEighty = new DataPointsAnalysis(new RangeType().min(70.0).max(80.0));
    DataPointsAnalysis eightyToNinety = new DataPointsAnalysis(new RangeType().min(80.0).max(90.0));
    DataPointsAnalysis ninetyToHundred = new DataPointsAnalysis(new RangeType().min(90.0).max(100.0));

    public void performParameterResolver(@NotNull List<DataPoint> chunkMap, int minIntersection) {
        HashMap<Integer, RangeSimilarity> map = new HashMap<>();

        int size = chunkMap.size();
        for (int i = 0; i < size; i++) {
            map.put(i, new RangeSimilarity());
        }

        for (int firstLoopIndex = 0; firstLoopIndex < size; firstLoopIndex++) {
            DataPoint dataPoint = chunkMap.get(firstLoopIndex);
            Set<String> properties = dataPoint.getProperties();
            int membersCount = dataPoint.getMembersCount();

            for (int secondLoopIndex = firstLoopIndex + 1; secondLoopIndex < chunkMap.size(); secondLoopIndex++) {
                DataPoint dataPoint2 = chunkMap.get(secondLoopIndex);
                Set<String> properties2 = dataPoint2.getProperties();

                int membersCount2 = dataPoint2.getMembersCount();
                int comparedMembers = membersCount + membersCount2;

                computeMetric(minIntersection, properties, properties2, comparedMembers, firstLoopIndex, secondLoopIndex, map);

            }
        }

        calculateMappedSimilarGroup(map, chunkMap);
    }

    public void setupStatisticalData(double similarityPercentage,
            int comparedProperties,
            int comparedMembers,
            int intersectionCount,
            double density,
            int firstLoopIndex,
            int secondLoopIndex,
            @NotNull HashMap<Integer, RangeSimilarity> map) {

        if (similarityPercentage >= 90) {
            ninetyToHundred.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateNinetyToHundred();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateNinetyToHundred();
            }
        } else if (similarityPercentage >= 80) {
            eightyToNinety.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateEightyToNinety();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateEightyToNinety();
            }
        } else if (similarityPercentage >= 70) {
            seventyToEighty.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateSeventyToEighty();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateSeventyToEighty();
            }
        } else if (similarityPercentage >= 60) {
            sixtyToSeventy.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateSixtyToSeventy();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateSixtyToSeventy();
            }
        } else if (similarityPercentage >= 50) {
            fiftyToSixty.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateFiftyToSixty();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateFiftyToSixty();
            }
        } else if (similarityPercentage >= 40) {
            fortyToFifty.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateFortyToFifty();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateFortyToFifty();
            }
        } else if (similarityPercentage >= 30) {
            thirtyToForty.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateThirtyToForty();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateThirtyToForty();
            }
        } else if (similarityPercentage >= 20) {
            twentyToThirty.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateTwentyToThirty();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateTwentyToThirty();
            }
        } else if (similarityPercentage >= 10) {
            tenToTwenty.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateTenToTwenty();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateTenToTwenty();
            }
        } else {
            zeroToTen.iterateChunkSuccess(
                    comparedProperties,
                    comparedMembers,
                    similarityPercentage,
                    intersectionCount,
                    density);

            RangeSimilarity rangeSimilarity = map.get(firstLoopIndex);
            rangeSimilarity.iterateZeroToTen();
            if (firstLoopIndex != secondLoopIndex) {
                map.get(secondLoopIndex).iterateZeroToTen();
            }
        }
    }

    public void computeMetric(int minIntersection,
            @NotNull Set<String> valueA,
            @NotNull Set<String> valueB,
            int comparedMembers,
            int firstLoopIndex,
            int secondLoopIndex,
            HashMap<Integer, RangeSimilarity> map) {
        int intersectionCount = 0;
        int setBunique = 0;

        int propertiesSizeA = valueA.size();
        int propertiesSizeB = valueB.size();
        int comparedProperties = propertiesSizeA + propertiesSizeB;

        double similarityPercentage;

        if (valueA.size() > valueB.size()) {
            for (String num : valueB) {
                if (valueA.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            if (intersectionCount < minIntersection) {
                similarityPercentage = 0;
            } else {
                similarityPercentage = ((double) intersectionCount / (valueA.size() + setBunique)) * 100;
            }

        } else {

            for (String num : valueA) {
                if (valueB.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            if (intersectionCount < minIntersection) {
                similarityPercentage = 0;
            } else {
                similarityPercentage = ((double) intersectionCount / (valueB.size() + setBunique)) * 100;
            }

        }

        double density = 100.0;
        if (setBunique != 0 && intersectionCount != 0) {
            density = ((double) intersectionCount / (setBunique + intersectionCount) * 100);
        }

        setupStatisticalData(similarityPercentage,
                comparedProperties,
                comparedMembers,
                intersectionCount,
                density,
                firstLoopIndex,
                secondLoopIndex,
                map);
    }

    public void calculateMappedSimilarGroup(@NotNull HashMap<Integer, RangeSimilarity> map, @NotNull List<DataPoint> chunkMap) {
        AssociationAnalyseObject ninetyToHundred = new AssociationAnalyseObject();
        AssociationAnalyseObject eightyToNinety = new AssociationAnalyseObject();
        AssociationAnalyseObject seventyToEighty = new AssociationAnalyseObject();
        AssociationAnalyseObject sixtyToSeventy = new AssociationAnalyseObject();
        AssociationAnalyseObject fiftyToSixty = new AssociationAnalyseObject();
        AssociationAnalyseObject fortyToFifty = new AssociationAnalyseObject();
        AssociationAnalyseObject thirtyToForty = new AssociationAnalyseObject();
        AssociationAnalyseObject twentyToThirty = new AssociationAnalyseObject();
        AssociationAnalyseObject tenToTwenty = new AssociationAnalyseObject();
        AssociationAnalyseObject zeroToTen = new AssociationAnalyseObject();

        map.forEach((key, value) -> {
            DataPoint dataPoint = chunkMap.get(key);
            int membersCount = dataPoint.getMembersCount();
            ninetyToHundred.resolveMinMaxAverage(value.getNinetyToHundred(), membersCount);
            eightyToNinety.resolveMinMaxAverage(value.getEightyToNinety(), membersCount);
            seventyToEighty.resolveMinMaxAverage(value.getSeventyToEighty(), membersCount);
            sixtyToSeventy.resolveMinMaxAverage(value.getSixtyToSeventy(), membersCount);
            fiftyToSixty.resolveMinMaxAverage(value.getFiftyToSixty(), membersCount);
            fortyToFifty.resolveMinMaxAverage(value.getFortyToFifty(), membersCount);
            thirtyToForty.resolveMinMaxAverage(value.getThirtyToForty(), membersCount);
            twentyToThirty.resolveMinMaxAverage(value.getTwentyToThirty(), membersCount);
            tenToTwenty.resolveMinMaxAverage(value.getTenToTwenty(), membersCount);
            zeroToTen.resolveMinMaxAverage(value.getZeroToTen(), membersCount);
        });

        this.ninetyToHundred.setMinMaxAverage(ninetyToHundred);
        this.eightyToNinety.setMinMaxAverage(eightyToNinety);
        this.seventyToEighty.setMinMaxAverage(seventyToEighty);
        this.sixtyToSeventy.setMinMaxAverage(sixtyToSeventy);
        this.fiftyToSixty.setMinMaxAverage(fiftyToSixty);
        this.fortyToFifty.setMinMaxAverage(fortyToFifty);
        this.thirtyToForty.setMinMaxAverage(thirtyToForty);
        this.twentyToThirty.setMinMaxAverage(twentyToThirty);
        this.tenToTwenty.setMinMaxAverage(tenToTwenty);
        this.zeroToTen.setMinMaxAverage(zeroToTen);
    }

}
