/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * @author skublik
 */

public class BasicMidPointAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setState(AuthenticationModuleState.SUCCESSFULLY);
        }
    }
}
