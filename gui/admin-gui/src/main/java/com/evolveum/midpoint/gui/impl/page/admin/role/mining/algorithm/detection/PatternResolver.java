/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ExtractPatternUtils.prepareDetectedPattern;

import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;

public class PatternResolver implements DetectionOperation {

    @Override
    public List<DetectedPattern> performUserBasedDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            DetectionOption detectionOption) {

        double minFrequency = detectionOption.getMinFrequencyThreshold() / 100;
        double maxFrequency = detectionOption.getMaxFrequencyThreshold() / 100;
        int minIntersection = detectionOption.getMinUsers();
        int minOccupancy = detectionOption.getMinRoles();

        List<DetectedPattern> intersections = new ArrayList<>();
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

            Set<String> properties = new HashSet<>(miningRoleTypeChunk.getRoles());
            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(prepareDetectedPattern(properties, members));
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
            Set<String> properties = new HashSet<>();
            for (MiningRoleTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getUsers());
                if (basicUsers.containsAll(members)) {
                    properties.addAll(preparedObject.getRoles());
                }
            }

            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(prepareDetectedPattern(properties, new HashSet<>(members)));
            }
        }

        for (List<String> members : innerIntersections) {
            Set<String> properties = new HashSet<>();
            if (outerIntersectionsList.contains(members)) {
                continue;
            }

            for (MiningRoleTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getUsers());
                if (basicUsers.containsAll(members)) {
                    properties.addAll(preparedObject.getRoles());
                }
            }

            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(prepareDetectedPattern(properties, new HashSet<>(members)));
            }

        }

        return intersections;
    }

    @Override
    public List<DetectedPattern> performRoleBasedDetection(List<MiningUserTypeChunk> miningUserTypeChunks,
            DetectionOption roleAnalysisSessionDetectionOptionType) {
        double minFrequency = roleAnalysisSessionDetectionOptionType.getMinFrequencyThreshold() / 100;
        double maxFrequency = roleAnalysisSessionDetectionOptionType.getMaxFrequencyThreshold() / 100;

        int minIntersection = roleAnalysisSessionDetectionOptionType.getMinRoles();
        int minOccupancy = roleAnalysisSessionDetectionOptionType.getMinUsers();

        List<DetectedPattern> intersections = new ArrayList<>();
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
            int propertiesCount = miningUserTypeChunk.getUsers().size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(prepareDetectedPattern(properties, members));
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
            Set<String> properties = new HashSet<>();
            for (MiningUserTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getRoles());
                if (basicUsers.containsAll(members)) {
                    properties.addAll(preparedObject.getUsers());
                }
            }

            int propertiesCount = properties.size();
            if (propertiesCount >= minOccupancy) {
                intersections.add(prepareDetectedPattern(properties, new HashSet<>(members)));
            }
        }

        for (List<String> members : innerIntersections) {
            int propertiesCount = 0;

            if (outerIntersectionsList.contains(members)) {
                continue;
            }

            Set<String> properties = new HashSet<>();
            for (MiningUserTypeChunk preparedObject : preparedObjects) {
                Set<String> basicUsers = new HashSet<>(preparedObject.getRoles());
                if (basicUsers.containsAll(members)) {
                    properties.addAll(preparedObject.getUsers());
                }
            }

            if (propertiesCount >= minOccupancy) {
                intersections.add(prepareDetectedPattern(properties, new HashSet<>(members)));
            }

        }
        return intersections;
    }

}
