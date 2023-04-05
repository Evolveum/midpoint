/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.evaluator.NodeAuthenticationEvaluatorImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.ClusterAuthenticationToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class ClusterProvider extends MidPointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterProvider.class);

    @Autowired
    private NodeAuthenticationEvaluatorImpl nodeAuthenticator;

    @Override
    protected AuthenticationEvaluator<PasswordAuthenticationContext> getEvaluator() {
        return null;
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment,
                                                    AuthenticationChannel channel, Class focusTyp) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }
        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);
        try {
            Authentication token;
            if (authentication instanceof ClusterAuthenticationToken) {
                String enteredPassword = (String) authentication.getCredentials();
                if (!nodeAuthenticator.authenticate(null, enteredUsername, enteredPassword, "node authentication")) {
                    throw new AuthenticationServiceException("web.security.flexAuth.cluster.auth.null");
                } else {
                    token = SecurityContextHolder.getContext().getAuthentication();
                }
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            LOGGER.debug("Node '{}' authenticated}", authentication.getPrincipal());
            token.setAuthenticated(true);
            return token;

        } catch (AuthenticationException e) {
            // This is something that the administrator should know about. Hence not DEBUG but INFO.
            LOGGER.info("Authentication failed for {}: {}", enteredUsername, e.getMessage());
            throw e;
        }
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection newAuthorities) {
        if (actualAuthentication instanceof ClusterAuthenticationToken) {
            return new ClusterAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    protected void writeAuthentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication, ModuleAuthenticationImpl moduleAuthentication, Authentication token) {
        mpAuthentication.setPrincipal(token.getPrincipal());
        mpAuthentication.setCredential(token.getCredentials());
        moduleAuthentication.setAuthentication(token);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ClusterAuthenticationToken.class.equals(authentication);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodeAuthenticator == null) ? 0 : nodeAuthenticator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
