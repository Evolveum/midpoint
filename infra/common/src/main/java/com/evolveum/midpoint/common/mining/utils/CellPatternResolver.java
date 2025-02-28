/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.objects.detection.SimpleHeatPattern;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * The `PatternResolver` class implements the `DetectionOperation` interface and provides
 * the algorithms for performing user-based and role-based pattern detection within the
 * role analysis process.
 * <p>
 * This class plays a crucial role in identifying patterns within the analyzed data, assisting
 * in making informed decisions about role and user assignments.
 * NOTE: There is possibility to specify required properties and allowed properties that must be present or meet in found pattern.
 */
public class CellPatternResolver implements Serializable {

    public static class Connection {

        List<String> members;
        List<String> properties;

        MiningBaseTypeChunk memberChunk;
        MiningBaseTypeChunk propertyChunk;

        public Connection(List<String> members, List<String> properties,
                MiningBaseTypeChunk memberChunk, MiningBaseTypeChunk propertyChunk) {
            this.members = members;
            this.properties = properties;
            this.memberChunk = memberChunk;
            this.propertyChunk = propertyChunk;
        }

        public List<String> getMembers() {
            return members;
        }

        public List<String> getProperties() {
            return properties;
        }

        public MiningBaseTypeChunk getMemberChunk() {
            return memberChunk;
        }

        public MiningBaseTypeChunk getPropertyChunk() {
            return propertyChunk;
        }
    }


    public <T extends MiningBaseTypeChunk> List<SimpleHeatPattern> performSingleCellDetection(
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<T> miningBaseTypeChunks,
            @NotNull PatternDetectionOption detectionOption,
            List<String> requiredProperties,
            List<String> allowedProperties) {

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

        List<T> preparedObjects = new ArrayList<>();

        prepareObjects(miningBaseTypeChunks,
                preparedObjects,
                requiredProperties,
                allowedProperties,
                minFrequency,
                maxFrequency,
                minIntersection,
                minOccupancy,
                userBasedDetection);

        //first level of intersection
        List<List<String>> outerIntersections = outerPatternDetection(
                preparedObjects,
                minIntersection,
                minOccupancy);

        //from there we calculate inner intersection
        Set<List<String>> innerIntersections = new HashSet<>();

        //last inner intersection that was found (tmp variable for innerIntersections calculation)
        List<List<String>> result = new ArrayList<>(outerIntersections);

        //there we identify all intersections that can be found in the data (this is true if remainsOperation is disabled)
        boolean calculate = true;
        int remainsOperation = 10;
        while (calculate && remainsOperation > 0) {
            result = new ArrayList<>(innerPatternDetection(
                    result,
                    minIntersection, preparedObjects));

            if (result.isEmpty()) {
                calculate = false;
            } else {
                innerIntersections.addAll(result);
            }
            remainsOperation--;
        }

        Set<List<String>> allPossibleIntersections = new HashSet<>();
        allPossibleIntersections.addAll(outerIntersections);
        allPossibleIntersections.addAll(innerIntersections);

        List<SimpleHeatPattern> simpleHeatPatterns = new ArrayList<>();
        int key = 0;
        for (List<String> references : allPossibleIntersections) {
            simpleHeatPatterns.add(new SimpleHeatPattern(references, key++));
        }

        //now we need map intersection to miningBaseTypeChunk
        for (T miningBaseTypeChunk : miningBaseTypeChunks) {
            List<String> properties = miningBaseTypeChunk.getProperties();
            for (SimpleHeatPattern pattern : simpleHeatPatterns) {
                if (pattern.isPartOf(new HashSet<>(properties))) {
                    //there we map intersection to miningBaseTypeChunk
                    //TODO simplify this
                    //there we calculate total relations over miningBaseTypeChunk for each pattern
                    int countOfMembers = miningBaseTypeChunk.getMembers().size();
                    int countOfIntersectedProperties = pattern.getPropertiesCount();
                    int totalRelations = countOfMembers * countOfIntersectedProperties;
                    pattern.incrementTotalRelations(totalRelations);
                }
            }
        }
        return simpleHeatPatterns;
    }

    /**
     * Prepares mining base type chunks for pattern detection based on specified thresholds
     * and analysis parameters.
     * <p>
     * Also, if chunk group is bigger than minOccupancy and properties
     * of chunk is bigger than minIntersection,
     * it will be added to intersections.
     *
     * @param miningBaseTypeChunks A list of mining base type chunks to be prepared for analysis.
     * @param preparedObjects A list to store prepared mining base type chunks.
     * @param minFrequency The minimum frequency threshold for chunk analysis.
     * @param maxFrequency The maximum frequency threshold for chunk analysis.
     * @param minIntersection The minimum number of intersections required for analysis.
     * @param minOccupancy The minimum occupancy threshold for analysis.
     * @param userBasedDetection A boolean indicating whether user-based detection is applied.
     * If true, user-based detection is applied; otherwise, role-based detection.
     * @param <T> Generic type extending MiningBaseTypeChunk.
     */
    private static <T extends MiningBaseTypeChunk> void prepareObjects(
            @NotNull List<T> miningBaseTypeChunks,
            @NotNull List<T> preparedObjects,
            List<String> requiredMembers,
            List<String> allowedProperties,
            double minFrequency,
            double maxFrequency,
            int minIntersection,
            int minOccupancy,
            boolean userBasedDetection) {
        for (T chunk : miningBaseTypeChunks) {
            //TODO temporary ignore frequency. Think about it
//            FrequencyItem frequencyItem = chunk.getFrequencyItem();
//            double frequency = frequencyItem.getFrequency();

//            if (frequency < minFrequency || frequency > maxFrequency) {
//                continue;
//            }

            List<String> chunkProperties = chunk.getProperties();
            Set<String> members = new HashSet<>(chunkProperties);
            if (members.size() < minIntersection) {
                continue;
            }

            if (requiredMembers != null && !requiredMembers.isEmpty()) {
                MiningBaseTypeChunk newChunk;
                if (members.containsAll(requiredMembers)) {
                    if (allowedProperties != null && !allowedProperties.isEmpty()) {
                        if (chunk instanceof MiningRoleTypeChunk) {
                            newChunk = new MiningRoleTypeChunk(chunk);
                        } else {
                            newChunk = new MiningUserTypeChunk(chunk);
                        }

                        @SuppressWarnings("unchecked") T preparedChunk = (T) newChunk;

                        preparedChunk.getProperties().retainAll(allowedProperties);
                        preparedObjects.add(preparedChunk);

                    }else {
                        preparedObjects.add(chunk);
                    }
                }
            } else {
                preparedObjects.add(chunk);
            }
        }
    }


    /**
     * Detects and retrieves inner patterns from outer intersections based on intersection thresholds
     * and role analysis progress handling.
     *
     * @param outerIntersectionsList The list of outer intersections for reference.
     * @param minIntersection The minimum number of intersections required for analysis.
     * @return A set of lists representing inner intersections derived from outer intersections.
     */
    @NotNull
    private static <T extends MiningBaseTypeChunk> Set<List<String>> innerPatternDetection(
            @NotNull List<List<String>> outerIntersectionsList,
            int minIntersection, @NotNull List<T> preparedObjects) {
        Set<List<String>> innerIntersections = new HashSet<>();
        for (int i = 0; i < outerIntersectionsList.size(); i++) {

            Set<String> pointsA = new HashSet<>(outerIntersectionsList.get(i));

            //TODO proof
            for (int j = i + 1; j < preparedObjects.size(); j++) {
                Set<String> intersection = new HashSet<>(preparedObjects.get(j).getProperties());
                intersection.retainAll(pointsA);
                if (intersection.size() >= minIntersection && intersection.size() != pointsA.size()) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    innerIntersections.add(nInter);
                }

            }

            //TODO im not sure if needed
//            for (int j = i + 1; j < outerIntersectionsList.size(); j++) {
//                Set<String> intersection = new HashSet<>(outerIntersectionsList.get(j));
//                intersection.retainAll(pointsA);
//
//                if (intersection.size() >= minIntersection) {
//                    List<String> nInter = new ArrayList<>(intersection);
//                    Collections.sort(nInter);
//                    innerIntersections.add(nInter);
//                }
//
//            }
        }
        return innerIntersections;
    }

    /**
     * Detects and retrieves outer patterns from prepared objects based on intersection thresholds
     * and role analysis progress handling.
     *
     * @param <T> Generic type extending MiningBaseTypeChunk.
     * @param preparedObjects A list of prepared mining base type chunks.
     * @param minIntersection The minimum number of intersections required for analysis.
     * @param minOccupancy The minimum occupancy threshold for analysis.
     * @return A set of lists representing outer intersections detected in the prepared objects.
     */
    @NotNull
    private static <T extends MiningBaseTypeChunk> List<List<String>> outerPatternDetection(
            @NotNull List<T> preparedObjects,
            int minIntersection, int minOccupancy) {
        Set<List<String>> outerIntersections = new HashSet<>();
        for (int i = 0; i < preparedObjects.size(); i++) {
            Set<String> pointsA = new HashSet<>(preparedObjects.get(i).getProperties());
            int countOfMembers = preparedObjects.get(i).getMembers().size();
            //TODO check it
            if (countOfMembers >= minOccupancy && pointsA.size() >= minIntersection) {
                List<String> nInter = new ArrayList<>(pointsA);
                Collections.sort(nInter);
                outerIntersections.add(nInter);
            }
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
