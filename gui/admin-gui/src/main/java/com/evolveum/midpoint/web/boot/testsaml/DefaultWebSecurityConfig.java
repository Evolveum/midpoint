/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.web.security.AuditedLogoutHandler;
import com.evolveum.midpoint.web.security.MidPointAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

public class DefaultWebSecurityConfig extends WebSecurityConfigurerAdapter {

    private List<AuthenticationProvider> authenticationProviders = new ArrayList<AuthenticationProvider>();

    public void addAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        if(authenticationProvider != null) {
            this.authenticationProviders.add(authenticationProvider);
        }
    }

    public List<AuthenticationProvider> getAuthenticationProviders() {
        return authenticationProviders;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (!getAuthenticationProviders().isEmpty()) {
            for (AuthenticationProvider authenticationProvider : getAuthenticationProviders()) {
                auth.authenticationProvider(authenticationProvider);
            }
        } else {
            super.configure(auth);
        }
    }

    public HttpSecurity getNewHttp() throws Exception {
        return getHttp();
    }

}

