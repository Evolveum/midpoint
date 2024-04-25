/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.analysis;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.RoleAnalysisServiceImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

// TODO - this class is just fast experiment

/**
 * Utility class for attribute analysis.
 * Used for calculating the density and similarity of the attributes.
 * Used for role analysis cluster similarity chart.
 */
public class AttributeAnalysisUtil {

    public static void runUserAttributeAnalysis(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<UserType>> prismUsers,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet) {

        processUserItemPathsNew(roleAnalysisService, prismUsers, attributeDefSet, attributeAnalysisStructures,
                task, result);

    }

    public static void runRoleAttributeAnalysis(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<RoleType>> prismRoles,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeRoleDefSet) {

        processRoleItemPathsNew(roleAnalysisService, prismRoles, attributeRoleDefSet, attributeAnalysisStructures,
                task, result);
    }

    public static void processUserItemPathsNew(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<UserType>> prismUsers,
            @NotNull List<RoleAnalysisAttributeDef> itemDef,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures, @NotNull Task task, @NotNull OperationResult result) {

        int usersCount = prismUsers.size();
        Map<String, AttributePathResult> attributeResultMap = new HashMap<>();
        for (PrismObject<UserType> prismUser : prismUsers) {
            for (RoleAnalysisAttributeDef item : itemDef) {
                ItemPath path = item.getPath();
                String displayValue = item.getDisplayValue();
                boolean isContainer = item.isContainer();

                AttributePathResult attributePathResult = attributeResultMap.get(displayValue);
                if (attributePathResult == null) {
                    attributePathResult = new AttributePathResult(new HashMap<>(), 0);
                    attributePathResult.setMultiValue(isContainer);
                    attributePathResult.setItemDefinition(item);
                    attributeResultMap.put(displayValue, attributePathResult);
                }

                if (isContainer) {
                    Set<String> values = item.resolveMultiValueItem(prismUser, path);
                    for (String value : values) {
                        attributePathResult.incrementFrequency(value);
                        attributePathResult.incrementTotalRelation();
                    }
                } else {
                    String value = item.resolveSingleValueItem(prismUser, path);
                    if (value != null) {
                        attributePathResult.incrementFrequency(value);
                        attributePathResult.incrementTotalRelation();
                    }
                }

            }
        }

        attributeResultMap.forEach((key, value) -> {
            AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(
                    value.getFrequencyMap().size(), usersCount, value.getTotalRelation(), key);
            attributeAnalysisStructure.setComplexType(UserType.COMPLEX_TYPE);
            attributeAnalysisStructure.setDescription(
                    generateFrequencyMapDescription(value.getFrequencyMap(), value.getMaximumFrequency()));
            generateAttributeAnalysisStructure(roleAnalysisService, UserType.class, value, attributeAnalysisStructure, usersCount,
                    task, result);
            attributeAnalysisStructures.add(attributeAnalysisStructure);
        });
    }

    public static void processRoleItemPathsNew(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<RoleType>> prismRoles,
            @NotNull List<RoleAnalysisAttributeDef> itemDef,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures, @NotNull Task task, @NotNull OperationResult result) {
        int rolesCount = prismRoles.size();
        Map<String, AttributePathResult> attributeResultMap = new HashMap<>();
        for (PrismObject<RoleType> prismRole : prismRoles) {
            for (RoleAnalysisAttributeDef item : itemDef) {
                ItemPath path = item.getPath();
                String displayValue = item.getDisplayValue();
                boolean isContainer = item.isContainer();

                AttributePathResult attributePathResult = attributeResultMap.get(displayValue);
                if (attributePathResult == null) {
                    attributePathResult = new AttributePathResult(new HashMap<>(), 0);
                    attributePathResult.setMultiValue(isContainer);
                    attributePathResult.setItemDefinition(item);
                    attributeResultMap.put(displayValue, attributePathResult);
                }

                if (isContainer) {
                    Set<String> values = item.resolveMultiValueItem(prismRole, path);
                    for (String value : values) {
                        attributePathResult.incrementFrequency(value);
                        attributePathResult.incrementTotalRelation();
                    }
                } else {
                    String value = item.resolveSingleValueItem(prismRole, path);
                    if (value != null) {
                        attributePathResult.incrementFrequency(value);
                        attributePathResult.incrementTotalRelation();
                    }
                }

            }
        }

        attributeResultMap.forEach((key, value) -> {
            AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(
                    value.getFrequencyMap().size(), rolesCount, value.getTotalRelation(), key);
            attributeAnalysisStructure.setComplexType(RoleType.COMPLEX_TYPE);
            attributeAnalysisStructure.setDescription(
                    generateFrequencyMapDescription(value.getFrequencyMap(), value.getMaximumFrequency()));
            generateAttributeAnalysisStructure(roleAnalysisService, RoleType.class, value, attributeAnalysisStructure, rolesCount,
                    task, result);
            attributeAnalysisStructures.add(attributeAnalysisStructure);
        });
    }

    public static @NotNull String generateFrequencyMapDescription(
            @NotNull Map<String, Integer> frequencyMap,
            int maximumFrequency) {
        StringBuilder description = new StringBuilder("Analysis of value frequencies:\n");

        if (frequencyMap.isEmpty()) {
            description.append("No values found in the frequency map.");
        } else {
            int totalValues = frequencyMap.size();
            int totalOccurrences = frequencyMap.values().stream().mapToInt(Integer::intValue).sum();
            double averageOccurrences = (double) totalOccurrences / totalValues;

            description.append("Total unique values: ").append(totalValues).append("\n");
            description.append("Total occurrences: ").append(totalOccurrences).append("\n");
            description.append("Average occurrences per value: ").append(String.format("%.2f", averageOccurrences)).append("\n");

            int threshold = totalOccurrences / totalValues;
            Set<String> highFrequencyValues = new HashSet<>();
            Set<String> lowFrequencyValues = new HashSet<>();
            Set<String> maximumFrequencyValues = new HashSet<>();

            int maxFrequency = 0;
            int minFrequency = 0;

            int maxFrequencyLow = 0;
            int minFrequencyLow = 0;
            for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
                String value = entry.getKey();
                int frequency = entry.getValue();

                if (frequency >= threshold) {
                    minFrequency = Math.min(minFrequency, frequency);
                    maxFrequency = Math.max(maxFrequency, frequency);
                    highFrequencyValues.add(value);
                } else {
                    minFrequencyLow = Math.min(minFrequencyLow, frequency);
                    maxFrequencyLow = Math.max(maxFrequencyLow, frequency);
                    lowFrequencyValues.add(value);
                }

                if (frequency == maximumFrequency) {
                    maximumFrequencyValues.add(value);
                }
            }

            description.append("Maximum frequency")
                    .append("(").append(maximumFrequency)
                    .append("times)").append(": ")
                    .append(maximumFrequencyValues)
                    .append("\n");

            description.append("High frequency values")
                    .append("(").append(minFrequency).append("-").append(maxFrequency)
                    .append("times)").append(": ")
                    .append(highFrequencyValues)
                    .append("\n");

            description.append("Low frequency values:")
                    .append("(").append(minFrequencyLow).append("-").append(maxFrequencyLow)
                    .append("times)").append(": ")
                    .append(lowFrequencyValues)
                    .append("\n");

        }

        return description.toString();
    }

    public static void generateAttributeAnalysisStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Class<? extends ObjectType> objectClass,
            @NotNull AttributePathResult attributePathResult,
            @NotNull AttributeAnalysisStructure attributeAnalysisStructure,
            int prismObjectsCount,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<String, Integer> frequencyMap = attributePathResult.getFrequencyMap();
        RoleAnalysisAttributeDef itemDefinition = attributePathResult.getItemDefinition();
        boolean isMultiValue = attributePathResult.isMultiValue();
        attributeAnalysisStructure.setAttributeStatistics(new ArrayList<>());
        frequencyMap.forEach((attributeSimpleValue, inGroupCount) -> {
            Integer inRepoCount = roleAnalysisService
                    .countObjects(objectClass, itemDefinition.getQuery(attributeSimpleValue), null,
                            task, result);

            double percentageFrequency = (double) inGroupCount / prismObjectsCount * 100;

            RoleAnalysisAttributeStatistics attributeStatistic = new RoleAnalysisAttributeStatistics();
            attributeStatistic.setAttributeValue(attributeSimpleValue);
            attributeStatistic.setFrequency(percentageFrequency);
            attributeStatistic.setInGroup(inGroupCount);
            attributeStatistic.setInRepo(inRepoCount);
            attributeAnalysisStructure.getAttributeStatistics().add(attributeStatistic);
        });

        attributeAnalysisStructure.setMultiValue(isMultiValue);
    }


    public static @NotNull Set<PrismObject<UserType>> fetchPrismUsers(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Set<String> objectOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Set<PrismObject<UserType>> prismUsers = new HashSet<>();

        objectOid.forEach(userOid -> {
            PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
            if (userTypeObject != null) {
                prismUsers.add(userTypeObject);
            }
        });

        return prismUsers;
    }

    public static @NotNull Set<PrismObject<RoleType>> fetchPrismRoles(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Set<String> objectOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Set<PrismObject<RoleType>> prismRolesSet = new HashSet<>();

        objectOid.forEach(roleOid -> {
            PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(roleOid, task, result);
            if (rolePrismObject != null) {
                prismRolesSet.add(rolePrismObject);
            }
        });

        return prismRolesSet;
    }
}
