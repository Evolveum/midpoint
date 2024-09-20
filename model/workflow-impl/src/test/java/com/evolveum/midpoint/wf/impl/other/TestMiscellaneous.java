/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import static com.evolveum.midpoint.util.MiscUtil.extractSingleton;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.query.*;

import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.cases.api.AuditingConstants;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.OperationResultRepoSearchAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import javax.xml.namespace.QName;

@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractWfTestPolicy {

    private static final File TEST_DIR = new File("src/test/resources/miscellaneous");

    private static final TestObject<RoleType> ROLE_SAILOR = TestObject.file(TEST_DIR, "role-sailor.xml", "3ccc0a00-6a3b-4ae0-94a3-d45fc457f63f");
    private static final TestObject<RoleType> ROLE_CAPTAIN = TestObject.file(TEST_DIR, "role-captain.xml", "15a99cf1-5886-44d4-8aaf-7e1f46ccec36");
    private static final TestObject<UserType> USER_SCOTT = TestObject.file(TEST_DIR, "user-scott.xml", "929c49ed-0100-4068-b8e4-137bd8ebd6b2");

    private static final TestObject<RoleType> METAROLE_PRIZE = TestObject.file(TEST_DIR, "metarole-prize.xml", "2330f9df-83bc-4270-86fc-27fca2b616a7");
    private static final TestObject<RoleType> METAROLE_APPROVE_UNASSIGN = TestObject.file(TEST_DIR, "metarole-approve-unassign.xml", "e5144353-c39d-445c-bf15-c4b80ce75918");

    private static final TestObject<RoleType> ROLE_GOLD = TestObject.file(TEST_DIR, "role-gold.xml", "0b3ad53e-7c1d-41d0-a447-ce94cd25c46a");
    private static final TestObject<RoleType> ROLE_SILVER = TestObject.file(TEST_DIR, "role-silver.xml", "ee5206f8-930a-4c85-bfee-c16e4462df23");
    private static final TestObject<RoleType> ROLE_BRONZE = TestObject.file(TEST_DIR, "role-bronze.xml", "f16f4dd7-2830-4d0a-b6ed-9fbf253dbaf3");

    private static final TestObject<ObjectTemplateType> TEMPLATE_ASSIGNING_CAPTAIN = TestObject.file(TEST_DIR, "template-assigning-captain.xml", "18ac3da2-f2fa-496a-8e54-789a090ff492");
    private static final TestObject<ObjectTemplateType> TEMPLATE_ASSIGNING_CAPTAIN_AFTER = TestObject.file(TEST_DIR, "template-assigning-captain-after.xml", "ace5d8f0-f54b-4f1b-92c0-8fa104a8fe84");
    private static final TestObject<RoleType> ROLE_ASSIGNING_CAPTAIN = TestObject.file(TEST_DIR, "role-assigning-captain.xml", "4bdd7ccc-8c52-41ff-a975-0313ec788507");

    private static final TestObject<UserType> USER_SCROOGE = TestObject.file(TEST_DIR, "user-scrooge.xml", "edf53304-2da0-4a7c-82b4-74fe35dcbc6e");
    private static final TestObject<UserType> USER_GIZMODUCK = TestObject.file(TEST_DIR, "user-gizmoduck.xml", "6d0a7fce-b698-4f1d-95ce-14246452add5");
    private static final TestObject<UserType> USER_LAUNCHPAD = TestObject.file(TEST_DIR, "user-launchpad.xml", "00880478-d006-4fc8-9d3a-87b5ec546c40");
    private static final TestObject<RoleType> ROLE_VAULT_ACCESS = TestObject.file(TEST_DIR, "role-vault-access.xml", "f6f95936-8714-4c7d-abdf-6cd3e6d2d6cc");
    private static final TestObject<RoleType> ROLE_ACCOUNTANT = TestObject.file(TEST_DIR, "role-accountant.xml", "5653fc70-3007-4f62-82dd-a36e0673505b");

    private static final TestObject<TaskType> TASK_CLEANUP = TestObject.file(TEST_DIR, "task-cleanup.xml", "781a7c9a-7b37-45c6-9154-5e57f5ad077f");

    private static final TestObject<RoleType> ROLE_TEST370 = TestObject.file(TEST_DIR, "role-test370.xml", "2c226eba-7279-4768-a34a-38392e3fcb19");
    private static final TestObject<UserType> USER_TEST370 = TestObject.file(TEST_DIR, "user-test370.xml", "a981ea50-d069-431d-86dc-f4c7dbbc4723");

    private static final TestObject<RoleType> ROLE_TEST380 = TestObject.file(TEST_DIR, "role-test380.xml", "8f39e4ad-298a-4d9a-b793-56ad2f0fc7ce");
    private static final TestObject<UserType> USER_TEST380 = TestObject.file(TEST_DIR, "user-test380.xml", "1994a4d0-4151-4260-82da-bcd1866c296a");

    private static final TestObject<RoleType> ROLE_AUTOCOMPLETIONS = TestObject.file(
            TEST_DIR, "role-autocompletions.xml", "a2570ee8-6c13-48b9-9a33-d8e88c4fe618");

    private static final TestObject<OrgType> ORG_APPROVERS = TestObject.file(
            TEST_DIR, "org-approvers.xml", "8b928d45-bb91-4a02-8418-6ae0d3b6a7d2");
    private static final TestObject<RoleType> ROLE_APPROVED_BY_ORG = TestObject.file(
            TEST_DIR, "role-approved-by-org.xml", "9a563d3e-12aa-4dc1-a6ee-de9e9b33974e");
    private static final TestObject<RoleType> ROLE_APPROVED_BY_MULTIPLE_RELATIONS = TestObject.file(
            TEST_DIR, "role-approved-by-multiple-relations.xml", "62d7fcdf-92b0-4c49-ae40-33b0a814ed56");
    private static final TestObject<UserType> USER_APPROVER_BY_MULTIPLE_RELATIONS = TestObject.file(
            TEST_DIR, "user-approver-by-multiple-relations.xml", "a9aca7bb-923e-4be6-9aa4-5c90af978207");
    private static final TestObject<RoleType> ROLE_APPROVE_WITH_SKIP_LAST_STAGE = TestObject.file(
            TEST_DIR, "role-approve-with-skip-last-stage.xml", "8b928d45-bb91-4a02-8418-6ae0d3b6a1d3");

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(ROLE_SAILOR, initResult);
        repoAdd(ROLE_CAPTAIN, initResult);

        repoAdd(METAROLE_PRIZE, initResult);
        repoAdd(METAROLE_APPROVE_UNASSIGN, initResult);
        repoAdd(ROLE_GOLD, initResult);
        repoAdd(ROLE_SILVER, initResult);
        repoAdd(ROLE_BRONZE, initResult);

        addAndRecompute(USER_SCOTT, initTask, initResult);

        repoAdd(TEMPLATE_ASSIGNING_CAPTAIN, initResult);
        repoAdd(TEMPLATE_ASSIGNING_CAPTAIN_AFTER, initResult);
        repoAdd(ROLE_ASSIGNING_CAPTAIN, initResult);

        repoAdd(ROLE_VAULT_ACCESS, initResult);
        repoAdd(ROLE_ACCOUNTANT, initResult);
        addAndRecompute(USER_SCROOGE, initTask, initResult);
        addAndRecompute(USER_GIZMODUCK, initTask, initResult);
        addAndRecompute(USER_LAUNCHPAD, initTask, initResult);

        addObject(ROLE_AUTOCOMPLETIONS, initTask, initResult);

        ORG_APPROVERS.init(this, initTask, initResult);
        ROLE_APPROVED_BY_ORG.init(this, initTask, initResult);
        ROLE_APPROVED_BY_MULTIPLE_RELATIONS.init(this, initTask, initResult);
        USER_APPROVER_BY_MULTIPLE_RELATIONS.init(this, initTask, initResult);

        ROLE_APPROVE_WITH_SKIP_LAST_STAGE.init(this, initTask, initResult);
    }

    @Test
    public void test100RequesterComment() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        dummyAuditService.clear();

        OperationBusinessContextType businessContext = new OperationBusinessContextType();
        final String REQUESTER_COMMENT = "req.comment";
        businessContext.setComment(REQUESTER_COMMENT);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK.oid, ROLE_SAILOR.oid, RoleType.COMPLEX_TYPE, null, null, null, true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, executeOptions().requestBusinessContext(businessContext), task, result);

        assertNotAssignedRole(USER_JACK.oid, ROLE_SAILOR.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);

        when();
        caseManager.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput().comment("OK"),
                null, task, result);

        then();
        CaseType aCase = getCase(CaseTypeUtil.getCaseRequired(workItem).getOid());
        display("workflow context", aCase.getApprovalContext());
        List<? extends CaseEventType> events = aCase.getEvent();
        assertEquals("Wrong # of events", 2, events.size());

        CaseCreationEventType event1 = (CaseCreationEventType) events.get(0);
        display("Event 1", event1);
        assertEquals("Wrong requester comment", REQUESTER_COMMENT, ApprovalContextUtil.getBusinessContext(aCase).getComment());

        WorkItemEventType event2 = (WorkItemEventType) events.get(1);
        display("Event 2", event2);

        assertNotNull("Original assignee is null", event2.getOriginalAssigneeRef());
        assertEquals("Wrong original assignee OID", USER_SCOTT.oid, event2.getOriginalAssigneeRef().getOid());

        displayDumpable("audit", dummyAuditService);
        List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        assertEquals("Wrong # of process instance audit records", 2, records.size());
        for (int i = 0; i < records.size(); i++) {
            AuditEventRecord record = records.get(i);
            assertEquals("Wrong requester comment in audit record #" + i, Collections.singleton(REQUESTER_COMMENT),
                    record.getPropertyValues(AuditingConstants.AUDIT_REQUESTER_COMMENT));
        }

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        AssignmentType assignment = assertAssignedRole(USER_JACK.oid, ROLE_SAILOR.oid, result);
        display("assignment after creation", assignment);
        assertNotNull("Null request timestamp in metadata", ValueMetadataTypeUtil.getRequestTimestamp(assignment));
        assertRefEquals("Wrong requestorRef in metadata",
                ObjectTypeUtil.createObjectRef(userAdministrator),
                extractSingleton(ValueMetadataTypeUtil.getRequestorRefs(assignment)));
        assertEquals(
                "Wrong requestorComment in metadata", REQUESTER_COMMENT,
                extractSingleton(ValueMetadataTypeUtil.getRequestorComments(assignment)));
    }

    @Test
    public void test105RequesterCommentImmediate() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        dummyAuditService.clear();

        OperationBusinessContextType businessContext = new OperationBusinessContextType();
        final String REQUESTER_COMMENT = "req.comment";
        businessContext.setComment(REQUESTER_COMMENT);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK.oid, ROLE_CAPTAIN.oid, RoleType.COMPLEX_TYPE, null, null, null, true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        ModelExecuteOptions options = executeOptions().requestBusinessContext(businessContext);
        options.executeImmediatelyAfterApproval(true);
        modelService.executeChanges(deltas, options, task, result);

        assertNotAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);

        when();
        caseManager.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput().comment("OK"),
                null, task, result);

        then();
        CaseType aCase = getCase(CaseTypeUtil.getCaseRequired(workItem).getOid());
        display("workflow context", aCase.getApprovalContext());
        List<? extends CaseEventType> events = aCase.getEvent();
        assertEquals("Wrong # of events", 2, events.size());

        CaseCreationEventType event1 = (CaseCreationEventType) events.get(0);
        display("Event 1", event1);
        assertEquals("Wrong requester comment", REQUESTER_COMMENT, ApprovalContextUtil.getBusinessContext(aCase).getComment());

        WorkItemEventType event2 = (WorkItemEventType) events.get(1);
        display("Event 2", event2);

        assertNotNull("Original assignee is null", event2.getOriginalAssigneeRef());
        assertEquals("Wrong original assignee OID", USER_SCOTT.oid, event2.getOriginalAssigneeRef().getOid());

        displayDumpable("audit", dummyAuditService);
        List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        assertEquals("Wrong # of process instance audit records", 2, records.size());
        for (int i = 0; i < records.size(); i++) {
            AuditEventRecord record = records.get(i);
            assertEquals("Wrong requester comment in audit record #" + i, Collections.singleton(REQUESTER_COMMENT),
                    record.getPropertyValues(AuditingConstants.AUDIT_REQUESTER_COMMENT));
        }

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        AssignmentType assignment = assertAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);
        display("assignment after creation", assignment);
        assertNotNull("Null request timestamp in metadata", ValueMetadataTypeUtil.getRequestTimestamp(assignment));
        assertRefEquals("Wrong requestorRef in metadata",
                ObjectTypeUtil.createObjectRef(userAdministrator),
                extractSingleton(ValueMetadataTypeUtil.getRequestorRefs(assignment)));
        assertEquals("Wrong requestorComment in metadata",
                REQUESTER_COMMENT, extractSingleton(ValueMetadataTypeUtil.getRequestorComments(assignment)));
    }

    @Test
    public void test110RequestPrunedRole() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        ModelExecuteOptions options = executeOptions().partialProcessing(
                new PartialProcessingOptionsType().approvals(PartialProcessingTypeType.SKIP));
        assignRole(USER_JACK.oid, ROLE_GOLD.oid, options, task, result);
        assertAssignedRole(getUser(USER_JACK.oid), ROLE_GOLD.oid);

        when();
        assignRole(USER_JACK.oid, ROLE_SILVER.oid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertInProgress("Operation NOT in progress", result);

        assertNotAssignedRole(USER_JACK.oid, ROLE_SILVER.oid, result);

        // complete the work item related to assigning role silver
        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);
        caseManager.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(),
                null, task, result);

        CaseType aCase = CaseTypeUtil.getCaseRequired(workItem);
        CaseType rootCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(rootCase);

        // should be pruned without approval
        assertNotAssignedRole(USER_JACK.oid, ROLE_GOLD.oid, result);
    }

    @Test
    public void test200GetRoleByTemplate() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        setDefaultUserTemplate(null);
        unassignAllRoles(USER_JACK.oid);
        assertNotAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);

        setDefaultUserTemplate(TEMPLATE_ASSIGNING_CAPTAIN.oid);

        when();
        // some innocent change
        modifyUserChangePassword(USER_JACK.oid, "PaSsWoRd123", task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);
    }

    @Test
    public void test210GetRoleByTemplateAfterAssignments() throws Exception {
        login(userAdministrator);

        taskManager.unsetGlobalTracingOverride();

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        setDefaultUserTemplate(null);
        unassignAllRoles(USER_JACK.oid);
        assertNotAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);

        setDefaultUserTemplate(TEMPLATE_ASSIGNING_CAPTAIN_AFTER.oid);

        when();
        // some innocent change
        modifyUserChangePassword(USER_JACK.oid, "PaSsWoRd123", task, result);
        // here the captain role appears in evaluatedAssignmentsTriple only in secondary phase; so no approvals are triggered

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);
    }

    @Test
    public void test220GetRoleByFocusMappings() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        setDefaultUserTemplate(null);
        unassignAllRoles(USER_JACK.oid);
        assertNotAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);

        when();
        assignRole(USER_JACK.oid, ROLE_ASSIGNING_CAPTAIN.oid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);
    }

    @Test
    public void test250SkippingApprovals() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        setDefaultUserTemplate(null);
        unassignAllRoles(USER_JACK.oid);
        assertNotAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);

        when();
        ObjectDelta<? extends ObjectType> delta =
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(ObjectTypeUtil.createAssignmentTo(ROLE_CAPTAIN.oid, ObjectTypes.ROLE))
                        .asObjectDelta(USER_JACK.oid);
        ModelExecuteOptions options = executeOptions().partialProcessing(
                new PartialProcessingOptionsType().approvals(PartialProcessingTypeType.SKIP));
        modelService.executeChanges(singletonList(delta), options, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);
    }

    /**
     * MID-6183
     */
    @Test
    public void test300DeleteRequestCase() throws Exception {
        given();

        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        unassignAllRoles(USER_JACK.oid);

        // @formatter:off
        ObjectDelta<? extends ObjectType> delta =
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_CAPTAIN.oid, ObjectTypes.ROLE))
                        .asObjectDelta(USER_JACK.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);

        RelatedCases relatedCases = new RelatedCases().find(task, result);
        CaseType approvalCase = relatedCases.getApprovalCase();
        CaseType requestCase = relatedCases.getRequestCase();

        when();

        deleteObject(CaseType.class, requestCase.getOid(), task, result);

        then();

        assertObjectDoesntExist(CaseType.class, requestCase.getOid());
        assertObjectDoesntExist(CaseType.class, approvalCase.getOid());
    }

    /**
     * MID-6183
     */
    @Test
    public void test310DeleteRequestCaseRaw() throws Exception {
        given();

        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        unassignAllRoles(USER_JACK.oid);

        // @formatter:off
        ObjectDelta<? extends ObjectType> delta =
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_CAPTAIN.oid, ObjectTypes.ROLE))
                        .asObjectDelta(USER_JACK.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(USER_JACK.oid, ROLE_CAPTAIN.oid, result);

        RelatedCases relatedCases = new RelatedCases().find(task, result);
        CaseType approvalCase = relatedCases.getApprovalCase();
        CaseType requestCase = relatedCases.getRequestCase();

        when();

        deleteObjectRaw(CaseType.class, requestCase.getOid(), task, result);

        then();

        assertObjectDoesntExist(CaseType.class, requestCase.getOid());
        assertObjectExists(CaseType.class, approvalCase.getOid());

        // just to clean up before downstream tests
        deleteObjectRaw(CaseType.class, approvalCase.getOid(), task, result);
    }

    @Test
    public void test350ApproveAsAttorneyAdministrator() throws Exception {
        given();

        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // @formatter:off
        ObjectDelta<? extends ObjectType> delta =
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_VAULT_ACCESS.oid, ObjectTypes.ROLE))
                        .asObjectDelta(USER_LAUNCHPAD.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(USER_LAUNCHPAD.oid, ROLE_VAULT_ACCESS.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);

        when();

        modelInteractionService.runUnderPowerOfAttorneyChecked(() -> {
            AbstractWorkItemOutputType output = new AbstractWorkItemOutputType()
                    .outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
            caseService.completeWorkItem(CaseTypeUtil.getId(workItem), output, task, result);
            return null;
        }, USER_SCROOGE.get(), task, result);

        then();

        // @formatter:off
        assertNotAssignedRole(USER_LAUNCHPAD.oid, ROLE_VAULT_ACCESS.oid, result);
        CaseType approvalCase = assertCase(result, "after")
                .display()
                .assertOperationRequestArchetype()
                .assertObjectRef(USER_LAUNCHPAD.oid, UserType.COMPLEX_TYPE)
                .assertClosed()
                .subcases()
                    .assertSubcases(1)
                    .single()
                    .getObject().asObjectable();

        assertCase(approvalCase, "after")
                .display()
                .assertNameOrig("Assigning role \"vault-access\" to user \"Launchpad McQuack (launchpad)\"")
                .assertApprovalCaseArchetype()
                .assertObjectRef(USER_LAUNCHPAD.oid, UserType.COMPLEX_TYPE)
                .assertTargetRef(ROLE_VAULT_ACCESS.oid, RoleType.COMPLEX_TYPE)
                .assertClosed()
                .assertRejected()
                .assertStageNumber(1)
                .events()
                    .assertEvents(2)
                    .ofType(CaseCreationEventType.class)
                        .assertInitiatorRef(USER_ADMINISTRATOR_OID, UserType.COMPLEX_TYPE)
                        .end()
                    .ofType(WorkItemCompletionEventType.class)
                        .assertOriginalAssigneeRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE)
                        .assertInitiatorRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE)
                        .assertAttorneyRef(USER_ADMINISTRATOR_OID, UserType.COMPLEX_TYPE)
                        .end()
                    .end()
                .workItems()
                    .assertWorkItems(1)
                    .single()
                        .assertNameOrig("Assigning role \"vault-access\" to user \"Launchpad McQuack (launchpad)\"")
                        .assertStageNumber(1)
                        .assertOriginalAssigneeRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE)
                        .assertPerformerRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE) // we should perhaps list attorney here as well
                        .assertClosed()
                        .assertRejected();
        // @formatter:on
    }

    @Test
    public void test360ApproveAsAttorneyGizmoduck() throws Exception {
        given();

        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // @formatter:off
        ObjectDelta<? extends ObjectType> delta =
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_VAULT_ACCESS.oid, ObjectTypes.ROLE))
                        .asObjectDelta(USER_LAUNCHPAD.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(USER_LAUNCHPAD.oid, ROLE_VAULT_ACCESS.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);

        when();

        login(USER_GIZMODUCK.get());

        modelInteractionService.runUnderPowerOfAttorneyChecked(() -> {
            AbstractWorkItemOutputType output = new AbstractWorkItemOutputType()
                    .outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
            caseService.completeWorkItem(CaseTypeUtil.getId(workItem), output, task, result);
            return null;
        }, USER_SCROOGE.get(), task, result);

        then();

        login(userAdministrator); // to avoid problems because of insufficient privileges

        // @formatter:off
        assertNotAssignedRole(USER_LAUNCHPAD.oid, ROLE_VAULT_ACCESS.oid, result);
        CaseType approvalCase = assertCase(result, "after")
                .display()
                .assertOperationRequestArchetype()
                .assertObjectRef(USER_LAUNCHPAD.oid, UserType.COMPLEX_TYPE)
                .assertClosed()
                .subcases()
                    .assertSubcases(1)
                    .single()
                        .getObject().asObjectable();

        assertCase(approvalCase, "after")
                .display()
                .assertNameOrig("Assigning role \"vault-access\" to user \"Launchpad McQuack (launchpad)\"")
                .assertApprovalCaseArchetype()
                .assertObjectRef(USER_LAUNCHPAD.oid, UserType.COMPLEX_TYPE)
                .assertTargetRef(ROLE_VAULT_ACCESS.oid, RoleType.COMPLEX_TYPE)
                .assertClosed()
                .assertRejected()
                .assertStageNumber(1)
                .events()
                    .assertEvents(2)
                    .ofType(CaseCreationEventType.class)
                        .assertInitiatorRef(USER_ADMINISTRATOR_OID, UserType.COMPLEX_TYPE)
                        .end()
                    .ofType(WorkItemCompletionEventType.class)
                        .assertOriginalAssigneeRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE)
                        .assertInitiatorRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE)
                        .assertAttorneyRef(USER_GIZMODUCK.oid, UserType.COMPLEX_TYPE)
                    .end()
                .end()
                .workItems()
                    .assertWorkItems(1)
                    .single()
                        .assertNameOrig("Assigning role \"vault-access\" to user \"Launchpad McQuack (launchpad)\"")
                        .assertStageNumber(1)
                        .assertOriginalAssigneeRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE)
                        .assertPerformerRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE) // we should perhaps list attorney here as well
                        .assertClosed()
                        .assertRejected();

        // @formatter:on
    }

    /**
     * Deletes a user that has an assignment-related constraint with a custom message.
     *
     * This used to fail with an NPE - see MID-7908.
     */
    @Test
    public void test370DeleteUserWithMessage() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator);

        given("user and role are created (in raw mode)");
        repoAdd(ROLE_TEST370, result);
        repoAdd(USER_TEST370, result);

        when("user is deleted");
        deleteObject(UserType.class, USER_TEST370.oid, task, result);

        then("user is gone");
        assertSuccess(result);
        assertNoObject(UserType.class, USER_TEST370.oid);
    }

    /**
     * Deletes a user that has an assignment-related constraint with the approval action.
     *
     * MID-7912
     */
    @Test
    public void test380DeleteUserWithApproval() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator);

        given("user and role are created (in raw mode)");
        repoAdd(ROLE_TEST380, result);
        repoAdd(USER_TEST380, result);

        when("user is deleted");
        deleteObject(UserType.class, USER_TEST380.oid, task, result);

        then("user is gone");
        assertSuccess(result);
        assertNoObject(UserType.class, USER_TEST380.oid);
    }

    /**
     * Cleans up closed cases.
     *
     * Marks one root and one non-root case as indestructible, just to check if they survive the cleanup.
     *
     * Expected survivors:
     *
     * - indestructible root and all of its children,
     * - indestructible child (selected in such a way that it has no children).
     *
     * Depends on previous test methods.
     */
    @Test
    public void test399CaseCleanup() throws Exception {
        given("mark indestructible cases");
        OperationResult result = getTestOperationResult();

        List<PrismObject<CaseType>> allCases = repositoryService.searchObjects(CaseType.class, null, null, result);
        List<CaseType> closedCases = selectClosedCases(allCases);
        display("closed cases (" + closedCases.size() + " out of " + allCases.size() + ")", closedCases);

        List<CaseType> closedRootCases = closedCases.stream()
                .filter(c -> c.getParentRef() == null)
                .collect(Collectors.toList());

        assertThat(closedRootCases).as("closed root cases").hasSizeGreaterThanOrEqualTo(2);

        CaseType indestructibleRoot = closedRootCases.get(0);
        markIndestructible(indestructibleRoot.getOid(), result);

        List<CaseType> indestructibleRootChildren = selectChildren(indestructibleRoot.getOid(), closedCases);
        display("Children of indestructible root", indestructibleRootChildren);

        CaseType indestructibleChild = selectIndestructibleChild(closedCases, closedRootCases, indestructibleRoot);
        markIndestructible(indestructibleChild.getOid(), result);

        when();
        addTask(TASK_CLEANUP, result);
        waitForTaskCloseOrSuspend(TASK_CLEANUP.oid, 20000);

        then();
        List<PrismObject<CaseType>> allCasesAfter = repositoryService.searchObjects(CaseType.class, null, null, result);
        List<CaseType> closedCasesAfter = selectClosedCases(allCasesAfter);
        display("closed cases after (" + closedCasesAfter.size() + " out of " + allCasesAfter.size() + ")",
                closedCasesAfter);

        assertThat(closedCasesAfter).as("closed cases after").hasSize(2 + indestructibleRootChildren.size());
        assertThat(selectOids(closedCasesAfter))
                .as("OIDs of closed cases after")
                .containsExactlyInAnyOrderElementsOf(
                        selectOids(
                                List.of(indestructibleRoot),
                                indestructibleRootChildren,
                                List.of(indestructibleChild)));
    }

    @SafeVarargs
    private Set<String> selectOids(List<CaseType>... cases) {
        return Arrays.stream(cases)
                .flatMap(Collection::stream)
                .map(CaseType::getOid)
                .collect(Collectors.toSet());
    }

    private List<CaseType> selectChildren(String oid, List<CaseType> allCases) {
        List<CaseType> directChildren = allCases.stream()
                .filter(c -> c.getParentRef() != null && c.getParentRef().getOid().equals(oid))
                .collect(Collectors.toList());
        List<CaseType> children = new ArrayList<>(directChildren);
        directChildren.forEach(ch ->
                children.addAll(selectChildren(ch.getOid(), allCases)));
        return children;
    }

    @NotNull
    private List<CaseType> selectClosedCases(List<PrismObject<CaseType>> allCases) {
        return allCases.stream()
                .map(c -> c.asObjectable())
                .filter(CaseTypeUtil::isClosed)
                .collect(Collectors.toList());
    }

    private void markIndestructible(String oid, OperationResult result) throws CommonException {
        System.out.println("Marking case " + oid + " as indestructible");
        repositoryService.modifyObject(
                CaseType.class, oid,
                deltaFor(CaseType.class)
                        .item(CaseType.F_INDESTRUCTIBLE)
                        .replace(true)
                        .asItemDeltas(),
                result);
    }

    /** Finds a direct, child-less child of a root that is other than `excludedRoot`. */
    private CaseType selectIndestructibleChild(List<CaseType> closedCases, List<CaseType> closedRootCases, CaseType excludedRoot) {
        for (CaseType aCase : closedCases) {
            if (aCase.getParentRef() != null &&
                    !aCase.getParentRef().getOid().equals(excludedRoot.getOid()) &&
                    isDirectChildOfSomeRoot(aCase, closedRootCases) &&
                    isChildLess(aCase, closedCases)) {
                return aCase; // This is a direct, child-less child of a root that is different from excludedRoot.
            }
        }
        throw new AssertionError("Suitable child case was not found");
    }

    /** Assumes that aCase has a parent. */
    private boolean isDirectChildOfSomeRoot(CaseType aCase, List<CaseType> closedRootCases) {
        return closedRootCases.stream()
                .anyMatch(root -> aCase.getParentRef().getOid().equals(root.getOid()));
    }

    private boolean isChildLess(CaseType aCase, List<CaseType> closedCases) {
        return closedCases.stream()
                .noneMatch(c -> c.getParentRef() != null && c.getParentRef().getOid().equals(aCase.getOid()));
    }

    /**
     * Checks that the notifications are sent out correctly even with auto-completed stages.
     *
     * MID-8587
     */
    @Test
    public void test400NotificationsWithAutoCompletion() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator);

        dummyTransport.clearMessages();

        when("a user with role assignment is created");
        String name = "test400";
        UserType user = new UserType()
                .name(name)
                .assignment(ROLE_AUTOCOMPLETIONS.assignmentTo());
        addObject(user, task, result);

        then("user is not created but case exists");
        assertNoUserByUsername(name);
        assertCase(result, "after")
                .display();

        and("case notifications are OK");
        List<Message> casesNotifications = dummyTransport.getMessages(DUMMY_SIMPLE_WORKFLOW_NOTIFIER_PROCESSES);
        displayCollection("notifications - cases", casesNotifications);
        //noinspection AssertBetweenInconvertibleTypes
        assertThat(casesNotifications).as("cases notifications")
                .singleElement()
                .extracting(m -> m.getSubject())
                .isEqualTo("An approval case has been opened");

        and("work items notifications are OK");
        List<Message> workItemsNotifications =
                new ArrayList<>(dummyTransport.getMessages(DUMMY_SIMPLE_WORKFLOW_NOTIFIER_WORK_ITEMS));
        workItemsNotifications.sort(
                Comparator.comparing(m -> m.getSubject()));
        displayCollection("notifications - work items", workItemsNotifications);
        assertThat(workItemsNotifications).as("work item notifications").hasSize(2);
        Message first = workItemsNotifications.get(0);
        assertThat(first.getSubject()).as("first work item notification subject")
                .isEqualTo("A new work item has been created");
        assertThat(first.getBody()).as("first work item notification body")
                .contains("Stage: 3/3");
        Message second = workItemsNotifications.get(1);
        assertThat(second.getSubject()).as("second work item notification subject")
                .isEqualTo("Work item has been allocated to you");
        assertThat(second.getBody()).as("second work item notification body")
                .contains("Stage: 3/3");
    }

    /** Checks that there is no (flawed) deputy query when there are no assignees present. MID-8134. */
    @Test
    public void test410NoDeputyQueryWhenThereAreNoAssignees() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator);

        when("a user with role assignment is created");
        String name = "test410";
        UserType user = new UserType()
                .name(name)
                .assignment(ROLE_APPROVED_BY_ORG.assignmentTo());

        setTracing(task, createDefaultTracingProfile()); // just to get the whole operation result
        addObject(user, task, result);
        OperationResultRepoSearchAsserter.forResult(result)
                .assertNotContains(
                        r -> r.getTraces().stream().anyMatch(
                                t -> isUserNullDeputyRefSearch(t)));

        then("user is not created but case exists");
        assertNoUserByUsername(name);
        assertCase(result, "after")
                .display();
    }

    /** Specific to MID-8134 */
    private boolean isUserNullDeputyRefSearch(TraceType t) {
        if (!(t instanceof RepositorySearchObjectsTraceType)) {
            return false;
        }
        RepositorySearchObjectsTraceType trace = (RepositorySearchObjectsTraceType) t;
        if (!UserType.COMPLEX_TYPE.equals(trace.getObjectType())) {
            return false;
        }
        QueryType queryBean = trace.getQuery();
        if (queryBean == null) {
            return false;
        }
        ObjectQuery query;
        try {
            query = PrismContext.get().getQueryConverter().createObjectQuery(UserType.class, queryBean);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        var filter = query.getFilter();
        if (!(filter instanceof RefFilter)) {
            return false;
        }
        RefFilter refFilter = (RefFilter) filter;
        return refFilter.getPath().equivalent(UserType.F_DELEGATED_REF)
                && refFilter.hasNoValue();
    }

    /** Checks that there are separate queries if multiple approver relations are searched for. MID-8134. */
    @Test
    public void test420SeparateRelationQueries() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator);

        when("a user with role assignment is created");
        String name = "test420";
        UserType user = new UserType()
                .name(name)
                .assignment(ROLE_APPROVED_BY_MULTIPLE_RELATIONS.assignmentTo());

        setTracing(task, createDefaultTracingProfile()); // just to get the whole operation result
        addObject(user, task, result);
        OperationResultRepoSearchAsserter.forResult(result)
                .forEachRepoSearch(
                        r -> r.getTraces().forEach(
                                t -> checkNoMultiRelationsOrFilter(t)));

        then("user is not created but case exists");
        assertNoUserByUsername(name);
        var workItem = assertCase(result, "after")
                .display()
                .subcases()
                .assertSubcases(2)
                .singleWithoutApprovalSchema().display().end() // user ADD
                .singleWithApprovalSchema() // assignment ADD
                .display()
                .workItems()
                .single()
                .assertAssignees(USER_APPROVER_BY_MULTIPLE_RELATIONS.oid)
                .getRealValue();

        when("work item is approved");
        approveWorkItem(workItem, task, result);

        CaseType parentCase = getCase(CaseTypeUtil.getCaseRequired(workItem).getParentRef().getOid());
        waitForCaseClose(parentCase);

        then("user with assignment exists");
        assertUserAfterByUsername(name)
                .assignments()
                .assertRole(ROLE_APPROVED_BY_MULTIPLE_RELATIONS.oid);
    }

    @Test
    public void test430ApproveWithSkipLastStage() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator);

        when("a user with role assignment is created");
        String name = "test430";
        UserType user = new UserType()
                .name(name)
                .assignment(ROLE_APPROVE_WITH_SKIP_LAST_STAGE.assignmentTo());

//        setTracing(task, createDefaultTracingProfile()); // just to get the whole operation result
        addObject(user, task, result);

        then("user is not created but case exists");
        assertNoUserByUsername(name);
        var workItem = assertCase(result, "after")
                .display()
                .subcases()
                .assertSubcases(2)
                .singleWithoutApprovalSchema().display().end() // user ADD
                .singleWithApprovalSchema() // assignment ADD
                .display()
                .workItems()
                .single()
                .assertAssignees(userAdministrator.getOid())
                .getRealValue();

        when("work item is approved");
        approveWorkItem(workItem, task, result);

        @NotNull CaseType caseBean = CaseTypeUtil.getCaseRequired(workItem);
        waitForCaseClose(caseBean);

        then("user with assignment exists");
        assertCase(getCase(caseBean.getOid()), "case")
                .assertApproved();
    }

    private void checkNoMultiRelationsOrFilter(TraceType t) {
        if (!(t instanceof RepositorySearchObjectsTraceType)) {
            return;
        }
        RepositorySearchObjectsTraceType trace = (RepositorySearchObjectsTraceType) t;
        QueryType queryBean = trace.getQuery();
        if (queryBean == null) {
            return;
        }
        ObjectQuery query;
        try {
            query = prismContext.getQueryConverter().createObjectQuery(
                    prismContext.getSchemaRegistry().determineCompileTimeClass(trace.getObjectType()),
                    queryBean);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        ObjectFilter filter = query.getFilter();
        if (filter == null) {
            return;
        }
        filter.accept(f -> {
            if (f instanceof OrFilter) {
                Set<QName> relations = ((OrFilter) f).getConditions().stream()
                        .filter(cond -> cond instanceof RefFilter)
                        .flatMap(cond -> emptyIfNull(((RefFilter) cond).getValues()).stream())
                        .map(value -> value.getRelation())
                        .collect(Collectors.toSet());
                if (relations.size() > 1) {
                    throw new AssertionError("Multi-relation filter found: " + relations + ", " + f);
                }
            }
        });
    }
}
