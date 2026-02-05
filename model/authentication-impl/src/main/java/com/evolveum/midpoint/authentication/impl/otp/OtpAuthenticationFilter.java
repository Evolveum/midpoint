/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.filter.MidpointAnonymousAuthenticationFilter;

import com.evolveum.midpoint.authentication.impl.filter.MidpointUsernamePasswordAuthenticationFilter;

import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class OtpAuthenticationFilter extends MidpointUsernamePasswordAuthenticationFilter {

    private static final String OTP_VERIFIED = "OTP_VERIFIED";

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (isPostOnly() && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        String username = getIdentifiedUsername();
        if (username == null) {
            throw new AuthenticationServiceException("Authentication failed: username not available.");
        }

        UsernamePasswordAuthenticationToken authRequest = new OtpAuthenticationToken(username, null);

        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private String getIdentifiedUsername() {
        MidpointAuthentication midpointAuthentication = AuthUtil.getMidpointAuthentication();
        Object principal = midpointAuthentication.getPrincipal();
        if (!(principal instanceof MidPointPrincipal)) {
            return "";
        }

        FocusType focus = ((MidPointPrincipal) principal).getFocus();
        return focus.getName().getNorm();

    }

    //    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        return !("/otpVerify".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod()));
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//            HttpServletResponse response,
//            FilterChain filterChain) throws ServletException, IOException {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthenticated");
//            return;
//        }
//
////        String username = authentication.getName();
////        String code = request.getParameter("code");
////        if (code == null || !otpService.verifyCode(secret, code)) {
////            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid TOTP");
////            return;
////        }
////
////        // mark session as TOTP verified
////        request.getSession(true).setAttribute(OTP_VERIFIED, Boolean.TRUE);
////
////        // redirect to desired protected resource (or respond 200)
////        response.sendRedirect("/");
//    }
}
