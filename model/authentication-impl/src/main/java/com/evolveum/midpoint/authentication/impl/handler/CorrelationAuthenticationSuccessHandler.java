/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
        // The next correlator isn't processed yet, so the module must not keep the token of the previous correlator.
        // The anonymous authentication filter sets an anonymous token again (as for the first correlator),
        // so a failure of the next correlator is handled the same way (redirect to the correlation page
        // with the error message).
        correlationModuleAuthentication.setAuthentication(null);

        getRedirectStrategy().sendRedirect(request, response, "/correlation");

    }
}
