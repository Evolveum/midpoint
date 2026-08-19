/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventRecordPayload;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.smart.api.ClientCallContext;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.info.AiInfo;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * {@link ServiceClient} decorator that records Smart service communication in the audit trail.
 *
 * A request audit record is written before invoking the delegate. An execution record with the same
 * request identifier is written after the call succeeds or fails.
 */
class AuditingServiceClient implements ServiceClient {

    private static final Trace LOGGER = TraceManager.getTrace(AuditingServiceClient.class);

    private static final String OP_AUDIT_SMART_SERVICE_CALL =
            AuditingServiceClient.class.getName() + ".auditSmartServiceCall";

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String AUDIT_DURATION_MILLIS = "smartService.durationMillis";

    private final ServiceClient delegate;
    private final AuditHelper auditHelper;
    private final boolean recordEvents;
    private final boolean recordData;

    AuditingServiceClient(ServiceClient delegate, AuditHelper auditHelper) {
        this(delegate, auditHelper, true, true);
    }

    AuditingServiceClient(ServiceClient delegate, AuditHelper auditHelper, boolean recordEvents, boolean recordData) {
        this.delegate = delegate;
        this.auditHelper = auditHelper;
        this.recordEvents = recordEvents || recordData;
        this.recordData = recordData;
    }

    @Override
    public <REQ, RESP> RESP invoke(Method method, REQ request, Class<RESP> responseClass,
            ClientCallContext callContext) throws SchemaException {

        if (!recordEvents) {
            return delegate.invoke(method, request, responseClass, callContext);
        }

        String requestText = recordData ? SmartServiceSerialization.serializeRequest(request) : null;
        String requestIdentifier = generateRequestIdentifier();

        // This must succeed before potentially sensitive data is sent to the Smart service.
        auditRequest(method, callContext, requestIdentifier, requestText);

        long startNanos = System.nanoTime();

        try {
            RESP response = delegate.invoke(method, request, responseClass, callContext);

            auditExecutionSuppressingFailures(
                    method,
                    callContext,
                    requestIdentifier,
                    OperationResultStatus.SUCCESS,
                    "Smart service call %s succeeded".formatted(method),
                    startNanos);

            return response;

        } catch (SchemaException | RuntimeException e) {
            auditExecutionSuppressingFailures(
                    method,
                    callContext,
                    requestIdentifier,
                    OperationResultStatus.FATAL_ERROR,
                    "Smart service call %s failed: %s".formatted(method, e.getMessage()),
                    startNanos);

            throw e;
        }
    }

    @Override
    public <REQ, RESP> CompletableFuture<RESP> invokeAsync(Method method, REQ request,
            Class<RESP> responseClass, ClientCallContext callContext) {

        if (!recordEvents) {
            return delegate.invokeAsync(method, request, responseClass, callContext);
        }

        String requestText;
        String requestIdentifier;

        try {
            requestText = recordData ? SmartServiceSerialization.serializeRequest(request) : null;
            requestIdentifier = generateRequestIdentifier();

            // This must succeed before potentially sensitive data is sent to the Smart service.
            auditRequest(method, callContext, requestIdentifier, requestText);

        } catch (SchemaException | RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }

        long startNanos = System.nanoTime();

        CompletableFuture<RESP> delegateFuture;
        try {
            delegateFuture = delegate.invokeAsync(method, request, responseClass, callContext);
        } catch (RuntimeException e) {
            auditExecutionSuppressingFailures(
                    method,
                    callContext,
                    requestIdentifier,
                    OperationResultStatus.FATAL_ERROR,
                    "Smart service call %s failed: %s".formatted(method, e.getMessage()),
                    startNanos);

            throw e;
        }

        CompletableFuture<RESP> auditedFuture = new CompletableFuture<>();

        delegateFuture.whenComplete((response, throwable) -> {
            if (throwable != null) {
                auditExecutionSuppressingFailures(
                        method,
                        callContext,
                        requestIdentifier,
                        OperationResultStatus.FATAL_ERROR,
                        "Smart service call %s failed: %s".formatted(
                                method, rootCauseMessage(throwable)),
                        startNanos);

                auditedFuture.completeExceptionally(throwable);

            } else {
                auditExecutionSuppressingFailures(
                        method,
                        callContext,
                        requestIdentifier,
                        OperationResultStatus.SUCCESS,
                        "Smart service call %s succeeded".formatted(method),
                        startNanos);

                auditedFuture.complete(response);
            }
        });

        return auditedFuture;
    }

    @Override
    public Optional<AiInfo> getAiInfo() {
        return delegate.getAiInfo();
    }

    @Override
    public void close() {
        delegate.close();
    }

    private void auditRequest(Method method, ClientCallContext callContext,
            String requestIdentifier, @Nullable String requestText) {

        var record = createRecord(
                method,
                callContext,
                requestIdentifier,
                AuditEventStage.REQUEST);

        if (requestText != null) {
            record.addPayload(
                    new AuditEventRecordPayload(
                            "request",
                            CONTENT_TYPE_JSON,
                            requestText));
        }

        audit(record, callContext);
    }

    private void auditExecutionSuppressingFailures(Method method, ClientCallContext callContext, String requestIdentifier,
            OperationResultStatus outcome, String message, long startNanos) {

        try {
            var record = createRecord(
                    method,
                    callContext,
                    requestIdentifier,
                    AuditEventStage.EXECUTION);

            record.setOutcome(outcome);
            record.setMessage(message);
            record.addPropertyValue(
                    AUDIT_DURATION_MILLIS,
                    String.valueOf(elapsedMillis(startNanos)));

            audit(record, callContext);

        } catch (Throwable t) {
            LOGGER.warn("Couldn't audit Smart service call execution {}", method, t);
        }
    }

    private AuditEventRecord createRecord(Method method, ClientCallContext callContext,
            String requestIdentifier, AuditEventStage stage) {

        var record = new AuditEventRecord(
                AuditEventType.SMART_SERVICE_CALL,
                stage);

        record.setRequestIdentifier(requestIdentifier);
        record.setParameter(method.name());

        addResourceTarget(record, callContext.resource());

        return record;
    }

    private void addResourceTarget(AuditEventRecord record, @Nullable ResourceType resource) {

        if (resource == null) {
            return;
        }

        record.setTarget(resource.asPrismObject(), resource.getOid());

        if (resource.getOid() != null) {
            record.addResourceOid(resource.getOid());
        }
    }

    private void audit(AuditEventRecord record, ClientCallContext callContext) {

        auditHelper.audit(
                record,
                null,
                callContext.task(),
                auditResult(callContext));
    }

    private OperationResult auditResult(ClientCallContext callContext) {
        return callContext.result() != null
                ? callContext.result()
                : new OperationResult(OP_AUDIT_SMART_SERVICE_CALL);
    }

    private static String generateRequestIdentifier() {
        return UUID.randomUUID().toString();
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable unwrapped =
                throwable instanceof CompletionException && throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable;

        return unwrapped.getMessage();
    }
}
