/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.FocusVerificationToken;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.FocusIdentificationAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FocusIdentificationProvider extends MidPointAbstractAuthenticationProvider<PasswordAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusIdentificationProvider.class);

    @Autowired
    private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;

    @Override
    protected AuthenticationEvaluator<PasswordAuthenticationContext> getEvaluator() {
        return passwordAuthenticationEvaluator;
    }

    @Override
    public Authentication authenticate(Authentication originalAuthentication) throws AuthenticationException {
        return super.authenticate(originalAuthentication);
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }
//        if (!(authentication.getPrincipal() instanceof MidPointPrincipal)) {
//            return authentication;
//        }
//        authentication.getPrincipal()
//        String enteredUsername = ((MidPointPrincipal) authentication.getPrincipal()).getUsername();
//        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);
//        return new UsernamePasswordAuthenticationToken("administrator", "5ecr3t");

        try {
            Authentication token;
            if (authentication instanceof FocusVerificationToken) {
                Map<ItemPath, String> attrValuesMap = (Map<ItemPath, String>) authentication.getDetails();
                if (attrValuesMap == null || attrValuesMap.isEmpty()) {
                    LOGGER.error("Unsupported authentication {}", authentication);
                    throw new AuthenticationServiceException("web.security.provider.unavailable");
                }
                FocusIdentificationAuthenticationContext ctx = new FocusIdentificationAuthenticationContext(attrValuesMap, focusType, null);
                token = getEvaluator().authenticateUserPreAuthenticated(connEnv, ctx);
                UsernamePasswordAuthenticationToken pwdToken = new UsernamePasswordAuthenticationToken(token.getPrincipal(),token.getCredentials());
                pwdToken.setAuthenticated(false);
                return pwdToken;

            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }
        } catch (AuthenticationException e) {
            LOGGER.info("Authentication failed for {}: {}", "TODO", e.getMessage());
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
        return FocusVerificationToken.class.equals(authentication);
    }

//    @Override
//    public Class<? extends CredentialPolicyType> getTypeOfCredential() {
//        return null; //todo
//    }

}
