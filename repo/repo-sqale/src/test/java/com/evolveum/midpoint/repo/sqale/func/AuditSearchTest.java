/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CHANNEL_REST_URI;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * This is primarily search test, but works reasonably well as audit event add test as well.
 * What is not properly added, can't be searched by and some tests would fail.
 */
@SuppressWarnings("unchecked")
public class AuditSearchTest extends SqaleRepoBaseTest {

    public static final long TIMESTAMP_1 = 1577836800000L; // 2020-01-01
    public static final long TIMESTAMP_2 = 1580515200000L; // 2020-02-01
    public static final long TIMESTAMP_3 = 1583020800000L; // 2020-03-01
    public static final long TIMESTAMP_4 = 1600000000000L; // 2020-04...

    private String initiatorOid;
    private String attorneyOid;
    private String targetOid;
    private String targetOwnerOid;
    private String record1EventIdentifier;
    private Long record1RepoId;
    private final String taskOid = UUID.randomUUID().toString();
    private final String resourceOid = UUID.randomUUID().toString();
    private final String ignoredDeltasOid = UUID.randomUUID().toString();

    @BeforeClass
    public void initAuditEvents() throws Exception {
        clearAudit();

        OperationResult result = createOperationResult();

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
        record1.setRemoteHostAddress("192.168.10.1");
        record1.setSessionIdentifier("session-1");
        record1.setTarget(target);
        record1.setTargetOwner(targetOwner);
        record1.addDelta(createDelta(UserType.F_FULL_NAME)); // values are not even necessary
        record1.addDelta(createDelta(UserType.F_FAMILY_NAME, PolyString.fromOrig("familyNameVal")));
        ObjectDeltaOperation<UserType> delta3 = createDelta(ItemPath.create(
                        ObjectType.F_METADATA, MetadataType.F_REQUEST_TIMESTAMP),
                MiscUtil.asXMLGregorianCalendar(System.currentTimeMillis()));
        // adding execution result to one of deltas
        OperationResult opResult3 = new OperationResult("delta-op", OperationResultStatus.SUCCESS, "message");
        opResult3.subresult("sub-op1").setMinor().build().setStatus(OperationResultStatus.PARTIAL_ERROR);
        opResult3.subresult("sub-op2").setMinor().build().setSuccess();
        opResult3.subresult("sub-op3").build().setSuccess();
        delta3.setExecutionResult(opResult3);
        record1.addDelta(delta3);
        // just want to see two values, that's all
        record1.addReferenceValue("ref1",
                ObjectTypeUtil.createObjectRef(targetOid, ObjectTypes.USER).asReferenceValue());
        record1.addReferenceValue("ref2",
                ObjectTypeUtil.createObjectRef(targetOid, ObjectTypes.USER).asReferenceValue());
        record1.addResourceOid(resourceOid);
        record1.addResourceOid(UUID.randomUUID().toString());
        record1.addResourceOid(UUID.randomUUID().toString());
        record1.getCustomColumnProperty().put("foo", "foo-val");
        auditService.audit(record1, NullTaskImpl.INSTANCE, result);
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
        record2.setRemoteHostAddress("192.168.10.2");
        record2.setSessionIdentifier("session-1"); // session-1 on purpose
        record2.setAttorney(attorney);
        record2.setRequestIdentifier("req-id");
        record2.addDelta(createDelta(UserType.F_FULL_NAME, PolyString.fromOrig("somePolyString")));
        record2.addDelta(createDelta(UserType.F_ADDITIONAL_NAME));
        // These two deltas should collapse into single no-op delta + no changed items for them.
        // They must have the same OID too, so they have the same resulting checksum.
        record2.addDelta(createDeltaWithIgnoredPath(UserType.F_GIVEN_NAME));
        record2.addDelta(createDeltaWithIgnoredPath(UserType.F_FAMILY_NAME));
        record2.getCustomColumnProperty().put("foo", "foo-value-2");
        record2.getCustomColumnProperty().put("bar", "bar-val");
        record2.setTaskOid(UUID.randomUUID().toString());
        auditService.audit(record2, NullTaskImpl.INSTANCE, result);

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
        record3.setTaskOid(taskOid);
        auditService.audit(record3, NullTaskImpl.INSTANCE, result);

        AuditEventRecord record4 = new AuditEventRecord();
        record4.setParameter("4");
        record4.setTimestamp(TIMESTAMP_4);
        auditService.audit(record4, NullTaskImpl.INSTANCE, result);
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

    @NotNull
    private ObjectDeltaOperation<UserType> createDeltaWithIgnoredPath(ItemPath itemPath)
            throws SchemaException {
        ObjectDeltaOperation<UserType> delta = new ObjectDeltaOperation<>();
        delta.setObjectDelta(prismContext.deltaFor(UserType.class)
                .item(itemPath).add() // this path with ADD without value should be ignored
                .asObjectDelta(ignoredDeltasOid));
        return delta;
    }

    private PrismObject<UserType> createUser(String userName)
            throws ObjectAlreadyExistsException, SchemaException {
        PrismObject<UserType> user = new UserType()
                .name(userName)
                .asPrismObject();
        repositoryService.addObject(user, null, createOperationResult());
        return user;
    }

    @Test
    public void test100SearchAllAuditEvents() throws SchemaException {
        when("searching audit with query without any conditions and paging");
        OperationResult operationResult = createOperationResult();
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                        .queryFor(AuditEventRecordType.class)
                        .build(),
                operationResult);

        then("all audit events are returned");
        assertThat(result).hasSize(4);

        and("operation result is success");
        OperationResult subresult = operationResult.getLastSubresult();
        assertThat(subresult).isNotNull();
        assertThat(subresult.getOperation()).isEqualTo("SqaleAuditService.searchObjects");
        assertThat(subresult.getStatus()).isEqualTo(OperationResultStatus.SUCCESS);
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
        AuditEventRecordType auditEventRecord1 = result.get(0);
        assertThat(auditEventRecord1.getEventIdentifier()).isEqualTo(record1EventIdentifier);
        record1RepoId = auditEventRecord1.getRepoId();
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
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOutcome()).isNull();
    }

    @Test
    public void test116SearchByOutcomeIsNotNull() throws SchemaException {
        when("searching audit filtered by not null outcome (enum)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .not()
                .item(AuditEventRecordType.F_OUTCOME).isNull()
                .build());

        then("only audit events with not null outcome are returned");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(aer -> aer.getOutcome() != null);
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
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(aer -> aer.getResult() == null);
    }

    @Test
    public void test119SearchByResultIsNotNull() throws SchemaException {
        when("searching audit filtered by not null result (string)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .not()
                .item(AuditEventRecordType.F_RESULT).isNull()
                .build());

        then("only audit events without not null result are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResult()).isNotNull();
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
        // getParameter() is used to identify specific audit records (1, 2, 3) as mentioned in init
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
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("3", "4");
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
    public void test152SearchByInitiatorIsNull() throws SchemaException {
        when("searching audit filtered by NULL initiator");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_INITIATOR_REF).isNull()
                .build());

        then("only audit events with NULL initiator are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "3", "4");
        assertThat(result).allMatch(aer -> aer.getInitiatorRef() == null);
    }

    @Test
    public void test152SearchByInitiatorIsNotNull() throws SchemaException {
        when("searching audit filtered by NOT NULL initiator");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .not()
                .item(AuditEventRecordType.F_INITIATOR_REF).isNull()
                .build());

        then("only audit events with some (NOT NULL) initiator are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getInitiatorRef() != null);
    }

    @Test
    public void test160SearchByAttorney() throws SchemaException {
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
    public void test163SearchByTarget() throws SchemaException {
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
    public void test165SearchByTargetOwner() throws SchemaException {
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
    public void test170SearchByChannel() throws SchemaException {
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
    public void test173SearchByHostIdentifier() throws SchemaException {
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
    public void test175SearchByRemoteHostAddress() throws SchemaException {
        when("searching audit filtered by remote host address equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_REMOTE_HOST_ADDRESS).eq("192.168.10.1")
                .build());

        then("only audit events with exactly the same remote host address are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(aer -> aer.getRemoteHostAddress().equals("192.168.10.1"));
    }

    @Test
    public void test177SearchByRequestIdentifier() throws SchemaException {
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
    public void test180SearchByNodeIdentifier() throws SchemaException {
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
    public void test181SearchByNodeIdentifierThatIsNotUsed() throws SchemaException {
        when("searching audit filtered by node identifier equal to value that is not used");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_NODE_IDENTIFIER).eq("node-nonexistent")
                .build());

        then("no audit events are returned");
        assertThat(result).isEmpty();
    }

    @Test
    public void test185SearchByParameter() throws SchemaException {
        when("searching audit filtered by parameter item");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_PARAMETER).eq("1")
                .build());

        then("only audit events with the same value of parameter are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("1");
    }

    @Test
    public void test190SearchBySessionIdentifier() throws SchemaException {
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
    public void test193SearchByTaskIdentifier() throws SchemaException {
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
    public void test195SearchByTaskOid() throws SchemaException {
        when("searching audit filtered by task OID");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TASK_OID).eq(taskOid)
                .build());

        then("only audit events with the same task OID are returned");
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(r -> r.getTaskOID().equals(taskOid));
    }

    /*
     * Our NOT means "complement" and must include NULL values for NOT(EQ...).
     * This is difference from SQL logic, where NOT x='value' does NOT return rows where x IS NULL.
     * The solution is to use (x='value' AND x IS NOT NULL) for conditions inside the NOT filter.
     */

    @Test
    public void test200SearchByRemoteHostAddressNotEqual() throws SchemaException {
        when("searching audit filtered by remote host address NOT equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .not()
                .item(AuditEventRecordType.F_REMOTE_HOST_ADDRESS).eq("192.168.10.1")
                .build());

        then("audit events with with remote host address not equal"
                + " to the value are returned - including NULLs");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("2", "3", "4");
        assertThat(result).allMatch(aer -> !"192.168.10.1".equals(aer.getRemoteHostAddress()));
    }

    @Test
    public void test250SearchByChangedItemsSimplePath() throws SchemaException {
        when("searching audit by changed items equal to simple path");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_CHANGED_ITEM).eq(
                        new ItemPathType(UserType.F_FULL_NAME))
                .build());

        then("audit events with the specified changed items are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void test252SearchByChangedItemsComplexPath() throws SchemaException {
        when("searching audit by changed items equal to complex path");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_CHANGED_ITEM).eq(
                        new ItemPathType(ItemPath.create(
                                ObjectType.F_METADATA, MetadataType.F_REQUEST_TIMESTAMP)))
                .build());

        then("only audit events with the specified changed items are returned");
        assertThat(result).hasSize(1);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1");
    }

    @Test
    public void test253SearchByChangedItemsPrefixOfComplexPath() throws SchemaException {
        when("searching audit by changed items equal to the prefix of complex path");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_CHANGED_ITEM).eq(
                        new ItemPathType(ObjectType.F_METADATA))
                .build());

        then("only audit events with the specified changed items are returned");
        // this is more about the audit writing than reading, but it is here to cover all the expectations
        assertThat(result).hasSize(1);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1");
    }

    @Test
    public void test255SearchByUnusedChangedItems() throws SchemaException {
        when("searching audit by changed item path that is not there");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_CHANGED_ITEM).eq(
                        new ItemPathType(ObjectType.F_DOCUMENTATION))
                .build());

        then("no audit records are returned");
        assertThat(result).isEmpty();
    }

    @Test
    public void test260SearchByChangedItemsIsNull() throws SchemaException {
        when("searching audit by null changed items");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_CHANGED_ITEM).isNull()
                .build());

        then("only audit events without any changed items are returned");
        // technically, null path in existing changed item would also match, but we don't care
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("3", "4");
    }

    @Test
    public void test265SearchByChangedItemsIsNotNullWithDistinct() throws SchemaException {
        when("searching audit by NOT null changed items with distinct option");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .not()
                        .item(AuditEventRecordType.F_CHANGED_ITEM).isNull()
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("audit events with some changed item(s) are returned - without duplications");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void test266SearchByChangedItemsIsNotNullWithDistinctAndPaging() throws SchemaException {
        when("searching audit by NOT null changed items with distinct option and paging");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .not()
                        .item(AuditEventRecordType.F_CHANGED_ITEM).isNull()
                        .asc(AuditEventRecordType.F_PARAMETER)
                        .offset(1)
                        .maxSize(1)
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("specified page of audit events with some changed item(s) is returned"
                + " - not affected by duplications");
        assertThat(result).hasSize(1);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("2");
    }

    @Test
    public void test270SearchByChangedItemsMultipleValues() throws SchemaException {
        // this tests multiple values for JOIN path
        when("searching audit by changed items equal to multiple values");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                        .queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CHANGED_ITEM).eq(
                                new ItemPathType(UserType.F_ADDITIONAL_NAME),
                                new ItemPathType(UserType.F_FAMILY_NAME))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("audit events with changed items equal to any of the specified values are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void test271SearchByParameterMultipleValues() throws SchemaException {
        // this tests multiple values for strings
        when("searching audit by parameter equal to multiple values");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_PARAMETER).eq("1", "2", "X") // X is unused, it's OK
                .build());

        then("audit events with parameter equal to any of the specified values are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void test272SearchByEventTypeMultipleValues() throws SchemaException {
        // this tests multiple values for enums
        when("searching audit by event type equal to multiple values");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_TYPE).eq(
                        AuditEventTypeType.ADD_OBJECT, AuditEventTypeType.MODIFY_OBJECT)
                .build());

        then("audit events with event type equal to any of the specified values are returned");
        assertThat(result).hasSize(3);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2", "3");
    }

    @Test
    public void test273SearchByTimestampMultipleValues() throws SchemaException {
        // this tests multiple values for timestamps
        when("searching audit by timestamp equal to multiple values");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP).eq(
                        MiscUtil.asXMLGregorianCalendar(TIMESTAMP_1),
                        MiscUtil.asXMLGregorianCalendar(TIMESTAMP_3))
                .build());

        then("audit events with timestamp equal to any of the specified values are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "3");
    }

    @Test
    public void test280SearchByResourceOid() throws SchemaException {
        when("searching audit by resource OID");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_RESOURCE_OID).eq(resourceOid)
                .build());

        then("audit events with the specified resource OID are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1");
    }

    @Test
    public void test281SearchByResourceOidAnyValue() throws SchemaException {
        when("searching audit by resource OID multiple values");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_RESOURCE_OID)
                .eq(resourceOid, UUID.randomUUID().toString()) // second value will not match
                .build());

        then("audit events with any of the the specified resource OIDs are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1");
    }

    @Test
    public void test300SearchReturnsMappedToManyEntities() throws SchemaException {
        when("searching audit with query without any conditions and paging");
        List<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_PARAMETER)
                .build());

        then("all audit events are returned");
        assertThat(result).hasSize(4);
        // using stream, it's unmodifiable list
        result = result.stream()
                .sorted(Comparator.comparing(AuditEventRecordType::getParameter))
                .collect(Collectors.toList());

        and("record 1 has all the items filled");
        AuditEventRecordType record1 = result.get(0);
        assertThat(record1.getProperty()).hasSize(1);
        AuditEventRecordPropertyType prop1 = record1.getProperty().get(0);
        assertThat(prop1.getName()).isEqualTo("prop1");
        assertThat(prop1.getValue()).containsExactly("val1");
        // Changed items are not returned, they are used for query only and can be found in deltas.
        // Also, they may be stored in wrong format (e.g. "canonicalized").
        assertThat(record1.getChangedItem()).isNullOrEmpty();
        // for other items we just use the size check, fetch mechanism is similar
        assertThat(record1.getDelta()).hasSize(3)
                .allMatch(d -> d.getObjectDelta() != null);
        assertThat(record1.getReference()).hasSize(2);
        assertThat(record1.getResourceOid()).hasSize(3);
        // we also want to be sure that returned objects have prism definitions
        assertThat(record1.asPrismContainerValue().getComplexTypeDefinition()).isNotNull();

        and("record 2 has expected delta count");
        AuditEventRecordType record2 = result.get(1);
        // two meaningless deltas collapsed to single no-op delta + there are two normal deltas too
        assertThat(record2.getDelta()).hasSize(3);

        and("record 3 has expected properties");
        AuditEventRecordType record3 = result.get(2);
        assertThat(record3.getProperty()).as("two different property keys").hasSize(2);
        // Currently, props are sorted by name during transformation to AERType.
        prop1 = record3.getProperty().get(0);
        assertThat(prop1.getName()).isEqualTo("prop1");
        assertThat(prop1.getValue()).containsExactlyInAnyOrder("val3-1", "val3-2", "val3-3");
        AuditEventRecordPropertyType prop2 = record3.getProperty().get(1);
        assertThat(prop2.getName()).isEqualTo("prop2");
        // feels fishy, but [null] it is, not empty collection; should NOT be inserted anyway
        assertThat(prop2.getValue()).containsExactly((String) null);
    }

    @Test
    public void test350SearchWithWrongQuery() {
        when("searching audit objects using wrong query");
        OperationResult operationResult = createOperationResult();

        then("operation throws exception");
        assertThatThrownBy(
                () -> searchObjects(prismContext
                                .queryFor(AuditEventRecordType.class)
                                .asc(AuditEventRecordType.F_CHANGED_ITEM)
                                .build(),
                        operationResult))
                // we don't want to impose what exact type is thrown
                .isInstanceOf(Exception.class);

        and("operation result is fatal error");
        OperationResult subresult = operationResult.getLastSubresult();
        assertThat(subresult).isNotNull();
        assertThat(subresult.getOperation()).isEqualTo("SqaleAuditService.searchObjects");
        assertThat(subresult.getStatus()).isEqualTo(OperationResultStatus.FATAL_ERROR);
    }

    // complex filters with AND and/or OR

    @Test
    public void test500SearchByTwoTimestampConditions() throws SchemaException {
        when("searching audit filtered by timestamp AND timestamp condition");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
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
        when("searching audit filtered by timestamp OR message condition");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .lt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2)) // matches only record 1
                .or()
                .item(AuditEventRecordType.F_MESSAGE)
                .endsWith("three").matchingCaseIgnore() // matches only record 3
                .build());

        then("only audit events matching any of conditions are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "3");
    }

    @Test
    public void test515SearchByMessageNorTimestamp() throws SchemaException {
        when("searching audit filtered by negated (NOT) timestamp OR message condition");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .not()
                .block()
                .item(AuditEventRecordType.F_TIMESTAMP)
                .lt(MiscUtil.asXMLGregorianCalendar(TIMESTAMP_2)) // matches only record 1
                .or()
                .item(AuditEventRecordType.F_MESSAGE)
                .endsWith("three").matchingCaseIgnore() // matches only record 3
                .endBlock()
                .build());

        then("only audit events matching neither of conditions are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("2", "4");
    }

    @Test
    public void test520SearchByChangedItemOrAnotherChangedItem() throws SchemaException {
        // result should be similar to changedItem.eq(multiple values)
        when("searching audit filtered by changed item OR another changed item");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CHANGED_ITEM)
                        .eq(new ItemPathType(UserType.F_ADDITIONAL_NAME))
                        .or()
                        .item(AuditEventRecordType.F_CHANGED_ITEM)
                        .eq(new ItemPathType(UserType.F_FAMILY_NAME))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("only audit events having either of (and/or) specified changed items are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void test521SearchByChangedItemAndAnotherChangedItem() throws SchemaException {
        when("searching audit filtered by changed item AND another changed item");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CHANGED_ITEM)
                        .eq(new ItemPathType(UserType.F_GIVEN_NAME))
                        .and()
                        .item(AuditEventRecordType.F_CHANGED_ITEM)
                        .eq(new ItemPathType(UserType.F_FAMILY_NAME))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("only audit events having both specified changed items are returned");
        assertThat(result).isEmpty();
    }

    @Test
    public void test530SearchByProperty() throws SchemaException {
        when("searching audit filtered by property key-value");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_PROPERTY)
                        .eq(new AuditEventRecordPropertyType()
                                .name("prop1").value("val1"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("only audit events having the specified property with key-value entry are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1");
    }

    @Test
    public void test531SearchEventWithNoProperty() throws SchemaException {
        when("searching audit with no property key-value");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_PROPERTY).isNull()
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("only audit events having no property entries are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("4");
    }

    @Test
    public void test532SearchByPropertyWithMultipleValuesForOneKey() throws SchemaException {
        when("searching audit filtered by property key-value");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_PROPERTY)
                        .eq(new AuditEventRecordPropertyType()
                                .name("prop1").value("val3-1").value("val3-2"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("only audit events having the specified property with all the provided values are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("3");
    }

    @Test
    public void test533SearchByPropertyWithMultipleValuesForOneKeyNoMatch() throws SchemaException {
        when("searching audit filtered by property key-value with non-matching values");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_PROPERTY)
                        .eq(new AuditEventRecordPropertyType()
                                // second value is in audit event 1, but not together with the first
                                .name("prop1").value("val3-1").value("val1"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("nothing is found");
        assertThat(result).isEmpty();
    }

    @Test
    public void test534SearchByPropertyWithMultipleKeyValuePairs() throws SchemaException {
        when("searching audit filtered by multiple property key-value pairs");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_PROPERTY).eq(
                                // with multiple "big" values ANY semantics is used
                                new AuditEventRecordPropertyType()
                                        .name("prop1").value("val1"),
                                new AuditEventRecordPropertyType()
                                        .name("prop2").value(null),
                                // non-matching value does not eliminate any results
                                new AuditEventRecordPropertyType()
                                        .name("prop-nonexistent").value("xy"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("audit events having any of the property entries are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "3");
    }

    @Test
    public void test550SearchByCustomColumnProperty() throws SchemaException {
        when("searching audit filtered by custom column property");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .eq(new AuditEventRecordCustomColumnPropertyType()
                                .name("foo").value("foo-val"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("only audit events having the specified custom property name and value are returned");
        assertThat(result).hasSize(1);
        List<AuditEventRecordCustomColumnPropertyType> customColumnProperty =
                result.get(0).getCustomColumnProperty();
        assertThat(customColumnProperty).isNotEmpty()
                .anyMatch(p -> p.getName().equals("foo") && p.getValue().equals("foo-val"));
    }

    @Test
    public void test551SearchByCustomColumnPropertyWrongValue() throws SchemaException {
        when("searching audit filtered by custom column property using wrong value");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .eq(new AuditEventRecordCustomColumnPropertyType()
                                .name("foo").value("foo-wrong-val"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("no audit records are returned");
        assertThat(result).isEmpty();
    }

    @Test
    public void test552SearchByCustomColumnPropertyStartsWith() throws SchemaException {
        when("searching audit filtered by custom column property");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .startsWith(new AuditEventRecordCustomColumnPropertyType()
                                .name("foo").value("foo-val"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("only audit events having the specified custom property name and value are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void test553SearchByCustomColumnPropertyNotEqual() throws SchemaException {
        when("searching audit filtered by custom column property not equal to value");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .not()
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .eq(new AuditEventRecordCustomColumnPropertyType()
                                .name("foo").value("foo-val"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("audit events having the custom property not equal or NULL are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("2", "3", "4");
    }

    @Test
    public void test555SearchByCustomColumnPropertyMultiValue() throws SchemaException {
        when("searching audit filtered by custom column property multiple values");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .eq(new AuditEventRecordCustomColumnPropertyType()
                                        .name("foo").value("foo-val"),
                                new AuditEventRecordCustomColumnPropertyType()
                                        .name("bar").value("bar-val"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        // any = multivalue for AERCCPType has in effect OR semantics
        then("audit events having any of specified custom property name and value are returned");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void test556SearchByCustomColumnPropertyAndAnother() throws SchemaException {
        when("searching audit filtered by custom column properties joined with AND");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .startsWith(new AuditEventRecordCustomColumnPropertyType()
                                .name("foo").value("foo-val"))
                        .and()
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .eq(new AuditEventRecordCustomColumnPropertyType()
                                .name("bar").value("bar-val"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("audit events having properties meeting both specified conditions are returned");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameter()).isEqualTo("2");
    }

    @Test
    public void test558SearchByCustomColumnPropertyWithNullValue() throws SchemaException {
        when("searching audit filtered by custom column property with null value");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_CUSTOM_COLUMN_PROPERTY)
                        .eq(new AuditEventRecordCustomColumnPropertyType().name("foo"))
                        .build(),
                SelectorOptions.create(GetOperationOptions.createDistinct()));

        then("audit events with the specified custom property set to NULL are returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("3", "4");
    }

    @Test
    public void test600SearchById() throws SchemaException {
        when("searching audit using ALL filter");
        SearchResultList<AuditEventRecordType> result = searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_REPO_ID).eq(record1RepoId)
                        .build());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventIdentifier()).isEqualTo(record1EventIdentifier);
    }

    @Test
    public void test800SearchWithAllFilter() throws SchemaException {
        when("searching audit using ALL filter");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .all()
                .build());

        then("all audit records are returned");
        assertThat(result).hasSize(4);
    }

    @Test
    public void test802SearchWithNoneFilter() throws SchemaException {
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
    public void test820SearchWithOrderByOneItem() throws SchemaException {
        when("searching audit with order by one item (no paging)");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .build());

        then("all records are returned ordered by specified item");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactly("1", "2", "3", "4");
    }

    @Test
    public void test850SearchWithOffsetAndMaxSize() throws SchemaException {
        when("searching audit using paging");
        SearchResultList<AuditEventRecordType> result = searchObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .offset(1)
                .maxSize(1)
                .build());

        then("only the expected page is returned");
        assertThat(result).extracting(aer -> aer.getParameter())
                .containsExactlyInAnyOrder("2");
    }

    @Test
    public void test900CountWithAllFilter() throws SchemaException {
        when("counting audit objects using ALL filter");
        OperationResult operationResult = createOperationResult();
        int result = countObjects(prismContext
                        .queryFor(AuditEventRecordType.class)
                        .all()
                        .build(),
                operationResult);

        then("count of all audit records is returned");
        assertThat(result).isEqualTo(4);

        and("operation result is success");
        OperationResult subresult = operationResult.getLastSubresult();
        assertThat(subresult).isNotNull();
        assertThat(subresult.getOperation()).isEqualTo("SqaleAuditService.countObjects");
        assertThat(subresult.getStatus()).isEqualTo(OperationResultStatus.SUCCESS);
    }

    @Test
    public void test902CountWithNoneFilter() throws SchemaException {
        when("counting audit objects using NONE filter");
        int result = countObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .none()
                .build());

        then("count of zero better be returned");
        assertThat(result).isZero();
    }

    @Test
    public void test910CountWithFilterOnOneItem() throws SchemaException {
        when("counting audit objects filtered to one object");
        int result = countObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_PARAMETER).eq("1")
                .build());

        then("count of 1 record is returned");
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void test920CountWithOrderIsAllowedButMeaningless() throws SchemaException {
        when("counting audit objects with order");
        int result = countObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .build());

        then("count of all records is returned, order is ignored");
        assertThat(result).isEqualTo(4);
    }

    @Test
    public void test930CountWithAllParametersNull() {
        when("counting audit objects with null query");
        int result = auditService.countObjects(null, null, createOperationResult());

        then("count of all records is returned");
        assertThat(result).isEqualTo(4);
    }

    @Test
    public void test950CountWithOffsetAndMaxSize() throws SchemaException {
        when("counting audit objects with paging");
        int result = countObjects(prismContext
                .queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_TIMESTAMP)
                .offset(1)
                .maxSize(1)
                .build());

        then("count of all records is returned, paging is ignored");
        assertThat(result).isEqualTo(4);
    }

    @Test
    public void test960CountWithWrongQuery() {
        when("counting audit objects using wrong query");
        OperationResult operationResult = createOperationResult();

        then("operation throws exception");
        assertThatThrownBy(
                () -> countObjects(prismContext
                                .queryFor(AuditEventRecordType.class)
                                .item(AuditEventRecordType.F_EVENT_STAGE).eq("BAD VALUE")
                                .build(),
                        operationResult))
                // we don't want to impose what exact type is thrown
                .isInstanceOf(Exception.class);

        and("operation result is fatal error");
        OperationResult subresult = operationResult.getLastSubresult();
        assertThat(subresult).isNotNull();
        assertThat(subresult.getOperation()).isEqualTo("SqaleAuditService.countObjects");
        assertThat(subresult.getStatus()).isEqualTo(OperationResultStatus.FATAL_ERROR);
    }

    @NotNull
    private SearchResultList<AuditEventRecordType> searchObjects(
            ObjectQuery query,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        return searchObjects(query, createOperationResult(), selectorOptions);
    }

    @NotNull
    private SearchResultList<AuditEventRecordType> searchObjects(
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        displayQuery(query);
        return auditService.searchObjects(
                query,
                Arrays.asList(selectorOptions),
                operationResult);
    }

    private int countObjects(
            ObjectQuery query,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        return countObjects(query, createOperationResult(), selectorOptions);
    }

    private int countObjects(
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        displayQuery(query);
        return auditService.countObjects(
                query,
                Arrays.asList(selectorOptions),
                operationResult);
    }
}
