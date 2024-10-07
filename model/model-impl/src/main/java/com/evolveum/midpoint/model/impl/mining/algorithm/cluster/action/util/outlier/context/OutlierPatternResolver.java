/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.context;

import java.io.Serializable;
import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.SimpleHeatPattern;

/**
 * The `PatternResolver` class implements the `DetectionOperation` interface and provides
 * the algorithms for performing user-based and role-based pattern detection within the
 * role analysis process.
 * <p>
 * This class plays a crucial role in identifying patterns within the analyzed data, assisting
 * in making informed decisions about role and user assignments.
 */

//TODO experimental class
//NOTE this class is not used (just experiment)
public class OutlierPatternResolver implements Serializable {

    public <T extends MiningBaseTypeChunk> List<SimpleHeatPattern> performSingleAnomalyCellDetection(
            @NotNull List<T> miningBaseTypeChunks,
            @NotNull PatternDetectionOption detectionOption,
            List<String> requiredProperties,
            List<String> allowedProperties) {

        int minIntersection;
        int minOccupancy;
        minIntersection = detectionOption.getMinUsers();
        minOccupancy = detectionOption.getMinRoles();

        List<T> preparedObjects = new ArrayList<>();

        prepareObjects(miningBaseTypeChunks,
                preparedObjects,
                requiredProperties,
                allowedProperties,
                minIntersection
        );

        //first level of intersection
        List<List<String>> outerIntersections = outerPatternDetection(
                preparedObjects,
                minIntersection,
                minOccupancy);

        //from there we calculate inner intersection
        Set<List<String>> innerIntersections = new HashSet<>();

        //last inner intersection that was found (tmp variable for innerIntersections calculation)
        List<List<String>> result = new ArrayList<>(outerIntersections);

        //TODO proof
        //there we identify all intersections that can be found in the data (this is true if remainsOperation is disabled)
        boolean calculate = true;

        // TODO: Clarify the optimal number of iterations for remainsOperation.
        // Currently set to 3, it serves as a safety break to prevent excessive looping.
        // Is this break necessary, or should we determine the required number dynamically based on some condition, or disable it?
        int remainsOperation = 3;
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
     * @param minIntersection The minimum number of intersections required for analysis.
     * @param <T> Generic type extending MiningBaseTypeChunk.
     */
    private static <T extends MiningBaseTypeChunk> void prepareObjects(
            @NotNull List<T> miningBaseTypeChunks,
            @NotNull List<T> preparedObjects,
            List<String> requiredMembers,
            List<String> allowedProperties,
            int minIntersection) {
        for (T chunk : miningBaseTypeChunks) {
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

                    } else {
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
                    innerIntersections.add(nInter);
                }

            }

//            //TODO test this one
//            List<String> pointsA = outerIntersectionsList.get(i);
//
//            for (int j = i + 1; j < preparedObjects.size(); j++) {
//                List<String> pointsB = preparedObjects.get(j).getProperties();
//                List<String> intersection = findIntersection(pointsB, pointsA);
//
//                if (intersection.size() >= minIntersection && intersection.size() != pointsA.size()) {
//                    innerIntersections.add(intersection);
//                }
//            }

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

    public static List<String> findIntersection(List<String> list1, List<String> list2) {
        Set<String> set2 = new HashSet<>(list2);
        List<String> intersection = new ArrayList<>();

        for (String item : list1) {
            if (set2.contains(item)) {
                intersection.add(item);
            }
        }

        return intersection;
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
