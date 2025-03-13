/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributeByItemPath;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

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

        AbstractAnalysisSessionOptionType sessionOptionType = getSessionOption(session, processMode);
        if (sessionOptionType == null) {
            return null;
        }

        Set<ItemPath> ruleIdentifiers = extractRuleIdentifiers(
                sessionOptionType.getClusteringAttributeSetting().getClusteringAttributeRule());

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        Set<String> candidateNames = new HashSet<>();
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            RoleAnalysisAttributeAnalysisResultType userAttributeResult = clusterStatistics.getUserAttributeAnalysisResult();
            List<RoleAnalysisAttributeAnalysisType> attributeAnalysisList = userAttributeResult.getAttributeAnalysis();

            collectUserCandidateNames(roleAnalysisService,
                    task,
                    result,
                    attributeAnalysisList,
                    ruleIdentifiers,
                    sessionOptionType,
                    candidateNames);
        } else {
            List<ObjectReferenceType> member = cluster.getMember();
            Set<PrismObject<RoleType>> prismRoles = new HashSet<>();

            cacheRoles(roleAnalysisService, task, result, member, prismRoles);

            List<ClusteringAttributeRuleType> clusteringAttributeRule = getClusteringAttributeRules(session);
            PrismObjectDefinition<RoleType> roleDefinition = PrismContext.get().getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(RoleType.class);

            Map<ItemPath, ItemDefinition<?>> itemDefinitionMap = new HashMap<>();
            List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = analyseRolesClusteringAttributes(
                    roleAnalysisService,
                    task,
                    result,
                    clusteringAttributeRule,
                    roleDefinition,
                    itemDefinitionMap,
                    prismRoles);

            if (roleAttributeAnalysisStructures == null || roleAttributeAnalysisStructures.isEmpty()) {
                return null;
            }

            for (AttributeAnalysisStructure analysis : roleAttributeAnalysisStructures) {
                ItemPath itemPath = analysis.getItemPath();
                if (itemPath == null) {
                    continue;
                }

                collectRoleCandidateNames(roleAnalysisService,
                        task,
                        result,
                        analysis,
                        ruleIdentifiers,
                        itemPath,
                        itemDefinitionMap,
                        candidateNames);
            }
        }

        return candidateNames.size() == 1 ? candidateNames.iterator().next() : null;
    }

    private static @Nullable AbstractAnalysisSessionOptionType getSessionOption(
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProcessModeType processMode) {
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
            return (userModeOptions != null && userModeOptions.getClusteringAttributeSetting() != null
                    && userModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule() != null)
                    ? userModeOptions
                    : null;
        } else {
            RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
            return (roleModeOptions != null && roleModeOptions.getClusteringAttributeSetting() != null
                    && roleModeOptions.getClusteringAttributeSetting().getClusteringAttributeRule() != null)
                    ? roleModeOptions
                    : null;
        }
    }

    private static void collectUserCandidateNames(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeAnalysisType> attributeAnalysisList,
            Set<ItemPath> ruleIdentifiers,
            AbstractAnalysisSessionOptionType sessionOptionType,
            Set<String> candidateNames) {

        for (RoleAnalysisAttributeAnalysisType analysis : attributeAnalysisList) {
            ItemPathType itemPathType = analysis.getItemPath();
            if (itemPathType == null || analysis.getDensity() != 100) {
                continue;
            }

            ItemPath itemPath = itemPathType.getItemPath();
            if (!containRuleItemPath(ruleIdentifiers, itemPath)) {
                continue;
            }

            List<RoleAnalysisAttributeStatisticsType> attributeStatisticsList = analysis.getAttributeStatistics();
            if (attributeStatisticsList.size() != 1) {
                continue;
            }

            RoleAnalysisAttributeStatisticsType attributeStatistic = attributeStatisticsList.get(0);
            String value = attributeStatistic.getAttributeValue();
            RoleAnalysisAttributeDef attribute = getAttributeByItemPath(itemPath, sessionOptionType.getUserAnalysisAttributeSetting());

            if (attribute == null) {
                continue;
            }

            candidateNames.add(generateCandidateName(roleAnalysisService, task, result, itemPath, value, attribute.isReference()));
        }
    }

    private static void collectRoleCandidateNames(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result,
            AttributeAnalysisStructure analysis,
            @NotNull Set<ItemPath> ruleIdentifiers,
            ItemPath itemPath,
            Map<ItemPath, ItemDefinition<?>> itemDefinitionMap,
            Set<String> candidateNames) {

        if (!ruleIdentifiers.contains(itemPath) || analysis.getDensity() != 100) {
            return;
        }

        List<RoleAnalysisAttributeStatisticsType> attributeStatisticsList = analysis.getAttributeStatistics();
        if (attributeStatisticsList.size() != 1) {
            return;
        }

        RoleAnalysisAttributeStatisticsType attributeStatistic = attributeStatisticsList.get(0);
        String value = attributeStatistic.getAttributeValue();
        if (value == null) {
            return;
        }

        QName itemTargetType = itemDefinitionMap.get(itemPath).getTypeName();
        boolean isReference = itemTargetType.equals(ObjectReferenceType.COMPLEX_TYPE);

        candidateNames.add(generateCandidateName(roleAnalysisService, task, result, itemPath, value, isReference));
    }

    private static @NotNull String generateCandidateName(
            RoleAnalysisService roleAnalysisService,
            Task task,
            OperationResult result,
            ItemPath itemPath,
            @NotNull String value,
            boolean isReference) {

        if (value.isEmpty()) {
            return "unknown";
        }

        if (isReference) {
            PrismObject<? extends ObjectType> object = roleAnalysisService.getObject(FocusType.class, value, task, result);
            return itemPath + "-" + (object != null ? object.getName() : value);
        }

        return itemPath.last() + "-" + value;
    }


    private static List<AttributeAnalysisStructure> analyseRolesClusteringAttributes(@NotNull RoleAnalysisService roleAnalysisService, @NotNull Task task, @NotNull OperationResult result, List<ClusteringAttributeRuleType> clusteringAttributeRule, PrismObjectDefinition<RoleType> roleDefinition, Map<ItemPath, ItemDefinition<?>> itemDefinitionMap, Set<PrismObject<RoleType>> prismRoles) {
        List<RoleAnalysisAttributeDef> attributeRoleDefSet = new ArrayList<>();
        for (ClusteringAttributeRuleType rule : clusteringAttributeRule) {
            ItemPathType path = rule.getPath();
            if (path == null) {
                continue;
            }
            ItemPath itemPath = path.getItemPath();
            ItemDefinition<?> itemDefinition = roleDefinition.findItemDefinition(itemPath);
            itemDefinitionMap.put(itemPath, itemDefinition);
            attributeRoleDefSet.add(new RoleAnalysisAttributeDef(itemPath, itemDefinition, RoleType.class));
        }

        List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = roleAnalysisService
                .roleTypeAttributeAnalysis(prismRoles, 100.0, task, result, attributeRoleDefSet);
        return roleAttributeAnalysisStructures;
    }

    private static List<ClusteringAttributeRuleType> getClusteringAttributeRules(RoleAnalysisSessionType session) {
        RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
        ClusteringAttributeSettingType clusteringAttributeSetting = roleModeOptions.getClusteringAttributeSetting();
        List<ClusteringAttributeRuleType> clusteringAttributeRule = clusteringAttributeSetting.getClusteringAttributeRule();
        return clusteringAttributeRule;
    }

    private static void cacheRoles(@NotNull RoleAnalysisService roleAnalysisService, @NotNull Task task, @NotNull OperationResult result, List<ObjectReferenceType> member, Set<PrismObject<RoleType>> prismRoles) {
        for (ObjectReferenceType objectReferenceType : member) {
            PrismObject<RoleType> role = roleAnalysisService.getObject(RoleType.class, objectReferenceType.getOid(), task, result);
            if (role != null) {
                prismRoles.add(role);
            }
        }
    }

    private static boolean containRuleItemPath(@NotNull Set<ItemPath> ruleIdentifiers, ItemPath itemPath) {
        for (ItemPath ruleIdentifier : ruleIdentifiers) {
            if (ruleIdentifier.equivalent(itemPath)) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull Set<ItemPath> extractRuleIdentifiers(@NotNull List<ClusteringAttributeRuleType> matchingRule) {
        Set<ItemPath> ruleIdentifiers = new HashSet<>();
        for (ClusteringAttributeRuleType ruleType : matchingRule) {
            ItemPathType path = ruleType.getPath();
            if (path == null) {
                continue;
            }
            ruleIdentifiers.add(path.getItemPath());
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
