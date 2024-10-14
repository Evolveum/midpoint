/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.filter.ldap.AuditedAuthenticationException;
import com.evolveum.midpoint.authentication.impl.filter.ldap.LdapDirContextAdapter;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.LdapAuthenticationToken;
import com.evolveum.midpoint.authentication.impl.module.authentication.LdapModuleAuthentication;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.model.api.util.AuthenticationEvaluatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MidPointLdapAuthenticationProvider extends MidpointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointLdapAuthenticationProvider.class);

    private final LdapAuthenticationProvider authenticatorProvider;

    @Autowired private ModelAuditRecorder auditProvider;
    @Autowired private Clock clock;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;

    public MidPointLdapAuthenticationProvider(LdapAuthenticator authenticator) {
        this.authenticatorProvider = createAuthenticatorProvider(authenticator);
    }

    public void setUserDetailsContextMapper(UserDetailsContextMapper userDetails){
        this.authenticatorProvider.setUserDetailsContextMapper(userDetails);
    }

    public LdapAuthenticationProvider getAuthenticatorProvider() {
        return authenticatorProvider;
    }

    private LdapAuthenticationProvider createAuthenticatorProvider(LdapAuthenticator authenticator){
        return new LdapAuthenticationProvider(authenticator) {
            @Override
            protected DirContextOperations doAuthentication(UsernamePasswordAuthenticationToken authentication) {
                    DirContextOperations originalDirContextOperations = super.doAuthentication(authentication);
                    return MidPointLdapAuthenticationProvider.this.doAuthentication(authentication, originalDirContextOperations);
            }

            @Override
            protected Authentication createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication,
                    UserDetails user) {
                Authentication authNCtx = super.createSuccessfulAuthentication(authentication, user);
                MidPointLdapAuthenticationProvider.this.createSuccessfulAuthentication(authentication, authNCtx);
                return authNCtx;
            }
        };
    }

    protected DirContextOperations doAuthentication(
            UsernamePasswordAuthenticationToken authentication, DirContextOperations originalDirContextOperations) {
        if (originalDirContextOperations instanceof DirContextAdapter) {
            Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
            if (actualAuthentication instanceof MidpointAuthentication mpAuthentication) {
                ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleOrThrowException();
                if (moduleAuthentication instanceof LdapModuleAuthentication){
                    //HACK because of NP in DirContextAdapter(DirContextAdapter master)
                    if (!originalDirContextOperations.isUpdateMode()) {
                        ((DirContextAdapter) originalDirContextOperations).setUpdateMode(true);
                        ((DirContextAdapter) originalDirContextOperations).setUpdateMode(false);
                    }
                    LdapDirContextAdapter mpDirContextAdapter = new LdapDirContextAdapter((DirContextAdapter)originalDirContextOperations);
                    mpDirContextAdapter.setNamingAttr(((LdapModuleAuthentication) moduleAuthentication).getNamingAttribute());

                    AuthenticationRequirements authRequirements = initAuthRequirements(actualAuthentication);

                    if (authRequirements.focusType != null) {
                        mpDirContextAdapter.setFocusType(authRequirements.focusType);
                    }

                    mpDirContextAdapter.setChannel(authRequirements.channel);
                    mpDirContextAdapter.setRequireAssignment(authRequirements.requireAssignment);
                    mpDirContextAdapter.setConnectionEnvironment(createEnvironment(authRequirements.channel));

                    return mpDirContextAdapter;
                }
            }
        }
        return originalDirContextOperations;
    }

    protected void createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication,
            Authentication authNCtx) {
        Object principal = authNCtx.getPrincipal();
        if (!(principal instanceof MidPointPrincipal midPointPrincipal)) {
            recordPasswordAuthenticationFailure(authentication.getName(), "not contains required assignment");
            throw new BadCredentialsException("LdapAuthentication.incorrect.value");
        }
        FocusType focusType = midPointPrincipal.getFocus();

        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication mpAuthentication) {
            List<ObjectReferenceType> requireAssignment = mpAuthentication.getSequence().getRequireAssignmentTarget();
            if (!AuthenticationEvaluatorUtil.checkRequiredAssignmentTargets(focusType, requireAssignment)) {
                recordPasswordAuthenticationFailure(midPointPrincipal.getUsername(), "does not contain required assignment");
                throw new DisabledException("web.security.flexAuth.invalid.required.assignment");
            }
        }

        recordPasswordAuthenticationSuccess(midPointPrincipal);
    }

    private RuntimeException processInternalAuthenticationException(InternalAuthenticationServiceException rootExeption, Throwable currentException) {
        if (currentException instanceof javax.naming.AuthenticationException) {
            String message = currentException.getMessage();
            if (message.contains("error code 49")) {
                // JNDI and Active Directory strike again
                return new BadCredentialsException("Invalid username and/or password.", rootExeption);
            }
        }
        Throwable cause = currentException.getCause();
        if (cause == null) {
            return rootExeption;
        } else {
            return processInternalAuthenticationException(rootExeption, cause);
        }
    }

    @Override
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List requireAssignment, AuthenticationChannel channel, Class focusType) throws AuthenticationException {

//        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'",
                enteredUsername);

        if (!(authentication instanceof LdapAuthenticationToken)) {
            LOGGER.debug("Unsupported authentication {}", authentication);
            recordPasswordAuthenticationFailure(authentication.getName(), "unavailable provider");
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        LdapAuthenticationToken authenticationForProvider;
        if (authentication.getPrincipal() == null
                || (authentication.getPrincipal() instanceof String principalName && StringUtils.isEmpty(principalName))) {
            authenticationForProvider = new LdapAuthenticationToken(enteredUsername, authentication.getCredentials(), authentication.getAuthorities());
        } else {
            authenticationForProvider = (LdapAuthenticationToken) authentication;
        }
        try {
            Authentication token = this.authenticatorProvider.authenticate(authenticationForProvider);
            MidPointPrincipal principal = (MidPointPrincipal)token.getPrincipal();

            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authenticationForProvider.getPrincipal(),
                    authenticationForProvider.getClass().getSimpleName(), principal.getAuthorities());
            return token;

        } catch (AuditedAuthenticationException e) {
            throw e.getCause();
        } catch (InternalAuthenticationServiceException e) {
            // This sometimes happens ... for unknown reasons the underlying libraries cannot
            // figure out correct exception. Which results to wrong error message (MID-4518)
            // So, be smart here and try to figure out correct error.
            recordPasswordAuthenticationFailure(authenticationForProvider.getName(), e.getMessage());
            throw processInternalAuthenticationException(e, e);

        } catch (IncorrectResultSizeDataAccessException e) {
            LOGGER.debug("Failed to authenticate user {}. Error: {}", authenticationForProvider.getName(), e.getMessage(), e);
            recordPasswordAuthenticationFailure(authenticationForProvider.getName(), "bad user");
            throw new BadCredentialsException("web.security.provider.invalid.credentials", e);
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to authenticate user {}. Error: {}", authenticationForProvider.getName(), e.getMessage(), e);
            recordPasswordAuthenticationFailure(authenticationForProvider.getName(), "bad credentials");
            throw e;
        }
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection newAuthorities) {
        if (actualAuthentication instanceof LdapAuthenticationToken) {
            return new LdapAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return LdapAuthenticationToken.class.equals(authentication);
    }

    @Override
    public int hashCode() {
        return this.authenticatorProvider.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.authenticatorProvider.equals(obj);
    }

    public void recordPasswordAuthenticationSuccess(@NotNull MidPointPrincipal principal) {
        String channel = getChannel();
        ConnectionEnvironment connectionEnvironment = createConnectEnvironment(channel);
        AuthenticationBehavioralDataType behavior = AuthUtil.getOrCreateBehavioralDataForSequence(principal.getFocus(), connectionEnvironment.getSequenceIdentifier());

        FocusType focusBefore = principal.getFocus().clone();
        Integer failedLogins = behavior.getFailedLogins();
        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
            behavior.setFailedLogins(0);
            successLoginAfterFail = true;
        }
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        HttpConnectionInformation connectionInfo = SecurityUtil.getCurrentConnectionInformation();
        if (connectionInfo != null) {
            event.setFrom(connectionInfo.getRemoteHostAddress());
        }

        behavior.setPreviousSuccessfulLogin(behavior.getLastSuccessfulLogin());
        behavior.setLastSuccessfulLogin(event);

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
        }
        recordAuthenticationSuccess(principal.getFocus(), channel);
    }

    private void recordAuthenticationSuccess(@NotNull FocusType focusType, @NotNull String channel) {
        auditProvider.auditLoginSuccess(focusType, createConnectEnvironment(channel));
    }

    public void recordPasswordAuthenticationFailure(String name, String reason) {

        FocusType focus = null;
        String channel = getChannel();
        MidPointPrincipal principal = null;
        try {
            // For recording audit log, we don't need to support GUI config
            principal = focusProfileService.getPrincipal(
                    name, getFocusType(), ProfileCompilerOptions.createOnlyPrincipalOption());
            focus = principal.getFocus();
        } catch (Exception e) {
            //ignore if non-exist
        }
        ConnectionEnvironment connectionEnvironment = createConnectEnvironment(channel);
        if (principal != null && focus != null) {
            AuthenticationBehavioralDataType behavior = AuthUtil.getOrCreateBehavioralDataForSequence(focus, connectionEnvironment.getSequenceIdentifier());

            FocusType focusBefore = focus.clone();
            Integer failedLogins = behavior.getFailedLogins();

            if (failedLogins == null) {
                failedLogins = 1;
            } else {
                failedLogins++;
            }

            behavior.setFailedLogins(failedLogins);

            LoginEventType event = new LoginEventType();
            event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
            HttpConnectionInformation connectionInfo = SecurityUtil.getCurrentConnectionInformation();
            if (connectionInfo != null) {
                event.setFrom(connectionInfo.getRemoteHostAddress());
            }

            behavior.setLastFailedLogin(event);
            if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(true)) {
                focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
            }
        }

        recordAuthenticationFailure(name, focus, channel, reason);
    }

    private Class<? extends FocusType> getFocusType() {
        Class<? extends FocusType> focusType = UserType.class;
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication mpAuthentication) {
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleOrThrowException();
            if (moduleAuthentication != null && moduleAuthentication.getFocusType() != null) {
                focusType = PrismContext.get().getSchemaRegistry().determineCompileTimeClass(moduleAuthentication.getFocusType());
            }
        }
        return focusType;
    }

    private void recordAuthenticationFailure(String name, FocusType focus, String channel, String reason) {
        auditProvider.auditLoginFailure(name, focus, createConnectEnvironment(channel), reason);
    }
}
