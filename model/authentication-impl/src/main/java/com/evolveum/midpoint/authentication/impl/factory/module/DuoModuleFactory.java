/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.impl.module.authentication.DuoModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.DuoModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.DuoModuleWebSecurityConfigurer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DuoAuthenticationModuleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author skublik
 */
@Component
public class DuoModuleFactory extends RemoteModuleFactory<
        DuoModuleWebSecurityConfiguration,
        DuoModuleWebSecurityConfigurer,
        DuoAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(DuoModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof DuoAuthenticationModuleType;
    }

    @Override
    protected DuoModuleWebSecurityConfigurer createModuleConfigurer(
            DuoAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor,
            ServletRequest request) {
        return new DuoModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request);
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(
            DuoAuthenticationModuleType moduleType,
            DuoModuleWebSecurityConfiguration configuration,
            AuthenticationSequenceModuleType sequenceModule,
            ServletRequest request) {

        ItemPathType pathToUsernameAttributeBean = moduleType.getPathForDuoUsername();
        ItemPath pathToUsernameAttribute;
        if (pathToUsernameAttributeBean != null) {
            pathToUsernameAttribute = moduleType.getPathForDuoUsername().getItemPath();
        } else {
            pathToUsernameAttribute = FocusType.F_NAME;
        }
        DuoModuleAuthentication moduleAuthentication = new DuoModuleAuthentication(sequenceModule, pathToUsernameAttribute);

        IdentityProvider provider = createIdentityProvider(
                RemoteModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX,
                "",
                request,
                configuration,
                configuration.getModuleIdentifier());
        moduleAuthentication.setProviders(List.of(provider));

        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }
}
