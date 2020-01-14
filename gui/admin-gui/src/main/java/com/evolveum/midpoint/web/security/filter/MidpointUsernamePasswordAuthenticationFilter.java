/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

public class MidpointUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private boolean postOnly = true;

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

//        if (logger.isDebugEnabled()) {
//            logger.debug("Authentication request failed: " + failed.toString(), failed);
//            logger.debug("Updated SecurityContextHolder to contain null Authentication");
//            logger.debug("Delegating to authentication failure handler " + failureHandler);
//        }

        getRememberMeServices().loginFail(request, response);

        getFailureHandler().onAuthenticationFailure(request, response, failed);
    }

    @Override
    public void setPostOnly(boolean postOnly) {
        this.postOnly = postOnly;
        super.setPostOnly(postOnly);
    }

    public boolean isPostOnly() {
        return postOnly;
    }
}
