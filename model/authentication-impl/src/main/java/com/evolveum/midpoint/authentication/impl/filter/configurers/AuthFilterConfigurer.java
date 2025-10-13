/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.configurers;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.authentication.impl.filter.MidpointAuthFilter;
import com.evolveum.midpoint.authentication.impl.filter.SequenceAuditFilter;
import com.evolveum.midpoint.authentication.impl.filter.TransformExceptionFilter;

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.accept.ContentNegotiationStrategy;

/**
 * @author skublik
 */

public class AuthFilterConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractHttpConfigurer<SecurityContextConfigurer<H>, H> {

    @Override
    public void configure(H http) throws Exception {

        Map<Class<?>, Object> sharedObjects = new HashMap<>();

        setSharedObject(sharedObjects, http, InvalidSessionStrategy.class);
        setSharedObject(sharedObjects, http, SessionAuthenticationStrategy.class);
        setSharedObject(sharedObjects, http, ApplicationContext.class);
        setSharedObject(sharedObjects, http, ContentNegotiationStrategy.class);
        setSharedObject(sharedObjects, http, SecurityContextRepository.class);

        MidpointAuthFilter mpFilter = postProcess(new MidpointAuthFilter(sharedObjects));
        mpFilter.createFilterForAuthenticatedRequest();
        http.addFilterBefore(mpFilter, SessionManagementFilter.class);

        http.addFilterAfter(postProcess(new SequenceAuditFilter()), AnonymousAuthenticationFilter.class);

        http.addFilterAfter(new TransformExceptionFilter(), AnonymousAuthenticationFilter.class);
    }


    private void setSharedObject(Map<Class<?>, Object> sharedObjects, H http, Class<?> clazz) {
        sharedObjects.put(clazz, http.getSharedObject(clazz));
    }
}
