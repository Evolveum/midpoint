/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;

import com.evolveum.midpoint.authentication.impl.module.authentication.MailNonceModuleAuthenticationImpl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.authentication.api.NonceAuthenticationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
public class MailNonceProvider extends AbstractCredentialProvider<NonceAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(MailNonceProvider.class);

    @Autowired
    private AuthenticationEvaluator<NonceAuthenticationContext> nonceAuthenticationEvaluator;

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
    protected Authentication doAuthenticate(
            Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        Authentication token;
        if (authentication instanceof MailNonceAuthenticationToken) {
            String nonce = (String) authentication.getCredentials();
            NonceAuthenticationContext authContext = new NonceAuthenticationContext(enteredUsername,
                    focusType, nonce, getNoncePolicy(enteredUsername), requireAssignment, channel);
            token = getEvaluator().authenticate(connEnv, authContext);
        } else {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;

    }

    private NonceCredentialsPolicyType getNoncePolicy(String username) {
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }

        if (illegalAuthentication()){
            return null;
        }

        UserType user = AuthSequenceUtil.searchUserPrivileged(username, securityContextManager, manager,
                modelService, prismContext);
        if (user == null) {
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }

        SecurityPolicyType securityPolicy = AuthSequenceUtil.resolveSecurityPolicy(user.asPrismObject(), securityContextManager, manager, modelInteractionService);
        if (illegalPolicy(securityPolicy)){
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        ModuleAuthentication moduleAuth = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
        String nameOfCredential = ((MailNonceModuleAuthenticationImpl) moduleAuth).getCredentialName();
        for (NonceCredentialsPolicyType noncePolicy : securityPolicy.getCredentials().getNonce()) {
            if (noncePolicy != null && nameOfCredential.equals(noncePolicy.getName())) {
                return noncePolicy;
            }
        }
        LOGGER.debug("Couldn't find nonce credential by name " + nameOfCredential);
        return null;
    }

    private boolean illegalPolicy(SecurityPolicyType securityPolicy) {
        if (securityPolicy == null) {
            LOGGER.debug("Security policy from principal is null");
            return true;
        }
        if (securityPolicy.getCredentials() == null) {
            LOGGER.debug("Credentials in security policy from principal is null");
            return true;
        }
        if (securityPolicy.getCredentials().getNonce() == null) {
            LOGGER.debug("Nonce credentials in security policy from principal is null");
            return true;
        }
        return false;
    }

    private boolean illegalAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof MidpointAuthentication)) {
            LOGGER.debug("Actual authentication isn't MidpointAuthentication");
            return true;
        }
        ModuleAuthentication moduleAuth = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
        if (!(moduleAuth instanceof MailNonceModuleAuthenticationImpl)) {
            LOGGER.debug("Actual processing authentication module isn't MailNonceModuleAuthentication");
            return true;
        }
        String nameOfCredential = ((MailNonceModuleAuthenticationImpl) moduleAuth).getCredentialName();
        if (nameOfCredential == null) {
            LOGGER.debug("Name of credential in processing module is null");
            return true;
        }
        return false;
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
        return MailNonceAuthenticationToken.class.equals(authentication);
    }

    @Override
    public Class getTypeOfCredential() {
        return NonceCredentialsPolicyType.class;
    }

}
