/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

@Component
public class FocusAuthenticationResultRecorder {

    private static final String DOT_CLASS = FocusAuthenticationResultRecorder.class.getName() + ".";
    private static final String OPERATION_UPDATE_PRINCIPAL_DYNAMICALLY = DOT_CLASS + "updatePrincipalDynamically";
    private static final Trace LOGGER = TraceManager.getTrace(FocusAuthenticationResultRecorder.class);

    @Autowired private ModelAuditRecorder auditProvider;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private Clock clock;

    @Autowired private ModelAuditRecorder securityHelper;
    @Autowired private RepositoryService repositoryService;

    public void recordModuleAuthenticationAttemptSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv) {
        AuthenticationAttemptDataType authAttemptData = AuthUtil.findOrCreateAuthenticationAttemptDataFoModule(connEnv, principal);

        Integer failedLogins = authAttemptData.getFailedAttempts();

        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
            LOGGER.debug("Resetting {} failed module attempt(s) for user '{}' after successful authentication (sequence={}, module={})",
                    failedLogins, principal.getUsername(), connEnv.getSequenceIdentifier(), connEnv.getModuleIdentifier());
            authAttemptData.setFailedAttempts(0);
            successLoginAfterFail = true;
        }
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        //TODO previoous successful auth
        //authAttemptData.(behavioralData.getLastSuccessfulLogin());
        authAttemptData.setLastSuccessfulAuthentication(event);

        authAttemptData.setLockoutTimestamp(null);
        authAttemptData.setLockoutExpirationTimestamp(null);

        ActivationType activation = principal.getFocus().getActivation();
        if (activation != null) {
            if (LockoutStatusType.LOCKED.equals(activation.getLockoutStatus())) {
                LOGGER.debug("Clearing lockout status for user '{}' after successful authentication", principal.getUsername());
                successLoginAfterFail = true;
            }
            activation.setLockoutStatus(LockoutStatusType.NORMAL);
            activation.setLockoutExpirationTimestamp(null);
        }

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            updatePrincipalDynamically(principal);
        }
    }

    public void recordModuleAuthenticationAttemptFailure(MidPointPrincipal principal, CredentialPolicyType credentialsPolicy, ConnectionEnvironment connEnv) {
        FocusType focusAfter = principal.getFocus();

        AuthenticationAttemptDataType authAttemptData = AuthUtil.findOrCreateAuthenticationAttemptDataFoModule(connEnv, principal);
        LOGGER.debug("recordModuleAuthenticationAttemptFailure: authAttemptData={}", authAttemptData);

        Integer failedLogins = authAttemptData.getFailedAttempts();
        LoginEventType lastFailedLogin = authAttemptData.getLastFailedAuthentication();
        XMLGregorianCalendar lastFailedLoginTs = null;
        if (lastFailedLogin != null) {
            lastFailedLoginTs = lastFailedLogin.getTimestamp();
        }
        LOGGER.debug("recordModuleAuthenticationAttemptFailure: failedLogins={}, lastFailedLoginTs={}", failedLogins, lastFailedLoginTs);

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

        LOGGER.debug("Failed module attempt for user '{}': count={} (sequence={}, module={})",
                principal.getUsername(), failedLogins, connEnv.getSequenceIdentifier(), connEnv.getModuleIdentifier());
        authAttemptData.setFailedAttempts(failedLogins);

        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        authAttemptData.setLastFailedAuthentication(event);

        if (SecurityUtil.isOverFailedLockoutAttempts(failedLogins, credentialsPolicy)) {
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
            LOGGER.debug("User '{}' locked out after {} failed attempt(s), expiration={}",
                    principal.getUsername(), failedLogins, lockoutExpirationTs);
            activation.setLockoutExpirationTimestamp(lockoutExpirationTs);
            authAttemptData.setLockoutExpirationTimestamp(lockoutExpirationTs);
            authAttemptData.setLockoutTimestamp(event.getTimestamp());
            focusAfter.getTrigger().add(
                    new TriggerType()
                            .handlerUri(ModelPublicConstants.UNLOCK_TRIGGER_HANDLER_URI)
                            .timestamp(lockoutExpirationTs));
        }

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(true)) {
            updatePrincipalDynamically(principal);
        }
    }

    public void recordSequenceAuthenticationSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv) {
        if (principal == null) {
            return;
        }
        AuthenticationBehavioralDataType behavior = AuthUtil.getOrCreateBehavioralDataForSequence(principal, connEnv.getSequenceIdentifier());

        Integer failedLogins = behavior.getFailedLogins();

        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
            behavior.setFailedLogins(0);
            successLoginAfterFail = true;
        }
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        behavior.setPreviousSuccessfulLogin(behavior.getLastSuccessfulLogin());
        behavior.setLastSuccessfulLogin(event);

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            updatePrincipalDynamically(principal);
        }
        securityHelper.auditLoginSuccess(principal.getFocus(), connEnv);
    }

    public void recordSequenceAuthenticationFailure(String username, MidPointPrincipal principal, CredentialPolicyType credentialsPolicy, String reason, ConnectionEnvironment connEnv) {
        FocusType focusType = null;
        if (principal == null && StringUtils.isNotEmpty(username)) {
            try {
                // For recording audit log, we don't need to support GUI config
                principal = focusProfileService.getPrincipal(
                        username, FocusType.class, ProfileCompilerOptions.createOnlyPrincipalOption());
            } catch (CommonException e) {
                //ignore error
            }
        }
        if (principal != null) {
            focusType = principal.getFocus();
            if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(true)) {
                processFocusChange(principal, credentialsPolicy, connEnv);
            }
        }
        securityHelper.auditLoginFailure(username, focusType, connEnv, reason);
    }

    private void processFocusChange(MidPointPrincipal principal, CredentialPolicyType credentialsPolicy, ConnectionEnvironment connEnv) {
        FocusType focusAfter = principal.getFocus();

        AuthenticationBehavioralDataType behavior = AuthUtil.getOrCreateBehavioralDataForSequence(principal, connEnv.getSequenceIdentifier());

        Integer failedLogins = behavior.getFailedLogins();
        LoginEventType lastFailedLogin = behavior.getLastFailedLogin();
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

        LOGGER.debug("Failed sequence attempt for user '{}': count={} (sequence={})",
                principal.getUsername(), failedLogins, connEnv.getSequenceIdentifier());
        behavior.setFailedLogins(failedLogins);

        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());

        behavior.setLastFailedLogin(event);

        if (SecurityUtil.isOverFailedLockoutAttempts(failedLogins, credentialsPolicy)) {
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
            LOGGER.debug("User '{}' locked out after {} failed sequence attempt(s), expiration={}",
                    principal.getUsername(), failedLogins, lockoutExpirationTs);
            activation.setLockoutExpirationTimestamp(lockoutExpirationTs);
            focusAfter.getTrigger().add(
                    new TriggerType()
                            .handlerUri(ModelPublicConstants.UNLOCK_TRIGGER_HANDLER_URI)
                            .timestamp(lockoutExpirationTs));
        }

        updatePrincipalDynamically(principal);
    }

    private Collection<? extends ItemDelta<?, ?>> computeModifications(@NotNull FocusType before, @NotNull FocusType after) {
        ObjectDelta<? extends FocusType> delta = ((PrismObject<FocusType>)before.asPrismObject())
                .diff((PrismObject<FocusType>) after.asPrismObject(), ParameterizedEquivalenceStrategy.DATA);
        assert delta.isModify();
        return delta.getModifications();
    }

    private void updatePrincipalDynamically(@NotNull MidPointPrincipal principal) {
        OperationResult result = new OperationResult(OPERATION_UPDATE_PRINCIPAL_DYNAMICALLY);
        try {
            repositoryService.modifyObjectDynamically(FocusType.class,
                    principal.getOid(),
                    null,
                    oldPrincipalValue ->
                            computeModifications(oldPrincipalValue, principal.getFocus()),
                    null,
                    result);
        } catch (CommonException e) {
            LOGGER.debug("Couldn't modify principal with the authentication result information: {}", e.getMessage(), e);
        }

    }

}
