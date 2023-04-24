/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * @author skublik
 */

public abstract class AbstractModuleFactory {

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

    public abstract AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String sequenceSuffix,
                                                  ServletRequest request, Map<Class<?>, Object> sharedObjects,
                                                  AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy,
                                                  AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception;

    protected Integer getOrder(){
        return 0;
    }

    protected void setSharedObjects(HttpSecurity http, Map<Class<?>, Object> sharedObjects) {
        for (Map.Entry<Class<?>, Object> sharedObject : sharedObjects.entrySet()) {
            http.setSharedObject((Class<? super Object>) sharedObject.getKey(), sharedObject.getValue());
        }
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

    protected HttpSecurity getNewHttpSecurity(ModuleWebSecurityConfigurer module) throws Exception {
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        return module.getNewHttpSecurity();
    }

}
