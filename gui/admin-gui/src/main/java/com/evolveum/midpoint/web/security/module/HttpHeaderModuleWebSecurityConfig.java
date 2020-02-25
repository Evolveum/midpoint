/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module;

import com.evolveum.midpoint.web.security.MidpointAuthenticationFauileHandler;
import com.evolveum.midpoint.web.security.MidpointProviderManager;
import com.evolveum.midpoint.web.security.filter.MidpointRequestHeaderAuthenticationFilter;
import com.evolveum.midpoint.web.security.module.configuration.HttpHeaderModuleWebSecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

/**
 * @author skublik
 */

public class HttpHeaderModuleWebSecurityConfig<C extends HttpHeaderModuleWebSecurityConfiguration> extends LoginFormModuleWebSecurityConfig<C> {

    @Autowired
    private MidpointProviderManager authenticationManager;

    public HttpHeaderModuleWebSecurityConfig(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.addFilterBefore(requestHeaderAuthenticationFilter(), LogoutFilter.class);
        http.logout().logoutSuccessHandler(createLogoutHandler(getConfiguration().getDefaultSuccessLogoutURL()));
    }


    private RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
        MidpointRequestHeaderAuthenticationFilter filter = new MidpointRequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(getConfiguration().getPrincipalRequestHeader());
        filter.setExceptionIfHeaderMissing(false);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationFailureHandler(new MidpointAuthenticationFauileHandler());

        return filter;
    }
}
