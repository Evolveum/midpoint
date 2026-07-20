/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionClassProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionMethodProfileType;
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
            FunctionLibrariesProfile.full(),
            AccessDecision.ALLOW);

    /**
     * Profile that mimics the legacy non-root behavior for bulk actions:
     * no expressions - this limits all of "execute-script", "notification" (with unsafe custom event handler), and
     * the new "evaluate-expression" actions.
     */
    private static final ExpressionProfile LEGACY_UNPRIVILEGED_BULK_ACTIONS = new ExpressionProfile(
            SchemaConstants.LEGACY_UNPRIVILEGED_BULK_ACTIONS_PROFILE_ID,
            ExpressionEvaluatorsProfile.none(),
            BulkActionsProfile.full(), // actions without scripts/expressions are safe
            FunctionLibrariesProfile.none(),
            AccessDecision.DENY); // actually does not matter

    /**
     * Profile that forbids everything.
     */
    private static final ExpressionProfile NONE = new ExpressionProfile(
            SchemaConstants.NONE_EXPRESSION_PROFILE_ID,
            ExpressionEvaluatorsProfile.none(),
            BulkActionsProfile.none(),
            FunctionLibrariesProfile.none(),
            AccessDecision.DENY); // actually does not matter

    /**
     * Profile for mappings quality assessment: allows only Groovy and MEL script evaluators with security restrictions.
     * Specifically denies dangerous operations like String.execute() in Groovy to prevent shell command execution.
     * This profile is used when evaluating AI-generated or untrusted mapping scripts.
     */
    public static ExpressionProfile createMappingsQualityAssessmentProfile() {
        ExpressionPermissionProfile groovyPermissionsProfile = ExpressionPermissionProfile.closed(
                "LLM Groovy scripts permission profile", AccessDecision.ALLOW, Collections.emptyList(),
                List.of(new ExpressionPermissionClassProfileType()
                        .decision(AuthorizationDecisionType.ALLOW)
                        .name("java.lang.String")
                        .method(new ExpressionPermissionMethodProfileType()
                                .name("execute")
                                .decision(AuthorizationDecisionType.DENY))));

        ExpressionEvaluatorProfile scriptEvaluatorProfile = new ExpressionEvaluatorProfile(
                SchemaConstantsGenerated.C_SCRIPT, AccessDecision.DENY, // Deny by default
                List.of(
                        new ScriptLanguageExpressionProfile(
                                "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy",
                                AccessDecision.ALLOW,
                                true,
                                groovyPermissionsProfile),
                        new ScriptLanguageExpressionProfile(
                                "http://midpoint.evolveum.com/xml/ns/public/expression/language#mel",
                                AccessDecision.ALLOW,
                                true,
                                null)));

        return new ExpressionProfile(
                SchemaConstants.MAPPINGS_QUALITY_ASSESSMENT_PROFILE_ID,
                new ExpressionEvaluatorsProfile(AccessDecision.DENY, List.of(scriptEvaluatorProfile)),
                BulkActionsProfile.none(),
                FunctionLibrariesProfile.none(),
                AccessDecision.DENY); // No privilege elevation
    }

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

    /** Are privilege elevation features (e.g. `runAsRef`) allowed? */
    @NotNull private final AccessDecision privilegeElevation;

    public ExpressionProfile(
            @NotNull String identifier,
            @NotNull ExpressionEvaluatorsProfile evaluatorsProfile,
            @NotNull BulkActionsProfile bulkActionsProfile,
            @NotNull FunctionLibrariesProfile librariesProfile,
            @NotNull AccessDecision privilegeElevation) {
        this.identifier = identifier;
        this.evaluatorsProfile = evaluatorsProfile;
        this.bulkActionsProfile = bulkActionsProfile;
        this.librariesProfile = librariesProfile;
        this.privilegeElevation = privilegeElevation;
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

    public @NotNull AccessDecision getPrivilegeElevation() {
        return privilegeElevation;
    }
}
