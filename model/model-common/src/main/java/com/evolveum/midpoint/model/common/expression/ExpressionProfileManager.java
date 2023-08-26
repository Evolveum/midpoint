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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
     * Does not allow undetermined profiles, and treats external profiles with care.
     */
    public @NotNull ExpressionProfile determineExpressionProfileStrict(
            @NotNull ConfigurationItemOrigin origin, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        if (origin instanceof ConfigurationItemOrigin.External external) {
            return determineExpressionProfileForChannel(external.getChannelUri(), task, result);
        } else {
            return determineExpressionProfileCommon(origin, result);
        }
    }

    /**
     * Determines {@link ExpressionProfile} for given configuration item origin.
     *
     * FIXME this is a transitory, unsafe implementation: various unknown/external origins are treated as trustworthy.
     *   This should be changed before expression profiles are declared fully functional.
     */
    public @NotNull ExpressionProfile determineExpressionProfileUnsafe(
            @NotNull ConfigurationItemOrigin origin, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        if (origin instanceof ConfigurationItemOrigin.Undetermined undetermined) {
            stateCheck(!undetermined.isSafe(), "Safe undetermined origin cannot be used to derive expression profile");
            return ExpressionProfile.full(); // Later, we may throw an exception here
        } else if (origin instanceof ConfigurationItemOrigin.External) {
            return ExpressionProfile.full(); // FIXME we should perhaps return a restricted profile here (from the configuration?)
        } else {
            return determineExpressionProfileCommon(origin, result);
        }
    }

    /**
     * A strict version of {@link #determineExpressionProfileUnsafe(ConfigurationItemOrigin, OperationResult)} that does not
     * allow undetermined nor external profiles.
     */
    private @NotNull ExpressionProfile determineExpressionProfileCommon(
            @NotNull ConfigurationItemOrigin origin, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        if (origin instanceof ConfigurationItemOrigin.InObject inObject) {
            return determineExpressionProfile(inObject.getOriginatingPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.InDelta inDelta) {
            return determineExpressionProfile(inDelta.getTargetPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.Generated) {
            return ExpressionProfile.full(); // Most probably OK
        } else if (origin instanceof ConfigurationItemOrigin.Undetermined) {
            throw new IllegalStateException("Undetermined origin for expression profile is not supported");
        } else if (origin instanceof ConfigurationItemOrigin.External) {
            throw new IllegalStateException("'External' origin for expression profile is not supported");
        } else {
            throw new AssertionError(origin);
        }
    }

    /**
     * Determines expression profile for {@link ConfigurationItemOrigin.External} origin.
     *
     * We assume that the configuration item was entered via GUI or provided via REST right by the logged-in principal.
     * Hence, we allow everything only if the principal is a root. Otherwise, we allow nothing.
     *
     * For the "init" channel, we could allow everything, but its safer to assume that the initialization is always
     * carried out with full privileges, and that it's safe to check the authorization there as well.
     *
     * This could be made configurable in the future.
     */
    private @NotNull ExpressionProfile determineExpressionProfileForChannel(
            @NotNull String channelUri, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        if (SchemaConstants.CHANNEL_INIT_URI.equals(channelUri)
                || SchemaConstants.CHANNEL_REST_URI.equals(channelUri)
                || SchemaConstants.CHANNEL_USER_URI.equals(channelUri)) {
            if (securityEnforcer.isAuthorizedAll(task, result)) {
                return ExpressionProfile.full();
            } else {
                return ExpressionProfile.none();
            }
        } else {
            throw new UnsupportedOperationException("The expression profile cannot be determined for channel: " + channelUri);
        }
    }

    /**
     * Special version of {@link #determineExpressionProfileUnsafe(ConfigurationItemOrigin, OperationResult)}
     * for scripting (bulk actions). It is not as permissive: some origins are banned, and the default for non-root users
     * is the restricted profile (unless `privileged` is set to true - in order to provide backwards compatibility with 4.7).
     */
    public @NotNull ExpressionProfile determineBulkActionsProfile(
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
            throw new UnsupportedOperationException("Undetermined origin for bulk actions is not supported");
        } else if (origin instanceof ConfigurationItemOrigin.External) {
            profile = null;
        } else {
            throw new AssertionError(origin);
        }
        if (profile != null) {
            return profile;
        }
        if (privileged || securityEnforcer.isAuthorizedAll(task, result)) {
            return getPrivilegedBulkActionsProfile(result);
        } else {
            return getUnprivilegedBulkActionsProfile(result);
        }
    }

    private @NotNull ExpressionProfile getPrivilegedBulkActionsProfile(@NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        var defaults = getDefaults(result);
        var defaultProfileId = defaults != null ? defaults.getPrivilegedBulkActions() : null;
        if (defaultProfileId != null) {
            return systemObjectCache.getExpressionProfile(defaultProfileId, result);
        } else {
            return ExpressionProfile.full();
        }
    }

    private @NotNull ExpressionProfile getUnprivilegedBulkActionsProfile(@NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        var defaults = getDefaults(result);
        var defaultProfileId = defaults != null ? defaults.getBulkActions() : null;
        if (defaultProfileId != null) {
            return systemObjectCache.getExpressionProfile(defaultProfileId, result);
        } else {
            return ExpressionProfile.legacyUnprivilegedBulkActions();
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
    public @NotNull ExpressionProfile getProfileForCustomWorkflowNotifications(OperationResult result)
            throws SchemaException, ConfigurationException {
        var defaults = getDefaults(result);
        var notifications = defaults != null ? defaults.getCustomWorkflowNotifications() : null;
        if (notifications != null) {
            return systemObjectCache.getExpressionProfile(notifications, result);
        } else {
            return ExpressionProfile.legacyUnprivilegedBulkActions();
        }
    }
}
