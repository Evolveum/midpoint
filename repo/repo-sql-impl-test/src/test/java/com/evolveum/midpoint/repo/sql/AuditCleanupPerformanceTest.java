/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditCleanupPerformanceTest extends BaseSQLRepoTest {

    private static final int RECORDS = 50000;

    private static final boolean DO_CREATE = true;
    private static final boolean DO_CLEANUP = true;

    @Test
    public void testAuditCleanup() throws Exception {
        //GIVEN
        if (DO_CREATE) {
            prepareAuditEventRecords();
        }

        if (DO_CLEANUP) {
            //WHEN
            CleanupPolicyType policy = new CleanupPolicyType().maxRecords(1);

            OperationResult result = new OperationResult("Cleanup audit");
            long cleanupStart = System.currentTimeMillis();
            auditService.cleanupAudit(policy, result);
            long cleanupDuration = System.currentTimeMillis() - cleanupStart;
            System.out.println("Cleanup done in " + cleanupDuration + " ms (" + cleanupDuration / (RECORDS - 1) + " ms per record)");
            result.recomputeStatus();

            //THEN
            assertAuditEventRecord(result);
        }
    }

    private void assertAuditEventRecord(OperationResult result) {
        AssertJUnit.assertTrue(result.isSuccess());
        try (Session session = getFactory().openSession()) {
            session.beginTransaction();
            Query query = session.createQuery("from " + RAuditEventRecord.class.getSimpleName());
            List<RAuditEventRecord> records = query.list();
            AssertJUnit.assertEquals(1, records.size());
            session.getTransaction().commit();
        }
    }

    private void prepareAuditEventRecords() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < RECORDS;) {
            AuditEventRecord record = new AuditEventRecord();
            record.addDelta(createObjectDeltaOperation(i));
            record.setTimestamp(System.currentTimeMillis());
            record.addPropertyValue("prop1", "val1");
            record.addReferenceValue("ref1", ObjectTypeUtil.createObjectRef("oid1", ObjectTypes.USER).asReferenceValue());
            auditService.audit(record, new NullTaskImpl());
            i++;
            if (i%1000 == 0 || i == RECORDS) {
                long duration = System.currentTimeMillis() - start;
                System.out.println(i + " records created in " + duration + " ms (" + duration / i + " ms per record)");
            }
        }
        try (Session session = getFactory().openSession()) {
            session.beginTransaction();
            Query query = session.createQuery("select count(*) from " + RAuditEventRecord.class.getSimpleName());
            Long count = (Long) query.uniqueResult();
            AssertJUnit.assertEquals(RECORDS, (long) count);
            session.getTransaction().commit();
        }
    }

    private ObjectDeltaOperation<UserType> createObjectDeltaOperation(int i) throws Exception {
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("d" + i)
                .asObjectDeltaCast("oid-" + i);
        ObjectDeltaOperation<UserType> odo = new ObjectDeltaOperation<>();
        odo.setObjectDelta(delta);
        odo.setExecutionResult(new OperationResult("asdf"));
        return odo;
    }
}
