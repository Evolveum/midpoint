/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CHANNEL_REST_URI;

import java.util.Arrays;
import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPropertyType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@UnusedTestElement
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditSearchTest extends BaseSQLRepoTest {

    public static final long TIMESTAMP_1 = 1577836800000L; // 2020-01-01
    public static final long TIMESTAMP_2 = 1580515200000L; // 2020-02-01
    public static final long TIMESTAMP_3 = 1583020800000L; // 2020-03-01

    private String initiatorOid;
    private String attorneyOid;
    private String targetOid;
    private String targetOwnerOid;
    private String record1EventIdentifier;

    @Override
    public void initSystem() throws Exception {
        PrismObject<UserType> initiator = createUser("initiator");
        initiatorOid = initiator.getOid();
        PrismObject<UserType> attorney = createUser("attorney");
        attorneyOid = attorney.getOid();
        PrismObject<UserType> target = createUser("target");
        targetOid = target.getOid();
        PrismObject<? extends FocusType> targetOwner = createUser("targetOwner");
        targetOwnerOid = targetOwner.getOid();

        AuditEventRecord record1 = new AuditEventRecord();
        // all tested records have parameter, it is used for assertions where practical
        record1.setParameter("1");
        record1.addPropertyValue("prop1", "val1");
        record1.setTimestamp(TIMESTAMP_1);
        record1.setEventType(AuditEventType.ADD_OBJECT);
        record1.setMessage("record1");
        record1.setOutcome(OperationResultStatus.SUCCESS);
        record1.setResult("result1");
        record1.setHostIdentifier("localhost");
        record1.setNodeIdentifier("node1");
        record1.setRemoteHostAddress("192.168.10.10");
        record1.setSessionIdentifier("session-1");
        record1.setTarget(target, prismContext);
        record1.setTargetOwner(targetOwner);
        auditService.audit(record1, NullTaskImpl.INSTANCE);
        record1EventIdentifier = record1.getEventIdentifier();

        AuditEventRecord record2 = new AuditEventRecord();
        record2.setParameter("2");
        record2.addPropertyValue("prop1", "val2");
        record2.setTimestamp(TIMESTAMP_2);
        record2.setEventType(AuditEventType.MODIFY_OBJECT);
        record2.setEventStage(AuditEventStage.EXECUTION);
        record2.setMessage("record2");
        record2.setOutcome(OperationResultStatus.UNKNOWN);
        record2.setInitiator(initiator);
        record2.setHostIdentifier("127.0.0.1");
        record2.setRemoteHostAddress("192.168.10.10");
        record2.setSessionIdentifier("session-1"); // session-1 on purpose
        record2.setAttorney(attorney);
        record2.setRequestIdentifier("req-id");
        auditService.audit(record2, NullTaskImpl.INSTANCE);

        AuditEventRecord record3 = new AuditEventRecord();
        record3.setParameter("3");
        record3.addPropertyValue("prop1", "val3-1");
        record3.addPropertyValue("prop1", "val3-2");
        record3.addPropertyValue("prop1", "val3-3");
        record3.addPropertyValue("prop2", null);
        record3.setTimestamp(TIMESTAMP_3);
        record3.setEventType(AuditEventType.MODIFY_OBJECT);
        record3.setEventStage(AuditEventStage.EXECUTION);
        record3.setMessage("RECORD THREE");
        // null outcome is kinda like "unknown", but not quite, filter/GUI must handle it
        record3.setChannel(CHANNEL_REST_URI);
        record3.setTaskIdentifier("task-identifier");
        record3.setTaskOid("task-oid");
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
        when("searching audit with query without any conditions and paging");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .build());

        then("all audit events are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test110SearchByEventIdentifier() throws SchemaException {
        when("searching audit with by without any conditions");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_IDENTIFIER).eq(record1EventIdentifier)
                .build());

        then("all audit events are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventIdentifier()).isEqualTo(record1EventIdentifier);
    }

    @Test
    public void test111SearchByEventType() throws SchemaException {
        when("searching audit filtered by event type");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_TYPE).eq(AuditEventTypeType.ADD_OBJECT)
                .build());

        then("only audit events of the specified type are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getEventType() == AuditEventTypeType.ADD_OBJECT);
    }

    @Test
    public void test112SearchByUnusedEventType() throws SchemaException {
        when("searching audit filtered by unused event type");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_TYPE).eq(AuditEventTypeType.RECONCILIATION)
                .build());

        then("no audit events are returned");
        assertThat(result).hasSize(0);
    }

    @Test
    public void test113SearchByEventStage() throws SchemaException {
        when("searching audit filtered by event stage");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_STAGE).eq(AuditEventStageType.EXECUTION)
                .build());

        then("only audit events with the specified stage are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getEventStage() == AuditEventStageType.EXECUTION);
    }

    @Test
    public void test114SearchByOutcome() throws SchemaException {
        when("searching audit filtered by outcome");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_OUTCOME).eq(OperationResultStatusType.UNKNOWN)
                .build());

        then("only audit events with the specified outcome are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOutcome()).isEqualTo(OperationResultStatusType.UNKNOWN);
    }

    @Test
    public void test115SearchByOutcomeIsNull() throws SchemaException {
        when("searching audit filtered by null outcome (enum)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_OUTCOME).isNull()
                .build());

        then("only audit events without any outcome are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOutcome()).isNull();
    }

    @Test
    public void test117SearchByResult() throws SchemaException {
        when("searching audit filtered by result");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_RESULT).eq("result1")
                .build());

        then("only audit events with specified result are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getResult().equals("result1"));
    }

    @Test
    public void test118SearchByResultIsNull() throws SchemaException {
        when("searching audit filtered by null result (string)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_RESULT).isNull()
                .build());

        then("only audit events without any result are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getResult() == null);
    }

    @Test
    public void test120SearchByMessageEquals() throws SchemaException {
        when("searching audit filtered by message equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).eq("record1")
                .build());

        then("only audit events with exactly the same message are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getMessage().equals("record1"));
    }

    @Test
    public void test121SearchByMessageEqualsIgnoreCase() throws SchemaException {
        when("searching audit filtered by message equal to value with ignore-case matcher");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).eq("ReCoRd1").matchingCaseIgnore()
                .build());

        then("only audit events with the same message ignoring case are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getMessage().equals("record1"));
    }

    @Test
    public void test125SearchByMessageContains() throws SchemaException {
        when("searching audit filtered by message containing a string");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).contains("ord")
                .build());

        then("only audit events with the message containing the specified value are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getMessage().contains("ord"));
    }

    @Test
    public void test126SearchByMessageContainsIgnoreCase() throws SchemaException {
        when("searching audit filtered by message containing a string with ignore-case matcher");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).contains("ord").matchingCaseIgnore()
                .build());

        then("only audit events with the message containing the specified value ignoring case are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test130SearchByMessageStartsWith() throws SchemaException {
        when("searching audit filtered by message starting with a string");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).startsWith("rec")
                .build());

        then("only audit events with the message starting with the specified value are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getMessage().startsWith("rec"));
    }

    @Test
    public void test131SearchByMessageStartsWithIgnoreCase() throws SchemaException {
        when("searching audit filtered by message starting with a string with ignore-case matcher");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).startsWith("rec").matchingCaseIgnore()
                .build());

        then("only audit events with the message starting with the specified value ignoring case are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test135SearchByMessageEndsWith() throws SchemaException {
        when("searching audit filtered by message ending with a string");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).endsWith("THREE")
                .build());

        then("only audit events with the message ending with the specified value are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getMessage().endsWith("THREE"));
    }

    @Test
    public void test136SearchByMessageEndsWithIgnoreCase() throws SchemaException {
        when("searching audit filtered by message ending with a string with ignore-case matcher");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_MESSAGE).endsWith("three").matchingCaseIgnore()
                .build());

        then("only audit events with the message ending with the specified value ignoring case are returned");
        assertThat(result).hasSize(1);
    }

    @Test
    public void test140SearchByTimestampLessOrEqual() throws SchemaException {
        when("searching audit filtered by timestamp up to specified time");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .le(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build());

        then("only audit events with the timestamp less or equal to specified time are returned");
        assertThat(result).hasSize(2);
    }

    @Test
    public void test141SearchByTimestampEqual() throws SchemaException {
        when("searching audit filtered by timestamp equal to specified time");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .eq(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build());

        then("only audit events with the timestamp equal to are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @Test
    public void test142SearchByTimestampGreater() throws SchemaException {
        when("searching audit filtered by timestamp up to specified time");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .gt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2))
                .build());

        then("only audit events with the timestamp less or equal are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("3");
    }

    @Test
    public void test145SearchByTimestampIsNull() throws SchemaException {
        when("searching audit filtered by timestamp is null");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP).isNull()
                .build());

        // this does not test IS NULL properly as =NULL would also return nothing, just saying...
        then("no audit events are returned as all have timestamp");
        assertThat(result).isEmpty();
    }

    @Test
    public void test150SearchByInitiator() throws SchemaException {
        when("searching audit filtered by initiator (reference by OID)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_INITIATOR_REF)
                .ref(initiatorOid)
                .build());

        then("only audit events with the specific initiator are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
        assertThat(result.get(0).getInitiatorRef()).isNotNull()
                .matches(r -> r.getOid().equals(initiatorOid))
                .matches(r -> r.getDescription().equals("initiator"))
                .matches(r -> r.getType().equals(UserType.COMPLEX_TYPE));
    }

    @Test
    public void test151SearchByInitiatorIsNull() throws SchemaException {
        when("searching audit filtered by NULL initiator");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_INITIATOR_REF).isNull()
                .build());

        then("only audit events with NULL initiator are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getInitiatorRef() == null);
    }

    @Test
    public void test152SearchByAttorney() throws SchemaException {
        when("searching audit filtered by attorney (reference by OID)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_ATTORNEY_REF)
                .ref(attorneyOid)
                .build());

        then("only audit events with the specified attorney are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
        assertThat(result.get(0).getAttorneyRef()).isNotNull()
                .matches(r -> r.getOid().equals(attorneyOid))
                .matches(r -> r.getDescription().equals("attorney"))
                // type of this reference is always focus, audit record doesn't store it
                .matches(r -> r.getType().equals(FocusType.COMPLEX_TYPE));
    }

    @Test
    public void test154SearchByTarget() throws SchemaException {
        when("searching audit filtered by target (reference by OID)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TARGET_REF)
                .ref(targetOid)
                .build());

        then("only audit events with the specified target are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("1");
        assertThat(result.get(0).getTargetRef()).isNotNull()
                .matches(r -> r.getOid().equals(targetOid))
                .matches(r -> r.getDescription().equals("target"))
                .matches(r -> r.getType().equals(UserType.COMPLEX_TYPE));
    }

    @Test
    public void test156SearchByTargetOwner() throws SchemaException {
        when("searching audit filtered by target owner (reference by OID)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TARGET_REF)
                .ref(targetOid)
                .build());

        then("only audit events with the specified target owner are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("1");
        assertThat(result.get(0).getTargetOwnerRef()).isNotNull()
                .matches(r -> r.getOid().equals(targetOwnerOid))
                .matches(r -> r.getDescription().equals("targetOwner"))
                // type of this reference is always focus, audit record doesn't store it
                .matches(r -> r.getType().equals(UserType.COMPLEX_TYPE));
    }

    // if this works, all other operations work too based on message related tests
    @Test
    public void test160SearchByChannel() throws SchemaException {
        when("searching audit filtered by channel equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_CHANNEL).eq(CHANNEL_REST_URI)
                .build());

        then("only audit events with exactly the same channel are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getChannel().equals(CHANNEL_REST_URI));
    }

    @Test
    public void test161SearchByHostIdentifier() throws SchemaException {
        when("searching audit filtered by host identifier equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_HOST_IDENTIFIER).eq("localhost")
                .build());

        then("only audit events with exactly the same host identifier are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getHostIdentifier().equals("localhost"));
    }

    @Test
    public void test162SearchByRemoteHostAddress() throws SchemaException {
        when("searching audit filtered by remote host address equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_REMOTE_HOST_ADDRESS).eq("192.168.10.10")
                .build());

        then("only audit events with exactly the same remote host address are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getRemoteHostAddress().equals("192.168.10.10"));
    }

    @Test
    public void test163SearchByRequestIdentifier() throws SchemaException {
        when("searching audit filtered by request identifier equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_REQUEST_IDENTIFIER).eq("req-id")
                .build());

        then("only audit events with exactly the same request identifier are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getRequestIdentifier().equals("req-id"));
    }

    @Test
    public void test165SearchByNodeIdentifier() throws SchemaException {
        when("searching audit filtered by node identifier equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_NODE_IDENTIFIER).eq("node1")
                .build());

        then("only audit events with exactly the same host identifier are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNodeIdentifier()).isEqualTo("node1");
    }

    @Test
    public void test165SearchByNodeIdentifierThatIsNotUsed() throws SchemaException {
        when("searching audit filtered by node identifier equal to value that is not used");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_NODE_IDENTIFIER).eq("node-nonexistent")
                .build());

        then("no audit events are returned");
        assertThat(result).isEmpty();
    }

    @Test
    public void test166SearchByParameter() throws SchemaException {
        when("searching audit filtered by parameter attribute");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_PARAMETER).eq("1")
                .build());

        then("only audit events with the same value of parameter are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("1");
    }

    @Test
    public void test168SearchBySessionIdentifier() throws SchemaException {
        when("searching audit filtered by session identifier");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_SESSION_IDENTIFIER).eq("session-1")
                .build());

        then("only audit events with the same session identifier are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getSessionIdentifier().equals("session-1"));
    }

    @Test
    public void test170SearchByTaskIdentifier() throws SchemaException {
        when("searching audit filtered by task identifier");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TASK_IDENTIFIER).eq("task-identifier")
                .build());

        then("only audit events with the same task identifier are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(r -> r.getTaskIdentifier().equals("task-identifier"));
    }

    @Test
    public void test172SearchByTaskOid() throws SchemaException {
        when("searching audit filtered by task OID");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TASK_OID).eq("task-oid")
                .build());

        then("only audit events with the same task OID are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(r -> r.getTaskOID().equals("task-oid"));
    }

    @Test
    public void test190SearchReturnsMappedToManyAttributes() throws SchemaException {
        when("searching audit with query without any conditions and paging");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class).build());

        then("all audit events are returned");
        assertThat(result).hasSize(3);
        result.sort(Comparator.comparing(AuditEventRecordType::getParameter));

        AuditEventRecordType record1 = result.get(0);
        assertThat(record1.getProperty()).hasSize(1);
        AuditEventRecordPropertyType prop1 = record1.getProperty().get(0);
        assertThat(prop1.getName()).isEqualTo("prop1");
        assertThat(prop1.getValue()).containsExactly("val1");

        AuditEventRecordType record3 = result.get(2);
        assertThat(record3.getProperty()).as("two different property keys").hasSize(2);
        prop1 = record3.getProperty().get(0);
        assertThat(prop1.getName()).isEqualTo("prop1");
        assertThat(prop1.getValue()).containsExactlyInAnyOrder("val3-1", "val3-2", "val3-3");
        AuditEventRecordPropertyType prop2 = record3.getProperty().get(1);
        assertThat(prop2.getName()).isEqualTo("prop2");
        // feels fishy, but [null] it is, not empty collection
        assertThat(prop2.getValue()).containsExactly((String) null);
    }

    // complex filters with AND and/or OR

    @Test
    public void test500SearchByTwoTimestampConditions() throws SchemaException {
        when("searching audit filtered by timestamp AND timestamp condition");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .gt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_1)) // matches records 2 and 3
                .and()
                .item(AuditEventRecordType.F_TIMESTAMP)
                .lt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_3)) // matches records 1 and 2
                .build());

        then("only audit events matching both timestamp conditions are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @Test
    public void test501SearchByMessageAndTimestamp() throws SchemaException {
        when("searching audit filtered by timestamp AND message condition");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .gt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_1)) // matches records 2 and 3
                .and()
                .item(AuditEventRecordType.F_MESSAGE)
                .startsWith("rec") // matches records 1 and 2
                .build());

        then("only audit events matching both conditions are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @Test
    public void test510SearchByMessageOrTimestamp() throws SchemaException {
        when("searching audit filtered by timestamp AND message condition");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .lt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2)) // matches only record 1
                .or()
                .item(AuditEventRecordType.F_MESSAGE)
                .endsWith("three").matchingCaseIgnore() // matches only record 3
                .build());

        then("only audit events matching both conditions are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "3");
    }

    @Test
    public void test900SearchWithAllFilter() throws SchemaException {
        when("searching audit using ALL filter");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .all()
                .build());

        then("all audit records are returned");
        assertThat(result).hasSize(3);
    }

    @Test
    public void test902SearchWithNoneFilter() throws SchemaException {
        when("searching audit using NONE filter");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .none()
                .build());

        then("no audit records are returned");
        assertThat(result).hasSize(0);
    }

    // empty filter and no paging is covered by test100
    @Test
    public void test920SearchWithOrderByOneItem() throws SchemaException {
        when("searching audit with order by one item (no paging)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .build());

        then("all records are returned ordered by specified item");
        assertThat(result).hasSize(3);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactly("1", "2", "3");
    }

    @Test
    public void test950SearchWithOffsetAndMaxSize() throws SchemaException {
        when("searching audit using no paging");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .offset(1)
                .maxSize(1)
                .build());

        then("only the expected page is returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @SafeVarargs
    @NotNull
    private final SearchResultList<AuditEventRecordType> searchObjects(
            ObjectQuery query,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        QueryType queryType = prismContext.getQueryConverter().createQueryType(query);
        System.out.println("queryType = " +
                prismContext.xmlSerializer().serializeAnyData(
                        queryType, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY));
        return auditService.searchObjects(
                query,
                Arrays.asList(selectorOptions),
                createOperationResult());
    }
}
