/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType.F_DELTAS_TO_APPROVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.F_ORIGINAL_ASSIGNEE_REF;

import java.io.File;
import java.util.*;

import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.impl.WorkItemManager;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;
import com.evolveum.midpoint.model.api.CaseService;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.wf.api.ApprovalsManager;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractWfTestPolicy extends AbstractWfTest {

    private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");

    @Autowired protected Clockwork clockwork;
    @Autowired protected ClockworkMedic clockworkMedic;
    @Autowired protected TaskManager taskManager;
    @Autowired protected CaseManager caseManager;
    @Autowired protected ApprovalsManager approvalsManager;
    @Autowired protected CaseService caseService;
    @Autowired protected CaseEngineImpl caseEngine;
    @Autowired protected WorkItemManager workItemManager;
    @Autowired protected PrimaryChangeProcessor primaryChangeProcessor;
    @Autowired protected SystemObjectCache systemObjectCache;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected WfTestHelper testHelper;
    @Autowired protected MiscHelper miscHelper;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    protected abstract static class TestDetails {
        protected LensContext createModelContext(OperationResult result) throws Exception {
            return null;
        }

        protected void afterFirstClockworkRun(CaseType rootCase, CaseType case0, List<CaseType> subcases,
                List<CaseWorkItemType> workItems, Task opTask, OperationResult result) throws Exception {
        }

        protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) throws Exception {
        }

        protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases, Task opTask,
                OperationResult result) throws Exception {
        }

        protected boolean executeImmediately() {
            return false;
        }

        protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
            return null;
        }

        public boolean strictlySequentialApprovals() {
            return false;
        }

        public List<ApprovalInstruction> getApprovalSequence() {
            return null;
        }

        public void setTracing(Task opTask) {
        }
    }

    protected <F extends FocusType> OperationResult executeTest(TestDetails testDetails, int expectedSubTaskCount)
            throws Exception {

        // GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        Task opTask = getTestTask();

        boolean USE_FULL_TRACING = false;
        //noinspection ConstantConditions
        if (USE_FULL_TRACING) {
            setModelAndWorkflowLoggingTracing(opTask);
        } else {
            testDetails.setTracing(opTask);
        }

        opTask.setOwner(userAdministrator);
        OperationResult result = opTask.getResult();

        LensContext<F> modelContext = testDetails.createModelContext(result);
        displayDumpable("Model context at test start", modelContext);

        // this has problems with deleting assignments by ID
        //assertFocusModificationSanity(modelContext);

        // WHEN

        HookOperationMode mode;
        clockworkMedic.enterModelMethod(true);
        try {
            mode = clockwork.run(modelContext, opTask, result);
        } finally {
            clockworkMedic.exitModelMethod(true);
        }

        // THEN

        displayDumpable("Model context after first clockwork.run", modelContext);
        assertEquals("Unexpected state of the context", ModelState.PRIMARY, modelContext.getState());
        assertEquals("Wrong mode after clockwork.run in " + modelContext.getState(), HookOperationMode.BACKGROUND, mode);
        opTask.refresh(result);
        display("Model task after first clockwork.run", opTask);

        CaseType rootCase = testHelper.getRootCase(result);
        List<CaseType> subcases = miscHelper.getSubcases(rootCase, result);
        CaseType case0 = WfTestHelper.findAndRemoveCase0(subcases);

        assertEquals("Incorrect number of sub-cases", expectedSubTaskCount, subcases.size());

        final Collection<SelectorOptions<GetOperationOptions>> options1 = schemaService.getOperationOptionsBuilder()
                .item(T_PARENT, F_OBJECT_REF).resolve()
                .item(T_PARENT, F_TARGET_REF).resolve()
                .item(F_ASSIGNEE_REF).resolve()
                .item(F_ORIGINAL_ASSIGNEE_REF).resolve()
                .item(T_PARENT, F_REQUESTOR_REF).resolve()
                .build();

        List<CaseWorkItemType> workItems = new ArrayList<>( // to assure modifiable result list
                modelService.searchContainers(CaseWorkItemType.class,
                        ObjectQueryUtil.openItemsQuery(), options1, opTask, result));

        displayDumpable("changes by state after first clockwork run", approvalsManager
                .getChangesByState(rootCase, modelInteractionService, prismContext, opTask, result));

        testDetails.afterFirstClockworkRun(rootCase, case0, subcases, workItems, opTask, result);

        if (testDetails.executeImmediately()) {
            if (case0 != null) {
                testHelper.waitForCaseClose(case0, 20000);
            }
            displayDumpable("changes by state after case0 finishes", approvalsManager
                    .getChangesByState(rootCase, modelInteractionService, prismContext, opTask, result));
            testDetails.afterCase0Finishes(rootCase, opTask, result);
        }

        for (int i = 0; i < subcases.size(); i++) {
            CaseType subcase = subcases.get(i);
            PrismProperty<ObjectTreeDeltasType> deltas = subcase.asPrismObject()
                    .findProperty(ItemPath.create(F_APPROVAL_CONTEXT, F_DELTAS_TO_APPROVE));
            assertNotNull("There are no modifications in subcase #" + i + ": " + subcase, deltas);
            assertEquals("Incorrect number of modifications in subcase #" + i + ": " + subcase, 1, deltas.getRealValues().size());
            // todo check correctness of the modification?

            // now check the workflow state
            String caseOid = subcase.getOid();
            List<CaseWorkItemType> caseWorkItems = getWorkItemsForCase(caseOid, null, result);
            assertFalse("work item not found", caseWorkItems.isEmpty());

            for (CaseWorkItemType caseWorkItem : caseWorkItems) {
                Boolean approve = testDetails.decideOnApproval(caseWorkItem);
                if (approve != null) {
                    caseManager.completeWorkItem(WorkItemId.create(caseOid, caseWorkItem.getId()),
                            new AbstractWorkItemOutputType()
                                    .outcome(ApprovalUtils.toUri(approve)),
                            null, opTask, result);
                    login(userAdministrator);
                    break;
                }
            }
        }

        // alternative way of approvals executions
        if (CollectionUtils.isNotEmpty(testDetails.getApprovalSequence())) {
            List<ApprovalInstruction> instructions = new ArrayList<>(testDetails.getApprovalSequence());
            while (!instructions.isEmpty()) {
                List<CaseWorkItemType> currentWorkItems = modelService
                        .searchContainers(CaseWorkItemType.class, ObjectQueryUtil.openItemsQuery(), options1, opTask, result);
                boolean matched = false;

                Collection<ApprovalInstruction> instructionsToConsider = testDetails.strictlySequentialApprovals()
                        ? singleton(instructions.get(0))
                        : instructions;

                main:
                for (ApprovalInstruction approvalInstruction : instructionsToConsider) {
                    for (CaseWorkItemType workItem : currentWorkItems) {
                        if (approvalInstruction.matches(workItem)) {
                            if (approvalInstruction.beforeApproval != null) {
                                approvalInstruction.beforeApproval.run();
                            }
                            login(getUserFromRepo(approvalInstruction.approverOid));
                            System.out.println("Completing work item " + WorkItemId.of(workItem) + " using " + approvalInstruction);
                            caseManager.completeWorkItem(WorkItemId.of(workItem),
                                    new AbstractWorkItemOutputType()
                                            .outcome(ApprovalUtils.toUri(approvalInstruction.approval))
                                            .comment(approvalInstruction.comment),
                                    null, opTask, result);
                            if (approvalInstruction.afterApproval != null) {
                                approvalInstruction.afterApproval.run();
                            }
                            login(userAdministrator);
                            matched = true;
                            instructions.remove(approvalInstruction);
                            break main;
                        }
                    }
                }
                if (!matched) {
                    fail("None of approval instructions " + instructionsToConsider + " matched any of current work items: "
                            + currentWorkItems);
                }
            }
        }

        CaseType rootCaseAfter = testHelper.waitForCaseClose(rootCase, 60000);

        subcases = miscHelper.getSubcases(rootCaseAfter, result);
        WfTestHelper.findAndRemoveCase0(subcases);

        displayDumpable("changes by state after root case finishes", approvalsManager.getChangesByState(
                rootCaseAfter, modelInteractionService, prismContext, opTask, result));

        testDetails.afterRootCaseFinishes(rootCaseAfter, subcases, opTask, result);

        notificationManager.setDisabled(true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        displayDumpable("Output context", modelContext);
        return result;
    }

    protected void assertWfContextAfterClockworkRun(
            CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
            String objectOid, List<ExpectedTask> expectedTasks, List<ExpectedWorkItem> expectedWorkItems) {

        display("rootCase", rootCase);
        assertEquals("Wrong # of wf subcases (" + expectedTasks + ")", expectedTasks.size(), subcases.size());
        int i = 0;
        for (CaseType subcase : subcases) {
            display("Subcase #" + (i + 1) + ": ", subcase);
            checkCase(subcase, subcase.toString(), expectedTasks.get(i));
            WfTestUtil
                    .assertRef("requester ref", subcase.getRequestorRef(), USER_ADMINISTRATOR_OID, false, false);
            i++;
        }

        assertEquals("Wrong # of work items", expectedWorkItems.size(), workItems.size());
        i = 0;
        for (CaseWorkItemType workItem : workItems) {
            display("Work item #" + (i + 1) + ": ", workItem);
            display("Case", CaseTypeUtil.getCase(workItem));
            if (objectOid != null) {
                WfTestUtil.assertRef("object reference", ApprovalContextUtil.getObjectRef(workItem), objectOid, true, true);
            }

            String targetOid = expectedWorkItems.get(i).targetOid;
            if (targetOid != null) {
                WfTestUtil.assertRef("target reference", ApprovalContextUtil.getTargetRef(workItem), targetOid, true, true);
            }
            WfTestUtil
                    .assertRef("assignee reference", workItem.getOriginalAssigneeRef(), expectedWorkItems.get(i).assigneeOid, false, true);
            // name is not known
            //WfTestUtil.assertRef("task reference", workItem.getTaskRef(), null, false, true);
            final CaseType subcase = CaseTypeUtil.getCaseRequired(workItem);
            checkCase(subcase, "subcase in workItem", expectedWorkItems.get(i).task);
            WfTestUtil
                    .assertRef("requester ref", subcase.getRequestorRef(), USER_ADMINISTRATOR_OID, false, true);

            i++;
        }
    }

    private void checkCase(CaseType subcase, String context, ExpectedTask expectedTask) {
        assertNull("Unexpected fetch result in wf subtask: " + context, subcase.getFetchResult());
        ApprovalContextType wfc = subcase.getApprovalContext();
        assertNotNull("Missing workflow context in wf subtask: " + context, wfc);
        assertEquals("Wrong process ID name in subtask: " + context, expectedTask.processName, subcase.getName().getOrig());
        if (expectedTask.targetOid != null) {
            assertEquals("Wrong target OID in subtask: " + context, expectedTask.targetOid, subcase.getTargetRef().getOid());
        } else {
            assertNull("TargetRef in subtask: " + context + " present even if it shouldn't", subcase.getTargetRef());
        }
        assertNotNull("Missing process start time in subtask: " + context, CaseTypeUtil.getStartTimestamp(subcase));
        assertNull("Unexpected process end time in subtask: " + context, subcase.getCloseTimestamp());
        assertNull("Wrong outcome", subcase.getOutcome());
        //assertEquals("Wrong state", null, wfc.getState());
    }

    protected String getTargetOid(CaseWorkItemType caseWorkItem) {
        ObjectReferenceType targetRef = CaseTypeUtil.getCaseRequired(caseWorkItem).getTargetRef();
        assertNotNull("targetRef not found", targetRef);
        String roleOid = targetRef.getOid();
        assertNotNull("requested role OID not found", roleOid);
        return roleOid;
    }

    protected void checkTargetOid(CaseWorkItemType caseWorkItem, String expectedOid) {
        String realOid = getTargetOid(caseWorkItem);
        assertEquals("Unexpected target OID", expectedOid, realOid);
    }

    protected abstract static class TestDetails2<F extends FocusType> {
        protected PrismObject<F> getFocus(OperationResult result) {
            return null;
        }

        protected ObjectDelta<F> getFocusDelta() throws Exception {
            return null;
        }

        protected int getNumberOfDeltasToApprove() {
            return 0;
        }

        protected List<Boolean> getApprovals() {
            return null;
        }

        protected List<ObjectDelta<F>> getExpectedDeltasToApprove() {
            return null;
        }

        protected ObjectDelta<F> getExpectedDelta0() {
            return null;
        }

        protected String getObjectOid() {
            return null;
        }

        protected List<ExpectedTask> getExpectedTasks() {
            return null;
        }

        protected List<ExpectedWorkItem> getExpectedWorkItems() {
            return null;
        }

        protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
        }

        // mutually exclusive with getApprovalSequence
        protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
            return true;
        }

        private void sortSubcases(List<CaseType> subtasks) {
            subtasks.sort(Comparator.comparing(this::getCompareKey));
        }

        private void sortWorkItems(List<CaseWorkItemType> workItems) {
            workItems.sort(Comparator.comparing(this::getCompareKey));
        }

        private String getCompareKey(CaseType aCase) {
            return aCase.getTargetRef().getOid();
        }

        private String getCompareKey(CaseWorkItemType workItem) {
            return workItem.getOriginalAssigneeRef().getOid();
        }

        public List<ApprovalInstruction> getApprovalSequence() {
            return null;
        }

        protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
                OperationResult result) {
        }

        public void setTracing(Task opTask) {
        }
    }

    protected <F extends FocusType> OperationResult executeTest2(TestDetails2<F> testDetails2, int expectedSubTaskCount,
            boolean immediate) throws Exception {
        return executeTest(new TestDetails() {
            @Override
            protected LensContext<F> createModelContext(OperationResult result) throws Exception {
                PrismObject<F> focus = testDetails2.getFocus(result);
                // TODO "object create" context
                LensContext<F> lensContext = createLensContext(focus.getCompileTimeClass());
                fillContextWithFocus(lensContext, focus);
                addFocusDeltaToContext(lensContext, testDetails2.getFocusDelta());
                if (immediate) {
                    lensContext.setOptions(executeOptions().executeImmediatelyAfterApproval());
                }
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) throws Exception {
                if (!immediate) {
                    for (int i = 0; i <= testDetails2.getNumberOfDeltasToApprove(); i++) {
                        testDetails2.assertDeltaExecuted(i, false, opTask, result);
                    }
                    testDetails2.sortSubcases(subcases);
                    testDetails2.sortWorkItems(workItems);
                    assertWfContextAfterClockworkRun(rootCase, subcases, workItems,
                            testDetails2.getObjectOid(),
                            testDetails2.getExpectedTasks(), testDetails2.getExpectedWorkItems());
                    for (CaseType subcase : subcases) {
                        if (subcase.getApprovalContext() != null) {
                            OperationResult opResult = new OperationResult("dummy");
                            ApprovalSchemaExecutionInformationType info =
                                    approvalsManager.getApprovalSchemaExecutionInformation(subcase.getOid(), opTask, opResult);
                            modelObjectResolver.resolveAllReferences(Collections.singleton(info.asPrismContainerValue()), opTask, result); // MID-6171
                            display("Execution info for " + subcase, info);
                            opResult.computeStatus();
                            assertSuccess("Unexpected problem when looking at getApprovalSchemaExecutionInformation result", opResult);
                        }
                    }
                }
                testDetails2.afterFirstClockworkRun(rootCase, subcases, workItems, result);
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) throws Exception {
                if (!immediate) {
                    return;
                }
                for (int i = 1; i <= testDetails2.getNumberOfDeltasToApprove(); i++) {
                    testDetails2.assertDeltaExecuted(i, false, opTask, result);
                }
                testDetails2.assertDeltaExecuted(0, true, opTask, result);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                for (int i = 0; i <= testDetails2.getNumberOfDeltasToApprove(); i++) {
                    testDetails2.assertDeltaExecuted(i, i == 0 || testDetails2.getApprovals().get(i - 1), opTask, result);
                }
            }

            @Override
            protected boolean executeImmediately() {
                return immediate;
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                return testDetails2.decideOnApproval(caseWorkItem);
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                return testDetails2.getApprovalSequence();
            }

            @Override
            public void setTracing(Task opTask) {
                testDetails2.setTracing(opTask);
            }
        }, expectedSubTaskCount);
    }
}
