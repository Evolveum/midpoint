/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.analysis;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    String itemPath;
    double density;
    String description;
    List<RoleAnalysisAttributeStatistics> attributeStatistics = new ArrayList<>();
    boolean isMultiValue;
    QName complexType;

    public AttributeAnalysisStructure(int uniqueValues, int objectCount, int totalValues, String itemPath) {
        this.uniqueValues = uniqueValues;
        this.totalValues = totalValues;
        int possibleRelations = uniqueValues * objectCount;
        this.density = calculateDensity(totalValues, possibleRelations);
        this.itemPath = itemPath;
    }

    public AttributeAnalysisStructure(double density, String itemPath) {
        this.density = density;
        this.itemPath = itemPath;
    }

    public AttributeAnalysisStructure(double density, String itemPath, QName complexType) {
        this.density = density;
        this.itemPath = itemPath;
        this.complexType = complexType;
    }

    public static @NotNull List<AttributeAnalysisStructure> extractAttributeAnalysis(@NotNull RoleAnalysisClusterType cluster) {
        List<AttributeAnalysisStructure> attributeAnalysisStructures = new ArrayList<>();

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        if (clusterStatistics == null) {
            return attributeAnalysisStructures;
        }

        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();

        attributeAnalysisStructures.addAll(
                extractAttributeAnalysis(userAttributeAnalysisResult.getAttributeAnalysis(), UserType.COMPLEX_TYPE));
        attributeAnalysisStructures.addAll(
                extractAttributeAnalysis(roleAttributeAnalysisResult.getAttributeAnalysis(), RoleType.COMPLEX_TYPE));

        return attributeAnalysisStructures;
    }

    public static @NotNull List<AttributeAnalysisStructure> extractAttributeAnalysis(
            @NotNull List<RoleAnalysisAttributeAnalysis> attributeAnalysisList,
            @NotNull QName complexType) {
        List<AttributeAnalysisStructure> analysisStructures = new ArrayList<>();
        for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysisList) {
            Double density = attribute.getDensity();
            String itemPath = attribute.getItemPath();
            analysisStructures.add(new AttributeAnalysisStructure(density, itemPath, complexType));
        }
        return analysisStructures;
    }

    public AttributeAnalysisStructure(RoleAnalysisAttributeAnalysis attributeAnalysis) {
        if (attributeAnalysis == null) {
            return;
        }
        this.itemPath = attributeAnalysis.getItemPath();
        this.density = attributeAnalysis.getDensity();
        this.description = attributeAnalysis.getDescription();
    }

    public void addUniqueValues(int uniqueValues) {
        this.uniqueValues += uniqueValues;
    }

    public void addTotalValues(int totalValues) {
        this.totalValues += totalValues;
    }

    protected double calculateDensity(int relations, int possibleRelations) {
        if (relations == 0 || possibleRelations == 0) {
            return 0;
        }
        return Math.min((relations / (double) possibleRelations) * 100, 100);
    }

    public int getUniqueValues() {
        return uniqueValues;
    }

    public int getTotalValues() {
        return totalValues;
    }

    public String getItemPath() {
        return itemPath;
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

    public List<RoleAnalysisAttributeStatistics> getAttributeStatistics() {
        return attributeStatistics;
    }

    public void setAttributeStatistics(List<RoleAnalysisAttributeStatistics> attributeStatistics) {
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

    public void setComplexType(QName complexType) {
        this.complexType = complexType;
    }
}
