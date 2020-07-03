/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

@UnusedTestElement
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditSearchTest extends BaseSQLRepoTest {

    public static final long TIMESTAMP_1 = 1577836800000L; // 2020-01-01
    public static final long TIMESTAMP_2 = 1580515200000L; // 2020-02-01
    public static final long TIMESTAMP_3 = 1583020800000L; // 2020-03-01

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Override
    public void initSystem() throws Exception {
        AuditEventRecord record1 = new AuditEventRecord();
        record1.addPropertyValue("prop", "val1");
        record1.setTimestamp(TIMESTAMP_1);
        record1.setEventType(AuditEventType.ADD_OBJECT);
        record1.setMessage("record1");
        auditService.audit(record1, NullTaskImpl.INSTANCE);

        AuditEventRecord record2 = new AuditEventRecord();
        record2.addPropertyValue("prop", "val2");
        record2.setTimestamp(TIMESTAMP_2);
        record2.setEventType(AuditEventType.MODIFY_OBJECT);
        record2.setEventStage(AuditEventStage.EXECUTION);
        record2.setMessage("record2");
        auditService.audit(record2, NullTaskImpl.INSTANCE);

        AuditEventRecord record3 = new AuditEventRecord();
        record3.addPropertyValue("prop", "val3-1");
        record3.addPropertyValue("prop", "val3-2");
        record3.addPropertyValue("prop", "val3-3");
        record3.setTimestamp(TIMESTAMP_3);
        record3.setEventType(AuditEventType.MODIFY_OBJECT);
        record3.setEventStage(AuditEventStage.EXECUTION);
        record3.setMessage("RECORD THREE");
        auditService.audit(record3, NullTaskImpl.INSTANCE);

        // TODO remove after done
        try (Connection conn = dataSourceFactory.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "select id, message, timestampValue from M_AUDIT_EVENT"
                                + " WHERE timestampValue <= ?")) {
            stmt.setTimestamp(1, new Timestamp(TIMESTAMP_2));

            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                Timestamp timestampValue = res.getTimestamp("timestampValue");
                System.out.println(res.getLong("id") + " - " + timestampValue + ": " + res.getString("message"));
                System.out.println("timestampValue = " + timestampValue.getTime());
            }
        }
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
    public void test110SearchAuditEventsOfSomeType() throws SchemaException {
        when("searching audit filtered by event type");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_TYPE).eq(AuditEventTypeType.ADD_OBJECT)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events of the specified type are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getEventType() == AuditEventTypeType.ADD_OBJECT);
    }

    @Test
    public void test112SearchAuditEventsWithSomeStage() throws SchemaException {
        when("searching audit filtered by event stage");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_STAGE).eq(AuditEventStageType.EXECUTION)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the specified stage are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getEventStage() == AuditEventStageType.EXECUTION);
    }

    @Test
    public void test120SearchAuditEventsWithMessageEquals() throws SchemaException {
        when("searching audit filtered by message equal to value");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).eq("record1")
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with exactly the same message are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getMessage().equals("record1"));
    }

    @Test
    public void test121SearchAuditEventsWithMessageEqualsIgnoreCase() throws SchemaException {
        when("searching audit filtered by message equal to value with ignore-case matcher");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).eq("ReCoRd1").matchingCaseIgnore()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the same message ignoring case are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getMessage().equals("record1"));
    }

    @Test
    public void test125SearchAuditEventsWithMessageContains() throws SchemaException {
        when("searching audit filtered by message containing a string");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).contains("ord")
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the message containing the specified value are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getMessage().contains("ord"));
    }

    @Test
    public void test126SearchAuditEventsWithMessageContainsIgnoreCase() throws SchemaException {
        when("searching audit filtered by message containing a string with ignore-case matcher");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).contains("ord").matchingCaseIgnore()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the message containing the specified value ignoring case are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test130SearchAuditEventsWithMessageStartsWith() throws SchemaException {
        when("searching audit filtered by message starting with a string");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).startsWith("rec")
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the message starting with the specified value are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getMessage().startsWith("rec"));
    }

    @Test
    public void test131SearchAuditEventsWithMessageStartsWithIgnoreCase() throws SchemaException {
        when("searching audit filtered by message starting with a string with ignore-case matcher");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).startsWith("rec").matchingCaseIgnore()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the message starting with the specified value ignoring case are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test135SearchAuditEventsWithMessageEndsWith() throws SchemaException {
        when("searching audit filtered by message ending with a string");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).endsWith("THREE")
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the message ending with the specified value are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getMessage().endsWith("THREE"));
    }

    @Test
    public void test136SearchAuditEventsWithMessageEndsWithIgnoreCase() throws SchemaException {
        when("searching audit filtered by message ending with a string with ignore-case matcher");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).endsWith("three").matchingCaseIgnore()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the message ending with the specified value ignoring case are returned");
        assertThat(result).hasSize(1);
    }

    @Test
    public void test140SearchAuditEventsTimestampLessOrEqual() throws SchemaException {
        when("searching audit filtered by timestamp up to specified time");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .le(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the timestamp less or equal to specified time are returned");
        assertThat(result).hasSize(2);
    }

    @Test
    public void test141SearchAuditEventsTimestampEqual() throws SchemaException {
        when("searching audit filtered by timestamp equal to specified time");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .eq(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the timestamp equal to are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("record2");
    }

    @Test
    public void test142SearchAuditEventsTimestampGreater() throws SchemaException {
        when("searching audit filtered by timestamp up to specified time");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .gt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the timestamp less or equal are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("RECORD THREE");
    }

    // complex filter

    @Test
    public void test500SearchAuditEventsByTwoTimestampConditions() throws SchemaException {
        when("searching audit filtered by timestamp AND timestamp condition");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .gt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_1)) // matches records 2 and 3
                .and()
                .item(AuditEventRecordType.F_TIMESTAMP)
                .lt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_3)) // matches records 1 and 2
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events matching both timestamp conditions are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("record2");
    }

    @Test
    public void test501SearchAuditEventsByMessageAndTimestamp() throws SchemaException {
        when("searching audit filtered by timestamp AND message condition");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .gt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_1)) // matches records 2 and 3
                .and()
                .item(AuditEventRecordType.F_MESSAGE)
                .startsWith("rec") // matches records 1 and 2
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events matching both conditions are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("record2");
    }

    @Test
    public void test510SearchAuditEventsByMessageOrTimestamp() throws SchemaException {
        when("searching audit filtered by timestamp AND message condition");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .lt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2)) // matches only record 1
                .or()
                .item(AuditEventRecordType.F_MESSAGE)
                .endsWith("three").matchingCaseIgnore() // matches only record 3
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events matching both conditions are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer ->
                aer.getMessage().equals("record1") || aer.getMessage().equals("RECORD THREE"));
    }

}
