/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.io.Serializable;

/**
 * Specifies limitations (via {@link #permissionProfile}) on execution of a script expression in given {@link #language}.
 *
 * Part of {@link ExpressionEvaluatorProfileImpl}.
 *
 * @author semancik
 */
@SuppressWarnings("ClassCanBeRecord")
@NullMarked
public class ScriptLanguageExpressionProfileImpl implements Serializable, ScriptLanguageExpressionProfile {

    /** Language (specified by URI) to which this profile applies. E.g. Groovy, Velocity, ... */
    private final String language;

    /** @see ScriptLanguageExpressionProfile#getDefaultDecision() */
    private final AccessDecision defaultDecision;

    /** @see ScriptLanguageExpressionProfile#isTypeChecking() */
    private final boolean typeChecking;

    /** @see ScriptLanguageExpressionProfile#getPermissionProfile() */
    @Nullable private final ExpressionPermissionProfile permissionProfile;

    public ScriptLanguageExpressionProfileImpl(
            String language,
            AccessDecision defaultDecision,
            boolean typeChecking,
            @Nullable ExpressionPermissionProfile permissionProfile) {
        this.language = language;
        this.defaultDecision = defaultDecision;
        this.typeChecking = typeChecking;
        this.permissionProfile = permissionProfile;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public AccessDecision getDefaultDecision() {
        return defaultDecision;
    }

    @Override
    public boolean isTypeChecking() {
        return typeChecking;
    }

    @Override
    public @Nullable ExpressionPermissionProfile getPermissionProfile() {
        return permissionProfile;
    }
}
