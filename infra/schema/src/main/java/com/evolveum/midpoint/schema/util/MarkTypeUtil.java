/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
