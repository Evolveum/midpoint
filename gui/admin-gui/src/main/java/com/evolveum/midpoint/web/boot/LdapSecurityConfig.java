/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import java.util.Arrays;

/**
 * Created by Viliam Repan (lazyman).
 */
@Profile("ldap")
@Configuration
public class LdapSecurityConfig {

    @Value("${auth.ldap.host}")
    private String ldapHost;

    @Value("${auth.ldap.manager}")
    private String ldapUserDn;
    @Value("${auth.ldap.manager.password}")
    private String ldapUserPassword;

    @Value("${auth.ldap.dn.pattern:}")
    private String ldapDnPattern;

    @Value("${auth.ldap.search.pattern:}")
    private String ldapSearchPattern;

    @Value("${auth.ldap.search.subtree}")
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
           @Qualifier("userDetailsService") UserDetailsContextMapper userDetailsContextMapper) {

        BindAuthenticator auth = new BindAuthenticator(contextSource());
        if (StringUtils.isNotEmpty(ldapDnPattern)) {
            auth.setUserDnPatterns(new String[]{ldapDnPattern});
        }
        if (StringUtils.isNotEmpty(ldapSearchPattern)) {
            auth.setUserSearch(userSearch());
        }

        MidPointLdapAuthenticationProvider provider = new MidPointLdapAuthenticationProvider(auth);
        provider.setUserDetailsContextMapper(userDetailsContextMapper);

        return provider;
    }

    @ConditionalOnProperty("auth.ldap.search.pattern")
    @Bean
    public FilterBasedLdapUserSearch userSearch() {
        FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch("", ldapSearchPattern, contextSource());
        search.setSearchSubtree(searchSubtree);
        return search;
    }
}
