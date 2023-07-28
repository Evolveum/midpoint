/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ExtractPatternUtils.addDetectedObjectIntersection;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ExtractPatternUtils.addDetectedObjectJaccard;

public class ExtractIntersections {

    public static List<DetectedPattern> businessRoleDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<MiningUserTypeChunk> miningUserTypeChunks, double minFrequency,
            double maxFrequency, int minIntersection, Integer minOccupancy, RoleAnalysisProcessModeType mode) {

        List<DetectedPattern> intersections = new ArrayList<>();

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            loadUsersIntersections(miningRoleTypeChunks, minFrequency, maxFrequency, minIntersection, intersections, minOccupancy);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            loadRolesIntersections(miningUserTypeChunks, minFrequency, maxFrequency, minIntersection, intersections, minOccupancy);
        }

        return intersections;
    }

    private static void loadUsersIntersections(List<MiningRoleTypeChunk> miningRoleTypeChunks, double minFrequency, double maxFrequency,
            int minIntersection, List<DetectedPattern> intersections, int minOccupancy) {
        List<MiningRoleTypeChunk> preparedObjects = new ArrayList<>();
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            double frequency = miningRoleTypeChunk.getFrequency();
            if (frequency < minFrequency || frequency > maxFrequency) {
                continue;
            }
            Set<String> members = new HashSet<>(miningRoleTypeChunk.getUsers());
            if (members.size() < minIntersection) {
                continue;
            }
            preparedObjects.add(miningRoleTypeChunk);

            int propertiesCount = miningRoleTypeChunk.getRoles().size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(addDetectedObjectIntersection(propertiesCount, members, null));
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

        for (List<String> members : outerIntersectionsList) {
            int propertiesCount = 0;
            for (MiningRoleTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getUsers());
                if (basicUsers.containsAll(members)) {
                    propertiesCount = propertiesCount + preparedObject.getRoles().size();
                }
            }

            if (propertiesCount >= minOccupancy) {
                intersections.add(addDetectedObjectIntersection(propertiesCount, new HashSet<>(members), null));
            }
        }

        for (List<String> members : innerIntersections) {
            int propertiesCount = 0;

            if (outerIntersectionsList.contains(members)) {
                continue;
            }

            for (MiningRoleTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getUsers());
                if (basicUsers.containsAll(members)) {
                    propertiesCount = propertiesCount + preparedObject.getRoles().size();
                }
            }
            if (propertiesCount >= minOccupancy) {
                intersections.add(addDetectedObjectIntersection(propertiesCount, new HashSet<>(members), null));
            }

        }
    }

    private static void loadRolesIntersections(List<MiningUserTypeChunk> miningUserTypeChunks, double minFrequency, double maxFrequency,
            int minIntersection, List<DetectedPattern> intersections, int minOccupancy) {
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

            int propertiesCount = miningUserTypeChunk.getUsers().size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(addDetectedObjectIntersection(propertiesCount, members, null));
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

        for (List<String> members : outerIntersectionsList) {
            int propertiesCount = 0;
            for (MiningUserTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getRoles());
                if (basicUsers.containsAll(members)) {
                    propertiesCount = propertiesCount + preparedObject.getUsers().size();
                }
            }

            if (propertiesCount >= minOccupancy) {
                intersections.add(addDetectedObjectIntersection(propertiesCount, new HashSet<>(members), null));
            }
        }

        for (List<String> members : innerIntersections) {
            int propertiesCount = 0;

            if (outerIntersectionsList.contains(members)) {
                continue;
            }

            for (MiningUserTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getRoles());
                if (basicUsers.containsAll(members)) {
                    propertiesCount = propertiesCount + preparedObject.getUsers().size();
                }
            }

            if (propertiesCount >= minOccupancy) {
                intersections.add(addDetectedObjectIntersection(propertiesCount, new HashSet<>(members), null));
            }

        }
    }

}
