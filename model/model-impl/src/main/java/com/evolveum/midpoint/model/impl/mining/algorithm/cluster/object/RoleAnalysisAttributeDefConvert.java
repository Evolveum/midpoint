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

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

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
                || rule.getPath() == null) {
            return;
        }

        ItemPath path = rule.getPath().getItemPath();
        PrismObjectDefinition<?> objectDef = getObjectDefinition(processMode);
        RoleAnalysisAttributeDef attributeForAnalysis = new RoleAnalysisAttributeDef(path, objectDef.findItemDefinition(path),
                processMode.equals(RoleAnalysisProcessModeType.ROLE) ? RoleType.class : UserType.class);

        this.isMultiValue = attributeForAnalysis.isMultiValue();
        this.roleAnalysisAttributeDef = attributeForAnalysis;
        this.attributeDisplayValue = attributeForAnalysis.getDisplayValue();
        this.similarity = similarity * 0.01;
        this.similarity = rule.getSimilarity();
        this.weight = rule.getWeight();
    }

    private PrismObjectDefinition<?> getObjectDefinition(@NotNull RoleAnalysisProcessModeType processMode) {
        return processMode.equals(RoleAnalysisProcessModeType.ROLE)
                ? PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class)
                : PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    }

    public static @NotNull List<RoleAnalysisAttributeDefConvert> generateMatchingRulesList(
            ClusteringAttributeSettingType clusteringSettings,
            @NotNull RoleAnalysisProcessModeType processMode) {
        if (clusteringSettings == null) {
            return new ArrayList<>();
        }
        List<ClusteringAttributeRuleType> matchingRule = clusteringSettings.getClusteringAttributeRule();
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
