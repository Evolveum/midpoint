/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.analysis;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an attribute analysis structure.
 * Used for storing statistical data about the role analysis cluster.
 * Represent information about the attribute similarity and density.
 */
// TODO - this class is just fast experiment
public class AttributeAnalysisStructure implements Serializable {

    int uniqueValues;
    int totalValues;
    private ItemPath itemPath;
    double density;
    String description;
    private transient List<RoleAnalysisAttributeStatisticsType> attributeStatistics = new ArrayList<>();
    boolean isMultiValue;
    QName complexType;
    int analyzedObjectsCount;

    public AttributeAnalysisStructure(int uniqueValues, int objectCount, int totalValues, ItemPath itemPath, QName complexType) {
        this.uniqueValues = uniqueValues;
        this.totalValues = totalValues;
        int possibleRelations = uniqueValues * objectCount;
        this.density = calculateDensity(totalValues, possibleRelations);
        this.itemPath = itemPath;
        this.complexType = complexType;
        this.analyzedObjectsCount = objectCount;
    }

    public AttributeAnalysisStructure(double density, ItemPath itemPath, QName complexType, int objectCount) {
        this.density = density;
        this.itemPath = itemPath;
        this.complexType = complexType;
        this.analyzedObjectsCount = objectCount;
    }

    public static @NotNull List<AttributeAnalysisStructure> extractAttributeAnalysis(@NotNull RoleAnalysisClusterType cluster) {
        List<AttributeAnalysisStructure> attributeAnalysisStructures = new ArrayList<>();

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        if (clusterStatistics == null) {
            return attributeAnalysisStructures;
        }

        RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();

        attributeAnalysisStructures.addAll(
                extractAttributeAnalysis(userAttributeAnalysisResult.getAttributeAnalysis(), UserType.COMPLEX_TYPE));
        attributeAnalysisStructures.addAll(
                extractAttributeAnalysis(roleAttributeAnalysisResult.getAttributeAnalysis(), RoleType.COMPLEX_TYPE));

        return attributeAnalysisStructures;
    }

    public static @NotNull List<AttributeAnalysisStructure> extractAttributeAnalysis(
            @NotNull List<RoleAnalysisAttributeAnalysisType> attributeAnalysisList,
            @NotNull QName complexType) {
        List<AttributeAnalysisStructure> analysisStructures = new ArrayList<>();
        for (RoleAnalysisAttributeAnalysisType attribute : attributeAnalysisList) {
            Double density = attribute.getDensity();
            ItemPath itemPath = attribute.getItemPath() != null ? attribute.getItemPath().getItemPath() : null;
            Integer analysedObjectCount = attribute.getAnalysedObjectCount();
            analysisStructures.add(new AttributeAnalysisStructure(density, itemPath, complexType, analysedObjectCount));
        }
        return analysisStructures;
    }

    public AttributeAnalysisStructure(RoleAnalysisAttributeAnalysisType attributeAnalysis) {
        if (attributeAnalysis == null) {
            return;
        }
        this.itemPath = attributeAnalysis.getItemPath() != null ? attributeAnalysis.getItemPath().getItemPath() : null;
        this.density = attributeAnalysis.getDensity();
        this.description = attributeAnalysis.getDescription();
    }

    protected double calculateDensity(int relations, int possibleRelations) {
        if (relations == 0 || possibleRelations == 0) {
            return 0;
        }
        return Math.min((relations / (double) possibleRelations) * 100, 100);
    }

    public void addUniqueValues(int uniqueValues) {
        this.uniqueValues += uniqueValues;
    }

    public void addTotalValues(int totalValues) {
        this.totalValues += totalValues;
    }

    public int getUniqueValues() {
        return uniqueValues;
    }

    public int getTotalValues() {
        return totalValues;
    }

    public ItemPath getItemPath() {
        return itemPath;
    }

    public ItemPathType getItemPathType() {
        return itemPath != null ? itemPath.toBean() : null;
    }

    public double getDensity() {
        return density;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RoleAnalysisAttributeStatisticsType> getAttributeStatistics() {
        return attributeStatistics;
    }

    public void setAttributeStatistics(List<RoleAnalysisAttributeStatisticsType> attributeStatistics) {
        this.attributeStatistics = attributeStatistics;
    }

    public boolean isMultiValue() {
        return isMultiValue;
    }

    public void setMultiValue(boolean multiValue) {
        isMultiValue = multiValue;
    }

    public QName getComplexType() {
        return complexType;
    }

    public int getAnalyzedObjectsCount() {
        return analyzedObjectsCount;
    }

    public @NotNull RoleAnalysisAttributeAnalysisType buildRoleAnalysisAttributeAnalysisContainer() {
        RoleAnalysisAttributeAnalysisType attributeAnalysisContainer = new RoleAnalysisAttributeAnalysisType();
        attributeAnalysisContainer.setDensity(this.getDensity());
        attributeAnalysisContainer.setItemPath(this.getItemPathType());
        attributeAnalysisContainer.setDescription(this.getDescription());
        attributeAnalysisContainer.setParentType(this.getComplexType());
        attributeAnalysisContainer.setAnalysedObjectCount(this.getAnalyzedObjectsCount());

        //TBD need this to be stored? is unique values important? (considering for multivalued attributes)
//        attributeAnalysisContainer.setRelations((double) this.getTotalValues());
//        int possibleRelations = uniqueValues * this.getAnalyzedObjectsCount();
//        attributeAnalysisContainer.setPossibleRelation((double) possibleRelations);

        List<RoleAnalysisAttributeStatisticsType> attributeStatisticsResults = this.getAttributeStatistics();
        for (RoleAnalysisAttributeStatisticsType attributeStatistic : attributeStatisticsResults) {
            attributeAnalysisContainer.getAttributeStatistics().add(attributeStatistic);
        }

        return attributeAnalysisContainer;
    }

    //TBD
//    public static double getWeightedItemFactorConfidence(@Nullable RoleAnalysisAttributeAnalysisResult compareAttributeResult) {
//        if (compareAttributeResult == null) {
//            return 0;
//        }
//
//        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();
//        if (attributeAnalysis.isEmpty()) {
//            return 0;
//        }
//
//        double totalWeightedDensity = 0.0;
//        double totalWeight = 0.0;
//        for (RoleAnalysisAttributeAnalysis analysisItem : attributeAnalysis) {
//            Double density = analysisItem.getDensity();
//            Double weight = analysisItem.getWeight();
//
//            totalWeightedDensity += density * weight;
//            totalWeight += weight;
//        }
//
//        return totalWeight > 0 ? totalWeightedDensity / totalWeight : 0.0;
//    }
    //TBD
//    public static double calculateStandardItemConfidence(){
//        double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;
//        return itemsConfidence;
//    }

}
