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

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.model.api.util.AuthenticationEvaluatorUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public abstract class AuthenticationEvaluatorImpl<C extends AbstractCredentialType, T extends AbstractAuthenticationContext>
        implements AuthenticationEvaluator<T>, MessageSourceAware {

    private static final Trace LOGGER = TraceManager.getTrace(AuthenticationEvaluatorImpl.class);

    @Autowired private Protector protector;
    @Autowired private Clock clock;
    @Autowired private ModelAuditRecorder securityHelper;

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

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx.getUsername(),
                authnCtx.getPrincipalType(), authnCtx.isSupportActivationByChannel());

        FocusType focusType = principal.getFocus();
        CredentialsType credentials = focusType.getCredentials();
        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        if (checkCredentials(principal, authnCtx, connEnv)) {
            if (!AuthenticationEvaluatorUtil.checkRequiredAssignmentTargets(focusType, authnCtx.getRequireAssignments())) {
                recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "does not contain required assignment", authnCtx.getPrincipalType(), false);
                recordPasswordAuthenticationFailure(principal, connEnv, getCredential(credentials), credentialsPolicy, "does not contain required assignment", false);
                throw new InternalAuthenticationServiceException("web.security.flexAuth.invalid.required.assignment");
            }
        } else {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "password mismatch", authnCtx.getPrincipalType(), false);
            recordPasswordAuthenticationFailure(principal, connEnv, getCredential(credentials), credentialsPolicy, "password mismatch", false);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof MidpointAuthentication && !((MidpointAuthentication) authentication).canContinueAfterCredentialsCheckFail()) {
//                if (((MidpointAuthentication) authentication).wrongConfiguredSufficientModuleExists()) {
//                    throw new AccessDeniedException("Wrong authentication modules configuration. Please, contact the system administrator");
//                } else {
                    throw new BadCredentialsException("web.security.provider.invalid.credentials");
//                }
            }
        }

        checkAuthorizations(principal, connEnv, authnCtx);
        recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, null, authnCtx.getPrincipalType(), true);
        recordPasswordAuthenticationSuccess(principal, connEnv, getCredential(credentials), false);
        return new UsernamePasswordAuthenticationToken(principal, authnCtx.getEnteredCredential(), principal.getAuthorities());
    }

    @Override
    @NotNull
    public FocusType checkCredentials(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {

        checkEnteredCredentials(connEnv, authnCtx);

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx.getUsername(), authnCtx.getPrincipalType(), false);

        FocusType focusType = principal.getFocus();
        CredentialsType credentials = focusType.getCredentials();
        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        if (!checkCredentials(principal, authnCtx, connEnv)) {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "password mismatch", authnCtx.getPrincipalType(), false);
            recordPasswordAuthenticationFailure(principal, connEnv, getCredential(credentials), credentialsPolicy, "password mismatch", false);
            throw new BadCredentialsException("web.security.provider.invalid.credentials");
        }
        checkAuthorizations(principal, connEnv, authnCtx);
        recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "password mismatch", authnCtx.getPrincipalType(), true);
        recordPasswordAuthenticationSuccess(principal, connEnv, getCredential(credentials), false);
        return focusType;
    }

    private void checkAuthorizations(MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv, AbstractAuthenticationContext authnCtx) {
        if (supportsAuthzCheck()) {
            // Authorizations
            if (hasNoneAuthorization(principal)) {
                recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "no authorizations", authnCtx.getPrincipalType(),false);
                throw new DisabledException("web.security.provider.access.denied");
            }
        }
    }

    private boolean checkCredentials(MidPointPrincipal principal, T authnCtx, ConnectionEnvironment connEnv) {

        FocusType focusType = principal.getFocus();
        CredentialsType credentials = focusType.getCredentials();
        if (credentials == null || getCredential(credentials) == null) {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "no credentials in user", authnCtx.getPrincipalType(), false);
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid.credentials");
        }

        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        // Lockout
        if (isLockedOut(getCredential(credentials), credentialsPolicy)) {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "password locked-out", authnCtx.getPrincipalType(), false);
            throw new LockedException("web.security.provider.locked");
        }

        // Password age
        checkPasswordValidityAndAge(connEnv, principal, getCredential(credentials), credentialsPolicy);

        return passwordMatches(connEnv, principal, getCredential(credentials), authnCtx);
    }

    private CredentialPolicyType getCredentialsPolicy(MidPointPrincipal principal, T authnCtx){
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

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, username, FocusType.class, true);

        FocusType focusType = principal.getFocus();
        CredentialsType credentials = focusType.getCredentials();
        if (credentials == null) {
            recordAuthenticationBehavior(username, null, connEnv, "no credentials in user", FocusType.class, false);
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid.credentials");
        }
        PasswordType passwordType = credentials.getPassword();
        SecurityPolicyType securityPolicy = principal.getApplicableSecurityPolicy();
        PasswordCredentialsPolicyType passwordCredentialsPolicy = SecurityUtil.getEffectivePasswordCredentialsPolicy(securityPolicy);

        // Lockout
        if (isLockedOut(passwordType, passwordCredentialsPolicy)) {
            recordAuthenticationBehavior(username, null, connEnv, "password locked-out", FocusType.class,false);
            throw new LockedException("web.security.provider.locked");
        }

        // Password age
        checkPasswordValidityAndAge(connEnv, principal, passwordType.getValue(), passwordType.getMetadata(), passwordCredentialsPolicy);

        String password = getPassword(connEnv, principal, passwordType.getValue());

        // Authorizations
        if (hasNoneAuthorization(principal)) {
            recordAuthenticationBehavior(username, null, connEnv, "no authorizations", FocusType.class,false);
            throw new InternalAuthenticationServiceException("web.security.provider.access.denied");
        }

        return password;
    }

    @Override
    public PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(ConnectionEnvironment connEnv, PreAuthenticationContext authnCtx) {

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx.getUsername(),
                authnCtx.getPrincipalType(), authnCtx.isSupportActivationByChannel());

        // Authorizations
        if (hasNoneAuthorization(principal)) {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "no authorizations", authnCtx.getPrincipalType(), false);
            throw new InternalAuthenticationServiceException("web.security.provider.access.denied");
        }

        if (AuthenticationEvaluatorUtil.checkRequiredAssignmentTargets(principal.getFocus(), authnCtx.getRequireAssignments())) {
            PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, null, authnCtx.getPrincipalType(), true);
            return token;
        } else {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "not contains required assignment", authnCtx.getPrincipalType(), false);
            throw new InternalAuthenticationServiceException("web.security.flexAuth.invalid.required.assignment");
        }
    }

    @NotNull
    protected MidPointPrincipal getAndCheckPrincipal(ConnectionEnvironment connEnv, String enteredUsername, Class<? extends FocusType> clazz,
            boolean supportsActivationCheck) {

        if (StringUtils.isBlank(enteredUsername)) {
            recordAuthenticationFailure(enteredUsername, connEnv, "no username");
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }

        MidPointPrincipal principal;
        try {
            principal = focusProfileService.getPrincipal(enteredUsername, clazz);
        } catch (ObjectNotFoundException e) {
            recordAuthenticationFailure(enteredUsername, connEnv, "no focus");
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        } catch (SchemaException e) {
            recordAuthenticationFailure(enteredUsername, connEnv, "schema error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (CommunicationException e) {
            recordAuthenticationFailure(enteredUsername, connEnv, "communication error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (ConfigurationException e) {
            recordAuthenticationFailure(enteredUsername, connEnv, "configuration error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (SecurityViolationException e) {
            recordAuthenticationFailure(enteredUsername, connEnv, "security violation");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (ExpressionEvaluationException e) {
            recordAuthenticationFailure(enteredUsername, connEnv, "expression error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        }


        if (principal == null) {
            recordAuthenticationBehavior(enteredUsername, null, connEnv, "no focus", clazz, false);
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }

        if (supportsActivationCheck && !principal.isEnabled()) {
            recordAuthenticationBehavior(enteredUsername, principal, connEnv, "focus disabled", clazz, false);
            throw new DisabledException("web.security.provider.disabled");
        }
        return principal;
    }

    protected boolean hasNoneAuthorization(MidPointPrincipal principal) {
        Collection<Authorization> authorizations = principal.getAuthorities();
        if (authorizations == null || authorizations.isEmpty()){
            return true;
        }
        boolean exist = false;
        for (Authorization auth : authorizations){
            if (auth.getAction() != null && !auth.getAction().isEmpty()){
                exist = true;
            }
        }
        return !exist;
    }

    private <P extends CredentialPolicyType> void checkPasswordValidityAndAge(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, C credentials,
            P passwordCredentialsPolicy) {
        if (credentials == null) {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "no stored credential value", principal.getFocus().getClass(), false);
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
                    recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "password expired", principal.getFocus().getClass(), false);
                    throw new CredentialsExpiredException("web.security.provider.credential.expired");
                }
            }
        }
    }

    private void checkPasswordValidityAndAge(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString, MetadataType passwordMetadata,
            CredentialPolicyType passwordCredentialsPolicy) {
        if (protectedString == null) {
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "no stored password value", principal.getFocus().getClass(), false);
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.password.bad");
        }
        if (passwordCredentialsPolicy == null) {
            return;
        }
        Duration maxAge = passwordCredentialsPolicy.getMaxAge();
        if (maxAge != null) {
            XMLGregorianCalendar changeTimestamp = MiscSchemaUtil.getChangeTimestamp(passwordMetadata);
            if (changeTimestamp != null) {
                XMLGregorianCalendar passwordValidUntil = XmlTypeConverter.addDuration(changeTimestamp, maxAge);
                if (clock.isPast(passwordValidUntil)) {
                    recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "password expired", principal.getFocus().getClass(), false);
                    throw new CredentialsExpiredException("web.security.provider.credential.expired");
                }
            }
        }
    }


//    protected boolean matchDecryptedValue(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, String decryptedValue,
//            String enteredPassword){
//        return enteredPassword.equals(decryptedValue);
//    }
//
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
            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "error decrypting password: "+e.getMessage(), principal.getFocus().getClass(), false);
            throw new AuthenticationServiceException("web.security.provider.unavailable", e);
        }
    }

    protected String getDecryptedValue(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString) {
        String decryptedPassword;
        if (protectedString.getEncryptedDataType() != null) {
            try {
                decryptedPassword = protector.decryptString(protectedString);
            } catch (EncryptionException e) {
                recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "error decrypting password: "+e.getMessage(), principal.getFocus().getClass(), false);
                throw new AuthenticationServiceException("web.security.provider.unavailable", e);
            }
        } else {
            LOGGER.warn("Authenticating user based on clear value. Please check objects, "
                    + "this should not happen. Protected string should be encrypted.");
            decryptedPassword = protectedString.getClearValue();
        }
        return decryptedPassword;
    }

    private String getPassword(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString) {
        String decryptedPassword;
        if (protectedString.getEncryptedDataType() != null) {
            try {
                decryptedPassword = protector.decryptString(protectedString);
            } catch (EncryptionException e) {
                recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "error decrypting password: "+e.getMessage(), principal.getFocus().getClass(), false);
                throw new AuthenticationServiceException("web.security.provider.unavailable", e);
            }
        } else {
            LOGGER.warn("Authenticating user based on clear value. Please check objects, "
                    + "this should not happen. Protected string should be encrypted.");
            decryptedPassword = protectedString.getClearValue();
        }
        return decryptedPassword;
    }

    private boolean isLockedOut(AbstractCredentialType credentialsType, CredentialPolicyType credentialsPolicy) {
        return isOverFailedLockoutAttempts(credentialsType, credentialsPolicy) && !isLockoutExpired(credentialsType, credentialsPolicy);
    }

    private boolean isOverFailedLockoutAttempts(AbstractCredentialType credentialsType, CredentialPolicyType credentialsPolicy) {
        int failedLogins = credentialsType.getFailedLogins() != null ? credentialsType.getFailedLogins() : 0;
        return isOverFailedLockoutAttempts(failedLogins, credentialsPolicy);
    }

    private boolean isOverFailedLockoutAttempts(int failedLogins, CredentialPolicyType credentialsPolicy) {
        return credentialsPolicy != null && credentialsPolicy.getLockoutMaxFailedAttempts() != null &&
                credentialsPolicy.getLockoutMaxFailedAttempts() > 0 && failedLogins >= credentialsPolicy.getLockoutMaxFailedAttempts();
    }

    private boolean isLockoutExpired(AbstractCredentialType credentialsType, CredentialPolicyType credentialsPolicy) {
        Duration lockoutDuration = credentialsPolicy.getLockoutDuration();
        if (lockoutDuration == null) {
            return false;
        }
        LoginEventType lastFailedLogin = credentialsType.getLastFailedLogin();
        if (lastFailedLogin == null) {
            return true;
        }
        XMLGregorianCalendar lastFailedLoginTimestamp = lastFailedLogin.getTimestamp();
        if (lastFailedLoginTimestamp == null) {
            return true;
        }
        XMLGregorianCalendar lockedUntilTimestamp = XmlTypeConverter.addDuration(lastFailedLoginTimestamp, lockoutDuration);
        return clock.isPast(lockedUntilTimestamp);
    }

    public void recordAuthenticationBehavior(String username, MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            String reason, Class<? extends FocusType> focusType, boolean isSuccess) {
        if (principal == null && focusType != null) {
            try {
                principal = focusProfileService.getPrincipal(username, focusType);
            } catch (Exception e) {
                //ignore if non-exist
            }
        }
        if (principal != null) {
            AuthenticationBehavioralDataType behavior = AuthenticationEvaluatorUtil.getBehavior(principal.getFocus());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthenticationAttemptDataType authAttemptData = null;
            if (authentication instanceof MidpointAuthentication) {
                String sequenceIdentifier = ((MidpointAuthentication) authentication).getSequence().getIdentifier();
                String moduleIdentifier = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication().getModuleIdentifier();
                authAttemptData = AuthenticationEvaluatorUtil.findOrCreateAuthenticationAttemptData(behavior,
                        sequenceIdentifier, moduleIdentifier);
                authAttemptData.setChannel(((MidpointAuthentication) authentication).getSequence().getChannel());
                recordModuleAuthenticationAttempt(principal, authAttemptData, connEnv, isSuccess);
            }

            if (isSuccess) {
                recordPasswordAuthenticationSuccess(principal, connEnv, behavior, true);
            } else {
                recordPasswordAuthenticationFailure(principal, connEnv, behavior, null, reason, true);
            }
        } else {
            recordAuthenticationFailure(username, connEnv, reason);
        }
    }

    private void recordModuleAuthenticationAttempt(@NotNull MidPointPrincipal principal, @NotNull AuthenticationAttemptDataType authAttemptData,
            @NotNull ConnectionEnvironment connEnv, boolean isSuccess) {
        FocusType focusBefore = principal.getFocus().clone();
        Integer failedLogins = authAttemptData.getFailedAttempts();
        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
            authAttemptData.setFailedAttempts(0);
            successLoginAfterFail = true;
        }
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        if (isSuccess) {
            authAttemptData.setLastSuccessfulAuthentication(event);
        } else {
            authAttemptData.setLastFailedAuthentication(event);
        }
        LockoutStatusType oldLockoutStatus = authAttemptData.getLockoutStatus();
        if (LockoutStatusType.LOCKED.equals(oldLockoutStatus) && isSuccess) {
            authAttemptData.setLockoutStatus(LockoutStatusType.NORMAL);
            authAttemptData.setLockoutExpirationTimestamp(null);
        }

        ActivationType activation = principal.getFocus().getActivation();
        //todo decide user lockout status

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
        }
    }

    protected void recordPasswordAuthenticationSuccess(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            @NotNull AuthenticationBehavioralDataType behavioralData, boolean audit) {
        FocusType focusBefore = principal.getFocus().clone();
        Integer failedLogins = behavioralData.getFailedLogins();
        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
            behavioralData.setFailedLogins(0);
            successLoginAfterFail = true;
        }
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        behavioralData.setPreviousSuccessfulLogin(behavioralData.getLastSuccessfulLogin());
        behavioralData.setLastSuccessfulLogin(event);

        ActivationType activation = principal.getFocus().getActivation();
        if (activation != null) {
            if (LockoutStatusType.LOCKED.equals(activation.getLockoutStatus())) {
                successLoginAfterFail = true;
            }
            activation.setLockoutStatus(LockoutStatusType.NORMAL);
            activation.setLockoutExpirationTimestamp(null);
        }

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
        }
        if (audit) {
            recordAuthenticationSuccess(principal, connEnv);
        }
    }

    private void recordAuthenticationSuccess(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv) {
        securityHelper.auditLoginSuccess(principal.getFocus(), connEnv);
    }

    private void recordPasswordAuthenticationFailure(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            @NotNull AuthenticationBehavioralDataType behavioralData, CredentialPolicyType credentialsPolicy, String reason, boolean audit) {
        FocusType focusAfter = principal.getFocus();
        FocusType focusBefore = focusAfter.clone();
        Integer failedLogins = behavioralData.getFailedLogins();
        LoginEventType lastFailedLogin = behavioralData.getLastFailedLogin();
        XMLGregorianCalendar lastFailedLoginTs = null;
        if (lastFailedLogin != null) {
            lastFailedLoginTs = lastFailedLogin.getTimestamp();
        }

        if (credentialsPolicy != null) {
            Duration lockoutFailedAttemptsDuration = credentialsPolicy.getLockoutFailedAttemptsDuration();
            if (lockoutFailedAttemptsDuration != null) {
                if (lastFailedLoginTs != null) {
                    XMLGregorianCalendar failedLoginsExpirationTs = XmlTypeConverter.addDuration(lastFailedLoginTs, lockoutFailedAttemptsDuration);
                    if (clock.isPast(failedLoginsExpirationTs)) {
                        failedLogins = 0;
                    }
                }
            }
        }
        if (failedLogins == null) {
            failedLogins = 1;
        } else {
            failedLogins++;
        }

        behavioralData.setFailedLogins(failedLogins);

        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        behavioralData.setLastFailedLogin(event);

        if (isOverFailedLockoutAttempts(failedLogins, credentialsPolicy)) {
            ActivationType activation = focusAfter.getActivation();
            if (activation == null) {
                activation = new ActivationType();
                focusAfter.setActivation(activation);
            }
            activation.setLockoutStatus(LockoutStatusType.LOCKED);
            XMLGregorianCalendar lockoutExpirationTs = null;
            Duration lockoutDuration = credentialsPolicy.getLockoutDuration();
            if (lockoutDuration != null) {
                lockoutExpirationTs = XmlTypeConverter.addDuration(event.getTimestamp(), lockoutDuration);
            }
            activation.setLockoutExpirationTimestamp(lockoutExpirationTs);
            focusAfter.getTrigger().add(
                    new TriggerType()
                            .handlerUri(ModelPublicConstants.UNLOCK_TRIGGER_HANDLER_URI)
                            .timestamp(lockoutExpirationTs));
        }

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(true)) {
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, focusAfter));
        }
        if (audit) {
            recordAuthenticationFailure(principal, connEnv, reason);
        }
    }

    protected void recordAuthenticationFailure(@NotNull MidPointPrincipal principal, ConnectionEnvironment connEnv, String reason) {
        securityHelper.auditLoginFailure(principal.getUsername(), principal.getFocus(), connEnv, reason);
    }

    protected void recordAuthenticationFailure(String username, ConnectionEnvironment connEnv, String reason) {
        securityHelper.auditLoginFailure(username, null, connEnv, reason);
    }

    private Collection<? extends ItemDelta<?, ?>> computeModifications(@NotNull FocusType before, @NotNull FocusType after) {
        ObjectDelta<? extends FocusType> delta = ((PrismObject<FocusType>)before.asPrismObject())
                .diff((PrismObject<FocusType>) after.asPrismObject(), ParameterizedEquivalenceStrategy.DATA);
        assert delta.isModify();
        return delta.getModifications();
    }
}
