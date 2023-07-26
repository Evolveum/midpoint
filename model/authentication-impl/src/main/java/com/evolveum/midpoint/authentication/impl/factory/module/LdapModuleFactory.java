/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.Map;
import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.impl.ldap.MidpointPrincipalContextMapper;
import com.evolveum.midpoint.authentication.impl.provider.MidPointLdapAuthenticationProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.LdapWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.LdapModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.LdapModuleWebSecurityConfiguration;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */
@Component
public class LdapModuleFactory extends AbstractModuleFactory<LdapAuthenticationModuleType> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCredentialModuleFactory.class);

    @Autowired
    private Protector protector;

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof LdapAuthenticationModuleType;
    }

    @Override
    public AuthModule createModuleFilter(LdapAuthenticationModuleType moduleType, String sequenceSuffix,
            ServletRequest request, Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {

        if (!(moduleType instanceof LdapAuthenticationModuleType)) {
            LOGGER.error("This factory support only LdapAuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        LdapModuleWebSecurityConfiguration configuration = LdapModuleWebSecurityConfiguration.build(moduleType, sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);

        configuration.addAuthenticationProvider(getProvider(moduleType));

        LdapWebSecurityConfigurer<LdapModuleWebSecurityConfiguration> module = createModule(configuration);
        HttpSecurity http = getNewHttpSecurity(module);
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(
                moduleType, configuration, sequenceModule);
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private AuthenticationProvider getProvider(LdapAuthenticationModuleType moduleType){
        DefaultSpringSecurityContextSource ctx = new DefaultSpringSecurityContextSource(moduleType.getHost());
        ctx.setUserDn(moduleType.getUserDn());

        try {
            ctx.setPassword(protector.decryptString(moduleType.getUserPassword()));
        } catch (EncryptionException e) {
            LOGGER.error("Couldn't obtain clear string for configuration of LDAP user password from " + moduleType.getUserPassword());
        }
        getObjectObjectPostProcessor().postProcess(ctx);

        BindAuthenticator auth = new BindAuthenticator(ctx);
        if (StringUtils.isNotEmpty(moduleType.getDnPattern())) {
            auth.setUserDnPatterns(new String[]{moduleType.getDnPattern()});
        }
        if (moduleType.getSearch() != null) {
            FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch("", moduleType.getSearch().getPattern(), ctx);
            if (moduleType.getSearch().isSubtree() != null) {
                search.setSearchSubtree(moduleType.getSearch().isSubtree());
            }
            getObjectObjectPostProcessor().postProcess(search);
            auth.setUserSearch(search);
        }
        getObjectObjectPostProcessor().postProcess(auth);

        MidPointLdapAuthenticationProvider provider = new MidPointLdapAuthenticationProvider(auth);
        provider.setUserDetailsContextMapper(getObjectObjectPostProcessor().postProcess(new MidpointPrincipalContextMapper()));
        getObjectObjectPostProcessor().postProcess(provider.getAuthenticatorProvider());
        getObjectObjectPostProcessor().postProcess(provider);

        return provider;
    }

    private LdapWebSecurityConfigurer<LdapModuleWebSecurityConfiguration> createModule(LdapModuleWebSecurityConfiguration configuration) {
        return  getObjectObjectPostProcessor().postProcess(new LdapWebSecurityConfigurer<>(configuration));
    }

    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(LdapAuthenticationModuleType moduleType,
            ModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule) {
        LdapModuleAuthentication moduleAuthentication = new LdapModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        if (moduleType.getSearch() != null) {
            moduleAuthentication.setNamingAttribute(moduleType.getSearch().getNamingAttr());
        }
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }
}
