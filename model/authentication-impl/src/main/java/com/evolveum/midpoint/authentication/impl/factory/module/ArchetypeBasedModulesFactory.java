/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.authentication.ArchetypeBasedModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.ArchetypeBasedModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.ArchetypeBasedModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.ClusterProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ArchetypeBasedModulesFactory  extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeBasedModulesFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof ArchetypeBasedModuleType;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String sequenceSuffix,
            ServletRequest request, Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel,
            AuthenticationSequenceModuleType sequenceModule) throws Exception {

        if (!(moduleType instanceof ArchetypeBasedModuleType)) {
            LOGGER.error("This factory support only ArchetypeBasedModuleType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        ArchetypeBasedModuleWebSecurityConfiguration configuration =
                ArchetypeBasedModuleWebSecurityConfiguration.build(moduleType, sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);

        configuration.addAuthenticationProvider(createProvider());

        ArchetypeBasedModuleWebSecurityConfigurer<ArchetypeBasedModuleWebSecurityConfiguration> module = createModule(configuration);
        HttpSecurity http = getNewHttpSecurity(module);
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(
                (ArchetypeBasedModuleType) moduleType, configuration, sequenceModule);
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private AuthenticationProvider createProvider(){
        return getObjectObjectPostProcessor().postProcess(new ClusterProvider());
    }

    private ArchetypeBasedModuleWebSecurityConfigurer<ArchetypeBasedModuleWebSecurityConfiguration> createModule(
            ArchetypeBasedModuleWebSecurityConfiguration configuration) {
        return  getObjectObjectPostProcessor().postProcess(new ArchetypeBasedModuleWebSecurityConfigurer<>(configuration));
    }

    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(ArchetypeBasedModuleType moduleType,
            ModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule) {
        ArchetypeBasedModuleAuthentication moduleAuthentication = new ArchetypeBasedModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

    @Override
    protected void isSupportedChannel(AuthenticationChannel authenticationChannel) {
        if (authenticationChannel == null) {
            return;
        }
        if (!SchemaConstants.CHANNEL_LOGIN_RECOVERY_URI.equals(authenticationChannel.getChannelId())) {
            throw new IllegalArgumentException("Unsupported factory " + this.getClass().getSimpleName()
                    + " for channel " + authenticationChannel.getChannelId());
        }
    }
}
