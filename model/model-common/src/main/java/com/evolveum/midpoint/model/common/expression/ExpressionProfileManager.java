/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.MidPointTrustDescriptor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.ExpressionProfileSupplier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DefaultExpressionProfilesConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Manages (cached) expression profiles.
 *
 * Because of implementation reasons, the profiles are cached within {@link SystemObjectCache}.
 * This probably should change in the future, along with moving this class to `repo-common` (at least partly).
 */
@NullMarked
@Component
public class ExpressionProfileManager {

    @Autowired SystemObjectCache systemObjectCache;
    @Autowired ArchetypeManager archetypeManager;
    @Autowired SecurityEnforcer securityEnforcer;

    /**
     * This is a default for expressions stored in repository objects because of compatibility reasons.
     * In some cases, setting more restrictive profile may cause system to break.
     */
    private static final ExpressionProfile DEFAULT_EXPRESSION_PROFILE_FOR_REPOSITORY_OBJECTS = ExpressionProfile.full();

    public ExpressionProfile determineExpressionProfile(
            MidPointTrustDescriptor trustDescriptor, Task task, OperationResult result)
            throws SecurityViolationException {
        return determineExpressionProfileInternal(
                trustDescriptor,
                result,
                this::getGeneralDefaultProfileId,
                DEFAULT_EXPRESSION_PROFILE_FOR_REPOSITORY_OBJECTS,
                ExpressionProfile.none());
    }

    /**
     * Determining expression profile for scripting (bulk actions). It is driven by different defaults than the general
     * expression profile.
     */
    public ExpressionProfile determineBulkActionsProfile(
            MidPointTrustDescriptor trustDescriptor, boolean privileged, Task task, OperationResult result)
            throws SecurityViolationException {

        return determineExpressionProfileInternal(
                trustDescriptor,
                result,
                (lResult) -> {
                    if (privileged || securityEnforcer.isAuthorizedAll(task, result)) {
                        return getPrivilegedBulkActionsProfileId(result);
                    } else {
                        return getUnprivilegedBulkActionsProfileId(result);
                    }
                },
                DEFAULT_EXPRESSION_PROFILE_FOR_REPOSITORY_OBJECTS,
                privileged ? ExpressionProfile.full() : ExpressionProfile.legacyUnprivilegedBulkActions());
    }

    private ExpressionProfile determineExpressionProfileInternal(
            MidPointTrustDescriptor trustDescriptor,
            OperationResult result,
            ExpressionProfileIdSupplier defaultProfileIdSupplier,
            ExpressionProfile defaultForRepositoryObjects,
            ExpressionProfile defaultForUntrusted)
            throws SecurityViolationException {

        if (trustDescriptor instanceof MidPointTrustDescriptor.Explicit explicit) {
            return explicit.expressionProfile();
        }

        try {
            String profileId;
            if (trustDescriptor instanceof MidPointTrustDescriptor.RepositoryObject repositoryObject) {
                profileId = determineExpressionProfileId(repositoryObject, result);
            } else if (trustDescriptor instanceof MidPointTrustDescriptor.Untrusted) {
                profileId = null;
            } else {
                throw new UnsupportedOperationException("Unsupported trust descriptor: " + trustDescriptor);
            }

            if (profileId == null) {
                profileId = defaultProfileIdSupplier.getExpressionProfileId(result);
            }
            if (profileId != null) {
                return systemObjectCache.getExpressionProfile(profileId, result);
            }
            if (trustDescriptor instanceof MidPointTrustDescriptor.RepositoryObject) {
                return defaultForRepositoryObjects;
            } else {
                return defaultForUntrusted;
            }
        } catch (CommonException e) {
            throw new SecurityViolationException(
                    "Couldn't determine expression profile for %s: %s".formatted(trustDescriptor, e.getMessage()),
                    e);
        }
    }

    /**
     * Determines expression profile ID based on archetype policy for a given object.
     *
     * We intentionally do not use {@link ArchetypeManager#determineArchetypePolicy(ObjectType, OperationResult)} method here,
     * as it tries to merge the policy from all archetypes; and it's 1. slow, 2. unreliable, because of ignoring potential
     * conflicts. So, we do it in more explicit way.
     */
    private @Nullable String determineExpressionProfileId(
            MidPointTrustDescriptor.RepositoryObject objectSpec, OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException {

        // hopefully obtained from the cache
        var archetypes = archetypeManager.resolveArchetypeOidsStrict(objectSpec.archetypeOids(), result);

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
                    "Multiple expression profile IDs for %s: %s".formatted(objectSpec, idsFromArchetypes));
        } else if (idsFromArchetypes.size() == 1) {
            return idsFromArchetypes.iterator().next();
        } else {
            var systemConfig = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfig != null) {
                var objectPolicy = ArchetypeManager.determineObjectPolicyConfiguration(
                        objectSpec.type(), objectSpec.subtypes(), systemConfig);
                if (objectPolicy != null) {
                    return objectPolicy.getExpressionProfile();
                }
            }
            return null;
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
    private ExpressionProfile determineExpressionProfileForChannel(String channelUri, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
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

    private @Nullable String getPrivilegedBulkActionsProfileId(OperationResult result)
            throws SchemaException {
        var defaults = getDefaults(result);
        if (defaults == null) {
            return null;
        }
        var privileged = defaults.getPrivilegedBulkActions();
        if (privileged != null) {
            return privileged;
        }
        var otherBulk = defaults.getBulkActions();
        if (otherBulk != null) {
            return otherBulk;
        }
        return defaults.getGeneral();
    }

    private @Nullable String getUnprivilegedBulkActionsProfileId(OperationResult result)
            throws SchemaException {
        var defaults = getDefaults(result);
        if (defaults == null) {
            return null;
        }
        var bulkActions = defaults.getBulkActions();
        if (bulkActions != null) {
            return bulkActions;
        }
        return defaults.getGeneral();
    }

    private @Nullable String getGeneralDefaultProfileId(OperationResult result)
            throws SchemaException {
        var defaults = getDefaults(result);
        if (defaults == null) {
            return null;
        }
        return defaults.getGeneral();
    }

    private @Nullable DefaultExpressionProfilesConfigurationType getDefaults(OperationResult result) throws SchemaException {
        var config = systemObjectCache.getSystemConfigurationBean(result);
        var expressions = config != null ? config.getExpressions() : null;
        return expressions != null ? expressions.getDefaults() : null;
    }

    @SuppressWarnings("unused") // used by Spring
    public ExpressionProfileSupplier getDefaultExpressionProfileSupplier() {
        return this::determineExpressionProfile;
    }

    public interface ExpressionProfileIdSupplier extends Serializable {

        @Nullable String getExpressionProfileId(OperationResult result)
                throws CommonException;
    }
}
