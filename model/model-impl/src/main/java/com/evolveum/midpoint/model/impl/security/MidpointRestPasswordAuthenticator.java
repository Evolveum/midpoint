/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import javax.ws.rs.container.ContainerRequestContext;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;

@Component
public class MidpointRestPasswordAuthenticator extends MidpointRestAuthenticator<PasswordAuthenticationContext>{


    @Autowired(required = true)
    private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;

    @Override
    protected AuthenticationEvaluator<PasswordAuthenticationContext> getAuthenticationEvaluator() {
        return passwordAuthenticationEvaluator;
    }

    @Override
    protected PasswordAuthenticationContext createAuthenticationContext(AuthorizationPolicy policy, ContainerRequestContext requestCtx, Class<? extends FocusType> clazz){
        return new PasswordAuthenticationContext(policy.getUserName(), policy.getPassword(), clazz);
    }

}
