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
import com.evolveum.midpoint.authentication.impl.module.authentication.token.ClusterAuthenticationToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class ClusterProvider extends MidpointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterProvider.class);

    @Autowired private NodeAuthenticationEvaluatorImpl nodeAuthenticator;


    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection newAuthorities) {
        if (actualAuthentication instanceof ClusterAuthenticationToken) {
            return new ClusterAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    //TODO changed to recordAuthentication on midPointAuthentication
    protected void writeAuthentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication, Authentication token) {
        mpAuthentication.setPrincipal(token.getPrincipal());
        mpAuthentication.setCredential(token.getCredentials());
        mpAuthentication.setToken(token);

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

    @Override
    protected Authentication doAuthenticate(Authentication authentication, List requireAssignment, AuthenticationChannel channel, Class focusType) {
        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

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
    }
}
