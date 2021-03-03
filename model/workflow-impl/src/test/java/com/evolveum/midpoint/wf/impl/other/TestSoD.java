/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.ExpectedTask;
import com.evolveum.midpoint.wf.impl.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Testing approvals of role SoD: assigning roles that are in conflict.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSoD extends AbstractWfTestPolicy {

    protected static final File TEST_SOD_RESOURCE_DIR = new File("src/test/resources/sod");

    protected static final File METAROLE_CRIMINAL_EXCLUSION_FILE = new File(TEST_SOD_RESOURCE_DIR, "metarole-criminal-exclusion.xml");
    protected static final File ROLE_JUDGE_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-judge.xml");
    protected static final File ROLE_PIRATE_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-pirate.xml");
    protected static final File ROLE_THIEF_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-thief.xml");
    protected static final File ROLE_RESPECTABLE_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-respectable.xml");
    protected static final File USER_SOD_APPROVER_FILE = new File(TEST_SOD_RESOURCE_DIR, "user-sod-approver.xml");

    protected String metaroleCriminalExclusion;
    protected String roleJudgeOid;
    protected String rolePirateOid;
    protected String roleThiefOid;
    protected String roleRespectableOid;
    protected String userSodApproverOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        metaroleCriminalExclusion = repoAddObjectFromFile(METAROLE_CRIMINAL_EXCLUSION_FILE, initResult).getOid();
        roleJudgeOid = repoAddObjectFromFile(ROLE_JUDGE_FILE, initResult).getOid();
        rolePirateOid = repoAddObjectFromFile(ROLE_PIRATE_FILE, initResult).getOid();
        roleThiefOid = repoAddObjectFromFile(ROLE_THIEF_FILE, initResult).getOid();
        roleRespectableOid = repoAddObjectFromFile(ROLE_RESPECTABLE_FILE, initResult).getOid();
        userSodApproverOid = addAndRecomputeUser(USER_SOD_APPROVER_FILE, initTask, initResult);
    }

    /**
     * Assign Judge to jack. This should work without approvals.
     */
    @Test
    public void test010AssignRoleJudge() throws Exception {
        given();

        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        assignRole(userJackOid, roleJudgeOid, task, result);

        then();
        display("jack as a Judge", getUser(userJackOid));
        assertAssignedRole(userJackOid, roleJudgeOid, result);
    }

    /**
     * Assign Pirate to jack. This should trigger an approval.
     */
    @Test
    public void test020AssignRolePirate() throws Exception {
        given();
        login(userAdministrator);

        OperationResult result = getTestOperationResult();

        PrismObject<UserType> jack = getUser(userJackOid);
        String originalDescription = jack.asObjectable().getDescription();

        ObjectDelta<UserType> addPirateDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(rolePirateOid, ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> changeDescriptionDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("Pirate Judge")
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil.summarize(addPirateDelta, changeDescriptionDelta);

        when();
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jack.clone();
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() {
                return primaryDelta.clone();
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return Collections.singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return Collections.singletonList(addPirateDelta.clone());
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return changeDescriptionDelta.clone();
            }

            @Override
            protected String getObjectOid() {
                return jack.getOid();
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return Collections.singletonList(
                        new ExpectedTask(rolePirateOid, "Role \"Pirate\" excludes role \"Judge\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                List<ExpectedTask> etasks = getExpectedTasks();
                return Collections.singletonList(
                        new ExpectedWorkItem(userSodApproverOid, rolePirateOid, etasks.get(0)));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                switch (number) {
                    case 0:
                        if (yes) {
                            assertUserProperty(userJackOid, UserType.F_DESCRIPTION, "Pirate Judge");
                        } else {
                            if (originalDescription != null) {
                                assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
                            } else {
                                assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
                            }
                        }
                        break;

                    case 1:
                        if (yes) {
                            assertAssignedRole(userJackOid, rolePirateOid, result);
                        } else {
                            assertNotAssignedRole(userJackOid, rolePirateOid, result);
                        }
                        break;
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                login(getUser(userSodApproverOid));
                return true;
            }

        }, 1, false);

        then();
        display("jack as a Pirate + Judge", getUser(userJackOid));
        AssignmentType judgeAssignment = assertAssignedRole(userJackOid, roleJudgeOid, result);
        assertExclusionViolationSituation(judgeAssignment);
        AssignmentType pirateAssignment = assertAssignedRole(userJackOid, rolePirateOid, result);
        assertExclusionViolationSituation(pirateAssignment);
    }

    private void assertExclusionViolationSituation(AssignmentType assignment) {
        assertThat(assignment.getPolicySituation())
                .as("policy situation in " + assignment)
                .hasSize(1)
                .containsExactly(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION);
    }

    /**
     * Assign Respectable to jack. This should trigger an approval as well (because it implies a Thief).
     */
    @Test
    public void test030AssignRoleRespectable() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN
        unassignRole(userJackOid, rolePirateOid, task, result);
        assertNotAssignedRole(userJackOid, rolePirateOid, result);

        // WHEN+THEN
        PrismObject<UserType> jack = getUser(userJackOid);
        ObjectDelta<UserType> addRespectableDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRespectableOid, ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);

        // WHEN+THEN
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jack.clone();
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() {
                return addRespectableDelta.clone();
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return Collections.singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return Arrays.asList(addRespectableDelta.clone());
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                //return ObjectDelta.createEmptyModifyDelta(UserType.class, jack.getOid(), prismContext);
                return prismContext.deltaFactory().object()
                        .createModifyDelta(jack.getOid(), Collections.emptyList(), UserType.class
                        );
            }

            @Override
            protected String getObjectOid() {
                return jack.getOid();
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return Collections.singletonList(
                        new ExpectedTask(roleRespectableOid, "Role \"Thief\" (Respectable -> Thief) excludes role \"Judge\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                List<ExpectedTask> etasks = getExpectedTasks();
                return Collections.singletonList(
                        new ExpectedWorkItem(userSodApproverOid, roleRespectableOid, etasks.get(0)));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                switch (number) {
                    case 1:
                        if (yes) {
                            assertAssignedRole(userJackOid, roleRespectableOid, result);
                        } else {
                            assertNotAssignedRole(userJackOid, roleRespectableOid, result);
                        }
                        break;
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                login(getUser(userSodApproverOid));
                return true;
            }

        }, 1, false);

        // THEN
        display("jack as a Judge + Respectable", getUser(userJackOid));
        assertAssignedRole(userJackOid, roleJudgeOid, result);
        assertAssignedRole(userJackOid, roleRespectableOid, result);
    }
}
