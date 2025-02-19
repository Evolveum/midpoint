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

import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
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

    //addtionalObject -> in User mode those are roles, in Role mode those are users
    private void loadStatistics(RoleAnalysisObjectDto roleAnalysisObjectDto, List<String> members, List<String> mustMeet, ModelServiceLocator serviceLocator) {
//        List<String> members = userChunk.getMembers();
        List<SimpleHeatPattern> totalRelationOfPatternsForCell;
//        List<String> mustMeet = roleChunk.getProperties();
        List<String> topPattern = new ArrayList<>();

        List<T> additionalObjects = roleAnalysisObjectDto.getAdditionalMiningChunk();

        if (new HashSet<>(mustMeet).containsAll(members)) {
            //There is executing all size patterns detections,
            // this is why we does not mirror pattern detection option from cluster settings. But think about 10% fq threshold
            PatternDetectionOption detectionOption = new PatternDetectionOption(
                    10, 100, 2, 2);
            totalRelationOfPatternsForCell = new OutlierPatternResolver()
                    .performSingleCellDetection(RoleAnalysisProcessModeType.USER, additionalObjects, detectionOption, members, mustMeet);

            patternCount = totalRelationOfPatternsForCell.size();
            for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
                int relations = simpleHeatPattern.getTotalRelations();
                totalRelations += relations;
                if (relations > topPatternRelation) {
                    topPatternRelation = relations;
                    topPattern = simpleHeatPattern.getPropertiesOids();
                }
            }

            int clusterRelations = 0;
            for (T roleTypeChunk : additionalObjects) {
                int propertiesCount = roleTypeChunk.getProperties().size();
                int membersCount = roleTypeChunk.getMembers().size();
                clusterRelations += (propertiesCount * membersCount);
            }
            topPatternCoverage = ((double) topPatternRelation / clusterRelations) * 100;

        }

        List<String> finalTopPattern = topPattern;
//        double finalTopPatternCoverage = topPatternCoverage;
        int finalTotalRelations = totalRelations;
        int finalTopPatternRelation = topPatternRelation;
        int finalPatternCount = patternCount;

        List<String> roleProperty = new ArrayList<>();
        for (T role : additionalObjects) {
            List<String> userProperty = role.getProperties();
            if (new HashSet<>(userProperty).containsAll(finalTopPattern)) {
                roleProperty.addAll(role.getMembers());
            }
        }

        double metric = (finalTopPattern.size() * roleProperty.size()) - finalTopPattern.size();

        RoleAnalysisDetectionPatternType pattern = new RoleAnalysisDetectionPatternType();

        Set<String> users = new HashSet<>(finalTopPattern);
        Set<String> roles = new HashSet<>(roleProperty);

        for (String usersRef : users) {
            pattern.getUserOccupancy().add(
                    new ObjectReferenceType().oid(usersRef).type(UserType.COMPLEX_TYPE));

        }

        for (String rolesRef : roles) {
            pattern.getRolesOccupancy().add(
                    new ObjectReferenceType().oid(rolesRef).type(RoleType.COMPLEX_TYPE)
            );
        }

        pattern.setReductionCount(metric);

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        Task task = serviceLocator.createSimpleTask("InitPattern");
        RoleAnalysisService roleAnalysisService = serviceLocator.getRoleAnalysisService();

//        ObjectReferenceType roleAnalysisSessionRef = cluster.asObjectable().getRoleAnalysisSessionRef();
//        if (roleAnalysisSessionRef != null) {
//            PrismObject<RoleAnalysisSessionType> session = roleAnalysisService
//                    .getObject(RoleAnalysisSessionType.class, roleAnalysisSessionRef.getOid(), task, task.getResult());
//            if (session == null) {
//                return;
//            }
//            List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService
//                    .resolveAnalysisAttributes(session.asObjectable(), UserType.COMPLEX_TYPE);
//            List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService
//                    .resolveAnalysisAttributes(session.asObjectable(), RoleType.COMPLEX_TYPE);

        List<RoleAnalysisAttributeDef> roleAnalysisAttributes = roleAnalysisObjectDto.getRoleAnalysisAttributes();
        List<RoleAnalysisAttributeDef> userAnalysisAttributes = roleAnalysisObjectDto.getUserAnalysisAttributes();
        roleAnalysisService.resolveDetectedPatternsAttributes(Collections.singletonList(pattern), userExistCache,
                roleExistCache, task, task.getResult(), roleAnalysisAttributes, userAnalysisAttributes);

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

        double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;
        pattern.setItemConfidence(itemsConfidence);
//        }

        detectedPattern = transformPatternWithAttributes(pattern);
    }

    private double calculateDensity(@NotNull List<RoleAnalysisAttributeAnalysisType> attributeAnalysisList) {
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
