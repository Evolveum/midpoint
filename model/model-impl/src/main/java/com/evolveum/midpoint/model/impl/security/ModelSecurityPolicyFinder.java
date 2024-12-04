/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.security.SecurityPolicyFinder;
import com.evolveum.midpoint.schema.merger.securitypolicy.SecurityPolicyCustomMerger;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 * Looks up security policies.
 *
 * As {@link SecurityPolicyFinder} but residing in `model-impl` because of the dependency on {@link ArchetypeManager}.
 * (Actually, we should consider moving it to `repo-common`, to have everything related to security policy resolution
 * in one place.)
 */
@Component
public class ModelSecurityPolicyFinder {

    private static final Trace LOGGER = TraceManager.getTrace(ModelSecurityPolicyFinder.class);

    @Autowired private SecurityPolicyFinder securityPolicyFinder;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private ModelObjectResolver objectResolver;

    /**
     * Returns security policy applicable for the specified focus (if any).
     * It looks for organization, archetype and global policies and takes into account deprecated properties
     * and password policy references.
     *
     * The resulting security policy has all the (non-deprecated) properties set. If there is also referenced value policy,
     * it is will be stored as "object" in the value policy reference inside the returned security policy.
     */
    public <F extends FocusType> SecurityPolicyType locateSecurityPolicyForFocus(
            @Nullable PrismObject<F> focus,
            @Nullable PrismObject<SystemConfigurationType> systemConfiguration,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {

        var globalSecurityPolicy = securityPolicyFinder.locateGlobalSecurityPolicy(systemConfiguration, true, result);
        if (focus != null) {
            var focusSecurityPolicy = resolveSecurityPolicyForFocus(focus, globalSecurityPolicy, task, result);
            if (focusSecurityPolicy != null) {
                traceSecurityPolicy(focusSecurityPolicy, focus);
                return focusSecurityPolicy;
            }
        }
        traceSecurityPolicy(globalSecurityPolicy, focus);
        return globalSecurityPolicy;
    }

    private <F extends FocusType> SecurityPolicyType resolveSecurityPolicyForFocus(
            PrismObject<F> focus, SecurityPolicyType globalSecurityPolicy, Task task, OperationResult result)
            throws SchemaException {
        var securityPolicyFromArchetypes = locateFocusSecurityPolicyFromArchetypes(focus, task, result);
        var securityPolicyFromOrgs = locateFocusSecurityPolicyFromOrgs(focus, task, result);
        return SecurityPolicyCustomMerger.mergeSecurityPolicies(
                securityPolicyFromArchetypes, securityPolicyFromOrgs, globalSecurityPolicy);
    }

    /**
     * Returns security policy applicable for the specified archetype (if any), taking into account global security policy.
     *
     * @see #locateSecurityPolicyForFocus(PrismObject, PrismObject, Task, OperationResult)
     */
    public SecurityPolicyType locateSecurityPolicyForArchetype(
            @Nullable String archetypeOid,
            @Nullable PrismObject<SystemConfigurationType> systemConfiguration,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {

        var globalSecurityPolicy = securityPolicyFinder.locateGlobalSecurityPolicy(systemConfiguration, true, result);
        if (archetypeOid != null) {
            var archetypeSecurityPolicy = resolveSecurityPolicyForArchetype(archetypeOid, globalSecurityPolicy, task, result);
            if (archetypeSecurityPolicy != null) {
                traceSecurityPolicy(archetypeSecurityPolicy, null);
                return archetypeSecurityPolicy;
            }
        }
        traceSecurityPolicy(globalSecurityPolicy, null);
        return globalSecurityPolicy;
    }

    /**
     * Returns security policy referenced from the archetype and merged with the global security policy referenced from the
     * system configuration.
     */
    private SecurityPolicyType resolveSecurityPolicyForArchetype(
            String archetypeOid, SecurityPolicyType globalSecurityPolicy, Task task, OperationResult result)
            throws SchemaException {
        try {
            var archetype = archetypeManager.getArchetype(archetypeOid, result);
            var shortDescription = "load security policy from archetype";
            var archetypeSecurityPolicy = loadArchetypeSecurityPolicy(archetype, shortDescription, task, result);

            if (archetypeSecurityPolicy == null) {
                return null;
            }

            return SecurityPolicyCustomMerger.mergeSecurityPolicies(archetypeSecurityPolicy, globalSecurityPolicy);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Cannot load archetype object, ", e);
        }
        return null;
    }

    private <F extends FocusType> SecurityPolicyType locateFocusSecurityPolicyFromOrgs(
            PrismObject<F> focus, Task task, OperationResult result) throws SchemaException {
        PrismObject<SecurityPolicyType> orgSecurityPolicyObject = objectResolver.searchOrgTreeWidthFirstReference(
                focus,
                org -> {
                    // For create-on-demand feature used in "safe" preview mode (the default), the org returned here may be null.
                    // (It is unlike real simulations that go a step further, and provide in-memory object here.)
                    // If the org is null, we have nothing to do; we simply return null policy reference.
                    return org != null ? org.asObjectable().getSecurityPolicyRef() : null;
                },
                "security policy",
                task,
                result);
        var orgSecurityPolicy = asObjectable(orgSecurityPolicyObject);
        LOGGER.trace("Found organization security policy: {}", orgSecurityPolicy);
        securityPolicyFinder.resolveValuePolicyRefs(orgSecurityPolicy, result);
        return orgSecurityPolicy;
    }

    private <F extends FocusType> SecurityPolicyType locateFocusSecurityPolicyFromArchetypes(
            PrismObject<F> focus, Task task, OperationResult result) throws SchemaException {
        var archetypeSecurityPolicy = searchSecurityPolicyFromArchetype(focus, "security policy", task, result);
        LOGGER.trace("Found archetype security policy: {}", archetypeSecurityPolicy);
        securityPolicyFinder.resolveValuePolicyRefs(archetypeSecurityPolicy, result);
        return archetypeSecurityPolicy;
    }

    @SuppressWarnings("SameParameterValue")
    private <O extends ObjectType> SecurityPolicyType searchSecurityPolicyFromArchetype(
            PrismObject<O> object, String shortDesc, Task task, OperationResult result) throws SchemaException {
        if (object == null) {
            LOGGER.trace("No object provided. Cannot find security policy specific for an object.");
            return null;
        }
        ArchetypeType structuralArchetype = archetypeManager.determineStructuralArchetype(object.asObjectable(), result);
        if (structuralArchetype == null) {
            return null;
        }
        return loadArchetypeSecurityPolicy(structuralArchetype, shortDesc, task, result);
    }

    private SecurityPolicyType loadArchetypeSecurityPolicy(
            ArchetypeType archetype, String shortDesc, Task task, OperationResult result) {
        ObjectReferenceType securityPolicyRef = archetype.getSecurityPolicyRef();
        if (securityPolicyRef == null) {
            return null;
        }
        PrismObject<SecurityPolicyType> securityPolicy;
        try {
            securityPolicy = objectResolver.resolve(securityPolicyRef.asReferenceValue(), shortDesc, task, result);
        } catch (ObjectNotFoundException ex) {
            LOGGER.warn("Cannot find security policy referenced in archetype {}, oid {}", archetype.getName(),
                    archetype.getOid());
            return null;
        }
        return mergeSecurityPolicyWithSuperArchetype(archetype, securityPolicy.asObjectable(), task, result);
    }

    private SecurityPolicyType mergeSecurityPolicyWithSuperArchetype(ArchetypeType archetype, SecurityPolicyType securityPolicy,
            Task task, OperationResult result) {
        ArchetypeType superArchetype;
        try {
            superArchetype = archetype.getSuperArchetypeRef() != null ?
                    objectResolver.resolve(archetype.getSuperArchetypeRef(), ArchetypeType.class, null, "resolving super archetype ref", task, result)
                    : null;
        } catch (Exception ex) {
            LOGGER.warn("Cannot resolve super archetype reference for archetype {}, oid {}", archetype.getName(), archetype.getOid());
            return securityPolicy;
        }
        if (superArchetype == null) {
            return securityPolicy;
        }
        SecurityPolicyType superArchetypeSecurityPolicy;
        try {
            superArchetypeSecurityPolicy = superArchetype.getSecurityPolicyRef() != null ?
                    objectResolver.resolve(superArchetype.getSecurityPolicyRef(), SecurityPolicyType.class, null, "resolving security policy ref", task, result)
                    : null;
        } catch (Exception ex) {
            LOGGER.warn("Cannot resolve security policy reference for archetype {}, oid {}", superArchetype.getName(), superArchetype.getOid());
            return securityPolicy;
        }
        if (superArchetypeSecurityPolicy == null) {
            return securityPolicy;
        }
        SecurityPolicyType mergedSecurityPolicy = SecurityPolicyCustomMerger.mergeSecurityPolicies(securityPolicy, superArchetypeSecurityPolicy);
        return mergeSecurityPolicyWithSuperArchetype(superArchetype, mergedSecurityPolicy, task, result);
    }

    private void traceSecurityPolicy(SecurityPolicyType policy, PrismObject<?> object) {
        if (object != null) {
            if (policy == null) {
                LOGGER.trace("Located security policy for {}: null", object);
            } else {
                LOGGER.trace("Located security policy for {}:\n{}", object, policy.asPrismObject().debugDumpLazily(1));
            }
        } else {
            if (policy == null) {
                LOGGER.trace("Located global security policy: null");
            } else {
                LOGGER.trace("Located global security policy :\n{}", policy.asPrismObject().debugDumpLazily(1));
            }
        }
    }
}
