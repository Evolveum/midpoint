/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static java.util.Collections.emptyList;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ShadowAuditHelper {

    @Autowired private ProvisioningService provisioningService;
    @Autowired private AuditHelper auditHelper;

    public void auditEvent(AuditEventType event, ShadowType shadow, ProvisioningContext ctx, OperationResult result) {
        Task task = ctx.getTask();

        SystemConfigurationType config = provisioningService.getSystemConfiguration();
        if (!isEnhancedShadowAuditingEnabled(config)) {
            return;
        }

        ProvisioningOperationContext operationContext = ctx.getOperationContext();
        AuditEventRecord auditRecord = new AuditEventRecord(event, AuditEventStage.RESOURCE);
        auditRecord.setRequestIdentifier(operationContext.requestIdentifier());

        boolean recordResourceOids;
        List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord;
        ExpressionType eventRecordingExpression = null;

        if (config != null && config.getAudit() != null && config.getAudit().getEventRecording() != null) {
            SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
            recordResourceOids = Boolean.TRUE.equals(eventRecording.isRecordResourceOids());
            propertiesToRecord = eventRecording.getProperty();
            eventRecordingExpression = eventRecording.getExpression();
        } else {
            recordResourceOids = false;
            propertiesToRecord = emptyList();
        }

        // todo channel
        auditRecord.setTimestamp(Clock.get().currentTimeMillis());

//        addRecordMessage(auditRecord, clone.getMessage());    // todo add message and other fields to event

        for (SystemConfigurationAuditEventRecordingPropertyType property : propertiesToRecord) {
            auditHelper.evaluateAuditRecordProperty(property, auditRecord, shadow.asPrismObject(),
                    operationContext.expressionProfile(), task, result);
        }

        if (eventRecordingExpression != null) {
            // MID-6839
            auditRecord = auditHelper.evaluateRecordingExpression(eventRecordingExpression, auditRecord, shadow.asPrismObject(),
                    operationContext.expressionProfile(), operationContext.expressionEnvironment(), ctx.getTask(), result);
        }

        if (auditRecord == null) {
            return;
        }

        ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = operationContext.nameResolver();

        auditHelper.audit(auditRecord, nameResolver, ctx.getTask(), result);
    }

    private boolean isEnhancedShadowAuditingEnabled(SystemConfigurationType config) {
        if (config == null) {
            return false;
        }

        if (config.getAudit() == null || config.getAudit().getEventRecording() == null) {
            return true;
        }

        SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
        return BooleanUtils.isNotFalse(eventRecording.isRecordEnhancedShadowChanges());
    }
}
