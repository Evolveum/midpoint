/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BulkActionProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BulkActionsProfileType;
import org.jetbrains.annotations.Nullable;

/**
 * Specifies limitations on the use of a scripting actions. It is a compiled form of a {@link BulkActionsProfileType}.
 *
 * Could be named also `ScriptingActionsProfile` but maybe it will contain more than actions in the future.
 */
public class BulkActionsProfile extends AbstractSecurityProfile {

    /** Scripting actions profiles, keyed by action name (both legacy and modern ones can be used). Unmodifiable. */
    @NotNull private final Map<String, BulkActionProfile> actionProfiles;

    /** "Allow all" profile. */
    private static final BulkActionsProfile FULL = new BulkActionsProfile(
            SchemaConstants.FULL_EXPRESSION_PROFILE_ID,
            AccessDecision.ALLOW,
            Map.of());

    /** "Allow nothing" profile. */
    private static final BulkActionsProfile NONE = new BulkActionsProfile(
            SchemaConstants.NONE_EXPRESSION_PROFILE_ID,
            AccessDecision.DENY,
            Map.of());

    private BulkActionsProfile(
            @NotNull String identifier,
            @NotNull AccessDecision defaultDecision,
            @NotNull Map<String, BulkActionProfile> actionProfiles) {
        super(identifier, defaultDecision);
        this.actionProfiles = actionProfiles;
    }

    public static @NotNull BulkActionsProfile full() {
        return FULL;
    }

    public static @NotNull BulkActionsProfile none() {
        return NONE;
    }

    public static BulkActionsProfile of(@NotNull BulkActionsProfileType bean) throws ConfigurationException {
        String identifier = MiscUtil.configNonNull(bean.getIdentifier(), "No identifier in scripting profile %s", bean);
        Map<String, BulkActionProfile> actionProfileMap = new HashMap<>();
        for (BulkActionProfileType actionBean : bean.getAction()) {
            var actionProfile = BulkActionProfile.of(actionBean);
            actionProfileMap.put(actionProfile.action(), actionProfile);
        }
        return new BulkActionsProfile(
                identifier,
                AccessDecision.translate(
                        MiscUtil.configNonNull(
                                bean.getDecision(), "No decision in scripting profile %s", bean.getIdentifier())),
                Collections.unmodifiableMap(actionProfileMap));
    }

    public @NotNull AccessDecision decideActionAccess(
            @NotNull String legacyActionName, @Nullable String configurationElementName) {
        var byLegacyName = actionProfiles.get(legacyActionName);
        if (byLegacyName != null) {
            return byLegacyName.decision();
        }
        if (configurationElementName != null) {
            var byConfigurationElementName = actionProfiles.get(configurationElementName);
            if (byConfigurationElementName != null) {
                return byConfigurationElementName.decision();
            }
        }
        return getDefaultDecision();
    }
}
