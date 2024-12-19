/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Various tests related to recomputation (or other treatment) of members of changed abstract roles.
 * See also https://docs.evolveum.com/midpoint/reference/synchronization/linked-objects/.
 *
 * There are two kinds of organizations here:
 *
 * - departments:
 * * orgs are managed directly in midPoint
 * * members (users-dcs-xxxx, users-cc-xxxx) are recomputed by default, and using direct iterative bulk action task
 * - clubs:
 * * orgs are managed by reconciliation from special "clubs" resource, although members are managed by midPoint
 * * members (alice, bob, chuck) are NOT recomputed by default
 * * recompute is done using delayed triggers, and is requested e.g. when synchronizing clubs from "clubs" resource
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMemberRecompute extends AbstractEmptyModelIntegrationTest implements CommonTasks {

    public static final File TEST_DIR = new File("src/test/resources/member-recompute");

    private static final ItemName MEMBER_RECOMPUTATION_WORKER_THREADS_NAME = new ItemName(NS_LINKED, "memberRecomputationWorkerThreads");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> TEMPLATE_USER = TestObject.file(TEST_DIR, "template-user.xml", "7d6bf307-58c2-4ea9-8599-19586623b41a");
    private static final TestObject<ArchetypeType> ARCHETYPE_DEPARTMENT = TestObject.file(TEST_DIR, "archetype-department.xml", "b685545e-995f-45e0-8d32-92cd3781ef54");
    private static final TestObject<ArchetypeType> ARCHETYPE_CLUB = TestObject.file(TEST_DIR, "archetype-club.xml", "fefa9261-b897-439c-ad79-15f10d547bba");

    private static final String ATTR_DISPLAY_NAME = "displayName";

    private static final DummyTestResource RESOURCE_CLUBS = new DummyTestResource(TEST_DIR, "resource-clubs.xml", "a79a332a-b469-4092-b3f7-063165ce1024", "clubs",
            controller ->
                    controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                            ATTR_DISPLAY_NAME, String.class, false, false));

    private static final String CLUB_POKER_NAME = "poker";
    private static final String CLUB_POKER_DISPLAY_NAME = "Poker Club";
    private static final String CLUB_CHESS_NAME = "chess";
    private static final String CLUB_CHESS_DISPLAY_NAME = "Chess Club";

    private static final TestObject<TaskType> TASK_RECONCILE_CLUBS = TestObject.file(TEST_DIR, "task-reconcile-clubs.xml", "8d6f6a54-cfdc-4439-9f6a-06ff2a0ba273");

    private static final TestObject<OrgType> ORG_DCS = TestObject.file(TEST_DIR, "org-dcs.xml", "67720733-9de6-47da-b856-ce063c4a6659");
    private static final TestObject<OrgType> ORG_CC = TestObject.file(TEST_DIR, "org-cc.xml", "08a8fe26-e8b6-4005-b23d-e7dc1472b209");
    private static final TestObject<OrgType> ORG_IT_STAFF = TestObject.file(TEST_DIR, "org-it-staff.xml", "51726874-de60-42f1-aab4-a4afb0702833");

    private static final TestObject<UserType> USER_ALICE = TestObject.file(TEST_DIR, "user-alice.xml", "aaeff30f-00ba-4b76-aed1-3d29081e0104");
    private static final TestObject<UserType> USER_BOB = TestObject.file(TEST_DIR, "user-bob.xml", "fbbaa4a8-f159-49f6-a019-987e54ab58e6");
    private static final TestObject<UserType> USER_CHUCK = TestObject.file(TEST_DIR, "user-chuck.xml", "e5434ddf-8571-4eb7-bf6f-33904653549e");

    private static final TestObject<TaskType> TASK_TEMPLATE_RECOMPUTE_MEMBERS = TestObject.file(TEST_DIR, "task-template-recompute-members.xml", "9c50ac7e-73c0-45cf-85e7-9a94959242f9");
    private static final String TASK_TRIGGER_CLUB_MEMBERS_RECOMPUTATION_NAME = "Trigger club members recomputation";

    private DummyGroup groupPokerClub, groupChessClub;
    private String pokerClubOid, chessClubOid;

    @SuppressWarnings("FieldCanBeLocal") private final int DCS_USERS = 20;
    @SuppressWarnings("FieldCanBeLocal") private final int CC_USERS = 10;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importTaskArchetypes(initResult);

        addObject(TASK_TEMPLATE_RECOMPUTE_MEMBERS, initTask, initResult);

        addObject(TEMPLATE_USER, initTask, initResult);
        addObject(ARCHETYPE_DEPARTMENT, initTask, initResult);
        addObject(ARCHETYPE_CLUB, initTask, initResult);

        initDummyResource(RESOURCE_CLUBS, initTask, initResult);
        createClubGroups();

        addObject(TASK_TRIGGER_SCANNER_ON_DEMAND, initTask, initResult);
        addObject(TASK_RECONCILE_CLUBS, initTask, initResult);

        // We want to have clubs prepared in repo after initSystem.
        reconcileClubs(initResult);

        addObject(ORG_DCS, initTask, initResult);
        addObject(ORG_CC, initTask, initResult);
        addObject(ORG_IT_STAFF, initTask, initResult);

        createUsers("user-dcs-%04d", DCS_USERS, initTask, initResult, ORG_DCS, ORG_IT_STAFF);
        createUsers("user-cc-%04d", CC_USERS, initTask, initResult, ORG_CC, ORG_IT_STAFF);

        addObject(USER_ALICE, initTask, initResult);
        addObject(USER_BOB, initTask, initResult);
        addObject(USER_CHUCK, initTask, initResult);
        assignClubMembers(initTask, initResult);

//        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_LOGGING;
    }

    private void createClubGroups() throws Exception {
        groupPokerClub = RESOURCE_CLUBS.controller.addGroup(CLUB_POKER_NAME);
        groupPokerClub.addAttributeValue(ATTR_DISPLAY_NAME, CLUB_POKER_DISPLAY_NAME);

        groupChessClub = RESOURCE_CLUBS.controller.addGroup(CLUB_CHESS_NAME);
        groupChessClub.addAttributeValue(ATTR_DISPLAY_NAME, CLUB_CHESS_DISPLAY_NAME);
    }

    private void reconcileClubs(OperationResult result) throws CommonException {
        Task task = rerunTask(TASK_RECONCILE_CLUBS.oid, result);
        assertSuccess(task.getResult());
    }

    private void assignClubMembers(Task initTask, OperationResult initResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        // We need to know clubs OIDs, so let's do assertions here.
        pokerClubOid = assertOrgByName(CLUB_POKER_NAME, "after init")
                .display()
                .assertDisplayName(CLUB_POKER_DISPLAY_NAME)
                .getOid();
        chessClubOid = assertOrgByName(CLUB_CHESS_NAME, "after init")
                .display()
                .assertDisplayName(CLUB_CHESS_DISPLAY_NAME)
                .getOid();

        assignOrg(USER_ALICE.oid, pokerClubOid, initTask, initResult);
        assignOrg(USER_BOB.oid, chessClubOid, initTask, initResult);
        assignOrg(USER_CHUCK.oid, pokerClubOid, initTask, initResult);
        assignOrg(USER_CHUCK.oid, chessClubOid, initTask, initResult);
    }

    private void createUsers(String namePattern, int count, Task task, OperationResult result, TestObject<?>... targets)
            throws CommonException {
        for (int i = 0; i < count; i++) {
            UserType user = new UserType()
                    .name(String.format(namePattern, i));
            for (TestObject<?> target : targets) {
                user.getAssignment().add(ObjectTypeUtil.createAssignmentTo(target.get(), SchemaConstants.ORG_DEFAULT));
            }
            addObject(user, task, result);
        }
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test000Sanity() throws Exception {
        assertUserByUsername("user-dcs-0000", "after init")
                .display()
                .assertCostCenter("07210");
        assertUserByUsername("user-cc-0000", "after init")
                .display()
                .assertCostCenter("07330");

        // clubs were already asserted

        assertUser(USER_ALICE.oid, "after init")
                .display()
                .assertOrganizations("poker (Poker Club)");
        assertUser(USER_BOB.oid, "after init")
                .display()
                .assertOrganizations("chess (Chess Club)");
        assertUser(USER_CHUCK.oid, "after init")
                .display()
                .assertOrganizations("poker (Poker Club)", "chess (Chess Club)");
    }

    /**
     * Changing DCS cost center. Member recomputation (directly in iterative task) is triggered by default.
     */
    @Test
    public void test100ChangeCostCenter() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        ObjectDelta<OrgType> delta = deltaFor(OrgType.class)
                .item(OrgType.F_COST_CENTER).replace("07999")
                .asObjectDelta(ORG_DCS.oid);
        ModelExecuteOptions options = ModelExecuteOptions.create()
                .setExtensionPropertyRealValues(prismContext, MEMBER_RECOMPUTATION_WORKER_THREADS_NAME, 3);
        executeChanges(delta, options, task, result);

        then();
        assertSuccess(result);

        String taskOid = result.findTaskOid();
        assertThat(taskOid).as("background task OID").isNotNull();

        Task recomputeTask = waitForTaskFinish(taskOid);
        assertTask(recomputeTask, "recompute task after")
                .display()
                .assertSuccess()
                .assertArchetypeRef(SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value());
        assertThat(recomputeTask.getRootActivityDefinitionOrClone().getDistribution().getWorkerThreads())
                .as("worker threads").isEqualTo(3);

        assertUserAfterByUsername("user-dcs-0000")
                .assertCostCenter("07999");
        assertUserAfterByUsername("user-cc-0000")
                .assertCostCenter("07330");
    }

    /**
     * Changing DCS cost center but with recompute turned off.
     */
    @Test
    public void test110ChangeCostCenterNoRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        ObjectDelta<OrgType> delta = deltaFor(OrgType.class)
                .item(OrgType.F_COST_CENTER).replace("07777")
                .asObjectDelta(ORG_DCS.oid);

        executeChanges(delta, doNotRecompute(), task, result);

        String taskOid = result.findTaskOid();
        assertThat(taskOid).as("background task OID").isNull();

        then();
        assertSuccess(result);
        assertUserAfterByUsername("user-dcs-0000")
                .assertCostCenter("07999");
        assertUserAfterByUsername("user-cc-0000")
                .assertCostCenter("07330");
    }

    /**
     * Changing chess club but with no explicit options (default is "no recompute").
     */
    @Test
    public void test200ChangeChessClubNoRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        ObjectDelta<OrgType> delta = deltaFor(OrgType.class)
                .item(OrgType.F_DISPLAY_NAME).replace(PolyString.fromOrig("Black Knight Chess Club"))
                .asObjectDelta(chessClubOid);
        // The default is "do not recompute" for clubs.
        executeChanges(delta, null, task, result);

        then();

        assertSuccess(result);

        String taskOid = result.findTaskOid();
        assertThat(taskOid).as("background task OID").isNull();

        assertNoObjectByName(TaskType.class, TASK_TRIGGER_CLUB_MEMBERS_RECOMPUTATION_NAME, task, result);

        assertOrgAfter(chessClubOid)
                .assertDisplayName("Black Knight Chess Club");
        assertUserAfter(USER_BOB.oid)
                .assertOrganizations("chess (Chess Club)");
        assertUserAfter(USER_CHUCK.oid)
                .assertOrganizations("poker (Poker Club)", "chess (Chess Club)");
    }

    /**
     * Changing chess clubs via reconciliation. Execute options provided in sync reaction ensure that members
     * are recomputed (using delayed triggers, as is the most appropriate for such use case).
     */
    @Test
    public void test210ChangeClubsViaReconciliation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        groupPokerClub.replaceAttributeValue(ATTR_DISPLAY_NAME, "New Poker Club");
        groupChessClub.replaceAttributeValue(ATTR_DISPLAY_NAME, "Levice Chess Club");

        when();
        reconcileClubs(result);

        List<PrismObject<TaskType>> triggeringTasks = assertObjectByName(TaskType.class,
                TASK_TRIGGER_CLUB_MEMBERS_RECOMPUTATION_NAME, task, result);

        // Two changed clubs, two tasks.
        assertThat(triggeringTasks.size()).as("# of triggering tasks").isEqualTo(2);
        for (PrismObject<TaskType> triggeringTask : triggeringTasks) {
            assertTask(triggeringTask.asObjectable(), "triggering task")
                    .display()
                    .assertName(TASK_TRIGGER_CLUB_MEMBERS_RECOMPUTATION_NAME)
                    .assertArchetypeRef(SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value());
            assertThat(triggeringTask.asObjectable().getActivity().getDistribution().getWorkerThreads())
                    .as("worker threads").isEqualTo(4); // see resource-clubs.xml

            waitForTaskFinish(triggeringTask.getOid());
        }

        then();
        assertSuccess(result);
        assertOrgAfter(pokerClubOid)
                .assertDisplayName("New Poker Club");
        assertOrgAfter(chessClubOid)
                .assertDisplayName("Levice Chess Club");
        assertUserAfter(USER_ALICE.oid)
                .triggers()
                .assertTriggers(1);
        assertUserAfter(USER_BOB.oid)
                .triggers()
                .assertTriggers(1);

        // The following assertion on the number of triggers can occasionally fail because of the asynchronous nature
        // of the whole process:
        //
        // - clubs reconciliation task creates recompute triggers on affected users,
        // - these triggers are created using optimizing trigger creator, hence there should be at most one
        //   trigger per any particular object,
        // - unfortunately, trigger creation itself is asynchronous (even multithreaded, but that's probably not
        //   the main problem) - to not slow down the main processing,
        // - hence, it can occur that more triggers creation are requested for a single object "at the same time",
        //   optimizing trigger creator is out of luck here; two triggers will be created.
        //
        // We should adapt the test somehow. Either we should remove asynchronicity, or we should accept that
        // there can be sometimes 2 triggers.
        //
        // See MID-10299.
        assertUserAfter(USER_CHUCK.oid)
                .triggers()
                .assertTriggers(1); // due to optimization

        try {
            clock.overrideDuration("PT1M"); // this is the delay set for trigger creator
            Task triggerScanner = runTriggerScannerOnDemand(result);
            assertTask(triggerScanner, "trigger scanner after")
                    .display()
                    .assertSuccess()
                    .rootItemProcessingInformation()
                    .assertSuccessCount(3);
        } finally {
            clock.resetOverride();
        }

        assertUser(USER_ALICE.oid, "after trigger processed")
                .display()
                .triggers()
                .assertNone()
                .end()
                .assertOrganizations("poker (New Poker Club)");
        assertUser(USER_BOB.oid, "after trigger processed")
                .display()
                .triggers()
                .assertNone()
                .end()
                .assertOrganizations("chess (Levice Chess Club)");
        assertUser(USER_CHUCK.oid, "after trigger processed")
                .display()
                .triggers()
                .assertNone()
                .end()
                .assertOrganizations("poker (New Poker Club)", "chess (Levice Chess Club)");
    }

    @NotNull
    private ModelExecuteOptions doNotRecompute() throws SchemaException {
        return ModelExecuteOptions.create()
                .setExtensionPropertyRealValues(prismContext, RECOMPUTE_MEMBERS_NAME, false);
    }
}
