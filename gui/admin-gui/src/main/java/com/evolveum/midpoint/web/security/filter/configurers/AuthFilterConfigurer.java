/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter.configurers;

import com.evolveum.midpoint.web.security.filter.MidpointAuthFilter;
import com.evolveum.midpoint.web.security.filter.TranslateExceptionFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.accept.ContentNegotiationStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author skublik
 */

public class AuthFilterConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractHttpConfigurer<SecurityContextConfigurer<H>, H> {

    @Override
    @SuppressWarnings("unchecked")
    public void configure(H http) throws Exception {

        Map<Class<? extends Object>, Object> sharedObjects = new HashMap<Class<? extends Object>, Object>();

        setSharedObject(sharedObjects, http, InvalidSessionStrategy.class);
        setSharedObject(sharedObjects, http, SessionAuthenticationStrategy.class);
        setSharedObject(sharedObjects, http, ApplicationContext.class);
        setSharedObject(sharedObjects, http, ContentNegotiationStrategy.class);
        setSharedObject(sharedObjects, http, SecurityContextRepository.class);
        MidpointAuthFilter mpFilter = postProcess(new MidpointAuthFilter(sharedObjects));
        mpFilter.createFilterForAuthenticatedRequest();
        http.addFilterBefore(mpFilter, SessionManagementFilter.class);
        http.addFilterBefore(new TranslateExceptionFilter(), MidpointAuthFilter.class);
    }

    private void setSharedObject(Map<Class<? extends Object>, Object> sharedObjects, H http, Class<? extends Object> clazz) {
        sharedObjects.put(clazz, http.getSharedObject(clazz));
    }
}
