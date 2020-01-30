/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.provider;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.model.api.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.SecurityQuestionsAuthenticationToken;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */

public class SecurityQuestionProvider extends AbstractCredentialProvider<SecurityQuestionsAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityQuestionProvider.class);

    @Autowired
    private transient AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> questionAuthenticationEvaluator;

    @Override
    protected AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> getEvaluator() {
        return questionAuthenticationEvaluator;
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment, AuthenticationChannel channel) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof MidPointUserProfilePrincipal) {
            return authentication;
        }
        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnviroment(channel);

        try {
            Authentication token;
            if (authentication instanceof SecurityQuestionsAuthenticationToken) {
                Map<String, String> answers = (Map<String, String>) authentication.getCredentials();
                SecurityQuestionsAuthenticationContext authContext = new SecurityQuestionsAuthenticationContext(enteredUsername, answers, requireAssignment);
                if (channel != null) {
                    authContext.setSupportActivationByChannel(channel.isSupportActivationByChannel());
                }
                token = getEvaluator().authenticate(connEnv, authContext);
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            MidPointPrincipal principal = (MidPointPrincipal)token.getPrincipal();

            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                    authentication.getClass().getSimpleName(), principal.getAuthorities());
            return token;

        } catch (AuthenticationException e) {
            LOGGER.info("Authentication failed for {}: {}", enteredUsername, e.getMessage());
            throw e;
        }
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new UsernamePasswordAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        if (SecurityQuestionsAuthenticationToken.class.equals(authentication)) {
            return true;
        }

        return false;
    }

    @Override
    public Class getTypeOfCredential() {
        return SecurityQuestionsCredentialsPolicyType.class;
    }

}
