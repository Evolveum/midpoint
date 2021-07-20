/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Component("samlAuthenticationEvaluator")
public class SamlAuthenticationEvaluatorImpl extends PasswordAuthenticationEvaluatorImpl {

    public static final String SAML_LOGIN_PATH = "/saml2/select";

    @Override
    public PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(ConnectionEnvironment connEnv, AbstractAuthenticationContext authnCtx) {

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx.getUsername(),
                authnCtx.getPrincipalType(), authnCtx.isSupportActivationByChannel());

        String path = ((ServletRequestAttributes) Objects.requireNonNull(
                RequestContextHolder.getRequestAttributes()))
                .getRequest().getServletPath();

        if (!hasAnyAuthorization(principal) && SAML_LOGIN_PATH.equals(path)) {
            PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, null, authnCtx.getPrincipalType(), true);
            return token;
        }
        return super.authenticateUserPreAuthenticated(connEnv, authnCtx);
    }
}
