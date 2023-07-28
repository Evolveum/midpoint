/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ExtractPatternUtils.addDetectedObjectJaccard;

public class ExtractJaccard {

    public static List<DetectedPattern> businessRoleDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<MiningUserTypeChunk> miningUserTypeChunks, double minFrequency,
            double maxFrequency, int minIntersection, Integer minOccupancy, RoleAnalysisProcessModeType mode, double similarity) {

        List<DetectedPattern> intersections = new ArrayList<>();

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            loadUsersIntersections(miningRoleTypeChunks, minFrequency, maxFrequency, minIntersection, intersections,
                    minOccupancy, similarity);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            loadRolesIntersections(miningUserTypeChunks, minFrequency, maxFrequency, minIntersection, intersections,
                    minOccupancy, similarity);
        }

        return intersections;
    }

    private static void loadUsersIntersections(List<MiningRoleTypeChunk> miningRoleTypeChunks, double minFrequency, double maxFrequency,
            int minIntersection, List<DetectedPattern> intersections, int minOccupancy, double similarity) {

        List<MiningRoleTypeChunk> preparedObjects = new ArrayList<>();
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            double frequency = miningRoleTypeChunk.getFrequency();
            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }
            Set<String> members = new HashSet<>(miningRoleTypeChunk.getUsers());
            int membersCount = members.size();
            if (membersCount < minIntersection) {
                continue;
            }
            preparedObjects.add(miningRoleTypeChunk);

            int propertiesCount = miningRoleTypeChunk.getRoles().size();
            if (propertiesCount >= minOccupancy) {

                intersections.add(addDetectedObjectJaccard(new HashSet<>(miningRoleTypeChunk.getRoles()),
                        members, null));
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
            Set<String> members = new HashSet<>(key.getUsers());
            Set<String> properties = new HashSet<>(key.getRoles());
            for (MiningRoleTypeChunk value : values) {
                members.addAll(value.getUsers());
                properties.addAll(value.getRoles());
            }

            intersections.add(addDetectedObjectJaccard(properties, members, null));
        }

    }

    private static void loadRolesIntersections(List<MiningUserTypeChunk> miningUserTypeChunks, double minFrequency, double maxFrequency,
            int minIntersection, List<DetectedPattern> intersections, int minOccupancy, double similarity) {
        List<MiningUserTypeChunk> preparedObjects = new ArrayList<>();
        for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
            double frequency = miningUserTypeChunk.getFrequency();
            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }

            Set<String> members = new HashSet<>(miningUserTypeChunk.getRoles());
            if (members.size() < minIntersection) {
                continue;
            }
            preparedObjects.add(miningUserTypeChunk);

            Set<String> properties = new HashSet<>(miningUserTypeChunk.getUsers());
            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(addDetectedObjectJaccard(properties, members, null));
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
            Set<String> properties = new HashSet<>(key.getUsers());
            Set<String> members = new HashSet<>(key.getRoles());
            for (MiningUserTypeChunk value : values) {
                members.addAll(value.getRoles());
                properties.addAll(value.getUsers());
            }

            intersections.add(addDetectedObjectJaccard(properties, members, null));

        }

    }

}
