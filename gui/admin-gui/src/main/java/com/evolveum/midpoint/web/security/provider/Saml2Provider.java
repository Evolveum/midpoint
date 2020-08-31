/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.provider;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.saml2.attribute.Attribute;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

/**
 * @author skublik
 */

public class Saml2Provider extends MidPointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(Saml2Provider.class);

    @Autowired
    private AuthenticationEvaluator<PasswordAuthenticationContext> authenticationEvaluator;

    @Override
    protected AuthenticationEvaluator getEvaluator() {
        return authenticationEvaluator;
    }

    @Override
    protected void writeAutentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication,
            ModuleAuthentication moduleAuthentication, Authentication token) {
        Object principal = token.getPrincipal();
        if (principal != null && principal instanceof GuiProfiledPrincipal) {
            mpAuthentication.setPrincipal(principal);
        }

        moduleAuthentication.setAuthentication(originalAuthentication);
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment,
            AuthenticationChannel channel, Class focusType) throws AuthenticationException {
        ConnectionEnvironment connEnv = createEnvironment(channel);

        try {
            Authentication token;
            if (authentication instanceof DefaultSamlAuthentication) {
                DefaultSamlAuthentication samlAuthentication = (DefaultSamlAuthentication) authentication;
                Saml2ModuleAuthentication samlModule = (Saml2ModuleAuthentication) SecurityUtils.getProcessingModule(true);
                List<Attribute> attributes = ((DefaultSamlAuthentication) authentication).getAssertion().getAttributes();
                String enteredUsername = "";
                for (Attribute attribute : attributes) {
                    if (attribute != null
                            && ((attribute.getFriendlyName() != null && attribute.getFriendlyName().equals(samlModule.getNamesOfUsernameAttributes().get(samlAuthentication.getAssertingEntityId())))
                            || (attribute.getName() != null && attribute.getName().equals(samlModule.getNamesOfUsernameAttributes().get(samlAuthentication.getAssertingEntityId()))))) {
                        List<Object> values = attribute.getValues();
                        if (values == null) {
                            LOGGER.error("Saml attribute, which define username don't contains value");
                            throw new AuthenticationServiceException("web.security.auth.saml2.username.null");
                        }
                        if (values.size() != 1) {
                            LOGGER.error("Saml attribute, which define username contains more values {}", values);
                            throw new AuthenticationServiceException("web.security.auth.saml2.username.more.values");
                        }
                        enteredUsername = (String) values.iterator().next();
                    }
                }
                PreAuthenticationContext authContext = new PreAuthenticationContext(enteredUsername, focusType, requireAssignment);
                if (channel != null) {
                    authContext.setSupportActivationByChannel(channel.isSupportActivationByChannel());
                }
                token = authenticationEvaluator.authenticateUserPreAuthenticated(connEnv, authContext);
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                    authentication.getClass().getSimpleName(), principal.getAuthorities());
            return token;

        } catch (AuthenticationException e) {
            LOGGER.info("Authentication with saml module failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection newAuthorities) {
        if (actualAuthentication instanceof PreAuthenticatedAuthenticationToken) {
            return new PreAuthenticatedAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class authentication) {
        if (DefaultSamlAuthentication.class.equals(authentication)) {
            return true;
        }

        return false;
    }
}
