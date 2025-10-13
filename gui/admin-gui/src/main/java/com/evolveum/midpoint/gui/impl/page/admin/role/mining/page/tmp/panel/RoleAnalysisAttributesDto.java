/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisSimpleModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisAttributesDto implements Serializable {

    public static final String F_ATTRIBUTES_MODEL = "attributesModel";
    public static final String F_CHART_MODEL = "chartModel";
    public static final String F_DISPLAY = "display";

    private String display;
    private List<RoleAnalysisAttributeAnalysisDto> attributesModel;
    private List<RoleAnalysisSimpleModel> chartModel;

    private boolean compared;

    private RoleAnalysisAttributesDto(String display, List<RoleAnalysisAttributeAnalysisDto> attributesModel, List<RoleAnalysisSimpleModel> chartModel) {
        this.display = display;
        this.attributesModel = attributesModel;
        this.chartModel = chartModel;
    }

    public static RoleAnalysisAttributesDto loadFromDetectedPattern(String displayKey, DetectedPattern detectedPattern) {

        RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = detectedPattern.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult = detectedPattern.getRoleAttributeAnalysisResult();

        return loadModel(displayKey, userAttributeAnalysisResult, roleAttributeAnalysisResult);
//                attributeStatistics.addAll(collectAttributesStatistics(detectedPattern.getRoleAttributeAnalysisResult()));//TODO do we need this? where is this set?
    }

    private static RoleAnalysisAttributesDto loadModel(String displayKey,
            RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult,
            RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult) {
        List<RoleAnalysisAttributeAnalysisType> attributeStatistics = collectAttributesStatistics(userAttributeAnalysisResult);
        List<RoleAnalysisAttributeAnalysisDto> attributesModel = attributeStatistics.stream()
                .map(attribute -> new RoleAnalysisAttributeAnalysisDto(attribute, UserType.class))
                .toList();

        List<RoleAnalysisSimpleModel> chartData = RoleAnalysisSimpleModel
                .getRoleAnalysisSimpleModel(roleAttributeAnalysisResult, userAttributeAnalysisResult);

        return new RoleAnalysisAttributesDto(displayKey, attributesModel, chartData);
    }

    public static RoleAnalysisAttributesDto loadFromCluster(String displayKey, RoleAnalysisClusterType cluster) {

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        if (clusterStatistics == null) {
            return new RoleAnalysisAttributesDto(displayKey, new ArrayList<>(), new ArrayList<>());
        }

        RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();

        return loadModel(displayKey, userAttributeAnalysisResult, roleAttributeAnalysisResult);

    }

    public static RoleAnalysisAttributesDto fromPartitionAttributeAnalysis(String displayKey, RoleAnalysisOutlierPartitionType partition) {

        AttributeAnalysisType attributeAnalysis = partition.getPartitionAnalysis().getAttributeAnalysis();
        if (attributeAnalysis == null) {
            return new RoleAnalysisAttributesDto(displayKey, new ArrayList<>(), new ArrayList<>());
        }

        RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = attributeAnalysis.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResultType clusterCompare = attributeAnalysis.getUserClusterCompare();

        return loadCompared(displayKey, userAttributeAnalysisResult, clusterCompare);
    }

    public static RoleAnalysisAttributesDto ofCompare(String displayKey,
            RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult,
            RoleAnalysisAttributeAnalysisResultType compareAttributeResult) {
        return loadCompared(displayKey, userAttributeAnalysisResult, compareAttributeResult);
    }

    private static RoleAnalysisAttributesDto loadCompared(String displayKey,
            RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult,
            RoleAnalysisAttributeAnalysisResultType compareAttributeResult) {
        List<RoleAnalysisAttributeAnalysisType> attributeStatistics = collectAttributesStatistics(userAttributeAnalysisResult);
        List<RoleAnalysisAttributeAnalysisDto> attributesModel = attributeStatistics.stream()
                .map(attribute -> new RoleAnalysisAttributeAnalysisDto(attribute, UserType.class))
                .toList();

        List<RoleAnalysisSimpleModel> chartData = RoleAnalysisSimpleModel
                .getRoleAnalysisSimpleComparedModel(userAttributeAnalysisResult, null,
                        compareAttributeResult, null);
        RoleAnalysisAttributesDto attributesDto = new RoleAnalysisAttributesDto(displayKey, attributesModel, chartData);
        attributesDto.compared = true;
        return attributesDto;
    }

    public static RoleAnalysisAttributesDto fromAnomalyStatistics(String displayKey, DetectedAnomalyStatisticsType anomalyModelStatistics) {
        AttributeAnalysisType attributeAnalysis = anomalyModelStatistics.getAttributeAnalysis();
        if (attributeAnalysis == null) {
            return new RoleAnalysisAttributesDto(displayKey, new ArrayList<>(), new ArrayList<>());
        }

        RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult = attributeAnalysis
                .getRoleAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResultType userRoleMembersCompare = attributeAnalysis
                .getUserRoleMembersCompare();

        if (roleAttributeAnalysisResult == null || userRoleMembersCompare == null) {
            return new RoleAnalysisAttributesDto(displayKey, new ArrayList<>(), new ArrayList<>());
        }

        return loadCompared(displayKey, roleAttributeAnalysisResult, userRoleMembersCompare);
    }

    private static List<RoleAnalysisAttributeAnalysisType> collectAttributesStatistics(RoleAnalysisAttributeAnalysisResultType attributeAnalysisResult) {
        if (attributeAnalysisResult == null) {
            return new ArrayList<>();
        }

        return attributeAnalysisResult.getAttributeAnalysis();
    }

    public boolean isCompared() {
        return compared;
    }

    public List<RoleAnalysisSimpleModel> getChartModel() {
        return chartModel;
    }

    //TODO better name
    public List<RoleAnalysisAttributeAnalysisDto> getAttributesModel() {
        return attributesModel;
    }
}

