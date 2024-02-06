package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisMatchingRuleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Represents an attribute match with associated extension properties.
 */
public class AttributeMatch implements Serializable {

    private ItemPathType key;
    boolean isMultiValue;
    double similarity;
    double weight;

    public AttributeMatch(RoleAnalysisMatchingRuleType rule) {
        if (rule == null
                || rule.getMatchRule() == null
                || rule.getWeight() == null
                || rule.getItemPath() == null) {
            return;
        }

        this.key = rule.getItemPath();
        this.isMultiValue = rule.getMatchRule().isIsMultiValue();
        this.similarity = rule.getMatchRule().getMatchSimilarity();
        this.weight = rule.getWeight();
    }

    public static List<AttributeMatch> generateMatchingRulesList(List<RoleAnalysisMatchingRuleType> matchingRule) {
        List<AttributeMatch> attributeMatches = new ArrayList<>();
        for (RoleAnalysisMatchingRuleType rule : matchingRule) {

            if (rule.getMatchRule() == null ||
                    rule.getMatchRule().getMatchSimilarity() == null
                    || rule.getWeight() == null
                    || rule.getItemPath() == null) {
                continue;
            }
            attributeMatches.add(new AttributeMatch(rule));
        }
        return attributeMatches;
    }

    public ItemPathType getKey() {
        return key;
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

}
