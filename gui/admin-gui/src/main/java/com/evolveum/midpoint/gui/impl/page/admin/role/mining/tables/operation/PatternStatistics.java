/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPatternWithAttributes;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.common.mining.utils.CellPatternResolver;
import com.evolveum.midpoint.common.mining.objects.detection.SimpleHeatPattern;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PatternStatistics<T extends MiningBaseTypeChunk> implements Serializable {

    private int patternCount = 0;
    private int totalRelations = 0;
    private int topPatternRelation = 0;
    private double topPatternCoverage = 0;
    private DetectedPattern detectedPattern;

    public PatternStatistics(RoleAnalysisObjectDto roleAnalysisObjectDto, List<String> members, List<String> mustMeet, ModelServiceLocator serviceLocator) {
        loadStatistics(roleAnalysisObjectDto, members, mustMeet, serviceLocator);
    }

    //NOTE additionalObject -> in User mode those are roles, in Role mode those are users
    private void loadStatistics(@NotNull RoleAnalysisObjectDto roleAnalysisObjectDto,
            List<String> members,
            List<String> mustMeet,
            ModelServiceLocator serviceLocator) {

        List<T> additionalObjects = roleAnalysisObjectDto.getAdditionalMiningChunk();
        RoleAnalysisProcessModeType processMode = roleAnalysisObjectDto.isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER;

        SimpleHeatPattern topPattern = extractTopPatternAndComputeStatistics(members, mustMeet, processMode, additionalObjects);

        if (topPattern == null) {
            return;
        }

        List<String> patternMembersObjectOidSet = topPattern.getMembers();
        if (patternMembersObjectOidSet == null || patternMembersObjectOidSet.isEmpty()) {
            return;
        }

        List<String> patternPropertiesObjectOidSet = getPatternPropertiesObjectOidSet(additionalObjects, topPattern);

        RoleAnalysisDetectionPatternType pattern = buildDetectedPattern(roleAnalysisObjectDto, serviceLocator,
                patternMembersObjectOidSet, patternPropertiesObjectOidSet);

        detectedPattern = transformPatternWithAttributes(pattern);
    }

    private @NotNull RoleAnalysisDetectionPatternType buildDetectedPattern(
            @NotNull RoleAnalysisObjectDto roleAnalysisObjectDto,
            @NotNull ModelServiceLocator serviceLocator,
            @NotNull List<String> patternMembersObjectOidSet,
            @NotNull List<String> patternPropertiesObjectOidSet) {
        RoleAnalysisDetectionPatternType pattern = new RoleAnalysisDetectionPatternType();
        loadPatternUserRef(pattern, roleAnalysisObjectDto.isRoleMode, patternMembersObjectOidSet, patternPropertiesObjectOidSet);
        double metric = calculateRelationMetric(patternMembersObjectOidSet, patternPropertiesObjectOidSet,
                roleAnalysisObjectDto.isRoleMode);
        pattern.setReductionCount(metric);

        resolvePatternAttributeAnalysis(roleAnalysisObjectDto, serviceLocator, pattern);

        double itemsConfidence = calculatePatternConfidence(pattern);
        pattern.setItemConfidence(itemsConfidence);
        return pattern;
    }

    private static void resolvePatternAttributeAnalysis(
            @NotNull RoleAnalysisObjectDto roleAnalysisObjectDto,
            @NotNull ModelServiceLocator serviceLocator,
            @NotNull RoleAnalysisDetectionPatternType pattern) {
        Task task = serviceLocator.createSimpleTask("InitPattern");
        RoleAnalysisService roleAnalysisService = serviceLocator.getRoleAnalysisService();

        List<RoleAnalysisAttributeDef> roleAnalysisAttributes = roleAnalysisObjectDto.getRoleAnalysisAttributes();
        List<RoleAnalysisAttributeDef> userAnalysisAttributes = roleAnalysisObjectDto.getUserAnalysisAttributes();
        roleAnalysisService.resolveDetectedPatternsAttributes(Collections.singletonList(pattern), new HashMap<>(),
                new HashMap<>(), task, task.getResult(), roleAnalysisAttributes, userAnalysisAttributes);
    }

    private SimpleHeatPattern extractTopPatternAndComputeStatistics(
            @NotNull List<String> members,
            @NotNull List<String> mustMeet,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<T> additionalObjects) {
        SimpleHeatPattern topPattern = null;
        if (new HashSet<>(mustMeet).containsAll(members)) {
            //There is executing all size patterns detections,
            // this is why we do not mirror pattern detection option from cluster settings. But think about 10% fq threshold
            PatternDetectionOption detectionOption = new PatternDetectionOption(
                    10, 100, 2, 2);
            List<SimpleHeatPattern> totalRelationOfPatternsForCell = new CellPatternResolver()
                    .performSingleCellDetection(processMode, additionalObjects, detectionOption, members, mustMeet);

            patternCount = totalRelationOfPatternsForCell.size();
            for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
                int relations = simpleHeatPattern.getTotalRelations();
                totalRelations += relations;
                if (relations > topPatternRelation) {
                    topPatternRelation = relations;
                    topPattern = simpleHeatPattern;
                }
            }

            int clusterRelations = 0;
            for (T roleTypeChunk : additionalObjects) {
                int propertiesCount = roleTypeChunk.getProperties().size();
                int membersCount = roleTypeChunk.getMembers().size();
                clusterRelations += (propertiesCount * membersCount);
            }

            if (clusterRelations == 0) {
                topPatternCoverage = 0;
            } else {
                topPatternCoverage = ((double) topPatternRelation / clusterRelations) * 100;
            }

        }
        return topPattern;
    }

    private void loadPatternUserRef(
            @NotNull RoleAnalysisDetectionPatternType pattern,
            boolean isRoleMode,
            @NotNull List<String> patternMembersObjectOidSet,
            @NotNull List<String> patternPropertiesObjectOidSet) {
        List<String> users = isRoleMode ? patternPropertiesObjectOidSet : patternMembersObjectOidSet;
        for (String usersRef : users) {
            pattern.getUserOccupancy().add(
                    new ObjectReferenceType().oid(usersRef).type(UserType.COMPLEX_TYPE));

        }

        List<String> roles = isRoleMode ? patternMembersObjectOidSet : patternPropertiesObjectOidSet;
        for (String rolesRef : roles) {
            pattern.getRolesOccupancy().add(
                    new ObjectReferenceType().oid(rolesRef).type(RoleType.COMPLEX_TYPE)
            );
        }

    }

    private static <T extends MiningBaseTypeChunk> @NotNull List<String> getPatternPropertiesObjectOidSet(
            @NotNull List<T> additionalObjects,
            @NotNull SimpleHeatPattern topPattern) {
        List<String> memberObjectOids = new ArrayList<>();
        for (T chunk : additionalObjects) {
            List<String> chunkProperties = chunk.getProperties();
            if (new HashSet<>(chunkProperties).containsAll(topPattern.getMembers())) {
                memberObjectOids.addAll(chunk.getMembers());
            }
        }
        return memberObjectOids;
    }

    private static int calculateRelationMetric(@NotNull List<String> members, @NotNull List<String> properties, boolean isRoleMode) {
        int part = members.size() * properties.size();

        if (isRoleMode) {
            return part - properties.size();
        }

        return part - members.size();
    }

    public static double calculatePatternConfidence(@NotNull RoleAnalysisDetectionPatternType pattern) {
        double totalDensity = 0.0;
        int totalCount = 0;
        RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult = pattern.getRoleAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = pattern.getUserAttributeAnalysisResult();

        if (roleAttributeAnalysisResult != null) {
            totalDensity += calculateDensity(roleAttributeAnalysisResult.getAttributeAnalysis());
            totalCount += roleAttributeAnalysisResult.getAttributeAnalysis().size();
        }
        if (userAttributeAnalysisResult != null) {
            totalDensity += calculateDensity(userAttributeAnalysisResult.getAttributeAnalysis());
            totalCount += userAttributeAnalysisResult.getAttributeAnalysis().size();
        }

        int itemCount = (roleAttributeAnalysisResult != null
                ? roleAttributeAnalysisResult.getAttributeAnalysis().size() : 0)
                + (userAttributeAnalysisResult != null ? userAttributeAnalysisResult.getAttributeAnalysis().size() : 0);

        return (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;
    }

    private static double calculateDensity(@NotNull List<RoleAnalysisAttributeAnalysisType> attributeAnalysisList) {
        double totalDensity = 0.0;
        for (RoleAnalysisAttributeAnalysisType attributeAnalysis : attributeAnalysisList) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
            }
        }
        return totalDensity;
    }

    public DetectedPattern getDetectedPattern() {
        return detectedPattern;
    }

    protected int getDetectedPatternCount() {
        return patternCount;
    }

    protected int getTopPatternRelations() {
        return topPatternRelation;
    }

    protected int getTotalRelations() {
        return totalRelations;
    }

    protected double getMaxCoverage() {
        return topPatternCoverage;
    }
}
