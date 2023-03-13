/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType.ADD_ASSIGNEES;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType.REPLACE_ASSIGNEES;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDelegation extends AbstractWfTestPolicy {

    private static final File TEST_RESOURCE_DIR = new File("src/test/resources/delegation");

    private static final File ROLE_PRINCE_FILE = new File(TEST_RESOURCE_DIR, "role-prince.xml");
    private static final String ROLE_PRINCE_OID = "9252328b-b0b4-458f-9397-31b67291d566";

    private static final File USER_LONGSHANKS_FILE = new File(TEST_RESOURCE_DIR, "user-longshanks.xml");
    private static final String USER_LONGSHANKS_OID = "ad6f2526-4b59-4e57-9bb4-4584d34babc0";

    private static final File USER_GIRTH_FILE = new File(TEST_RESOURCE_DIR, "user-girth.xml");
    private static final String USER_GIRTH_OID = "11d242f4-a49f-4168-9ebc-60c4825b43c7";

    private static final File USER_KEEN_FILE = new File(TEST_RESOURCE_DIR, "user-keen.xml");
    private static final String USER_KEEN_OID = "96cdc455-b433-405a-8877-140306cd570b";

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    private WorkItemId workItemId;
    private String caseOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_PRINCE_FILE, initResult);
        addAndRecompute(USER_LONGSHANKS_FILE, initTask, initResult);
        addAndRecompute(USER_GIRTH_FILE, initTask, initResult);
        addAndRecompute(USER_KEEN_FILE, initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Test
    public void test100CreateTask() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assignRole(USER_JACK.oid, ROLE_PRINCE_OID, task, result);                // should start approval process
        assertNotAssignedRole(USER_JACK.oid, ROLE_PRINCE_OID, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        workItemId = WorkItemId.of(workItem);
        caseOid = CaseTypeUtil.getCaseRequired(workItem).getOid();

        display("work item", workItem);
        display("case", getObjectViaRepo(CaseType.class, caseOid));

        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_LONGSHANKS_OID);
    }

    @Test
    public void test110DelegateToGirthUnauthorized() throws Exception {
        login(getUserFromRepo(USER_KEEN_OID));

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        try {
            WorkItemDelegationRequestType request = new WorkItemDelegationRequestType()
                    .delegate(ort(USER_GIRTH_OID))
                    .method(ADD_ASSIGNEES);
            caseService.delegateWorkItem(workItemId, request, task, result);
            fail("delegate succeeded even if it shouldn't");
        } catch (SecurityViolationException e) {
            // ok
        }

        CaseWorkItemType workItem = getWorkItem(task, result);
        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_LONGSHANKS_OID);
    }

    @Test
    public void test120DelegateToGirth() throws Exception {
        login(getUserFromRepo(USER_LONGSHANKS_OID));

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        WorkItemDelegationRequestType request = new WorkItemDelegationRequestType()
                .delegate(ort(USER_GIRTH_OID))
                .method(ADD_ASSIGNEES)
                .comment("check this");
        caseService.delegateWorkItem(workItemId, request, task, result);

        result.computeStatus();
        assertSuccess(result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);

        PrismObject<CaseType> aCase = getObjectViaRepo(CaseType.class, caseOid);
        display("task", aCase);

        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_LONGSHANKS_OID, USER_GIRTH_OID);
        assertRefEquals("Wrong originalAssigneeRef", ort(USER_LONGSHANKS_OID), workItem.getOriginalAssigneeRef());

        List<WorkItemDelegationEventType> events = ApprovalContextUtil.getWorkItemEvents(aCase.asObjectable(), workItemId.id, WorkItemDelegationEventType.class);
        assertEquals("Wrong # of delegation events", 1, events.size());
        WorkItemDelegationEventType event = events.get(0);
        assertEquals("Wrong comment", "check this", event.getComment());
        // TODO check content
    }

    @Test
    public void test130DelegateToKeenByReplace() throws Exception {
        login(getUserFromRepo(USER_LONGSHANKS_OID));

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        WorkItemDelegationRequestType request = new WorkItemDelegationRequestType()
                .delegate(ort(USER_KEEN_OID))
                .method(REPLACE_ASSIGNEES);
        caseService.delegateWorkItem(workItemId, request, task, result);

        result.computeStatus();
        assertSuccess(result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);

        PrismObject<CaseType> aCase = getObjectViaRepo(CaseType.class, caseOid);
        display("task", aCase);

        PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), USER_KEEN_OID);
        assertRefEquals("Wrong originalAssigneeRef", ort(USER_LONGSHANKS_OID), workItem.getOriginalAssigneeRef());

        List<WorkItemDelegationEventType> events = ApprovalContextUtil.getWorkItemEvents(aCase.asObjectable(), workItemId.id, WorkItemDelegationEventType.class);
        assertEquals("Wrong # of delegation events", 2, events.size());
        // TODO check content
    }

    @Test
    public void test140DelegateToNoneByReplace() throws Exception {
        login(getUserFromRepo(USER_KEEN_OID));

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        WorkItemDelegationRequestType request = new WorkItemDelegationRequestType()
                .method(REPLACE_ASSIGNEES);
        caseService.delegateWorkItem(workItemId, request, task, result);

        result.computeStatus();
        assertSuccess(result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);

        PrismObject<CaseType> aCase = getObjectViaRepo(CaseType.class, caseOid);
        display("task", aCase);

        assertEquals("Wrong assigneeRef count", 0, workItem.getAssigneeRef().size());
        assertRefEquals("Wrong originalAssigneeRef", ort(USER_LONGSHANKS_OID), workItem.getOriginalAssigneeRef());

        List<WorkItemDelegationEventType> events = ApprovalContextUtil.getWorkItemEvents(aCase.asObjectable(), workItemId.id, WorkItemDelegationEventType.class);
        assertEquals("Wrong # of delegation events", 3, events.size());
        // TODO check content
    }
}
