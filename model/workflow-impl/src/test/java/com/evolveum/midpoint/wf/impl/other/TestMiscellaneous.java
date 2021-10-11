/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.other;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.TestResource;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractWfTestPolicy {

    private static final File TEST_RESOURCE_DIR = new File("src/test/resources/miscellaneous");

    private static final TestResource<RoleType> ROLE_SAILOR = new TestResource<>(TEST_RESOURCE_DIR, "role-sailor.xml", "3ccc0a00-6a3b-4ae0-94a3-d45fc457f63f");
    private static final TestResource<RoleType> ROLE_CAPTAIN = new TestResource<>(TEST_RESOURCE_DIR, "role-captain.xml", "15a99cf1-5886-44d4-8aaf-7e1f46ccec36");
    private static final TestResource<UserType> USER_SCOTT = new TestResource<>(TEST_RESOURCE_DIR, "user-scott.xml", "929c49ed-0100-4068-b8e4-137bd8ebd6b2");

    private static final TestResource<RoleType> METAROLE_PRIZE = new TestResource<>(TEST_RESOURCE_DIR, "metarole-prize.xml", "2330f9df-83bc-4270-86fc-27fca2b616a7");
    private static final TestResource<RoleType> METAROLE_APPROVE_UNASSIGN = new TestResource<>(TEST_RESOURCE_DIR, "metarole-approve-unassign.xml", "e5144353-c39d-445c-bf15-c4b80ce75918");

    private static final TestResource<RoleType> ROLE_GOLD = new TestResource<>(TEST_RESOURCE_DIR, "role-gold.xml", "0b3ad53e-7c1d-41d0-a447-ce94cd25c46a");
    private static final TestResource<RoleType> ROLE_SILVER = new TestResource<>(TEST_RESOURCE_DIR, "role-silver.xml", "ee5206f8-930a-4c85-bfee-c16e4462df23");
    private static final TestResource<RoleType> ROLE_BRONZE = new TestResource<>(TEST_RESOURCE_DIR, "role-bronze.xml", "f16f4dd7-2830-4d0a-b6ed-9fbf253dbaf3");

    private static final TestResource<ObjectTemplateType> TEMPLATE_ASSIGNING_CAPTAIN = new TestResource<>(TEST_RESOURCE_DIR, "template-assigning-captain.xml", "18ac3da2-f2fa-496a-8e54-789a090ff492");
    private static final TestResource<ObjectTemplateType> TEMPLATE_ASSIGNING_CAPTAIN_AFTER = new TestResource<>(TEST_RESOURCE_DIR, "template-assigning-captain-after.xml", "ace5d8f0-f54b-4f1b-92c0-8fa104a8fe84");
    private static final TestResource<RoleType> ROLE_ASSIGNING_CAPTAIN = new TestResource<>(TEST_RESOURCE_DIR, "role-assigning-captain.xml", "4bdd7ccc-8c52-41ff-a975-0313ec788507");

    private static final TestResource<UserType> USER_SCROOGE = new TestResource<>(TEST_RESOURCE_DIR, "user-scrooge.xml", "edf53304-2da0-4a7c-82b4-74fe35dcbc6e");
    private static final TestResource<UserType> USER_GIZMODUCK = new TestResource<>(TEST_RESOURCE_DIR, "user-gizmoduck.xml", "6d0a7fce-b698-4f1d-95ce-14246452add5");
    private static final TestResource<UserType> USER_LAUNCHPAD = new TestResource<>(TEST_RESOURCE_DIR, "user-launchpad.xml", "00880478-d006-4fc8-9d3a-87b5ec546c40");
    private static final TestResource<RoleType> ROLE_VAULT_ACCESS = new TestResource<>(TEST_RESOURCE_DIR, "role-vault-access.xml", "f6f95936-8714-4c7d-abdf-6cd3e6d2d6cc");
    private static final TestResource<RoleType> ROLE_ACCOUNTANT = new TestResource<>(TEST_RESOURCE_DIR, "role-accountant.xml", "5653fc70-3007-4f62-82dd-a36e0673505b");

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
    }

    @Test
    public void test100RequesterComment() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        dummyAuditService.clear();

        OperationBusinessContextType businessContext = new OperationBusinessContextType();
        final String REQUESTER_COMMENT = "req.comment";
        businessContext.setComment(REQUESTER_COMMENT);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userJackOid, ROLE_SAILOR.oid, RoleType.COMPLEX_TYPE, null, null, null, true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, ModelExecuteOptions.createRequestBusinessContext(businessContext), task, result);

        assertNotAssignedRole(userJackOid, ROLE_SAILOR.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);

        // WHEN
        workflowManager.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK"),
                null, task, result);

        // THEN
        CaseType aCase = getCase(CaseWorkItemUtil.getCaseRequired(workItem).getOid());
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
                    record.getPropertyValues(WorkflowConstants.AUDIT_REQUESTER_COMMENT));
        }

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        AssignmentType assignment = assertAssignedRole(userJackOid, ROLE_SAILOR.oid, result);
        display("assignment after creation", assignment);
        MetadataType metadata = assignment.getMetadata();
        assertNotNull("Null request timestamp in metadata", metadata.getRequestTimestamp());
        assertRefEquals("Wrong requestorRef in metadata", ObjectTypeUtil.createObjectRef(userAdministrator, prismContext), metadata.getRequestorRef());
        assertEquals("Wrong requestorComment in metadata", REQUESTER_COMMENT, metadata.getRequestorComment());
    }

    @Test
    public void test105RequesterCommentImmediate() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        dummyAuditService.clear();

        OperationBusinessContextType businessContext = new OperationBusinessContextType();
        final String REQUESTER_COMMENT = "req.comment";
        businessContext.setComment(REQUESTER_COMMENT);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userJackOid, ROLE_CAPTAIN.oid, RoleType.COMPLEX_TYPE, null, null, null, true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        ModelExecuteOptions options = ModelExecuteOptions.createRequestBusinessContext(businessContext);
        options.setExecuteImmediatelyAfterApproval(true);
        modelService.executeChanges(deltas, options, task, result);

        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);

        // WHEN
        workflowManager.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK"),
                null, task, result);

        // THEN
        CaseType aCase = getCase(CaseWorkItemUtil.getCaseRequired(workItem).getOid());
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
                    record.getPropertyValues(WorkflowConstants.AUDIT_REQUESTER_COMMENT));
        }

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        AssignmentType assignment = assertAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);
        display("assignment after creation", assignment);
        MetadataType metadata = assignment.getMetadata();
        assertNotNull("Null request timestamp in metadata", metadata.getRequestTimestamp());
        assertRefEquals("Wrong requestorRef in metadata", ObjectTypeUtil.createObjectRef(userAdministrator, prismContext), metadata.getRequestorRef());
        assertEquals("Wrong requestorComment in metadata", REQUESTER_COMMENT, metadata.getRequestorComment());
    }

    @Test
    public void test110RequestPrunedRole() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        ModelExecuteOptions options = ModelExecuteOptions
                .createPartialProcessing(new PartialProcessingOptionsType().approvals(PartialProcessingTypeType.SKIP));
        assignRole(userJackOid, ROLE_GOLD.oid, options, task, result);
        assertAssignedRole(getUser(userJackOid), ROLE_GOLD.oid);

        // WHEN

        assignRole(userJackOid, ROLE_SILVER.oid, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertInProgress("Operation NOT in progress", result);

        assertNotAssignedRole(userJackOid, ROLE_SILVER.oid, result);

        // complete the work item related to assigning role silver
        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);
        workflowManager.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                null, task, result);

        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItem);
        CaseType rootCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(rootCase);

        assertNotAssignedRole(userJackOid, ROLE_GOLD.oid, result);            // should be pruned without approval
    }

    @Test
    public void test200GetRoleByTemplate() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN
        setDefaultUserTemplate(TEMPLATE_ASSIGNING_CAPTAIN.oid);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);

        // WHEN
        // some innocent change
        modifyUserChangePassword(userJackOid, "PaSsWoRd123", task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);
    }

    @Test
    public void test210GetRoleByTemplateAfterAssignments() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN
        setDefaultUserTemplate(TEMPLATE_ASSIGNING_CAPTAIN_AFTER.oid);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);

        // WHEN
        // some innocent change
        modifyUserChangePassword(userJackOid, "PaSsWoRd123", task, result);
        // here the captain role appears in evaluatedAssignmentsTriple only in secondary phase; so no approvals are triggered

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);
    }

    @Test
    public void test220GetRoleByFocusMappings() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN
        setDefaultUserTemplate(null);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);

        // WHEN
        assignRole(userJackOid, ROLE_ASSIGNING_CAPTAIN.oid, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);
    }

    @Test
    public void test250SkippingApprovals() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN
        setDefaultUserTemplate(null);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);

        // WHEN
        ObjectDelta<? extends ObjectType> delta =
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(ObjectTypeUtil.createAssignmentTo(ROLE_CAPTAIN.oid, ObjectTypes.ROLE, prismContext))
                        .asObjectDelta(userJackOid);
        ModelExecuteOptions options = ModelExecuteOptions.createPartialProcessing(
                new PartialProcessingOptionsType().approvals(PartialProcessingTypeType.SKIP));
        modelService.executeChanges(Collections.singletonList(delta), options, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);
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
        unassignAllRoles(userJackOid);

        // @formatter:off
        ObjectDelta<? extends ObjectType> delta =
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_CAPTAIN.oid, ObjectTypes.ROLE, prismContext))
                        .asObjectDelta(userJackOid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);

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
        unassignAllRoles(userJackOid);

        // @formatter:off
        ObjectDelta<? extends ObjectType> delta =
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_CAPTAIN.oid, ObjectTypes.ROLE, prismContext))
                        .asObjectDelta(userJackOid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN.oid, result);

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
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_VAULT_ACCESS.oid, ObjectTypes.ROLE, prismContext))
                        .asObjectDelta(USER_LAUNCHPAD.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(USER_LAUNCHPAD.oid, ROLE_VAULT_ACCESS.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);

        when();

        modelInteractionService.runUnderPowerOfAttorneyChecked(() -> {
            AbstractWorkItemOutputType output = new AbstractWorkItemOutputType(prismContext)
                    .outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
            workflowService.completeWorkItem(CaseWorkItemUtil.getId(workItem), output, task, result);
            return null;
        }, USER_SCROOGE.object, task, result);

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
                .assertNameOrig("Assigning role \"vault-access\" to user \"launchpad\"")
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
                        .assertNameOrig("Assigning role \"vault-access\" to user \"launchpad\"")
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
                            .add(ObjectTypeUtil.createAssignmentTo(ROLE_VAULT_ACCESS.oid, ObjectTypes.ROLE, prismContext))
                        .asObjectDelta(USER_LAUNCHPAD.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);
        assertNotAssignedRole(USER_LAUNCHPAD.oid, ROLE_VAULT_ACCESS.oid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);

        when();

        login(USER_GIZMODUCK.object);

        modelInteractionService.runUnderPowerOfAttorneyChecked(() -> {
            AbstractWorkItemOutputType output = new AbstractWorkItemOutputType(prismContext)
                    .outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
            workflowService.completeWorkItem(CaseWorkItemUtil.getId(workItem), output, task, result);
            return null;
        }, USER_SCROOGE.object, task, result);

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
                .assertNameOrig("Assigning role \"vault-access\" to user \"launchpad\"")
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
                        .assertNameOrig("Assigning role \"vault-access\" to user \"launchpad\"")
                        .assertStageNumber(1)
                        .assertOriginalAssigneeRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE)
                        .assertPerformerRef(USER_SCROOGE.oid, UserType.COMPLEX_TYPE) // we should perhaps list attorney here as well
                        .assertClosed()
                        .assertRejected();

        // @formatter:on
    }

}
