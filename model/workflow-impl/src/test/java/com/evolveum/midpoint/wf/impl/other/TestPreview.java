/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.util.ApprovalSchemaExecutionInformationUtil.getEmbeddedCaseBean;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalSchemaExecutionInformationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.CaseEventUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the preview feature:
 * 1) before operation is executed,
 * 2) in various stages of approval process as well.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPreview extends AbstractWfTestPolicy {

    private static final File TEST_RESOURCE_DIR = new File("src/test/resources/preview");

    /*
     * Alice is given lab-manager role. This assignment should be approved in three stages:
     * 1. by Jane the Lab Owner
     * 2. by Martin the Department Head
     * 3. By Peter the Dean *OR* Kate the Administrator
     */
    private static final TestResource<RoleType> ROLE_LAB_MANAGER = new TestResource<>(TEST_RESOURCE_DIR, "role-lab-manager.xml", "e4b5d89a-cb7f-4d26-b31f-e86556e2a4ca");
    private static final TestResource<UserType> USER_ALICE = new TestResource<>(TEST_RESOURCE_DIR, "user-alice.xml", "0b728b21-1649-40d2-80dd-566a5faaeb86");
    private static final TestResource<UserType> USER_JANE = new TestResource<>(TEST_RESOURCE_DIR, "user-jane-the-lab-owner.xml", "feb34927-7671-401e-9f5b-8f7ec94f3112");
    private static final TestResource<UserType> USER_MARTIN = new TestResource<>(TEST_RESOURCE_DIR, "user-martin-the-dept-head.xml", "072bf16a-e424-456c-a212-7996f34c3c5c");
    private static final TestResource<UserType> USER_PETER = new TestResource<>(TEST_RESOURCE_DIR, "user-peter-the-dean.xml", "408beff8-c988-4c77-ac5e-ed26697d6982");
    private static final TestResource<UserType> USER_KATE = new TestResource<>(TEST_RESOURCE_DIR, "user-kate-the-administrator.xml", "4aab211b-5faf-45e2-acaf-a17a89d39fd1");

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_ALICE, initResult);
        repoAdd(USER_JANE, initResult);
        repoAdd(USER_MARTIN, initResult);
        repoAdd(USER_PETER, initResult);
        repoAdd(USER_KATE, initResult);
        repoAdd(ROLE_LAB_MANAGER, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    /**
     * Use pure "previewChanges" and check the approval schema execution information.
     */
    @Test
    public void test100PurePreview() throws Exception {
        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        ModelExecuteOptions options = executeOptions();
        options.getOrCreatePartialProcessing().setApprovals(PartialProcessingTypeType.PROCESS);

        when();
        ModelContext<UserType> modelContext =
                modelInteractionService.previewChanges(getAssignmentDeltas(), options, task, result);

        then();
        ApprovalSchemaExecutionInformationType execInfo = getSingleExecutionInformation(modelContext);
        displayExecutionInformation(execInfo);

        // Everything is "in the future", so we have to obtain approvers information from the execInfo.stage list
        assertThat(getFutureStageApprovers(execInfo, 1)).as("stage 1 approvers").containsExactlyInAnyOrder(USER_JANE.oid);
        assertThat(getFutureStageApprovers(execInfo, 2)).as("stage 2 approvers").containsExactlyInAnyOrder(USER_MARTIN.oid);
        assertThat(getFutureStageApprovers(execInfo, 3)).as("stage 3 approvers").containsExactlyInAnyOrder(USER_PETER.oid, USER_KATE.oid);
    }

    /**
     * Start the execution (i.e. case will be created) and check the approval schema execution information
     * just after that.
     */
    @Test
    public void test110InfoAfterStart() throws Exception {
        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        executeChanges(getAssignmentDeltas(), null, task, result);

        then();
        CaseType approvalCase = assertCase(result, "after")
                .display()
                .subcases()
                .single()
                .display()
                .assertStageNumber(1)
                .getObject().asObjectable();

        ApprovalSchemaExecutionInformationType execInfo =
                approvalsManager.getApprovalSchemaExecutionInformation(approvalCase.getOid(), task, result);
        displayExecutionInformation(execInfo);

        assertThat(getCurrentStageApprovers(execInfo)).as("current stage (1) approvers").containsExactlyInAnyOrder(USER_JANE.oid);

        // Everything after stage 1 is "in the future", so we have to obtain approvers information from the execInfo.stage list
        assertThat(getFutureStageApprovers(execInfo, 2)).as("stage 2 approvers").containsExactlyInAnyOrder(USER_MARTIN.oid);
        assertThat(getFutureStageApprovers(execInfo, 3)).as("stage 3 approvers").containsExactlyInAnyOrder(USER_PETER.oid, USER_KATE.oid);
    }

    /**
     * Approve the work item by administrator; this will move the case into second stage.
     */
    @Test
    public void test120InfoAfterStageOneApproval() throws Exception {
        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        CaseWorkItemType workItem = getWorkItem(task, result);
        approveWorkItem(workItem, task, result);

        then();
        CaseType approvalCase = assertCase(workItem, "after")
                .display()
                .assertStageNumber(2)
                .getObject().asObjectable();

        ApprovalSchemaExecutionInformationType execInfo =
                approvalsManager.getApprovalSchemaExecutionInformation(approvalCase.getOid(), task, result);
        displayExecutionInformation(execInfo);

        assertThat(getPastStageApprovers(execInfo, 1)).as("stage 1 approvers").containsExactlyInAnyOrder(USER_ADMINISTRATOR_OID);
        assertThat(getCurrentStageApprovers(execInfo)).as("current stage (2) approvers").containsExactlyInAnyOrder(USER_MARTIN.oid);
        // Everything after stage 2 is "in the future", so we have to obtain approvers information from the execInfo.stage list
        assertThat(getFutureStageApprovers(execInfo, 3)).as("stage 3 approvers").containsExactlyInAnyOrder(USER_PETER.oid, USER_KATE.oid);
    }

    /**
     * Approve the work item again by administrator; this will move the case into the third stage.
     */
    @Test
    public void test130InfoAfterStageTwoApproval() throws Exception {
        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        CaseWorkItemType workItem = getWorkItem(task, result);
        approveWorkItem(workItem, task, result);

        then();
        CaseType approvalCase = assertCase(workItem, "after")
                .display()
                .assertStageNumber(3)
                .getObject().asObjectable();

        ApprovalSchemaExecutionInformationType execInfo =
                approvalsManager.getApprovalSchemaExecutionInformation(approvalCase.getOid(), task, result);
        displayExecutionInformation(execInfo);

        assertThat(getPastStageApprovers(execInfo, 1)).as("stage 1 approvers").containsExactlyInAnyOrder(USER_ADMINISTRATOR_OID);
        assertThat(getPastStageApprovers(execInfo, 2)).as("stage 2 approvers").containsExactlyInAnyOrder(USER_ADMINISTRATOR_OID);
        assertThat(getCurrentStageApprovers(execInfo)).as("current stage (3) approvers").containsExactlyInAnyOrder(USER_PETER.oid, USER_KATE.oid);
    }

    /**
     * Approve one of the work items again by administrator. The other work item will be still open.
     */
    @Test
    public void test140InfoAfterStageThreeFirstApproval() throws Exception {
        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        CaseWorkItemType workItem = workItems.stream()
                .filter(wi -> MiscUtil.extractSingleton(wi.getAssigneeRef()).getOid().equals(USER_PETER.oid))
                .findFirst().orElseThrow(() -> new AssertionError("No Peter's work item"));
        approveWorkItem(workItem, task, result);

        then();
        CaseType approvalCase = assertCase(workItem, "after")
                .display()
                .assertStageNumber(3)
                .getObject().asObjectable();

        ApprovalSchemaExecutionInformationType execInfo =
                approvalsManager.getApprovalSchemaExecutionInformation(approvalCase.getOid(), task, result);
        displayExecutionInformation(execInfo);

        assertThat(getPastStageApprovers(execInfo, 1)).as("stage 1 approvers").containsExactlyInAnyOrder(USER_ADMINISTRATOR_OID);
        assertThat(getPastStageApprovers(execInfo, 2)).as("stage 2 approvers").containsExactlyInAnyOrder(USER_ADMINISTRATOR_OID);
        assertThat(getCurrentStageApprovers(execInfo)).as("current stage (3) approvers").containsExactlyInAnyOrder(USER_ADMINISTRATOR_OID, USER_KATE.oid);
        assertThat(getOpenWorkItemsApprovers(execInfo)).as("open work items approvers").containsExactlyInAnyOrder(USER_KATE.oid);
    }

    private Collection<String> getPastStageApprovers(ApprovalSchemaExecutionInformationType execInfo, int stageNumber) {
        return getApproversFromCompletionEvents(execInfo, stageNumber);
    }

    private Collection<String> getCurrentStageApprovers(ApprovalSchemaExecutionInformationType execInfo) {
        Set<String> approvers = new HashSet<>(getOpenWorkItemsApprovers(execInfo));
        approvers.addAll(getApproversFromCompletionEvents(execInfo, execInfo.getCurrentStageNumber()));
        return approvers;
    }

    private Collection<String> getFutureStageApprovers(ApprovalSchemaExecutionInformationType executionInfo, int stageNumber) {
        ApprovalStageExecutionInformationType stage = ApprovalSchemaExecutionInformationUtil.getStage(executionInfo, stageNumber);
        assertThat(stage).isNotNull();
        assertThat(stage.getExecutionPreview()).isNotNull();
        return stage.getExecutionPreview().getExpectedApproverRef().stream()
                .map(ObjectReferenceType::getOid)
                .collect(Collectors.toSet());
    }

    private Collection<String> getOpenWorkItemsApprovers(ApprovalSchemaExecutionInformationType execInfo) {
        CaseType aCase = getEmbeddedCaseBean(execInfo);
        return aCase.getWorkItem().stream()
                .filter(wi -> java.util.Objects.equals(wi.getStageNumber(), aCase.getStageNumber()))
                .filter(CaseTypeUtil::isCaseWorkItemNotClosed)
                .flatMap(wi -> wi.getAssigneeRef().stream())
                .map(ObjectReferenceType::getOid)
                .collect(Collectors.toSet());
    }

    // We could use either (closed) work items or WorkItemCompletionEventType events.
    // The latter is better because we can also have attorney information there.
    private Collection<String> getApproversFromCompletionEvents(ApprovalSchemaExecutionInformationType executionInfo, int stageNumber) {
        CaseType aCase = getEmbeddedCaseBean(executionInfo);
        return aCase.getEvent().stream()
                .filter(event -> event instanceof WorkItemCompletionEventType)
                .map(event -> (WorkItemCompletionEventType) event)
                .filter(event -> java.util.Objects.equals(event.getStageNumber(), stageNumber))
                .filter(CaseEventUtil::completedByUserAction)
                .map(event -> event.getInitiatorRef().getOid())
                .collect(Collectors.toSet());
    }

    private ApprovalSchemaExecutionInformationType getSingleExecutionInformation(ModelContext<UserType> modelContext) {
        List<ApprovalSchemaExecutionInformationType> approvalsExecutionList =
                modelContext.getHookPreviewResults(ApprovalSchemaExecutionInformationType.class);
        assertThat(approvalsExecutionList).size().as("approvals execution list").isEqualTo(1);
        return approvalsExecutionList.get(0);
    }

    private void displayExecutionInformation(ApprovalSchemaExecutionInformationType execution) throws SchemaException {
        String xml = prismContext.xmlSerializer().options(SerializationOptions.createSerializeCompositeObjects())
                .root(SchemaConstantsGenerated.C_APPROVAL_SCHEMA_EXECUTION_INFORMATION).serialize(execution.asPrismContainerValue());
        displayValue("execution information", xml);
    }

    private List<ObjectDelta<? extends ObjectType>> getAssignmentDeltas() throws SchemaException {
        return Collections.singletonList(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(ObjectTypeUtil.createAssignmentTo(ROLE_LAB_MANAGER.oid, ObjectTypes.ROLE))
                        .asObjectDelta(USER_ALICE.oid));
    }
}
