/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
@Component("securityContextManager")
public class SecurityContextManagerImpl implements SecurityContextManager {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityContextManagerImpl.class);

    private MidPointPrincipalManager userProfileService = null;
    private ThreadLocal<HttpConnectionInformation> connectionInformationThreadLocal = new ThreadLocal<>();
    private ThreadLocal<String> temporaryPrincipalOidThreadLocal = new ThreadLocal<>();

    @Override
    public MidPointPrincipalManager getUserProfileService() {
        return userProfileService;
    }

    @Override
    public void setUserProfileService(MidPointPrincipalManager userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Override
    public MidPointPrincipal getPrincipal() throws SecurityViolationException {
        return SecurityUtil.getPrincipal();
    }

    @Override
    public String getPrincipalOid() {
        String oid = SecurityUtil.getPrincipalOidIfAuthenticated();
        if (oid != null) {
            return oid;
        } else {
            return temporaryPrincipalOidThreadLocal.get();
        }
    }

    @Override
    public void setTemporaryPrincipalOid(String value) {
        temporaryPrincipalOidThreadLocal.set(value);
    }

    @Override
    public void clearTemporaryPrincipalOid() {
        temporaryPrincipalOidThreadLocal.remove();
    }

    @Override
    public boolean isAuthenticated() {
        return SecurityUtil.isAuthenticated();
    }

    @Override
    public Authentication getAuthentication() {
        return SecurityUtil.getAuthentication();
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(MidPointPrincipal principal) {
        // Make sure that constructor with authorities is used. Otherwise the context will not be authenticated.
        Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
        setupPreAuthenticatedSecurityContext(authentication);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(PrismObject<? extends FocusType> focus) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal;
        if (userProfileService == null) {
            LOGGER.warn("No user profile service set up in SecurityEnforcer. "
                    + "This is OK in low-level tests but it is a serious problem in running system");
            principal = new MidPointPrincipal(focus.asObjectable());
        } else {
            principal = userProfileService.getPrincipal(focus);
        }
        setupPreAuthenticatedSecurityContext(principal);
    }

    @Override
    public <T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        LOGGER.debug("Running {} as {}", producer, user);
        Authentication origAuthentication = SecurityContextHolder.getContext().getAuthentication();
        setupPreAuthenticatedSecurityContext(user);
        try {
            return producer.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(origAuthentication);
            LOGGER.debug("Finished running {} as {}", producer, user);
        }
    }

    private boolean isAnonymous(Authentication origAuthentication) {
        if (origAuthentication instanceof AuthenticationAnonymousChecker) {
            return ((AuthenticationAnonymousChecker)origAuthentication).isAnonymous();
        }
        if (origAuthentication instanceof AnonymousAuthenticationToken) {
            return true;
        }
        return false;
    }

    @Override
    public <T> T runPrivileged(Producer<T> producer) {
        LOGGER.debug("Running {} as privileged", producer);
        Authentication origAuthentication = SecurityContextHolder.getContext().getAuthentication();
        LOGGER.trace("ORIG auth {}", origAuthentication);

        // Try to reuse the original identity as much as possible. All we need to is add AUTZ_ALL
        // to the list of authorities
        Authorization privilegedAuthorization = createPrivilegedAuthorization();
        Object newPrincipal = null;

        if (origAuthentication != null) {
            Object origPrincipal = origAuthentication.getPrincipal();
            if (isAnonymous(origAuthentication)) {
                newPrincipal = origPrincipal;
            } else {
                LOGGER.trace("ORIG principal {} ({})", origPrincipal, origPrincipal != null ? origPrincipal.getClass() : null);
                if (origPrincipal != null) {
                    if (origPrincipal instanceof MidPointPrincipal) {
                        MidPointPrincipal newMidPointPrincipal = ((MidPointPrincipal)origPrincipal).clone();
                        newMidPointPrincipal.getAuthorities().add(privilegedAuthorization);
                        newPrincipal = newMidPointPrincipal;
                    }
                }
            }

            Collection<GrantedAuthority> newAuthorities = new ArrayList<>();
            newAuthorities.addAll(origAuthentication.getAuthorities());
            newAuthorities.add(privilegedAuthorization);
            PreAuthenticatedAuthenticationToken newAuthorization = new PreAuthenticatedAuthenticationToken(newPrincipal, null, newAuthorities);

            LOGGER.trace("NEW auth {}", newAuthorization);
            SecurityContextHolder.getContext().setAuthentication(newAuthorization);
        } else {
            LOGGER.debug("No original authentication, do NOT setting any privileged security context");
        }


        try {
            return producer.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(origAuthentication);
            LOGGER.debug("Finished running {} as privileged", producer);
            LOGGER.trace("Security context after privileged operation: {}", SecurityContextHolder.getContext());
        }

    }

    private Authorization createPrivilegedAuthorization() {
        AuthorizationType authorizationBean = new AuthorizationType();
        authorizationBean.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
        return new Authorization(authorizationBean);
    }

    @Override
    public void storeConnectionInformation(HttpConnectionInformation value) {
        connectionInformationThreadLocal.set(value);
    }

    @Override
    public HttpConnectionInformation getStoredConnectionInformation() {
        return connectionInformationThreadLocal.get();
    }


}
