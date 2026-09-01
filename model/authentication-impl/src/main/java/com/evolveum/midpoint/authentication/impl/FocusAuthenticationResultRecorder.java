/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
import java.util.function.Consumer;

@Component
public class FocusAuthenticationResultRecorder {

    private static final Trace LOGGER = TraceManager.getTrace(FocusAuthenticationResultRecorder.class);

    private static final String DOT_CLASS = FocusAuthenticationResultRecorder.class.getName() + ".";

    private static final String OPERATION_UPDATE_PRINCIPAL_DYNAMICALLY = DOT_CLASS + "updatePrincipalDynamically";

    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private Clock clock;
    @Autowired private ModelAuditRecorder securityHelper;
    @Autowired private RepositoryService repositoryService;

    public void recordModuleAuthenticationAttemptSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv) {
        LoginEventType event = createLoginEvent(connEnv);
        boolean successLoginAfterFail = applyModuleAttemptSuccess(principal.getFocus(), connEnv, event);
        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            updatePrincipalDynamically(principal, focus -> applyModuleAttemptSuccess(focus, connEnv, event));
        }
    }

    private boolean applyModuleAttemptSuccess(FocusType focus, ConnectionEnvironment connEnv, LoginEventType event) {
        AuthenticationAttemptDataType authAttemptData = AuthUtil.findOrCreateAuthenticationAttemptDataFoModule(connEnv, focus);

        Integer failedLogins = authAttemptData.getFailedAttempts();

        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
            LOGGER.debug("Resetting {} failed module attempt(s) for user '{}' after successful authentication (sequence={}, module={})",
                    failedLogins, focus.getName(), connEnv.getSequenceIdentifier(), connEnv.getModuleIdentifier());
            authAttemptData.setFailedAttempts(0);
            successLoginAfterFail = true;
        }

        //TODO previoous successful auth
        //authAttemptData.(behavioralData.getLastSuccessfulLogin());
        authAttemptData.setLastSuccessfulAuthentication(event);

        authAttemptData.setLockoutTimestamp(null);
        authAttemptData.setLockoutExpirationTimestamp(null);

        ActivationType activation = focus.getActivation();
        if (activation != null) {
            if (LockoutStatusType.LOCKED.equals(activation.getLockoutStatus())) {
                LOGGER.debug("Clearing lockout status for user '{}' after successful authentication", focus.getName());
                successLoginAfterFail = true;
            }
            activation.setLockoutStatus(LockoutStatusType.NORMAL);
            activation.setLockoutExpirationTimestamp(null);
        }
        return successLoginAfterFail;
    }

    public void recordModuleAuthenticationAttemptFailure(MidPointPrincipal principal, CredentialPolicyType credentialsPolicy, ConnectionEnvironment connEnv) {
        LoginEventType event = createLoginEvent(connEnv);
        applyModuleAttemptFailure(principal.getFocus(), credentialsPolicy, connEnv, event);
        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(true)) {
            updatePrincipalDynamically(principal, focus -> applyModuleAttemptFailure(focus, credentialsPolicy, connEnv, event));
        }
    }

    private void applyModuleAttemptFailure(
            FocusType focus, CredentialPolicyType credentialsPolicy, ConnectionEnvironment connEnv, LoginEventType event) {
        AuthenticationAttemptDataType authAttemptData = AuthUtil.findOrCreateAuthenticationAttemptDataFoModule(connEnv, focus);
        LOGGER.debug("recordModuleAuthenticationAttemptFailure: authAttemptData={}", authAttemptData);

        Integer failedLogins = computeFailedLogins(
                authAttemptData.getFailedAttempts(), authAttemptData.getLastFailedAuthentication(), credentialsPolicy);

        LOGGER.debug("Failed module attempt for user '{}': count={} (sequence={}, module={})",
                focus.getName(), failedLogins, connEnv.getSequenceIdentifier(), connEnv.getModuleIdentifier());
        authAttemptData.setFailedAttempts(failedLogins);
        authAttemptData.setLastFailedAuthentication(event);

        if (SecurityUtil.isOverFailedLockoutAttempts(failedLogins, credentialsPolicy)) {
            XMLGregorianCalendar lockoutExpirationTs = lockOutFocus(focus, credentialsPolicy, event, failedLogins);
            authAttemptData.setLockoutExpirationTimestamp(lockoutExpirationTs);
            authAttemptData.setLockoutTimestamp(event.getTimestamp());
        }
    }

    public void recordSequenceAuthenticationSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv) {
        if (principal == null) {
            return;
        }
        LoginEventType event = createLoginEvent(connEnv);
        boolean successLoginAfterFail = applySequenceSuccess(principal.getFocus(), connEnv.getSequenceIdentifier(), event);
        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            updatePrincipalDynamically(principal, focus -> applySequenceSuccess(focus, connEnv.getSequenceIdentifier(), event));
        }
        securityHelper.auditLoginSuccess(principal.getFocus(), connEnv);
    }

    private boolean applySequenceSuccess(FocusType focus, String sequenceIdentifier, LoginEventType event) {
        AuthenticationBehavioralDataType behavior = AuthUtil.getOrCreateBehavioralDataForSequence(focus, sequenceIdentifier);

        Integer failedLogins = behavior.getFailedLogins();

        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
            behavior.setFailedLogins(0);
            successLoginAfterFail = true;
        }

        behavior.setPreviousSuccessfulLogin(behavior.getLastSuccessfulLogin());
        behavior.setLastSuccessfulLogin(event);
        return successLoginAfterFail;
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
                LoginEventType event = createLoginEvent(connEnv);
                String sequenceIdentifier = connEnv.getSequenceIdentifier();
                applySequenceFailure(principal.getFocus(), credentialsPolicy, sequenceIdentifier, event);
                updatePrincipalDynamically(principal,
                        focus -> applySequenceFailure(focus, credentialsPolicy, sequenceIdentifier, event));
            }
        }
        securityHelper.auditLoginFailure(username, focusType, connEnv, reason);
    }

    private void applySequenceFailure(
            FocusType focus, CredentialPolicyType credentialsPolicy, String sequenceIdentifier, LoginEventType event) {
        AuthenticationBehavioralDataType behavior = AuthUtil.getOrCreateBehavioralDataForSequence(focus, sequenceIdentifier);

        Integer failedLogins = computeFailedLogins(behavior.getFailedLogins(), behavior.getLastFailedLogin(), credentialsPolicy);

        LOGGER.debug("Failed sequence attempt for user '{}': count={} (sequence={})",
                focus.getName(), failedLogins, sequenceIdentifier);
        behavior.setFailedLogins(failedLogins);
        behavior.setLastFailedLogin(event);

        if (SecurityUtil.isOverFailedLockoutAttempts(failedLogins, credentialsPolicy)) {
            lockOutFocus(focus, credentialsPolicy, event, failedLogins);
        }
    }

    private LoginEventType createLoginEvent(ConnectionEnvironment connEnv) {
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(connEnv.getRemoteHostAddress());
        return event;
    }

    private @NotNull Integer computeFailedLogins(
            Integer failedLogins, LoginEventType lastFailedLogin, CredentialPolicyType credentialsPolicy) {
        XMLGregorianCalendar lastFailedLoginTs = lastFailedLogin != null ? lastFailedLogin.getTimestamp() : null;

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
        return failedLogins == null ? 1 : failedLogins + 1;
    }

    private XMLGregorianCalendar lockOutFocus(
            FocusType focus, CredentialPolicyType credentialsPolicy, LoginEventType event, Integer failedLogins) {
        ActivationType activation = focus.getActivation();
        if (activation == null) {
            activation = new ActivationType();
            focus.setActivation(activation);
        }
        activation.setLockoutStatus(LockoutStatusType.LOCKED);
        XMLGregorianCalendar lockoutExpirationTs = null;
        Duration lockoutDuration = credentialsPolicy.getLockoutDuration();
        if (lockoutDuration != null) {
            lockoutExpirationTs = XmlTypeConverter.addDuration(event.getTimestamp(), lockoutDuration);
        }
        LOGGER.debug("User '{}' locked out after {} failed attempt(s), expiration={}",
                focus.getName(), failedLogins, lockoutExpirationTs);
        activation.setLockoutExpirationTimestamp(lockoutExpirationTs);
        focus.getTrigger().add(
                new TriggerType()
                        .handlerUri(ModelPublicConstants.UNLOCK_TRIGGER_HANDLER_URI)
                        .timestamp(lockoutExpirationTs));
        return lockoutExpirationTs;
    }

    /**
     * Persists the authentication result by applying the given mutation to the fresh repository
     * object. The resulting delta contains only the changes made by this recorder; the same
     * mutation is applied by the caller to the in-memory focus of the principal.
     * Diffing against the in-memory principal focus is not correct, as it may be stale and
     * the diff would revert concurrent modifications, e.g. resurrect the mail nonce spent during
     * currently running authentication (#12082).
     */
    private void updatePrincipalDynamically(@NotNull MidPointPrincipal principal, @NotNull Consumer<FocusType> mutator) {
        OperationResult result = new OperationResult(OPERATION_UPDATE_PRINCIPAL_DYNAMICALLY);
        try {
            repositoryService.modifyObjectDynamically(FocusType.class,
                    principal.getOid(),
                    null,
                    freshFocus -> computeModifications(freshFocus, mutator),
                    null,
                    result);
        } catch (CommonException e) {
            LOGGER.debug("Couldn't modify principal with the authentication result information: {}", e.getMessage(), e);
        }

    }

    private Collection<? extends ItemDelta<?, ?>> computeModifications(
            @NotNull FocusType freshFocus, @NotNull Consumer<FocusType> mutator) {
        FocusType updatedFocus = freshFocus.asPrismObject().clone().asObjectable();
        mutator.accept(updatedFocus);
        ObjectDelta<? extends FocusType> delta = ((PrismObject<FocusType>) freshFocus.asPrismObject())
                .diff((PrismObject<FocusType>) updatedFocus.asPrismObject(), ParameterizedEquivalenceStrategy.DATA);
        assert delta.isModify();
        return delta.getModifications();
    }

}
