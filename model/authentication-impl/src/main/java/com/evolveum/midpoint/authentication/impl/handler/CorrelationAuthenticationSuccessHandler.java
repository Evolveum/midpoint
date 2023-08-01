/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.handler;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.CorrelationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;

import com.evolveum.midpoint.security.api.MidPointPrincipal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        if (mpAuthentication.getPrincipal() instanceof MidPointPrincipal) {
//            moduleAuthentication.setState(AuthenticationModuleState.SUCCESSFULLY);
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }


        if (!(moduleAuthentication instanceof CorrelationModuleAuthenticationImpl correlationModuleAuthentication)) {
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
