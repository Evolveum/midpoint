/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.security;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

import com.evolveum.midpoint.schema.merger.securitypolicy.SecurityPolicyCustomMerger;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;

/**
 * Looks up security policies.
 *
 * == Exception reporting
 *
 * If the security policy cannot be obtained, methods in this class throw a {@link SystemException} instead of specific
 * exceptions like {@link ObjectNotFoundException} etc. The reason is that from the point of view of a regular caller,
 * the inability to obtain the security policy is a generic system-level issue.
 */
@Component
public class SecurityPolicyFinder {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityPolicyFinder.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private SystemObjectCache systemObjectCache;

    /** Returns global security policy; optionally resolves value policy references. */
    public @Nullable SecurityPolicyType locateGlobalSecurityPolicy(
            @Nullable PrismObject<SystemConfigurationType> systemConfiguration,
            boolean resolveValueRefs,
            OperationResult result) {
        var policy = locateGlobalSecurityPolicy(asObjectable(systemConfiguration), result);
        if (resolveValueRefs) {
            resolveValuePolicyRefs(policy, result);
        }
        return policy;
    }

    /** Returns global security policy. Does not resolve value policy references. */
    private SecurityPolicyType locateGlobalSecurityPolicy(SystemConfigurationType systemConfiguration, OperationResult result) {
        var globalSecurityPolicyRef = systemConfiguration != null ? systemConfiguration.getGlobalSecurityPolicyRef() : null;
        if (globalSecurityPolicyRef == null) {
            return null;
        }
        try {
            var globalSecurityPolicyType = resolve(globalSecurityPolicyRef, SecurityPolicyType.class, null, result);
            LOGGER.trace("Using global security policy: {}", globalSecurityPolicyType);
            return globalSecurityPolicyType;
        } catch (SystemException e) {
            // TODO Shouldn't we pass the SystemException instead to make administrator well aware of the broken configuration?
            LoggingUtils.logException(LOGGER, "Couldn't resolve global security policy", e);
            return null;
        }
    }

    /**
     * Returns security policy related to the projection, resolved in the *modern* way.
     *
     * - The global security policy is taken into account (merged with the specific one, if present).
     * - Value references are not resolved, as the client may or may not need them.
     */
    private @Nullable SecurityPolicyType locateResourceObjectSecurityPolicy(
            @NotNull ResourceObjectDefinition objectDefinition, @NotNull OperationResult result) {
        var systemConfiguration = getSystemConfigurationBean(result);
        var globalSecurityPolicy = locateGlobalSecurityPolicy(systemConfiguration, result);
        var specificSecurityPolicy = locateResourceObjectSecurityPolicyInternal(objectDefinition, result);
        return SecurityPolicyCustomMerger.mergeSecurityPolicies(specificSecurityPolicy, globalSecurityPolicy);
    }

    /**
     * Returns security policy related to the projection, resolved in the *legacy* way.
     *
     * - The global security policy is ignored.
     * - Value policy references in the returned object are resolved.
     */
    public @Nullable SecurityPolicyType locateResourceObjectSecurityPolicyLegacy(
            @NotNull ResourceObjectDefinition objectDefinition, @NotNull OperationResult result) {
        var specificSecurityPolicy = locateResourceObjectSecurityPolicyInternal(objectDefinition, result);
        resolveValuePolicyRefs(specificSecurityPolicy, result);
        return specificSecurityPolicy;
    }

    private @Nullable SecurityPolicyType locateResourceObjectSecurityPolicyInternal(
            @NotNull ResourceObjectDefinition objectDefinition, @NotNull OperationResult result) {
        var securityPolicyRef = objectDefinition.getSecurityPolicyRef();
        if (securityPolicyRef == null) {
            LOGGER.trace("Security policy not defined for {}", objectDefinition);
            return null;
        }
        LOGGER.trace("Loading security policy {} for {}", securityPolicyRef, objectDefinition);
        return resolve(securityPolicyRef, SecurityPolicyType.class, null, result);
    }

    public void resolveValuePolicyRefs(SecurityPolicyType securityPolicy, OperationResult result) {
        if (securityPolicy == null) {
            return;
        }
        var credentialsPolicy = securityPolicy.getCredentials();
        if (credentialsPolicy != null) {
            resolveValuePolicyRef(securityPolicy, credentialsPolicy.getPassword(), "password policy", result);
            for (var nonce : credentialsPolicy.getNonce()) {
                resolveValuePolicyRef(securityPolicy, nonce, "nonce credential policy", result);
            }
            resolveValuePolicyRef(
                    securityPolicy, credentialsPolicy.getSecurityQuestions(), "security questions policy", result);
        }
    }

    private void resolveValuePolicyRef(
            SecurityPolicyType securityPolicy, CredentialPolicyType credPolicy, String credShortDesc, OperationResult result) {
        if (credPolicy == null) {
            return;
        }
        ObjectReferenceType valuePolicyRef = credPolicy.getValuePolicyRef();
        if (valuePolicyRef == null) {
            return;
        }
        try {
            var valuePolicy = resolve(valuePolicyRef, ValuePolicyType.class, null, result);
            valuePolicyRef.asReferenceValue().setObject(valuePolicy.asPrismObject());
        } catch (SystemException e) {
            LoggingUtils.logException(LOGGER, "Couldn't resolve {} {} referenced from {}", e,
                    credShortDesc, valuePolicyRef.getOid(), securityPolicy);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <O extends ObjectType> @NotNull O resolve(
            @NotNull Referencable ref,
            @NotNull Class<O> expectedType,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) {
        try {
            var type = ObjectTypeUtil.getTypeClass(ref, expectedType);
            var oid = requireNonNull(ref.getOid(), "No-OID references are not supported while resolving %s", ref);
            return repositoryService
                    .getObject(type, oid, options, result)
                    .asObjectable();
        } catch (CommonException e) {
            // TODO Shouldn't we pass the SystemException instead to make administrator well aware of the broken configuration?
            throw new SystemException("Couldn't resolve " + ref + ": " + e.getMessage(), e); // See explanation in class javadoc
        }
    }

    private @Nullable SystemConfigurationType getSystemConfigurationBean(@NotNull OperationResult result) {
        try {
            return systemObjectCache.getSystemConfigurationBean(result);
        } catch (SchemaException e) {
            // TODO Shouldn't we pass the SystemException instead to make administrator well aware of the broken configuration?
            throw new SystemException("Couldn't get the system configuration: " + e.getMessage(), e); // see class javadoc
        }
    }

    // TODO optimize this method eventually (avoid merging unnecessary parts)
    public CredentialsPolicyType locateResourceObjectCredentialsPolicy(
            @NotNull ResourceObjectDefinition objectDefinition, OperationResult result) {
        var securityPolicy = locateResourceObjectSecurityPolicy(objectDefinition, result);
        return securityPolicy != null ? securityPolicy.getCredentials() : null;
    }
}
