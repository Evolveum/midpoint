/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionPackageProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionProfileType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Profile for evaluation of all kinds of expressions.
 *
 * @author Radovan Semancik
 */
public class ExpressionProfile implements Serializable { // TODO: DebugDumpable

    private static final ExpressionProfile FULL = new ExpressionProfile(
            SchemaConstants.FULL_EXPRESSION_PROFILE_ID,
            ExpressionEvaluatorsProfile.full(),
            BulkActionsProfile.full(),
            FunctionLibrariesProfile.full(),
            AccessDecision.ALLOW);

    private static final ExpressionProfile LEGACY_UNPRIVILEGED_BULK_ACTIONS = new ExpressionProfile(
            SchemaConstants.LEGACY_UNPRIVILEGED_BULK_ACTIONS_PROFILE_ID,
            ExpressionEvaluatorsProfile.none(),
            BulkActionsProfile.full(), // actions without scripts/expressions are safe
            FunctionLibrariesProfile.none(),
            AccessDecision.DENY); // this actually does not matter

    private static final ExpressionProfile NONE = new ExpressionProfile(
            SchemaConstants.NONE_EXPRESSION_PROFILE_ID,
            ExpressionEvaluatorsProfile.none(),
            BulkActionsProfile.none(),
            FunctionLibrariesProfile.none(),
            AccessDecision.DENY); // this actually does not matter

    private static final ExpressionProfile MAPPINGS_QUALITY_ASSESSMENT = new ExpressionProfile(
            SchemaConstants.MAPPINGS_QUALITY_ASSESSMENT_PROFILE_ID,
            new ExpressionEvaluatorsProfile(
                    AccessDecision.DENY,
                    List.of(new ExpressionEvaluatorProfileImpl(
                            SchemaConstantsGenerated.C_SCRIPT,
                            AccessDecision.DENY,
                            List.of(new ScriptLanguageExpressionProfileImpl(
                                    MidPointConstants.EXPRESSION_LANGUAGE_MEL_URL,
                                    AccessDecision.ALLOW,
                                    true,
                                    ExpressionPermissionProfile.closed(
                                            SchemaConstants.MAPPINGS_QUALITY_ASSESSMENT_PROFILE_ID,
                                            AccessDecision.DENY,
                                            MidPointConstants.SAFE_MEL_EXTENSIONS.stream().map(
                                                            extensionName -> new ExpressionPermissionPackageProfileType()
                                                                    .name(extensionName)
                                                                    .decision(AuthorizationDecisionType.ALLOW))
                                                    .toList(),
                                            List.of())))))),
            BulkActionsProfile.none(),
            FunctionLibrariesProfile.none(),
            AccessDecision.DENY);

    private static final ExpressionProfile AS_IS_ONLY = new ExpressionProfile(
            SchemaConstants.AS_IS_ONLY_PROFILE_ID,
            new ExpressionEvaluatorsProfile(
                    AccessDecision.DENY,
                    List.of(new ExpressionEvaluatorProfileImpl(
                            SchemaConstantsGenerated.C_AS_IS,
                            AccessDecision.ALLOW,
                            List.of()))),
            BulkActionsProfile.none(),
            FunctionLibrariesProfile.none(),
            AccessDecision.DENY);

    /**
     * Identifier of the expression profile, referencable from e.g. archetypes on which it is used.
     *
     * @see ExpressionProfileType#getIdentifier()
     */
    @NotNull private final String identifier;

    /** Profiles for individual evaluators (`script`, `path`, `value`, etc). */
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

    /**
     * "Allow all" expression profile. Used to avoid `null` values that mean "not determined".
     *
     * DANGEROUS. Use only when you know what you're doing.
     *
     * Do not use for tests. See {@code IntegrationTestTools#fullExpressionProfileForTests()} instead.
     */
    public static @NotNull ExpressionProfile full() {
        return FULL;
    }

    /**
     * Profile that forbids everything.
     *
     * Can be used as a safety mechanism to prevent any expressions from being evaluated e.g. until real profile is determined.
     */
    public static @NotNull ExpressionProfile none() {
        return NONE;
    }

    /**
     * This is a default for expressions stored in repository objects because of compatibility reasons.
     * It is set to {@link #full()}, because of backwards compatibility: setting more restrictive profile
     * may cause system to break.
     *
     * DANGEROUS. Deployments should always set a more restrictive profile as a default for authorized objects
     * in the system configuration.
     */
    public static @NotNull ExpressionProfile legacyDefaultForAuthorizedObjects() {
        return full();
    }

    /**
     * Profile that mimics the legacy non-root behavior for bulk actions: there are no expressions allowed. This ensures the
     * safety of unsafe actions: `execute-script`, `evaluate-expression`, and `notification` (with unsafe custom event handler).
     */
    public static @NotNull ExpressionProfile legacyDefaultForUnprivilegedBulkActions() {
        return LEGACY_UNPRIVILEGED_BULK_ACTIONS;
    }

    /**
     * Profile that is a legacy default behavior for privileged bulk actions: everything is permitted.
     *
     * DANGEROUS. Do not use except for legacy compatibility
     */
    public static @NotNull ExpressionProfile legacyDefaultForPrivilegedBulkActions() {
        return full();
    }

    /**
     * Profile for mappings suggested by smart integration (primarily LLMs): allows only MEL script evaluator and excludes
     * potentially dangerous modules, namely `midpoint` and `crypto`. This profile is used when evaluating AI-generated
     * or untrusted mapping scripts.
     */
    public static @NotNull ExpressionProfile mappingsQualityAssessment() {
        return MAPPINGS_QUALITY_ASSESSMENT;
    }

    /** Profile that allows "asIs" evaluator only. Safe. Used when evaluating empty expressions. */
    public static @NotNull ExpressionProfile asIsOnly() {
        return AS_IS_ONLY;
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

    public ExpressionEvaluatorProfile getEvaluatorProfile(QName qualifiedEvaluatorName) {
        return evaluatorsProfile.getEvaluatorProfile(qualifiedEvaluatorName);
    }

    public @NotNull AccessDecision getPrivilegeElevation() {
        return privilegeElevation;
    }
}
