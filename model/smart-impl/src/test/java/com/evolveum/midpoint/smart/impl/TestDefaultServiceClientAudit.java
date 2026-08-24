/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventRecordPayload;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.smart.api.ClientCallContext;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.info.AiInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestObjectTypesRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestObjectTypesResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Integration tests for Smart service call auditing performed by {@link AuditingServiceClient}.
 */
@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDefaultServiceClientAudit extends AbstractSmartIntegrationTest {

    private static final String RESOURCE_OID = "00000000-0000-0000-0000-000000012345";

    @Autowired private AuditHelper auditHelper;

    @BeforeMethod
    public void clearAudit() {
        dummyAuditService.clear();
    }

    @Test
    public void test100SuccessfulSynchronousCallEmitsRequestAndExecution() throws Exception {
        var delegate = new RecordingServiceClient();

        var response = audited(delegate).invoke(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                request(),
                SiSuggestObjectTypesResponseType.class,
                contextWithResource());

        assertThat(response).isNotNull();
        assertThat(delegate.syncCalls).isEqualTo(1);

        var records = externalServiceCallRecords();
        assertRequestExecutionPair(records);
        assertCommon(records.get(0), AuditEventStage.REQUEST);
        assertCommon(records.get(1), AuditEventStage.EXECUTION);
        assertThat(records.get(1).getOutcome()).isEqualTo(OperationResultStatus.SUCCESS);
        assertResourceTarget(records.get(0));
        assertResourceTarget(records.get(1));
        assertRequestPayload(records.get(0));
        assertNoResponsePayload(records.get(1));
        assertDurationMillis(records.get(1));
    }

    @DataProvider
    public Object[][] smartServiceAuditConfiguration() {
        return new Object[][] {
                { true, true, true, true },
                { true, false, true, false },
                { false, true, true, true },
                { false, false, false, false }
        };
    }

    @Test(dataProvider = "smartServiceAuditConfiguration")
    public void test110SmartServiceAuditConfiguration(boolean recordEvents, boolean recordData,
            boolean expectedRecords, boolean expectedRequestPayload) throws Exception {

        var delegate = new RecordingServiceClient();

        var response = audited(delegate, recordEvents, recordData).invoke(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                recordData ? request() : unserializableRequest(),
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty());

        assertThat(response).isNotNull();
        assertThat(delegate.syncCalls).isEqualTo(1);

        var records = externalServiceCallRecords();
        if (!expectedRecords) {
            assertThat(records).isEmpty();
            return;
        }

        assertRequestExecutionPair(records);
        if (expectedRequestPayload) {
            assertRequestPayload(records.get(0));
        } else {
            assertNoRequestPayload(records.get(0));
        }
        assertNoResponsePayload(records.get(1));
    }

    @Test
    public void test112RecordDataFalseSkipsRequestSerialization() throws Exception {
        var delegate = new RecordingServiceClient();
        var unserializableRequest = unserializableRequest();

        assertThatThrownBy(() -> SmartServiceSerialization.serializeRequest(unserializableRequest))
                .isInstanceOf(RuntimeException.class);

        var response = audited(delegate, true, false).invoke(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                unserializableRequest,
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty());

        assertThat(response).isNotNull();
        assertThat(delegate.syncCalls).isEqualTo(1);

        var records = externalServiceCallRecords();
        assertRequestExecutionPair(records);
        assertNoRequestPayload(records.get(0));
        assertNoResponsePayload(records.get(1));
    }

    @Test
    public void test115MissingSmartServiceAuditConfigurationDefaultsToRecordingEventsAndData() {
        var missingSystemConfiguration = auditHelper.getAuditConfiguration(null);
        assertThat(missingSystemConfiguration.isRecordSmartServiceEvents()).isTrue();
        assertThat(missingSystemConfiguration.isRecordSmartServiceData()).isTrue();

        var missingAuditConfiguration = auditHelper.getAuditConfiguration(new SystemConfigurationType());
        assertThat(missingAuditConfiguration.isRecordSmartServiceEvents()).isTrue();
        assertThat(missingAuditConfiguration.isRecordSmartServiceData()).isTrue();
    }

    @Test
    public void test120FailedCallEmitsExecutionFailureAndPreservesException() {
        var original = new SystemException("service failed");
        var delegate = new RecordingServiceClient(original);

        assertThatThrownBy(() -> audited(delegate).invoke(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                request(),
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty()))
                .isSameAs(original);

        var records = externalServiceCallRecords();
        assertRequestExecutionPair(records);
        assertThat(records.get(1).getOutcome()).isEqualTo(OperationResultStatus.FATAL_ERROR);
        assertNoResponsePayload(records.get(1));
        assertDurationMillis(records.get(1));
    }

    @Test
    public void test130RequestAuditFailurePropagatesAndSkipsDelegate() {
        var delegate = new RecordingServiceClient();
        var auditFailure = new SystemException("audit failed");

        assertThatThrownBy(() -> audited(delegate, new ThrowingAuditHelper(auditFailure, AuditEventStage.REQUEST)).invoke(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                request(),
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty()))
                .isSameAs(auditFailure);

        assertThat(delegate.syncCalls).isZero();
    }

    @Test
    public void test135AsyncRequestAuditFailureCompletesExceptionallyAndSkipsDelegate() {
        var delegate = new RecordingServiceClient();
        var auditFailure = new SystemException("audit failed");

        var future = audited(delegate, new ThrowingAuditHelper(auditFailure, AuditEventStage.REQUEST)).invokeAsync(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                request(),
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty());

        assertThat(future).isCompletedExceptionally();
        assertThat(delegate.asyncCalls).isZero();
        assertThat(externalServiceCallRecords()).isEmpty();
    }

    @Test
    public void test140ExecutionAuditFailureDoesNotMaskSuccessfulCall() throws Exception {
        var delegate = new RecordingServiceClient();

        var response = audited(
                delegate,
                new ThrowingAuditHelper(new SystemException("execution audit failed"), AuditEventStage.EXECUTION))
                .invoke(
                        ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                        request(),
                        SiSuggestObjectTypesResponseType.class,
                        ClientCallContext.empty());

        assertThat(response).isNotNull();
        assertThat(delegate.syncCalls).isEqualTo(1);
    }

    @Test
    public void test150ExecutionAuditFailureDoesNotMaskServiceCallException() {
        var original = new SystemException("service failed");
        var delegate = new RecordingServiceClient(original);

        assertThatThrownBy(() -> audited(
                delegate,
                new ThrowingAuditHelper(new SystemException("execution audit failed"), AuditEventStage.EXECUTION))
                .invoke(
                        ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                        request(),
                        SiSuggestObjectTypesResponseType.class,
                        ClientCallContext.empty()))
                .isSameAs(original);
    }

    @Test
    public void test160AsyncCallEmitsSingleRequestExecutionPair() {
        var delegate = new RecordingServiceClient();

        audited(delegate).invokeAsync(
                        ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                        request(),
                        SiSuggestObjectTypesResponseType.class,
                        contextWithResource())
                .join();

        assertThat(delegate.asyncCalls).isEqualTo(1);

        var records = externalServiceCallRecords();
        assertRequestExecutionPair(records);
        assertThat(records).hasSize(2);
        assertDurationMillis(records.get(1));
    }

    @Test
    public void test170AsyncSynchronousStartFailureEmitsExecutionFailure() {
        var original = new SystemException("async start failed");
        var delegate = new RecordingServiceClient();
        delegate.asyncStartFailure = original;

        assertThatThrownBy(() -> audited(delegate).invokeAsync(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                request(),
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty()))
                .isSameAs(original);

        var records = externalServiceCallRecords();
        assertRequestExecutionPair(records);
        assertThat(records.get(1).getOutcome()).isEqualTo(OperationResultStatus.FATAL_ERROR);
    }

    @Test
    public void test175AsyncExceptionalCompletionEmitsExecutionFailure() {
        var original = new SystemException("async failed");
        var delegate = new RecordingServiceClient(original);

        var future = audited(delegate).invokeAsync(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                request(),
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty());

        assertThat(delegate.asyncCalls).isEqualTo(1);
        assertThat(future).isCompletedExceptionally();

        var records = externalServiceCallRecords();
        assertRequestExecutionPair(records);
        assertThat(records.get(1).getOutcome()).isEqualTo(OperationResultStatus.FATAL_ERROR);
        assertNoResponsePayload(records.get(1));
    }

    @Test
    public void test180EmptyContextDoesNotCrashAndHasNoResourceTarget() throws Exception {
        var delegate = new RecordingServiceClient();

        audited(delegate).invoke(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                request(),
                SiSuggestObjectTypesResponseType.class,
                ClientCallContext.empty());

        var records = externalServiceCallRecords();
        assertRequestExecutionPair(records);
        assertThat(records.get(0).getTargetRef()).isNull();
        assertThat(records.get(1).getTargetRef()).isNull();
        assertThat(records.get(0).getResourceOids()).isEmpty();
        assertThat(records.get(1).getResourceOids()).isEmpty();
    }

    @Test
    public void test190ServiceClientCompatibilityInvokeProvidesEmptyContext() throws Exception {
        var client = new MockServiceClientImpl(new SiSuggestObjectTypesResponseType());

        client.invoke(
                ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                new SiSuggestObjectTypesRequestType(),
                SiSuggestObjectTypesResponseType.class);

        var context = client.getLastCallContext();
        assertThat(client.getLastMethod()).isEqualTo(ServiceClient.Method.SUGGEST_OBJECT_TYPES);
        assertThat(context.task()).isNull();
        assertThat(context.result()).isNull();
        assertThat(context.resource()).isNull();
    }

    @Test
    public void test195ServiceClientCompatibilityInvokeAsyncProvidesEmptyContext() {
        var client = new MockServiceClientImpl(new SiSuggestObjectTypesResponseType());

        client.invokeAsync(
                        ServiceClient.Method.SUGGEST_OBJECT_TYPES,
                        new SiSuggestObjectTypesRequestType(),
                        SiSuggestObjectTypesResponseType.class)
                .join();

        var context = client.getLastCallContext();
        assertThat(client.getLastMethod()).isEqualTo(ServiceClient.Method.SUGGEST_OBJECT_TYPES);
        assertThat(context.task()).isNull();
        assertThat(context.result()).isNull();
        assertThat(context.resource()).isNull();
    }

    private AuditingServiceClient audited(ServiceClient delegate) {
        return audited(delegate, auditHelper);
    }

    private AuditingServiceClient audited(ServiceClient delegate, AuditHelper auditHelper) {
        return new AuditingServiceClient(delegate, auditHelper);
    }

    private AuditingServiceClient audited(ServiceClient delegate, boolean recordEvents, boolean recordData) {
        return new AuditingServiceClient(delegate, auditHelper, recordEvents, recordData);
    }

    private SiSuggestObjectTypesRequestType request() {
        return new SiSuggestObjectTypesRequestType();
    }

    private Object unserializableRequest() {
        return new Object();
    }

    private ClientCallContext contextWithResource() {
        return ClientCallContext.of(getTestTask(), getTestTask().getResult(), new ResourceType().oid(RESOURCE_OID));
    }

    private List<AuditEventRecord> externalServiceCallRecords() {
        return dummyAuditService.getRecordsOfType(AuditEventType.EXTERNAL_SERVICE_CALL);
    }

    private void assertRequestExecutionPair(List<AuditEventRecord> records) {
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getEventStage()).isEqualTo(AuditEventStage.REQUEST);
        assertThat(records.get(1).getEventStage()).isEqualTo(AuditEventStage.EXECUTION);
        assertThat(records.get(0).getRequestIdentifier()).isNotBlank();
        assertThat(records.get(1).getRequestIdentifier()).isEqualTo(records.get(0).getRequestIdentifier());
    }

    private void assertCommon(AuditEventRecord record, AuditEventStage stage) {
        assertThat(record.getEventType()).isEqualTo(AuditEventType.EXTERNAL_SERVICE_CALL);
        assertThat(record.getEventStage()).isEqualTo(stage);
        assertThat(record.getParameter()).isEqualTo(ServiceClient.Method.SUGGEST_OBJECT_TYPES.name());
    }

    private void assertResourceTarget(AuditEventRecord record) {
        assertThat(record.getTargetRef()).isNotNull();
        assertThat(record.getTargetRef().getOid()).isEqualTo(RESOURCE_OID);
        assertThat(record.getResourceOids()).containsExactly(RESOURCE_OID);
    }

    private void assertRequestPayload(AuditEventRecord record) {
        AuditEventRecordPayload payload = getPayload(record, "request");

        assertThat(payload.getContentType()).isEqualTo("application/json");
        assertThat(payload.getContent()).isNotBlank();
    }

    private void assertNoResponsePayload(AuditEventRecord record) {
        assertNoPayload(record, "response");
    }

    private void assertNoRequestPayload(AuditEventRecord record) {
        assertNoPayload(record, "request");
    }

    private void assertNoPayload(AuditEventRecord record, String name) {
        assertThat(record.getPayloads())
                .extracting(AuditEventRecordPayload::getName)
                .doesNotContain(name);
    }

    private void assertDurationMillis(AuditEventRecord record) {
        assertThat(record.getPropertyValues("externalService.durationMillis"))
                .singleElement()
                .satisfies(value -> assertThat(Long.parseLong(value)).isNotNegative());
    }

    private AuditEventRecordPayload getPayload(AuditEventRecord record, String name) {
        return record.getPayloads().stream()
                .filter(payload -> name.equals(payload.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No payload named " + name));
    }

    private static class RecordingServiceClient implements ServiceClient {

        private final RuntimeException failure;
        private RuntimeException asyncStartFailure;
        private int syncCalls;
        private int asyncCalls;

        private RecordingServiceClient() {
            this(null);
        }

        private RecordingServiceClient(@Nullable RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public <REQ, RESP> RESP invoke(
                Method method, REQ request, Class<RESP> responseClass, ClientCallContext callContext)
                throws SchemaException {
            syncCalls++;
            if (failure != null) {
                throw failure;
            }
            return response(responseClass);
        }

        @Override
        public <REQ, RESP> CompletableFuture<RESP> invokeAsync(
                Method method, REQ request, Class<RESP> responseClass, ClientCallContext callContext) {
            asyncCalls++;
            if (asyncStartFailure != null) {
                throw asyncStartFailure;
            }
            if (failure != null) {
                return CompletableFuture.failedFuture(failure);
            }
            return CompletableFuture.completedFuture(response(responseClass));
        }

        @Override
        public Optional<AiInfo> getAiInfo() {
            return Optional.empty();
        }

        @Override
        public void close() {
        }

        private static <RESP> RESP response(Class<RESP> responseClass) {
            try {
                return responseClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new SystemException("Couldn't instantiate test response", e);
            }
        }
    }

    private static class ThrowingAuditHelper extends AuditHelper {

        private final RuntimeException failure;
        private final AuditEventStage throwingStage;

        private ThrowingAuditHelper(RuntimeException failure, AuditEventStage throwingStage) {
            this.failure = failure;
            this.throwingStage = throwingStage;
        }

        @Override
        public void audit(
                AuditEventRecord record,
                @Nullable ObjectDeltaSchemaLevelUtil.NameResolver externalNameResolver,
                Task task,
                OperationResult parentResult) {
            if (record.getEventStage() == throwingStage) {
                throw failure;
            }
        }
    }
}
