/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression;

import java.util.HashSet;
import java.util.Set;

import com.evolveum.axiom.concepts.CheckedSupplier;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
    public ExpressionProfile determineExpressionProfile(
            MidPointTrustDescriptor trustDescriptor, Task task, OperationResult result)
            throws SecurityViolationException {

        return determineExpressionProfileInternal(trustDescriptor, false, task, result);
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

        return determineExpressionProfileInternal(trustDescriptor, true, task, result);
    }

    /**
     * Common logic for determination of expression profile from trust descriptor. See the code inside.
     *
     * @param trustDescriptor The trust descriptor to determine the expression profile for.
     * @param bulkAction Whether this is for a bulk action (true) or for an expression (false).
     */
    private ExpressionProfile determineExpressionProfileInternal(
            MidPointTrustDescriptor trustDescriptor, boolean bulkAction, Task task, OperationResult result)
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

            // Let's try the optimal case first: if we know that the object access was already authorized,
            // we can have a look at object type [and archetype/subtype] and determine the profile from that.
            //
            // This part is the same for regular expressions and for bulk actions.

            if (trustDescriptor instanceof MidPointTrustDescriptor.AuthorizedObject authorizedObject) {

                var profileIdInfo = determineExpressionProfileIdForAuthorizedObject(authorizedObject, result);
                if (profileIdInfo.id != null) {
                    return systemObjectCache.getExpressionProfile(profileIdInfo.id, result);
                } else if (profileIdInfo.isForCurrentPrincipal) {
                    // A special case where the profile points us at the current principal.
                    // Currently, this can occur for bulk actions. (See "User submitted task" archetype.)
                    trustDescriptor = MidPointTrustDescriptor.forCurrentPrincipal();
                }

            }

            var defaults = getDefaults(result);
            var isPrivilegedSupplier = createCachingIsPrivilegedSupplier(task, result);

            // If the expression profile cannot be determined from the object type/archetype/subtype,
            // we have to look at the defaults. This is different for regular expressions and for bulk actions,
            // and for "current principal" vs "authorized object" case.

            if (trustDescriptor instanceof MidPointTrustDescriptor.CurrentPrincipal) {

                if (defaults != null) {

                    // for both bulk actions and expressions
                    if (isPrivilegedSupplier.get()) {
                        String modernDefault1 = defaults.getPrivilegedPrincipal();
                        if (modernDefault1 != null) {
                            return systemObjectCache.getExpressionProfile(modernDefault1, result);
                        }
                    }
                    String modernDefault2 = defaults.getPrincipal();
                    if (modernDefault2 != null) {
                        return systemObjectCache.getExpressionProfile(modernDefault2, result);
                    }

                    if (bulkAction) {
                        // for bulk actions only (legacy)
                        if (isPrivilegedSupplier.get()) {
                            String legacyBulkActionDefault1 = defaults.getPrivilegedBulkActions();
                            if (legacyBulkActionDefault1 != null) {
                                return systemObjectCache.getExpressionProfile(legacyBulkActionDefault1, result);
                            }
                        }
                        String legacyBulkActionDefault2 = defaults.getBulkActions();
                        if (legacyBulkActionDefault2 != null) {
                            return systemObjectCache.getExpressionProfile(legacyBulkActionDefault2, result);
                        }
                    }
                }

                if (!bulkAction) {
                    // By default, we don't allow any expressions to be evaluated in "current principal" mode
                    return ExpressionProfile.none();
                } else {
                    return isPrivilegedSupplier.get() ?
                            ExpressionProfile.legacyDefaultForPrivilegedBulkActions() :
                            ExpressionProfile.legacyDefaultForUnprivilegedBulkActions();
                }

            } else if (trustDescriptor instanceof MidPointTrustDescriptor.AuthorizedObject) {

                if (defaults != null) {

                    if (bulkAction) {
                        String modernDefaultForBulkActions = defaults.getBulkActionsInAuthorizedObjects();
                        if (modernDefaultForBulkActions != null) {
                            return systemObjectCache.getExpressionProfile(modernDefaultForBulkActions, result);
                        }
                    }

                    // for both bulk actions and expressions
                    String modernDefault = defaults.getAuthorizedObjects();
                    if (modernDefault != null) {
                        return systemObjectCache.getExpressionProfile(modernDefault, result);
                    }

                    if (bulkAction) {
                        // for bulk actions only (legacy)
                        boolean isPrivileged = isPrivilegedSupplier.get();
                        if (isPrivileged) {
                            String legacyDefault1 = defaults.getPrivilegedBulkActions();
                            if (legacyDefault1 != null) {
                                return systemObjectCache.getExpressionProfile(legacyDefault1, result);
                            }
                        }
                        String legacyDefault2 = defaults.getBulkActions();
                        if (legacyDefault2 != null) {
                            return systemObjectCache.getExpressionProfile(legacyDefault2, result);
                        }
                    }
                }

                if (!bulkAction) {
                    // This is the default for expressions stored in authorized objects (dangerous!)
                    return ExpressionProfile.legacyDefaultForAuthorizedObjects();
                } else {
                    // This is not a good default, but we have to return something.
                    // The legacy default is to allow everything for privileged users and very little for unprivileged users.
                    // This is the same behavior as it was before 4.11.
                    return isPrivilegedSupplier.get() ?
                            ExpressionProfile.legacyDefaultForPrivilegedBulkActions() :
                            ExpressionProfile.legacyDefaultForUnprivilegedBulkActions();
                }
            } else {
                throw new IllegalStateException("Unexpected trust descriptor: " + trustDescriptor);
            }
        } catch (CommonException e) {
            throw new SecurityViolationException(
                    "Couldn't determine expression profile for %s: %s".formatted(trustDescriptor, e.getMessage()),
                    e);
        }
    }

    /**
     * This is a supplier that delays (potentially costly) authorization check until really needed and
     * then caches its result if it's needed in this method twice.
     */
    private CheckedSupplier<Boolean, SecurityViolationException> createCachingIsPrivilegedSupplier(Task task, OperationResult result) {
        return new CheckedSupplier<>() {
            @Nullable private Boolean cachedValue;
            @Override
            public Boolean get() throws SecurityViolationException {
                try {
                    if (cachedValue == null) {
                        cachedValue = isAuthorizedAll(task, result);
                    }
                    return cachedValue;
                } catch (CommonException e) {
                    throw new SecurityViolationException(e);
                }
            }
        };
    }

    private boolean isAuthorizedAll(Task task, OperationResult result) throws CommonException {
        return securityEnforcer.isAuthorizedAll(task, result);
    }

    /**
     * Determines expression profile ID based on archetype policy for a given object.
     *
     * We intentionally do not use {@link ArchetypeManager#determineArchetypePolicy(ObjectType, OperationResult)} method here,
     * as it tries to merge the policy from all archetypes; and it's 1. slow, 2. unreliable, because of ignoring potential
     * conflicts. So, we do it in more explicit way.
     */
    private ProfileIdInfo determineExpressionProfileIdForAuthorizedObject(
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

        // "For current principal" this takes precendence over any other profile ID, because it is a special case:
        // it means that the expression should be evaluated in the context of the current principal, not in the context
        // of the authorized object.
        if (idsFromArchetypes.contains(SchemaConstants.CURRENT_PRINCIPAL_PROFILE_ID)) {
            return ProfileIdInfo.forCurrentPrincipal();
        }

        if (idsFromArchetypes.size() > 1) {
            throw new ConfigurationException(
                    "Multiple expression profile IDs for %s: %s".formatted(objectSpec, idsFromArchetypes));
        } else if (idsFromArchetypes.size() == 1) {
            return ProfileIdInfo.forId(idsFromArchetypes.iterator().next());
        } else {
            var systemConfig = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfig != null) {
                var objectPolicy = ArchetypeManager.determineObjectPolicyConfiguration(
                        objectSpec.type(), objectSpec.subtypes(), systemConfig);
                if (objectPolicy != null) {
                    return ProfileIdInfo.forId(objectPolicy.getExpressionProfile());
                }
            }
            return ProfileIdInfo.unknown();
        }
    }

    private record ProfileIdInfo(@Nullable String id, boolean isForCurrentPrincipal) {
        private static ProfileIdInfo unknown() {
            return new ProfileIdInfo(null, false);
        }
        private static ProfileIdInfo forCurrentPrincipal() {
            return new ProfileIdInfo(null, true);
        }
        private static ProfileIdInfo forId(String id) {
            return new ProfileIdInfo(id, false);
        }
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
}
