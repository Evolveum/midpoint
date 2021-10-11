/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.provider;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.context.NonceAuthenticationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.MailNonceAuthenticationToken;
import com.evolveum.midpoint.web.security.module.authentication.MailNonceModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.List;

/**
 * @author skublik
 */

public class MailNonceProvider extends AbstractCredentialProvider<NonceAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(MailNonceProvider.class);

    @Autowired
    private transient AuthenticationEvaluator<NonceAuthenticationContext> nonceAuthenticationEvaluator;

    @Autowired
    private SecurityContextManager securityContextManager;

    @Autowired
    private TaskManager manager;

    @Autowired
    private ModelService modelService;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelInteractionService modelInteractionService;

    @Override
    protected AuthenticationEvaluator<NonceAuthenticationContext> getEvaluator() {
        return nonceAuthenticationEvaluator;
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }
        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnviroment(channel);
        try {
            Authentication token;
            if (authentication instanceof MailNonceAuthenticationToken) {
                String nonce = (String) authentication.getCredentials();
                NonceAuthenticationContext authContext = new NonceAuthenticationContext(enteredUsername,
                        focusType, nonce, getNoncePolicy(enteredUsername), requireAssignment);
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

    private NonceCredentialsPolicyType getNoncePolicy(String username) {
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("web.security.provider.invalid");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof MidpointAuthentication)) {
            LOGGER.debug("Actual authentication isn't MidpointAuthentication");
            return null;
        }
        ModuleAuthentication moduleAuth = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
        if (!(moduleAuth instanceof MailNonceModuleAuthentication)) {
            LOGGER.debug("Actual processing authentication module isn't MailNonceModuleAuthentication");
            return null;
        }
        String nameOfCredential = ((MailNonceModuleAuthentication) moduleAuth).getCredentialName();
        if (nameOfCredential == null) {
            LOGGER.debug("Name of credential in processing module is null");
            return null;
        }

        UserType user = SecurityUtils.searchUserPrivileged(username, securityContextManager, manager,
                modelService, prismContext);

        if (user == null) {
            throw new UsernameNotFoundException("web.security.provider.invalid");
        }

        SecurityPolicyType securityPolicy  = SecurityUtils.resolveSecurityPolicy(user.asPrismObject(), securityContextManager, manager, modelInteractionService);

        if (securityPolicy == null) {
            LOGGER.debug("Security policy from principal is null");
            return null;
        }
        if (securityPolicy.getCredentials() == null) {
            LOGGER.debug("Credentials in security policy from principal is null");
            return null;
        }
        if (securityPolicy.getCredentials().getNonce() == null) {
            LOGGER.debug("Nonce credentials in security policy from principal is null");
            return null;
        }
        for (NonceCredentialsPolicyType noncePolicy : securityPolicy.getCredentials().getNonce()) {
            if (noncePolicy != null && nameOfCredential.equals(noncePolicy.getName())) {
                return noncePolicy;
            }
        }
        LOGGER.debug("Couldn't find nonce credential by name " + nameOfCredential);
        return null;
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new MailNonceAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        if (MailNonceAuthenticationToken.class.equals(authentication)) {
            return true;
        }

        return false;
    }

    @Override
    public Class getTypeOfCredential() {
        return NonceCredentialsPolicyType.class;
    }

}
