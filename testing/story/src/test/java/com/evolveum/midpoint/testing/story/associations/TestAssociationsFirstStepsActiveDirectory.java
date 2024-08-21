/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.associations;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_UNMANAGED;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;
import static com.evolveum.midpoint.test.asserter.predicates.ReferenceAssertionPredicates.references;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ENTITLEMENT;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Here we check comprehensive "First steps for associations" scenario.
 *
 * The test methods here are chained: they depend on each other.
 *
 * General idea:
 *
 * . TODO
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssociationsFirstStepsActiveDirectory extends AbstractStoryTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "associations-first-steps-ad");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final String INTENT_DEFAULT = "default";
    private static final String INTENT_GROUP = "group";

    private static final String RESOURCE_AD_OID = "11adacf4-6e5a-4775-931b-8af8cd4843f7";
    private static final int INITIAL_AD_ACCOUNTS = 1;
    private static final int INITIAL_AD_GROUPS = 2;
    private static final int EXTRA_ROLES = 1;

    private static final String JACK = "jack";
    private static final String JIM = "jim";
    private static final String ALICE = "alice";

    private static final String ADMINISTRATORS = "administrators";
    private static final String TESTERS = "testers";
    private static final String OPERATORS = "operators";

    private static final ItemName RI_GROUP = ItemName.from(NS_RI, "group");

    /**
     * This object is initialized each time the dummy resource is initialized.
     * As the resource definition evolves, this happens in multiple test methods.
     */
    private static DummyAdScenario adScenario;

    private static final TestObject<ArchetypeType> ARCHETYPE_GROUP = TestObject.file(
            TEST_DIR, "archetype-group.xml", "022a6a2d-b66a-44b6-9b27-6c9f0bf058c8");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_USER = TestObject.file(
            TEST_DIR, "object-template-user.xml", "d6f7dabf-60d6-4707-87ed-5d48483dd18c");

    private static final DummyTestResource RESOURCE_AD_100 = createAdResource("resource-ad-100.xml");
    private static final DummyTestResource RESOURCE_AD_120 = createAdResource("resource-ad-120.xml");
    private static final DummyTestResource RESOURCE_AD_130 = createAdResource("resource-ad-130.xml");
    private static final DummyTestResource RESOURCE_AD_140 = createAdResource("resource-ad-140.xml");
    private static final DummyTestResource RESOURCE_AD_150 = createAdResource("resource-ad-150.xml");
    private static final DummyTestResource RESOURCE_AD_300 = createAdResource("resource-ad-300.xml");

    private String administratorsRoleOid;
    private String testersRoleOid;
    private String operatorsRoleOid;

    private String administratorsShadowOid;
    private String testersShadowOid;
    private String operatorsShadowOid;

    @Override
    protected boolean requiresNativeRepository() {
        return true;
    }

    private static DummyTestResource createAdResource(String fileName) {
        return new DummyTestResource(TEST_DIR, fileName, RESOURCE_AD_OID, "ad",
                        c -> adScenario = DummyAdScenario.on(c).initialize());
    }

    private static void initializeAdObjects() {
        try {
            assertThat(adScenario).isNotNull();
            DummyObject jack = adScenario.account.add(JACK);
            DummyObject administrators = adScenario.group.add(ADMINISTRATORS);
            adScenario.group.add(TESTERS);
            adScenario.accountGroup.add(jack, administrators);
            // no testers yet
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                ARCHETYPE_GROUP, OBJECT_TEMPLATE_USER);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * The resource definition contains only accounts and groups, no association types.
     *
     * Here we check that accounts can be retrieved.
     */
    @Test
    public void test100ListingAccounts() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("first definition is imported and tested");
        initTestObjects(task, result, RESOURCE_AD_100);
        initializeAdObjects();

        when("accounts are retrieved");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_AD_100.get())
                        .queryFor(ACCOUNT, INTENT_DEFAULT)
                        .build(),
                null, task, result);

        then("there are all accounts");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(INITIAL_AD_ACCOUNTS);

        // check that reference attributes are there
    }

    /**
     * Here we check that accounts and groups can be imported.
     *
     * Resource definition is unchanged (still no associations).
     */
    @Test
    public void test110ImportingObjects() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        when("accounts are imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_AD_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
                .withProcessingAllAccounts()
                .execute(result);

        then("the users are there");
        assertUsers(INITIAL_AD_ACCOUNTS + 1);
        assertUserAfterByUsername(JACK); // TODO some assertions

        importGroups(0, result);
    }

    /** This is a separate method to be ad-hoc callable from other tests (if needed). */
    private void importGroups(int expectedNew, OperationResult result) throws CommonException, IOException {
        when("groups are imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_AD_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ENTITLEMENT, INTENT_GROUP))
                .withProcessingAllAccounts()
                .execute(result);

        then("the groups are there");
        assertRoles(INITIAL_AD_GROUPS + EXTRA_ROLES + expectedNew);

        var administratorsAsserter = assertRoleAfterByName(ADMINISTRATORS);
        administratorsAsserter
                .assertHasArchetype(ARCHETYPE_GROUP.oid);
        administratorsRoleOid = administratorsAsserter.getOid();
        administratorsShadowOid = administratorsAsserter.links().singleLive().getOid();

        var testersAsserter = assertRoleAfterByName(TESTERS);
        testersAsserter
                .assertHasArchetype(ARCHETYPE_GROUP.oid);
        testersRoleOid = testersAsserter.getOid();
        testersShadowOid = testersAsserter.links().singleLive().getOid();

        // TODO more assertions
    }

    /**
     * We define the association, with a simple inbound mapping (`group` -> `targetRef`); but no synchronization yet.
     *
     * Then we import an account in simulation mode. There should be no processing.
     */
    @Test
    public void test120SimpleInboundPreview() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportAdResource(RESOURCE_AD_120, task, result);

        when("account is reimported (simulation)");
        var simResult = importJackSimulated(task, result);

        then("the result is 'unchanged' (no synchronization options)");
        assertProcessedObjectsAfter(simResult)
                .by().objectType(UserType.class).find().assertState(ObjectProcessingStateType.UNMODIFIED).end()
                .by().objectType(ShadowType.class).find().assertState(ObjectProcessingStateType.UNMODIFIED).end();
    }

    /**
     * We add a synchronization reaction for `unmatched` value.
     *
     * Then we import an account in simulation mode. An assignment addition should be seen in the simulation result.
     */
    @Test
    public void test130SimpleInboundPreviewWithSyncReaction() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportAdResource(RESOURCE_AD_130, task, result);

        when("account is reimported (simulation)");
        var simResult = importJackSimulated(task, result);

        then("the assignment is added to the user (simulation)");
        // @formatter:off
        assertProcessedObjectsAfter(simResult)
                .by().objectType(UserType.class).find()
                    .assertState(ObjectProcessingStateType.MODIFIED)
                    .assertEventMarks(
                            CommonInitialObjects.MARK_FOCUS_ASSIGNMENT_CHANGED,
                            CommonInitialObjects.MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED)
                    .delta()
                        .assertModify()
                        .container(UserType.F_ASSIGNMENT)
                            .valuesToAdd()
                                .single()
                                    .assertItemValueSatisfies(
                                            AssignmentType.F_TARGET_REF, references(administratorsRoleOid, RoleType.COMPLEX_TYPE))
                                .end()
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).find()
                    .assertState(ObjectProcessingStateType.UNMODIFIED)
                .end();
        // @formatter:on
    }

    /**
     * The association type definition is switched from `proposed` to `active` state.
     *
     * Importing an account in real mode.
     */
    @Test
    public void test140SimpleInboundImport() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportAdResource(RESOURCE_AD_140, task, result);

        when("account is reimported (real)");
        importJackReal(result);

        then("the assignment is added to the user (real)");
        assertUserAfterByUsername(JACK)
                .assignments()
                .single()
                .assertTargetOid(administratorsRoleOid)
                .assertTargetType(RoleType.COMPLEX_TYPE)
                .assertTargetRelationMatches(ORG_DEFAULT);
    }

    /**
     * Correlation on `targetRef` is added. The `matched` synchronization reaction is added as well.
     *
     * Importing an account in real mode.
     */
    @Test
    public void test150AddingSynchronizationReactionAndReimporting() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportAdResource(RESOURCE_AD_150, task, result);

        when("account is reimported (real)");
        importJackReal(result);

        then("there is still a single assignment");
        assertUserAfterByUsername(JACK)
                .assignments()
                .single()
                .assertTargetOid(administratorsRoleOid)
                .assertTargetType(RoleType.COMPLEX_TYPE)
                .assertTargetRelationMatches(ORG_DEFAULT);
    }

    /**
     * Adding membership of `testers` and re-importing the account.
     *
     * (The resource definition is unchanged.)
     */
    @Test
    public void test160AddingTestersMembershipAndReimporting() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        given("added membership to other group");
        var jack = adScenario.account.getByNameRequired(JACK);
        var testers = adScenario.group.getByNameRequired(TESTERS);
        adScenario.accountGroup.add(jack, testers);

        when("account is reimported (real)");
        importJackReal(result);

        then("there are two assignments");
        assertUserAfterByUsername(JACK)
                .assignments()
                .assertAssignments(2)
                .by().targetOid(administratorsRoleOid).targetType(RoleType.COMPLEX_TYPE).find().end()
                .by().targetOid(testersRoleOid).targetType(RoleType.COMPLEX_TYPE).find().end();
    }

    /**
     * Now we start with outbound mappings.
     *
     * We assume that all existing memberships are imported into midPoint as roles.
     * Currently, this includes `administrators` and `testers`.
     * There are no other groups yet.
     *
     * We will start with outbounds.
     * At the same time, we must be aware that the resource is still the authoritative source for the membership of most groups.
     * In particular, for these we've not seen yet.
     *
     * Hence, we'll mark each new group as `tolerated`, which will mean that we won't remove its membership from accounts
     * for which there is no assignment for that group yet.
     *
     * NOTE: We still create assignments by inbounds. So, for some operations (namely, the import/reconciliation) the assignments
     * will be created before the outbounds are processed. But not for others.
     *
     * Changes in the resource definition:
     *
     * . added outbound mappings for the association
     * . added marking of the new groups as `tolerated`, and switched default tolerance to `false`
     *
     * Actions in this test:
     *
     * . creating new group `operators` right on the resource, and importing the groups (it should get the `tolerated` mark)
     * . adding `jack` to `operators` (on the resource)
     * . remove `jack` from `testers`, to induce a change in the account
     * . the membership of `operators` should be kept intact, and an assignment should be created in midPoint
     */
    @Test
    public void test300ModifyJackWithToleratedEntitlement() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportAdResource(RESOURCE_AD_300, task, result);

        and("'operators' group is added to the resource, and groups are imported");
        var operators = adScenario.group.add(OPERATORS);
        importGroups(1, result);

        var operatorsAsserter = assertRoleAfterByName(OPERATORS);
        operatorsAsserter
                .assertHasArchetype(ARCHETYPE_GROUP.oid);
        operatorsRoleOid = operatorsAsserter.getOid();
        operatorsShadowOid = operatorsAsserter
                .links()
                .singleLive()
                .resolveTarget()
                .display()
                .assertEffectiveMark(MARK_UNMANAGED.oid)
                .getOid();

        and("jack is added to 'operators' manually on the resource");
        adScenario.accountGroup.add(
                adScenario.account.getByNameRequired(JACK),
                operators);

        when("jack is removed from 'testers' by unassigning the role in midPoint");
        var jackAsserter = assertUserBeforeByUsername(JACK);
        var testersAssignment = jackAsserter
                .assignments()
                .forRole(testersRoleOid)
                .getAssignment();
        var jackOid = jackAsserter.getOid();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(testersAssignment.clone())
                        .asObjectDelta(jackOid),
                null, task, result);

        then("jack is no longer in 'testers' but in 'operators'");
        assertUserAfterByUsername(JACK)
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .associations()
                .association(RI_GROUP)
                .assertShadowOids(administratorsShadowOid, operatorsShadowOid);

        and("all is OK");
        assertSuccess(result);
    }

    /** Provisioning user `jim` with membership of `testers` (simulated). */
    @Test
    public void test310ProvisionJimWithOutboundsSimulated() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        and("user is created");
        var jim = new UserType()
                .name(JIM)
                .assignment(new AssignmentType()
                        .construction(RESOURCE_AD_300.construction(ACCOUNT, INTENT_DEFAULT)));
        addObject(jim, task, result);
        assertUserBeforeByUsername(JIM)
                .singleLink();
        displayDumpable("account before", adScenario.account.getByNameRequired(JIM));

        when("assignment providing membership of testers (simulation)");
        var simResult = executeWithSimulationResult(
                List.of(createAssignmentDelta(jim.getOid(), testersRoleOid)),
                task, result);

        then("all is OK");
        assertProcessedObjects(simResult, "after")
                .display()
                .by().objectType(ShadowType.class).find()
                .assertState(ObjectProcessingStateType.MODIFIED)
                .delta()
                .assertModified(ShadowType.F_ASSOCIATIONS.append(RI_GROUP));

        when("assignment providing membership of testers (preview changes)");
        previewChanges(
                createAssignmentDelta(jim.getOid(), testersRoleOid),
                null, task, result);

        then("all is OK");
        assertSuccess(result);
    }

    /** Provisioning user `jim` with membership of `testers` (real). */
    @Test
    public void test320ProvisionJimWithOutboundsReal() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        and("user 'jim' existing from the previous test");

        var jimOid = assertUserBeforeByUsername(JIM)
                .singleLink()
                .end()
                .getOid();

        displayDumpable("account before", adScenario.account.getByNameRequired(JIM));

        when("assignment to testers is created (real)");
        executeChanges(
                createAssignmentDelta(jimOid, testersRoleOid),
                null, task, result);

        then("all is OK");
        assertUserAfterByUsername(JIM)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertAssignments(2)
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .assertSize(1)
                .assertValuesCount(1);

        displayDumpable("account after", adScenario.account.getByNameRequired(JIM));

        when("assignment providing membership of administrators is created (real)");
        executeChanges(
                createAssignmentDelta(jimOid, administratorsRoleOid),
                null, task, result);

        then("all is OK");
        var newAssignment = assertUserAfterByUsername(JIM)
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .assertSize(1)
                .assertValuesCount(2)
                .end()
                .end()
                .end()
                .assignments()
                .assertAssignments(3)
                .forRole(administratorsRoleOid)
                .getAssignment();

        displayDumpable("account after", adScenario.account.getByNameRequired(JIM));

        when("assignment providing membership of administrators is deleted (real)");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(newAssignment.clone())
                        .asObjectDelta(jimOid),
                null, task, result);

        then("all is OK");
        displayDumpable("account after", adScenario.account.getByNameRequired(JIM));
        assertUserAfterByUsername(JIM)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertAssignments(2)
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .assertSize(1)
                .assertValuesCount(1);
    }

    @Test
    public void test350ProvisionNewAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        when("user with membership of testers created");
        var alice = new UserType()
                .name(ALICE)
                .assignment(new AssignmentType()
                        .construction(RESOURCE_AD_300.construction(ACCOUNT, INTENT_DEFAULT)))
                .assignment(new AssignmentType()
                        .targetRef(testersRoleOid, RoleType.COMPLEX_TYPE));
        addObject(alice, task, result);

        then("all is OK");
        assertUserAfterByUsername(ALICE)
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .assertSize(1)
                .assertValuesCount(1);

        displayDumpable("account after", adScenario.account.getByNameRequired(ALICE));

        when("recomputing alice (simulated)");
        var simResult = executeWithSimulationResult(
                TaskExecutionMode.SIMULATED_PRODUCTION,
                null,
                task, result,
                simulationResult -> recomputeUser(alice.getOid(), task, result));
        assertProcessedObjectsAfter(simResult)
                .by().objectType(UserType.class).find().assertState(ObjectProcessingStateType.UNMODIFIED).end()
                .by().objectType(ShadowType.class).find().assertState(ObjectProcessingStateType.UNMODIFIED).end();
    }

    @Test
    public void test360AddNewAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        var userName = getTestNameShort();

        given("reusing resource definition from previous test");

        when("user with 'testers' assignments created");
        var user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .targetRef(testersRoleOid, RoleType.COMPLEX_TYPE));
        addObject(user, task, result);

        when("assignment providing membership of 'administrators' (simulation)");
        var simResult = executeWithSimulationResult(
                List.of(createAssignmentDelta(user.getOid(), administratorsRoleOid)),
                task, result);

        then("all is OK");
        assertProcessedObjects(simResult, "after")
                .display()
                .by().objectType(ShadowType.class).find()
                .assertState(ObjectProcessingStateType.MODIFIED)
                .delta()
                .assertModified(ShadowType.F_ASSOCIATIONS.append(RI_GROUP));

        when("assignment providing membership of 'administrators' (preview changes)");
        previewChanges(
                createAssignmentDelta(user.getOid(), administratorsRoleOid),
                null, task, result);

        then("all is OK");
        assertSuccess(result);
    }

    private void reimportAdResource(DummyTestResource resource, Task task, OperationResult result) throws Exception {
        deleteObject(ResourceType.class, RESOURCE_AD_OID, task, result);
        initTestObjects(task, result, resource);
    }

    private TestSimulationResult importJackSimulated(Task task, OperationResult result) throws Exception {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_AD_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
                .withNameValue(JACK)
                .simulatedDevelopment()
                .withTracing()
                .executeOnForegroundSimulated(null, task, result);
    }

    private void importJackReal(OperationResult result) throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_AD_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
                .withNameValue(JACK)
                .executeOnForeground(result);
    }

    private ObjectDelta<ObjectType> createAssignmentDelta(String userOid, String roleOid) throws SchemaException {
        return deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .targetRef(roleOid, RoleType.COMPLEX_TYPE))
                .asObjectDelta(userOid);
    }
}
