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
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    /**
     * Returns {@link ExpressionProfile} for given object, based on its archetype policy.
     */
    public <O extends ObjectType> @NotNull ExpressionProfile determineExpressionProfile(
            @NotNull PrismObject<O> object, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        Preconditions.checkNotNull(object, "Object is null"); // explicitly checking to avoid false 'null' profiles
        var profileId = determineExpressionProfileId(object, result);
        if (profileId != null) {
            return systemObjectCache.getExpressionProfile(profileId, result);
        } else {
            return ExpressionProfile.full();
        }
    }

    /**
     * We intentionally do not use {@link ArchetypeManager#determineArchetypePolicy(ObjectType, OperationResult)} method,
     * as it tries to merge the policy from all archetypes; and it's 1. slow, 2. unreliable, because of ignoring potential
     * conflicts. Let's do it in more explicit way.
     */
    private <O extends ObjectType> @Nullable String determineExpressionProfileId(
            @NotNull PrismObject<O> object, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {

        O objectable = object.asObjectable();

        var structuralArchetype = // hopefully obtained from the cache
                objectable instanceof AssignmentHolderType assignmentHolder ?
                        archetypeManager.determineStructuralArchetype(assignmentHolder, result) : null;

        // The policy is (generally) cached, so this should be fast
        var structuralArchetypePolicy = archetypeManager.getPolicyForArchetype(structuralArchetype, result);
        if (structuralArchetypePolicy != null) {
            var profileId = structuralArchetypePolicy.getExpressionProfile();
            if (profileId != null) {
                return profileId;
            }
        }

        var objectPolicy = archetypeManager.determineObjectPolicyConfiguration(objectable, result);
        return objectPolicy != null ? objectPolicy.getExpressionProfile() : null;
    }

    public @NotNull ExpressionProfile determineExpressionProfile(
            @NotNull ConfigurationItemOrigin origin, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        if (origin instanceof ConfigurationItemOrigin.InObject inObject) {
            return determineExpressionProfile(inObject.getOriginatingPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.InDelta inDelta) {
            return determineExpressionProfile(inDelta.getTargetPrismObject(), result);
        } else if (origin instanceof ConfigurationItemOrigin.Generated) {
            return ExpressionProfile.full(); // Most probably OK
        } else if (origin instanceof ConfigurationItemOrigin.Undetermined) {
            return ExpressionProfile.full(); // Later, we may throw an exception here
        } else if (origin instanceof ConfigurationItemOrigin.External) {
            return ExpressionProfile.full(); // FIXME we should perhaps return a restricted profile here (from the configuration?)
        } else {
            throw new AssertionError(origin);
        }
    }
}
