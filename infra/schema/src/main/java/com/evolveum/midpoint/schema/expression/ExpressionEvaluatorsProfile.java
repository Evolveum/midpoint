/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Specifies limitations on the use of a individual expression evaluators (e.g. script, path, value, etc).
 */
public class ExpressionEvaluatorsProfile implements Serializable {

    // Here could be an identifier, but this profile is currently (from the configuration point of view) embedded
    // right in ExpressionProfileType. So it does not need an identifier.

    /** Default decision to be used if the suitable evaluator profile can be found. */
    @NotNull private final AccessDecision defaultDecision;

    /** Profiles for individual evaluators (e.g. script, path, value, etc). Immutable. Keyed by qualified evaluator name. */
    @NotNull private final Map<QName, ExpressionEvaluatorProfileImpl> evaluatorProfilesMap;

    /** "Allow all" profile. */
    private static final ExpressionEvaluatorsProfile FULL = new ExpressionEvaluatorsProfile(
            AccessDecision.ALLOW,
            List.of());

    /** "Allow none" profile. */
    private static final ExpressionEvaluatorsProfile NONE = new ExpressionEvaluatorsProfile(
            AccessDecision.DENY,
            List.of());

    public ExpressionEvaluatorsProfile(
            @NotNull AccessDecision defaultDecision,
            @NotNull List<ExpressionEvaluatorProfileImpl> evaluatorProfiles) {
        this.defaultDecision = defaultDecision;
        this.evaluatorProfilesMap = evaluatorProfiles.stream()
                .collect(Collectors.toUnmodifiableMap(ExpressionEvaluatorProfileImpl::getType, p -> p));
    }

    public static @NotNull ExpressionEvaluatorsProfile full() {
        return FULL;
    }

    public static @NotNull ExpressionEvaluatorsProfile none() {
        return NONE;
    }

    @NotNull ExpressionEvaluatorProfile getEvaluatorProfile(@NotNull QName qualifiedEvaluatorName) {
        Preconditions.checkArgument(
                QNameUtil.isQualified(qualifiedEvaluatorName),
                "Expression evaluator name must be qualified: %s", qualifiedEvaluatorName);
        return Objects.requireNonNullElseGet(
                evaluatorProfilesMap.get(qualifiedEvaluatorName),
                () -> ExpressionEvaluatorProfile.forDecision(defaultDecision));
    }
}
