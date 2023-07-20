/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionProfileType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Profile for evaluation of expressions of a particular kind (e.g. script, path, value, etc).
 *
 * NOTE: This is pretty much throw-away implementation. Just the interface is important now.
 *
 * @author Radovan Semancik
 */
public class ExpressionProfile implements Serializable { // TODO: DebugDumpable

    /** "Allow all" expression profile. Used to avoid `null` values that mean "not determined". */
    private static final ExpressionProfile FULL = new ExpressionProfile(
            SchemaConstants.FULL_EXPRESSION_PROFILE_ID,
            AccessDecision.ALLOW,
            List.of());

    /**
     * Identifier of the expression profile, referencable from e.g. archetypes on which it is used.
     *
     * @see ExpressionProfileType#getIdentifier()
     */
    @NotNull private final String identifier;

    /** Default decision to be used if the suitable evaluator profile can be found. */
    @NotNull private final AccessDecision defaultDecision;

    /** Profiles for individual evaluators (e.g. script, path, value, etc). Immutable. */
    private final List<ExpressionEvaluatorProfile> evaluatorProfiles;

    public ExpressionProfile(
            @NotNull String identifier,
            @NotNull AccessDecision defaultDecision,
            List<ExpressionEvaluatorProfile> evaluatorProfiles) {
        this.identifier = identifier;
        this.defaultDecision = defaultDecision;
        this.evaluatorProfiles = List.copyOf(evaluatorProfiles);
    }

    public static @NotNull ExpressionProfile full() {
        return FULL;
    }

    public @NotNull String getIdentifier() {
        return identifier;
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

    @Override
    public String toString() {
        return "ExpressionProfile(" + identifier + ")";
    }
}
