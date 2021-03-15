/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author skublik
 */

public class MidpointRegisterSessionAuthenticationStrategy extends RegisterSessionAuthenticationStrategy {

    private RegisterSessionAuthenticationStrategy strategy;

    public MidpointRegisterSessionAuthenticationStrategy(RegisterSessionAuthenticationStrategy strategy) {
        super(new SessionRegistryImpl());
        this.strategy = strategy;
    }

    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws SessionAuthenticationException {
        if (!SecurityUtils.isRecordSessionLessAccessChannel(request)) {
            strategy.onAuthentication(authentication, request, response);
        }
    }
}
