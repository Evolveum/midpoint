/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributeByDisplayValue;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ClusterExplanation implements Serializable {
    private Set<AttributeMatchExplanation> attributeExplanation;
    private String attributeValue;
    private Double weight;

    public ClusterExplanation() {
    }

    public static String resolveClusterName(Set<ClusterExplanation> clusterExplanationSet) {
        if (clusterExplanationSet == null || clusterExplanationSet.isEmpty()) {
            return null;
        }
        if (clusterExplanationSet.size() == 1) {
            return getCandidateName(clusterExplanationSet.iterator().next().getAttributeExplanation());
        }
        return null;
    }

    public static String resolveClusterName(
            @NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisSessionType session,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (!isValidInput(cluster, session)) {
            return null;
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        Set<String> ruleIdentifiers;

        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
            if (userModeOptions == null
                    || userModeOptions.getClusteringAttributeSetting() == null
                    || userModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule() == null) {
                return null;
            }
            ruleIdentifiers = extractRuleIdentifiers(userModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule());
        } else {
            RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
            if (roleModeOptions == null
                    || roleModeOptions.getClusteringAttributeSetting() == null
                    || roleModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule() == null) {
                return null;
            }
            ruleIdentifiers = extractRuleIdentifiers(roleModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule());
        }

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        Set<String> candidateNames = new HashSet<>();
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            RoleAnalysisAttributeAnalysisResult userAttributeResult = clusterStatistics.getUserAttributeAnalysisResult();
            List<RoleAnalysisAttributeAnalysis> attributeAnalysisList = userAttributeResult.getAttributeAnalysis();

            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysisList) {
                String itemPath = analysis.getItemPath();
                if (ruleIdentifiers.contains(itemPath) && analysis.getDensity() == 100) {
                    List<RoleAnalysisAttributeStatistics> attributeStatisticsList = analysis.getAttributeStatistics();
                    if (attributeStatisticsList.size() == 1) {
                        RoleAnalysisAttributeStatistics attributeStatistic = attributeStatisticsList.get(0);
                        String value = attributeStatistic.getAttributeValue();
                        RoleAnalysisAttributeDef attribute = getAttributeByDisplayValue(itemPath);

                        String candidateName;
                        if (attribute.getIdentifierType().equals(RoleAnalysisAttributeDef.IdentifierType.FINAL)) {
                            if (value.isEmpty()) {
                                candidateName = "unknown";
                            }else {
                                candidateName = itemPath + "-" + value;
                            }
                        } else {
                            PrismObject<? extends ObjectType> object;
                            object = roleAnalysisService.getObject(FocusType.class, value, task, result);

                            candidateName = object != null ? itemPath + "-" + object.getName() : itemPath + "-" + value;
                        }
                        candidateNames.add(candidateName);
                    }
                }
            }
        } else {
            RoleAnalysisAttributeAnalysisResult roleAttributeResult = clusterStatistics.getRoleAttributeAnalysisResult();
            List<RoleAnalysisAttributeAnalysis> attributeAnalysisList = roleAttributeResult.getAttributeAnalysis();

            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysisList) {
                String itemPath = analysis.getItemPath();
                if (ruleIdentifiers.contains(itemPath) && analysis.getDensity() == 100) {
                    List<RoleAnalysisAttributeStatistics> attributeStatisticsList = analysis.getAttributeStatistics();
                    if (attributeStatisticsList.size() == 1) {
                        RoleAnalysisAttributeStatistics attributeStatistic = attributeStatisticsList.get(0);
                        String value = attributeStatistic.getAttributeValue();
                        RoleAnalysisAttributeDef attribute = getAttributeByDisplayValue(itemPath);

                        String candidateName;
                        if (attribute.getIdentifierType().equals(RoleAnalysisAttributeDef.IdentifierType.FINAL)) {
                            candidateName = itemPath + "-" + value;
                        } else {
                            PrismObject<? extends ObjectType> object;
                            object = roleAnalysisService.getObject(FocusType.class, value, task, result);
                            candidateName = object != null ? itemPath + "-" + object.getName() : itemPath + "-" + value;
                        }
                        candidateNames.add(candidateName);
                    }
                }
            }
        }

        return candidateNames.size() == 1 ? candidateNames.iterator().next() : null;
    }

    private static @NotNull Set<String> extractRuleIdentifiers(@NotNull List<ClusteringAttributeRuleType> matchingRule) {
        Set<String> ruleIdentifiers = new HashSet<>();
        for (ClusteringAttributeRuleType ruleType : matchingRule) {
            ruleIdentifiers.add(ruleType.getAttributeIdentifier());
        }
        return ruleIdentifiers;
    }

    private static boolean isValidInput(RoleAnalysisClusterType cluster, RoleAnalysisSessionType session) {
        return cluster != null && session != null && isValidAnalysisOption(session);
    }

    private static boolean isValidAnalysisOption(@NotNull RoleAnalysisSessionType session) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption != null ? analysisOption.getProcessMode() : null;

        if (processMode == null) {
            return false;
        }

        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
            if (userModeOptions == null
                    || userModeOptions.getClusteringAttributeSetting() == null
                    || userModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule() == null) {
                return false;
            }
            List<ClusteringAttributeRuleType> clusteringAttributeRule = userModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule();
            return clusteringAttributeRule != null && !clusteringAttributeRule.isEmpty();
        } else {
            RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
            if (roleModeOptions == null
                    || roleModeOptions.getClusteringAttributeSetting() == null
                    || roleModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule() == null) {
                return false;
            }
            List<ClusteringAttributeRuleType> clusteringAttributeRule = roleModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule();
            return clusteringAttributeRule != null && !clusteringAttributeRule.isEmpty();
        }
    }

    public static String getClusterExplanationDescription(Set<ClusterExplanation> clusterExplanationSet) {
        if (clusterExplanationSet == null || clusterExplanationSet.isEmpty()) {
            return "No cluster explanation found.";
        }
        if (clusterExplanationSet.size() == 1) {
            return "There is a single cluster explanation.\n Cluster explanation: "
                    + getExplanation(clusterExplanationSet.iterator().next().getAttributeExplanation());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("There are multiple cluster explanations. ");

            for (ClusterExplanation explanation : clusterExplanationSet) {
                sb.append("\nCluster explanation: ")
                        .append(getExplanation(explanation.getAttributeExplanation()));
            }
            return sb.toString();
        }
    }

    public static String getExplanation(Set<AttributeMatchExplanation> attributeExplanation) {
        if (attributeExplanation == null) {
            return null;
        }

        if (attributeExplanation.size() == 1) {
            AttributeMatchExplanation explanation = attributeExplanation.iterator().next();
            return "There is a single attribute match :\n Attribute path: "
                    + explanation.getAttributePath() + " with value " + explanation.getAttributeValue() + "\n";
        } else {

            StringBuilder sb = new StringBuilder();
            for (AttributeMatchExplanation attributeMatchExplanation : attributeExplanation) {
                sb.append("Attribute path: ")
                        .append(attributeMatchExplanation.getAttributePath())
                        .append(" with value ")
                        .append(attributeMatchExplanation.getAttributeValue()).append("\n");
            }

            return "There are " + attributeExplanation.size() + " multiple attribute matches: \n" + sb;
        }
    }

    public static String getCandidateName(Set<AttributeMatchExplanation> attributeExplanation) {
        if (attributeExplanation == null) {
            return null;
        }

        if (attributeExplanation.size() == 1) {
            AttributeMatchExplanation explanation = attributeExplanation.iterator().next();

            return explanation.getAttributePath() + "_" + explanation.getAttributeValue();
        }
        return null;
    }

    public ClusterExplanation(Set<AttributeMatchExplanation> attributeExplanation, String attributeValue, Double weight) {
        this.attributeExplanation = attributeExplanation;
        this.attributeValue = attributeValue;
        this.weight = weight;
    }

    public Set<AttributeMatchExplanation> getAttributeExplanation() {
        return attributeExplanation;
    }

    public void setAttributeExplanation(Set<AttributeMatchExplanation> attributeExplanation) {
        this.attributeExplanation = attributeExplanation;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        ClusterExplanation that = (ClusterExplanation) o;
        return Objects.equals(attributeExplanation, that.attributeExplanation) &&
                Objects.equals(attributeValue, that.attributeValue) &&
                Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeExplanation, attributeValue, weight);
    }
}
