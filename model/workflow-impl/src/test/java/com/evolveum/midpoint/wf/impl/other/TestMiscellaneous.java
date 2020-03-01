/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.other;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractWfTestPolicy {

    private static final File TEST_RESOURCE_DIR = new File("src/test/resources/miscellaneous");

    private static final File ROLE_SAILOR_FILE = new File(TEST_RESOURCE_DIR, "role-sailor.xml");
    private static final String ROLE_SAILOR_OID = "3ccc0a00-6a3b-4ae0-94a3-d45fc457f63f";

    private static final File ROLE_CAPTAIN_FILE = new File(TEST_RESOURCE_DIR, "role-captain.xml");
    private static final String ROLE_CAPTAIN_OID = "15a99cf1-5886-44d4-8aaf-7e1f46ccec36";

    private static final File USER_SCOTT_FILE = new File(TEST_RESOURCE_DIR, "user-scott.xml");
    private static final String USER_SCOTT_OID = "929c49ed-0100-4068-b8e4-137bd8ebd6b2";

    private static final File METAROLE_PRIZE_FILE = new File(TEST_RESOURCE_DIR, "metarole-prize.xml");

    private static final File METAROLE_APPROVE_UNASSIGN_FILE = new File(TEST_RESOURCE_DIR, "metarole-approve-unassign.xml");

    private static final File ROLE_GOLD_FILE = new File(TEST_RESOURCE_DIR, "role-gold.xml");
    private static final String ROLE_GOLD_OID = "0b3ad53e-7c1d-41d0-a447-ce94cd25c46a";

    private static final File ROLE_SILVER_FILE = new File(TEST_RESOURCE_DIR, "role-silver.xml");
    private static final String ROLE_SILVER_OID = "ee5206f8-930a-4c85-bfee-c16e4462df23";

    private static final File ROLE_BRONZE_FILE = new File(TEST_RESOURCE_DIR, "role-bronze.xml");
    //private static final String ROLE_BRONZE_OID = "f16f4dd7-2830-4d0a-b6ed-9fbf253dbaf3";

    private static final File TEMPLATE_ASSIGNING_CAPTAIN_FILE = new File(TEST_RESOURCE_DIR, "template-assigning-captain.xml");
    private static final String TEMPLATE_ASSIGNING_CAPTAIN_OID = "18ac3da2-f2fa-496a-8e54-789a090ff492";

    private static final File TEMPLATE_ASSIGNING_CAPTAIN_AFTER_FILE = new File(TEST_RESOURCE_DIR, "template-assigning-captain-after.xml");
    private static final String TEMPLATE_ASSIGNING_CAPTAIN_AFTER_OID = "ace5d8f0-f54b-4f1b-92c0-8fa104a8fe84";

    private static final File ROLE_ASSIGNING_CAPTAIN_FILE = new File(TEST_RESOURCE_DIR, "role-assigning-captain.xml");
    private static final String ROLE_ASSIGNING_CAPTAIN_OID = "4bdd7ccc-8c52-41ff-a975-0313ec788507";

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_SAILOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_CAPTAIN_FILE, initResult);

        repoAddObjectFromFile(METAROLE_PRIZE_FILE, initResult);
        repoAddObjectFromFile(METAROLE_APPROVE_UNASSIGN_FILE, initResult);
        repoAddObjectFromFile(ROLE_GOLD_FILE, initResult);
        repoAddObjectFromFile(ROLE_SILVER_FILE, initResult);
        repoAddObjectFromFile(ROLE_BRONZE_FILE, initResult);

        addAndRecompute(USER_SCOTT_FILE, initTask, initResult);

        repoAddObjectFromFile(TEMPLATE_ASSIGNING_CAPTAIN_FILE, initResult);
        repoAddObjectFromFile(TEMPLATE_ASSIGNING_CAPTAIN_AFTER_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGNING_CAPTAIN_FILE, initResult);
    }

    @Test
    public void test100RequesterComment() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestResult();

        // GIVEN

        dummyAuditService.clear();

        OperationBusinessContextType businessContext = new OperationBusinessContextType();
        final String REQUESTER_COMMENT = "req.comment";
        businessContext.setComment(REQUESTER_COMMENT);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userJackOid, ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE, null, null, null, true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, ModelExecuteOptions.createRequestBusinessContext(businessContext), task, result);

        assertNotAssignedRole(userJackOid, ROLE_SAILOR_OID, task, result);

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
        assertEquals("Wrong original assignee OID", USER_SCOTT_OID, event2.getOriginalAssigneeRef().getOid());

        display("audit", dummyAuditService);
        List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        assertEquals("Wrong # of process instance audit records", 2, records.size());
        for (int i = 0; i < records.size(); i++) {
            AuditEventRecord record = records.get(i);
            assertEquals("Wrong requester comment in audit record #" + i, Collections.singleton(REQUESTER_COMMENT),
                    record.getPropertyValues(WorkflowConstants.AUDIT_REQUESTER_COMMENT));
        }

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        AssignmentType assignment = assertAssignedRole(userJackOid, ROLE_SAILOR_OID, result);
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
        OperationResult result = getTestResult();

        // GIVEN

        dummyAuditService.clear();

        OperationBusinessContextType businessContext = new OperationBusinessContextType();
        final String REQUESTER_COMMENT = "req.comment";
        businessContext.setComment(REQUESTER_COMMENT);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userJackOid, ROLE_CAPTAIN_OID, RoleType.COMPLEX_TYPE, null, null, null, true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        ModelExecuteOptions options = ModelExecuteOptions.createRequestBusinessContext(businessContext);
        options.setExecuteImmediatelyAfterApproval(true);
        modelService.executeChanges(deltas, options, task, result);

        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN_OID, task, result);

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
        assertEquals("Wrong original assignee OID", USER_SCOTT_OID, event2.getOriginalAssigneeRef().getOid());

        display("audit", dummyAuditService);
        List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        assertEquals("Wrong # of process instance audit records", 2, records.size());
        for (int i = 0; i < records.size(); i++) {
            AuditEventRecord record = records.get(i);
            assertEquals("Wrong requester comment in audit record #" + i, Collections.singleton(REQUESTER_COMMENT),
                    record.getPropertyValues(WorkflowConstants.AUDIT_REQUESTER_COMMENT));
        }

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        AssignmentType assignment = assertAssignedRole(userJackOid, ROLE_CAPTAIN_OID, result);
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
        OperationResult result = getTestResult();

        // GIVEN

        ModelExecuteOptions options = ModelExecuteOptions
                .createPartialProcessing(new PartialProcessingOptionsType().approvals(PartialProcessingTypeType.SKIP));
        assignRole(userJackOid, ROLE_GOLD_OID, options, task, result);
        assertAssignedRole(getUser(userJackOid), ROLE_GOLD_OID);

        // WHEN

        assignRole(userJackOid, ROLE_SILVER_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertInProgress("Operation NOT in progress", result);

        assertNotAssignedRole(userJackOid, ROLE_SILVER_OID, task, result);

        // complete the work item related to assigning role silver
        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);
        workflowManager.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                null, task, result);

        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItem);
        CaseType rootCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(rootCase);

        assertNotAssignedRole(userJackOid, ROLE_GOLD_OID, task, result);            // should be pruned without approval
    }

    @Test
    public void test200GetRoleByTemplate() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestResult();

        // GIVEN
        setDefaultUserTemplate(TEMPLATE_ASSIGNING_CAPTAIN_OID);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN_OID, task, result);

        // WHEN
        // some innocent change
        modifyUserChangePassword(userJackOid, "PaSsWoRd123", task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN_OID, result);
    }

    @Test
    public void test210GetRoleByTemplateAfterAssignments() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestResult();

        // GIVEN
        setDefaultUserTemplate(TEMPLATE_ASSIGNING_CAPTAIN_AFTER_OID);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN_OID, task, result);

        // WHEN
        // some innocent change
        modifyUserChangePassword(userJackOid, "PaSsWoRd123", task, result);
        // here the captain role appears in evaluatedAssignmentsTriple only in secondary phase; so no approvals are triggered

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN_OID, result);
    }

    @Test
    public void test220GetRoleByFocusMappings() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestResult();

        // GIVEN
        setDefaultUserTemplate(null);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN_OID, task, result);

        // WHEN
        assignRole(userJackOid, ROLE_ASSIGNING_CAPTAIN_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN_OID, result);
    }

    @Test
    public void test250SkippingApprovals() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestResult();

        // GIVEN
        setDefaultUserTemplate(null);
        unassignAllRoles(userJackOid);
        assertNotAssignedRole(userJackOid, ROLE_CAPTAIN_OID, task, result);

        // WHEN
        ObjectDelta<? extends ObjectType> delta =
                prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                        .add(ObjectTypeUtil.createAssignmentTo(ROLE_CAPTAIN_OID, ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ModelExecuteOptions options = ModelExecuteOptions.createPartialProcessing(
                new PartialProcessingOptionsType().approvals(PartialProcessingTypeType.SKIP));
        modelService.executeChanges(Collections.singletonList(delta), options, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(userJackOid, ROLE_CAPTAIN_OID, result);
    }
}
