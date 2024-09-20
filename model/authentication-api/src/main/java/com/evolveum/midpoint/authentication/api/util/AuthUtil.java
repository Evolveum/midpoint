/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.util;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.NotLoggedInException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class AuthUtil {

    private static final Trace LOGGER = TraceManager.getTrace(AuthUtil.class);

    private static final String DOT_CLASS = AuthUtil.class.getName() + ".";
    private static final String OPERATION_LOAD_FLOW_POLICY = DOT_CLASS + "loadFlowPolicy";

    public static @Nullable MidPointPrincipal getMidpointPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = authentication != null ? authentication.getPrincipal() : null;
        return principal instanceof MidPointPrincipal mp ? mp : null;
    }

    public static @NotNull FocusType getPrincipalObjectRequired() throws NotLoggedInException {
        return MiscUtil.requireNonNull(getMidpointPrincipal(), () -> new NotLoggedInException())
                .getFocus();
    }

    public static @NotNull ObjectReferenceType getPrincipalRefRequired() throws NotLoggedInException {
        return MiscUtil.requireNonNull(getMidpointPrincipal(), () -> new NotLoggedInException())
                .toObjectReference();
    }

    public static @NotNull GuiProfiledPrincipal getGuiProfiledPrincipalRequired() throws NotLoggedInException {
        return MiscUtil.requireNonNull(
                getPrincipalUser(),
                () -> new NotLoggedInException());
    }

    public static GuiProfiledPrincipal getPrincipalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return getPrincipalUser(authentication);
    }

    public static GuiProfiledPrincipal getPrincipalUser(Authentication authentication) {
        if (authentication == null) {
            LOGGER.trace("Authentication not available in security context.");
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }
        if (principal instanceof GuiProfiledPrincipal) {
            return (GuiProfiledPrincipal) principal;
        }
        if (AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
            // silently ignore to avoid filling the logs
            return null;
        }
        LOGGER.debug("Principal user in security context holder is {} ({}) but not type of {}",
                principal, principal.getClass(), GuiProfiledPrincipal.class.getName());
        return null;
    }

    public static boolean isPostAuthenticationEnabled(TaskManager taskManager, ModelInteractionService modelInteractionService) {
        MidPointPrincipal midpointPrincipal = getPrincipalUser();
        if (midpointPrincipal != null) {
            FocusType focus = midpointPrincipal.getFocus();
            Task task = taskManager.createTaskInstance(OPERATION_LOAD_FLOW_POLICY);
            OperationResult parentResult = new OperationResult(OPERATION_LOAD_FLOW_POLICY);
            RegistrationsPolicyType registrationPolicyType;
            try {
                registrationPolicyType = modelInteractionService.getFlowPolicy(focus.asPrismObject(), task, parentResult);
                if (registrationPolicyType == null) {
                    return false;
                }
                SelfRegistrationPolicyType postAuthenticationPolicy = registrationPolicyType.getPostAuthentication();
                if (postAuthenticationPolicy == null) {
                    return false;
                }
                String requiredLifecycleState = postAuthenticationPolicy.getRequiredLifecycleState();
                if (StringUtils.isNotBlank(requiredLifecycleState) && requiredLifecycleState.equals(focus.getLifecycleState())) {
                    return true;
                }
            } catch (CommonException e) {
                LoggingUtils.logException(LOGGER, "Cannot determine post authentication policies", e);
            }
        }
        return false;
    }

    /**
     * Convenient method to return instance of MidpointAuthentication if exists
     * If not present, exception is thrown.
     *
     * TODO: maybe we wll need to change exception to return null
     */
    public static MidpointAuthentication getMidpointAuthentication() {
        return getMidpointAuthentication(true);
    }

    public static MidpointAuthentication getMidpointAuthentication(boolean required) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            if (required) {
                throw new AuthenticationServiceException("web.security.flexAuth.auth.wrong.type");
            }
            return null;
        }
        return (MidpointAuthentication) authentication;
    }

    @Nullable
    public static MidpointAuthentication getMidpointAuthenticationNotRequired() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            return null;
        }
        return (MidpointAuthentication) authentication;
    }

    public static ModuleAuthentication getAuthenticatedModule() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        MidpointAuthentication mpAuthentication = getMidpointAuthentication();
        for (ModuleAuthentication moduleAuthentication : mpAuthentication.getAuthentications()) {
            if (AuthenticationModuleState.SUCCESSFULLY.equals(moduleAuthentication.getState())) {
                return moduleAuthentication;
            }
        }
        return null;
//        } else {
//            String message = "Unsupported type " + (authentication == null ? null : authentication.getClass().getName())
//                    + " of authentication for MidpointLogoutRedirectFilter, supported is only MidpointAuthentication";
//            throw new IllegalArgumentException(message);
//        }
//        return null;
    }

    @Nullable
    public static ModuleAuthentication getProcessingModuleIfExist() {
        return getProcessingModule(false);
    }

    @NotNull
    public static ModuleAuthentication getProcessingModule() {
        return Objects.requireNonNull(getProcessingModule(true));
    }

    private static ModuleAuthentication getProcessingModule(boolean required) {

        MidpointAuthentication mpAuthentication = getMidpointAuthentication(required);
        ModuleAuthentication moduleAuthentication = null;
        if (mpAuthentication != null) {
            moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        }
        if (required && moduleAuthentication == null) {
            LOGGER.error("Couldn't find processing module authentication {}", mpAuthentication);
            throw new AuthenticationServiceException("web.security.flexAuth.module.null");
        }
        return moduleAuthentication;
    }

    public static String stripEndingSlashes(String s) {
        if (StringUtils.isNotEmpty(s) && s.endsWith("/")) {
            if (s.equals("/")) {
                return "";
            }
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String stripStartingSlashes(String s) {
        if (StringUtils.isNotEmpty(s) && s.startsWith("/")) {
            if (s.equals("/")) {
                return "";
            }
            s = s.substring(1);
        }
        return s;
    }

    public static String stripSlashes(String s) {
        s = stripStartingSlashes(s);
        s = stripEndingSlashes(s);
        return s;
    }

    public static String resolveTokenTypeByModuleType(String nameOfModuleType) {
        if (AuthenticationModuleNameConstants.OIDC.equals(nameOfModuleType)) {
            return "Bearer";
        }
        return nameOfModuleType;
    }

    public static void clearMidpointAuthentication() {
        MidpointAuthentication oldAuthentication = getMidpointAuthentication();
//        Authentication oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
//        if (oldAuthentication instanceof MidpointAuthentication
        if (oldAuthentication.getAuthenticationChannel() != null) {
            RemoveUnusedSecurityFilterPublisher.get().publishCustomEvent(oldAuthentication.getAuthModules());
        }
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public static AuthenticationAttemptDataType findAuthAttemptDataForModule(ConnectionEnvironment connectionEnvironment, MidPointPrincipal principal) {
        return findAuthAttemptDataForModule(connectionEnvironment, principal.getFocus());
    }

    public static AuthenticationAttemptDataType findAuthAttemptDataForModule(ConnectionEnvironment connectionEnvironment, FocusType focus) {
        String sequenceIdentifier = connectionEnvironment.getSequenceIdentifier();
        String moduleIdentifier = connectionEnvironment.getModuleIdentifier();
        if (StringUtils.isEmpty(sequenceIdentifier) || StringUtils.isEmpty(moduleIdentifier)) {
            return null;
        }

        if (focus.getBehavior() == null || focus.getBehavior().getAuthentication() == null) {
            return null;
        }
        AuthenticationBehavioralDataType behavioralDataType = getBehavioralDataForSequence(focus, sequenceIdentifier);
        if (behavioralDataType == null) {
            return null;
        }
        List<AuthenticationAttemptDataType> authAttempts = behavioralDataType.getAuthenticationAttempt();
        return authAttempts.stream()
                .filter(attempt -> sequenceIdentifier.equals(attempt.getSequenceIdentifier())
                        && moduleIdentifier.equals(attempt.getModuleIdentifier()))
                .findFirst().orElse(null);
    }

    public static AuthenticationBehavioralDataType getOrCreateBehavioralDataForSequence(MidPointPrincipal principal, String sequenceId) {
        FocusType focus = principal.getFocus();
        return getOrCreateBehavioralDataForSequence(focus, sequenceId);
    }

    public static AuthenticationBehavioralDataType getBehavioralDataForSequence(FocusType focus, String sequenceId) {
        if (focus.getBehavior() == null) {
            focus.setBehavior(new BehaviorType());
        }
        return focus.getBehavior().getAuthentication()
                .stream()
                .filter(authData -> sequenceId.equals(authData.getSequenceIdentifier()))
                .findFirst()
                .orElse(null);
    }

    public static AuthenticationBehavioralDataType getOrCreateBehavioralDataForSequence(FocusType focus, String sequenceId) {
        if (focus.getBehavior() == null) {
            focus.setBehavior(new BehaviorType());
        }
        AuthenticationBehavioralDataType authenticationData = focus.getBehavior().getAuthentication()
                .stream()
                .filter(authData -> sequenceId.equals(authData.getSequenceIdentifier()))
                .findFirst()
                .orElse(null);

        if (authenticationData == null) {
            authenticationData = new AuthenticationBehavioralDataType().sequenceIdentifier(sequenceId);
            focus.getBehavior().getAuthentication().add(authenticationData);
        }
        return authenticationData;
    }

    public static AuthenticationAttemptDataType findOrCreateAuthenticationAttemptDataFoModule(ConnectionEnvironment connectionEnvironment, MidPointPrincipal principal) {
        return findOrCreateAuthenticationAttemptDataFoModule(connectionEnvironment, principal.getFocus());
    }

    public static AuthenticationAttemptDataType findOrCreateAuthenticationAttemptDataFoModule(ConnectionEnvironment connectionEnvironment, FocusType focus) {
        AuthenticationAttemptDataType data = findAuthAttemptDataForModule(connectionEnvironment, focus);
        if (data == null) {
            data = new AuthenticationAttemptDataType();
            data.setSequenceIdentifier(connectionEnvironment.getSequenceIdentifier());
            data.setModuleIdentifier(connectionEnvironment.getModuleIdentifier());
            AuthenticationBehavioralDataType behavior = getOrCreateBehavioralDataForSequence(focus, connectionEnvironment.getSequenceIdentifier());
            behavior.getAuthenticationAttempt().add(data);
        }

//        data.setChannel(connectionEnvironment.getChannel());
        return data;
    }

    public static String generateBadCredentialsMessageKey(Authentication authentication) {
        String defaultPrefix = "web.security.provider.";
        String defaultSuffix = "invalid.credentials";
        if (!(authentication instanceof MidpointAuthentication)) {
            return defaultPrefix + defaultSuffix;
        }
        //todo generate another message keys for self registration?
        if (isPasswordResetAuthChannel((MidpointAuthentication) authentication)) {
            return defaultPrefix + SchemaConstants.CHANNEL_RESET_PASSWORD_QNAME.getLocalPart() + "." + defaultSuffix;
        }
        return defaultPrefix + defaultSuffix;
    }

    private static boolean isPasswordResetAuthChannel(MidpointAuthentication authentication) {
        if (authentication.getAuthenticationChannel() == null) {
            return false;
        }
        return SchemaConstants.CHANNEL_RESET_PASSWORD_URI.equals(authentication.getAuthenticationChannel().getChannelId());
    }

    public static boolean isClusterAuthentication(MidpointAuthentication authentication) {
        if (authentication.getAuthModules().size() != 1) {
            return false;
        }
        ModuleAuthentication baseAuthentication = authentication.getAuthModules().get(0).getBaseModuleAuthentication();
        if (baseAuthentication == null) {
            return false;
        }
        return AuthenticationModuleNameConstants.CLUSTER.equals(baseAuthentication.getModuleTypeName());
    }
}
