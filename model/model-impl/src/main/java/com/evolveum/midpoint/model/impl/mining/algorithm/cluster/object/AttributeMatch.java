package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisMatchingRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis;

/**
 * Represents an attribute match with associated extension properties.
 */
public class AttributeMatch implements Serializable {

    RoleAnalysisAttributeDef roleAnalysisAttributeDef;
    private String attributeDisplayValue;
    boolean isMultiValue;
    double similarity;
    double weight;

    public AttributeMatch(RoleAnalysisMatchingRuleType rule, @NotNull RoleAnalysisProcessModeType processMode) {
        if (rule == null
                || rule.getMatchRule() == null
                || rule.getWeight() == null) {
            return;
        }

        this.isMultiValue = rule.getMatchRule().isIsMultiValue();
        this.similarity = rule.getMatchRule().getMatchSimilarity();
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

        if (rule.getMatchRule().getMatchSimilarity() != null) {
            Double matchSimilarity = rule.getMatchRule().getMatchSimilarity();
            this.similarity = matchSimilarity * 0.01;
        }
    }

    public static @NotNull List<AttributeMatch> generateMatchingRulesList(
            @NotNull List<RoleAnalysisMatchingRuleType> matchingRule,
            @NotNull RoleAnalysisProcessModeType processMode) {
        List<AttributeMatch> attributeMatches = new ArrayList<>();
        for (RoleAnalysisMatchingRuleType rule : matchingRule) {
            AttributeMatch attributeMatch = new AttributeMatch(rule, processMode);
            attributeMatches.add(attributeMatch);
        }
        return attributeMatches;
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
