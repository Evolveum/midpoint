/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.handler;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.CorrelationModuleAuthenticationImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.Authentication;

import java.io.IOException;

public class CorrelationAuthenticationSuccessHandler extends MidPointAuthenticationSuccessHandler {


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();

        if (!(moduleAuthentication instanceof CorrelationModuleAuthenticationImpl correlationModuleAuthentication)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        if (CollectionUtils.isNotEmpty(correlationModuleAuthentication.getOwners())) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        if (correlationModuleAuthentication.isLastCorrelator()) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        correlationModuleAuthentication.setNextCorrelator();

//        super.onAuthenticationSuccess(request, response, authentication);
//        request.
        moduleAuthentication.setState(AuthenticationModuleState.LOGIN_PROCESSING);

        getRedirectStrategy().sendRedirect(request, response, "/correlation");

    }
}
