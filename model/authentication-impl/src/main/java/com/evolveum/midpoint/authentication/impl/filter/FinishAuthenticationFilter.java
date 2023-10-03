/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter has to run after login filter was run and the success and failure handlers
 * finished their evaluation. In those handlers, module state is set which is crucial for
 * correct evaluation.
 *
 * The aim of FinishAuthenticationFilter is to check the overall authentication,
 * when authentication is success then class call gui compiler
 * for compilation of admin gui configuration for authenticated principal.
 * It should be recoded only once per sequence, therefore the isAlreadyRecorded() check.
 */
public class FinishAuthenticationFilter extends OncePerRequestFilter {

    private static final Trace LOGGER = TraceManager.getTrace(FinishAuthenticationFilter.class);

    private GuiProfiledPrincipalManager focusProfileService;

    @Autowired
    public void setPrincipalManager(GuiProfiledPrincipalManager focusProfileService) {
        this.focusProfileService = focusProfileService;
    }

    @Autowired(required = false)
    private SessionRegistry sessionRegistry;

    public FinishAuthenticationFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        LOGGER.trace("Running FinishAuthenticationFilter");

        Authentication authentication = SecurityUtil.getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            LOGGER.trace("No MidpointAuthentication present, continue with filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        if (!mpAuthentication.isAuthenticated()) {
            LOGGER.trace("Skipping compile principal profile, failed authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        if (!mpAuthentication.isFinished()) {
            LOGGER.trace("Skipping compile principal profile, unfinished authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        if (mpAuthentication.isAlreadyCompiledGui()) {
            LOGGER.trace("Skipping compile principal profile, already was compiled.");
            filterChain.doFilter(request, response);
            return;
        }

        if (!(mpAuthentication.getPrincipal() instanceof GuiProfiledPrincipal)) {
            LOGGER.trace("Skipping compile principal profile, because couldn't find GuiProfiledPrincipal.");
            filterChain.doFilter(request, response);
            return;
        }

        compileGuiProfile(mpAuthentication);

        filterChain.doFilter(request, response);
    }

    private void compileGuiProfile(MidpointAuthentication mpAuthentication) {
        AuthenticationChannel channel = mpAuthentication.getAuthenticationChannel();
        boolean supportGuiConfig = channel == null || channel.isSupportGuiConfigByChannel();
        MidPointPrincipal principal = (MidPointPrincipal) mpAuthentication.getPrincipal();

        if (!supportGuiConfig) {
            return;
        }

        focusProfileService.refreshCompiledProfile(
                (GuiProfiledPrincipal) principal,
                ProfileCompilerOptions.create()
                        .collectAuthorization(true)
                        .compileGuiAdminConfiguration(supportGuiConfig)
                        .locateSecurityPolicy(supportGuiConfig)
                        .tryReusingSecurityPolicy(true)
                        .terminateDisabledUserSession(false));
        mpAuthentication.setAlreadyCompiledGui(true);
    }
}
