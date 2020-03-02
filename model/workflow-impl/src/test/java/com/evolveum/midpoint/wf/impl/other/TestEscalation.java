/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.other;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
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

    @Test
    public void test100CreateE1ApprovalCase() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assignRole(userJackOid, ROLE_E1_OID, task, result);                // should start approval process
        assertNotAssignedRole(userJackOid, ROLE_E1_OID, task, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        WorkItemId.of(workItem);

        approvalCaseOid = CaseWorkItemUtil.getCaseRequired(workItem).getOid();
        CaseType aCase = getCase(approvalCaseOid);

        display("work item", workItem);
        display("workflow task", aCase);

        // 5 days: notification
        // D-2 days: escalate (i.e. 12 days after creation)
        // D-0 days: approve (i.e. 14 days after creation)
        assertEquals("Wrong # of triggers", 3, aCase.getTrigger().size());

        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_BOB_OID);
        PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), USER_BOB_OID);
    }

    @Test
    public void test110NotifyAfter5Days() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        clock.overrideDuration("P5DT1H");            // at P5D there's a notify action
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // TODO assert notifications

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItem);
        display("case", aCase);
        assertEquals("Wrong # of triggers", 2, aCase.getTrigger().size());

        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_BOB_OID);
        PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), USER_BOB_OID);
    }

    @Test
    public void test120EscalateAfter12Days() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        clock.resetOverride();
        clock.overrideDuration("P12DT1H");        // at -P2D (i.e. P12D) there is a delegate action
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);
        CaseType aCase = CaseWorkItemUtil.getCaseRequired(workItem);
        display("case", aCase);
        assertEquals("Wrong # of triggers", 1, aCase.getTrigger().size());

        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_BOB_OID, USER_BOBEK_OID);
        PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), USER_BOB_OID);
        assertEquals("Wrong escalation level number", 1, ApprovalContextUtil.getEscalationLevelNumber(workItem));
    }

    @Test
    public void test130CompleteAfter14Days() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        clock.resetOverride();
        clock.overrideDuration("P14DT1H");        // at 0 (i.e. P14D) there is a delegate action
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        CaseType aCase = getCase(approvalCaseOid);
        display("task", aCase);
        assertEquals("Wrong # of triggers", 0, aCase.getTrigger().size());

        CaseType rootCase = getCase(aCase.getParentRef().getOid());
        display("rootTask", rootCase);
        waitForCaseClose(rootCase, 60000);

        assertAssignedRole(userJackOid, ROLE_E1_OID, result);
    }

    @Test
    public void test200CreateE2ApprovalCase() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        clock.resetOverride();
        resetTriggerTask(TASK_TRIGGER_SCANNER_OID, TASK_TRIGGER_SCANNER_FILE, result);

        // WHEN
        assignRole(userJackOid, ROLE_E2_OID, task, result);                // should start approval process

        // THEN
        assertNotAssignedRole(userJackOid, ROLE_E2_OID, task, result);

        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work items", workItems);

        approvalCaseOid = CaseWorkItemUtil.getCaseRequired(workItems.get(0)).getOid();
        CaseType aCase = getCase(approvalCaseOid);

        display("workflow case", aCase);

        // D-0 days (i.e. in P3D): escalate - twice: once for each approver
        // Note that at first escalation level there will be auto-rejection action at deadline (in P5D then)
        assertEquals("Wrong # of triggers", 2, aCase.getTrigger().size());
    }

    @Test
    public void test210EscalateAfter3Days() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN

        clock.overrideDuration("P3DT1H");        // at 3D there's a deadline with escalation
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN

        SearchResultList<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work items after deadline", workItems);

        CaseType aCase = getCase(approvalCaseOid);
        display("workflow case", aCase);

        // D-0 days (i.e. in P5D): reject (twice)
        assertEquals("Wrong # of triggers", 2, aCase.getTrigger().size());

        displayCollection("audit records", dummyAuditService.getRecords());
        display("dummy transport", dummyTransport);

        assertEquals("Wrong # of work items", 2, workItems.size());
        assertEquals("The work item deadlines differ after escalation", workItems.get(0).getDeadline(), workItems.get(1).getDeadline());
    }

    @Test
    public void test220Reject() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN

        clock.resetOverride();
        clock.overrideDuration("P5DT2H");        // at 5D there's a deadline with auto-rejection
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN

        SearchResultList<CaseWorkItemType> workItems = getWorkItems(task, result);
        displayWorkItems("Work items after deadline", workItems);
        assertEquals("Wrong # of work items", 0, workItems.size());

        CaseType aCase = getCase(approvalCaseOid);
        display("workflow case", aCase);
        assertEquals("Wrong # of triggers", 0, aCase.getTrigger().size());
        Map<String, WorkItemCompletionEventType> eventMap = new HashMap<>();
        for (CaseEventType event : aCase.getEvent()) {
            if (event instanceof WorkItemCompletionEventType) {
                WorkItemCompletionEventType c = (WorkItemCompletionEventType) event;
                eventMap.put(c.getExternalWorkItemId(), c);
                assertNotNull("No result in "+c, c.getOutput());
                assertEquals("Wrong outcome in "+c, WorkItemOutcomeType.REJECT, ApprovalUtils.fromUri(c.getOutput().getOutcome()));
                assertNotNull("No cause in "+c, c.getCause());
                assertEquals("Wrong cause type in "+c, WorkItemEventCauseTypeType.TIMED_ACTION, c.getCause().getType());
                assertEquals("Wrong cause name in "+c, "auto-reject", c.getCause().getName());
                assertEquals("Wrong cause display name in "+c, "Automatic rejection at deadline", c.getCause().getDisplayName());
            }
        }
        display("completion event map", eventMap);
        assertEquals("Wrong # of completion events", 2, eventMap.size());

        displayCollection("audit records", dummyAuditService.getRecords());
        List<AuditEventRecord> workItemAuditRecords = dummyAuditService.getRecordsOfType(AuditEventType.WORK_ITEM);
        assertEquals("Wrong # of work item audit records", 2, workItemAuditRecords.size());
        for (AuditEventRecord r : workItemAuditRecords) {
            assertEquals("Wrong causeType in "+r, Collections.singleton("timedAction"), r.getPropertyValues(WorkflowConstants.AUDIT_CAUSE_TYPE));
            assertEquals("Wrong causeName in "+r, Collections.singleton("auto-reject"), r.getPropertyValues(WorkflowConstants.AUDIT_CAUSE_NAME));
            assertEquals("Wrong causeDisplayName in "+r, Collections.singleton("Automatic rejection at deadline"), r.getPropertyValues(WorkflowConstants.AUDIT_CAUSE_DISPLAY_NAME));
            assertEquals("Wrong result in "+r, "Rejected", r.getResult());
        }
        displayCollection("notifications - process", dummyTransport.getMessages("dummy:simpleWorkflowNotifier-Processes"));
        List<Message> notifications = dummyTransport.getMessages("dummy:simpleWorkflowNotifier-WorkItems");
        displayCollection("notifications - work items", notifications);
        for (Message notification : notifications) {
            assertContains(notification, "Reason: Automatic rejection at deadline (timed action)");
            assertContains(notification, "Result: REJECTED");
        }
    }

    private void assertContains(Message notification, String text) {
        if (!notification.getBody().contains(text)) {
            fail("No '"+text+"' in "+notification);
        }
    }

}
