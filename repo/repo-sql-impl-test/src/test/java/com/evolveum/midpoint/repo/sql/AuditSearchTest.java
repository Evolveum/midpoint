/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

// TODO MID-6319 - finish and add to test suite
@UnusedTestElement
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditSearchTest extends BaseSQLRepoTest {

    public static final long TIMESTAMP_1 = 1577836800000L; // 2020-01-01
    public static final long TIMESTAMP_2 = 1580515200000L; // 2020-02-01
    public static final long TIMESTAMP_3 = 1583020800000L; // 2020-03-01

    @Override
    public void initSystem() throws Exception {
        AuditEventRecord record1 = new AuditEventRecord();
        record1.addPropertyValue("prop", "val1");
        record1.setTimestamp(TIMESTAMP_1);
        record1.setEventType(AuditEventType.ADD_OBJECT);
        auditService.audit(record1, NullTaskImpl.INSTANCE);

        AuditEventRecord record2 = new AuditEventRecord();
        record2.addPropertyValue("prop", "val2");
        record2.setTimestamp(TIMESTAMP_2);
        record2.setEventType(AuditEventType.MODIFY_OBJECT);
        record2.setEventStage(AuditEventStage.EXECUTION);
        auditService.audit(record2, NullTaskImpl.INSTANCE);

        AuditEventRecord record3 = new AuditEventRecord();
        record3.addPropertyValue("prop", "val3-1");
        record3.addPropertyValue("prop", "val3-2");
        record3.addPropertyValue("prop", "val3-3");
        record3.setTimestamp(TIMESTAMP_3);
        record3.setEventType(AuditEventType.MODIFY_OBJECT);
        record3.setEventStage(AuditEventStage.EXECUTION);
        auditService.audit(record3, NullTaskImpl.INSTANCE);

        // TODO add some sample audits here
    }

    @Test
    public void test100SearchAllAuditEvents() throws SchemaException {
        when("Searching audit with query without any conditions");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class).build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("All audit events are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test110SearchAllAuditEventsOfSomeType() throws SchemaException {
        when("searching audit filtered by event type");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_TYPE).eq(AuditEventTypeType.ADD_OBJECT)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events of a particular type are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getEventType() == AuditEventTypeType.ADD_OBJECT);
    }

    @Test
    public void test112SearchAllAuditEventsWithSomeStage() throws SchemaException {
        when("searching audit filtered by event type");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_STAGE).eq(AuditEventStageType.EXECUTION)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events of a particular type are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getEventStage() == AuditEventStageType.EXECUTION);
    }
}

