/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

/**
 * @author semancik
 */
@Component("securityContextManager")
public class SecurityContextManagerImpl implements SecurityContextManager {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityContextManagerImpl.class);

    private MidPointPrincipalManager userProfileService = null;
    private final ThreadLocal<HttpConnectionInformation> connectionInformationThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<String> temporaryPrincipalOidThreadLocal = new ThreadLocal<>();

    @Override
    public MidPointPrincipalManager getUserProfileService() {
        return userProfileService;
    }

    @Override
    public void setUserProfileService(MidPointPrincipalManager userProfileService) {
        this.userProfileService = userProfileService;
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
        Authentication authentication =
                new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
        setupPreAuthenticatedSecurityContext(authentication);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(PrismObject<? extends FocusType> focus, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        setupPreAuthenticatedSecurityContext(
                focus,
                ProfileCompilerOptions.createNotCompileGuiAdminConfiguration()
                        .locateSecurityPolicy(false),
                result);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(
            PrismObject<? extends FocusType> focus, ProfileCompilerOptions options, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        MidPointPrincipal principal;
        if (userProfileService == null) {
            LOGGER.warn("No user profile service set up in SecurityEnforcer. "
                    + "This is OK in low-level tests but it is a serious problem in running system");
            principal = MidPointPrincipal.create(focus.asObjectable());
        } else {
            // For expression using runAsRef, task execution, etc., we don't need to support GUI config
            principal = userProfileService.getPrincipal(
                    focus,
                    options,
                    result);
        }
        principal.checkEnabled();
        setupPreAuthenticatedSecurityContext(principal);
    }

    private static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public <T> T runAs(
            @NotNull ResultAwareProducer<T> producer,
            @Nullable PrismObject<? extends FocusType> newPrincipalObject,
            boolean privileged,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        LOGGER.debug("Running {} as {} (privileged: {})", producer, newPrincipalObject, privileged);
        Authentication origAuthentication = getCurrentAuthentication();
        try {
            if (newPrincipalObject != null) {
                setupPreAuthenticatedSecurityContext(
                        newPrincipalObject,
                        ProfileCompilerOptions.createNotCompileGuiAdminConfiguration()
                                .locateSecurityPolicy(false)
                                .runAsRunner(true),
                        result);
            }
            if (privileged) {
                loginAsPrivileged(getCurrentAuthentication());
            }
            return producer.get(result);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(origAuthentication);
            LOGGER.debug("Finished running {} as {} (privileged: {})", producer, newPrincipalObject, privileged);
        }
    }

    private boolean isAnonymous(Authentication origAuthentication) {
        if (origAuthentication instanceof AuthenticationAnonymousChecker authenticationAnonymousChecker) {
            return authenticationAnonymousChecker.isAnonymous();
        }
        return origAuthentication instanceof AnonymousAuthenticationToken;
    }

    @Override
    public <T> T runPrivileged(@NotNull Producer<T> producer) {
        LOGGER.debug("Running {} as privileged", producer);

        Authentication origAuthentication = getCurrentAuthentication();
        LOGGER.trace("ORIG auth {}", origAuthentication);

        try {
            loginAsPrivileged(origAuthentication);
            return producer.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(origAuthentication);
            LOGGER.debug("Finished running {} as privileged", producer);
            LOGGER.trace("Security context after privileged operation: {}", SecurityContextHolder.getContext());
        }
    }

    /** Derives a "privileged" {@link Authentication} from the one provided (if there's any) and applies it. */
    private void loginAsPrivileged(@Nullable Authentication origAuthentication) {

        // Try to reuse the original identity as much as possible. All we need to is add AUTZ_ALL
        // to the list of authorities
        Authorization privilegedAuthorization = SecurityUtil.createPrivilegedAuthorization();

        if (origAuthentication != null) {
            Collection<GrantedAuthority> newAuthorities;
            Object newPrincipal;
            Object origPrincipal = origAuthentication.getPrincipal();
            if (isAnonymous(origAuthentication)) {
                newPrincipal = origPrincipal;
                newAuthorities = createNewAuthorities(origAuthentication, privilegedAuthorization);
            } else {
                LOGGER.trace("ORIG principal {} ({})", origPrincipal, origPrincipal != null ? origPrincipal.getClass() : null);
                if (origPrincipal instanceof MidPointPrincipal midPointPrincipal) {
                    MidPointPrincipal newMidPointPrincipal =
                            midPointPrincipal.cloneWithAdditionalAuthorizations(
                                    List.of(privilegedAuthorization), true);
                    newPrincipal = newMidPointPrincipal;
                    newAuthorities = List.copyOf(newMidPointPrincipal.getAuthorities());
                } else {
                    newPrincipal = null;
                    newAuthorities = createNewAuthorities(origAuthentication, privilegedAuthorization);
                }
            }

            PreAuthenticatedAuthenticationToken newAuthorization =
                    new PreAuthenticatedAuthenticationToken(newPrincipal, null, newAuthorities);

            LOGGER.trace("NEW auth {}", newAuthorization);
            SecurityContextHolder.getContext().setAuthentication(newAuthorization);
        } else {
            LOGGER.debug("No original authentication, do NOT setting any privileged security context");
        }
    }

    private Collection<GrantedAuthority> createNewAuthorities(
            Authentication origAuthentication, Authorization privilegedAuthorization) {
        Collection<GrantedAuthority> newAuthorities = new ArrayList<>(origAuthentication.getAuthorities());
        newAuthorities.add(privilegedAuthorization);
        return newAuthorities;
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
