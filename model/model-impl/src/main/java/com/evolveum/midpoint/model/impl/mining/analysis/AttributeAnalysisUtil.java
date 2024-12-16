/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.analysis;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

//TODO optimize

/**
 * Utility class for attribute analysis.
 * Used for calculating the density and similarity of the attributes.
 * Used for role analysis cluster similarity chart.
 */
public class AttributeAnalysisUtil {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeAnalysisUtil.class);

    private AttributeAnalysisUtil() {
    }

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

    //TODO experiment
    public static void runUserAttributeAnalysisCached(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<UserType>> prismUsers,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet) {

        processUserItemPathsNewCache(roleAnalysisService, prismUsers, attributeDefSet, attributeAnalysisStructures,
                userAnalysisCache, task, result);

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

    public static void processUserItemPathsNewCache(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<UserType>> prismUsers,
            @NotNull List<RoleAnalysisAttributeDef> itemDef,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        int usersCount = prismUsers.size();
        Map<ItemPath, AttributePathResult> attributeResultMap = new HashMap<>();
        for (PrismObject<UserType> prismUser : prismUsers) {
            Map<ItemPath, AttributePathResult> userCache = userAnalysisCache.getMemberUserAnalysisCache(prismUser.getOid());
            if (userCache == null) {
                analyzeAndCacheUserAttributes(itemDef, userAnalysisCache, prismUser);
            }

            splitUserAttributeAnalysis(userAnalysisCache, prismUser, attributeResultMap);
        }

        prepareUserAnalysisStructure(roleAnalysisService, attributeAnalysisStructures, attributeResultMap, usersCount, task, result);
    }

    private static void prepareUserAnalysisStructure(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures,
            @NotNull Map<ItemPath, AttributePathResult> attributeResultMap,
            int usersCount,
            @NotNull Task task,
            @NotNull OperationResult result) {
        attributeResultMap.forEach((key, value) -> {
            AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(
                    value.getFrequencyMap().size(), usersCount, value.getTotalRelation(), key, UserType.COMPLEX_TYPE);
            generateAttributeAnalysisStructure(roleAnalysisService, UserType.class, value, attributeAnalysisStructure, usersCount,
                    task, result);
            attributeAnalysisStructures.add(attributeAnalysisStructure);
        });
    }

    private static void analyzeAndCacheUserAttributes(
            @NotNull List<RoleAnalysisAttributeDef> itemDef,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull PrismObject<UserType> prismUser) {
        Map<ItemPath, AttributePathResult> targetUserCacheCandidate = new HashMap<>();
        extractAttributeStatistics(itemDef, prismUser, targetUserCacheCandidate);
        userAnalysisCache.putMemberUserAnalysisCache(prismUser.getOid(), targetUserCacheCandidate);
    }

    private static void extractAttributeStatistics(
            @NotNull List<RoleAnalysisAttributeDef> itemDef,
            @NotNull PrismObject<?> prismObject,
            Map<ItemPath, AttributePathResult> targetUserCacheCandidate) {
        for (RoleAnalysisAttributeDef item : itemDef) {
            ItemPath path = item.getPath();
            String displayValue = item.getDisplayValue();

            AttributePathResult attributePathResult = targetUserCacheCandidate.computeIfAbsent(path, k -> {
                AttributePathResult newResult = new AttributePathResult(new HashMap<>(), 0);
                newResult.setItemDefinition(item);
                return newResult;
            });

            if (item.isMultiValue()) {
                Set<String> values = item.resolveMultiValueItem(prismObject, path);
                for (String value : values) {
                    attributePathResult.incrementFrequency(value);
                    attributePathResult.incrementTotalRelation();
                }
            } else {
                String value = item.resolveSingleValueItem(prismObject, path);
                if (value != null) {
                    attributePathResult.incrementFrequency(value);
                    attributePathResult.incrementTotalRelation();
                }
            }

        }
    }

    private static void splitUserAttributeAnalysis(
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull PrismObject<UserType> prismUser,
            @NotNull Map<ItemPath, AttributePathResult> attributeResultMap) {
        Map<ItemPath, AttributePathResult> userCache = userAnalysisCache.getMemberUserAnalysisCache(prismUser.getOid());

        userCache.forEach((key, cachedValue) -> {
            AttributePathResult attributePathResult = attributeResultMap.get(key);
            if (attributePathResult == null) {
                attributePathResult = new AttributePathResult(new HashMap<>(), 0);
                attributeResultMap.put(key, attributePathResult);
            }

            int totalRelation = cachedValue.getTotalRelation();
            boolean multiValue = cachedValue.isMultiValue();
            RoleAnalysisAttributeDef itemDefinition = cachedValue.getItemDefinition();
            Map<String, Integer> frequencyMap = new HashMap<>(cachedValue.getFrequencyMap());
            attributePathResult.addToTotalRelation(totalRelation);
            attributePathResult.splitFrequencyMap(frequencyMap);
//            attributePathResult.setMultiValue(multiValue);
            attributePathResult.setItemDefinition(itemDefinition);
            attributePathResult.addTotalRelation(totalRelation);

        });
    }

    public static void processUserItemPathsNew(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<UserType>> prismUsers,
            @NotNull List<RoleAnalysisAttributeDef> itemDef,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures,
            @NotNull Task task,
            @NotNull OperationResult result) {

        int usersCount = prismUsers.size();
        Map<ItemPath, AttributePathResult> attributeResultMap = new HashMap<>();
        for (PrismObject<UserType> prismUser : prismUsers) {
            extractAttributeStatistics(itemDef, prismUser, attributeResultMap);
        }

        prepareUserAnalysisStructure(roleAnalysisService, attributeAnalysisStructures, attributeResultMap, usersCount, task, result);
    }

    public static void processRoleItemPathsNew(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull Set<PrismObject<RoleType>> prismRoles,
            @NotNull List<RoleAnalysisAttributeDef> itemDef,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures, @NotNull Task task, @NotNull OperationResult result) {
        int rolesCount = prismRoles.size();
        Map<ItemPath, AttributePathResult> attributeResultMap = new HashMap<>();
        for (PrismObject<RoleType> prismRole : prismRoles) {
            extractAttributeStatistics(itemDef, prismRole, attributeResultMap);
        }

        attributeResultMap.forEach((key, value) -> {
            AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(
                    value.getFrequencyMap().size(), rolesCount, value.getTotalRelation(), key, RoleType.COMPLEX_TYPE);
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

            String times = "times";
            description.append("Maximum frequency")
                    .append("(").append(maximumFrequency).append(times).append(")").append(": ")
                    .append(maximumFrequencyValues)
                    .append("\n");

            description.append("High frequency values")
                    .append("(").append(minFrequency).append("-").append(maxFrequency).append(times).append(")").append(": ")
                    .append(highFrequencyValues)
                    .append("\n");

            description.append("Low frequency values:")
                    .append("(").append(minFrequencyLow).append("-").append(maxFrequencyLow).append(times).append(")").append(": ")
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
            if (percentageFrequency > 100) {
                LOGGER.warn("Percentage frequency is greater than 100");
            }
            attributeStatistic.setInGroup(inGroupCount);
            attributeStatistic.setInRepo(inRepoCount);
            attributeAnalysisStructure.getAttributeStatistics().add(attributeStatistic);
        });

        attributeAnalysisStructure.setMultiValue(isMultiValue);
    }

    public static @NotNull Set<PrismObject<RoleType>> fetchPrismRoles(
            @NotNull RoleAnalysisService roleAnalysisService,
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
