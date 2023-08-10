/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /** Profiles for individual evaluators (e.g. script, path, value, etc). Immutable. */
    @NotNull private final List<ExpressionEvaluatorProfile> evaluatorProfiles;

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
            @NotNull List<ExpressionEvaluatorProfile> evaluatorProfiles) {
        this.defaultDecision = defaultDecision;
        this.evaluatorProfiles = evaluatorProfiles;
    }

    public static @NotNull ExpressionEvaluatorsProfile full() {
        return FULL;
    }

    public static @NotNull ExpressionEvaluatorsProfile none() {
        return NONE;
    }

    public @NotNull AccessDecision getDefaultDecision() {
        return defaultDecision;
    }

    public @Nullable ExpressionEvaluatorProfile getEvaluatorProfile(@NotNull QName type) {
        for (ExpressionEvaluatorProfile evaluatorProfile : evaluatorProfiles) {
            if (QNameUtil.match(evaluatorProfile.getType(), type)) {
                return evaluatorProfile;
            }
        }
        return null;
    }
}
