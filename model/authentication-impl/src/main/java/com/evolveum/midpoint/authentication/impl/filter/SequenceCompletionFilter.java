/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;

import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.filter.OncePerRequestFilter;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationSequenceListener;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This filter has to run after login filter was run and the success and failure handlers
 * finished their evaluation. In those handlers, module state is set which is crucial for
 * correct evaluation.
 *
 * The aim of SequenceCompletionFilter is to check the overall authentication, authentication for
 * the whole sequence. While partial (module) authentication results are evaluated and
 * recorded by corresponding provider (plus evaluator), the overall status if the whole
 * sequence authentication was successful or not is handled here. The sequence is completed
 * only once, therefore the isAlreadyAudited() check.
 *
 * The result is recorded to two places:
 *
 * - focus/behavior/authentication
 * - audit
 *
 * The outcome is also passed to the {@link AuthenticationSequenceListener}s among the providers of the sequence modules.
 * */
public class SequenceCompletionFilter extends GenericFilterBean {

    private static final Trace LOGGER = TraceManager.getTrace(SequenceCompletionFilter.class);

    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;

    private GuiProfiledPrincipalManager focusProfileService;

    @Autowired
    public void setPrincipalManager(GuiProfiledPrincipalManager focusProfileService) {
        this.focusProfileService = focusProfileService;
    }

    private boolean recordOnEndOfChain = true;

    public SequenceCompletionFilter() {
    }

    @VisibleForTesting
    public SequenceCompletionFilter(FocusAuthenticationResultRecorder authenticationRecorder) {
        this.authenticationRecorder = authenticationRecorder;
    }

    public void setRecordOnEndOfChain(boolean recordOnEndOfChain) {
        this.recordOnEndOfChain = recordOnEndOfChain;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LOGGER.trace("Running SequenceCompletionFilter");

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
            notifyListeners(mpAuthentication, true);
            mpAuthentication.setAlreadyAudited(true);
            LOGGER.trace("Authentication sequence {} evaluated as successful.", mpAuthentication.getSequenceIdentifier());
        } else if (mpAuthentication.isFinished() && StringUtils.isNotEmpty(mpAuthentication.getUsername())) {
            authenticationRecorder.recordSequenceAuthenticationFailure(mpAuthentication.getUsername(), mpPrincipal, null,
                    mpAuthentication.getFailedReason(), createConnectionEnvironment(request, mpAuthentication));
            notifyListeners(mpAuthentication, false);
            mpAuthentication.setAlreadyAudited(true);
            LOGGER.trace("Authentication sequence {} evaluated as failed.", mpAuthentication.getSequenceIdentifier());
        }
    }

    private void notifyListeners(MidpointAuthentication mpAuthentication, boolean succeeded) {
        for (AuthModule<?> authModule : mpAuthentication.getAuthModules()) {
            List<AuthenticationProvider> providers = authModule.getAuthenticationProviders();
            if (providers == null || providers.isEmpty()) {
                continue;
            }

            ModuleAuthentication moduleAuthentication = findModuleAuthentication(mpAuthentication, authModule.getModuleIdentifier());
            for (AuthenticationProvider provider : providers) {
                if (!(provider instanceof AuthenticationSequenceListener listener)) {
                    continue;
                }

                if (succeeded) {
                    listener.sequenceSucceeded(mpAuthentication, moduleAuthentication);
                } else {
                    listener.sequenceFailed(mpAuthentication, moduleAuthentication);
                }
            }
        }
    }

    private ModuleAuthentication findModuleAuthentication(MidpointAuthentication mpAuthentication, String moduleIdentifier) {
        return mpAuthentication.getAuthentications().stream()
                .filter(module -> moduleIdentifier != null && moduleIdentifier.equals(module.getModuleIdentifier()))
                .findFirst()
                .orElse(null);
    }

    private ConnectionEnvironment createConnectionEnvironment(HttpServletRequest request, MidpointAuthentication mpAuthentication) {
        String sessionId = SecurityUtil.getOrCreateAuditSessionId(request);
        if (mpAuthentication.getSessionId() != null) {
            sessionId = mpAuthentication.getSessionId();
        }

        ConnectionEnvironment connectionEnvironment = ConnectionEnvironment.create(mpAuthentication.getAuthenticationChannel().getChannelId());
        connectionEnvironment.setSequenceIdentifier(mpAuthentication.getSequenceIdentifier());
        connectionEnvironment.setSessionIdOverride(sessionId);

        return connectionEnvironment;
    }
}
