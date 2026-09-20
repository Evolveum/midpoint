/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.axiom.concepts.CheckedSupplier;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
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
     * Determines expression profile for {@link ExpressionType} values and (in future) for scripts and maybe other objects.
     *
     * NOT to be used for bulk actions.
     *
     * @see #determineBulkActionsProfile(MidPointTrustDescriptor, Task, OperationResult)
     */
    private ExpressionProfile determineExpressionProfile(
            MidPointTrustDescriptor trustDescriptor, Task task, OperationResult result)
            throws SecurityViolationException {

        if (trustDescriptor instanceof MidPointTrustDescriptor.CurrentPrincipal) {
            // This is a special case: we don't want to evaluate any expressions (ExpressionType) from the outside,
            // so we just return the "none" profile.
            return ExpressionProfile.none();
        }

        return determineExpressionProfileInternal(
                trustDescriptor,
                result,
                this::getAuthorizedObjectsDefaultProfileId,
                () -> trustDescriptor instanceof MidPointTrustDescriptor.AuthorizedObject ?
                        ExpressionProfile.legacyDefaultForAuthorizedObjects() :
                        ExpressionProfile.none()); // We don't want to evaluate any expressions from the outside
    }

    /**
     * Determines expression profile for midPoint (bulk) actions.
     *
     * For legacy reasons, it is driven by different defaults than expression profile determination for expressions.
     *
     * @see #determineExpressionProfile(MidPointTrustDescriptor, Task, OperationResult)
     */
    public ExpressionProfile determineBulkActionsProfile(
            MidPointTrustDescriptor trustDescriptor, Task task, OperationResult result)
            throws SecurityViolationException {

        // This is a supplier that delays (potentially costly) authorization check until really needed and then caches its result
        // if it's needed in this method twice.
        CheckedSupplier<Boolean, SecurityViolationException> cachingIsAuthorizedSupplier =
                new CheckedSupplier<>() {
                    @Nullable private Boolean cachedValue;
                    @Override
                    public Boolean get() throws SecurityViolationException {
                        try {
                            if (cachedValue == null) {
                                cachedValue = securityEnforcer.isAuthorizedAll(task, result);
                            }
                            return cachedValue;
                        } catch (CommonException e) {
                            throw new SecurityViolationException(e);
                        }
                    }
                };

        ExpressionProfileIdSupplier defaultProfileIdSupplier = (isAuthorizedObjectTrust, lResult) -> {
            // This is used if the profile cannot be determined from containing [authorized] object type/archetype/subtype.
            if (cachingIsAuthorizedSupplier.get()) {
                return getPrivilegedBulkActionsDefaultProfileId(isAuthorizedObjectTrust, lResult);
            } else {
                return getUnprivilegedBulkActionsDefaultProfileId(isAuthorizedObjectTrust, lResult);
            }
        };

        CheckedSupplier<ExpressionProfile, SecurityViolationException> defaultProfileSupplier = () -> {
            // This is used if nothing is configured.
            if (cachingIsAuthorizedSupplier.get()) {
                return ExpressionProfile.legacyDefaultForPrivilegedBulkActions();
            } else {
                return ExpressionProfile.legacyDefaultForUnprivilegedBulkActions();
            }
        };

        return determineExpressionProfileInternal(
                trustDescriptor, result, defaultProfileIdSupplier, defaultProfileSupplier);
    }

    /** Common logic for determination of expression profile from trust descriptor. See the code inside. */
    private ExpressionProfile determineExpressionProfileInternal(
            MidPointTrustDescriptor trustDescriptor,
            OperationResult result,
            ExpressionProfileIdSupplier defaultProfileIdSupplier,
            CheckedSupplier<ExpressionProfile, SecurityViolationException> defaultProfileSupplier)
            throws SecurityViolationException {

        Preconditions.checkArgument(
                trustDescriptor instanceof MidPointTrustDescriptor.Explicit
                || trustDescriptor instanceof MidPointTrustDescriptor.AuthorizedObject
                || trustDescriptor instanceof MidPointTrustDescriptor.CurrentPrincipal,
                "Unsupported trust descriptor: %s", trustDescriptor);

        if (trustDescriptor instanceof MidPointTrustDescriptor.Explicit explicit) {
            return explicit.expressionProfile();
        }

        try {
            String profileId = null;

            boolean isAuthorizedObjectTrust = trustDescriptor instanceof MidPointTrustDescriptor.AuthorizedObject;

            if (trustDescriptor instanceof MidPointTrustDescriptor.AuthorizedObject authorizedObject) {
                // This is the best case: we know that the object access was already authorized,
                // so we can have a look at object type [and archetype/subtype] and determine the profile from that.
                profileId = determineExpressionProfileIdForAuthorizedObject(authorizedObject, result);
            }

            if (profileId == null) {
                // If that's not the case, we have to look at defaults provided within the system configuration.
                // These are different for bulk actions and for ExpressionType values, so we have to use the supplier
                // that is passed in.
                profileId = defaultProfileIdSupplier.getExpressionProfileId(isAuthorizedObjectTrust, result);
            }

            if (profileId == null) {
                // Out of luck. We have to rely on the default profile supplier.
                return defaultProfileSupplier.get();
            }

            return systemObjectCache.getExpressionProfile(profileId, result);

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
    private @Nullable String determineExpressionProfileIdForAuthorizedObject(
            MidPointTrustDescriptor.AuthorizedObject objectSpec, OperationResult result)
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

    private @Nullable String getPrivilegedBulkActionsDefaultProfileId(boolean isAuthorizedObjectTrust, OperationResult result)
            throws SchemaException {
        var defaults = getDefaults(result);
        if (defaults == null) {
            return null;
        }
        var modernDefault = isAuthorizedObjectTrust ?
                defaults.getBulkActionsInAuthorizedObjects() : defaults.getPrivilegedPrincipal();
        if (modernDefault != null) {
            return modernDefault;
        }
        var legacyDefault1 = defaults.getPrivilegedBulkActions();
        if (legacyDefault1 != null) {
            return legacyDefault1;
        }
        return defaults.getBulkActions(); // legacy default 2
    }

    private @Nullable String getUnprivilegedBulkActionsDefaultProfileId(boolean isAuthorizedObjectTrust, OperationResult result)
            throws SchemaException {
        var defaults = getDefaults(result);
        if (defaults == null) {
            return null;
        }
        var modernDefault = isAuthorizedObjectTrust ?
                defaults.getBulkActionsInAuthorizedObjects() : defaults.getUnprivilegedPrincipal();
        if (modernDefault != null) {
            return modernDefault;
        }
        return defaults.getBulkActions(); // legacy default
    }

    // NOT to be called for bulk actions, because they have different defaults
    private @Nullable String getAuthorizedObjectsDefaultProfileId(boolean isAuthorizedObject, OperationResult result)
            throws SchemaException {
        Preconditions.checkArgument(isAuthorizedObject);
        var defaults = getDefaults(result);
        if (defaults == null) {
            return null;
        }
        return defaults.getAuthorizedObjects();
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

        @Nullable String getExpressionProfileId(boolean isAuthorizedObjectTrust, OperationResult result)
                throws CommonException;
    }
}
