/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.repo.sql.audit.mapping.*;
import com.evolveum.midpoint.repo.sql.helpers.JdbcSession;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditEventRecord;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CleanupTest extends BaseSQLRepoTest {

    private Calendar create_2013_07_12_12_00_Calendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.MAY);
        calendar.set(Calendar.DAY_OF_MONTH, 7);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Duration createDuration(Calendar when, long now) throws Exception {
        long seconds = (now - when.getTimeInMillis()) / 1000;
        return DatatypeFactory.newInstance().newDuration("PT" + seconds + "S").negate();
    }

    private CleanupPolicyType createPolicy(Calendar when, long now) throws Exception {
        CleanupPolicyType policy = new CleanupPolicyType();

        Duration duration = createDuration(when, now);
        policy.setMaxAge(duration);

        return policy;
    }

    private CleanupPolicyType createPolicy(int maxRecords) {
        CleanupPolicyType policy = new CleanupPolicyType();

        policy.setMaxRecords(maxRecords);

        return policy;
    }

    @AfterMethod
    public void cleanup() {
        try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startTransaction()) {
            jdbcSession.delete(QAuditDeltaMapping.INSTANCE.defaultAlias()).execute();
            jdbcSession.delete(QAuditItemMapping.INSTANCE.defaultAlias()).execute();
            jdbcSession.delete(QAuditPropertyValueMapping.INSTANCE.defaultAlias()).execute();
            jdbcSession.delete(QAuditResourceMapping.INSTANCE.defaultAlias()).execute();
            jdbcSession.delete(QAuditRefValueMapping.INSTANCE.defaultAlias()).execute();
            jdbcSession.delete(QAuditEventRecordMapping.INSTANCE.defaultAlias()).execute();
        }
    }

    @Test
    public void testAuditCleanupMaxAge() throws Exception {
        //GIVEN
        prepareAuditEventRecords();

        //WHEN
        Calendar calendar = create_2013_07_12_12_00_Calendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.add(Calendar.MINUTE, 1);

        final long NOW = System.currentTimeMillis();
        CleanupPolicyType policy = createPolicy(calendar, NOW);

        OperationResult result = new OperationResult("Cleanup audit");
        auditService.cleanupAudit(policy, result);
        result.recomputeStatus();

        then();
        AssertJUnit.assertTrue(result.isSuccess());
        MAuditEventRecord record = assertAndReturnAuditEventRecord(1);

        Date finished = Date.from(record.timestamp);
        Date mark = new Date(NOW);
        Duration duration = policy.getMaxAge();
        duration.addTo(mark);

        AssertJUnit.assertTrue("finished: " + finished + ", mark: " + mark, finished.after(mark));
    }

    @Test
    public void testAuditCleanupMaxRecords() throws Exception {
        //GIVEN
        prepareAuditEventRecords();

        //WHEN
        Calendar calendar = create_2013_07_12_12_00_Calendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.add(Calendar.MINUTE, 1);

        CleanupPolicyType policy = createPolicy(1);

        OperationResult result = new OperationResult("Cleanup audit");
        auditService.cleanupAudit(policy, result);
        result.recomputeStatus();

        then();
        AssertJUnit.assertTrue(result.isSuccess());
        assertAndReturnAuditEventRecord(1);
    }

    private void prepareAuditEventRecords() throws Exception {
        Calendar calendar = create_2013_07_12_12_00_Calendar();
        for (int i = 0; i < 3; i++) {
            long timestamp = calendar.getTimeInMillis();
            AuditEventRecord record = new AuditEventRecord();
            record.addDelta(createObjectDeltaOperation(i));
            record.setTimestamp(timestamp);
            record.addPropertyValue("prop1", "val1");
            record.addReferenceValue("ref1", ObjectTypeUtil.createObjectRef("oid1", ObjectTypes.USER).asReferenceValue());
            logger.info("Adding audit record with timestamp {}", new Date(timestamp));

            auditService.audit(record, new NullTaskImpl());
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        assertAndReturnAuditEventRecord(3);
    }

    private ObjectDeltaOperation<?> createObjectDeltaOperation(int i) throws Exception {
        ObjectDeltaOperation<UserType> delta = new ObjectDeltaOperation<>();
        delta.setExecutionResult(new OperationResult("asdf"));
        UserType user = new UserType();
        prismContext.adopt(user);
        PolyStringType name = new PolyStringType();
        name.setOrig("a" + i);
        name.setNorm("a" + i);
        user.setName(name);

        delta.setObjectDelta(DeltaFactory.Object.createAddDelta(user.asPrismObject()));

        return delta;
    }

    private MAuditEventRecord assertAndReturnAuditEventRecord(int expectedCount) {
        List<MAuditEventRecord> records = select(QAuditEventRecordMapping.INSTANCE);
        AssertJUnit.assertEquals(expectedCount, records.size());
        return records.get(0);
    }
}
