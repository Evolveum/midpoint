/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertReferenceValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.model.test.DummyTransport;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.util.ApprovalUtils;

@SuppressWarnings({ "FieldCanBeLocal", "SameParameterValue" })
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestStrings extends AbstractStoryTest {

    @Autowired private WorkflowService workflowService;
    @Autowired private DummyTransport dummyTransport;

    private static final File TEST_DIR = new File("src/test/resources/strings");
    private static final File ORG_DIR = new File(TEST_DIR, "orgs");
    private static final File ROLES_DIR = new File(TEST_DIR, "roles");
    private static final File ROLES_SPECIFIC_DIR = new File(TEST_DIR, "roles-specific");
    private static final File USERS_DIR = new File(TEST_DIR, "users");

    private static final File ORG_MONKEY_ISLAND_FILE = new File(ORG_DIR, "0-org-monkey-island-modified.xml");
    private static final TestResource<OrgType> ORG_TEAMS = new TestResource<>(ORG_DIR, "1-teams.xml", "b7cbed1f-5e73-4455-b88a-b5b3ac2a0f54");
    private static final TestResource<OrgType> ORG_ROLE_CATALOG = new TestResource<>(ORG_DIR, "2-role-catalog.xml", "b77c512a-85b9-470e-a7ab-a55b8f187674");
    private static final TestResource<OrgType> ORG_SECURITY_APPROVERS = new TestResource<>(ORG_DIR, "security-approvers.xml", "a14afc10-e4a2-48a4-abfd-e8a2399f98d3");
    private static final TestResource<OrgType> ORG_SOD_APPROVERS = new TestResource<>(ORG_DIR, "sod-approvers.xml", "cb73377a-da8d-4fbe-b174-19879bae9032");

    private static final TestResource<RoleType> ROLE_END_USER = new TestResource<>(ROLES_DIR, "role-end-user.xml", "00000000-0000-0000-0000-000000000008");
    private static final TestResource<FormType> FORM_USER_DETAILS = new TestResource<>(ROLES_DIR, "form-user-details.xml", "6a1874a7-1e60-43b3-8d67-7f76484dead5");
    private static final TestResource<RoleType> METAROLE_APPROVAL_ROLE_APPROVERS_FIRST = new TestResource<>(ROLES_DIR, "metarole-approval-role-approvers-first.xml", "e3c28c94-798a-4f93-85f8-de7cbe37315b");
    private static final TestResource<RoleType> METAROLE_APPROVAL_ROLE_APPROVERS_FORM = new TestResource<>(ROLES_DIR, "metarole-approval-role-approvers-form.xml", "df092a19-68f0-4056-adf8-482f8fd26410");
    private static final TestResource<RoleType> METAROLE_APPROVAL_SECURITY = new TestResource<>(ROLES_DIR, "metarole-approval-security.xml", "9c0c224f-f279-44b5-b906-8e8418a651a2");

    private static final File ROLE_A_TEST_1 = new File(ROLES_SPECIFIC_DIR, "a-test-1.xml");
    private static String roleATest1Oid;
    private static final File ROLE_A_TEST_2A = new File(ROLES_SPECIFIC_DIR, "a-test-2a.xml");
    private static final File ROLE_A_TEST_2B = new File(ROLES_SPECIFIC_DIR, "a-test-2b.xml");
    private static final File ROLE_A_TEST_3A = new File(ROLES_SPECIFIC_DIR, "a-test-3a.xml");
    private static final File ROLE_A_TEST_3B = new File(ROLES_SPECIFIC_DIR, "a-test-3b.xml");
    private static final File ROLE_A_TEST_3X = new File(ROLES_SPECIFIC_DIR, "a-test-3x.xml");
    private static final File ROLE_A_TEST_3Y = new File(ROLES_SPECIFIC_DIR, "a-test-3y.xml");
    private static final File ROLE_A_TEST_4 = new File(ROLES_SPECIFIC_DIR, "a-test-4.xml");
    private static String roleATest4Oid;

    private static final File USER_BARKEEPER_FILE = new File(USERS_DIR, "barkeeper.xml");
    private static final File USER_BOB_FILE = new File(USERS_DIR, "bob.xml");
    private static String userBobOid;
    private static final File USER_CARLA_FILE = new File(USERS_DIR, "carla.xml");
    private static String userCarlaOid;
    private static final File USER_CHEESE_FILE = new File(USERS_DIR, "cheese.xml");
    private static String userCheeseOid;
    private static final File USER_CHEF_FILE = new File(USERS_DIR, "chef.xml");
    private static String userChefOid;
    private static final File USER_ELAINE_FILE = new File(USERS_DIR, "elaine.xml");
    private static String userElaineOid;
    private static final File USER_GUYBRUSH_FILE = new File(USERS_DIR, "guybrush.xml");
    private static String userGuybrushOid;
    private static final File USER_LECHUCK_FILE = new File(USERS_DIR, "lechuck.xml");
    private static String userLechuckOid;
    private static final File USER_LECHUCK_DEPUTY_FILE = new File(USERS_DIR, "lechuck-deputy.xml");
    private static final File USER_LECHUCK_DEPUTY_DEPUTY_FILE = new File(USERS_DIR, "lechuck-deputy-deputy.xml");
    private static final File USER_LECHUCK_DEPUTY_LIMITED_FILE = new File(USERS_DIR, "lechuck-deputy-limited.xml");

    private static final File CONFIG_WITH_GLOBAL_RULES_FILE = new File(ROLES_DIR, "global-policy-rules.xml");

    private static final String DUMMY_WORK_ITEM_LIFECYCLE = "dummy:workItemLifecycle";
    private static final String DUMMY_WORK_ITEM_ALLOCATION = "dummy:workItemAllocation";
    private static final String DUMMY_PROCESS = "dummy:process";

    private static final int CASE_WAIT_TIMEOUT = 40000;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        transplantGlobalPolicyRulesAdd(CONFIG_WITH_GLOBAL_RULES_FILE, initTask, initResult);

        // we prefer running trigger scanner by hand
        reimportWithNoSchedule(TASK_TRIGGER_SCANNER_OID, TASK_TRIGGER_SCANNER_FILE, initTask, initResult);
        // and we don't need validity scanner
        taskManager.suspendAndDeleteTasks(Collections.singletonList(TASK_VALIDITY_SCANNER_OID), 60000L, true, initResult);

        Task triggerScanner = taskManager.getTaskPlain(TASK_TRIGGER_SCANNER_OID, initResult);
        display("triggerScanner", triggerScanner);

        // import of story objects
        repoAddObjectsFromFile(ORG_MONKEY_ISLAND_FILE, OrgType.class, initResult);
        repoAdd(ORG_TEAMS, initResult);
        repoAdd(ORG_ROLE_CATALOG, initResult);
        repoAdd(ORG_SECURITY_APPROVERS, initResult);
        repoAdd(ORG_SOD_APPROVERS, initResult);

        repoAdd(ROLE_END_USER, initResult);
        repoAdd(FORM_USER_DETAILS, initResult);
        repoAdd(METAROLE_APPROVAL_ROLE_APPROVERS_FIRST, initResult);
        repoAdd(METAROLE_APPROVAL_ROLE_APPROVERS_FORM, initResult);
        repoAdd(METAROLE_APPROVAL_SECURITY, initResult);

        roleATest1Oid = addAndRecompute(ROLE_A_TEST_1, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_2A, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_2B, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3A, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3B, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3X, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3Y, initTask, initResult);
        roleATest4Oid = addAndRecompute(ROLE_A_TEST_4, initTask, initResult);

        addAndRecomputeUser(USER_BARKEEPER_FILE, initTask, initResult);
        userBobOid = addAndRecomputeUser(USER_BOB_FILE, initTask, initResult);
        userCarlaOid = addAndRecomputeUser(USER_CARLA_FILE, initTask, initResult);
        userCheeseOid = addAndRecomputeUser(USER_CHEESE_FILE, initTask, initResult);
        userChefOid = addAndRecomputeUser(USER_CHEF_FILE, initTask, initResult);
        userElaineOid = addAndRecomputeUser(USER_ELAINE_FILE, initTask, initResult);
        userGuybrushOid = addAndRecomputeUser(USER_GUYBRUSH_FILE, initTask, initResult);
        userLechuckOid = addAndRecomputeUser(USER_LECHUCK_FILE, initTask, initResult);
        addAndRecomputeUser(USER_LECHUCK_DEPUTY_FILE, initTask, initResult);
        addAndRecomputeUser(USER_LECHUCK_DEPUTY_DEPUTY_FILE, initTask, initResult);
        addAndRecomputeUser(USER_LECHUCK_DEPUTY_LIMITED_FILE, initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Test
    public void test000Sanity() throws Exception {
        // TODO
    }

    //region Basic approval
    @Test
    public void test100SimpleAssignmentStart() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        when();
        assignRole(userBobOid, roleATest1Oid, task, task.getResult());

        // THEN
        then();
        assertNotAssignedRole(getUser(userBobOid), roleATest1Oid);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItem);
        display("wfTask", aCase);

        assertTriggers(aCase, 2);

        ApprovalContextType actx = aCase.getApprovalContext();
        ApprovalSchemaType schema = actx.getApprovalSchema();
        assertEquals("Wrong # of approval levels", 3, schema.getStage().size());
        assertApprovalLevel(schema, 1, "Line managers", "P5D", 2);
        assertApprovalLevel(schema, 2, "Security", "P7D", 1);
        assertApprovalLevel(schema, 3, "Role approvers (all)", "P5D", 2);
        assertStage(aCase, 1, 3, "Line managers", null);
        assertAssignee(workItem, userLechuckOid, userLechuckOid);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);

        assertEquals("Wrong # of work items lifecycle messages", 3, lifecycleMessages.size());
        Map<String,Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "A new work item has been created",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "A new work item has been created",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "A new work item has been created",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");

        assertEquals("Wrong # of work items allocation messages", 3, allocationMessages.size());
        Map<String,Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "Work item has been allocated to you",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted2.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "Work item has been allocated to you",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted2.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "Work item has been allocated to you",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");

        assertEquals("Wrong # of process messages", 1, processMessages.size());
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "Workflow process instance has been started",
                "Process instance name: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test102SimpleAssignmentApproveByLechuck() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        login(userAdministrator);
        CaseWorkItemType workItem = getWorkItem(task, result);

        // WHEN
        when();
        PrismObject<UserType> lechuck = getUserFromRepo(userLechuckOid);
        login(lechuck);
        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK. LeChuck"),
                task, result);

        // THEN
        then();
        login(userAdministrator);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 1st approval", workItems);
        assertEquals("Wrong # of work items on level 2", 2, workItems.size());
        CaseType aCase = getCase(CaseWorkItemUtil.getCaseRequired(workItem).getOid());
        display("case after 1st approval", aCase);

        assertStage(aCase, 2, 3, "Security", null);
        assertTriggers(aCase, 4);

        // notifications
        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertEquals("Wrong # of work items lifecycle messages", 5, lifecycleMessages.size());
        assertEquals("Wrong # of work items allocation messages", 5, allocationMessages.size());
        assertNull("process messages", processMessages);

        Map<String,Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Elaine Marley (elaine)", "(in 7 days)", "^Result:");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        Map<String,Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted2.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted2.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted2.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)", "Allocated to: Elaine Marley (elaine)",
                "(in 7 days)", "^Result:");
        assertMessage(sorted2.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        // events
        List<CaseEventType> events = assertEvents(aCase, 2);
        assertCompletionEvent(events.get(1), userLechuckOid, userLechuckOid, 1, WorkItemOutcomeType.APPROVE, "OK. LeChuck");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test104SimpleAssignmentApproveByAdministrator() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        login(userAdministrator);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 1st approval", workItems);
        CaseWorkItemType elaineWorkItem = workItems.stream()
                .filter(wi -> userElaineOid.equals(wi.getOriginalAssigneeRef().getOid()))
                .findFirst().orElseThrow(() -> new AssertionError("No work item for elaine"));

        // WHEN
        // Second approval
        workflowService.completeWorkItem(WorkItemId.of(elaineWorkItem),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK. Security."),
                task, result);

        // THEN
        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 2nd approval", workItems);
        assertEquals("Wrong # of work items on level 3", 2, workItems.size());
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItems.get(0));
        display("wfTask after 2nd approval", aCase);

        assertStage(aCase, 3, 3, "Role approvers (all)", null);
        assertTriggers(aCase, 4);

        Map<String, CaseWorkItemType> workItemsMap = sortByOriginalAssignee(workItems);
        assertNotNull("chef is not an approver", workItemsMap.get(userChefOid));
        assertNotNull("cheese is not an approver", workItemsMap.get(userCheeseOid));

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertEquals("Wrong # of work items lifecycle messages", 4, lifecycleMessages.size());
        assertEquals("Wrong # of work items allocation messages", 4, allocationMessages.size());
        assertNull("process messages", processMessages);

        Map<String,Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)", "Allocated to: Elaine Marley (elaine)",
                "Carried out by: midPoint Administrator (administrator)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "Work item has been cancelled",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "^Result:", "^Deadline:", "^Carried out by:");
        assertMessage(sorted.get("cheese@evolveum.com"), "cheese@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Ignatius Cheese (cheese)", "^Result:", "(in 5 days)");
        assertMessage(sorted.get("chef@evolveum.com"), "chef@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Scumm Bar Chef (chef)", "^Result:", "(in 5 days)");

        Map<String,Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)", "Allocated to: Elaine Marley (elaine)",
                "Carried out by: midPoint Administrator (administrator)", "Result: APPROVED", "^Deadline:");
        assertMessage(sorted2.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "Work item has been cancelled",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "^Result:", "^Deadline:", "^Carried out by:");
        assertMessage(sorted2.get("cheese@evolveum.com"), "cheese@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Ignatius Cheese (cheese)", "^Result:", "(in 5 days)");
        assertMessage(sorted2.get("chef@evolveum.com"), "chef@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Scumm Bar Chef (chef)", "^Result:", "(in 5 days)");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test106SimpleAssignmentApproveByCheese() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        login(userAdministrator);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        Map<String, CaseWorkItemType> workItemsMap = sortByOriginalAssignee(workItems);

        // WHEN
        login(getUser(userCheeseOid));
        workflowService.completeWorkItem(WorkItemId.of(workItemsMap.get(userCheeseOid)),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK. Cheese."),
                task, result);

        // THEN
        login(userAdministrator);
        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 3rd approval", workItems);
        assertEquals("Wrong # of work items on level 3", 1, workItems.size());
        workItemsMap = sortByOriginalAssignee(workItems);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItems.get(0));
        display("case after 3rd approval", aCase);

        assertStage(aCase, 3, 3, "Role approvers (all)", null);
        assertTriggers(aCase, 2);

        assertNotNull("chef is not an approver", workItemsMap.get(userChefOid));

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertEquals("Wrong # of work items lifecycle messages", 1, lifecycleMessages.size());
        assertEquals("Wrong # of work items allocation messages", 1, allocationMessages.size());
        assertNull("process messages", processMessages);

        assertMessage(lifecycleMessages.get(0), "cheese@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Ignatius Cheese (cheese)", "Carried out by: Ignatius Cheese (cheese)",
                "Result: APPROVED", "^Deadline:");
        assertMessage(allocationMessages.get(0), "cheese@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Ignatius Cheese (cheese)", "Carried out by: Ignatius Cheese (cheese)",
                "Result: APPROVED", "^Deadline:");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test108SimpleAssignmentApproveByChef() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN
        login(userAdministrator);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        Map<String, CaseWorkItemType> workItemsMap = sortByOriginalAssignee(workItems);

        // WHEN
        login(getUser(userChefOid));
        WorkItemId workItemId = WorkItemId.of(workItemsMap.get(userChefOid));
        workflowService.completeWorkItem(workItemId,
                ApprovalUtils.createApproveOutput(prismContext).comment("OK. Chef."),
                task, result);

        // THEN
        login(userAdministrator);
        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 4th approval", workItems);
        assertEquals("Wrong # of work items on level 3", 0, workItems.size());
        CaseType aCase = getCase(workItemId.caseOid);
        display("wfTask after 4th approval", aCase);

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        assertAssignedRole(getUser(userBobOid), roleATest1Oid);

        assertTriggers(aCase, 0);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertEquals("Wrong # of work items lifecycle messages", 1, lifecycleMessages.size());
        assertEquals("Wrong # of work items allocation messages", 1, allocationMessages.size());
        assertEquals("Wrong # of process messages", 1, processMessages.size());

        assertMessage(lifecycleMessages.get(0), "chef@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Scumm Bar Chef (chef)", "Carried out by: Scumm Bar Chef (chef)",
                "Result: APPROVED", "^Deadline:");
        assertMessage(allocationMessages.get(0), "chef@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Scumm Bar Chef (chef)", "Carried out by: Scumm Bar Chef (chef)",
                "Result: APPROVED", "^Deadline:");
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "Workflow process instance has finished",
                "Process instance name: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Result: APPROVED");

        displayDumpable("audit", dummyAuditService);

        // TODO after audit is OK
//        List<AuditEventRecord> workItemEvents = filter(getParamAuditRecords(
//                WorkflowConstants.AUDIT_WORK_ITEM_ID, workItemId.asString(), task, result), AuditEventStage.EXECUTION);
//        assertAuditReferenceValue(workItemEvents, WorkflowConstants.AUDIT_OBJECT, userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditTarget(workItemEvents.get(0), userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditReferenceValue(workItemEvents.get(0), WorkflowConstants.AUDIT_TARGET, roleATest1Oid, RoleType.COMPLEX_TYPE, "a-test-1");
        // TODO other items
//        List<AuditEventRecord> processEvents = filter(getParamAuditRecords(
//                WorkflowConstants.AUDIT_PROCESS_INSTANCE_ID, wfTask.asObjectable().getApprovalContext().getCaseOid(), task, result),
//                AuditEventType.WORKFLOW_PROCESS_INSTANCE, AuditEventStage.EXECUTION);
//        assertAuditReferenceValue(processEvents, WorkflowConstants.AUDIT_OBJECT, userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditTarget(processEvents.get(0), userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditReferenceValue(processEvents.get(0), WorkflowConstants.AUDIT_TARGET, roleATest1Oid, RoleType.COMPLEX_TYPE, "a-test-1");
        // TODO other items
    }

    //endregion

    //region Testing escalation
    @Test
    public void test200EscalatedApprovalStart() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        assignRole(userCarlaOid, roleATest1Oid, task, task.getResult());

        // THEN
        assertNotAssignedRole(getUser(userCarlaOid), roleATest1Oid);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItem);
        display("case", aCase);

        assertTriggers(aCase, 2);

        assertStage(aCase, 1, 3, "Line managers", null);
        assertAssignee(workItem, userGuybrushOid, userGuybrushOid);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertEquals("Wrong # of work items messages", 1, lifecycleMessages.size());
        assertMessage(lifecycleMessages.get(0), "guybrush@evolveum.com", "A new work item has been created", "Stage: Line managers (1/3)", "Allocated to: Guybrush Threepwood (guybrush)");
        assertMessage(allocationMessages.get(0), "guybrush@evolveum.com", "Work item has been allocated to you", "Stage: Line managers (1/3)", "Allocated to: Guybrush Threepwood (guybrush)");

        assertEquals("Wrong # of work items allocation messages", 1, allocationMessages.size());
        //assertMessage(lifecycleMessages.get(0), "guybrush@evolveum.com", "A new work item has been created", "Stage: Line managers (1/3)", "Guybrush Threepwood (guybrush)");

        assertEquals("Wrong # of process messages", 1, processMessages.size());
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "Workflow process instance has been started",
                "Process instance name: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test202FourDaysLater() throws Exception {
        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        clock.overrideDuration("P4D");
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertNull("lifecycle messages", lifecycleMessages);
        assertEquals("Wrong # of work items allocation messages", 1, allocationMessages.size());
        assertMessage(allocationMessages.get(0), "guybrush@evolveum.com", "Work item will be automatically escalated in 1 day",
                "Stage: Line managers (1/3)", "Allocated to (before escalation): Guybrush Threepwood (guybrush)");
        assertNull("process messages", processMessages);

        displayDumpable("audit", dummyAuditService);
    }

    // escalation should occur here
    @Test
    public void test204SixDaysLater() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        clock.resetOverride();
        clock.overrideDuration("P6D");
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work items after timed escalation", workItems);
        assertEquals("Wrong # of work items after timed escalation", 1, workItems.size());
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItems.get(0));
        display("aCase after timed escalation", aCase);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        // asserts - work item
        CaseWorkItemType workItem = workItems.get(0);
        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userGuybrushOid, userCheeseOid);
        PrismAsserts.assertDuration("Wrong duration between now and deadline", "P9D", System.currentTimeMillis(), workItem.getDeadline(), null);
        PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), userGuybrushOid);
        assertEquals("Wrong stage #", (Integer) 1, workItem.getStageNumber());
        assertEquals("Wrong escalation level #", 1, ApprovalContextUtil.getEscalationLevelNumber(workItem));
        assertEquals("Wrong escalation level name", "Line manager escalation", ApprovalContextUtil.getEscalationLevelName(workItem));

        List<CaseEventType> events = assertEvents(aCase, 2);
        assertEscalationEvent(events.get(1), userAdministrator.getOid(), userGuybrushOid, 1,
                Collections.singletonList(userGuybrushOid), Collections.singletonList(userCheeseOid), WorkItemDelegationMethodType.ADD_ASSIGNEES,
                1, "Line manager escalation");

        // asserts - notifications
        assertNull("lifecycle messages", lifecycleMessages);
        assertNull("process messages", processMessages);
        assertEquals("Wrong # of work items allocation messages", 3, allocationMessages.size());

        ArrayListValuedHashMap<String, Message> sorted = sortByRecipients(allocationMessages);
        assertMessage(sorted.get("guybrush@evolveum.com").get(0), "guybrush@evolveum.com", "Work item has been escalated",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Allocated to (before escalation): Guybrush Threepwood (guybrush)",
                "(in 5 days)");
        assertMessage(sorted.get("guybrush@evolveum.com").get(1), "guybrush@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "|Allocated to (after escalation): Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to (after escalation): Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "(in 9 days)");
        assertMessage(sorted.get("cheese@evolveum.com").get(0), "cheese@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "|Allocated to (after escalation): Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to (after escalation): Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "(in 9 days)");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test205EightDaysLater() throws Exception {
        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        clock.resetOverride();
        clock.overrideDuration("P8D");
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertNull("lifecycle messages", lifecycleMessages);
        assertNull("process messages", processMessages);
        assertEquals("Wrong # of work items allocation messages", 4, allocationMessages.size());
        ArrayListValuedHashMap<String, Message> sorted = sortByRecipients(allocationMessages);
        // FIXME The following assertions fail when daylight saving switch is approaching. We should fix it somehow, some day ...
        assertMessage(sorted.get("guybrush@evolveum.com").get(0), "guybrush@evolveum.com", "Work item will be automatically completed in 2 days 12 hours",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "(in 9 days)");
        assertMessage(sorted.get("guybrush@evolveum.com").get(1), "guybrush@evolveum.com", "Work item will be automatically completed in 2 days",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "(in 9 days)");
        assertMessage(sorted.get("cheese@evolveum.com").get(0), "cheese@evolveum.com", "Work item will be automatically completed in 2 days 12 hours",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "(in 9 days)");
        assertMessage(sorted.get("cheese@evolveum.com").get(1), "cheese@evolveum.com", "Work item will be automatically completed in 2 days",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "(in 9 days)");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test206ApproveByCheese() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        login(userAdministrator);
        clock.resetOverride();
        CaseWorkItemType workItem = getWorkItem(task, result);
        PrismObject<UserType> cheese = getUserFromRepo(userCheeseOid);
        login(cheese);

        // WHEN
        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK. Cheese."),
                task, result);

        // THEN
        login(userAdministrator);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        assertEquals("Wrong # of work items on level 2", 2, workItems.size());
        displayWorkItems("Work item after 1st approval", workItems);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItems.get(0));
        display("aCase after 1st approval", aCase);

        assertStage(aCase, 2, 3, "Security", null);
        assertTriggers(aCase, 4);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertEquals("Wrong # of work items lifecycle messages", 4, lifecycleMessages.size());
        assertEquals("Wrong # of work items allocation messages", 4, allocationMessages.size());
        assertNull("process messages", processMessages);

        Map<String,Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("guybrush@evolveum.com"), "guybrush@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: APPROVED", "^Deadline:");
        assertMessage(sorted.get("cheese@evolveum.com"), "cheese@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: APPROVED", "^Deadline:");
        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Elaine Marley (elaine)", "(in 7 days)", "^Result:");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        Map<String,Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("guybrush@evolveum.com"), "guybrush@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: APPROVED", "^Deadline:");
        assertMessage(sorted2.get("cheese@evolveum.com"), "cheese@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: APPROVED", "^Deadline:");
        assertMessage(sorted2.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Elaine Marley (elaine)", "(in 7 days)", "^Result:");
        assertMessage(sorted2.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        displayDumpable("audit", dummyAuditService);
    }

    // notification should be send
    @Test
    public void test208SixDaysLater() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        clock.resetOverride();
        reimportWithNoSchedule(TASK_TRIGGER_SCANNER_OID, TASK_TRIGGER_SCANNER_FILE, task, result);
        clock.overrideDuration("P6D");

        // WHEN
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertNull("lifecycle messages", lifecycleMessages);
        assertNull("process messages", processMessages);
        assertEquals("Wrong # of work items allocation messages", 2, allocationMessages.size());
        Map<String, Message> sorted = sortByRecipientsSingle(allocationMessages);

        // FIXME The following assertions fail when daylight saving switch is approaching. We should fix it somehow, some day ...
        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com",
                "Work item will be automatically completed in 2 days",
                "Security (2/3)", "Allocated to: Elaine Marley (elaine)", "(in 7 days)");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com",
                "Work item will be automatically completed in 2 days",
                "Security (2/3)", "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)");

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test209EightDaysLater() throws Exception {
        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        clock.resetOverride();
        clock.overrideDuration("P8D");

        // WHEN
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        assertEquals("Wrong # of work items lifecycle messages", 2, lifecycleMessages.size());
        assertEquals("Wrong # of work items allocation messages", 2, allocationMessages.size());
        assertEquals("Wrong # of process messages", 1, processMessages.size());
        checkTwoCompleted(lifecycleMessages);
        checkTwoCompleted(allocationMessages);
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "Workflow process instance has finished",
                "Process instance name: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Result: REJECTED");

        displayDumpable("audit", dummyAuditService);
    }

    private void checkTwoCompleted(List<Message> lifecycleMessages) {
        Map<String, Message> sorted = sortByRecipientsSingle(lifecycleMessages);

        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com",
                null,
                "Security (2/3)", "Allocated to: Elaine Marley (elaine)");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com",
                null,
                "Security (2/3)", "Allocated to: Horridly Scarred Barkeep (barkeeper)");
        assertMessage(lifecycleMessages.get(0), null, "Work item has been completed",
                "^Carried out by:",
                "Reason: Timed action",
                "Result: REJECTED");
        assertMessage(lifecycleMessages.get(1), null, "Work item has been completed",
                "^Carried out by:",
                "Reason: Timed action",
                "Result: REJECTED");
    }

    @Test
    public void test220FormRoleAssignmentStart() throws Exception {
        PrismObject<UserType> bob = getUserFromRepo(userBobOid);
        login(bob);

        Task task = getTestTask();
        task.setOwner(bob);
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        assignRole(userBobOid, roleATest4Oid, task, task.getResult());

        // THEN
        login(userAdministrator);
        assertNotAssignedRole(getUser(userBobOid), roleATest4Oid);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after start", workItems);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItems.get(0));
        display("aCase", aCase);

//        assertTargetTriggers(aCase, 2);

        ApprovalContextType actx = aCase.getApprovalContext();
        ApprovalSchemaType schema = actx.getApprovalSchema();
        assertEquals("Wrong # of approval levels", 2, schema.getStage().size());
        assertApprovalLevel(schema, 1, "Line managers", "P5D", 2);
        assertApprovalLevel(schema, 2, "Role approvers (first)", "P5D", 2);
        assertStage(aCase, 1, 2, "Line managers", null);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);

        displayDumpable("audit", dummyAuditService);
    }

    @Test
    public void test221FormApproveByLechuck() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        login(userAdministrator);
        CaseWorkItemType workItem = getWorkItem(task, result);

        // WHEN
        PrismObject<UserType> lechuck = getUserFromRepo(userLechuckOid);
        login(lechuck);

        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK. LeChuck"),
                task, result);

        // THEN
        login(userAdministrator);
        assertNotAssignedRole(getUser(userBobOid), roleATest4Oid);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 1st approval", workItems);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItems.get(0));
        assertStage(aCase, 2, 2, "Role approvers (first)", null);

        ApprovalStageDefinitionType level = ApprovalContextUtil.getCurrentStageDefinition(aCase);
        assertEquals("Wrong evaluation strategy", LevelEvaluationStrategyType.FIRST_DECIDES, level.getEvaluationStrategy());

        // notifications
        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);
        dummyTransport.clearMessages();

        displayDumpable("audit", dummyAuditService);
    }


    @Test
    public void test222FormApproveByCheese() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // GIVEN
        login(userAdministrator);
        SearchResultList<CaseWorkItemType> workItems = getWorkItems(task, result);
        CaseWorkItemType workItem = sortByOriginalAssignee(workItems).get(userCheeseOid);
        assertNotNull("No work item for cheese", workItem);

        // WHEN
        PrismObject<UserType> cheese = getUserFromRepo(userCheeseOid);
        login(cheese);
        ObjectDelta<UserType> formDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("Hello")
                .asObjectDelta(userBobOid);
        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext).comment("OK. LeChuck"),
                formDelta,
                task, result);

        // THEN
        login(userAdministrator);

        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 2nd approval", workItems);
        assertEquals("Wrong # of work items after 2nd approval", 0, workItems.size());

        CaseType aCase = getCase(CaseWorkItemUtil.getCaseRequired(workItem).getOid());
        display("aCase after 2nd approval", aCase);

        assertStage(aCase, 2, 2, "Role approvers (first)", null);
        // assertTargetTriggers(aCase, 4);

        // notifications
        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);

        // audit
        displayDumpable("audit", dummyAuditService);
        List<AuditEventRecord> records = dummyAuditService.getRecords();
        if (records.size() != 4 && records.size() != 5) {
            fail("Wrong # of audit records: " + records.size() + " (expected 4 or 5)");
        }
        AuditEventRecord record = records.get(0);
        Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = record.getDeltas();
        assertEquals("Wrong # of deltas in audit record", 1, deltas.size());
        ObjectDeltaOperation<? extends ObjectType> delta = deltas.iterator().next();
        assertEquals("Wrong # of modifications in audit record delta", 2, delta.getObjectDelta().getModifications().size());
        ItemDelta<?, ?> itemDelta = delta.getObjectDelta().getModifications().stream()
                .filter(d -> UserType.F_DESCRIPTION.equivalent(d.getPath()))
                .findFirst().orElse(null);
        assertNotNull("No user.description item delta found", itemDelta);
        assertEquals("Wrong value in delta", "Hello", itemDelta.getValuesToReplace().iterator().next().getRealValue());

        // record #1, #2: cancellation of work items of other approvers
        // record #3: finishing process execution
        // optional #4: asynchronous execution in task

        CaseType rootCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(rootCase, CASE_WAIT_TIMEOUT);
        assertAssignedRole(getUser(userBobOid), roleATest4Oid);
    }

    @Test
    public void test250ApproverAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        assignRole(userBobOid, roleATest1Oid, SchemaConstants.ORG_APPROVER, task, task.getResult());

        // THEN
        assertNull("bob has assigned role \"a-test-1\" as an approver", getUserAssignment(userBobOid, roleATest1Oid, SchemaConstants.ORG_APPROVER));

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after start", workItems);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItems.get(0));
        display("aCase", aCase);

        ApprovalSchemaType schema = aCase.getApprovalContext().getApprovalSchema();
        assertEquals("Wrong # of approval levels", 3, schema.getStage().size());
        assertApprovalLevel(schema, 1, "Line managers", "P5D", 2);
        assertApprovalLevel(schema, 2, "Security", "P7D", 1);
        assertApprovalLevel(schema, 3, "Role approvers (all)", "P5D", 2);
        assertStage(aCase, 1, 3, "Line managers", null);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);

        displayDumpable("audit", dummyAuditService);
    }


    //endregion

    private void assertMessage(Message message, String recipient, String subject, String... texts) {
        assertNotNull("No message for " + recipient, message);
        assertEquals("Wrong # of recipients", 1, message.getTo().size());
        if (recipient != null) {
            assertEquals("Wrong recipient", recipient, message.getTo().get(0));
        }
        if (subject != null) {
            assertEquals("Wrong subject", subject, message.getSubject());
        }
        condition: for (String text : texts) {
            if (text.startsWith("^")) {
                String pureText = text.substring(1);
                if (message.getBody().contains(pureText)) {
                    fail("Message body does contain '" + pureText + "' even if it shouldn't: " + message.getBody());
                }
            } else if (text.startsWith("|")) {
                String[] strings = StringUtils.split(text, "|");
                for (String string : strings) {
                    if (message.getBody().contains(string)) {
                        continue condition;
                    }
                }
                fail("Message body does not contain any of " + Arrays.asList(strings) + ": " + message.getBody());
            } else {
                if (!message.getBody().contains(text)) {
                    fail("Message body doesn't contain '" + text + "': " + message.getBody());
                }
            }
        }
    }

    private ArrayListValuedHashMap<String, Message> sortByRecipients(Collection<Message> messages) {
        ArrayListValuedHashMap<String, Message> rv = new ArrayListValuedHashMap<>();
        messages.forEach(m ->
                m.getTo().forEach(
                        to -> rv.put(to, m)));
        return rv;
    }

    private Map<String, Message> sortByRecipientsSingle(Collection<Message> messages) {
        Map<String, Message> rv = new HashMap<>();
        messages.forEach(m ->
                m.getTo().forEach(
                        to -> rv.put(to, m)));
        return rv;
    }

    private void assertTriggers(CaseType aCase, int count) {
        assertEquals("Wrong # of triggers", count, aCase.getTrigger().size());
    }

    private void assertAssignee(CaseWorkItemType workItem, String originalAssignee, String... currentAssignee) throws SchemaException {
        assertRefEquals("Wrong original assignee", ObjectTypeUtil.createObjectRef(originalAssignee, ObjectTypes.USER), workItem.getOriginalAssigneeRef());
        assertReferenceValues(ref(workItem.getAssigneeRef()), currentAssignee);
    }


    private void assertStage(CaseType aCase, Integer stageNumber, Integer stageCount, String stageName, String stageDisplayName) {
        ApprovalContextType wfc = aCase.getApprovalContext();
        assertEquals("Wrong stage number", stageNumber, aCase.getStageNumber());
        assertEquals("Wrong stage count", stageCount, ApprovalContextUtil.getStageCount(wfc));
        assertEquals("Wrong stage name", stageName, ApprovalContextUtil.getStageName(aCase));
        assertEquals("Wrong stage name", stageDisplayName, ApprovalContextUtil.getStageDisplayName(aCase));
    }

    private void assertApprovalLevel(ApprovalSchemaType schema, int number, String name, String duration, int timedActions) {
        ApprovalStageDefinitionType level = schema.getStage().get(number-1);
        assertEquals("Wrong level number", number, (int) level.getNumber());
        assertEquals("Wrong level name", name, level.getName());
        assertEquals("Wrong level duration", XmlTypeConverter.createDuration(duration), level.getDuration());
        assertEquals("Wrong # of timed actions", timedActions, level.getTimedActions().size());
    }

    private List<CaseEventType> assertEvents(CaseType aCase, int expectedCount) {
        assertEquals("Wrong # of wf events", expectedCount, aCase.getEvent().size());
        return aCase.getEvent();
    }

    private void assertEscalationEvent(CaseEventType caseEvent, String initiator, String originalAssignee,
            int stageNumber, List<String> assigneesBefore, List<String> delegatedTo,
            WorkItemDelegationMethodType methodType, int newEscalationLevelNumber, String newEscalationLevelName) throws SchemaException {
        assertThat(caseEvent).as("case event").isInstanceOf(WorkItemEscalationEventType.class);
        WorkItemEscalationEventType event = (WorkItemEscalationEventType) caseEvent;
        assertEvent(event, initiator, originalAssignee, stageNumber);
        PrismAsserts.assertReferenceValues(ref(event.getAssigneeBefore()), assigneesBefore.toArray(new String[0]));
        PrismAsserts.assertReferenceValues(ref(event.getDelegatedTo()), delegatedTo.toArray(new String[0]));
        assertEquals("Wrong delegation method", methodType, event.getDelegationMethod());
        assertEquals("Wrong escalation level #", (Integer) newEscalationLevelNumber, event.getNewEscalationLevel().getNumber());
        assertEquals("Wrong escalation level name", newEscalationLevelName, event.getNewEscalationLevel().getName());
    }

    private void assertCompletionEvent(CaseEventType caseEvent, String initiator, String originalAssignee,
            int stageNumber, WorkItemOutcomeType outcome, String comment) throws SchemaException {
        assertThat(caseEvent).as("case event").isInstanceOf(WorkItemCompletionEventType.class);
        WorkItemCompletionEventType event = (WorkItemCompletionEventType) caseEvent;
        assertEvent(event, initiator, originalAssignee, stageNumber);
        assertEquals("Wrong outcome", outcome, ApprovalUtils.fromUri(event.getOutput().getOutcome()));
        assertEquals("Wrong comment", comment, event.getOutput().getComment());
    }

    private void assertEvent(CaseEventType caseEvent,
            String initiator, String originalAssignee, Integer stageNumber) throws SchemaException {
        assertThat(caseEvent).as("case event").isInstanceOf(WorkItemEventType.class);
        WorkItemEventType event = (WorkItemEventType) caseEvent;
        PrismAsserts.assertReferenceValue(ref(event.getInitiatorRef()), initiator);
        assertEquals("Wrong stage #", stageNumber, event.getStageNumber());
        //assertEquals("Wrong stage name", stageName, event.getStageName());
        if (originalAssignee != null) {
            assertNotNull("Null original assignee", event.getOriginalAssigneeRef());
            PrismAsserts.assertReferenceValue(ref(event.getOriginalAssigneeRef()), originalAssignee);
        }
    }
}
