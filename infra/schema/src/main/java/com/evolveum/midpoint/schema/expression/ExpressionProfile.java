/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionProfileType;

import org.jetbrains.annotations.NotNull;

/**
 * Profile for evaluation of "regular" expressions, scripting expressions, and function libraries.
 *
 * NOTE: This is pretty much throw-away implementation. Just the interface is important now.
 *
 * @author Radovan Semancik
 */
public class ExpressionProfile implements Serializable { // TODO: DebugDumpable

    /** "Allow all" expression profile. Used to avoid `null` values that mean "not determined". */
    private static final ExpressionProfile FULL = new ExpressionProfile(
            SchemaConstants.FULL_EXPRESSION_PROFILE_ID,
            ExpressionEvaluatorsProfile.full(),
            ScriptingProfile.full(), // TODO what about scripts etc that currently require #all authorization?
            FunctionLibrariesProfile.full());

    /**
     * Profile that mimics the legacy non-root behavior for bulk actions:
     * no expressions - this limits all of "execute-script", "notification" (with unsafe custom event handler), and
     * the new "evaluate-expression" actions.
     */
    private static final ExpressionProfile SCRIPTING_LEGACY_UNPRIVILEGED = new ExpressionProfile(
            SchemaConstants.LEGACY_UNPRIVILEGED_SCRIPTING_PROFILE_ID,
            ExpressionEvaluatorsProfile.none(),
            ScriptingProfile.full(), // actions without scripts/expressions are safe
            FunctionLibrariesProfile.none());

    /**
     * Identifier of the expression profile, referencable from e.g. archetypes on which it is used.
     *
     * @see ExpressionProfileType#getIdentifier()
     */
    @NotNull private final String identifier;

    @NotNull private final ExpressionEvaluatorsProfile evaluatorsProfile;

    /** Profile for midPoint scripting language (bulk actions). */
    @NotNull private final ScriptingProfile scriptingProfile;

    /** Profile for using function libraries. */
    @NotNull private final FunctionLibrariesProfile librariesProfile;

    public ExpressionProfile(
            @NotNull String identifier,
            @NotNull ExpressionEvaluatorsProfile evaluatorsProfile,
            @NotNull ScriptingProfile scriptingProfile,
            @NotNull FunctionLibrariesProfile librariesProfile) {
        this.identifier = identifier;
        this.evaluatorsProfile = evaluatorsProfile;
        this.scriptingProfile = scriptingProfile;
        this.librariesProfile = librariesProfile;
    }

    public static @NotNull ExpressionProfile full() {
        return FULL;
    }

    public static @NotNull ExpressionProfile scriptingLegacyUnprivileged() {
        return SCRIPTING_LEGACY_UNPRIVILEGED;
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public @NotNull ScriptingProfile getScriptingProfile() {
        return scriptingProfile;
    }

    public @NotNull FunctionLibrariesProfile getLibrariesProfile() {
        return librariesProfile;
    }

    @Override
    public String toString() {
        return "ExpressionProfile(ID: %s; scripting: %s; libraries: %s)".formatted(
                identifier, scriptingProfile.getIdentifier(), librariesProfile.getIdentifier());
    }

    public @NotNull ExpressionEvaluatorsProfile getEvaluatorsProfile() {
        return evaluatorsProfile;
    }
}
