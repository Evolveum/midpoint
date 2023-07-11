/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningOperationChunk;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;

public class ExtractIntersections {

    public static List<IntersectionObject> findPossibleBusinessRole(MiningOperationChunk miningOperationChunk, double minFrequency,
            double maxFrequency, int minIntersection, Integer minOccupancy, ClusterObjectUtils.Mode mode) {

        List<IntersectionObject> intersections = new ArrayList<>();

        if (mode.equals(ClusterObjectUtils.Mode.USER)) {

            loadUsersIntersections(miningOperationChunk, minFrequency, maxFrequency, minIntersection, intersections, minOccupancy);

        } else if (mode.equals(ClusterObjectUtils.Mode.ROLE)) {
            loadRolesIntersections(miningOperationChunk, minFrequency, maxFrequency, minIntersection, intersections, minOccupancy);
        }

        return intersections;
    }

    private static void loadUsersIntersections(MiningOperationChunk miningOperationChunk, double minFrequency, double maxFrequency,
            int minIntersection, List<IntersectionObject> intersections, int minOccupancy) {
        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks();
        List<MiningRoleTypeChunk> preparedObjects = new ArrayList<>();
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            double frequency = miningRoleTypeChunk.getFrequency();
            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }
            preparedObjects.add(miningRoleTypeChunk);

            List<String> users = miningRoleTypeChunk.getUsers();
            if (users.size() >= minIntersection) {
                int size = miningRoleTypeChunk.getRoles().size();
                if (size >= minOccupancy) {
                    intersections.add(new IntersectionObject(new HashSet<>(users), size * users.size(),
                            "outer", size,
                            null, new HashSet<>()));
                }
            }
        }

        Set<List<String>> outerIntersections = new HashSet<>();

        for (int i = 0; i < preparedObjects.size(); i++) {
            Set<String> pointsA = new HashSet<>(preparedObjects.get(i).getUsers());
            for (int j = i + 1; j < preparedObjects.size(); j++) {
                Set<String> intersection = new HashSet<>(preparedObjects.get(j).getUsers());
                intersection.retainAll(pointsA);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    outerIntersections.add(nInter);
                }

            }
        }

        Set<List<String>> innerIntersections = new HashSet<>();

        List<List<String>> outerIntersectionsList = new ArrayList<>(outerIntersections);
        for (int i = 0; i < outerIntersectionsList.size(); i++) {
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

        for (List<String> users : outerIntersectionsList) {
            int counter = 0;
            for (MiningRoleTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getUsers());
                if (basicUsers.containsAll(users)) {
                    counter = counter + preparedObject.getRoles().size();
                }
            }

            if (counter >= minOccupancy) {
                intersections.add(new IntersectionObject(new HashSet<>(users), counter * users.size(),
                        "outer", counter,
                        null, new HashSet<>()));
            }
        }

        for (List<String> users : innerIntersections) {
            int counter = 0;

            if (outerIntersectionsList.contains(users)) {
                continue;
            }

            for (MiningRoleTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getUsers());
                if (basicUsers.containsAll(users)) {
                    counter = counter + preparedObject.getRoles().size();
                }
            }
            if (counter >= minOccupancy) {
                intersections.add(new IntersectionObject(new HashSet<>(users), counter * users.size(),
                        "inner", counter,
                        null, new HashSet<>()));
            }

        }
    }

    private static void loadRolesIntersections(MiningOperationChunk miningOperationChunk, double minFrequency, double maxFrequency,
            int minIntersection, List<IntersectionObject> intersections, int minOccupancy) {
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks();
        List<MiningUserTypeChunk> preparedObjects = new ArrayList<>();
        for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
            double frequency = miningUserTypeChunk.getFrequency();
            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }
            preparedObjects.add(miningUserTypeChunk);

            List<String> roles = miningUserTypeChunk.getRoles();
            if (roles.size() >= minIntersection) {

                int size = miningUserTypeChunk.getUsers().size();
                if (size >= minOccupancy) {
                    intersections.add(new IntersectionObject(new HashSet<>(roles), roles.size() * size,
                            "outer", size,
                            null, new HashSet<>()));
                }
            }
        }

        Set<List<String>> outerIntersections = new HashSet<>();

        for (int i = 0; i < preparedObjects.size(); i++) {
            Set<String> pointsA = new HashSet<>(preparedObjects.get(i).getRoles());
            for (int j = i + 1; j < preparedObjects.size(); j++) {
                Set<String> intersection = new HashSet<>(preparedObjects.get(j).getRoles());
                intersection.retainAll(pointsA);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    outerIntersections.add(nInter);
                }

            }
        }

        Set<List<String>> innerIntersections = new HashSet<>();

        List<List<String>> outerIntersectionsList = new ArrayList<>(outerIntersections);
        for (int i = 0; i < outerIntersectionsList.size(); i++) {
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

        for (List<String> roles : outerIntersectionsList) {
            int counter = 0;
            for (MiningUserTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getRoles());
                if (basicUsers.containsAll(roles)) {
                    counter = counter + preparedObject.getUsers().size();
                }
            }

            if (counter >= minOccupancy) {
                intersections.add(new IntersectionObject(new HashSet<>(roles), counter * roles.size(),
                        "outer", counter,
                        null, new HashSet<>()));
            }
        }

        for (List<String> roles : innerIntersections) {
            int counter = 0;

            if (outerIntersectionsList.contains(roles)) {
                continue;
            }

            for (MiningUserTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getRoles());
                if (basicUsers.containsAll(roles)) {
                    counter = counter + preparedObject.getUsers().size();
                }
            }

            if (counter >= minOccupancy) {
                intersections.add(new IntersectionObject(new HashSet<>(roles), counter * roles.size(),
                        "inner", counter,
                        null, new HashSet<>()));
            }

        }
    }

}
