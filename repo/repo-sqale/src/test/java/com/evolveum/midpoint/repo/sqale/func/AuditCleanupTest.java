/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.querydsl.core.types.dsl.NumberExpression;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecordMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Just a functional test, not performance.
 */
@SuppressWarnings("unchecked")
public class AuditCleanupTest extends SqaleRepoBaseTest {

    /**
     * Prepares `count` audit records with timestamp starting with specified value
     * and going up by second for each item.
     * Removes any previous audit records for consistency.
     */
    private void prepareAuditRecords(long startTimestamp, int count, OperationResult result)
            throws SchemaException {
        clearAudit();

        long timestamp = startTimestamp;
        for (int i = 1; i <= count; i++) {
            AuditEventRecord record = new AuditEventRecord();
            record.setParameter(String.valueOf(i));
            record.setTimestamp(timestamp);
            record.getCustomColumnProperty().put("foo", "foo-value");

            // to check that child tables (delta and refs) are also cleared
            ObjectDeltaOperation<UserType> delta = new ObjectDeltaOperation<>();
            delta.setObjectDelta(prismContext.deltaFor(UserType.class)
                    .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("newVal"))
                    .asObjectDelta(UUID.randomUUID().toString()));
            record.addDelta(delta);

            record.addReferenceValue("ref1",
                    ObjectTypeUtil.createObjectRef(UUID.randomUUID().toString(), ObjectTypes.USER)
                            .asReferenceValue());

            auditService.audit(record, NullTaskImpl.INSTANCE, result);

            timestamp += 1000;
        }
    }

    // These tests rely on uninterrupted ID series, but cleanup works even with holes
    @Test
    public void test100CleanupByCount() throws SchemaException {
        given("audit has 100 records");
        OperationResult operationResult = createOperationResult();
        prepareAuditRecords(System.currentTimeMillis(), 100, operationResult);
        QAuditEventRecord qae = QAuditEventRecordMapping.get().defaultAlias();
        long maxId = selectMinMaxId(qae, qae.id.max());

        when("audit cleanup is called to leave 50 records");
        int recordsToLeave = 50;
        auditService.cleanupAudit(new CleanupPolicyType()
                .maxRecords(recordsToLeave), operationResult);

        then("operation is success and only 50 records are left");
        assertThatOperationResult(operationResult).isSuccess();
        assertCount(qae, recordsToLeave);
        long minId = selectMinMaxId(qae, qae.id.min());
        // top IDs are left, that is the newest records
        assertThat(maxId - minId).isEqualTo(recordsToLeave - 1);
    }

    @Test
    public void test101CleanupByHighCount() throws SchemaException {
        given("audit has 100 records");
        OperationResult operationResult = createOperationResult();
        prepareAuditRecords(System.currentTimeMillis(), 100, operationResult);
        QAuditEventRecord qae = QAuditEventRecordMapping.get().defaultAlias();

        when("audit cleanup is called to leave 150 records");
        int recordsToLeave = 150;
        auditService.cleanupAudit(new CleanupPolicyType()
                .maxRecords(recordsToLeave), operationResult);

        then("operation is success and nothing is deleted");
        assertThatOperationResult(operationResult).isSuccess();
        assertCount(qae, 100); // original count
    }

    @Test
    public void test102CleanupByZeroCount() throws SchemaException {
        given("audit has 100 records");
        OperationResult operationResult = createOperationResult();
        prepareAuditRecords(System.currentTimeMillis(), 100, operationResult);
        QAuditEventRecord qae = QAuditEventRecordMapping.get().defaultAlias();

        when("audit cleanup is called to leave 0 records");
        int recordsToLeave = 0;
        auditService.cleanupAudit(new CleanupPolicyType()
                .maxRecords(recordsToLeave), operationResult);

        then("operation is success and everything is deleted");
        assertThatOperationResult(operationResult).isSuccess();
        assertCount(qae, 0);
    }

    @Test
    public void test200CleanupByAge() throws SchemaException {
        given("audit has 100 records across the last 100s");
        OperationResult operationResult = createOperationResult();
        long startTimestamp = System.currentTimeMillis() - 100_000;
        prepareAuditRecords(startTimestamp, 100, operationResult);
        QAuditEventRecord qae = QAuditEventRecordMapping.get().defaultAlias();

        when("audit cleanup is called to leave just last 1 minute");
        auditService.cleanupAudit(new CleanupPolicyType()
                .maxAge(XmlTypeConverter.createDuration("PT1M")), operationResult);

        then("operation is success and only the last minute of records are left");
        assertThatOperationResult(operationResult).isSuccess();
        // it's probably 59, some time passed between last audit and the cleanup call
        assertThat(count(qae)).isLessThanOrEqualTo(60)
                .isGreaterThan(55); // but something should be there, this is extreme 5s leeway
        assertThat(count(qae, qae.timestamp.lt(Instant.ofEpochMilli(startTimestamp + 40_000))))
                .isZero(); // start + 40s should be < now - 60s, it should be all gone
    }

    private long selectMinMaxId(QAuditEventRecord qae, NumberExpression<Long> minMaxPath) {
        try (JdbcSession jdbcSession = startReadOnlyTransaction()) {
            return jdbcSession.newQuery()
                    .select(minMaxPath)
                    .from(qae)
                    .fetchOne();
        }
    }
}
