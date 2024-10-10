/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.filter.ldap.MidpointPrincipalContextMapper;
import com.evolveum.midpoint.authentication.impl.module.authentication.LdapModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LdapModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.LdapWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.MidPointLdapAuthenticationProvider;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LdapAuthenticationModuleType;

/**
 * @author skublik
 */
@Component
public class LdapModuleFactory extends AbstractModuleFactory<
        LdapModuleWebSecurityConfiguration,
        LdapWebSecurityConfigurer,
        LdapAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(LdapModuleFactory.class);

    @Autowired private Protector protector;

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof LdapAuthenticationModuleType;
    }

    @Override
    protected LdapWebSecurityConfigurer createModuleConfigurer(LdapAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new LdapWebSecurityConfigurer(moduleType,
                sequenceSuffix,
                authenticationChannel,
                objectPostProcessor,
                request,
                getProvider(moduleType));
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(LdapAuthenticationModuleType moduleType, LdapModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        LdapModuleAuthentication moduleAuthentication = new LdapModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        if (moduleType.getSearch() != null) {
            moduleAuthentication.setNamingAttribute(moduleType.getSearch().getNamingAttr());
        }
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

    private AuthenticationProvider getProvider(LdapAuthenticationModuleType moduleType){
        DefaultSpringSecurityContextSource ctx = new DefaultSpringSecurityContextSource(moduleType.getHost());
        ctx.setUserDn(moduleType.getUserDn());

        try {
            if (moduleType.getUserPassword() != null) {
                ctx.setPassword(protector.decryptString(moduleType.getUserPassword()));
            }
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

}
