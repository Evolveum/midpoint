/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.evaluator.context.NodeAuthenticationContext;
import com.evolveum.midpoint.authentication.impl.evaluator.NodeAuthenticationEvaluatorImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.ClusterAuthenticationToken;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
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
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List requireAssignment, AuthenticationChannel channel, Class focusType) {

        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_REST_URI);
        if (!(authentication instanceof ClusterAuthenticationToken)) {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        String enteredPassword = (String) authentication.getCredentials();
        NodeAuthenticationContext nodeCtx = new NodeAuthenticationContext(null, enteredUsername, enteredPassword);
        Authentication token = nodeAuthenticator.authenticate(connEnv, nodeCtx);

        LOGGER.debug("Node '{}' authenticated}", authentication.getPrincipal());
        token.setAuthenticated(true);
        return token;
    }
}
