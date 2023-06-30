/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.ClusteringObjectMapped;

public class ExtractIntersections {

    public static @NotNull List<IntersectionObject> generateIntersectionsMap(@NotNull List<ClusteringObjectMapped>
            clusteringObjectMapped, int minIntersection, double frequency, HashMap<String, Double> frequencyMap,
            double maxFrequency) {

        List<List<String>> clusterPoints = new ArrayList<>();
        for (ClusteringObjectMapped object : clusteringObjectMapped) {
            List<String> points = object.getPoints();
            List<String> preparedPoints = new ArrayList<>();
            for (String oid : points) {

                Double fr = frequencyMap.get(oid);
                if (frequency > fr || maxFrequency < fr) {
                    continue;
                }
                preparedPoints.add(oid);

            }
            clusterPoints.add(preparedPoints);
        }

        return getOuterIntersectionMap(clusterPoints, minIntersection, clusteringObjectMapped);
    }

    private static List<IntersectionObject> getOuterIntersectionMap(List<List<String>> clusterPoints, int minIntersection,
            List<ClusteringObjectMapped> clusteringObjectMapped) {

        HashMap<Set<String>, String> mappedIntersections = new HashMap<>();

        Set<Set<String>> outerIntersectionsSet = new HashSet<>();
        for (int i = 0; i < clusterPoints.size(); i++) {
            Set<String> pointsA = new HashSet<>(clusterPoints.get(i));
            for (int j = i + 1; j < clusterPoints.size(); j++) {
                Set<String> intersection = new HashSet<>(clusterPoints.get(j));
                intersection.retainAll(pointsA);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    outerIntersectionsSet.add(new HashSet<>(nInter));
                    mappedIntersections.putIfAbsent(new HashSet<>(nInter), "outer");
                }

            }
        }

        List<Set<String>> finalIntersections = new ArrayList<>(outerIntersectionsSet);
        for (int i = 0; i < finalIntersections.size(); i++) {
            Set<String> pointsA = new HashSet<>(finalIntersections.get(i));
            for (int j = i + 1; j < finalIntersections.size(); j++) {
                Set<String> intersection = new HashSet<>(finalIntersections.get(j));
                intersection.retainAll(pointsA);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    mappedIntersections.putIfAbsent(new HashSet<>(nInter), "inner");

                }
            }
        }

        return prepareIntersectionObjects(clusteringObjectMapped, mappedIntersections);
    }

    private static List<IntersectionObject> prepareIntersectionObjects(List<ClusteringObjectMapped> clusteringObjectMapped, HashMap<Set<String>,
            String> intersectionsSet) {
        List<IntersectionObject> intersectionObjectList = new ArrayList<>();

        for (Map.Entry<Set<String>, String> entry : intersectionsSet.entrySet()) {
            Set<String> key = entry.getKey();
            String value = entry.getValue();
            int counter = 0;
            for (ClusteringObjectMapped object : clusteringObjectMapped) {
                if (new HashSet<>(object.getPoints()).containsAll(key)) {
                    counter = counter + object.getElements().size();
                }
            }

            intersectionObjectList.add(new IntersectionObject(key, counter * key.size(), value, counter, null, new HashSet<>()));
        }

        return intersectionObjectList;
    }

}
