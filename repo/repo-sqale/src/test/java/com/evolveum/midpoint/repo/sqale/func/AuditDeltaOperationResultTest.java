/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.*;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the System Configuration audit/eventRecording/deltaSuccessExecutionResult setting.
 */
public class AuditDeltaOperationResultTest extends SqaleRepoBaseTest {

    @AfterClass
    public void resetAuditConfig() {
        // Without this other audit tests would have to care.
        auditService.applyAuditConfiguration(null);
    }

    @Test
    public void test100DefaultAuditConfiguration() throws Exception {
        OperationResult result = createOperationResult();

        given("clear audit and default audit configuration");
        clearAudit();
        auditService.applyAuditConfiguration(null);

        when("audit event with delta operation executions is stored");
        createAuditRecordWithDeltas(result);

        then("success result for delta is cleaned up");
        QAuditEventRecord a = QAuditEventRecordMapping.get().defaultAlias();
        assertThat(selectOne(a)).isNotNull();

        QAuditDelta d = QAuditDeltaMapping.get().defaultAlias();
        List<MAuditDelta> deltaRows = select(d);
        assertThat(deltaRows).hasSize(2);
        List<OperationResultType> deltaResults = deltaRows.stream()
                .map(row -> (OperationResultType) parseFullObject(row.fullResult))
                .collect(Collectors.toList());
        assertThat(deltaResults)
                .filteredOn(res -> res.getOperation().equals("delta-op2"))
                .singleElement() // hasSize(1) + first() shortcut
                .matches(res -> res.getStatus().equals(OperationResultStatusType.SUCCESS))
                .extracting(OperationResultType::getPartialResults, listAsserterFactory(OperationResultType.class))
                .hasSize(2)
                .anyMatch(s -> s.getOperation().equals("sub-op2-1")) // minor non-success must stay
                .anyMatch(s -> s.getOperation().equals("sub-op2-3")); // non-minor success stays too

        and("non-success result has all subresults");
        assertThat(deltaResults)
                .filteredOn(res -> res.getOperation().equals("delta-op1"))
                .singleElement()
                .matches(res -> res.getStatus().equals(OperationResultStatusType.WARNING))
                .extracting(OperationResultType::getPartialResults, listAsserterFactory(OperationResultType.class))
                .hasSize(3); // everything is there
    }

    @Test
    public void test110StoringFullSuccessResults() throws Exception {
        OperationResult result = createOperationResult();

        given("clear audit and audit configuration set to FULL");
        clearAudit();
        auditService.applyAuditConfiguration(new SystemConfigurationAuditType()
                .eventRecording(new SystemConfigurationAuditEventRecordingType()
                        .deltaSuccessExecutionResult(OperationResultDetailLevel.FULL)));

        when("audit event with delta operation executions is stored");
        createAuditRecordWithDeltas(result);

        then("success result for delta preserved completely");
        QAuditEventRecord a = QAuditEventRecordMapping.get().defaultAlias();
        assertThat(selectOne(a)).isNotNull();

        QAuditDelta d = QAuditDeltaMapping.get().defaultAlias();
        List<MAuditDelta> deltaRows = select(d);
        assertThat(deltaRows).hasSize(2)
                .extracting(row -> (OperationResultType) parseFullObject(row.fullResult))
                .allMatch(ort -> ort.getPartialResults().size() == 3); // both results have all subresults
    }

    @Test
    public void test120StoringNoneSuccessResults() throws Exception {
        OperationResult result = createOperationResult();

        given("clear audit and audit configuration set to NONE");
        clearAudit();
        auditService.applyAuditConfiguration(new SystemConfigurationAuditType()
                .eventRecording(new SystemConfigurationAuditEventRecordingType()
                        .deltaSuccessExecutionResult(OperationResultDetailLevel.NONE)));

        when("audit event with delta operation executions is stored");
        createAuditRecordWithDeltas(result);

        then("success result for delta preserved completely");
        QAuditEventRecord a = QAuditEventRecordMapping.get().defaultAlias();
        assertThat(selectOne(a)).isNotNull();

        QAuditDelta d = QAuditDeltaMapping.get().defaultAlias();
        List<MAuditDelta> deltaRows = select(d);
        assertThat(deltaRows).hasSize(2)
                .anyMatch(row -> row.fullResult == null && row.status == OperationResultStatusType.SUCCESS)
                .filteredOn(row -> row.fullResult != null)
                .extracting(row -> (OperationResultType) parseFullObject(row.fullResult))
                .singleElement()
                .matches(ort -> ort.getPartialResults().size() == 3 // warning is preserved fully
                        && ort.getStatus() == OperationResultStatusType.WARNING);
    }

    @Test
    public void test130StoringOnlyTopSuccessResults() throws Exception {
        OperationResult result = createOperationResult();

        given("clear audit and audit configuration set to TOP");
        clearAudit();
        auditService.applyAuditConfiguration(new SystemConfigurationAuditType()
                .eventRecording(new SystemConfigurationAuditEventRecordingType()
                        .deltaSuccessExecutionResult(OperationResultDetailLevel.TOP)));

        when("audit event with delta operation executions is stored");
        createAuditRecordWithDeltas(result);

        then("success result for delta preserved completely");
        QAuditEventRecord a = QAuditEventRecordMapping.get().defaultAlias();
        assertThat(selectOne(a)).isNotNull();

        QAuditDelta d = QAuditDeltaMapping.get().defaultAlias();
        List<MAuditDelta> deltaRows = select(d);
        assertThat(deltaRows).hasSize(2)
                .extracting(row -> (OperationResultType) parseFullObject(row.fullResult))
                .anyMatch(ort -> ort.getPartialResults().size() == 3 // warning is preserved fully
                        && ort.getStatus() == OperationResultStatusType.WARNING)
                .anyMatch(ort -> ort.getPartialResults().isEmpty() // no subresults for success
                        && ort.getStatus() == OperationResultStatusType.SUCCESS);
    }

    @Test
    public void test140StoringCleanedUpSuccessResults() throws Exception {
        // this is like default without any settings
        OperationResult result = createOperationResult();

        given("clear audit and audit configuration set to TOP");
        clearAudit();
        auditService.applyAuditConfiguration(new SystemConfigurationAuditType()
                .eventRecording(new SystemConfigurationAuditEventRecordingType()
                        .deltaSuccessExecutionResult(OperationResultDetailLevel.CLEANED_UP)));

        when("audit event with delta operation executions is stored");
        createAuditRecordWithDeltas(result);

        then("success result for delta preserved completely");
        QAuditEventRecord a = QAuditEventRecordMapping.get().defaultAlias();
        assertThat(selectOne(a)).isNotNull();

        QAuditDelta d = QAuditDeltaMapping.get().defaultAlias();
        List<MAuditDelta> deltaRows = select(d);
        assertThat(deltaRows).hasSize(2)
                .extracting(row -> (OperationResultType) parseFullObject(row.fullResult))
                .anyMatch(ort -> ort.getPartialResults().size() == 3 // warning is preserved fully
                        && ort.getStatus() == OperationResultStatusType.WARNING)
                // two subresults are left, see test100 for details
                .anyMatch(ort -> ort.getPartialResults().size() == 2
                        && ort.getStatus() == OperationResultStatusType.SUCCESS);
    }

    private void createAuditRecordWithDeltas(OperationResult result) throws SchemaException {
        AuditEventRecord record = new AuditEventRecord();
        ObjectDeltaOperation<UserType> delta1 = createDelta(UserType.F_FULL_NAME);
        OperationResult opResult1 = new OperationResult("delta-op1", OperationResultStatus.WARNING, "warn message");
        opResult1.subresult("sub-op1-1").setMinor().build().setStatus(OperationResultStatus.PARTIAL_ERROR);
        opResult1.subresult("sub-op1-2").setMinor().build().setSuccess();
        opResult1.subresult("sub-op1-3").build().setSuccess();
        delta1.setExecutionResult(opResult1);
        record.addDelta(delta1);

        ObjectDeltaOperation<UserType> delta2 = createDelta(UserType.F_ADDITIONAL_NAME);
        OperationResult opResult2 = new OperationResult("delta-op2", OperationResultStatus.SUCCESS, "message");
        opResult2.subresult("sub-op2-1").setMinor().build().setStatus(OperationResultStatus.PARTIAL_ERROR);
        opResult2.subresult("sub-op2-2").setMinor().build().setSuccess();
        opResult2.subresult("sub-op2-3").build().setSuccess();
        delta2.setExecutionResult(opResult2);
        record.addDelta(delta2);
        auditService.audit(record, NullTaskImpl.INSTANCE, result);
    }

    @NotNull
    private ObjectDeltaOperation<UserType> createDelta(ItemPath itemPath, Object... values)
            throws SchemaException {
        ObjectDeltaOperation<UserType> delta = new ObjectDeltaOperation<>();
        delta.setObjectDelta(prismContext.deltaFor(UserType.class)
                .item(itemPath).replace(values)
                .asObjectDelta(UUID.randomUUID().toString()));
        return delta;
    }
}
