/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import java.util.Collection;
import java.util.List;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.security.api.*;

import com.evolveum.midpoint.util.QNameUtil;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.UserProfileService;
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
public abstract class AuthenticationEvaluatorImpl<C extends AbstractCredentialType, T extends AbstractAuthenticationContext> implements AuthenticationEvaluator<T>, MessageSourceAware {

    private static final Trace LOGGER = TraceManager.getTrace(AuthenticationEvaluatorImpl.class);

    @Autowired private Protector protector;
    @Autowired private Clock clock;
    @Autowired private SecurityHelper securityHelper;

    // Has to be package-private so the tests can manipulate it
    @Autowired UserProfileService userProfileService;

    protected MessageSourceAccessor messages;

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    protected abstract void checkEnteredCredentials(ConnectionEnvironment connEnv, T authCtx);
    protected abstract boolean suportsAuthzCheck();
    protected abstract C getCredential(CredentialsType credentials);
    protected abstract void validateCredentialNotNull(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, C credential);
    protected abstract boolean passwordMatches(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, C passwordType,
            T authCtx);
    protected abstract CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy, T authnCtx) throws SchemaException;
    protected abstract boolean supportsActivation();

    @Override
    public UsernamePasswordAuthenticationToken authenticate(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {

        checkEnteredCredentials(connEnv, authnCtx);

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx.getUsername(), authnCtx.isSupportActivationByChannel());

        UserType userType = principal.getUser();
        CredentialsType credentials = userType.getCredentials();
        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        if (checkCredentials(principal, authnCtx, connEnv)) {

            if(checkRequiredAssignment(userType.getAssignment(), authnCtx.getRequireAssignments())){
                recordPasswordAuthenticationSuccess(principal, connEnv, getCredential(credentials));
                return new UsernamePasswordAuthenticationToken(principal, authnCtx.getEnteredCredential(), principal.getAuthorities());
            } else {
                recordPasswordAuthenticationFailure(principal, connEnv, getCredential(credentials), credentialsPolicy, "not contains required assignment");
                throw new InternalAuthenticationServiceException("web.security.flexAuth.invalid.required.assignment");
            }
        } else {
            recordPasswordAuthenticationFailure(principal, connEnv, getCredential(credentials), credentialsPolicy, "password mismatch");
            throw new BadCredentialsException("web.security.provider.invalid");
        }
    }

    protected boolean checkRequiredAssignment(List<AssignmentType> assignments, List<ObjectReferenceType> requireAssignments) {
        if (requireAssignments == null || requireAssignments.isEmpty()){
            return true;
        }
        if (assignments == null || assignments.isEmpty()) {
            return false;
        }

        for (ObjectReferenceType requiredAssignment : requireAssignments) {
            if (requiredAssignment == null) {
                throw new IllegalStateException("Required assignment is null");
            }
            if (requiredAssignment.getOid() == null){
                throw new IllegalStateException("Oid of required assignment is null");
            }
            boolean match = false;
            for (AssignmentType assignment : assignments) {
                ObjectReferenceType targetRef = assignment.getTargetRef();
                if (targetRef != null) {
                    if (targetRef.getOid() != null && targetRef.getOid().equals(requiredAssignment.getOid())) {
                        match = true;
                        break;
                    }
                } else if (assignment.getConstruction() != null && requiredAssignment.getType() != null
                && QNameUtil.match(requiredAssignment.getType(), ResourceType.COMPLEX_TYPE)) {
                            if (assignment.getConstruction().getResourceRef() != null &&
                                    assignment.getConstruction().getResourceRef().getOid() != null &&
                                    assignment.getConstruction().getResourceRef().getOid().equals(requiredAssignment.getOid())) {
                                match = true;
                                break;
                            }
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    @Override
    @NotNull
    public UserType checkCredentials(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {

        checkEnteredCredentials(connEnv, authnCtx);

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx.getUsername(), false);

        UserType userType = principal.getUser();
        CredentialsType credentials = userType.getCredentials();
        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        if (checkCredentials(principal, authnCtx, connEnv)) {
            return userType;
        } else {
            recordPasswordAuthenticationFailure(principal, connEnv, getCredential(credentials), credentialsPolicy, "password mismatch");

            throw new BadCredentialsException("web.security.provider.invalid");
        }
    }

    private boolean checkCredentials(MidPointPrincipal principal, T authnCtx, ConnectionEnvironment connEnv) {

        UserType userType = principal.getUser();
        CredentialsType credentials = userType.getCredentials();
        if (credentials == null || getCredential(credentials) == null) {
            recordAuthenticationFailure(principal, connEnv, "no credentials in user");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid");
        }

        CredentialPolicyType credentialsPolicy = getCredentialsPolicy(principal, authnCtx);

        // Lockout
        if (isLockedOut(getCredential(credentials), credentialsPolicy)) {
            recordAuthenticationFailure(principal, connEnv, "password locked-out");
            throw new LockedException("web.security.provider.locked");
        }

        if (suportsAuthzCheck()) {
            // Authorizations
            if (!hasAnyAuthorization(principal)) {
                recordAuthenticationFailure(principal, connEnv, "no authorizations");
                throw new DisabledException("web.security.provider.access.denied");
            }
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

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, username, true);

        UserType userType = principal.getUser();
        CredentialsType credentials = userType.getCredentials();
        if (credentials == null) {
            recordAuthenticationFailure(principal, connEnv, "no credentials in user");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid");
        }
        PasswordType passwordType = credentials.getPassword();
        SecurityPolicyType securityPolicy = principal.getApplicableSecurityPolicy();
        PasswordCredentialsPolicyType passwordCredentialsPolicy = SecurityUtil.getEffectivePasswordCredentialsPolicy(securityPolicy);

        // Lockout
        if (isLockedOut(passwordType, passwordCredentialsPolicy)) {
            recordAuthenticationFailure(principal, connEnv, "password locked-out");
            throw new LockedException("web.security.provider.locked");
        }

        // Authorizations
        if (!hasAnyAuthorization(principal)) {
            recordAuthenticationFailure(principal, connEnv, "no authorizations");
            throw new InternalAuthenticationServiceException("web.security.provider.access.denied");
        }

        // Password age
        checkPasswordValidityAndAge(connEnv, principal, passwordType.getValue(), passwordType.getMetadata(), passwordCredentialsPolicy);

        return getPassword(connEnv, principal, passwordType.getValue());
    }

    @Override
    public PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(ConnectionEnvironment connEnv, AbstractAuthenticationContext authnCtx) {

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx.getUsername(), authnCtx.isSupportActivationByChannel());

        // Authorizations
        if (!hasAnyAuthorization(principal)) {
            recordAuthenticationFailure(principal, connEnv, "no authorizations");
            throw new InternalAuthenticationServiceException("web.security.provider.access.denied");
        }

        if(checkRequiredAssignment(principal.getUser().getAssignment(), authnCtx.getRequireAssignments())){
            PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
            recordAuthenticationSuccess(principal, connEnv);
            return token;
        } else {
            recordAuthenticationFailure(principal.getUsername(), connEnv,"not contains required assignment");
            throw new InternalAuthenticationServiceException("web.security.flexAuth.invalid.required.assignment");
        }
    }

    @NotNull
    private MidPointPrincipal getAndCheckPrincipal(ConnectionEnvironment connEnv, String enteredUsername, boolean supportsActivationCheck) {

        if (StringUtils.isBlank(enteredUsername)) {
            recordAuthenticationFailure(enteredUsername, connEnv, "no username");
            throw new UsernameNotFoundException("web.security.provider.invalid");
        }

        MidPointPrincipal principal;
        try {
            principal = userProfileService.getPrincipal(enteredUsername);
        } catch (ObjectNotFoundException e) {
            recordAuthenticationFailure(enteredUsername, connEnv, "no user");
            throw new UsernameNotFoundException("web.security.provider.invalid");
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
            recordAuthenticationFailure(enteredUsername, connEnv, "no user");
            throw new UsernameNotFoundException("web.security.provider.invalid");
        }

        if (supportsActivationCheck && !principal.isEnabled()) {
            recordAuthenticationFailure(principal, connEnv, "user disabled");
            throw new DisabledException("web.security.provider.disabled");
        }
        return principal;
    }

    private boolean hasAnyAuthorization(MidPointPrincipal principal) {
        Collection<Authorization> authorizations = principal.getAuthorities();
        if (authorizations == null || authorizations.isEmpty()){
            return false;
        }
        for (Authorization auth : authorizations){
            if (auth.getAction() != null && !auth.getAction().isEmpty()){
                return true;
            }
        }
        return false;
    }

    private <P extends CredentialPolicyType> void checkPasswordValidityAndAge(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, C credentials,
            P passwordCredentialsPolicy) {
        if (credentials == null) {
            recordAuthenticationFailure(principal, connEnv, "no stored credential value");
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
                    recordAuthenticationFailure(principal, connEnv, "password expired");
                    throw new CredentialsExpiredException("web.security.provider.credential.expired");
                }
            }
        }
    }

    private void checkPasswordValidityAndAge(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString, MetadataType passwordMetadata,
            CredentialPolicyType passwordCredentialsPolicy) {
        if (protectedString == null) {
            recordAuthenticationFailure(principal, connEnv, "no stored password value");
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
                    recordAuthenticationFailure(principal, connEnv, "password expired");
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
            recordAuthenticationFailure(principal, connEnv, "error decrypting password: "+e.getMessage());
            throw new AuthenticationServiceException("web.security.provider.unavailable", e);
        }
    }

    protected String getDecryptedValue(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString) {
        String decryptedPassword;
        if (protectedString.getEncryptedDataType() != null) {
            try {
                decryptedPassword = protector.decryptString(protectedString);
            } catch (EncryptionException e) {
                recordAuthenticationFailure(principal, connEnv, "error decrypting password: "+e.getMessage());
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
                recordAuthenticationFailure(principal, connEnv, "error decrypting password: "+e.getMessage());
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

    public void recordPasswordAuthenticationSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv, C passwordType) {
        UserType userBefore = principal.getUser().clone();
        Integer failedLogins = passwordType.getFailedLogins();
        if (failedLogins != null && failedLogins > 0) {
            passwordType.setFailedLogins(0);
        }
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        passwordType.setPreviousSuccessfulLogin(passwordType.getLastSuccessfulLogin());
        passwordType.setLastSuccessfulLogin(event);

        ActivationType activation = principal.getUser().getActivation();
        if (activation != null) {
            activation.setLockoutStatus(LockoutStatusType.NORMAL);
            activation.setLockoutExpirationTimestamp(null);
        }

        userProfileService.updateUser(principal, computeModifications(userBefore, principal.getUser()));
        recordAuthenticationSuccess(principal, connEnv);
    }

    private void recordAuthenticationSuccess(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv) {
        securityHelper.auditLoginSuccess(principal.getUser(), connEnv);
    }

    public void recordPasswordAuthenticationFailure(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            @NotNull C passwordType, CredentialPolicyType credentialsPolicy, String reason) {
        UserType userBefore = principal.getUser().clone();
        Integer failedLogins = passwordType.getFailedLogins();
        LoginEventType lastFailedLogin = passwordType.getLastFailedLogin();
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

        passwordType.setFailedLogins(failedLogins);

        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        passwordType.setLastFailedLogin(event);

        ActivationType activationType = principal.getUser().getActivation();

        if (isOverFailedLockoutAttempts(failedLogins, credentialsPolicy)) {
            if (activationType == null) {
                activationType = new ActivationType();
                principal.getUser().setActivation(activationType);
            }
            activationType.setLockoutStatus(LockoutStatusType.LOCKED);
            XMLGregorianCalendar lockoutExpirationTs = null;
            Duration lockoutDuration = credentialsPolicy.getLockoutDuration();
            if (lockoutDuration != null) {
                lockoutExpirationTs = XmlTypeConverter.addDuration(event.getTimestamp(), lockoutDuration);
            }
            activationType.setLockoutExpirationTimestamp(lockoutExpirationTs);
        }

        userProfileService.updateUser(principal, computeModifications(userBefore, principal.getUser()));
        recordAuthenticationFailure(principal, connEnv, reason);
    }

    protected void recordAuthenticationFailure(@NotNull MidPointPrincipal principal, ConnectionEnvironment connEnv, String reason) {
        securityHelper.auditLoginFailure(principal.getUsername(), principal.getUser(), connEnv, reason);
    }

    protected void recordAuthenticationFailure(String username, ConnectionEnvironment connEnv, String reason) {
        securityHelper.auditLoginFailure(username, null, connEnv, reason);
    }

    private Collection<? extends ItemDelta<?, ?>> computeModifications(@NotNull UserType before, @NotNull UserType after) {
        ObjectDelta<UserType> delta = before.asPrismObject().diff(after.asPrismObject(), ParameterizedEquivalenceStrategy.LITERAL);
        assert delta.isModify();
        return delta.getModifications();
    }
}
