/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventRecordPayload;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.AuditConfiguration;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditEventRecordingPropertyType;

/**
 * Records connector generation service communication in the audit trail, mirroring the
 * {@code AuditingServiceClient} used for Smart service calls.
 *
 * A request audit record is written before a request is posted to the service, carrying the
 * posted data. An execution record with the same request identifier is written when the
 * request completes or fails. API polling checks (job status refreshes, documentation probes)
 * are not recorded.
 */
public final class ConndevServiceCallAuditor {

    private static final Trace LOGGER = TraceManager.getTrace(ConndevServiceCallAuditor.class);

    private static final String OP_AUDIT_EXTERNAL_SERVICE_CALL =
            ConndevServiceCallAuditor.class.getName() + ".auditExternalServiceCall";

    static final String CONTENT_TYPE_JSON = "application/json";
    static final String AUDIT_DURATION_MILLIS = "externalService.durationMillis";

    private final AuditHelper auditHelper;
    private final AuditConfiguration auditConfiguration;
    @Nullable private final Task task;
    @Nullable private final OperationResult result;
    @Nullable private final PrismObject<ConnectorDevelopmentType> development;

    private final boolean recordEvents;
    private final boolean recordData;

    public ConndevServiceCallAuditor(AuditHelper auditHelper, AuditConfiguration auditConfiguration,
            @Nullable Task task, @Nullable OperationResult result,
            @Nullable PrismObject<ConnectorDevelopmentType> development) {
        this.auditHelper = auditHelper;
        this.auditConfiguration = auditConfiguration;
        this.task = task;
        this.result = result;
        this.development = development;
        this.recordEvents = auditConfiguration.isRecordExternalServiceEvents()
                || auditConfiguration.isRecordExternalServiceData();
        this.recordData = auditConfiguration.isRecordExternalServiceData();
    }

    boolean isEnabled() {
        return recordEvents;
    }

    boolean isRecordData() {
        return recordData;
    }

    /**
     * Writes the REQUEST-stage audit record before the request is posted. Failures propagate,
     * so the request is not sent when the audit record cannot be written.
     */
    void auditRequest(String endpoint, @Nullable String requestText, @Nullable String requestContentType,
            String requestIdentifier) {

        if (!recordEvents) {
            return;
        }

        var record = createRecord(endpoint, requestIdentifier, AuditEventStage.REQUEST);

        if (requestText != null) {
            record.addPayload(
                    new AuditEventRecordPayload(
                            "request",
                            requestContentType,
                            requestText));
        }

        audit(record);
    }

    /**
     * Writes the EXECUTION-stage audit record once the request completes or fails. Failures are
     * suppressed and never mask the outcome of the service call.
     */
    void auditExecution(String endpoint, String requestIdentifier, OperationResultStatus outcome,
            String message, long startNanos) {

        if (!recordEvents) {
            return;
        }

        try {
            var record = createRecord(endpoint, requestIdentifier, AuditEventStage.EXECUTION);

            record.setOutcome(outcome);
            record.setMessage(message);
            record.addPropertyValue(
                    AUDIT_DURATION_MILLIS,
                    String.valueOf(elapsedMillis(startNanos)));

            audit(record);

        } catch (Throwable t) {
            LOGGER.warn("Couldn't audit connector generation service call execution {}", endpoint, t);
        }
    }

    private AuditEventRecord createRecord(String endpoint, String requestIdentifier, AuditEventStage stage) {

        var record = new AuditEventRecord(AuditEventType.EXTERNAL_SERVICE_CALL, stage);

        record.setRequestIdentifier(requestIdentifier);
        record.setParameter(endpoint);

        if (development != null) {
            record.setTarget(development, development.getOid());

            if (development.getOid() != null) {
                record.addResourceOid(development.getOid());
            }
        }

        return record;
    }

    private void audit(AuditEventRecord record) {

        OperationResult operationResult = auditResult();
        AuditEventRecord processedRecord = applyConfiguredAuditEventRecording(record, operationResult);
        if (processedRecord == null) {
            return;
        }

        auditHelper.audit(
                processedRecord,
                null,
                task,
                operationResult);
    }

    /**
     * Applies the configured generic audit event-recording processing to the record.
     *
     * Evaluates configured audit properties and the event-recording expression.
     * Returns {@code null} if the expression suppresses the audit event.
     */
    private @Nullable AuditEventRecord applyConfiguredAuditEventRecording(
            AuditEventRecord record, OperationResult operationResult) {

        for (SystemConfigurationAuditEventRecordingPropertyType property : auditConfiguration.getPropertiesToRecord()) {
            auditHelper.evaluateAuditRecordProperty(
                    property,
                    record,
                    development,
                    task,
                    operationResult);
        }

        if (auditConfiguration.getEventRecordingExpression() == null) {
            return record;
        }

        return auditHelper.evaluateRecordingExpression(
                auditConfiguration.getEventRecordingExpression(),
                record,
                development,
                null,
                task,
                operationResult);
    }

    private OperationResult auditResult() {
        return result != null
                ? result
                : new OperationResult(OP_AUDIT_EXTERNAL_SERVICE_CALL);
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
