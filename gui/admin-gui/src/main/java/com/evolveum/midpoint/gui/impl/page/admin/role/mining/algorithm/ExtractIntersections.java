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
            userObjects, int minIntersection, double frequency, HashMap<String, Double> frequencyMap,
            double maxFrequency) {

        List<List<String>> clusterRoles = new ArrayList<>();
        for (ClusteringObjectMapped prismUser : userObjects) {
            List<String> roles = prismUser.getRoles();
            List<String> preparedRoles = new ArrayList<>();
            for (String oid : roles) {

                Double fr = frequencyMap.get(oid);
                if (frequency > fr || maxFrequency < fr) {
                    continue;
                }
                preparedRoles.add(oid);

            }
            clusterRoles.add(preparedRoles);
        }

        return getOuterIntersectionMap(clusterRoles, minIntersection, userObjects);
    }

    private static List<IntersectionObject> getOuterIntersectionMap(List<List<String>> roles, int minIntersection,
            List<ClusteringObjectMapped> userObjects) {

        HashMap<Set<String>, String> mappedIntersections = new HashMap<>();

        Set<Set<String>> outerIntersectionsSet = new HashSet<>();
        for (int i = 0; i < roles.size(); i++) {
            Set<String> rolesA = new HashSet<>(roles.get(i));
            for (int j = i + 1; j < roles.size(); j++) {
                Set<String> intersection = new HashSet<>(roles.get(j));
                intersection.retainAll(rolesA);

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
            Set<String> rolesA = new HashSet<>(finalIntersections.get(i));
            for (int j = i + 1; j < finalIntersections.size(); j++) {
                Set<String> intersection = new HashSet<>(finalIntersections.get(j));
                intersection.retainAll(rolesA);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    mappedIntersections.putIfAbsent(new HashSet<>(nInter), "inner");

                }
            }
        }

        return prepareIntersectionObjects(userObjects, mappedIntersections);
    }

    private static List<IntersectionObject> prepareIntersectionObjects(List<ClusteringObjectMapped> users, HashMap<Set<String>,
            String> intersectionsSet) {
        List<IntersectionObject> intersectionObjectList = new ArrayList<>();

        for (Map.Entry<Set<String>, String> entry : intersectionsSet.entrySet()) {
            Set<String> key = entry.getKey();
            String value = entry.getValue();
            int counter = 0;
            for (ClusteringObjectMapped user : users) {
                if (new HashSet<>(user.getRoles()).containsAll(key)) {
                    counter = counter + user.getMembers().size();
                }
            }

            intersectionObjectList.add(new IntersectionObject(key, counter * key.size(), value, counter, null));
        }

        return intersectionObjectList;
    }

}
