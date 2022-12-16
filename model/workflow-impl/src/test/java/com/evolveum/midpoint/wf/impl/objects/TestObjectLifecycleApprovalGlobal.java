/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.objects;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.CloneUtil.cloneCollectionMembers;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.ApprovalInstruction;
import com.evolveum.midpoint.wf.impl.ExpectedTask;
import com.evolveum.midpoint.wf.impl.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests role lifecycle with global policy rules.
 *
 * Besides other (common) operations it tests the "processSpecification" feature in test500-test799.
 */
public class TestObjectLifecycleApprovalGlobal extends AbstractTestObjectLifecycleApproval {

    private static final File GLOBAL_POLICY_RULES_FILE = new File(TEST_RESOURCE_DIR, "global-policy-rules.xml");
    private static final @NotNull String OID1 = "10000001-d34d-b33f-f00d-d34db33ff00d";
    private static final @NotNull String OID2 = "20000002-d34d-b33f-f00d-d34db33ff00d";

    @Override
    protected boolean approveObjectAdd() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Override
    protected void updateSystemConfiguration(SystemConfigurationType systemConfiguration) throws SchemaException, IOException {
        super.updateSystemConfiguration(systemConfiguration);
        PrismObject<SystemConfigurationType> rulesContainer = prismContext.parserFor(GLOBAL_POLICY_RULES_FILE).parse();
        systemConfiguration.getGlobalPolicyRule().clear();
        systemConfiguration.getGlobalPolicyRule().addAll(cloneCollectionMembers(rulesContainer.asObjectable().getGlobalPolicyRule()));
    }

    private String roleJudgeOid;
    private String roleCaptainOid;
    private String roleThiefOid;

    @Test
    public void test500CreateRoleJudge() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        RoleType judge = new RoleType(prismContext)
                .name("judge")
                .riskLevel("high");

        ObjectDelta<RoleType> addObjectDelta = DeltaFactory.Object.createAddDelta(judge.asPrismObject());

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, addObjectDelta);
                lensContext.setOptions(executeOptions().executeImmediatelyAfterApproval());
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                display("subtasks", subcases);
                display("work items", workItems);
                // todo some asserts here
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) throws Exception {
                assertNoObject(judge);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                assertObject(judge);
            }

            @Override
            protected boolean executeImmediately() {
                return true;
            }

            @Override
            public boolean strictlySequentialApprovals() {
                return true;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ApprovalInstruction> instructions = new ArrayList<>();
                // this is step 2 in riskLevel part (first step is owner that is skipped)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Setting riskLevel")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 3 in riskLevel part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_PETER_OID, null, new ExpectedTask(null, "Setting riskLevel")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in main part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_JUPITER_OID, null, new ExpectedTask(null, "Adding role \"judge\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 3 in main part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Adding role \"judge\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                return instructions;
            }
        }, 2);

        // TODO some more asserts

        PrismObject<RoleType> judgeAfter = searchObjectByName(RoleType.class, "judge");
        roleJudgeOid = judgeAfter.getOid();

        PrismReferenceValue judgeOwner = getPrismContext().itemFactory().createReferenceValue(roleJudgeOid, RoleType.COMPLEX_TYPE);
        judgeOwner.setRelation(SchemaConstants.ORG_OWNER);
        executeChanges(prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(judgeAfter, SchemaConstants.ORG_OWNER))
                        .asObjectDelta(USER_JUDGE_OWNER_OID),
                null, task, result);

        display("Judge role", judgeAfter);
        display("Judge owner", getUser(USER_JUDGE_OWNER_OID));

        assertEquals("Wrong risk level", "high", judgeAfter.asObjectable().getRiskLevel());
    }

    @Test
    public void test510AddInducementsToJudge() throws Exception {
        login(userAdministrator);

        ObjectDelta<RoleType> judgeDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT)
                .add(createAssignmentTo(OID1, ObjectTypes.ROLE, prismContext),
                        createAssignmentTo(OID2, ObjectTypes.ROLE, prismContext))
                .item(RoleType.F_DESCRIPTION)
                .replace("hi")
                .asObjectDelta(roleJudgeOid);

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, judgeDelta);
                lensContext.setOptions(executeOptions().executeImmediatelyAfterApproval());
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                display("subtasks", subcases);
                display("work items", workItems);
                // todo some asserts here
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) {
                // nothing here
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                // nothing here
            }

            @Override
            protected boolean executeImmediately() {
                return true;
            }

            @Override
            public boolean strictlySequentialApprovals() {
                return true;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ApprovalInstruction> instructions = new ArrayList<>();
                // this is step 1 in 1st inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in 1st inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in 2nd inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in 2nd inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in main part (owner)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_JUDGE_OWNER_OID, null, new ExpectedTask(null, "Modifying role \"judge\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in main part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Modifying role \"judge\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                return instructions;
            }
        }, 3);

        // TODO some more asserts

        PrismObject<RoleType> judgeAfter = searchObjectByName(RoleType.class, "judge");

        display("Judge role", judgeAfter);

        assertEquals("Wrong risk level", "high", judgeAfter.asObjectable().getRiskLevel());
        assertEquals("Wrong description", "hi", judgeAfter.asObjectable().getDescription());
        assertInducedRoles(judgeAfter, OID1, OID2);
    }

    // MID-4372
    @Test
    public void test520DeleteRoleJudge() throws Exception {
        login(userAdministrator);

        deleteObject(UserType.class, USER_JUDGE_OWNER_OID);

        ObjectDelta<RoleType> deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(RoleType.class, roleJudgeOid
        );

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, deleteDelta);
                lensContext.setOptions(executeOptions().executeImmediatelyAfterApproval());
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                display("subtasks", subcases);
                display("work items", workItems);
                // todo some asserts here
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) {
                assertObjectExists(RoleType.class, roleJudgeOid);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                assertObjectDoesntExist(RoleType.class, roleJudgeOid);
            }

            @Override
            protected boolean executeImmediately() {
                return true;
            }

            @Override
            public boolean strictlySequentialApprovals() {
                return true;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ApprovalInstruction> instructions = new ArrayList<>();
                // this is step 2 in main part (first step is owner that is skipped as the owner was already deleted)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Deleting role \"judge\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                return instructions;
            }
        }, 1);
    }

    @Test
    public void test600CreateRoleCaptain() throws Exception {
        login(userAdministrator);

        RoleType captain = new RoleType(prismContext)
                .name("captain")
                .description("something")
                .riskLevel("high")
                .inducement(createAssignmentTo(OID1, ObjectTypes.ROLE, prismContext))
                .inducement(createAssignmentTo(OID2, ObjectTypes.ROLE, prismContext));
        ObjectDelta<RoleType> addObjectDelta = DeltaFactory.Object.createAddDelta(captain.asPrismObject());

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, addObjectDelta);
                lensContext.setOptions(executeOptions().executeImmediatelyAfterApproval());
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                display("subtasks", subcases);
                display("work items", workItems);
                // todo some asserts here
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) throws Exception {
                assertNoObject(captain);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                assertObject(captain);
            }

            @Override
            protected boolean executeImmediately() {
                return true;
            }

            @Override
            public boolean strictlySequentialApprovals() {
                return true;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ApprovalInstruction> instructions = new ArrayList<>();
                // this is step 2 in riskLevel part (first step is owner that is skipped)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Setting riskLevel")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 3 in riskLevel part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_PETER_OID, null, new ExpectedTask(null, "Setting riskLevel")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in main part (first step is owner that is skipped)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_JUPITER_OID, null, new ExpectedTask(null, "Adding role \"captain\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 3 in main part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Adding role \"captain\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                return instructions;
            }
        }, 4);

        // TODO some more asserts

        PrismObject<RoleType> captainAfter = searchObjectByName(RoleType.class, "captain");
        roleCaptainOid = captainAfter.getOid();
        display("Captain role", captainAfter);

        assertEquals("Wrong risk level", "high", captainAfter.asObjectable().getRiskLevel());
        assertInducedRoles(captainAfter, OID1, OID2);
    }

    @Test
    public void test610DeleteInducementsFromCaptain() throws Exception {
        login(userAdministrator);

        PrismObject<RoleType> captainBefore = getRole(roleCaptainOid);

        ObjectDelta<RoleType> captainDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT)
                .delete(cloneCollectionMembers(captainBefore.findContainer(RoleType.F_INDUCEMENT).getValues()))
                .asObjectDelta(roleCaptainOid);

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, captainDelta);
                lensContext.setOptions(executeOptions().executeImmediatelyAfterApproval());
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                display("subtasks", subcases);
                display("work items", workItems);
                // todo some asserts here
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) {
                // nothing here
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                // nothing here
            }

            @Override
            protected boolean executeImmediately() {
                return true;
            }

            @Override
            public boolean strictlySequentialApprovals() {
                return true;
            }

            @SuppressWarnings("Duplicates")
            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ApprovalInstruction> instructions = new ArrayList<>();
                // this is step 1 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                return instructions;
            }
        }, 2);

        // TODO some more asserts

        PrismObject<RoleType> captainAfter = getRole(roleCaptainOid);

        display("Captain role", captainAfter);

        assertEquals("Wrong risk level", "high", captainAfter.asObjectable().getRiskLevel());
        assertInducements(captainAfter, 0);
    }

    @Test
    public void test700CreateRoleThief() throws Exception {
        login(userAdministrator);

        RoleType thief = new RoleType(prismContext)
                .name("thief")
                .description("something")
                .riskLevel("high")
                .inducement(createAssignmentTo(OID1, ObjectTypes.ROLE, prismContext))
                .inducement(createAssignmentTo(OID2, ObjectTypes.ROLE, prismContext));

        ObjectDelta<RoleType> addObjectDelta = DeltaFactory.Object.createAddDelta(thief.asPrismObject());

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, addObjectDelta);
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                display("subtasks", subcases);
                display("work items", workItems);
                // todo some asserts here
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) throws Exception {
                assertNoObject(thief);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                assertObject(thief);
            }

            @Override
            protected boolean executeImmediately() {
                return false;
            }

            @Override
            public boolean strictlySequentialApprovals() {
                return true;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ApprovalInstruction> instructions = new ArrayList<>();
                // this is step 2 in riskLevel part (first step is owner that is skipped)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Setting riskLevel")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 3 in riskLevel part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_PETER_OID, null, new ExpectedTask(null, "Setting riskLevel")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in main part (first step is owner that is skipped)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_JUPITER_OID, null, new ExpectedTask(null, "Adding role \"thief\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 3 in main part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Adding role \"thief\"")), true, USER_ADMINISTRATOR_OID,
                        null));
                return instructions;
            }
        }, 4);

        // TODO some more asserts

        PrismObject<RoleType> thiefAfter = searchObjectByName(RoleType.class, "thief");
        roleThiefOid = thiefAfter.getOid();
        display("Thief role", thiefAfter);

        assertEquals("Wrong risk level", "high", thiefAfter.asObjectable().getRiskLevel());
        assertInducedRoles(thiefAfter, OID1, OID2);
    }

    @Test
    public void test710DeleteInducementsFromThief() throws Exception {
        login(userAdministrator);

        PrismObject<RoleType> thiefBefore = getRole(roleThiefOid);

        ObjectDelta<RoleType> captainDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT)
                .delete(cloneCollectionMembers(thiefBefore.findContainer(RoleType.F_INDUCEMENT).getValues()))
                .asObjectDelta(roleThiefOid);

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, captainDelta);
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                display("subtasks", subcases);
                display("work items", workItems);
                // todo some asserts here
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) {
                // nothing here
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                // nothing here
            }

            @Override
            protected boolean executeImmediately() {
                return false;
            }

            @Override
            public boolean strictlySequentialApprovals() {
                return true;
            }

            @SuppressWarnings("Duplicates")
            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ApprovalInstruction> instructions = new ArrayList<>();
                // this is step 1 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 1 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_ADMINISTRATOR_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                // this is step 2 in inducement part (2nd)
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_BOB_OID, null, new ExpectedTask(null, "Changing inducement")), true, USER_ADMINISTRATOR_OID,
                        null));
                return instructions;
            }
        }, 2);

        // TODO some more asserts

        PrismObject<RoleType> thiefAfter = getRole(roleThiefOid);

        display("Thief role", thiefAfter);

        assertEquals("Wrong risk level", "high", thiefAfter.asObjectable().getRiskLevel());
        assertInducements(thiefAfter, 0);
    }

    // TODO test that contains task0 that adds an object (i.e. rule for 'add' is not applied)
}
