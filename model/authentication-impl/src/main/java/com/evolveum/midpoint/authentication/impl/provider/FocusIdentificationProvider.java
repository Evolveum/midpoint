/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.FocusVerificationToken;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.FocusIdentificationAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
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

        ConnectionEnvironment connEnv = createEnvironment(channel, authentication);

        try {
            Authentication token;
            if (authentication instanceof FocusVerificationToken) {
                Map<ItemPath, String> attrValuesMap = (Map<ItemPath, String>) authentication.getDetails();
                if (attrValuesMap == null || attrValuesMap.isEmpty()) {
                    // E.g. no user name or other required property provided when resetting the password.
                    // Hence DEBUG, not ERROR, and BadCredentialsException, not AuthenticationServiceException.
                    LOGGER.debug("No details provided: {}", authentication);
                    throw new BadCredentialsException(AuthUtil.generateBadCredentialsMessageKey(authentication));
                }
                ModuleAuthentication moduleAuthentication = AuthUtil.getProcessingModule();
                List<ModuleItemConfigurationType> itemsConfig = null;
                if (moduleAuthentication instanceof FocusIdentificationModuleAuthentication focusModuleAuthentication) {
                    itemsConfig = focusModuleAuthentication.getModuleConfiguration();
                }
                FocusIdentificationAuthenticationContext ctx = new FocusIdentificationAuthenticationContext(attrValuesMap, focusType, itemsConfig, null);
                token = getEvaluator().authenticateUserPreAuthenticated(connEnv, ctx);
                UsernamePasswordAuthenticationToken pwdToken = new UsernamePasswordAuthenticationToken(token.getPrincipal(), token.getCredentials());
                pwdToken.setAuthenticated(false);
                return pwdToken;

            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }
        } catch (AuthenticationException e) {
            LOGGER.debug("Authentication failed for {}: {}", authentication, e.getMessage());
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

}
