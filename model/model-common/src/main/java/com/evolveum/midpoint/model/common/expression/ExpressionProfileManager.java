/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.SystemObjectCache;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Manages (cached) expression profiles.
 *
 * Because of implementation reasons, the profiles are cached within {@link SystemObjectCache}.
 * This probably should change in the future, along with moving this class to `repo-common` (at least partly).
 */
@Component
public class ExpressionProfileManager {

    @Autowired SystemObjectCache systemObjectCache;
    @Autowired ArchetypeManager archetypeManager;
    @Autowired SecurityEnforcer securityEnforcer;

    /**
     * Returns {@link ExpressionProfile} for given object, based on its archetype policy.
     * If no explicit profile can be found, the {@link ExpressionProfile#full()} is returned.
     * This is the legacy (pre-4.8) behavior.
     */
    public <O extends ObjectType> @NotNull ExpressionProfile determineExpressionProfile(
            @NotNull PrismObject<O> object, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        return Objects.requireNonNullElse(
                determineExpressionProfileOrNull(object, result),
                ExpressionProfile.full());
    }

    /**
     * Returns {@link ExpressionProfile} for given object, based on its archetype policy.
     * If no explicit profile is defined, `null` is returned, allowing to plug in a custom default.
     */
    private <O extends ObjectType> @Nullable ExpressionProfile determineExpressionProfileOrNull(
            @NotNull PrismObject<O> object, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        Preconditions.checkNotNull(object, "Object is null"); // explicitly checking to avoid false 'null' profiles
        var profileId = determineExpressionProfileId(object, result);
        if (profileId != null) {
            return systemObjectCache.getExpressionProfile(profileId, result);
        } else {
            return null;
        }
    }

    /**
     * Determines expression profile ID based on archetype policy for a given object.
     *
     * We intentionally do not use {@link ArchetypeManager#determineArchetypePolicy(ObjectType, OperationResult)} method here,
     * as it tries to merge the policy from all archetypes; and it's 1. slow, 2. unreliable, because of ignoring potential
     * conflicts. So, we do it in more explicit way.
     */
    private <O extends ObjectType> @Nullable String determineExpressionProfileId(
            @NotNull PrismObject<O> object, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {

        O objectable = object.asObjectable();

        // hopefully obtained from the cache
        var archetypes = archetypeManager.determineArchetypes(objectable, result);

        Set<String> idsFromArchetypes = new HashSet<>();
        for (ArchetypeType archetype : archetypes) {
            var policy = archetypeManager.getPolicyForArchetype(archetype, result);
            var profileId = policy != null ? policy.getExpressionProfile() : null;
            if (profileId != null) {
                idsFromArchetypes.add(profileId);
            }
        }

        if (idsFromArchetypes.size() > 1) {
            throw new ConfigurationException(
                    "Multiple expression profile IDs for %s: %s".formatted(
                            object, idsFromArchetypes));
        } else if (idsFromArchetypes.size() == 1) {
            return idsFromArchetypes.iterator().next();
        } else {
            var objectPolicy = archetypeManager.determineObjectPolicyConfiguration(objectable, result);
            return objectPolicy != null ? objectPolicy.getExpressionProfile() : null;
        }
    }

    /**
     * Determines {@link ExpressionProfile} for given configuration item origin.
     *
     * FIXME this is a transitory implementation: various unknown/external origins are treated as trustworthy.
     *   This should be changed before expression profiles are declared fully functional.
     */
    public @NotNull ExpressionProfile determineExpressionProfile(
            @NotNull ConfigurationItemOrigin origin, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        if (origin instanceof ConfigurationItemOrigin.InObject inObject) {
            return determineExpressionProfile(inObject.getOriginatingPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.InDelta inDelta) {
            return determineExpressionProfile(inDelta.getTargetPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.Generated) {
            return ExpressionProfile.full(); // Most probably OK
        } else if (origin instanceof ConfigurationItemOrigin.Undetermined undetermined) {
            stateCheck(!undetermined.isSafe(), "Safe undetermined origin cannot be used to derive expression profile");
            return ExpressionProfile.full(); // Later, we may throw an exception here
        } else if (origin instanceof ConfigurationItemOrigin.External) {
            return ExpressionProfile.full(); // FIXME we should perhaps return a restricted profile here (from the configuration?)
        } else {
            throw new AssertionError(origin);
        }
    }

    /**
     * Special version of {@link #determineExpressionProfile(ConfigurationItemOrigin, OperationResult)}
     * for scripting (bulk actions). It is not as permissive: some origins are banned, and the default for non-root users
     * is the restricted profile (unless `privileged` is set to true - in order to provide backwards compatibility with 4.7).
     */
    public @NotNull ExpressionProfile determineScriptingExpressionProfile(
            @NotNull ConfigurationItemOrigin origin, boolean privileged, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        @Nullable ExpressionProfile profile;
        if (origin instanceof ConfigurationItemOrigin.InObject inObject) {
            profile = determineExpressionProfileOrNull(inObject.getOriginatingPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.InDelta inDelta) {
            profile = determineExpressionProfileOrNull(inDelta.getTargetPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.Generated) {
            profile = ExpressionProfile.full(); // Most probably OK
        } else if (origin instanceof ConfigurationItemOrigin.Undetermined) {
            throw new UnsupportedOperationException("Undetermined origin for scripting expressions is not supported");
        } else if (origin instanceof ConfigurationItemOrigin.External) {
            profile = null;
        } else {
            throw new AssertionError(origin);
        }
        if (profile != null) {
            return profile;
        }
        if (privileged || securityEnforcer.isAuthorizedAll(task, result)) {
            return getPrivilegedScriptingProfile(result);
        } else {
            return getUnprivilegedScriptingProfile(result);
        }
    }

    private @NotNull ExpressionProfile getPrivilegedScriptingProfile(@NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        var defaults = getDefaults(result);
        var defaultScriptingProfileId = defaults != null ? defaults.getPrivilegedScripting() : null;
        if (defaultScriptingProfileId != null) {
            return systemObjectCache.getExpressionProfile(defaultScriptingProfileId, result);
        } else {
            return ExpressionProfile.full();
        }
    }

    private @NotNull ExpressionProfile getUnprivilegedScriptingProfile(@NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        var defaults = getDefaults(result);
        var defaultScriptingProfileId = defaults != null ? defaults.getScripting() : null;
        if (defaultScriptingProfileId != null) {
            return systemObjectCache.getExpressionProfile(defaultScriptingProfileId, result);
        } else {
            return ExpressionProfile.scriptingLegacyUnprivileged();
        }
    }

    private DefaultExpressionProfilesConfigurationType getDefaults(@NotNull OperationResult result) throws SchemaException {
        var config = systemObjectCache.getSystemConfigurationBean(result);
        var expressions = config != null ? config.getExpressions() : null;
        return expressions != null ? expressions.getDefaults() : null;
    }

    /**
     * Origin for custom workflow notifications is blurred, because they travel from policy rules to object triggers.
     * Hence, we should either disable them completely, or use a safe profile for them.
     */
    public @NotNull ExpressionProfile getProfileForCustomWorkflowNotifications(OperationResult result) {
        return ExpressionProfile.full(); // FIXME!!!
    }
}
