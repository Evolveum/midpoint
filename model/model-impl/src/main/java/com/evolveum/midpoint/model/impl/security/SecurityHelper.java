/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.prism.polystring.PolyString;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author semancik
 */
@Component
public class SecurityHelper implements ModelAuditRecorder {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityHelper.class);

    @Autowired private TaskManager taskManager;
    @Autowired private AuditHelper auditHelper;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SystemObjectCache systemObjectCache;

    @Override
    public void auditLoginSuccess(@NotNull ObjectType object, @NotNull ConnectionEnvironment connEnv) {
        var focus = object instanceof FocusType focus1 ? focus1 : null;
        auditLogin(object.getName().getOrig(), focus, connEnv, OperationResultStatus.SUCCESS, null);
    }

    @Override
    public void auditLoginFailure(@Nullable String username, @Nullable FocusType focus, @NotNull ConnectionEnvironment connEnv, String message) {
        auditLogin(username, focus, connEnv, OperationResultStatus.FATAL_ERROR, message);
    }

    private void auditLogin(
            @Nullable String username,
            @Nullable FocusType focus,
            @NotNull ConnectionEnvironment connEnv,
            @NotNull OperationResultStatus status,
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

        try {
            auditHelper.audit(record, null, task, new OperationResult(SecurityHelper.class.getName() + ".auditLogin"));
        } catch (Exception e) {
            LOGGER.error("Couldn't audit audit event because of malformed username: " + username, e);
            String normalizedUsername = new PolyString(username).getNorm();
            LOGGER.info("Normalization of username and create audit record with normalized username. Normalized username: " + normalizedUsername);
            record.setParameter(normalizedUsername);
            auditHelper.audit(record, null, task, new OperationResult(SecurityHelper.class.getName() + ".auditLogin"));
        }
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
        try {
            auditHelper.audit(record, null, task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't audit audit event", e);
        }
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

    public SecurityEnforcer getSecurityEnforcer() {
        return securityEnforcer;
    }
}
