/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis;

/**
 * Represents an attribute match with associated extension properties.
 */
public class RoleAnalysisAttributeDefConvert implements Serializable {

    RoleAnalysisAttributeDef roleAnalysisAttributeDef;
    private String attributeDisplayValue;
    boolean isMultiValue;
    double similarity;
    double weight;

    public RoleAnalysisAttributeDefConvert(ClusteringAttributeRuleType rule, @NotNull RoleAnalysisProcessModeType processMode) {
        if (rule == null
                || rule.getSimilarity() == null
                || rule.getWeight() == null
                || rule.getAttributeIdentifier() == null
                || rule.isIsMultiValue() == null) {
            return;
        }

        this.isMultiValue = rule.isIsMultiValue();
        this.similarity = rule.getSimilarity();
        this.weight = rule.getWeight();

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            for (RoleAnalysisAttributeDef attributesForRoleAnalysis : getAttributesForRoleAnalysis()) {
                if (attributesForRoleAnalysis.getDisplayValue().equals(rule.getAttributeIdentifier())) {
                    this.roleAnalysisAttributeDef = attributesForRoleAnalysis;
                    this.attributeDisplayValue = attributesForRoleAnalysis.getDisplayValue();
                }
            }
        } else {
            for (RoleAnalysisAttributeDef attributesForRoleAnalysis : getAttributesForUserAnalysis()) {
                if (attributesForRoleAnalysis.getDisplayValue().equals(rule.getAttributeIdentifier())) {
                    this.roleAnalysisAttributeDef = attributesForRoleAnalysis;
                    this.attributeDisplayValue = attributesForRoleAnalysis.getDisplayValue();
                }
            }
        }

        this.similarity = similarity * 0.01;

    }

    public static @NotNull List<RoleAnalysisAttributeDefConvert> generateMatchingRulesList(
            @NotNull List<ClusteringAttributeRuleType> matchingRule,
            @NotNull RoleAnalysisProcessModeType processMode) {
        List<RoleAnalysisAttributeDefConvert> roleAnalysisAttributeDefConverts = new ArrayList<>();
        for (ClusteringAttributeRuleType rule : matchingRule) {
            RoleAnalysisAttributeDefConvert roleAnalysisAttributeDefConvert = new RoleAnalysisAttributeDefConvert(rule, processMode);
            roleAnalysisAttributeDefConverts.add(roleAnalysisAttributeDefConvert);
        }
        return roleAnalysisAttributeDefConverts;
    }

    public boolean isMultiValue() {
        return isMultiValue;
    }

    public void setMultiValue(boolean multiValue) {
        isMultiValue = multiValue;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setMetric(double similarity) {
        this.similarity = similarity;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public RoleAnalysisAttributeDef getRoleAnalysisItemDef() {
        return roleAnalysisAttributeDef;
    }

    public void setRoleAnalysisItemDef(RoleAnalysisAttributeDef roleAnalysisAttributeDef) {
        this.roleAnalysisAttributeDef = roleAnalysisAttributeDef;
    }

    public String getAttributeDisplayValue() {
        return attributeDisplayValue;
    }

    public void setAttributeDisplayValue(String attributeDisplayValue) {
        this.attributeDisplayValue = attributeDisplayValue;
    }
}
