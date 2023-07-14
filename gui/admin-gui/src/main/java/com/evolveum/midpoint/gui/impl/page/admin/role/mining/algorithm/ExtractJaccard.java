/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.*;

public class ExtractJaccard {

    public static List<IntersectionObject> businessRoleDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<MiningUserTypeChunk> miningUserTypeChunks, double minFrequency,
            double maxFrequency, int minIntersection, Integer minOccupancy, ClusterObjectUtils.Mode mode, double similarity) {

        List<IntersectionObject> intersections = new ArrayList<>();

        if (mode.equals(ClusterObjectUtils.Mode.USER)) {
            loadUsersIntersections(miningRoleTypeChunks, minFrequency, maxFrequency, minIntersection, intersections,
                    minOccupancy, similarity);
        } else if (mode.equals(ClusterObjectUtils.Mode.ROLE)) {
            loadRolesIntersections(miningUserTypeChunks, minFrequency, maxFrequency, minIntersection, intersections,
                    minOccupancy, similarity);
        }

        return intersections;
    }

    private static void loadUsersIntersections(List<MiningRoleTypeChunk> miningRoleTypeChunks, double minFrequency, double maxFrequency,
            int minIntersection, List<IntersectionObject> intersections, int minOccupancy, double similarity) {
        List<MiningRoleTypeChunk> preparedObjects = new ArrayList<>();
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            double frequency = miningRoleTypeChunk.getFrequency();
            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }
            List<String> users = miningRoleTypeChunk.getUsers();
            if (users.size() < minIntersection) {
                continue;
            }
            preparedObjects.add(miningRoleTypeChunk);

            int size = miningRoleTypeChunk.getRoles().size();
            if (size >= minOccupancy) {
                intersections.add(new IntersectionObject(new HashSet<>(users), size * users.size(),
                        "jaccard", size,
                        null, new HashSet<>(miningRoleTypeChunk.getRoles()), ClusterObjectUtils.SearchMode.JACCARD));
            }

        }

        ListMultimap<MiningRoleTypeChunk, MiningRoleTypeChunk> map = ArrayListMultimap.create();
        for (int i = 0; i < preparedObjects.size(); i++) {
            MiningRoleTypeChunk miningRoleTypeChunkA = preparedObjects.get(i);
            Set<String> pointsA = new HashSet<>(miningRoleTypeChunkA.getUsers());
            for (int j = i + 1; j < preparedObjects.size(); j++) {
                MiningRoleTypeChunk miningRoleTypeChunkB = preparedObjects.get(j);
                HashSet<String> pointsB = new HashSet<>(miningRoleTypeChunkB.getUsers());
                double resultSimilarity = RoleUtils.jacquardSimilarity(pointsA, pointsB);

                if (resultSimilarity >= similarity) {
                    map.putAll(miningRoleTypeChunkB, Collections.singleton(miningRoleTypeChunkA));
                }

            }
        }

        for (MiningRoleTypeChunk key : map.keySet()) {
            List<MiningRoleTypeChunk> values = map.get(key);
            Set<String> users = new HashSet<>(key.getUsers());
            Set<String> roles = new HashSet<>(key.getRoles());
            for (MiningRoleTypeChunk value : values) {
                users.addAll(value.getUsers());
                roles.addAll(value.getRoles());
            }

            intersections.add(new IntersectionObject(users, users.size() * roles.size(),
                    "jaccard", roles.size(),
                    null, roles, ClusterObjectUtils.SearchMode.JACCARD));
        }

    }

    private static void loadRolesIntersections(List<MiningUserTypeChunk> miningUserTypeChunks, double minFrequency, double maxFrequency,
            int minIntersection, List<IntersectionObject> intersections, int minOccupancy, double similarity) {
        List<MiningUserTypeChunk> preparedObjects = new ArrayList<>();
        for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
            double frequency = miningUserTypeChunk.getFrequency();
            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }

            List<String> roles = miningUserTypeChunk.getRoles();
            if (roles.size() < minIntersection) {
                continue;
            }

            preparedObjects.add(miningUserTypeChunk);

            int occupancy = miningUserTypeChunk.getUsers().size();
            if (occupancy >= minOccupancy) {
                intersections.add(new IntersectionObject(new HashSet<>(roles), roles.size() * occupancy,
                        "outer", occupancy,
                        null, new HashSet<>(miningUserTypeChunk.getUsers()), ClusterObjectUtils.SearchMode.JACCARD));
            }

        }

        ListMultimap<MiningUserTypeChunk, MiningUserTypeChunk> map = ArrayListMultimap.create();
        for (int i = 0; i < preparedObjects.size(); i++) {
            MiningUserTypeChunk miningUserTypeChunkA = preparedObjects.get(i);
            Set<String> pointsA = new HashSet<>(miningUserTypeChunkA.getRoles());
            for (int j = i + 1; j < preparedObjects.size(); j++) {
                MiningUserTypeChunk miningUserTypeChunkB = preparedObjects.get(j);
                HashSet<String> pointsB = new HashSet<>(miningUserTypeChunkB.getRoles());
                double resultSimilarity = RoleUtils.jacquardSimilarity(pointsA, pointsB);

                if (resultSimilarity >= similarity) {
                    map.putAll(miningUserTypeChunkB, Collections.singleton(miningUserTypeChunkA));
                }

            }
        }

        for (MiningUserTypeChunk key : map.keySet()) {
            List<MiningUserTypeChunk> values = map.get(key);
            Set<String> users = new HashSet<>(key.getUsers());
            Set<String> roles = new HashSet<>(key.getRoles());
            for (MiningUserTypeChunk value : values) {
                roles.addAll(value.getRoles());
                users.addAll(value.getUsers());
            }

            intersections.add(new IntersectionObject(new HashSet<>(roles), roles.size() * users.size(),
                    "jaccard", users.size(),
                    null, users, ClusterObjectUtils.SearchMode.JACCARD));
        }

    }

}
