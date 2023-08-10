/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Specifies limitations (via {@link #permissionProfile}) on execution of a script expression in given {@link #language}.
 *
 * Part of {@link ExpressionEvaluatorProfile}.
 *
 * @author semancik
 */
@SuppressWarnings("ClassCanBeRecord")
public class ScriptLanguageExpressionProfile implements Serializable {

    /** Language (specified by URI) to which this profile applies. E.g. Groovy, Velocity, ... */
    @NotNull private final String language;

    /** Decision to be used if #permissionProfile does not provide its own. */
    @NotNull private final AccessDecision defaultDecision;

    /**
     * Should we apply strict type checking when evaluating the script? It is a prerequisite for using permission profiles,
     * i.e. if turned off, the execution will NOT start with permission profile set.
     *
     * TODO currently used only for Groovy.
     */
    private final boolean typeChecking;

    /** Details about what packages, classes and methods are allowed to be used in the script. */
    @Nullable private final ExpressionPermissionProfile permissionProfile;

    public ScriptLanguageExpressionProfile(
            @NotNull String language,
            @NotNull AccessDecision defaultDecision,
            boolean typeChecking,
            @Nullable ExpressionPermissionProfile permissionProfile) {
        this.language = language;
        this.defaultDecision = defaultDecision;
        this.typeChecking = typeChecking;
        this.permissionProfile = permissionProfile;
    }

    public @NotNull String getLanguage() {
        return language;
    }

    public @NotNull AccessDecision getDefaultDecision() {
        return defaultDecision;
    }

    public boolean isTypeChecking() {
        return typeChecking;
    }

    public @Nullable ExpressionPermissionProfile getPermissionProfile() {
        return permissionProfile;
    }

    public boolean hasRestrictions() {
        return permissionProfile != null && permissionProfile.hasRestrictions();
    }

    public @NotNull AccessDecision decideClassAccess(String className, String methodName) {
        if (permissionProfile == null) {
            return defaultDecision;
        }
        AccessDecision permissionDecision = permissionProfile.decideClassAccess(className, methodName);
        if (permissionDecision == AccessDecision.DEFAULT) {
            return defaultDecision;
        }
        return permissionDecision;
    }
}
