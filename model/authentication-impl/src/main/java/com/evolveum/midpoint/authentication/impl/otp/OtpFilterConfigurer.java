/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.authentication.impl.module.configurer.DuoModuleWebSecurityConfigurer;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;

public class OtpFilterConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractAuthenticationFilterConfigurer<H, OtpFilterConfigurer<H>, OtpAuthenticationFilter> {

    public OtpFilterConfigurer(OtpAuthenticationFilter filter) {
        super(filter, null);

    }

    @Override
    public void init(H http) throws Exception {
//        sendLoginProcessingUrlToSuper();
        super.loginPage(OtpModuleWebSecurityConfigurer.LOGIN_PAGE_URL);
        super.init(http);
    }

    @Override
    public OtpFilterConfigurer<H> loginPage(String loginPage) {
        return super.loginPage(loginPage);
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, loginProcessingUrl);
    }
}
