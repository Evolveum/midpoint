/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import javax.xml.datatype.Duration;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.common.util.AuditHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

/**
 * @author semancik
 */
@Component
public class SecurityHelper implements ModelAuditRecorder {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityHelper.class);

    @Autowired private TaskManager taskManager;
    @Autowired private AuditHelper auditHelper;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private PrismContext prismContext;
    @Autowired private SystemObjectCache systemObjectCache;

    @Override
    public void auditLoginSuccess(@NotNull FocusType user, @NotNull ConnectionEnvironment connEnv) {
        auditLogin(user.getName().getOrig(), user, connEnv, OperationResultStatus.SUCCESS, null);
    }

    public void auditLoginSuccess(@NotNull NodeType node, @NotNull ConnectionEnvironment connEnv) {
        auditLogin(node.getName().getOrig(), null, connEnv, OperationResultStatus.SUCCESS, null);
    }

    @Override
    public void auditLoginFailure(@Nullable String username, @Nullable FocusType focus, @NotNull ConnectionEnvironment connEnv, String message) {
        auditLogin(username, focus, connEnv, OperationResultStatus.FATAL_ERROR, message);
    }

    private void auditLogin(@Nullable String username, @Nullable FocusType focus, @NotNull ConnectionEnvironment connEnv, @NotNull OperationResultStatus status,
            @Nullable String message) {
        String channel = connEnv.getChannel();
        if (!SecurityUtil.isAuditedLoginAndLogout(getSystemConfig(), channel)) {
            return;
        }
        Task task = taskManager.createTaskInstance();
        task.setChannel(channel);

        LOGGER.debug("Login {} username={}, channel={}: {}",
                status == OperationResultStatus.SUCCESS ? "success" : "failure", username,
                connEnv.getChannel(), message);

        AuditEventRecord record = new AuditEventRecord(AuditEventType.CREATE_SESSION, AuditEventStage.REQUEST);
        record.setParameter(username);
        if (focus != null) {
            record.setInitiator(focus.asPrismObject());
        }
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(status);
        record.setMessage(message);
        storeConnectionEnvironment(record, connEnv);

        auditHelper.audit(record, null, task, new OperationResult(SecurityHelper.class.getName() + ".auditLogin"));
    }

    @Override
    public void auditLogout(ConnectionEnvironment connEnv, Task task, OperationResult result) {
        if (!SecurityUtil.isAuditedLoginAndLogout(getSystemConfig(), connEnv.getChannel())) {
            return;
        }
        AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
        PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
        record.setInitiatorAndLoginParameter(taskOwner);
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(OperationResultStatus.SUCCESS);
        storeConnectionEnvironment(record, connEnv);
        auditHelper.audit(record, null, task, result);
    }

    private SystemConfigurationType getSystemConfig() {
        SystemConfigurationType system = null;
        try {
            system = systemObjectCache.getSystemConfiguration(new OperationResult("LOAD SYSTEM CONFIGURATION")).asObjectable();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get system configuration from cache", e);
        }
        return system;
    }

    private void storeConnectionEnvironment(AuditEventRecord record, ConnectionEnvironment connEnv) {
        record.setChannel(connEnv.getChannel());
        record.setSessionIdentifier(connEnv.getSessionId());
        HttpConnectionInformation connInfo = connEnv.getConnectionInformation();
        if (connInfo != null) {
            record.setRemoteHostAddress(connInfo.getRemoteHostAddress());
            record.setHostIdentifier(connInfo.getLocalHostName());
        }
    }

    /**
     * Returns security policy applicable for the specified user. It looks for organization and global policies and takes into account
     * deprecated properties and password policy references. The resulting security policy has all the (non-deprecated) properties set.
     * If there is also referenced value policy, it is will be stored as "object" in the value policy reference inside the
     * returned security policy.
     */
    public <F extends FocusType> SecurityPolicyType locateSecurityPolicy(PrismObject<F> focus, PrismObject<SystemConfigurationType> systemConfiguration,
            Task task, OperationResult result) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        SecurityPolicyType focusSecurityPolicy = locateFocusSecurityPolicyFromOrgs(focus, task, result);
        if (focusSecurityPolicy != null) {
            traceSecurityPolicy(focusSecurityPolicy, focus);
            return focusSecurityPolicy;
        }

        SecurityPolicyType globalSecurityPolicy = locateGlobalSecurityPolicy(focus, systemConfiguration, task, result);
        if (globalSecurityPolicy != null) {
            traceSecurityPolicy(globalSecurityPolicy, focus);
            return globalSecurityPolicy;
        }

        return null;
    }

    public <F extends FocusType> SecurityPolicyType locateFocusSecurityPolicyFromOrgs(PrismObject<F> focus, Task task,
            OperationResult result) throws SchemaException {
        PrismObject<SecurityPolicyType> orgSecurityPolicy = objectResolver.searchOrgTreeWidthFirstReference(focus,
                o -> o.asObjectable().getSecurityPolicyRef(), "security policy", task, result);
        LOGGER.trace("Found organization security policy: {}", orgSecurityPolicy);
        if (orgSecurityPolicy != null) {
            SecurityPolicyType orgSecurityPolicyType = orgSecurityPolicy.asObjectable();
            postProcessSecurityPolicy(orgSecurityPolicyType, task, result);
            return orgSecurityPolicyType;
        } else {
            return null;
        }
    }

    public <F extends FocusType> SecurityPolicyType locateFocusSecurityPolicyFromArchetypes(PrismObject<F> focus, Task task,
            OperationResult result) throws SchemaException {
        PrismObject<SecurityPolicyType> archetypeSecurityPolicy = objectResolver.searchSecurityPolicyFromArchetype(focus,
                "security policy", task, result);   //todo may be use objectResolver.mergeSecurityPolicyWithSuperArchetype here
        LOGGER.trace("Found archetype security policy: {}", archetypeSecurityPolicy);
        if (archetypeSecurityPolicy != null) {
            SecurityPolicyType securityPolicyType = archetypeSecurityPolicy.asObjectable();
            postProcessSecurityPolicy(securityPolicyType, task, result);
            return securityPolicyType;
        } else {
            return null;
        }
    }

    public <F extends FocusType> SecurityPolicyType locateGlobalSecurityPolicy(PrismObject<F> focus,
            PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (systemConfiguration != null) {
            return resolveGlobalSecurityPolicy(focus, systemConfiguration.asObjectable(), task, result);
        } else {
            return null;
        }
    }

    public SecurityPolicyType locateProjectionSecurityPolicy(ResourceObjectDefinition structuralObjectClassDefinition,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        LOGGER.trace("locateProjectionSecurityPolicy starting");
        ObjectReferenceType securityPolicyRef = structuralObjectClassDefinition.getSecurityPolicyRef();
        if (securityPolicyRef == null || securityPolicyRef.getOid() == null) {
            LOGGER.trace("Security policy not defined for the structural object class.");
            return null;
        }
        LOGGER.trace("Loading security policy {} from: {}", securityPolicyRef, structuralObjectClassDefinition);
        // TODO Use read-only option. (But deal with the fact that we modify the returned object...)
        SecurityPolicyType securityPolicy = objectResolver.resolve(securityPolicyRef, SecurityPolicyType.class,
                null, " projection security policy", task, result);
        if (securityPolicy == null) {
            LOGGER.debug("Security policy {} defined for the projection does not exist", securityPolicyRef);
            return null;
        }
        postProcessSecurityPolicy(securityPolicy, task, result);
        return securityPolicy;
    }

    private <F extends FocusType> SecurityPolicyType resolveGlobalSecurityPolicy(PrismObject<F> focus,
            SystemConfigurationType systemConfiguration, Task task, OperationResult result)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectReferenceType globalSecurityPolicyRef = systemConfiguration.getGlobalSecurityPolicyRef();
        if (globalSecurityPolicyRef != null) {
            try {
                SecurityPolicyType globalSecurityPolicyType = objectResolver.resolve(globalSecurityPolicyRef, SecurityPolicyType.class, null, "global security policy reference in system configuration", task, result);
                LOGGER.trace("Using global security policy: {}", globalSecurityPolicyType);
                postProcessSecurityPolicy(globalSecurityPolicyType, task, result);
                traceSecurityPolicy(globalSecurityPolicyType, focus);
                return globalSecurityPolicyType;
            } catch (ObjectNotFoundException | SchemaException e) {
                LOGGER.error(e.getMessage(), e);
                traceSecurityPolicy(null, focus);
                return null;
            }
        }

        return null;
    }

    private <F extends FocusType> void traceSecurityPolicy(SecurityPolicyType securityPolicyType, PrismObject<F> user) {
        if (LOGGER.isTraceEnabled()) {
            if (user != null) {
                if (securityPolicyType == null) {
                    LOGGER.trace("Located security policy for {}: null", user);
                } else {
                    LOGGER.trace("Located security policy for {}:\n{}", user, securityPolicyType.asPrismObject().debugDump(1));
                }
            } else {
                if (securityPolicyType == null) {
                    LOGGER.trace("Located global security policy null");
                } else {
                    LOGGER.trace("Located global security policy :\n{}", securityPolicyType.asPrismObject().debugDump(1));
                }
            }
        }

    }

    private void postProcessSecurityPolicy(SecurityPolicyType securityPolicyType, Task task, OperationResult result) {
        CredentialsPolicyType creds = securityPolicyType.getCredentials();
        if (creds != null) {
            PasswordCredentialsPolicyType passwd = creds.getPassword();
            if (passwd != null) {
                postProcessPasswordCredentialPolicy(securityPolicyType, passwd, task, result);
            }
            for (NonceCredentialsPolicyType nonce : creds.getNonce()) {
                postProcessCredentialPolicy(securityPolicyType, nonce, "nonce credential policy", task, result);
            }
            SecurityQuestionsCredentialsPolicyType securityQuestions = creds.getSecurityQuestions();
            if (securityQuestions != null) {
                postProcessCredentialPolicy(securityPolicyType, securityQuestions, "security questions credential policy", task, result);
            }
        }
    }

    private void postProcessPasswordCredentialPolicy(SecurityPolicyType securityPolicyType, PasswordCredentialsPolicyType passwd, Task task, OperationResult result) {
        postProcessCredentialPolicy(securityPolicyType, passwd, "password credential policy", task, result);
    }

    private ValuePolicyType postProcessCredentialPolicy(SecurityPolicyType securityPolicyType, CredentialPolicyType credPolicy, String credShortDesc, Task task, OperationResult result) {
        ObjectReferenceType valuePolicyRef = credPolicy.getValuePolicyRef();
        if (valuePolicyRef == null) {
            return null;
        }
        ValuePolicyType valuePolicyType;
        try {
            valuePolicyType = objectResolver.resolve(valuePolicyRef, ValuePolicyType.class, null, credShortDesc + " in " + securityPolicyType, task, result);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            LOGGER.warn("{} {} referenced from {} was not found", credShortDesc, valuePolicyRef.getOid(), securityPolicyType);
            return null;
        }
        valuePolicyRef.asReferenceValue().setObject(valuePolicyType.asPrismObject());
        return valuePolicyType;
    }

    private SecurityPolicyType postProcessPasswordPolicy(ValuePolicyType passwordPolicyType) {
        SecurityPolicyType securityPolicyType = new SecurityPolicyType();
        CredentialsPolicyType creds = new CredentialsPolicyType();
        PasswordCredentialsPolicyType passwd = new PasswordCredentialsPolicyType();
        ObjectReferenceType passwordPolicyRef = new ObjectReferenceType();
        passwordPolicyRef.asReferenceValue().setObject(passwordPolicyType.asPrismObject());
        passwd.setValuePolicyRef(passwordPolicyRef);
        creds.setPassword(passwd);
        securityPolicyType.setCredentials(creds);
        return securityPolicyType;
    }

    private Duration daysToDuration(int days) {
        return XmlTypeConverter.createDuration((long) days * 1000 * 60 * 60 * 24);
    }

    public SecurityEnforcer getSecurityEnforcer() {
        return securityEnforcer;
    }

    public boolean isAuthorized(List<String> actions, Task task, OperationResult result) {
        try {
            for (String action : actions) {
                boolean isAuthorized = securityEnforcer.isAuthorized(action,
                        null, AuthorizationParameters.EMPTY, null, task, result);
                if (!isAuthorized) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
