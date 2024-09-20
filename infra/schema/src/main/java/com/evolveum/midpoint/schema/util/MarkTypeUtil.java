/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleEvaluationTargetType.OBJECT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleEvaluationTargetType.PROJECTION;

/**
 * Utilities for {@link MarkType}.
 */
public class MarkTypeUtil {

    public static @Nullable SimulationObjectPredicateType getSimulationDomain(@NotNull MarkType tag) {
        EventMarkInformationType eventMarkInfo = tag.getEventMark();
        if (eventMarkInfo == null) {
            return null;
        }
        EventMarkDomainType domain = eventMarkInfo.getDomain();
        if (domain == null) {
            return null;
        }
        return domain.getSimulation();
    }

    public static boolean attachedRuleEvaluatesOnProjection(@NotNull MarkType tag) {
        return tag.getPolicyRule().stream()
                .anyMatch(rule -> rule.getEvaluationTarget() == PROJECTION);
    }

    public static boolean attachedRuleEvaluatesOnFocus(@NotNull MarkType tag) {
        return tag.getPolicyRule().stream()
                .anyMatch(rule -> {
                    PolicyRuleEvaluationTargetType target = rule.getEvaluationTarget();
                    return target == null || target == OBJECT;
                });
    }

    public static boolean isEnabledByDefault(@NotNull MarkType mark) {
        EventMarkInformationType eventMarkInfo = mark.getEventMark();
        Boolean enabledByDefault = eventMarkInfo != null ? eventMarkInfo.isEnabledByDefault() : null;
        return !Boolean.FALSE.equals(enabledByDefault);
    }

    /** Returns {@code true} if the mark was assigned by a rule in a transitional way. Determined from the value metadata. */
    public static boolean isTransitional(@NotNull ObjectReferenceType markRef) {
        for (var metadataBean : ValueMetadataTypeUtil.getMetadataBeans(markRef)) {
            var provenance = metadataBean.getProvenance();
            if (provenance == null) {
                continue;
            }
            var markingRule = provenance.getMarkingRule();
            if (markingRule != null && Boolean.TRUE.equals(markingRule.getTransitional())) {
                return true;
            }
            var policyRule = provenance.getPolicyRule();
            if (policyRule != null && Boolean.TRUE.equals(policyRule.getTransitional())) {
                return true;
            }
        }
        return false;
    }

    /** Returns {@code true} if this value was added by a marking rule (regardless of whether there are any statements for it). */
    public static boolean isAddedByMarkingRule(@NotNull ObjectReferenceType markRef) {
        return ValueMetadataTypeUtil.getMetadataBeans(markRef).stream()
                .map(ValueMetadataType::getProvenance)
                .filter(Objects::nonNull)
                .anyMatch(provenance -> provenance.getMarkingRule() != null);
    }

    /** Returns {@code true} if this value was added by a policy rule (regardless of whether there are any statements for it). */
    public static boolean isAddedByPolicyRule(@NotNull ObjectReferenceType markRef) {
        return ValueMetadataTypeUtil.getMetadataBeans(markRef).stream()
                .map(ValueMetadataType::getProvenance)
                .filter(Objects::nonNull)
                .anyMatch(provenance -> provenance.getPolicyRule() != null);
    }

    /**
     * Returns {@code true} if this value was added by a policy statement (positive or negative);
     * regardless of any other source(s).
     */
    public static boolean isAddedByPolicyStatement(@NotNull ObjectReferenceType markRef) {
        return ValueMetadataTypeUtil.getMetadataBeans(markRef).stream()
                .map(ValueMetadataType::getProvenance)
                .filter(Objects::nonNull)
                .anyMatch(provenance -> provenance.getPolicyStatement() != null);
    }

    /**
     * Mark references stored under `effectiveMarkRef` can be really effective or not effective: when a mark is provided
     * by a marking/policy rule, but overridden by a policy statement, the respective value is kept in the list, because
     * we need the value metadata for it. It is marked by `org:related` relation.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isEffective(@NotNull ObjectReferenceType markRef) {
        return SchemaService.get().relationRegistry().isMember(markRef.getRelation());
    }
}
