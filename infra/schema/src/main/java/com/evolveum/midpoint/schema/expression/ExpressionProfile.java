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
 * Profile for evaluation of "regular" expressions, bulk actions, and function libraries.
 *
 * @author Radovan Semancik
 */
public class ExpressionProfile implements Serializable { // TODO: DebugDumpable

    /** "Allow all" expression profile. Used to avoid `null` values that mean "not determined". */
    private static final ExpressionProfile FULL = new ExpressionProfile(
            SchemaConstants.FULL_EXPRESSION_PROFILE_ID,
            ExpressionEvaluatorsProfile.full(),
            BulkActionsProfile.full(),
            FunctionLibrariesProfile.full());

    /**
     * Profile that mimics the legacy non-root behavior for bulk actions:
     * no expressions - this limits all of "execute-script", "notification" (with unsafe custom event handler), and
     * the new "evaluate-expression" actions.
     */
    private static final ExpressionProfile LEGACY_UNPRIVILEGED_BULK_ACTIONS = new ExpressionProfile(
            SchemaConstants.LEGACY_UNPRIVILEGED_BULK_ACTIONS_PROFILE_ID,
            ExpressionEvaluatorsProfile.none(),
            BulkActionsProfile.full(), // actions without scripts/expressions are safe
            FunctionLibrariesProfile.none());

    /**
     * Profile that forbids everything.
     */
    private static final ExpressionProfile NONE = new ExpressionProfile(
            SchemaConstants.NONE_EXPRESSION_PROFILE_ID,
            ExpressionEvaluatorsProfile.none(),
            BulkActionsProfile.none(),
            FunctionLibrariesProfile.none());

    /**
     * Identifier of the expression profile, referencable from e.g. archetypes on which it is used.
     *
     * @see ExpressionProfileType#getIdentifier()
     */
    @NotNull private final String identifier;

    @NotNull private final ExpressionEvaluatorsProfile evaluatorsProfile;

    /** Profile for midPoint scripting language (bulk actions). */
    @NotNull private final BulkActionsProfile bulkActionsProfile;

    /** Profile for using function libraries. */
    @NotNull private final FunctionLibrariesProfile librariesProfile;

    public ExpressionProfile(
            @NotNull String identifier,
            @NotNull ExpressionEvaluatorsProfile evaluatorsProfile,
            @NotNull BulkActionsProfile bulkActionsProfile,
            @NotNull FunctionLibrariesProfile librariesProfile) {
        this.identifier = identifier;
        this.evaluatorsProfile = evaluatorsProfile;
        this.bulkActionsProfile = bulkActionsProfile;
        this.librariesProfile = librariesProfile;
    }

    public static @NotNull ExpressionProfile full() {
        return FULL;
    }

    public static @NotNull ExpressionProfile none() {
        return NONE;
    }

    public static @NotNull ExpressionProfile legacyUnprivilegedBulkActions() {
        return LEGACY_UNPRIVILEGED_BULK_ACTIONS;
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public @NotNull BulkActionsProfile getScriptingProfile() {
        return bulkActionsProfile;
    }

    public @NotNull FunctionLibrariesProfile getLibrariesProfile() {
        return librariesProfile;
    }

    @Override
    public String toString() {
        return "ExpressionProfile(ID: %s; scripting: %s; libraries: %s)".formatted(
                identifier, bulkActionsProfile.getIdentifier(), librariesProfile.getIdentifier());
    }

    public @NotNull ExpressionEvaluatorsProfile getEvaluatorsProfile() {
        return evaluatorsProfile;
    }
}
