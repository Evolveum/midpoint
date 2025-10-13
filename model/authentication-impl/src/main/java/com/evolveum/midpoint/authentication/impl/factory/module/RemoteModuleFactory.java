/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

/**
 * @author skublik
 */
public abstract class RemoteModuleFactory<
        C extends ModuleWebSecurityConfiguration,
        CA extends ModuleWebSecurityConfigurer<C, MT>,
        MT extends AbstractAuthenticationModuleType,
        MA extends ModuleAuthentication> extends AbstractModuleFactory<C, CA, MT, MA> {


    protected IdentityProvider createIdentityProvider(String requestProcessingUrl,
            String registrationId,
            ServletRequest request,
            C configuration,
            String linkText) {
        String authRequestPrefixUrl = request.getServletContext().getContextPath() + configuration.getPrefixOfModule()
                + requestProcessingUrl;
        return new IdentityProvider()
                .setLinkText(linkText)
                .setRedirectLink(authRequestPrefixUrl.replace("{registrationId}", registrationId));
    }
}
