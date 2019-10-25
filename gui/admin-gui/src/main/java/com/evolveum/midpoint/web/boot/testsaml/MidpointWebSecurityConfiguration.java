/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.web.boot.WebSecurityConfig;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.debug.DebugFilter;
import org.springframework.web.accept.ContentNegotiationStrategy;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

@Configuration
public class MidpointWebSecurityConfiguration extends WebSecurityConfiguration {

    @Autowired(required = false)
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Autowired
    ApplicationContext context;

    @Override
    public Filter springSecurityFilterChain() throws Exception {
        Filter filter = super.springSecurityFilterChain();
        if (filter instanceof FilterChainProxy) {
            List<SecurityFilterChain> filters;
            if (!((FilterChainProxy) filter).getFilterChains().isEmpty()) {
                filters = new ArrayList<SecurityFilterChain>();
                filters.addAll(((FilterChainProxy) filter).getFilterChains());
                filters.remove(filters.size() - 1);
            } else {
                filters = ((FilterChainProxy) filter).getFilterChains();
            }
            MidpointFilterChainProxy mpFilter = objectObjectPostProcessor.postProcess(new MidpointFilterChainProxy(filters));
            mpFilter.afterPropertiesSet();
            return mpFilter;
        }
        return  filter;
    }


}
