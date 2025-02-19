/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.utils;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.ClusterExplanation.getClusterExplanationDescription;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.ClusterExplanation.resolveClusterName;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.Cluster;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.ClusterExplanation;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.DataPoint;
import com.evolveum.midpoint.model.impl.mining.algorithm.detection.DefaultPatternResolver;
import com.evolveum.midpoint.model.impl.mining.algorithm.detection.PatternConfidenceCalculator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * The `RoleAnalysisAlgorithmUtils` class provides utility methods for processing and analyzing data clusters
 * and outliers in role analysis.
 * These utilities are used to generate statistics, prepare cluster objects, and detect patterns during role analysis.
 */
public class RoleAnalysisAlgorithmUtils {

    /**
     * Processes the clusters and generates cluster statistics, including the detection of patterns and outliers.
     * This method is used in role analysis to analyze clusters of data points.
     *
     * @param roleAnalysisService The role analysis service for performing role analysis operations.
     * @param dataPoints The data points representing cluster data.
     * @param clusters The clusters to process.
     * @param session The role analysis session.
     * @param attributeAnalysisCache The attribute analysis cache.
     * @param objectCategorisationCache The object categorization cache.
     * @param handler A progress handler to report processing status.
     * @param task The current task.
     * @param result The operation result.
     */
    public void processClusters(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<DataPoint> dataPoints,
            @NotNull List<Cluster<DataPoint>> clusters,
            @NotNull RoleAnalysisSessionType session,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Integer sessionTypeObjectCount = roleAnalysisService.countSessionTypeObjects(task, result);

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        QName complexType = processMode.equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        int size = clusters.size();
        handler.enterNewStep("Generate Cluster Statistics model");
        handler.setOperationCountToProcess(size);

        Set<String> propertiesInClusters = new HashSet<>();
        IntStream.range(0, size).forEach(i1 -> {
            handler.iterateActualStatus();
            PrismObject<RoleAnalysisClusterType> clusterPrismObject = prepareClusters(roleAnalysisService,
                    clusters.get(i1),
                    String.valueOf(i1),
                    dataPoints,
                    session,
                    complexType,
                    sessionTypeObjectCount,
                    attributeAnalysisCache,
                    propertiesInClusters,
                    task,
                    result);

            importCluster(roleAnalysisService, session, task, result, clusterPrismObject);
        });

        Set<String> membersInNoiseClusters = new HashSet<>();
        Set<String> propertiesInNoiseClusters = new HashSet<>();
        resolveNoiseClusters(roleAnalysisService,
                dataPoints,
                session,
                attributeAnalysisCache,
                handler,
                task,
                result,
                propertiesInNoiseClusters,
                membersInNoiseClusters);

        loadSessionObjectCategorization(
                objectCategorisationCache,
                processMode,
                propertiesInNoiseClusters,
                membersInNoiseClusters,
                propertiesInClusters);
    }

    private void resolveNoiseClusters(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<DataPoint> dataPoints,
            @NotNull RoleAnalysisSessionType session,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result,
            Set<String> propertiesInNoiseClusters,
            Set<String> membersInNoiseClusters) {

        if (dataPoints.isEmpty()) {
            return;
        }

        handler.enterNewStep("Prepare Outliers");
        handler.setOperationCountToProcess(dataPoints.size());

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        QName complexType = processMode.equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        Map<OutlierNoiseCategoryType, List<DataPoint>> categorizedDataPoints = new EnumMap<>(OutlierNoiseCategoryType.class);
        List<DataPoint> unCategoryDataPoints = new ArrayList<>();

        for (OutlierNoiseCategoryType type : OutlierNoiseCategoryType.values()) {
            categorizedDataPoints.put(type, new ArrayList<>());
        }

        for (DataPoint dataPoint : dataPoints) {
            propertiesInNoiseClusters.addAll(dataPoint.getProperties());
            membersInNoiseClusters.addAll(dataPoint.getMembers());

            OutlierNoiseCategoryType pointStatus = dataPoint.getPointStatus();
            categorizedDataPoints.getOrDefault(pointStatus, unCategoryDataPoints).add(dataPoint);
        }

        categorizedDataPoints.forEach((category, categoryDataPoints) -> {
            if (!categoryDataPoints.isEmpty()) {
                processNoiseCluster(roleAnalysisService, session, category, categoryDataPoints, complexType,
                        attributeAnalysisCache, analysisOption, handler, task, result);
            }
        });

        if (!unCategoryDataPoints.isEmpty()) {
            processNoiseCluster(roleAnalysisService, session, null, unCategoryDataPoints, complexType,
                    attributeAnalysisCache, analysisOption, handler, task, result);
        }
    }

    private void processNoiseCluster(
            RoleAnalysisService roleAnalysisService,
            RoleAnalysisSessionType session,
            OutlierNoiseCategoryType category,
            List<DataPoint> categoryDataPoints,
            QName complexType,
            AttributeAnalysisCache attributeAnalysisCache,
            RoleAnalysisOptionType analysisOption,
            RoleAnalysisProgressIncrement handler,
            Task task,
            OperationResult result) {

        PrismObject<RoleAnalysisClusterType> noiseCluster = prepareOutlierClusters(roleAnalysisService, session,
                category, categoryDataPoints, complexType,
                attributeAnalysisCache, analysisOption, handler, task, result);

        importCluster(roleAnalysisService, session, task, result, noiseCluster);
    }

    private static void importCluster(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result,
            PrismObject<RoleAnalysisClusterType> clusterPrismObject) {
        if (clusterPrismObject != null) {
            roleAnalysisService.importCluster(clusterPrismObject,
                    session.getDefaultDetectionOption(),
                    createObjectRef(session),
                    task,
                    result);
        }
    }

    private static void loadSessionObjectCategorization(
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull Set<String> propertiesInNoiseClusters,
            @NotNull Set<String> membersInNoiseClusters,
            @NotNull Set<String> propertiesInClusters) {

        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            objectCategorisationCache.putAllCategory(propertiesInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE, UserType.COMPLEX_TYPE);

            objectCategorisationCache.putAllCategory(membersInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE, RoleType.COMPLEX_TYPE);
            objectCategorisationCache.putAllCategory(membersInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE, RoleType.COMPLEX_TYPE);
        } else {
            objectCategorisationCache.putAllCategory(propertiesInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE, RoleType.COMPLEX_TYPE);

            objectCategorisationCache.putAllCategory(membersInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE, UserType.COMPLEX_TYPE);
            objectCategorisationCache.putAllCategory(membersInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE, UserType.COMPLEX_TYPE);
        }

        Set<String> propertiesOnlyInNoiseClusters = new HashSet<>(propertiesInNoiseClusters);
        propertiesOnlyInNoiseClusters.removeAll(propertiesInClusters);

        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            objectCategorisationCache.putAllCategory(propertiesOnlyInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE, UserType.COMPLEX_TYPE);
        } else {
            objectCategorisationCache.putAllCategory(propertiesOnlyInNoiseClusters,
                    RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE, RoleType.COMPLEX_TYPE);
        }
    }

    private @Nullable ClusterStatistic statisticLoad(
            @NotNull RoleAnalysisService roleAnalysisService,
            Set<String> rolesInClusters,
            @NotNull RoleAnalysisSessionType session,
            @NotNull List<DataPoint> clusterDataPoints,
            @NotNull List<DataPoint> allDataPoints,
            @NotNull String clusterIndex,
            @NotNull QName complexType,
            @NotNull Integer sessionTypeObjectCount,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        PolyStringType name = PolyStringType.fromOrig(sessionTypeObjectCount + "_cluster_" + clusterIndex);

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalAssignPropertiesRelation = 0;
        int totalMembersCount = 0;

        Set<String> membersOidsSet = new HashSet<>();
        Set<String> propertiesOidsSet = new HashSet<>();

        for (DataPoint clusterDataPoint : clusterDataPoints) {
            allDataPoints.remove(clusterDataPoint);
            Set<String> properties = clusterDataPoint.getProperties();
            Set<String> members = clusterDataPoint.getMembers();
            membersOidsSet.addAll(members);
            propertiesOidsSet.addAll(properties);

            int groupSize = members.size();
            totalMembersCount += groupSize;

            int occupyPointsCount = properties.size();
            totalAssignPropertiesRelation += (occupyPointsCount * groupSize);
            minVectorPoint = Math.min(minVectorPoint, occupyPointsCount);
            maxVectorPoint = Math.max(maxVectorPoint, occupyPointsCount);
        }

        rolesInClusters.addAll(propertiesOidsSet);

        int existingPropertiesInCluster = propertiesOidsSet.size();

        if (existingPropertiesInCluster == 0 || totalMembersCount == 0) {
            return null;
        }

        int allPossibleRelation = existingPropertiesInCluster * totalMembersCount;

        double meanPoints = (double) totalAssignPropertiesRelation / totalMembersCount;

        double density = Math.min((totalAssignPropertiesRelation / (double) allPossibleRelation) * 100, 100);

        Set<ObjectReferenceType> processedObjectsRef = roleAnalysisService
                .generateObjectReferences(membersOidsSet, complexType, task, result);

        ClusterStatistic clusterStatistic = new ClusterStatistic(name, processedObjectsRef, totalMembersCount,
                existingPropertiesInCluster, minVectorPoint, maxVectorPoint, meanPoints, density);

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, RoleType.COMPLEX_TYPE);

        extractAttributeStatistics(roleAnalysisService, complexType, task, result, density, propertiesOidsSet,
                membersOidsSet, clusterStatistic, attributeAnalysisCache, userAnalysisAttributeDef, roleAnalysisAttributeDef);

        return clusterStatistic;
    }

    public static void extractAttributeStatistics(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull QName complexType,
            @NotNull Task task,
            @NotNull OperationResult result,
            double density,
            Set<String> propertiesOidsSet,
            Set<String> membersOidsSet,
            ClusterStatistic clusterStatistic,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @Nullable List<RoleAnalysisAttributeDef> userAttributeDefSet,
            @Nullable List<RoleAnalysisAttributeDef> roleAttributeDefSet) {

        if (userAttributeDefSet == null && roleAttributeDefSet == null) {
            return;
        }

        boolean isRoleMode = complexType.equals(RoleType.COMPLEX_TYPE);
        Double roleDensity = isRoleMode ? density : null;
        Double userDensity = isRoleMode ? null : density;

        Set<PrismObject<UserType>> users = getCachedUsers(roleAnalysisService, isRoleMode
                ? propertiesOidsSet : membersOidsSet, task, result);
        Set<PrismObject<RoleType>> roles = getCachedRoles(roleAnalysisService,
                isRoleMode ? membersOidsSet : propertiesOidsSet, task, result);

        if (userAttributeDefSet != null && !userAttributeDefSet.isEmpty()) {
            clusterStatistic.setUserAttributeAnalysisStructures(
                    roleAnalysisService.userTypeAttributeAnalysisCached(
                            users, userDensity, attributeAnalysisCache, userAttributeDefSet, task, result)
            );
        }

        if (roleAttributeDefSet != null && !roleAttributeDefSet.isEmpty()) {
            clusterStatistic.setRoleAttributeAnalysisStructures(
                    roleAnalysisService.roleTypeAttributeAnalysis(
                            roles, roleDensity, task, result, roleAttributeDefSet)
            );
        }
    }

    private static Set<PrismObject<UserType>> getCachedUsers(
            RoleAnalysisService roleAnalysisService, @NotNull Set<String> oids, Task task, OperationResult result) {
        return oids.stream()
                .map(oid -> roleAnalysisService.cacheUserTypeObject(new HashMap<>(), oid, task, result, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Set<PrismObject<RoleType>> getCachedRoles(
            RoleAnalysisService roleAnalysisService, @NotNull Set<String> oids, Task task, OperationResult result) {
        return oids.stream()
                .map(oid -> roleAnalysisService.cacheRoleTypeObject(new HashMap<>(), oid, task, result, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private @Nullable PrismObject<RoleAnalysisClusterType> prepareClusters(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Cluster<DataPoint> cluster,
            @NotNull String clusterIndex,
            @NotNull List<DataPoint> dataPoints,
            @NotNull RoleAnalysisSessionType session,
            @NotNull QName complexType,
            @NotNull Integer sessionTypeObjectCount,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull Set<String> rolesInClusters,
            @NotNull Task task,
            @NotNull OperationResult result) {

        List<DataPoint> dataPointCluster = cluster.getPoints();
        Set<ClusterExplanation> explanations = cluster.getExplanations();
        String clusterExplanationDescription = getClusterExplanationDescription(explanations);

        Set<String> elementsOids = new HashSet<>();
        for (DataPoint clusterDataPoint : dataPointCluster) {
            Set<String> elements = clusterDataPoint.getMembers();
            elementsOids.addAll(elements);
        }

        AbstractAnalysisSessionOptionType sessionOptionType = getSessionOptionType(session);

        if (elementsOids.size() < sessionOptionType.getMinMembersCount()) {
            return null;
        }

        ClusterStatistic clusterStatistic = statisticLoad(roleAnalysisService, rolesInClusters, session, dataPointCluster,
                dataPoints, clusterIndex, complexType, sessionTypeObjectCount, attributeAnalysisCache, task, result);

        assert clusterStatistic != null;
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = createClusterStatisticType(clusterStatistic,
                analysisOption.getProcessMode());

        boolean detect = isDetectable(session, clusterStatistic);
        return generateClusterObject(roleAnalysisService,
                clusterExplanationDescription,
                clusterStatistic,
                session,
                session.getOid(),
                roleAnalysisClusterStatisticType,
                attributeAnalysisCache,
                analysisOption,
                detect,
                task,
                result);
    }

    private static boolean isDetectable(@NotNull RoleAnalysisSessionType session, ClusterStatistic clusterStatistic) {
        boolean detect = true;
        RoleAnalysisDetectionProcessType detectMode = session.getDefaultDetectionOption().getDetectionProcessMode();

        if (detectMode == null) {
            detectMode = RoleAnalysisDetectionProcessType.FULL;
        }

        if (detectMode.equals(RoleAnalysisDetectionProcessType.PARTIAL)) {
            if (clusterStatistic.getPropertiesCount() > 300 || clusterStatistic.getMembersCount() > 300) {
                detect = false;
            }
        } else if (detectMode.equals(RoleAnalysisDetectionProcessType.SKIP)) {
            detect = false;
        }
        return detect;
    }

    private @NotNull PrismObject<RoleAnalysisClusterType> prepareOutlierClusters(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @Nullable OutlierNoiseCategoryType noiseCategory,
            @NotNull List<DataPoint> dataPoints,
            @NotNull QName complexType,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalDataPoints = dataPoints.size();
        int sumPoints = 0;

        Set<String> elementsOid = new HashSet<>();
        Set<String> pointsSet = new HashSet<>();
        Set<ObjectReferenceType> processedObjectsRef = new HashSet<>();
        for (DataPoint dataPoint : dataPoints) {
            handler.iterateActualStatus();

            Set<String> points = dataPoint.getProperties();
            pointsSet.addAll(points);

            Set<String> members = dataPoint.getMembers();
            elementsOid.addAll(members);

            int pointsSize = points.size();
            sumPoints += pointsSize;
            minVectorPoint = Math.min(minVectorPoint, pointsSize);
            maxVectorPoint = Math.max(maxVectorPoint, pointsSize);

            for (String member : members) {
                ObjectReferenceType objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setType(complexType);
                objectReferenceType.setOid(member);
                objectReferenceType.setDescription(dataPoint.getPointStatusIdentificator());
                processedObjectsRef.add(objectReferenceType);
            }
        }

        double meanPoints = (double) sumPoints / totalDataPoints;

        int pointsSize = pointsSet.size();
        int elementSize = elementsOid.size();
        double density = (sumPoints / (double) (elementSize * pointsSize)) * 100;

        PolyStringType name = PolyStringType.fromOrig(resolveNameForClusterNoise(noiseCategory));

        ClusterStatistic clusterStatistic = new ClusterStatistic(name, processedObjectsRef, elementSize,
                pointsSize, minVectorPoint, maxVectorPoint, meanPoints, density);

        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = createClusterStatisticType(clusterStatistic,
                analysisOption.getProcessMode());

        //temporary solution
        String sessionOid = session.getOid();
        PrismObject<RoleAnalysisClusterType> clusterObject = generateClusterObject(roleAnalysisService,
                null,
                clusterStatistic,
                null,
                sessionOid,
                roleAnalysisClusterStatisticType,
                attributeAnalysisCache,
                analysisOption,
                false,
                task,
                result
        );
        clusterObject.asObjectable().setCategory(RoleAnalysisClusterCategory.OUTLIERS);
        return clusterObject;
    }

    private @NotNull PrismObject<RoleAnalysisClusterType> generateClusterObject(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable String clusterExplanationDescription,
            @NotNull ClusterStatistic clusterStatistic,
            @Nullable RoleAnalysisSessionType session,
            String sessionOid,
            @NotNull AnalysisClusterStatisticType roleAnalysisClusterStatisticType,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull RoleAnalysisOptionType analysisOption,
            boolean detectPattern,
            @NotNull Task task,
            @NotNull OperationResult result) {

        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareClusterPrismObject();
        assert clusterTypePrismObject != null;

        Set<ObjectReferenceType> members = clusterStatistic.getMembersRef();

        RoleAnalysisClusterType cluster = clusterTypePrismObject.asObjectable();
        cluster.setOid(String.valueOf(UUID.randomUUID()));
        cluster.setCategory(RoleAnalysisClusterCategory.INLIERS);
        cluster.setRoleAnalysisSessionRef(new ObjectReferenceType().oid(sessionOid).type(RoleAnalysisSessionType.COMPLEX_TYPE));

        cluster.getMember().addAll(members);

        double maxReduction = 0;
        List<RoleAnalysisDetectionPatternType> detectedPatterns = processPatternAnalysis(roleAnalysisService, clusterStatistic,
                cluster, analysisOption, session, detectPattern, attributeAnalysisCache, task, result);

        if (detectedPatterns != null) {
            cluster.getDetectedPattern().addAll(detectedPatterns);
            maxReduction = calculateMaxReduction(detectedPatterns);
        }

        roleAnalysisClusterStatisticType.setDetectedReductionMetric(maxReduction);

        resolveAttributeStatistics(clusterStatistic, roleAnalysisClusterStatisticType);

        cluster.setClusterStatistics(roleAnalysisClusterStatisticType);

        String candidateName = resolveClusterName(cluster, session, roleAnalysisService, task, result);
        cluster.setName(candidateName != null && !candidateName.isEmpty()
                ? PolyStringType.fromOrig(candidateName)
                : clusterStatistic.getName());

        if (clusterExplanationDescription != null) {
            cluster.setDescription(clusterExplanationDescription);
        }

        return clusterTypePrismObject;
    }

    public static void resolveAttributeStatistics(@NotNull ClusterStatistic clusterStatistic,
            @NotNull AnalysisClusterStatisticType roleAnalysisClusterStatisticType) {
        List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = clusterStatistic.getRoleAttributeAnalysisStructures();
        List<AttributeAnalysisStructure> userAttributeAnalysisStructures = clusterStatistic.getUserAttributeAnalysisStructures();
        if (roleAttributeAnalysisStructures != null && !roleAttributeAnalysisStructures.isEmpty()) {
            RoleAnalysisAttributeAnalysisResultType attributeContainer = buildAttributeAnalysisContainer(roleAttributeAnalysisStructures);
            roleAnalysisClusterStatisticType.setRoleAttributeAnalysisResult(attributeContainer);
        }

        if (userAttributeAnalysisStructures != null && !userAttributeAnalysisStructures.isEmpty()) {
            RoleAnalysisAttributeAnalysisResultType attributeContainer = buildAttributeAnalysisContainer(userAttributeAnalysisStructures);
            roleAnalysisClusterStatisticType.setUserAttributeAnalysisResult(attributeContainer);
        }
    }

    private static @NotNull RoleAnalysisAttributeAnalysisResultType buildAttributeAnalysisContainer(
            @NotNull List<AttributeAnalysisStructure> roleAttributeAnalysisStructures) {
        RoleAnalysisAttributeAnalysisResultType roleAnalysis = new RoleAnalysisAttributeAnalysisResultType();
        for (AttributeAnalysisStructure roleAttributeAnalysisStructure : roleAttributeAnalysisStructures) {
            double density = roleAttributeAnalysisStructure.getDensity();
            if (density == 0) {
                continue;
            }
            RoleAnalysisAttributeAnalysisType roleAnalysisAttributeAnalysis = roleAttributeAnalysisStructure
                    .buildRoleAnalysisAttributeAnalysisContainer();
            roleAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
        }
        return roleAnalysis;
    }

    @Nullable
    private List<RoleAnalysisDetectionPatternType> processPatternAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ClusterStatistic clusterStatistic,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisOptionType analysisOption,
            @Nullable RoleAnalysisSessionType session,
            boolean detectPattern,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (session == null || !detectPattern) {
            return null;
        }

        RoleAnalysisProcessModeType mode = analysisOption.getProcessMode();
        DefaultPatternResolver defaultPatternResolver = new DefaultPatternResolver(roleAnalysisService, mode);
        List<RoleAnalysisDetectionPatternType> detectedPatterns = defaultPatternResolver
                .loadPattern(session, clusterStatistic, cluster, result, task);


        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService
                .resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService
                .resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

        if (userAnalysisAttributeDef == null && roleAnalysisAttributeDef == null) {
            return detectedPatterns;
        }

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        roleAnalysisService.resolveDetectedPatternsAttributesCached(detectedPatterns, userExistCache, roleExistCache,
                attributeAnalysisCache, roleAnalysisAttributeDef, userAnalysisAttributeDef, task, result);

        //TODO check it (might be duplicated inside pattern detection process)
        for (RoleAnalysisDetectionPatternType detectedPattern : detectedPatterns) {
            PatternConfidenceCalculator patternConfidenceCalculator = new PatternConfidenceCalculator(session, detectedPattern);
            double itemConfidence = patternConfidenceCalculator.calculateItemConfidence();
            detectedPattern.setItemConfidence(itemConfidence);
        }

        return detectedPatterns;
    }

    private double calculateMaxReduction(@NotNull List<RoleAnalysisDetectionPatternType> detectedPatterns) {
        double maxReduction = 0;
        for (RoleAnalysisDetectionPatternType detectedPattern : detectedPatterns) {
            Double clusterMetric = detectedPattern.getReductionCount();
            if (clusterMetric != null) {
                maxReduction = Math.max(maxReduction, clusterMetric);
            }
        }
        return maxReduction;

    }

    @Contract(pure = true)
    private @NotNull String resolveNameForClusterNoise(@Nullable OutlierNoiseCategoryType noiseCategory) {
        if (noiseCategory != null) {
            return switch (noiseCategory) {
                case ACCESS_NOISE -> "Access noise";
                case RULE_NOISE -> "Rule noise";
                case MEMBERS_NOISE -> "Members noise";
                case ACCESS_OR_RULE_NOISE -> "Access or rule noise";
                case OVERAL_NOISE -> "Overall noise";
                default -> "Non-category noise";
            };
        } else {
            return "Non-category noise";
        }
    }

}
