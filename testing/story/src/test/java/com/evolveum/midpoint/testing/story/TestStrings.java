/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertReferenceValues;

import java.io.File;
import java.util.*;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.CaseService;
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
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Various approval-related tests.
 *
 * Global + metadata-induced policy rules in nutshell:
 *
 * 1. Role assignment (subtype=test) must be approved by all line managers. (Order: 10) (global)
 * 2. Assignment of security-sensitive roles must be approved by group Security Approvers (Order: 20)
 * (in `metarole-approval-security`)
 * 3. SoD violations (indicated by policy situation) must be approved by group SoD Approvers (Order: 30) (global)
 * 4. Role assignment (subtype=test) must be approved by role approver. (Order: 40) (global)
 *
 * There's automated escalation followed by rejection for levels 10 and 40,
 * and automated rejection for levels 20 and 30 (no escalation). All of that is in global configuration.
 *
 * There are two special approval meta-roles:
 *
 * - `metarole-approval-role-approvers-first` - changes the approval strategy for level 40 to `firstDecides`
 * - `metarole-approval-role-approvers-form` - adds using a form to level 40
 *
 * == Testing scenarios
 *
 * 1. `test1xx` - straightforward approval of `a-test-1` assignment to `bob` by four approvers
 * 2. `test200-test219` - testing the escalation process
 * 3. `test220-test220` - testing approvals with a form
 * 4. `test250` - testing assignment with `org:approver` relation
 */
@SuppressWarnings({ "FieldCanBeLocal", "SameParameterValue" })
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestStrings extends AbstractStoryTest {

    @Autowired private CaseService caseService;

    private static final File TEST_DIR = new File("src/test/resources/strings");
    private static final File ORG_DIR = new File(TEST_DIR, "orgs");
    private static final File ROLES_DIR = new File(TEST_DIR, "roles");
    private static final File ROLES_SPECIFIC_DIR = new File(TEST_DIR, "roles-specific");
    private static final File USERS_DIR = new File(TEST_DIR, "users");

    private static final File ORG_MONKEY_ISLAND_FILE = new File(ORG_DIR, "0-org-monkey-island-modified.xml");
    private static final TestResource<OrgType> ORG_TEAMS =
            new TestResource<>(ORG_DIR, "1-teams.xml", "b7cbed1f-5e73-4455-b88a-b5b3ac2a0f54");
    private static final TestResource<OrgType> ORG_ROLE_CATALOG =
            new TestResource<>(ORG_DIR, "2-role-catalog.xml", "b77c512a-85b9-470e-a7ab-a55b8f187674");
    private static final TestResource<OrgType> ORG_SECURITY_APPROVERS =
            new TestResource<>(ORG_DIR, "security-approvers.xml", "a14afc10-e4a2-48a4-abfd-e8a2399f98d3");
    private static final TestResource<OrgType> ORG_SOD_APPROVERS =
            new TestResource<>(ORG_DIR, "sod-approvers.xml", "cb73377a-da8d-4fbe-b174-19879bae9032");

    private static final TestResource<RoleType> ROLE_END_USER =
            new TestResource<>(ROLES_DIR, "role-end-user.xml", "00000000-0000-0000-0000-000000000008");
    private static final TestResource<FormType> FORM_USER_DETAILS =
            new TestResource<>(ROLES_DIR, "form-user-details.xml", "6a1874a7-1e60-43b3-8d67-7f76484dead5");
    private static final TestResource<RoleType> METAROLE_APPROVAL_ROLE_APPROVERS_FIRST =
            new TestResource<>(ROLES_DIR, "metarole-approval-role-approvers-first.xml", "e3c28c94-798a-4f93-85f8-de7cbe37315b");
    private static final TestResource<RoleType> METAROLE_APPROVAL_ROLE_APPROVERS_FORM =
            new TestResource<>(ROLES_DIR, "metarole-approval-role-approvers-form.xml", "df092a19-68f0-4056-adf8-482f8fd26410");
    private static final TestResource<RoleType> METAROLE_APPROVAL_SECURITY =
            new TestResource<>(ROLES_DIR, "metarole-approval-security.xml", "9c0c224f-f279-44b5-b906-8e8418a651a2");

    private static final TestResource<RoleType> ROLE_A_TEST_1 =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-1.xml", "0daa49bc-6f5b-4746-8461-2e1a633070e3");
    private static final TestResource<RoleType> ROLE_A_TEST_2A =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-2a.xml", "fa636d6a-f016-491c-8cd5-cdcbfd516be5");
    private static final TestResource<RoleType> ROLE_A_TEST_2B =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-2b.xml", "ecb9287d-5852-4bec-9926-4ab1de518e26");
    private static final TestResource<RoleType> ROLE_A_TEST_3A =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-3a.xml", "3dc7e7de-17a8-488d-ba98-402971999138");
    private static final TestResource<RoleType> ROLE_A_TEST_3B =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-3b.xml", "0090e8a0-05a7-4181-b763-265cd2804ca7");
    private static final TestResource<RoleType> ROLE_A_TEST_3X =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-3x.xml", "d3767689-cf26-4e2f-8a1e-5cd139fc4401");
    private static final TestResource<RoleType> ROLE_A_TEST_3Y =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-3y.xml", "1e422483-997c-4808-bebd-0151d18a391e");
    private static final TestResource<RoleType> ROLE_A_TEST_4 =
            new TestResource<>(ROLES_SPECIFIC_DIR, "a-test-4.xml", "37e5a590-ab32-4e4f-8c73-64715a9b081d");

    private static final TestResource<UserType> USER_BARKEEPER =
            new TestResource<>(USERS_DIR, "barkeeper.xml", "b87eb285-b4ae-43c0-9e4c-7ba651de81fa");
    private static final TestResource<UserType> USER_BOB =
            new TestResource<>(USERS_DIR, "bob.xml", "469fd663-4492-4c24-8ce3-3907df7ac7ec");
    private static final TestResource<UserType> USER_CARLA =
            new TestResource<>(USERS_DIR, "carla.xml", "f9be8006-fd58-43f9-99ff-311935d9d3d3");
    private static final TestResource<UserType> USER_CHEESE =
            new TestResource<>(USERS_DIR, "cheese.xml", "b2a3f4ad-ad7b-4691-83d9-34d5ebb50a04");
    private static final TestResource<UserType> USER_CHEF =
            new TestResource<>(USERS_DIR, "chef.xml", "60dd9e6b-7403-4075-bcfa-d4566a552d41");
    private static final TestResource<UserType> USER_ELAINE =
            new TestResource<>(USERS_DIR, "elaine.xml", "771d00e6-792a-4296-8b4e-c4f59f712e0f");
    private static final TestResource<UserType> USER_GUYBRUSH =
            new TestResource<>(USERS_DIR, "guybrush.xml", "47f403ae-5a22-4226-aab9-cd321a62d6d3");
    private static final TestResource<UserType> USER_LECHUCK =
            new TestResource<>(USERS_DIR, "lechuck.xml", "058cf8d5-01ec-4818-87cc-6477b1a6505f");
    private static final TestResource<UserType> USER_LECHUCK_DEPUTY =
            new TestResource<>(USERS_DIR, "lechuck-deputy.xml", "0dde9c33-822f-4798-9fda-b3280edf6efa");
    private static final TestResource<UserType> USER_LECHUCK_DEPUTY_DEPUTY =
            new TestResource<>(USERS_DIR, "lechuck-deputy-deputy.xml", "120effb1-1122-40bd-847f-1d898148b7ce");
    private static final TestResource<UserType> USER_LECHUCK_DEPUTY_LIMITED =
            new TestResource<>(USERS_DIR, "lechuck-deputy-limited.xml", "7a194af0-792a-4e76-bb0b-a8367a68970a");

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
        reimportRecurringWithNoSchedule(TASK_TRIGGER_SCANNER_OID, TASK_TRIGGER_SCANNER_FILE, initTask, initResult);
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

        addAndRecompute(ROLE_A_TEST_1, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_2A, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_2B, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3A, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3B, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3X, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3Y, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_4, initTask, initResult);

        addAndRecompute(USER_BARKEEPER, initTask, initResult);
        addAndRecompute(USER_BOB, initTask, initResult);
        addAndRecompute(USER_CARLA, initTask, initResult);
        addAndRecompute(USER_CHEESE, initTask, initResult);
        addAndRecompute(USER_CHEF, initTask, initResult);
        addAndRecompute(USER_ELAINE, initTask, initResult);
        addAndRecompute(USER_GUYBRUSH, initTask, initResult);
        addAndRecompute(USER_LECHUCK, initTask, initResult);
        addAndRecompute(USER_LECHUCK_DEPUTY, initTask, initResult);
        addAndRecompute(USER_LECHUCK_DEPUTY_DEPUTY, initTask, initResult);
        addAndRecompute(USER_LECHUCK_DEPUTY_LIMITED, initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    //region Basic approval of a-test-1 to bob

    /**
     * Assigns role `a-test-1` to `bob`. Checks the case and notifications.
     *
     * The case is in stage 1 of 3 (line manager).
     */
    @Test
    public void test100SimpleAssignmentStart() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        assignRole(USER_BOB.oid, ROLE_A_TEST_1.oid, task, task.getResult());

        then();
        assertNotAssignedRole(getUser(USER_BOB.oid), ROLE_A_TEST_1.oid);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItem);
        display("wfTask", aCase);

        assertTriggers(aCase, 2);

        ApprovalContextType actx = aCase.getApprovalContext();
        ApprovalSchemaType schema = actx.getApprovalSchema();
        assertEquals("Wrong # of approval levels", 3, schema.getStage().size());
        assertApprovalLevel(schema, 1, "Line managers", "P5D", 2);
        assertApprovalLevel(schema, 2, "Security", "P7D", 1);
        assertApprovalLevel(schema, 3, "Role approvers (all)", "P5D", 2);
        assertStage(aCase, 1, 3, "Line managers", null);
        assertAssignee(workItem, USER_LECHUCK.oid, USER_LECHUCK.oid);

        displayDumpable("dummy transport", dummyTransport);

        List<Message> lifecycleMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_LIFECYCLE);
        List<Message> allocationMessages = dummyTransport.getMessages(DUMMY_WORK_ITEM_ALLOCATION);
        List<Message> processMessages = dummyTransport.getMessages(DUMMY_PROCESS);
        display("work items lifecycle notifications", lifecycleMessages);
        display("work items allocation notifications", allocationMessages);
        display("processes notifications", processMessages);

        assertEquals("Wrong # of work items lifecycle messages", 3, lifecycleMessages.size());
        Map<String, Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "A new work item has been created",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "A new work item has been created",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "A new work item has been created",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");

        assertEquals("Wrong # of work items allocation messages", 3, allocationMessages.size());
        Map<String, Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "Work item has been allocated to you",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted2.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "Work item has been allocated to you",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");
        assertMessage(sorted2.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "Work item has been allocated to you",
                "Stage: Line managers (1/3)", "Allocated to: Captain LeChuck (lechuck)", "(in 5 days)");

        assertEquals("Wrong # of process messages", 1, processMessages.size());
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "An approval case has been opened",
                "Case name: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"");

        displayDumpable("audit", dummyAuditService);
    }

    /**
     * User `lechuck` approves the assignment.
     *
     * The case is moved to stage 2 of 3 (security approval).
     */
    @Test
    public void test102SimpleAssignmentApproveByLechuck() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        login(userAdministrator);
        CaseWorkItemType workItem = getWorkItem(task, result);

        when();
        PrismObject<UserType> lechuck = getUserFromRepo(USER_LECHUCK.oid);
        login(lechuck);
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput().comment("OK. LeChuck"),
                task, result);

        then();
        login(userAdministrator);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 1st approval", workItems);
        assertEquals("Wrong # of work items on level 2", 2, workItems.size());
        CaseType aCase = getCase(CaseTypeUtil.getCaseRequired(workItem).getOid());
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

        Map<String, Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: Approved", "^Deadline:");
        assertMessage(sorted.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: Approved", "^Deadline:");
        assertMessage(sorted.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: Approved", "^Deadline:");
        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Elaine Marley (elaine)", "(in 7 days)", "^Result:");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        Map<String, Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("lechuck@evolveum.com"), "lechuck@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: Approved", "^Deadline:");
        assertMessage(sorted2.get("lechuck-deputy@evolveum.com"), "lechuck-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: Approved", "^Deadline:");
        assertMessage(sorted2.get("lechuck-deputy-deputy@evolveum.com"), "lechuck-deputy-deputy@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Line managers (1/3)",
                "Allocated to: Captain LeChuck (lechuck)", "Result: Approved", "^Deadline:");
        assertMessage(sorted2.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)", "Allocated to: Elaine Marley (elaine)",
                "(in 7 days)", "^Result:");
        assertMessage(sorted2.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        // events
        List<CaseEventType> events = assertEvents(aCase, 2);
        assertCompletionEvent(events.get(1), USER_LECHUCK.oid, USER_LECHUCK.oid, 1, WorkItemOutcomeType.APPROVE, "OK. LeChuck");

        displayDumpable("audit", dummyAuditService);
    }

    /**
     * User `administrator` approves the work item belonging to `elaine`.
     *
     * The case is moved to stage 3 of 3 (role approvers).
     */
    @Test
    public void test104SimpleAssignmentApproveByAdministrator() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        login(userAdministrator);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 1st approval", workItems);
        CaseWorkItemType elaineWorkItem = workItems.stream()
                .filter(wi -> USER_ELAINE.oid.equals(wi.getOriginalAssigneeRef().getOid()))
                .findFirst().orElseThrow(() -> new AssertionError("No work item for elaine"));

        when();
        // Second approval
        caseService.completeWorkItem(
                WorkItemId.of(elaineWorkItem),
                ApprovalUtils.createApproveOutput().comment("OK. Security."),
                task, result);

        then();
        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 2nd approval", workItems);
        assertEquals("Wrong # of work items on level 3", 2, workItems.size());
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItems.get(0));
        display("wfTask after 2nd approval", aCase);

        assertStage(aCase, 3, 3, "Role approvers (all)", null);
        assertTriggers(aCase, 4);

        Map<String, CaseWorkItemType> workItemsMap = sortByOriginalAssignee(workItems);
        assertNotNull("chef is not an approver", workItemsMap.get(USER_CHEF.oid));
        assertNotNull("cheese is not an approver", workItemsMap.get(USER_CHEESE.oid));

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

        Map<String, Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)", "Allocated to: Elaine Marley (elaine)",
                "Carried out by: midPoint Administrator (administrator)", "Result: Approved", "^Deadline:");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "Work item has been cancelled",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "^Result:", "^Deadline:", "^Carried out by:");
        assertMessage(sorted.get("cheese@evolveum.com"), "cheese@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Ignatius Cheese (cheese)", "^Result:", "(in 5 days)");
        assertMessage(sorted.get("chef@evolveum.com"), "chef@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Scumm Bar Chef (chef)", "^Result:", "(in 5 days)");

        Map<String, Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Stage: Security (2/3)", "Allocated to: Elaine Marley (elaine)",
                "Carried out by: midPoint Administrator (administrator)", "Result: Approved", "^Deadline:");
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

    /**
     * User `cheese` (role approver) approves the assignment.
     *
     * The case remains in stage 3, because there's one more approver (all must approve).
     */
    @Test
    public void test106SimpleAssignmentApproveByCheese() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        login(userAdministrator);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        Map<String, CaseWorkItemType> workItemsMap = sortByOriginalAssignee(workItems);

        when();
        login(getUser(USER_CHEESE.oid));
        caseService.completeWorkItem(
                WorkItemId.of(workItemsMap.get(USER_CHEESE.oid)),
                ApprovalUtils.createApproveOutput().comment("OK. Cheese."),
                task, result);

        then();
        login(userAdministrator);
        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 3rd approval", workItems);
        assertEquals("Wrong # of work items on level 3", 1, workItems.size());
        workItemsMap = sortByOriginalAssignee(workItems);
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItems.get(0));
        display("case after 3rd approval", aCase);

        assertStage(aCase, 3, 3, "Role approvers (all)", null);
        assertTriggers(aCase, 2);

        assertNotNull("chef is not an approver", workItemsMap.get(USER_CHEF.oid));

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
                "Result: Approved", "^Deadline:");
        assertMessage(allocationMessages.get(0), "cheese@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Ignatius Cheese (cheese)", "Carried out by: Ignatius Cheese (cheese)",
                "Result: Approved", "^Deadline:");

        displayDumpable("audit", dummyAuditService);
    }

    /**
     * User `chef` approves the assignment.
     *
     * The case will be closed, and the assignment created.
     */
    @Test
    public void test108SimpleAssignmentApproveByChef() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given();
        login(userAdministrator);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        Map<String, CaseWorkItemType> workItemsMap = sortByOriginalAssignee(workItems);

        when();
        login(getUser(USER_CHEF.oid));
        WorkItemId workItemId = WorkItemId.of(workItemsMap.get(USER_CHEF.oid));
        caseService.completeWorkItem(workItemId,
                ApprovalUtils.createApproveOutput().comment("OK. Chef."),
                task, result);

        then();
        login(userAdministrator);
        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 4th approval", workItems);
        assertEquals("Wrong # of work items on level 3", 0, workItems.size());
        CaseType aCase = getCase(workItemId.caseOid);
        display("case after 4th approval", aCase);

        CaseType parentCase = getCase(aCase.getParentRef().getOid());
        waitForCaseClose(parentCase);

        assertAssignedRole(getUser(USER_BOB.oid), ROLE_A_TEST_1.oid);

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
                "Result: Approved", "^Deadline:");
        assertMessage(allocationMessages.get(0), "chef@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Role approvers (all) (3/3)",
                "Allocated to: Scumm Bar Chef (chef)", "Carried out by: Scumm Bar Chef (chef)",
                "Result: Approved", "^Deadline:");
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "An approval case has been closed",
                "Case name: Assigning role \"a-test-1\" to user \"Ghost Pirate Bob (bob)\"", "Result: Approved");

        displayDumpable("audit", dummyAuditService);

        // TODO after audit is OK
//        List<AuditEventRecord> workItemEvents = filter(getParamAuditRecords(
//                AuditingConstants.AUDIT_WORK_ITEM_ID, workItemId.asString(), task, result), AuditEventStage.EXECUTION);
//        assertAuditReferenceValue(workItemEvents, AuditingConstants.AUDIT_OBJECT, userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditTarget(workItemEvents.get(0), userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditReferenceValue(workItemEvents.get(0), AuditingConstants.AUDIT_TARGET, roleATest1Oid, RoleType.COMPLEX_TYPE, "a-test-1");
        // TODO other items
//        List<AuditEventRecord> processEvents = filter(getParamAuditRecords(
//                AuditingConstants.AUDIT_PROCESS_INSTANCE_ID, wfTask.asObjectable().getApprovalContext().getCaseOid(), task, result),
//                AuditEventType.WORKFLOW_PROCESS_INSTANCE, AuditEventStage.EXECUTION);
//        assertAuditReferenceValue(processEvents, AuditingConstants.AUDIT_OBJECT, userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditTarget(processEvents.get(0), userBobOid, UserType.COMPLEX_TYPE, "bob");
//        assertAuditReferenceValue(processEvents.get(0), AuditingConstants.AUDIT_TARGET, roleATest1Oid, RoleType.COMPLEX_TYPE, "a-test-1");
        // TODO other items
    }

    //endregion

    //region Testing escalation

    /**
     * Role `a-test-1` is assigned to `carla`. The approval process is started.
     */
    @Test
    public void test200EscalatedApprovalStart() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        assignRole(USER_CARLA.oid, ROLE_A_TEST_1.oid, task, task.getResult());

        then();
        assertNotAssignedRole(getUser(USER_CARLA.oid), ROLE_A_TEST_1.oid);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("Work item", workItem);
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItem);
        display("case", aCase);

        assertTriggers(aCase, 2);

        assertStage(aCase, 1, 3, "Line managers", null);
        assertAssignee(workItem, USER_GUYBRUSH.oid, USER_GUYBRUSH.oid);

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
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "An approval case has been opened",
                "Case name: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"");

        displayDumpable("audit", dummyAuditService);
    }

    /**
     * Four days later, a notification about future escalation (that occurs 5 days after start) is sent.
     */
    @Test
    public void test202FourDaysLater() throws Exception {
        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        clock.overrideDuration("P4D");
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();
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

    /**
     * This is after 6 days (T+6D): On 5th day, the escalation takes place.
     *
     * Here we check that.
     *
     * There should be one work item, assigned to both `guybrush` (the original assignee), and `cheese` (the new one).
     */
    @Test
    public void test204SixDaysLater() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        clock.resetOverride();
        clock.overrideDuration("P6D");
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work items after timed escalation", workItems);
        assertEquals("Wrong # of work items after timed escalation", 1, workItems.size());
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItems.get(0));
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
        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_GUYBRUSH.oid, USER_CHEESE.oid);
        PrismAsserts.assertDuration("Wrong duration between now and deadline", "P9D", System.currentTimeMillis(), workItem.getDeadline(), null);
        PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), USER_GUYBRUSH.oid);
        assertEquals("Wrong stage #", (Integer) 1, workItem.getStageNumber());
        assertEquals("Wrong escalation level #", 1, WorkItemTypeUtil.getEscalationLevelNumber(workItem));
        assertEquals("Wrong escalation level name", "Line manager escalation", WorkItemTypeUtil.getEscalationLevelName(workItem));

        List<CaseEventType> events = assertEvents(aCase, 2);
        assertEscalationEvent(events.get(1), userAdministrator.getOid(), USER_GUYBRUSH.oid, 1,
                Collections.singletonList(USER_GUYBRUSH.oid), Collections.singletonList(USER_CHEESE.oid), WorkItemDelegationMethodType.ADD_ASSIGNEES,
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

    /**
     * Time T+8D. The stage (after escalation) ends at T+9D. This is because the deadline was derived
     * against work item creation time, not against the time when the escalation took place.
     *
     * So, 2 days, 12 hours before deadline (T+6.5D) a notification should be sent.
     * And 2 days before deadline (T+7D), another notification should be sent.
     *
     * So here we expect both of them.
     */
    @Test
    public void test205EightDaysLater() throws Exception {
        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        clock.resetOverride();
        clock.overrideDuration("P8D");
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();
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

    /**
     * The work item is approved by `cheese`, stepping into stage 2 (security).
     *
     * Assigned to `elaine` and `barkeeper`.
     */
    @Test
    public void test206ApproveByCheese() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        login(userAdministrator);
        clock.resetOverride();
        CaseWorkItemType workItem = getWorkItem(task, result);
        PrismObject<UserType> cheese = getUserFromRepo(USER_CHEESE.oid);
        login(cheese);

        when();
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput().comment("OK. Cheese."),
                task, result);

        then();
        login(userAdministrator);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        assertEquals("Wrong # of work items on level 2", 2, workItems.size());
        displayWorkItems("Work item after 1st approval", workItems);
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItems.get(0));
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

        Map<String, Message> sorted = sortByRecipientsSingle(lifecycleMessages);
        assertMessage(sorted.get("guybrush@evolveum.com"), "guybrush@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: Approved", "^Deadline:");
        assertMessage(sorted.get("cheese@evolveum.com"), "cheese@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: Approved", "^Deadline:");
        assertMessage(sorted.get("elaine@evolveum.com"), "elaine@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Elaine Marley (elaine)", "(in 7 days)", "^Result:");
        assertMessage(sorted.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "A new work item has been created",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        Map<String, Message> sorted2 = sortByRecipientsSingle(allocationMessages);
        assertMessage(sorted2.get("guybrush@evolveum.com"), "guybrush@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: Approved", "^Deadline:");
        assertMessage(sorted2.get("cheese@evolveum.com"), "cheese@evolveum.com", "Work item has been completed",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Line managers (1/3)",
                "Escalation level: Line manager escalation (1)",
                "Originally allocated to: Guybrush Threepwood (guybrush)",
                "|Allocated to: Guybrush Threepwood (guybrush), Ignatius Cheese (cheese)|Allocated to: Ignatius Cheese (cheese), Guybrush Threepwood (guybrush)",
                "Carried out by: Ignatius Cheese (cheese)",
                "Result: Approved", "^Deadline:");
        assertMessage(sorted2.get("elaine@evolveum.com"), "elaine@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Elaine Marley (elaine)", "(in 7 days)", "^Result:");
        assertMessage(sorted2.get("barkeeper@evolveum.com"), "barkeeper@evolveum.com", "Work item has been allocated to you",
                "Work item: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Stage: Security (2/3)",
                "Allocated to: Horridly Scarred Barkeep (barkeeper)", "(in 7 days)", "^Result:");

        displayDumpable("audit", dummyAuditService);
    }

    /**
     * The stage 2 duration is 7 days, with auto-rejection at T'+7D, notifying 2 days before deadline (T'+5D).
     *
     * We check in T'+6D, assuming some notifications being there.
     */
    @Test
    public void test208SixDaysLater() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        clock.resetOverride();
        reimportRecurringWithNoSchedule(TASK_TRIGGER_SCANNER_OID, TASK_TRIGGER_SCANNER_FILE, task, result);
        clock.overrideDuration("P6D");

        when();
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();
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

    /**
     * Checking in T'+8D the auto-completion has taken place (at T'+7D).
     */
    @Test
    public void test209EightDaysLater() throws Exception {
        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        clock.resetOverride();
        clock.overrideDuration("P8D");

        when();
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();
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
        assertMessage(processMessages.get(0), "administrator@evolveum.com", "An approval case has been closed",
                "Case name: Assigning role \"a-test-1\" to user \"Carla the Swordmaster (carla)\"", "Result: Rejected");

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
                "Result: Rejected");
        assertMessage(lifecycleMessages.get(1), null, "Work item has been completed",
                "^Carried out by:",
                "Reason: Timed action",
                "Result: Rejected");
    }
    //endregion

    //region Test form fulfillment

    /**
     * Role `a-test-4` has `metarole-approval-role-approvers-form`,
     * so it is being approved with gathering additional information.
     *
     * Here we assign the role, starting the approval case.
     */
    @Test
    public void test220FormRoleAssignmentStart() throws Exception {
        PrismObject<UserType> bob = getUserFromRepo(USER_BOB.oid);
        login(bob);

        Task task = getTestTask();
        task.setOwner(bob);
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        assignRole(USER_BOB.oid, ROLE_A_TEST_4.oid, task, task.getResult());

        then();
        login(userAdministrator);
        assertNotAssignedRole(getUser(USER_BOB.oid), ROLE_A_TEST_4.oid);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after start", workItems);
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItems.get(0));
        display("aCase", aCase);

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

    /**
     * LeChuck approves, without providing the form. We move to stage 2 of 2 (Role approvers).
     */
    @Test
    public void test221FormApproveByLechuck() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        login(userAdministrator);
        CaseWorkItemType workItem = getWorkItem(task, result);

        when();
        PrismObject<UserType> lechuck = getUserFromRepo(USER_LECHUCK.oid);
        login(lechuck);

        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput().comment("OK. LeChuck"),
                task, result);

        then();
        login(userAdministrator);
        assertNotAssignedRole(getUser(USER_BOB.oid), ROLE_A_TEST_4.oid);
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 1st approval", workItems);
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItems.get(0));
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

    /**
     * Cheese approves, filling-in a form.
     */
    @Test
    public void test222FormApproveByCheese() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        given();
        login(userAdministrator);
        SearchResultList<CaseWorkItemType> workItems = getWorkItems(task, result);
        CaseWorkItemType workItem = sortByOriginalAssignee(workItems).get(USER_CHEESE.oid);
        assertNotNull("No work item for cheese", workItem);

        when();
        PrismObject<UserType> cheese = getUserFromRepo(USER_CHEESE.oid);
        login(cheese);
        ObjectDelta<UserType> formDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("Hello")
                .asObjectDelta(USER_BOB.oid);
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput().comment("OK. LeChuck"),
                formDelta,
                task, result);

        then();
        login(userAdministrator);

        workItems = getWorkItems(task, result);
        displayWorkItems("Work item after 2nd approval", workItems);
        assertEquals("Wrong # of work items after 2nd approval", 0, workItems.size());

        CaseType aCase = getCase(CaseTypeUtil.getCaseRequired(workItem).getOid());
        display("aCase after 2nd approval", aCase);

        assertStage(aCase, 2, 2, "Role approvers (first)", null);

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
        assertAssignedRole(getUser(USER_BOB.oid), ROLE_A_TEST_4.oid);
    }
    //endregion

    //region Other
    @Test
    public void test250ApproverAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        assignRole(USER_BOB.oid, ROLE_A_TEST_1.oid, SchemaConstants.ORG_APPROVER, task, task.getResult());

        then();
        assertNull("bob has assigned role \"a-test-1\" as an approver", getUserAssignment(USER_BOB.oid, ROLE_A_TEST_1.oid, SchemaConstants.ORG_APPROVER));

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work item after start", workItems);
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItems.get(0));
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
        condition:
        for (String text : texts) {
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
        ApprovalStageDefinitionType level = schema.getStage().get(number - 1);
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
