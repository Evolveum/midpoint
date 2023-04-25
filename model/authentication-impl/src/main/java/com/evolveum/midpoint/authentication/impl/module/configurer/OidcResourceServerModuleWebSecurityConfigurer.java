/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidpointHttpAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.impl.entry.point.HttpAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.SequenceAuditFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcResourceServerModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.oidc.OidcBearerTokenAuthenticationFilter;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @author skublik
 */

public class OidcResourceServerModuleWebSecurityConfigurer<C extends OidcResourceServerModuleWebSecurityConfiguration>
        extends ModuleWebSecurityConfigurer<C> {

    @Autowired
    private ModelService model;

    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired
    private SecurityContextManager securityContextManager;

    @Autowired
    private TaskManager taskManager;

    public OidcResourceServerModuleWebSecurityConfigurer(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        HttpAuthenticationEntryPoint entryPoint = getObjectPostProcessor().postProcess(new HttpAuthenticationEntryPoint());
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");

        OidcBearerTokenAuthenticationFilter filter = getObjectPostProcessor().postProcess(new OidcBearerTokenAuthenticationFilter(authenticationManager(), entryPoint));
        RememberMeServices rememberMeServices = http.getSharedObject(RememberMeServices.class);
        if (rememberMeServices != null) {
            filter.setRememberMeServices(rememberMeServices);
        }
        http.authorizeRequests().accessDecisionManager(new MidpointHttpAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager, model));
        http.addFilterAt(filter, BasicAuthenticationFilter.class);

        SequenceAuditFilter sequenceAuditFilter = getObjectPostProcessor().postProcess(new SequenceAuditFilter());
        sequenceAuditFilter.setRecordOnEndOfChain(false);
        http.addFilterAfter(sequenceAuditFilter, BasicAuthenticationFilter.class);


        http.formLogin().disable()
                .csrf().disable();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(entryPoint)
                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());
    }
}
