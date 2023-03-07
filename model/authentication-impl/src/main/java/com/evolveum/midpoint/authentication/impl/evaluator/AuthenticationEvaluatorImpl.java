/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.evaluator;

import java.util.Collection;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.authentication.api.AutheticationFailedData;
import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.model.api.util.AuthenticationEvaluatorUtil;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
public abstract class AuthenticationEvaluatorImpl<C extends AbstractCredentialType, T extends AbstractAuthenticationContext>
        implements AuthenticationEvaluator<T>, MessageSourceAware {

    private static final Trace LOGGER = TraceManager.getTrace(AuthenticationEvaluatorImpl.class);

    @Autowired private Protector protector;
    @Autowired private Clock clock;
    @Autowired private ModelAuditRecorder securityHelper;
    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;
    private GuiProfiledPrincipalManager focusProfileService;
    protected MessageSourceAccessor messages;

    @Autowired
    public void setPrincipalManager(GuiProfiledPrincipalManager focusProfileService) {
        this.focusProfileService = focusProfileService;
    }

    @Override
    public void setMessageSource(@NotNull MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    protected abstract void checkEnteredCredentials(ConnectionEnvironment connEnv, T authCtx);
    protected abstract boolean supportsAuthzCheck();
    protected abstract C getCredential(CredentialsType credentials);
    protected abstract void validateCredentialNotNull(
            ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, C credential);
    protected abstract boolean passwordMatches(ConnectionEnvironment connEnv,
            @NotNull MidPointPrincipal principal, C passwordType, T authCtx);
    protected abstract CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy, T authnCtx) throws SchemaException;
    protected abstract boolean supportsActivation();

    @Override
    public UsernamePasswordAuthenticationToken authenticate(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {

        checkEnteredCredentials(connEnv, authnCtx);

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx, authnCtx.isSupportActivationByChannel());

        FocusType focusType = principal.getFocus();
        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        if (checkCredentials(principal, authnCtx, connEnv)) {
            if (!AuthenticationEvaluatorUtil.checkRequiredAssignmentTargets(focusType, authnCtx.getRequireAssignments())) {
                recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, credentialsPolicy, "does not contain required assignment");
                throw new InternalAuthenticationServiceException("web.security.flexAuth.invalid.required.assignment");
            }
        } else {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, credentialsPolicy, "password mismatch");
            throw new BadCredentialsException("web.security.provider.invalid.credentials");
        }

        checkAuthorizations(principal, connEnv, authnCtx);
        recordModuleAuthenticationSuccess(principal, connEnv, false);
        return new UsernamePasswordAuthenticationToken(principal, authnCtx.getEnteredCredential(), principal.getAuthorities());
    }

    @Override
    @NotNull
    public FocusType checkCredentials(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {

        checkEnteredCredentials(connEnv, authnCtx);

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx, false);

        FocusType focusType = principal.getFocus();
        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        if (!checkCredentials(principal, authnCtx, connEnv)) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, credentialsPolicy, "password mismatch");
            throw new BadCredentialsException("web.security.provider.invalid.credentials");
        }
        checkAuthorizations(principal, connEnv, authnCtx);
        recordModuleAuthenticationSuccess(principal, connEnv, false);
        return focusType;
    }

    private void checkAuthorizations(MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv, T authnCtx) {
        if (supportsAuthzCheck()) {
            // Authorizations
            if (hasNoneAuthorization(principal)) {
                recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, getCredentialsPolicy(principal, authnCtx), "no authorizations");
                throw new DisabledException("web.security.provider.access.denied");
            }
        }
    }

    private boolean checkCredentials(MidPointPrincipal principal, T authnCtx, ConnectionEnvironment connEnv) {

        FocusType focusType = principal.getFocus();
        CredentialsType credentials = focusType.getCredentials();
        if (credentials == null || getCredential(credentials) == null) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, getCredentialsPolicy(principal, authnCtx), "no credentials in user");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid.credentials");
        }

        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        // Lockout
        if (isLockedOut(getAuthenticationData(principal, connEnv), credentialsPolicy)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof MidpointAuthentication) {
                ((MidpointAuthentication) auth).setOverLockoutMaxAttempts(true);
            }
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, getCredentialsPolicy(principal, authnCtx), "password locked-out");
            throw new LockedException("web.security.provider.locked");
        }

        // Password age
        checkPasswordValidityAndAge(connEnv, principal, getCredential(credentials), credentialsPolicy);

        return passwordMatches(connEnv, principal, getCredential(credentials), authnCtx);
    }

    private CredentialPolicyType getCredentialsPolicy(MidPointPrincipal principal, T authnCtx) {
        SecurityPolicyType securityPolicy = principal.getApplicableSecurityPolicy();
        CredentialPolicyType credentialsPolicy;
        try {
            credentialsPolicy = getEffectiveCredentialPolicy(securityPolicy, authnCtx);
        } catch (SchemaException e) {
            // TODO how to properly hanlde the error????
            throw new AuthenticationServiceException("Bad config");
        }

        return credentialsPolicy;
    }

    /**
     * Special-purpose method used for Web Service authentication based on javax.security callbacks.
     *
     * In that case there is no reasonable way how to reuse existing methods. Therefore this method is NOT part of the
     * AuthenticationEvaluator interface. It is mostly a glue to make the old Java security code work.
     */
    public String getAndCheckUserPassword(ConnectionEnvironment connEnv, String username)
            throws AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {

        PreAuthenticationContext preAuthenticationContext = new PreAuthenticationContext(username, FocusType.class, null);
        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, (T) preAuthenticationContext, true);

        FocusType focusType = principal.getFocus();
        CredentialsType credentials = focusType.getCredentials();
        SecurityPolicyType securityPolicy = principal.getApplicableSecurityPolicy();
        PasswordCredentialsPolicyType passwordCredentialsPolicy = SecurityUtil.getEffectivePasswordCredentialsPolicy(securityPolicy);
        if (credentials == null) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, passwordCredentialsPolicy, "no credentials in user");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid.credentials");
        }
        PasswordType passwordType = credentials.getPassword();

        AuthenticationAttemptDataType authenticationAttemptData = getAuthenticationData(principal, connEnv);
        // Lockout
        if (isLockedOut(authenticationAttemptData, passwordCredentialsPolicy)) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, passwordCredentialsPolicy, "password locked-out");
            throw new LockedException("web.security.provider.locked");
        }

        // Password age
        checkPasswordValidityAndAge(connEnv, principal, (C) passwordType, passwordCredentialsPolicy);

        String password = getPassword(connEnv, principal, passwordType.getValue());

        // Authorizations
        if (hasNoneAuthorization(principal)) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, passwordCredentialsPolicy, "no authorizations");
            throw new InternalAuthenticationServiceException("web.security.provider.access.denied");
        }

        return password;
    }

    @Override
    public <AC extends AbstractAuthenticationContext> PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(ConnectionEnvironment connEnv, AC authnCtx) {

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx, authnCtx.isSupportActivationByChannel());

        // Authorizations
        if (hasNoneAuthorization(principal)) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "no authorizations");
            throw new InternalAuthenticationServiceException("web.security.provider.access.denied");
        }

        if (AuthenticationEvaluatorUtil.checkRequiredAssignmentTargets(principal.getFocus(), authnCtx.getRequireAssignments())) {
            PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
            recordModuleAuthenticationSuccess(principal, connEnv, true);
            return token;
        } else {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "not contains required assignment");
            throw new InternalAuthenticationServiceException("web.security.flexAuth.invalid.required.assignment");
        }
    }

    @NotNull
    protected <C extends AbstractAuthenticationContext> MidPointPrincipal getAndCheckPrincipal(ConnectionEnvironment connEnv, C authCtx, boolean supportActivationCheck) {
        ObjectQuery query = authCtx.createFocusQuery();
        String username = authCtx.getUsername();
        if (query == null) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "no username");
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }

        Class<? extends FocusType> clazz = authCtx.getPrincipalType();
        MidPointPrincipal principal;
        try {
            principal = focusProfileService.getPrincipal(query, clazz);
        } catch (ObjectNotFoundException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "no focus");
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        } catch (SchemaException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "schema error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (CommunicationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "communication error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (ConfigurationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "configuration error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (SecurityViolationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "security violation");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (ExpressionEvaluationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "expression error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        }

        if (principal == null) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "no focus");
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }

        if (supportActivationCheck && !principal.isEnabled()) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "focus disabled");
            throw new DisabledException("web.security.provider.disabled");
        }
        return principal;
    }

    protected boolean hasNoneAuthorization(MidPointPrincipal principal) {
        Collection<Authorization> authorizations = principal.getAuthorities();
        if (authorizations == null || authorizations.isEmpty()) {
            return true;
        }
        boolean exist = false;
        for (Authorization auth : authorizations) {
            if (auth.getAction() != null && !auth.getAction().isEmpty()) {
                exist = true;
            }
        }
        return !exist;
    }

    private <P extends CredentialPolicyType> void checkPasswordValidityAndAge(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, C credentials,
            P passwordCredentialsPolicy) {
        if (credentials == null) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, passwordCredentialsPolicy, "no stored credential value");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.credential.bad");
        }

        validateCredentialNotNull(connEnv, principal, credentials);

        if (passwordCredentialsPolicy == null) {
            return;
        }

        Duration maxAge = passwordCredentialsPolicy.getMaxAge();
        if (maxAge != null) {
            MetadataType credentialMetedata = credentials.getMetadata();
            XMLGregorianCalendar changeTimestamp = MiscSchemaUtil.getChangeTimestamp(credentialMetedata);
            if (changeTimestamp != null) {
                XMLGregorianCalendar passwordValidUntil = XmlTypeConverter.addDuration(changeTimestamp, maxAge);
                if (clock.isPast(passwordValidUntil)) {
                    recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, passwordCredentialsPolicy, "password expired");
                    throw new CredentialsExpiredException("web.security.provider.credential.expired");
                }
            }
        }
    }

    protected boolean decryptAndMatch(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString,
            String enteredPassword) {
        ProtectedStringType entered = new ProtectedStringType();
        entered.setClearValue(enteredPassword);
        try {
            return protector.compareCleartext(entered, protectedString);
        } catch (SchemaException | EncryptionException e) {
            // This is a serious error. It is not business as usual (e.g. wrong password or missing authorization).
            // This is either bug or serious misconfiguration (e.g. missing decryption key in keystore).
            // We do not want to just audit the failure. That would just log it on debug level.
            // But that would be too hard for system administrator to figure out what is going on - especially
            // if the administrator himself cannot log in. Therefore explicitly log those errors here.
            LOGGER.error("Error dealing with credentials of user \"{}\" credentials: {}", principal.getUsername(), e.getMessage());
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "error decrypting password: ");
            throw new AuthenticationServiceException("web.security.provider.unavailable", e);
        }
    }

    private String getPassword(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString) {
        String decryptedPassword;
        if (protectedString.getEncryptedDataType() != null) {
            try {
                decryptedPassword = protector.decryptString(protectedString);
            } catch (EncryptionException e) {
                recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "error decrypting password: ");
                throw new AuthenticationServiceException("web.security.provider.unavailable", e);
            }
        } else {
            LOGGER.warn("Authenticating user based on clear value. Please check objects, "
                    + "this should not happen. Protected string should be encrypted.");
            decryptedPassword = protectedString.getClearValue();
        }
        return decryptedPassword;
    }

    private boolean isLockedOut(AuthenticationAttemptDataType authenticationAttemptData, CredentialPolicyType credentialsPolicy) {
        return isOverFailedLockoutAttempts(authenticationAttemptData, credentialsPolicy) && !isLockoutExpired(authenticationAttemptData, credentialsPolicy);
    }

    private boolean isOverFailedLockoutAttempts(AuthenticationAttemptDataType authenticationAttemptData, CredentialPolicyType credentialsPolicy) {
        int failedLogins = getFailedLogins(authenticationAttemptData);
        return SecurityUtil.isOverFailedLockoutAttempts(failedLogins, credentialsPolicy);
    }

    private int getFailedLogins(AuthenticationAttemptDataType authenticationAttemptData) {
        Integer failedAttempts = authenticationAttemptData != null ? authenticationAttemptData.getFailedAttempts() : null;
        return failedAttempts == null ? 0 : failedAttempts.intValue();
    }

    private boolean isLockoutExpired(AuthenticationAttemptDataType authenticationAttemptData, CredentialPolicyType credentialsPolicy) {
        XMLGregorianCalendar lockoutExpiration = authenticationAttemptData.getLockoutExpirationTimestamp();
        if (lockoutExpiration != null) {
            return clock.isPast(lockoutExpiration);
        }

        Duration lockoutDuration = credentialsPolicy.getLockoutDuration();
        if (lockoutDuration == null) {
            return false;
        }

        XMLGregorianCalendar lockTimestamp = authenticationAttemptData.getLockoutTimestamp();
        if (lockTimestamp == null) {
            LoginEventType lastFailedLogin = getLastFailedLogin(authenticationAttemptData);
            if (lastFailedLogin == null) {
                return true;
            }
            lockTimestamp = lastFailedLogin.getTimestamp();
            if (lockTimestamp == null) {
                return true;
            }
        }
        XMLGregorianCalendar lockedUntilTimestamp = XmlTypeConverter.addDuration(lockTimestamp, lockoutDuration);
        return clock.isPast(lockedUntilTimestamp);
    }

    private LoginEventType getLastFailedLogin(AuthenticationAttemptDataType authenticationAttemptData) {
        return authenticationAttemptData.getLastFailedAuthentication();
    }

    protected void recordModuleAuthenticationSuccess(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            boolean audit) {
        authenticationRecorder.recordModuleAuthenticationAttemptSuccess(principal, connEnv);
    }

    protected void recordModuleAuthenticationFailure(String username, MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            CredentialPolicyType credentialsPolicy, String reason) {
        if (principal != null) {
            authenticationRecorder.recordModuleAuthenticationAttemptFailure(principal, credentialsPolicy, connEnv);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null) {
                moduleAuthentication.setFailureData(new AutheticationFailedData(reason, username));
            }
        }
    }

    protected void recordAuthenticationFailure(String username, ConnectionEnvironment connEnv, String reason) {
        securityHelper.auditLoginFailure(username, null, connEnv, reason);
    }

    public AuthenticationAttemptDataType getAuthenticationData(MidPointPrincipal principal, ConnectionEnvironment connectionEnvironment) {
        return AuthUtil.findAuthAttemptDataForModule(connectionEnvironment, principal);
    }
}
