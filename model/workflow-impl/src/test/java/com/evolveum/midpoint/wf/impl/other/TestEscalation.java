/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.cases.api.AuditingConstants;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the work item escalation mechanism.
 *
 * *For tests 1xx*, we create a case by assigning *E1* role. The approval process has a single stage and
 * it looks like this (see `metarole-escalated`):
 *
 * 1. `bob` is the approver and he has 14 days to complete his work item,
 * 2. 5 days after creation, a notification is sent,
 * 3. 2 days before deadline (i.e. 12 days after creation), an escalation to `bobek` takes place (escalation level 1),
 * 4. On the deadline, the case is automatically approved.
 *
 * *For tests 2xx*, we assign *E2* role. The process looks like this:
 *
 * 1. There are two approvers: `bob` and `bobek`, and a deadline of 3 days. (So, 2 work items will be created.)
 * 2. After 3 days, the escalation level 1 is entered, with a single assignee added to each of the two work items.
 * New deadline is 5 days.
 * 3. After those 5 days, automated rejection takes place.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestEscalation extends AbstractWfTestPolicy {

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    private static final File TASK_TRIGGER_SCANNER_FILE = new File(COMMON_DIR, "task-trigger-scanner.xml");
    protected static final String TASK_TRIGGER_SCANNER_OID = "00000000-0000-0000-0000-000000000007";

    private static final File TEST_ESCALATION_RESOURCE_DIR = new File("src/test/resources/escalation");
    private static final File METAROLE_ESCALATED_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "metarole-escalated.xml");

    private static final File ROLE_E1_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "role-e1.xml");
    private static final String ROLE_E1_OID = "7b959192-8c0c-4499-a51e-266b3b4dd8c4";

    private static final File ROLE_E2_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "role-e2.xml");
    private static final String ROLE_E2_OID = "bb38a7fc-8610-49b0-a76d-7b01cb8a6de1";

    private static final File USER_BOB_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "user-bob.xml");
    private static final String USER_BOB_OID = "6f007608-415b-49e3-b388-0217d535fc7d";

    private static final File USER_BOBEK_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "user-bobek.xml");
    private static final String USER_BOBEK_OID = "53b49582-de4b-4306-a135-41f46e64cbcc";

    private String approvalCaseOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        repoAddObjectFromFile(METAROLE_ESCALATED_FILE, initResult).getOid();
        repoAddObjectFromFile(ROLE_E1_FILE, initResult);
        repoAddObjectFromFile(ROLE_E2_FILE, initResult);

        addAndRecompute(USER_BOB_FILE, initTask, initResult);
        addAndRecompute(USER_BOBEK_FILE, initTask, initResult);

        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);
    }

    /**
     * Let us create the case.
     */
    @Test
    public void test100CreateE1ApprovalCase() throws Exception {
        given();
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        assignRole(USER_JACK.oid, ROLE_E1_OID, task, result); // should create approval case

        then();
        assertNotAssignedRole(USER_JACK.oid, ROLE_E1_OID, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        approvalCaseOid = CaseTypeUtil.getCaseRequired(workItem).getOid();
        CaseType aCase = getCase(approvalCaseOid);

        displayAllNotifications();

        // @formatter:off
        assertCase(aCase, "after")
                .display()
                .displayXml()
                .assertOpen()
                .assertStageNumber(1)
                .assertApprovalCaseArchetype()
                .workItems()
                    .single()
                        .assertNotClosed()
                        .assertEscalationLevelNumber(0)
                        .assertAssignees(USER_BOB_OID)
                        .assertOriginalAssigneeRef(USER_BOB_OID, UserType.COMPLEX_TYPE)
                    .end()
                .end()
                .events()
                    .assertEvents(1) // case open
                .end()
                .triggers()
                    // 5 days: notification
                    // D-2 days: escalate (i.e. 12 days after creation)
                    // D-0 days: approve (i.e. 14 days after creation)
                    .assertTriggers(3)
                .end();
        // @formatter:on
    }

    /**
     * We check that after 5 days we get a notification.
     */
    @Test
    public void test110CheckNotificationAfter5Days() throws Exception {
        given();
        login(userAdministrator);

        dummyTransport.clearMessages();
        clock.overrideDuration("P5DT1H"); // at P5D there's a notify action

        when();
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();

        displayDumpable("dummy transport", dummyTransport);
        assertHasDummyTransportMessageContaining("simpleWorkflowNotifier-WorkItems", "");

        // @formatter:off
        assertCase(approvalCaseOid, "after")
                .display()
                .assertOpen()
                .assertStageNumber(1)
                .workItems()
                    .single()
                        .assertNotClosed()
                        .assertEscalationLevelNumber(0)
                        .assertAssignees(USER_BOB_OID)
                        .assertOriginalAssigneeRef(USER_BOB_OID, UserType.COMPLEX_TYPE)
                    .end()
                .end()
                .triggers()
                    .assertTriggers(2)
                .end();
        // @formatter:on
    }

    /**
     * After 12 days (from creation) there should be an escalation.
     */
    @Test
    public void test120CheckEscalationAfter12Days() throws Exception {
        given();
        login(userAdministrator);

        dummyTransport.clearMessages();
        clock.resetOverride();
        clock.overrideDuration("P12DT1H"); // at -P2D (i.e. P12D) there is a delegate action

        when();
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();

        displayDumpable("dummy transport", dummyTransport);
        displayAllNotifications();

        // @formatter:off
        assertCase(approvalCaseOid, "after")
                .display()
                .assertOpen()
                .assertStageNumber(1)
                .workItems()
                    .single()
                        .assertNotClosed()
                        .assertEscalationLevelNumber(1)
                        .assertAssignees(USER_BOB_OID, USER_BOBEK_OID)
                        .assertOriginalAssigneeRef(USER_BOB_OID, UserType.COMPLEX_TYPE)
                    .end()
                .end()
                .triggers()
                    .assertTriggers(1)
                .end();
        // @formatter:on
    }

    /**
     * After 14 days (from creation) there should be an automatic approval.
     */
    @Test
    public void test130CheckCompletionAfter14Days() throws Exception {
        given();
        login(userAdministrator);

        OperationResult result = getTestOperationResult();

        dummyTransport.clearMessages();
        clock.resetOverride();
        clock.overrideDuration("P14DT1H"); // at 0 (i.e. P14D) there is a completion action

        when();
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();

        displayAllNotifications();

        // @formatter:off
        var approvalCase = assertCase(approvalCaseOid, "after")
                .display()
                .assertClosingOrClosed()
                .assertStageNumber(1)
                .workItems()
                    .single()
                        .assertClosed()
                        .assertEscalationLevelNumber(1)
                        .assertOriginalAssigneeRef(USER_BOB_OID, UserType.COMPLEX_TYPE)
                    .end()
                .end()
                .triggers()
                    .assertTriggers(0)
                .end()
                .getObjectable();

        var rootCase = assertCase(approvalCase.getParentRef().getOid(), "root")
                .display()
                .getObjectable();
        // @formatter:on

        waitForCaseClose(rootCase, 60000);

        assertAssignedRole(USER_JACK.oid, ROLE_E1_OID, result);
    }

    /**
     * We create the case for tests 2xx. (Assignment of E2.)
     */
    @Test
    public void test200CreateE2ApprovalCase() throws Exception {
        given();
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyTransport.clearMessages();
        clock.resetOverride();
        reimportRecurringWithNoSchedule(TASK_TRIGGER_SCANNER_OID, TASK_TRIGGER_SCANNER_FILE, task, result);

        when();
        assignRole(USER_JACK.oid, ROLE_E2_OID, task, result); // should start approval process

        then();
        assertNotAssignedRole(USER_JACK.oid, ROLE_E2_OID, result);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work items", workItems);

        approvalCaseOid = CaseTypeUtil.getCaseRequired(workItems.get(0)).getOid();

        displayAllNotifications();

        // @formatter:off
        assertCase(approvalCaseOid, "after")
                .display()
                .displayXml()
                .assertOpen()
                .assertStageNumber(1)
                .assertApprovalCaseArchetype()
                .workItems()
                    .forOriginalAssignee(USER_BOB_OID)
                        .assertNotClosed()
                        .assertEscalationLevelNumber(0)
                    .end()
                    .forOriginalAssignee(USER_BOBEK_OID)
                        .assertNotClosed()
                        .assertEscalationLevelNumber(0)
                    .end()
                .end()
                .triggers()
                    // D-0 days (i.e. in P3D): escalate - twice: once for each approver
                    // Note that at first escalation level there will be auto-rejection action at deadline (in P5D then)
                    .assertTriggers(2)
                .end();
        // @formatter:on
    }

    /**
     * After 3 days, the work items should be escalated.
     */
    @Test
    public void test210CheckEscalationAfter3Days() throws Exception {
        given();
        login(userAdministrator);

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        clock.resetOverride();
        clock.overrideDuration("P3DT1H"); // at 3D there's a deadline with escalation

        when();
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();

        displayCollection("audit records", dummyAuditService.getRecords());
        displayDumpable("dummy transport", dummyTransport);

        // @formatter:off
        var workItems = assertCase(approvalCaseOid, "after")
                .display()
                .assertOpen()
                .assertStageNumber(1)
                .assertApprovalCaseArchetype()
                .triggers()
                    // D-0 days (i.e. in P5D): reject (twice)
                    .assertTriggers(2)
                .end()
                .workItems()
                    .forOriginalAssignee(USER_BOB_OID)
                        .assertNotClosed()
                        .assertEscalationLevelNumber(1)
                    .end()
                    .forOriginalAssignee(USER_BOBEK_OID)
                        .assertNotClosed()
                        .assertEscalationLevelNumber(1)
                    .end()
                .getWorkItems();
        // @formatter:on

        var iterator = workItems.iterator();
        CaseWorkItemType first = iterator.next();
        CaseWorkItemType second = iterator.next();

        assertEquals("The work item deadlines differ after escalation", first.getDeadline(), second.getDeadline());
    }

    /**
     * After 5 days, the case should be auto-rejected.
     */
    @Test
    public void test220Reject() throws Exception {
        given();
        login(userAdministrator);

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        clock.resetOverride();
        clock.overrideDuration("P5DT2H");        // at 5D there's a deadline with auto-rejection

        when();
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        then();

        // @formatter:off
        var approvalCase = assertCase(approvalCaseOid, "after")
                .display()
                .displayXml()
                .assertClosed()
                .assertStageNumber(1)
                .triggers()
                    .assertTriggers(0)
                .end()
                .workItems()
                    .forOriginalAssignee(USER_BOB_OID)
                        .assertClosed()
                        .assertEscalationLevelNumber(1)
                    .end()
                    .forOriginalAssignee(USER_BOBEK_OID)
                        .assertClosed()
                        .assertEscalationLevelNumber(1)
                    .end()
                .end()
                .getObjectable();
        // @formatter:on

        Map<String, WorkItemCompletionEventType> eventMap = new HashMap<>();
        for (CaseEventType event : approvalCase.getEvent()) {
            if (event instanceof WorkItemCompletionEventType) {
                WorkItemCompletionEventType c = (WorkItemCompletionEventType) event;
                eventMap.put(c.getExternalWorkItemId(), c);
                assertNotNull("No result in " + c, c.getOutput());
                assertEquals("Wrong outcome in " + c, WorkItemOutcomeType.REJECT, ApprovalUtils.fromUri(c.getOutput().getOutcome()));
                assertNotNull("No cause in " + c, c.getCause());
                assertEquals("Wrong cause type in " + c, WorkItemEventCauseTypeType.TIMED_ACTION, c.getCause().getType());
                assertEquals("Wrong cause name in " + c, "auto-reject", c.getCause().getName());
                assertEquals("Wrong cause display name in " + c, "Automatic rejection at deadline", c.getCause().getDisplayName());
            }
        }
        displayValue("completion event map", eventMap);
        assertEquals("Wrong # of work item completion events in case history", 2, eventMap.size());

        displayCollection("audit records", dummyAuditService.getRecords());
        List<AuditEventRecord> workItemAuditRecords = dummyAuditService.getRecordsOfType(AuditEventType.WORK_ITEM);
        assertEquals("Wrong # of work item audit records", 2, workItemAuditRecords.size());
        for (AuditEventRecord r : workItemAuditRecords) {
            assertEquals("Wrong causeType in " + r, Collections.singleton("timedAction"), r.getPropertyValues(AuditingConstants.AUDIT_CAUSE_TYPE));
            assertEquals("Wrong causeName in " + r, Collections.singleton("auto-reject"), r.getPropertyValues(AuditingConstants.AUDIT_CAUSE_NAME));
            assertEquals("Wrong causeDisplayName in " + r, Collections.singleton("Automatic rejection at deadline"), r.getPropertyValues(AuditingConstants.AUDIT_CAUSE_DISPLAY_NAME));
            assertEquals("Wrong result in " + r, "Rejected", r.getResult());
        }
        displayCollection("notifications - cases", dummyTransport.getMessages(DUMMY_SIMPLE_WORKFLOW_NOTIFIER_PROCESSES));
        List<Message> notifications = dummyTransport.getMessages(DUMMY_SIMPLE_WORKFLOW_NOTIFIER_WORK_ITEMS);
        displayCollection("notifications - work items", notifications);
        for (Message notification : notifications) {
            assertContains(notification, "Reason: Automatic rejection at deadline (timed action)");
            assertContains(notification, "Result: Rejected");
        }
    }

    private void assertContains(Message notification, String text) {
        if (!notification.getBody().contains(text)) {
            fail("No '" + text + "' in " + notification);
        }
    }
}
