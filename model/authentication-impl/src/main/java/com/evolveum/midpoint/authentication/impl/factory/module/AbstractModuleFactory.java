/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.Map;

import com.evolveum.midpoint.authentication.api.ModuleFactory;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import com.evolveum.midpoint.authentication.impl.filter.RefuseUnauthenticatedRequestFilter;
import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

/**
 * @author skublik
 */

public abstract class AbstractModuleFactory<
        C extends ModuleWebSecurityConfiguration,
        CA extends ModuleWebSecurityConfigurer<C, MT>,
        MT extends AbstractAuthenticationModuleType,
        MA extends ModuleAuthentication> implements ModuleFactory<MT, MA> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractModuleFactory.class);

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Autowired
    private AuthModuleRegistryImpl registry;

    @Autowired
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    public AuthModuleRegistryImpl getRegistry() {
        return registry;
    }

    public ObjectPostProcessor<Object> getObjectObjectPostProcessor() {
        return objectObjectPostProcessor;
    }

    public abstract boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel);

    @Override
    public AuthModule<MA> createAuthModule(MT moduleType, String sequenceSuffix,
            ServletRequest request, Map<Class<?>, Object> sharedObjects,
            AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy,
            AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {

        if (moduleType == null) {
            LOGGER.error("This factory support only HttpHeaderAuthenticationModuleType, but modelType is null ");
            throw new IllegalArgumentException("Unsupported factory " + this.getClass().getSimpleName()
                    + " for null module ");
        }

        isSupportedChannel(authenticationChannel);

        CA configurer = createModuleConfigurer(moduleType, sequenceSuffix, authenticationChannel, getObjectObjectPostProcessor(), request);

        CA moduleConfigurer = getObjectObjectPostProcessor()
                .postProcess(configurer);

        HttpSecurity http =  moduleConfigurer.getNewHttpSecurity(sharedObjects);
        http.addFilterAfter(new RefuseUnauthenticatedRequestFilter(), SwitchUserFilter.class);

        SecurityFilterChain filter = http.build();
        postProcessFilter(filter, moduleConfigurer);

        MA moduleAuthentication = createEmptyModuleAuthentication(moduleType, moduleConfigurer.getConfiguration(), sequenceModule, request);
        moduleAuthentication.setFocusType(moduleType.getFocusType());

        return AuthModuleImpl.build(filter, moduleConfigurer.getConfiguration(), moduleAuthentication);
    }

    protected void postProcessFilter(SecurityFilterChain filter, CA configurer) {
        // Nothing to do here. Subclasses may override.
    }

    protected abstract CA createModuleConfigurer(MT moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request);

    protected abstract MA createEmptyModuleAuthentication(
            MT moduleType, C configuration,
            AuthenticationSequenceModuleType sequenceModule,
            ServletRequest request);


    public Integer getOrder() {
        return 0;
    }

    protected void isSupportedChannel(AuthenticationChannel authenticationChannel) {
        if (authenticationChannel == null) {
            return;
        }
        if (SchemaConstants.CHANNEL_SELF_REGISTRATION_URI.equals(authenticationChannel.getChannelId())) {
            throw new IllegalArgumentException("Unsupported factory " + this.getClass().getSimpleName()
                    + " for channel " + authenticationChannel.getChannelId());
        }
    }

//    HttpSecurity getNewHttpSecurity(ModuleWebSecurityConfigurer module) throws Exception {
////        module.setObjectPostProcessor(getObjectObjectPostProcessor());
//        HttpSecurity httpSecurity =  module.getNewHttpSecurity();
//        httpSecurity.addFilterAfter(new RefuseUnauthenticatedRequestFilter(), SwitchUserFilter.class);
//        return httpSecurity;
//    }

}
