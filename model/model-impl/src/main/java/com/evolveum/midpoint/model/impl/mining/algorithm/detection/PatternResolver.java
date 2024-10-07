/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.prepareDetectedPattern;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

/**
 * The `PatternResolver` class implements the `DetectionOperation` interface and provides
 * the algorithms for performing user-based and role-based pattern detection within the
 * role analysis process.
 * <p>
 * This class plays a crucial role in identifying patterns within the analyzed data, assisting
 * in making informed decisions about role and user assignments.
 */
public class PatternResolver implements DetectionOperation, Serializable {

    @Override
    public <T extends MiningBaseTypeChunk> @NotNull List<DetectedPattern> performDetection(
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<T> miningBaseTypeChunks,
            @NotNull PatternDetectionOption detectionOption,
            @NotNull RoleAnalysisProgressIncrement handler) {

        handler.enterNewStep("Pattern Detection");
        handler.setActive(true);
        handler.setOperationCountToProcess(miningBaseTypeChunks.size());

        double minFrequency = detectionOption.getMinFrequencyThreshold() / 100;
        double maxFrequency = detectionOption.getMaxFrequencyThreshold() / 100;

        int minIntersection;
        int minOccupancy;
        boolean userBasedDetection = false;

        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            userBasedDetection = true;
            minIntersection = detectionOption.getMinUsers();
            minOccupancy = detectionOption.getMinRoles();
        } else {
            minIntersection = detectionOption.getMinRoles();
            minOccupancy = detectionOption.getMinUsers();
        }

        List<DetectedPattern> intersections = new ArrayList<>();
        List<T> preparedObjects = new ArrayList<>();

        prepareObjects(handler, miningBaseTypeChunks,
                preparedObjects, intersections, minFrequency,
                maxFrequency,
                minIntersection,
                minOccupancy,
                userBasedDetection);

        List<List<String>> outerIntersections = outerPatternDetection(handler,
                preparedObjects,
                minIntersection);

        Set<List<String>> innerIntersections = innerPatternDetection(handler,
                outerIntersections,
                minIntersection);

        innerPatternPreparation(handler,
                outerIntersections,
                preparedObjects,
                userBasedDetection,
                minOccupancy,
                intersections
        );

        outerPatterPreparation(handler,
                innerIntersections,
                outerIntersections,
                preparedObjects,
                intersections, minOccupancy,
                userBasedDetection);

        return intersections;

    }

    /**
     * Prepares mining base type chunks for pattern detection based on specified thresholds
     * and analysis parameters.
     * <p>
     * Also, if chunk group is bigger than minOccupancy and properties
     * of chunk is bigger than minIntersection,
     * it will be added to intersections.
     *
     * @param handler The progress handler for the role analysis.
     * @param miningBaseTypeChunks A list of mining base type chunks to be prepared for analysis.
     * @param preparedObjects A list to store prepared mining base type chunks.
     * @param intersections A list to store detected patterns.
     * @param minFrequency The minimum frequency threshold for chunk analysis.
     * @param maxFrequency The maximum frequency threshold for chunk analysis.
     * @param minIntersection The minimum number of intersections required for analysis.
     * @param minOccupancy The minimum occupancy threshold for analysis.
     * @param userBasedDetection A boolean indicating whether user-based detection is applied.
     * If true, user-based detection is applied; otherwise, role-based detection.
     * @param <T> Generic type extending MiningBaseTypeChunk.
     */
    private static <T extends MiningBaseTypeChunk> void prepareObjects(
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull List<T> miningBaseTypeChunks,
            @NotNull List<T> preparedObjects,
            @NotNull List<DetectedPattern> intersections,
            double minFrequency,
            double maxFrequency,
            int minIntersection,
            int minOccupancy,
            boolean userBasedDetection) {
        for (T chunk : miningBaseTypeChunks) {
            handler.iterateActualStatus();

            FrequencyItem frequencyItem = chunk.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }
            Set<String> members = new HashSet<>(chunk.getProperties());
            if (members.size() < minIntersection) {
                continue;
            }
            preparedObjects.add(chunk);

            Set<String> properties = new HashSet<>(chunk.getMembers());
            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(userBasedDetection
                        ? prepareDetectedPattern(properties, members)
                        : prepareDetectedPattern(members, properties));
            }
        }
    }

    /**
     * Prepares outer patterns based on inner intersections, prepared objects, and analysis parameters,
     * considering role-based or user-based detection.
     *
     * @param handler The progress handler for the role analysis.
     * @param innerIntersections The set of inner intersections for reference.
     * @param outerIntersectionsList The list of outer intersections to be prepared.
     * @param preparedObjects A list of prepared mining base type chunks.
     * @param intersections A list to store detected patterns.
     * @param minOccupancy The minimum occupancy threshold for analysis.
     * @param userBasedDetection A boolean indicating whether user-based detection is applied.
     * If true, user-based detection is applied; otherwise, role-based detection.
     * @param <T> Generic type extending MiningBaseTypeChunk.
     */
    private static <T extends MiningBaseTypeChunk> void outerPatterPreparation(
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Set<List<String>> innerIntersections,
            @NotNull List<List<String>> outerIntersectionsList,
            @NotNull List<T> preparedObjects,
            @NotNull List<DetectedPattern> intersections,
            int minOccupancy,
            boolean userBasedDetection) {
        handler.enterNewStep("Outer Pattern Preparation");
        handler.setOperationCountToProcess(innerIntersections.size());

        for (List<String> members : innerIntersections) {
            handler.iterateActualStatus();

            if (outerIntersectionsList.contains(members)) {
                continue;
            }

            Set<String> properties = new HashSet<>();
            for (T preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getProperties());
                if (basicUsers.containsAll(members)) {
                    properties.addAll(preparedObject.getMembers());
                }
            }

            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(userBasedDetection
                        ? prepareDetectedPattern(properties, new HashSet<>(members))
                        : prepareDetectedPattern(new HashSet<>(members), properties));
            }
        }
    }

    /**
     * Prepares inner patterns based on outer intersections, prepared objects, and analysis parameters,
     * considering role-based or user-based detection.
     *
     * @param handler The progress handler for the role analysis.
     * @param outerIntersectionsList The list of outer intersections for reference.
     * @param preparedObjects A list of prepared mining base type chunks.
     * @param userBasedDetection A boolean indicating whether user-based detection is applied.
     * If true, user-based detection is applied; otherwise, role-based detection.
     * @param minOccupancy The minimum occupancy threshold for analysis.
     * @param intersections A list to store detected patterns.
     * @param <T> Generic type extending MiningBaseTypeChunk.
     */
    private static <T extends MiningBaseTypeChunk> void innerPatternPreparation(
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull List<List<String>> outerIntersectionsList,
            @NotNull List<T> preparedObjects,
            boolean userBasedDetection, int minOccupancy,
            @NotNull List<DetectedPattern> intersections) {
        handler.enterNewStep("Inner Pattern Preparation");
        handler.setOperationCountToProcess(outerIntersectionsList.size());

        for (List<String> members : outerIntersectionsList) {
            handler.iterateActualStatus();

            Set<String> properties = new HashSet<>();
            for (T preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getProperties());
                if (basicUsers.containsAll(members)) {
                    properties.addAll(preparedObject.getMembers());
                }
            }

            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(userBasedDetection
                        ? prepareDetectedPattern(properties, new HashSet<>(members))
                        : prepareDetectedPattern(new HashSet<>(members), properties));
            }
        }
    }

    /**
     * Detects and retrieves inner patterns from outer intersections based on intersection thresholds
     * and role analysis progress handling.
     *
     * @param handler The progress handler for the role analysis.
     * @param outerIntersectionsList The list of outer intersections for reference.
     * @param minIntersection The minimum number of intersections required for analysis.
     * @return A set of lists representing inner intersections derived from outer intersections.
     */
    @NotNull
    private static Set<List<String>> innerPatternDetection(
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull List<List<String>> outerIntersectionsList,
            int minIntersection) {
        handler.enterNewStep("Inner Detection");
        handler.setOperationCountToProcess(outerIntersectionsList.size());
        Set<List<String>> innerIntersections = new HashSet<>();
        for (int i = 0; i < outerIntersectionsList.size(); i++) {
            handler.iterateActualStatus();

            Set<String> pointsA = new HashSet<>(outerIntersectionsList.get(i));
            for (int j = i + 1; j < outerIntersectionsList.size(); j++) {
                Set<String> intersection = new HashSet<>(outerIntersectionsList.get(j));
                intersection.retainAll(pointsA);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    innerIntersections.add(nInter);
                }

            }
        }
        return innerIntersections;
    }

    /**
     * Detects and retrieves outer patterns from prepared objects based on intersection thresholds
     * and role analysis progress handling.
     *
     * @param handler The progress handler for the role analysis.
     * @param preparedObjects A list of prepared mining base type chunks.
     * @param minIntersection The minimum number of intersections required for analysis.
     * @param <T> Generic type extending MiningBaseTypeChunk.
     * @return A set of lists representing outer intersections detected in the prepared objects.
     */
    @NotNull
    private static <T extends MiningBaseTypeChunk> List<List<String>> outerPatternDetection(
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull List<T> preparedObjects,
            int minIntersection) {
        handler.enterNewStep("Outer Pattern Detection");
        handler.setOperationCountToProcess(preparedObjects.size());

        Set<List<String>> outerIntersections = new HashSet<>();
        for (int i = 0; i < preparedObjects.size(); i++) {
            handler.iterateActualStatus();

            Set<String> pointsA = new HashSet<>(preparedObjects.get(i).getProperties());
            for (int j = i + 1; j < preparedObjects.size(); j++) {
                Set<String> intersection = new HashSet<>(preparedObjects.get(j).getProperties());
                intersection.retainAll(pointsA);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    outerIntersections.add(nInter);
                }

            }
        }
        return new ArrayList<>(outerIntersections);
    }

}
