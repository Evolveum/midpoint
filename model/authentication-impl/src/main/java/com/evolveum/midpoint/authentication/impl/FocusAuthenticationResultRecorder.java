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
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

@Component
public class FocusAuthenticationResultRecorder {

    @Autowired private ModelAuditRecorder auditProvider;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private Clock clock;

    @Autowired private ModelAuditRecorder securityHelper;

    public void recordModuleAuthenticationAttemptSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv) {
        FocusType focusBefore = principal.getFocus().clone();

        AuthenticationAttemptDataType authAttemptData = AuthUtil.findOrCreateAuthenticationAttemptDataFoModule(connEnv, principal);

        Integer failedLogins = authAttemptData.getFailedAttempts();

        boolean successLoginAfterFail = false;
        if (failedLogins != null && failedLogins > 0) {
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
                successLoginAfterFail = true;
            }
            activation.setLockoutStatus(LockoutStatusType.NORMAL);
            activation.setLockoutExpirationTimestamp(null);
        }

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(successLoginAfterFail)) {
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
        }
    }

    public void recordModuleAuthenticationAttemptFailure(MidPointPrincipal principal, CredentialPolicyType credentialsPolicy, ConnectionEnvironment connEnv) {
        FocusType focusAfter = principal.getFocus();
        FocusType focusBefore = focusAfter.clone();

        AuthenticationAttemptDataType authAttemptData = AuthUtil.findOrCreateAuthenticationAttemptDataFoModule(connEnv, principal);

        Integer failedLogins = authAttemptData.getFailedAttempts();
        LoginEventType lastFailedLogin = authAttemptData.getLastFailedAuthentication();
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
            activation.setLockoutExpirationTimestamp(lockoutExpirationTs);
            authAttemptData.setLockoutExpirationTimestamp(lockoutExpirationTs);
            authAttemptData.setLockoutTimestamp(event.getTimestamp());
            focusAfter.getTrigger().add(
                    new TriggerType()
                            .handlerUri(ModelPublicConstants.UNLOCK_TRIGGER_HANDLER_URI)
                            .timestamp(lockoutExpirationTs));
        }

        if (AuthSequenceUtil.isAllowUpdatingAuthBehavior(true)) {
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, focusAfter));
        }
    }

    public void recordSequenceAuthenticationSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv) {
        if (principal == null) {
            //TODO logging?
            return;
        }
        FocusType focusBefore = principal.getFocus().clone();

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
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
        }
        securityHelper.auditLoginSuccess(principal.getFocus(), connEnv);
    }

    public void recordSequenceAuthenticationFailure(String username, MidPointPrincipal principal, CredentialPolicyType credentialsPolicy, String reason, ConnectionEnvironment connEnv) {
        FocusType focusType = null;
        if (principal == null && StringUtils.isNotEmpty(username)) {
            try {
                principal = focusProfileService.getPrincipal(username, FocusType.class);
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
        FocusType focusBefore = focusAfter.clone();

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
            activation.setLockoutExpirationTimestamp(lockoutExpirationTs);
            focusAfter.getTrigger().add(
                    new TriggerType()
                            .handlerUri(ModelPublicConstants.UNLOCK_TRIGGER_HANDLER_URI)
                            .timestamp(lockoutExpirationTs));
        }

        focusProfileService.updateFocus(principal, computeModifications(focusBefore, focusAfter));
    }

    private Collection<? extends ItemDelta<?, ?>> computeModifications(@NotNull FocusType before, @NotNull FocusType after) {
        ObjectDelta<? extends FocusType> delta = ((PrismObject<FocusType>)before.asPrismObject())
                .diff((PrismObject<FocusType>) after.asPrismObject(), ParameterizedEquivalenceStrategy.DATA);
        assert delta.isModify();
        return delta.getModifications();
    }

}
