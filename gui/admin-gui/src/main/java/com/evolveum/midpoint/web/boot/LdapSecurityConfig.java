/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.model.api.authentication.MidPointLdapAuthenticationProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

/**
 * Created by Viliam Repan (lazyman).
 */
@Profile("ldap")
@Configuration
public class LdapSecurityConfig {

    @Value("${auth.ldap.host}")
    private String ldapHost;

    @Value("${auth.ldap.manager:}")
    private String ldapUserDn;
    @Value("${auth.ldap.password:#{null}}")
    private String ldapUserPassword;

    @Value("${auth.ldap.dn.pattern:#{null}}")
    private String ldapDnPattern;

    @Value("${auth.ldap.search.pattern:#{null}}")
    private String ldapSearchPattern;

    @Value("${auth.ldap.search.subtree:true}")
    private boolean searchSubtree;

    @Bean
    public LdapContextSource contextSource() {
        DefaultSpringSecurityContextSource ctx = new DefaultSpringSecurityContextSource(ldapHost);
        ctx.setUserDn(ldapUserDn);
        ctx.setPassword(ldapUserPassword);

        return ctx;
    }

    @Bean
    public MidPointLdapAuthenticationProvider midPointAuthenticationProvider(
            @Qualifier("focusDetailsService") UserDetailsContextMapper userDetailsContextMapper) {

        MidPointLdapAuthenticationProvider provider = new MidPointLdapAuthenticationProvider(bindAuthenticator());
        provider.setUserDetailsContextMapper(userDetailsContextMapper);

        return provider;
    }

    @Bean
    public BindAuthenticator bindAuthenticator() {
        BindAuthenticator auth = new BindAuthenticator(contextSource());
        if (StringUtils.isNotEmpty(ldapDnPattern)) {
            auth.setUserDnPatterns(new String[]{ldapDnPattern});
        }
        if (StringUtils.isNotEmpty(ldapSearchPattern)) {
            auth.setUserSearch(userSearch());
        }

        return auth;
    }

    @ConditionalOnProperty("auth.ldap.search.pattern")
    @Bean
    public FilterBasedLdapUserSearch userSearch() {
        FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch("", ldapSearchPattern, contextSource());
        search.setSearchSubtree(searchSubtree);
        return search;
    }
}
