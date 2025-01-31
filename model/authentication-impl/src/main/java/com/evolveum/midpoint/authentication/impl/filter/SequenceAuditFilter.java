/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;

import com.evolveum.midpoint.util.exception.*;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;

import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.GenericFilterBean;
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
 * - focus/behavior/authentication
 * - audit
 */
public class SequenceAuditFilter extends GenericFilterBean {

    private static final Trace LOGGER = TraceManager.getTrace(SequenceAuditFilter.class);

    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;

    private GuiProfiledPrincipalManager focusProfileService;

    @Autowired
    public void setPrincipalManager(GuiProfiledPrincipalManager focusProfileService) {
        this.focusProfileService = focusProfileService;
    }

    private boolean recordOnEndOfChain = true;

    public SequenceAuditFilter() {
    }

    @VisibleForTesting
    public SequenceAuditFilter(FocusAuthenticationResultRecorder authenticationRecorder) {
        this.authenticationRecorder = authenticationRecorder;
    }

    public void setRecordOnEndOfChain(boolean recordOnEndOfChain) {
        this.recordOnEndOfChain = recordOnEndOfChain;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LOGGER.trace("Running SequenceAuditFilter");

        if (recordOnEndOfChain) {
            filterChain.doFilter(request, response);
        }

        Authentication authentication = SecurityUtil.getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            LOGGER.trace("No MidpointAuthentication present, continue with filter chain");
            if (!recordOnEndOfChain) {
                filterChain.doFilter(request, response);
            }
            return;
        }

        MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
        if (mpAuthentication.isAlreadyAudited()) {
            LOGGER.trace("Skipping auditing of authentication record, already audited.");
            if (!recordOnEndOfChain) {
                filterChain.doFilter(request, response);
            }
            return;
        }

        writeRecord((HttpServletRequest) request, mpAuthentication);

        if (!recordOnEndOfChain) {
            filterChain.doFilter(request, response);
        }

    }

    @VisibleForTesting
    public void writeRecord(HttpServletRequest request, MidpointAuthentication mpAuthentication) {
        MidPointPrincipal mpPrincipal = mpAuthentication.getPrincipal() instanceof MidPointPrincipal ? (MidPointPrincipal) mpAuthentication.getPrincipal() : null;
        boolean isAuthenticated = mpAuthentication.isAuthenticated();
        if (isAuthenticated) {
            authenticationRecorder.recordSequenceAuthenticationSuccess(mpPrincipal, createConnectionEnvironment(request, mpAuthentication));
            mpAuthentication.setAlreadyAudited(true);
            LOGGER.trace("Authentication sequence {} evaluated as successful.", mpAuthentication.getSequenceIdentifier());
        } else if (mpAuthentication.isFinished() && StringUtils.isNotEmpty(mpAuthentication.getUsername())) {
            authenticationRecorder.recordSequenceAuthenticationFailure(mpAuthentication.getUsername(), mpPrincipal, null,
                    mpAuthentication.getFailedReason(), createConnectionEnvironment(request, mpAuthentication));
            mpAuthentication.setAlreadyAudited(true);
            LOGGER.trace("Authentication sequence {} evaluated as failed.", mpAuthentication.getSequenceIdentifier());
        }
    }

    private ConnectionEnvironment createConnectionEnvironment(HttpServletRequest request, MidpointAuthentication mpAuthentication) {
        String sessionId = request != null ? request.getRequestedSessionId() : null;
        if (mpAuthentication.getSessionId() != null) {
            sessionId = mpAuthentication.getSessionId();
        }

        ConnectionEnvironment connectionEnvironment = ConnectionEnvironment.create(mpAuthentication.getAuthenticationChannel().getChannelId());
        connectionEnvironment.setSequenceIdentifier(mpAuthentication.getSequenceIdentifier());
        connectionEnvironment.setSessionIdOverride(sessionId);

        return connectionEnvironment;
    }
}
