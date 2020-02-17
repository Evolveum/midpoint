/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.factory.module;

import com.evolveum.midpoint.model.api.authentication.AuthModule;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.LdapWebSecurityConfig;
import com.evolveum.midpoint.web.security.module.authentication.LdapModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.LdapModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.provider.MidPointLdapAuthenticationProvider;
import com.evolveum.midpoint.web.security.util.AuthModuleImpl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.module.authentication.LoginFormModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.web.security.provider.PasswordProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.servlet.ServletRequest;
import java.util.Map;

/**
 * @author skublik
 */
@Component
public class LdapModuleFactory extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCredentialModuleFactory.class);

    @Autowired
    private Protector protector;

    @Autowired
    @Qualifier("userDetailsService")
    private UserDetailsContextMapper userDetailsContextMapper;

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof AuthenticationModuleLdapType) {
            return true;
        }
        return false;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence,
            ServletRequest request, Map<Class<? extends Object>, Object> sharedObjects,
            AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {

        if (!(moduleType instanceof AuthenticationModuleLdapType)) {
            LOGGER.error("This factory support only AuthenticationModuleLdapType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        ModuleWebSecurityConfigurationImpl configuration = LdapModuleWebSecurityConfiguration.build(moduleType, prefixOfSequence);
        configuration.setPrefixOfSequence(prefixOfSequence);

        configuration.addAuthenticationProvider(getProvider((AuthenticationModuleLdapType)moduleType, credentialPolicy));

        ModuleWebSecurityConfig module = createModule(configuration);
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthentication moduleAuthentication = createEmptyModuleAuthentication((AuthenticationModuleLdapType) moduleType, configuration);
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    protected AuthenticationProvider getProvider(AuthenticationModuleLdapType moduleType, CredentialsPolicyType credentialsPolicy){
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
        provider.setUserDetailsContextMapper(userDetailsContextMapper);
        getObjectObjectPostProcessor().postProcess(provider.getAuthenticatorProvider());
        getObjectObjectPostProcessor().postProcess(provider);

        return provider;
    };

    protected ModuleWebSecurityConfig createModule(ModuleWebSecurityConfiguration configuration) {
        return  getObjectObjectPostProcessor().postProcess(new LdapWebSecurityConfig((LdapModuleWebSecurityConfiguration) configuration));
    }

    protected ModuleAuthentication createEmptyModuleAuthentication(AuthenticationModuleLdapType moduleType,
                                                                   ModuleWebSecurityConfiguration configuration) {
        LdapModuleAuthentication moduleAuthentication = new LdapModuleAuthentication();
        moduleAuthentication.setPrefix(configuration.getPrefix());
        if (moduleType.getSearch() != null) {
            moduleAuthentication.setNamingAttribute(moduleType.getSearch().getNamingAttr());
        }
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        return moduleAuthentication;
    }
}
