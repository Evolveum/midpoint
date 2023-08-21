/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.evaluator.PreAuthenticatedEvaluatorImpl;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.authentication.api.evaluator.context.PasswordAuthenticationContext;

import com.evolveum.midpoint.authentication.api.evaluator.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collection;
import java.util.List;

/**
 * @author skublik
 */

public abstract class RemoteModuleProvider extends AbstractAuthenticationProvider {

    @Autowired
    @Qualifier("preAuthenticatedEvaluator")
    private PreAuthenticatedEvaluatorImpl<PreAuthenticationContext> authenticationEvaluator;

    @Autowired
    private ModelAuditRecorder auditProvider;

    public ModelAuditRecorder getAuditProvider() {
        return auditProvider;
    }

    @Override
    protected void writeAuthentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication,
            Authentication token) {
        Object principal = token.getPrincipal();
        if (principal instanceof GuiProfiledPrincipal) {
            mpAuthentication.setPrincipal(principal);
        }
        if (token instanceof PreAuthenticatedAuthenticationToken) {
            ((PreAuthenticatedAuthenticationToken) token).setDetails(originalAuthentication);
        }
        mpAuthentication.setToken(token);
    }

    protected PreAuthenticatedAuthenticationToken getPreAuthenticationToken(
            String enteredUsername,
            Class<? extends FocusType> focusType,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel){
        ConnectionEnvironment connEnv = createEnvironment(channel);
        PreAuthenticationContext authContext = new PreAuthenticationContext(enteredUsername, focusType, requireAssignment, channel);
        return authenticationEvaluator.authenticate(connEnv, authContext);
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection newAuthorities) {
        if (actualAuthentication instanceof PreAuthenticatedAuthenticationToken) {
            return new PreAuthenticatedAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }
}
