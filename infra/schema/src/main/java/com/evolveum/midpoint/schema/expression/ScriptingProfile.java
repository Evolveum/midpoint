/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptingActionProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptingProfileType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * Specifies limitations on the use of a scripting actions. It is a compiled form of a {@link ScriptingProfileType}.
 *
 * Could be named also `ScriptingActionsProfile` but maybe it will contain more than actions in the future.
 */
public class ScriptingProfile extends AbstractSecurityProfile {

    /** Scripting actions profiles, keyed by action name. Unmodifiable. */
    @NotNull private final Map<String, ScriptingActionProfile> actionProfiles;

    /** "Allow all" profile. */
    private static final ScriptingProfile FULL = new ScriptingProfile(
            SchemaConstants.FULL_EXPRESSION_PROFILE_ID,
            AccessDecision.ALLOW,
            Map.of());

    private ScriptingProfile(
            @NotNull String identifier,
            @NotNull AccessDecision defaultDecision,
            @NotNull Map<String, ScriptingActionProfile> actionProfiles) {
        super(identifier, defaultDecision);
        this.actionProfiles = actionProfiles;
    }

    public static @NotNull ScriptingProfile full() {
        return FULL;
    }

    public static ScriptingProfile of(@NotNull ScriptingProfileType bean) throws ConfigurationException {
        String identifier = MiscUtil.configNonNull(bean.getIdentifier(), "No identifier in scripting profile %s", bean);
        Map<String, ScriptingActionProfile> actionProfileMap = new HashMap<>();
        for (ScriptingActionProfileType actionBean : bean.getAction()) {
            var actionProfile = ScriptingActionProfile.of(actionBean);
            actionProfileMap.put(actionProfile.action(), actionProfile);
        }
        return new ScriptingProfile(
                identifier,
                AccessDecision.translate(
                        MiscUtil.configNonNull(
                                bean.getDecision(), "No decision in scripting profile %s", bean.getIdentifier())),
                Collections.unmodifiableMap(actionProfileMap));
    }

    public @NotNull Map<String, ScriptingActionProfile> getActionProfiles() {
        return actionProfiles;
    }
}
