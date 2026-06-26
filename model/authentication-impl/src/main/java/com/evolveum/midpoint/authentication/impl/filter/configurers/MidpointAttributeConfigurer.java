/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.configurers;

import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import static com.evolveum.midpoint.authentication.impl.util.MidpointRequestMatchers.pathMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class MidpointAttributeConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractAuthenticationFilterConfigurer<H, MidpointAttributeConfigurer<H>, AbstractAuthenticationProcessingFilter> {

    public MidpointAttributeConfigurer(AbstractAuthenticationProcessingFilter filter) {
        super(filter, null);
    }
    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return pathMatcher(loginProcessingUrl, "POST");
    }

    @Override
    public MidpointAttributeConfigurer<H> loginPage(String loginPage) {
        return super.loginPage(loginPage);
    }
}
