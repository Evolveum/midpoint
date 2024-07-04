/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.utils;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionExecutionUtil.executeClusteringOutliersDetection;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionExecutionUtil.executeBasicOutlierDetection;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.ClusterExplanation.getClusterExplanationDescription;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.ClusterExplanation.resolveClusterName;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;

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
     * @param handler A progress handler to report processing status.
     * @param task The current task.
     * @param result The operation result.
     * @return A list of PrismObjects representing the processed clusters.
     */
    @NotNull
    public List<PrismObject<RoleAnalysisClusterType>> processClusters(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<DataPoint> dataPoints,
            @NotNull List<Cluster<DataPoint>> clusters,
            @NotNull RoleAnalysisSessionType session,
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
        List<PrismObject<RoleAnalysisClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, size)
                .mapToObj(i -> {
                    handler.iterateActualStatus();
                    return prepareClusters(roleAnalysisService, clusters.get(i), String.valueOf(i), dataPoints,
                            session, complexType, sessionTypeObjectCount,
                            task, result);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Integer> nameOccurrences = new HashMap<>();

        double maxReduction = 0;
        for (PrismObject<RoleAnalysisClusterType> clusterPrismObject : clusterTypeObjectWithStatistic) {
            RoleAnalysisClusterType cluster = clusterPrismObject.asObjectable();
            String orig = cluster.getName().getOrig();
            int count = nameOccurrences.getOrDefault(orig, 0);
            if (count > 0) {
                cluster.setName(PolyStringType.fromOrig(orig + " (" + (count + 1) + ")"));
            }
            nameOccurrences.put(orig, count + 1);

            Double detectedReductionMetric = cluster.getClusterStatistics().getDetectedReductionMetric();
            if (detectedReductionMetric != null) {
                maxReduction = Math.max(maxReduction, detectedReductionMetric);
            }
        }

        boolean executeDetection = true;
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        if (analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
            executeDetection = false;
        }

        for (PrismObject<RoleAnalysisClusterType> roleAnalysisClusterTypePrismObject : clusterTypeObjectWithStatistic) {
            RoleAnalysisClusterType cluster = roleAnalysisClusterTypePrismObject.asObjectable();
            processMetricAnalysis(cluster, session, maxReduction, executeDetection);
        }

        handler.enterNewStep("Prepare Outliers");
        handler.setOperationCountToProcess(dataPoints.size());
        PrismObject<RoleAnalysisClusterType> outlierCluster;
        if (!dataPoints.isEmpty()) {
            outlierCluster = prepareOutlierClusters(roleAnalysisService
                    , dataPoints, complexType, analysisOption, sessionTypeObjectCount, handler,
                    task, result);
            clusterTypeObjectWithStatistic.add(outlierCluster);

        }

        return clusterTypeObjectWithStatistic;
    }

    private @Nullable ClusterStatistic statisticLoad(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull List<DataPoint> clusterDataPoints,
            @NotNull List<DataPoint> allDataPoints,
            @NotNull String clusterIndex,
            @NotNull QName complexType,
            @NotNull Integer sessionTypeObjectCount,
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

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

        if (userAnalysisAttributeDef != null && roleAnalysisAttributeDef != null) {
            extractAttributeStatistics(roleAnalysisService, complexType, task, result, density, propertiesOidsSet,
                    membersOidsSet, clusterStatistic, userAnalysisAttributeDef, roleAnalysisAttributeDef);

        }

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
            @NotNull List<RoleAnalysisAttributeDef> userAttributeDefSet,
            @NotNull List<RoleAnalysisAttributeDef> roleAttributeDefSet) {
        Set<PrismObject<UserType>> users;
        Set<PrismObject<RoleType>> roles;

        boolean isRoleMode = complexType.equals(RoleType.COMPLEX_TYPE);

        Double roleDensity = null;
        Double userDensity = null;
        if (isRoleMode) {
            roleDensity = density;
            users = propertiesOidsSet.stream().map(oid -> roleAnalysisService
                            .cacheUserTypeObject(new HashMap<>(), oid, task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            roles = membersOidsSet.stream().map(oid -> roleAnalysisService
                            .cacheRoleTypeObject(new HashMap<>(), oid, task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());
        } else {
            userDensity = density;
            users = membersOidsSet.stream().map(oid -> roleAnalysisService
                            .cacheUserTypeObject(new HashMap<>(), oid, task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            roles = propertiesOidsSet.stream().map(oid -> roleAnalysisService
                            .cacheRoleTypeObject(new HashMap<>(), oid, task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());
        }

        List<AttributeAnalysisStructure> userAttributeAnalysisStructures = roleAnalysisService
                .userTypeAttributeAnalysis(users, userDensity, task, result, userAttributeDefSet);
        List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = roleAnalysisService
                .roleTypeAttributeAnalysis(roles, roleDensity, task, result, roleAttributeDefSet);

        clusterStatistic.setUserAttributeAnalysisStructures(userAttributeAnalysisStructures);
        clusterStatistic.setRoleAttributeAnalysisStructures(roleAttributeAnalysisStructures);
    }

    private @Nullable PrismObject<RoleAnalysisClusterType> prepareClusters(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Cluster<DataPoint> cluster,
            @NotNull String clusterIndex,
            @NotNull List<DataPoint> dataPoints,
            @NotNull RoleAnalysisSessionType session,
            @NotNull QName complexType,
            @NotNull Integer sessionTypeObjectCount,
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

        ClusterStatistic clusterStatistic = statisticLoad(roleAnalysisService, session, dataPointCluster, dataPoints, clusterIndex,
                complexType, sessionTypeObjectCount, task, result);

        assert clusterStatistic != null;
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = createClusterStatisticType(clusterStatistic,
                analysisOption.getProcessMode());

        boolean detect = isDetectable(session, clusterStatistic);
        return generateClusterObject(roleAnalysisService,
                clusterExplanationDescription,
                clusterStatistic,
                session,
                roleAnalysisClusterStatisticType,
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

    private PrismObject<RoleAnalysisClusterType> prepareOutlierClusters(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<DataPoint> dataPoints,
            @NotNull QName complexType,
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull Integer sessionTypeObjectCount,
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

        PolyStringType name = PolyStringType.fromOrig(sessionTypeObjectCount + "_outliers");

        ClusterStatistic clusterStatistic = new ClusterStatistic(name, processedObjectsRef, elementSize,
                pointsSize, minVectorPoint, maxVectorPoint, meanPoints, density);

        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = createClusterStatisticType(clusterStatistic,
                analysisOption.getProcessMode());

        PrismObject<RoleAnalysisClusterType> clusterObject = generateClusterObject(roleAnalysisService,
                null,
                clusterStatistic,
                null,
                roleAnalysisClusterStatisticType,
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
            @NotNull AnalysisClusterStatisticType roleAnalysisClusterStatisticType,
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

        cluster.getMember().addAll(members);

        double maxReduction = 0;
        List<RoleAnalysisDetectionPatternType> detectedPatterns = processPatternAnalysis(
                roleAnalysisService, clusterStatistic, cluster, analysisOption, session, detectPattern, task, result);

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
            RoleAnalysisAttributeAnalysisResult roleAnalysis = new RoleAnalysisAttributeAnalysisResult();
            for (AttributeAnalysisStructure roleAttributeAnalysisStructure : roleAttributeAnalysisStructures) {
                double density = roleAttributeAnalysisStructure.getDensity();
                if (density == 0) {
                    continue;
                }
                RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
                roleAnalysisAttributeAnalysis.setDensity(density);
                roleAnalysisAttributeAnalysis.setItemPath(roleAttributeAnalysisStructure.getItemPath());
                roleAnalysisAttributeAnalysis.setIsMultiValue(roleAttributeAnalysisStructure.isMultiValue());
                roleAnalysisAttributeAnalysis.setDescription(roleAttributeAnalysisStructure.getDescription());
                List<RoleAnalysisAttributeStatistics> attributeStatistics = roleAttributeAnalysisStructure.getAttributeStatistics();
                for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                    roleAnalysisAttributeAnalysis.getAttributeStatistics().add(attributeStatistic);
                }
                roleAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
            }
            roleAnalysisClusterStatisticType.setRoleAttributeAnalysisResult(roleAnalysis);
        }

        if (userAttributeAnalysisStructures != null && !userAttributeAnalysisStructures.isEmpty()) {
            RoleAnalysisAttributeAnalysisResult userAnalysis = new RoleAnalysisAttributeAnalysisResult();
            for (AttributeAnalysisStructure userAttributeAnalysisStructure : userAttributeAnalysisStructures) {
                double density = userAttributeAnalysisStructure.getDensity();
                if (density == 0) {
                    continue;
                }
                RoleAnalysisAttributeAnalysis userAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
                userAnalysisAttributeAnalysis.setDensity(density);
                userAnalysisAttributeAnalysis.setItemPath(userAttributeAnalysisStructure.getItemPath());
                userAnalysisAttributeAnalysis.setIsMultiValue(userAttributeAnalysisStructure.isMultiValue());

                userAnalysisAttributeAnalysis.setDescription(userAttributeAnalysisStructure.getDescription());
                List<RoleAnalysisAttributeStatistics> attributeStatistics = userAttributeAnalysisStructure.getAttributeStatistics();
                for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                    userAnalysisAttributeAnalysis.getAttributeStatistics().add(attributeStatistic);
                }
                userAnalysis.getAttributeAnalysis().add(userAnalysisAttributeAnalysis);
            }
            roleAnalysisClusterStatisticType.setUserAttributeAnalysisResult(userAnalysis);
        }
    }

    private List<RoleAnalysisDetectionPatternType> processPatternAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ClusterStatistic clusterStatistic,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisOptionType analysisOption,
            @Nullable RoleAnalysisSessionType session,
            boolean detectPattern,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (session == null || !detectPattern) {
            return null;
        }

        RoleAnalysisProcessModeType mode = analysisOption.getProcessMode();
        DefaultPatternResolver defaultPatternResolver = new DefaultPatternResolver(roleAnalysisService, mode);
        List<RoleAnalysisDetectionPatternType> detectedPatterns = defaultPatternResolver
                .loadPattern(session, clusterStatistic, cluster, result, task);

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService
                .resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService
                .resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

        if (userAnalysisAttributeDef == null || roleAnalysisAttributeDef == null) {
            return detectedPatterns;
        }

        roleAnalysisService.resolveDetectedPatternsAttributes(detectedPatterns, userExistCache, roleExistCache, task, result,
                roleAnalysisAttributeDef, userAnalysisAttributeDef);

        return detectedPatterns;
    }

    private double calculateMaxReduction(@NotNull List<RoleAnalysisDetectionPatternType> detectedPatterns) {
        double maxReduction = 0;
        for (RoleAnalysisDetectionPatternType detectedPattern : detectedPatterns) {
            Double clusterMetric = detectedPattern.getClusterMetric();
            if (clusterMetric != null) {
                maxReduction = Math.max(maxReduction, clusterMetric);
            }
        }
        return maxReduction;

    }

    private void processMetricAnalysis(
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable RoleAnalysisSessionType session,
            double maxReduction,
            boolean detectPattern) {

        if (session == null || !detectPattern) {
            return;
        }

        List<RoleAnalysisDetectionPatternType> detectedPatterns = cluster.getDetectedPattern();

        for (RoleAnalysisDetectionPatternType detectedPattern : detectedPatterns) {
            PatternConfidenceCalculator patternConfidenceCalculator = new PatternConfidenceCalculator(session, detectedPattern, maxReduction);
            double itemConfidence = patternConfidenceCalculator.calculateItemConfidence();
            double reductionFactorConfidence = patternConfidenceCalculator.calculateReductionFactorConfidence();
            detectedPattern.setItemConfidence(itemConfidence);
            detectedPattern.setReductionConfidence(reductionFactorConfidence);
        }
    }

    /**
     * Processes the outliers analysis for the specified role analysis session cluster.
     * This method is used to analyze and import outliers in the role analysis session cluster.
     *
     * @param roleAnalysisService The role analysis service for performing role analysis operations.
     * @param cluster The role analysis cluster to process.
     * @param session The role analysis session.
     * @param analysisOption The role analysis option.
     * @param task The current task.
     * @param result The operation result.
     */
    public static void processOutliersAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable RoleAnalysisSessionType session,
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (session != null && analysisOption.getAnalysisCategory().equals(RoleAnalysisCategoryType.OUTLIERS)) {
            RoleAnalysisDetectionOptionType detectionOption = session.getDefaultDetectionOption();
            Double min = detectionOption.getFrequencyRange().getMin();
            if (min == null) {
                detectionOption.getFrequencyRange().setMin(0.01);
            }

            Collection<RoleAnalysisOutlierType> roleAnalysisOutlierTypes;

            UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
            Boolean detailedAnalysis = false;
            if (userModeOptions != null) {
                detailedAnalysis = userModeOptions.getDetailedAnalysis();
            }

            if (cluster.getCategory().equals(RoleAnalysisClusterCategory.OUTLIERS) && detailedAnalysis) {
                roleAnalysisOutlierTypes = executeClusteringOutliersDetection(
                        roleAnalysisService, cluster, session, task);
            } else {
                roleAnalysisOutlierTypes = executeBasicOutlierDetection(
                        roleAnalysisService, cluster, session, analysisOption, task);
            }

            Double sensitivity = detectionOption.getSensitivity();
            double requiredConfidence = roleAnalysisService.calculateOutlierConfidenceRequired(sensitivity);

            //TODO temporary solution
            requiredConfidence = requiredConfidence * 100;
            for (RoleAnalysisOutlierType roleAnalysisOutlierType : roleAnalysisOutlierTypes) {
                roleAnalysisService.resolveOutliers(roleAnalysisOutlierType, task, result, session, cluster, requiredConfidence);
            }
        }
    }
}
