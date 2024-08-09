/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.associations;

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
import org.testng.annotations.BeforeMethod;
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

    private static final ItemName RI_GROUP = ItemName.from(NS_RI, "group");

    /** Initialized for each test anew (when the specific resource is initialized). */
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

    private String administratorsOid;
    private String testersOid;

    @BeforeMethod
    public void onNativeOnly() {
        skipClassIfNotNativeRepository();
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
     * Resource definition: only accounts and groups, no association types.
     *
     * Checks that accounts can be retrieved.
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
     * Resource definition: same as above.
     *
     * Checks that accounts and groups can be imported.
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

        importGroups(result);
    }

    /** Extra method to be ad-hoc callable from the outside. */
    private void importGroups(OperationResult result) throws CommonException, IOException {
        when("groups are imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_AD_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ENTITLEMENT, INTENT_GROUP))
                .withProcessingAllAccounts()
                .execute(result);

        then("the groups are there");
        assertRoles(INITIAL_AD_GROUPS + EXTRA_ROLES);
        administratorsOid = assertRoleAfterByName(ADMINISTRATORS)
                .assertHasArchetype(ARCHETYPE_GROUP.oid)
                .getOid();
        testersOid = assertRoleAfterByName(TESTERS)
                .assertHasArchetype(ARCHETYPE_GROUP.oid)
                .getOid();
        // TODO more assertions
    }

    /**
     * Resource definition: simple inbound mapping for the association (group -> targetRef); no synchronization.
     *
     * Importing an account in simulation mode.
     */
    @Test
    public void test120SimpleInboundPreview() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_AD_120, task, result);

        when("account is reimported (simulation)");
        var simResult = importJackSimulated(task, result);

        then("the result is 'unchanged' (no synchronization options)");
        assertProcessedObjectsAfter(simResult)
                .by().objectType(UserType.class).find().assertState(ObjectProcessingStateType.UNMODIFIED).end()
                .by().objectType(ShadowType.class).find().assertState(ObjectProcessingStateType.UNMODIFIED).end();
    }

    /**
     * Resource definition: added synchronization reaction for unmatched value.
     *
     * Importing an account in simulation mode.
     */
    @Test
    public void test130SimpleInboundPreviewWithSyncReaction() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_AD_130, task, result);

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
                                            AssignmentType.F_TARGET_REF, references(administratorsOid, RoleType.COMPLEX_TYPE))
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
     * Resource definition: same as above, with lifecycle state set to default.
     *
     * Importing an account in real mode.
     */
    @Test
    public void test140SimpleInboundImport() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_AD_140, task, result);

        when("account is reimported (real)");
        importJackReal(result);

        then("the assignment is added to the user (real)");
        assertUserAfterByUsername(JACK)
                .assignments()
                .single()
                .assertTargetOid(administratorsOid)
                .assertTargetType(RoleType.COMPLEX_TYPE)
                .assertTargetRelationMatches(ORG_DEFAULT);
    }

    /**
     * Resource definition: Added correlation on `targetRef` and `matched` synchronization reaction.
     *
     * Importing an account in real mode.
     */
    @Test
    public void test150AddingSynchronizationReactionAndReimporting() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_AD_150, task, result);

        when("account is reimported (real)");
        importJackReal(result);

        then("there is still a single assignment");
        assertUserAfterByUsername(JACK)
                .assignments()
                .single()
                .assertTargetOid(administratorsOid)
                .assertTargetType(RoleType.COMPLEX_TYPE)
                .assertTargetRelationMatches(ORG_DEFAULT);
    }

    /**
     * Resource definition: same as above.
     *
     * Adding membership of testers and re-importing the account.
     */
    @Test
    public void test160AddingAccessRightAndReimporting() throws Exception {
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
                .by().targetOid(administratorsOid).targetType(RoleType.COMPLEX_TYPE).find().end()
                .by().targetOid(testersOid).targetType(RoleType.COMPLEX_TYPE).find().end();
    }

    /**
     * Resource definition: added outbounds.
     *
     * Provisioning user `jim` with membership of `testers` (simulated).
     */
    @Test
    public void test300ProvisionJimWithOutboundsSimulated() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_AD_300, task, result);

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
                List.of(createAssignmentDelta(jim.getOid(), testersOid)),
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
                createAssignmentDelta(jim.getOid(), testersOid),
                null, task, result);

        then("all is OK");
        assertSuccess(result);
    }

    /**
     * Resource definition: same as above.
     *
     * Provisioning user `jim` with membership of `testers` (real).
     */
    @Test
    public void test310ProvisionJimWithOutboundsReal() throws Exception {
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
                createAssignmentDelta(jimOid, testersOid),
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
                createAssignmentDelta(jimOid, administratorsOid),
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
                .forRole(administratorsOid)
                .getAssignment();

        displayDumpable("account after", adScenario.account.getByNameRequired(JIM));

        when("assignment providing membership of administrators is deleted (real)");
        traced(() -> executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(newAssignment.clone())
                        .asObjectDelta(jimOid),
                null, task, result));

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
                        .targetRef(testersOid, RoleType.COMPLEX_TYPE));
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
                        .targetRef(testersOid, RoleType.COMPLEX_TYPE));
        addObject(user, task, result);

        when("assignment providing membership of 'administrators' (simulation)");
        var simResult = executeWithSimulationResult(
                List.of(createAssignmentDelta(user.getOid(), administratorsOid)),
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
                createAssignmentDelta(user.getOid(), administratorsOid),
                null, task, result);

        then("all is OK");
        assertSuccess(result);
    }

    private void reimportDmsResource(DummyTestResource resource, Task task, OperationResult result) throws Exception {
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
