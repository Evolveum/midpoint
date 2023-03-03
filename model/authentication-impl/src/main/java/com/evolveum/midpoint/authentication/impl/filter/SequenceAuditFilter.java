/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;

import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.OncePerRequestFilter;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This filter has to run after login filter was run and the success and failure handlers
 * finished their evaluation. In those handlers, module state is set which is crucial for
 * correct evaluation.
 *
 * The aim of SequenceAuditFilter is to check the overall authentication, authentication for
 * the whole sequence. While partial (module) authentication results are evaluated and
 * recorded by corresponding provider (plus evaluator), the overall status if the whole
 * sequence authentication was successful or not is recorded here. It should be recoded
 * only once per sequence, therefore the isAlreadyRecorded() check.
 *
 * The result is recorded to two places:
 *
 * - focus/behavior/autentication
 * - audit
 */
public class SequenceAuditFilter extends OncePerRequestFilter {

    private static final Trace LOGGER = TraceManager.getTrace(SequenceAuditFilter.class);

    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;

    public SequenceAuditFilter(FocusAuthenticationResultRecorder authenticationRecorder) {
        this.authenticationRecorder = authenticationRecorder;
    }

    public SequenceAuditFilter() {
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        LOGGER.info("Running SequenceAuditFilter");

        Authentication authentication = SecurityUtil.getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            LOGGER.trace("No MidpointAuthentication present, continue with filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
        if (mpAuthentication.isAlreadyAudited()) {
            LOGGER.trace("Skipping auditing of authentication record, already audited.");
            filterChain.doFilter(request, response);
            return;
        }

        MidPointPrincipal mpPrincipal = mpAuthentication.getPrincipal() instanceof MidPointPrincipal ? (MidPointPrincipal) mpAuthentication.getPrincipal() : null;
        boolean isAuthenticated = mpAuthentication.isAuthenticated();
        if (isAuthenticated) {
            authenticationRecorder.recordSequenceAuthenticationSuccess(mpPrincipal, createConnectionEnvironment(mpAuthentication));
            mpAuthentication.setAlreadyAudited(true);
            LOGGER.trace("Authentication sequence {} evaluated as successful.", mpAuthentication.getSequenceIdentifier());
        } else if (mpAuthentication.isFinished()) {
            authenticationRecorder.recordSequenceAuthenticationFailure(mpAuthentication.getUsername(), mpPrincipal, null,
                    mpAuthentication.getFailedReason(), createConnectionEnvironment(mpAuthentication));
            mpAuthentication.setAlreadyAudited(true);
            HttpSession session = request.getSession(false);
            if (session != null) {
                request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", mpAuthentication.getAuthenticationExceptionIfExsits());
            }
            LOGGER.trace("Authentication sequence {} evaluated as failed.", mpAuthentication.getSequenceIdentifier());
        }
        filterChain.doFilter(request, response);
    }

    private ConnectionEnvironment createConnectionEnvironment(MidpointAuthentication mpAuthentication) {
        ConnectionEnvironment connectionEnvironment = ConnectionEnvironment.create(mpAuthentication.getAuthenticationChannel().getChannelId());
        connectionEnvironment.setSequenceIdentifier(mpAuthentication.getSequenceIdentifier());
        return connectionEnvironment;
    }
}
