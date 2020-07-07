/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CHANNEL_REST_URI;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@UnusedTestElement
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditSearchTest extends BaseSQLRepoTest {

    public static final long TIMESTAMP_1 = 1577836800000L; // 2020-01-01
    public static final long TIMESTAMP_2 = 1580515200000L; // 2020-02-01
    public static final long TIMESTAMP_3 = 1583020800000L; // 2020-03-01

    private String initiatorOid;
    private String attorneyOid;

    @Override
    public void initSystem() throws Exception {
        PrismObject<UserType> initiator = createUser("initiator");
        initiatorOid = initiator.getOid();
        PrismObject<UserType> attorney = createUser("attorney");
        attorneyOid = attorney.getOid();

        AuditEventRecord record1 = new AuditEventRecord();
        record1.addPropertyValue("prop", "val1");
        record1.setTimestamp(TIMESTAMP_1);
        record1.setEventType(AuditEventType.ADD_OBJECT);
        record1.setMessage("record1");
        record1.setOutcome(OperationResultStatus.SUCCESS);
        record1.setResult("result1");
        record1.setHostIdentifier("localhost");
        record1.setRemoteHostAddress("192.168.10.10");
        // all tested records have parameter, it is used for assertions where practical
        record1.setParameter("1");
        auditService.audit(record1, NullTaskImpl.INSTANCE);

        AuditEventRecord record2 = new AuditEventRecord();
        record2.addPropertyValue("prop", "val2");
        record2.setTimestamp(TIMESTAMP_2);
        record2.setEventType(AuditEventType.MODIFY_OBJECT);
        record2.setEventStage(AuditEventStage.EXECUTION);
        record2.setMessage("record2");
        record2.setOutcome(OperationResultStatus.UNKNOWN);
        record2.setInitiator(initiator);
        record2.setHostIdentifier("127.0.0.1");
        record2.setRemoteHostAddress("192.168.10.10");
        record2.setAttorney(attorney);
        record2.setRequestIdentifier("req-id");
        record2.setParameter("2");
        auditService.audit(record2, NullTaskImpl.INSTANCE);

        AuditEventRecord record3 = new AuditEventRecord();
        record3.addPropertyValue("prop", "val3-1");
        record3.addPropertyValue("prop", "val3-2");
        record3.addPropertyValue("prop", "val3-3");
        record3.setTimestamp(TIMESTAMP_3);
        record3.setEventType(AuditEventType.MODIFY_OBJECT);
        record3.setEventStage(AuditEventStage.EXECUTION);
        record3.setMessage("RECORD THREE");
        // null outcome is kinda like "unknown", but not quite, filter/GUI must handle it
        record3.setChannel(CHANNEL_REST_URI);
        record3.setParameter("3");
        auditService.audit(record3, NullTaskImpl.INSTANCE);
    }

    private PrismObject<UserType> createUser(String userName)
            throws ObjectAlreadyExistsException, SchemaException {
        PrismObject<UserType> user = new UserType(prismContext)
                .name(userName)
                .asPrismObject();
        repositoryService.addObject(user, null, createOperationResult());
        return user;
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
    public void test110SearchByEventType() throws SchemaException {
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
    public void test111SearchByUnusedEventType() throws SchemaException {
        when("searching audit filtered by unused event type");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_TYPE).eq(AuditEventTypeType.RECONCILIATION)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("no audit events are returned");
        assertThat(result).hasSize(0);
    }

    @Test
    public void test112SearchByEventStage() throws SchemaException {
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
    public void test114SearchByOutcome() throws SchemaException {
        when("searching audit filtered by outcome");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_OUTCOME).eq(OperationResultStatusType.UNKNOWN)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the specified outcome are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOutcome()).isEqualTo(OperationResultStatusType.UNKNOWN);
    }

    @Test
    public void test115SearchByOutcomeIsNull() throws SchemaException {
        when("searching audit filtered by null outcome (enum)");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_OUTCOME).isNull()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events without any outcome are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOutcome()).isNull();
    }

    @Test
    public void test118SearchByResultIsNull() throws SchemaException {
        when("searching audit filtered by null result (string)");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_RESULT).isNull()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events without any result are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getResult() == null);
    }

    @Test
    public void test120SearchByMessageEquals() throws SchemaException {
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
    public void test121SearchByMessageEqualsIgnoreCase() throws SchemaException {
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
    public void test125SearchByMessageContains() throws SchemaException {
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
    public void test126SearchByMessageContainsIgnoreCase() throws SchemaException {
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
    public void test130SearchByMessageStartsWith() throws SchemaException {
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
    public void test131SearchByMessageStartsWithIgnoreCase() throws SchemaException {
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
    public void test135SearchByMessageEndsWith() throws SchemaException {
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
    public void test136SearchByMessageEndsWithIgnoreCase() throws SchemaException {
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
    public void test140SearchByTimestampLessOrEqual() throws SchemaException {
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
    public void test141SearchByTimestampEqual() throws SchemaException {
        when("searching audit filtered by timestamp equal to specified time");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .eq(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the timestamp equal to are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @Test
    public void test142SearchByTimestampGreater() throws SchemaException {
        when("searching audit filtered by timestamp up to specified time");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .gt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the timestamp less or equal are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("3");
    }

    @Test
    public void test145SearchByTimestampIsNull() throws SchemaException {
        when("searching audit filtered by timestamp is null");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP).isNull()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        // this does not test IS NULL properly as =NULL would also return nothing, just saying...
        then("no audit events are returned as all have timestamp");
        assertThat(result).isEmpty();
    }

    @Test
    public void test150SearchByInitiator() throws SchemaException {
        when("searching audit filtered by initiator (reference by OID)");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_INITIATOR_REF)
                .ref(initiatorOid)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the specific initiator are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
        // TODO check mapping of initiator, see TODO in AuditEventRecordSqlTransformer#toAuditEventRecordType
    }

    @Test
    public void test151SearchByInitiatorIsNull() throws SchemaException {
        when("searching audit filtered by NULL initiator");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_INITIATOR_REF).isNull()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with NULL initiator are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getInitiatorRef() == null);
    }

    @Test
    public void test152SearchByAttorney() throws SchemaException {
        when("searching audit filtered by attorney (reference by OID)");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_ATTORNEY_REF)
                .ref(attorneyOid)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with the specified attorney are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
        // TODO check mapping of initiator, see TODO in AuditEventRecordSqlTransformer#toAuditEventRecordType
    }

    // if this works, all other operations work too based on message related tests
    @Test
    public void test160SearchByChannel() throws SchemaException {
        when("searching audit filtered by channel equal to value");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_CHANNEL).eq(CHANNEL_REST_URI)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with exactly the same channel are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getChannel().equals(CHANNEL_REST_URI));
    }

    @Test
    public void test161SearchByHostIdentifier() throws SchemaException {
        when("searching audit filtered by host identifier equal to value");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_HOST_IDENTIFIER).eq("localhost")
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with exactly the same host identifier are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getHostIdentifier().equals("localhost"));
    }

    @Test
    public void test162SearchByRemoteHostAddress() throws SchemaException {
        when("searching audit filtered by remote host address equal to value");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_REMOTE_HOST_ADDRESS).eq("192.168.10.10")
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with exactly the same remote host address are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getRemoteHostAddress().equals("192.168.10.10"));
    }

    @Test
    public void test163SearchByRequestIdentifier() throws SchemaException {
        when("searching audit filtered by request identifier equal to value");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_REQUEST_IDENTIFIER).eq("req-id")
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only audit events with exactly the same request identifier are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getRequestIdentifier().equals("req-id"));
    }

    // complex filters with AND and/or OR

    @Test
    public void test500SearchByTwoTimestampConditions() throws SchemaException {
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
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @Test
    public void test501SearchByMessageAndTimestamp() throws SchemaException {
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
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @Test
    public void test510SearchByMessageOrTimestamp() throws SchemaException {
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
        assertThat(result).extracting(aer -> aer.getParameter()).contains("1", "3");
    }

    @Test
    public void test900SearchWithAllFilter() throws SchemaException {
        when("searching audit using ALL filter");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .all()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("all audit records are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test902SearchWithNoneFilter() throws SchemaException {
        when("searching audit using NONE filter");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .none()
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("no audit records are returned");
        assertThat(result).hasSize(0);
    }

    @Test
    public void test902SearchWithEmptyFilter() throws SchemaException {
        when("searching audit without any filter specified");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("all audit records are returned (no restricting conditions are added)");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test920SearchWithOrderByOneItem() throws SchemaException {
        when("searching audit with order by one item (no paging)");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("all records are returned ordered by specified item");
        assertThat(result).hasSize(3);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactly("1", "2", "3");
    }

    @Test
    public void test950SearchWithNoPaging() throws SchemaException {
        when("searching audit using no paging");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("all audit records are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test955SearchWithOffsetAndMaxSize() throws SchemaException {
        when("searching audit using no paging");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .offset(1)
                .maxSize(1)
                .build();
        SearchResultList<AuditEventRecordType> result =
                auditService.searchObjects(query, null, null);

        then("only the expected page is returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }
}
