/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.factory.module.AbstractModuleFactory;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.HttpClusterModuleFactory;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AuthenticationSequenceModuleCreator<MA extends ModuleAuthentication> {

    private static final Trace LOGGER = TraceManager.getTrace(AuthenticationSequenceModuleCreator.class);

    private final AuthModuleRegistryImpl authRegistry;
    private final AuthenticationSequenceType sequence;
    private final HttpServletRequest request;
    private final AuthenticationModulesType authenticationModulesType;
    private final AuthenticationChannel authenticationChannel;
    private CredentialsPolicyType credentialPolicy;
    private Map<Class<?>, Object> sharedObjects;

    public AuthenticationSequenceModuleCreator(
            AuthModuleRegistryImpl authRegistry,
            AuthenticationSequenceType sequence,
            HttpServletRequest request,
            AuthenticationModulesType authenticationModulesType,
            AuthenticationChannel authenticationChannel) {
        this.authRegistry = authRegistry;
        this.sequence = sequence;
        this.request = request;
        this.authenticationModulesType = authenticationModulesType;
        this.authenticationChannel = authenticationChannel;
    }

    public AuthenticationSequenceModuleCreator credentialsPolicy(CredentialsPolicyType credentialPolicy) {
        this.credentialPolicy = credentialPolicy;
        return this;
    }

    public AuthenticationSequenceModuleCreator sharedObjects(Map<Class<?>, Object> sharedObjects) {
        this.sharedObjects = sharedObjects;
        return this;
    }

    public List<AuthModule<MA>> create() {
        Validate.notNull(authRegistry, "Registry for module factories is null");

        if (AuthSequenceUtil.isClusterSequence(request)) {
            return getSpecificModuleFilter(authRegistry, sequence.getChannel().getUrlSuffix(), request,
                    sharedObjects, authenticationModulesType, credentialPolicy);
        }

        Validate.notEmpty(sequence.getModule(), "Sequence " +
                (AuthSequenceUtil.getAuthSequenceIdentifier(sequence)) + " don't contains authentication modules");

        List<AuthenticationSequenceModuleType> sequenceModules = SecurityPolicyUtil.getSortedModules(sequence);
        return sequenceModules
                .stream()
                .map(this::createAuthModule)
                .collect(Collectors.toList());

    }

    private AuthModule<MA> createAuthModule(AuthenticationSequenceModuleType sequenceModule) {
        try {
            String sequenceModuleIdentifier = StringUtils.isNotEmpty(sequenceModule.getIdentifier()) ?
                    sequenceModule.getIdentifier() : sequenceModule.getName();
            AbstractAuthenticationModuleType module = getModuleByIdentifier(sequenceModuleIdentifier, authenticationModulesType);
            AbstractModuleFactory<AbstractAuthenticationModuleType, MA> moduleFactory = authRegistry.findModuleFactory(module, authenticationChannel);

            return moduleFactory.createModuleFilter(module, sequence.getChannel().getUrlSuffix(), request,
                    sharedObjects, authenticationModulesType, credentialPolicy, authenticationChannel, sequenceModule);

        } catch (Exception e) {
            LOGGER.error("Couldn't build filter for module moduleFactory", e);
        }
        return null;
    }

    private List<AuthModule<MA>> getSpecificModuleFilter(AuthModuleRegistryImpl authRegistry, String urlSuffix, HttpServletRequest httpRequest, Map<Class<?>, Object> sharedObjects,
            AuthenticationModulesType authenticationModulesType, CredentialsPolicyType credentialPolicy) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String channel = AuthSequenceUtil.searchChannelByPath(localePath);
        if (!AuthSequenceUtil.isPathForChannel("ws", channel)) {
            return null;
        }
        String header = httpRequest.getHeader("Authorization");
        if (header != null) {
            String type = header.split(" ")[0];
            if (AuthenticationModuleNameConstants.CLUSTER.equalsIgnoreCase(type)) {
                List<AuthModule<MA>> authModules = new ArrayList<>();
                HttpClusterModuleFactory factory = authRegistry.findModelFactoryByClass(HttpClusterModuleFactory.class);
                AbstractAuthenticationModuleType module = new AbstractAuthenticationModuleType() {
                };
                module.setIdentifier(AuthenticationModuleNameConstants.CLUSTER.toLowerCase() + "-module");
                try {
                    //noinspection unchecked
                    authModules.add((AuthModule<MA>) factory.createModuleFilter(module, urlSuffix, httpRequest,
                            sharedObjects, authenticationModulesType, credentialPolicy, null,
                            new AuthenticationSequenceModuleType()
                                    .necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT)
                                    .order(10)
                    ));
                } catch (Exception e) {
                    LOGGER.error("Couldn't create module for cluster authentication");
                    return null;
                }
                return authModules;
            }
        }

        return null;
    }

    private AbstractAuthenticationModuleType getModuleByIdentifier(String identifier, AuthenticationModulesType authenticationModulesType) {
        PrismContainerValue<?> modulesContainerValue = authenticationModulesType.asPrismContainerValue();

        List<AbstractAuthenticationModuleType> modules = new ArrayList<>();
        modulesContainerValue.accept(v -> {
            if (!(v instanceof PrismContainer<?> c)) {
                return;
            }

            if (!(AbstractAuthenticationModuleType.class.isAssignableFrom(Objects.requireNonNull(c.getCompileTimeClass())))) {
                return;
            }

            c.getValues().forEach(x -> modules.add((AbstractAuthenticationModuleType) ((PrismContainerValue<?>) x).asContainerable()));
        });

        for (AbstractAuthenticationModuleType module : modules) {
            String moduleIdentifier = StringUtils.isNotEmpty(module.getIdentifier()) ? module.getIdentifier() : module.getName();
            if (moduleIdentifier != null && StringUtils.equals(moduleIdentifier, identifier)) {
                return module;
            }
        }
        return null;
    }


}
