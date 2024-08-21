/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.associations;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.asserter.predicates.ReferenceAssertionPredicates.references;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.GENERIC;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.util.exception.CommonException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
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
public class TestAssociationsFirstSteps extends AbstractStoryTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "associations-first-steps");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final String NS_DMS = "http://midpoint.evolveum.com/xml/ns/samples/dms";

    private static final String INTENT_DEFAULT = "default";
    private static final String INTENT_DOCUMENT = "document";

    private static final String LEVEL_READ = "read";
    private static final String LEVEL_WRITE = "write";
    private static final String LEVEL_ADMIN = "admin";
    private static final QName RELATION_READ = new QName(NS_DMS, LEVEL_READ);
    private static final QName RELATION_WRITE = new QName(NS_DMS, LEVEL_WRITE);
    private static final QName RELATION_ADMIN = new QName(NS_DMS, LEVEL_ADMIN);

    private static final String RESOURCE_DMS_OID = "edbd434a-5532-4030-96da-b584d2942d8c";
    private static final int INITIAL_DMS_ACCOUNTS = 1;
    private static final int INITIAL_DMS_DOCUMENTS = 1;

    private static final String JACK = "jack";
    private static final String JIM = "jim";
    private static final String ALICE = "alice";
    private static final String GUIDE = "guide";

    /** Initialized for each test anew (when the specific resource is initialized). */
    private static DummyDmsScenario dmsScenario;

    private static final TestObject<ArchetypeType> ARCHETYPE_DOCUMENT = TestObject.file(
            TEST_DIR, "archetype-document.xml", "8c0b32f9-fadc-42cb-bc05-878ecfe001e8");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_USER = TestObject.file(
            TEST_DIR, "object-template-user.xml", "1b69e78f-0954-49cb-92a0-ed9b6476a807");

    private static final DummyTestResource RESOURCE_DMS_100 = createDmsResource("resource-dms-100.xml");
    private static final DummyTestResource RESOURCE_DMS_120 = createDmsResource("resource-dms-120.xml");
    private static final DummyTestResource RESOURCE_DMS_130 = createDmsResource("resource-dms-130.xml");
    private static final DummyTestResource RESOURCE_DMS_140 = createDmsResource("resource-dms-140.xml");
    private static final DummyTestResource RESOURCE_DMS_150 = createDmsResource("resource-dms-150.xml");
    private static final DummyTestResource RESOURCE_DMS_170 = createDmsResource("resource-dms-170.xml");
    private static final DummyTestResource RESOURCE_DMS_300 = createDmsResource("resource-dms-300.xml");

    private String guideOid;

    @Override
    protected boolean requiresNativeRepository() {
        return true;
    }

    private static DummyTestResource createDmsResource(String fileName) {
        return new DummyTestResource(TEST_DIR, fileName, RESOURCE_DMS_OID, "dms",
                        c -> dmsScenario = DummyDmsScenario.on(c).initialize());
    }

    private static void initializeDmsObjects() {
        try {
            assertThat(dmsScenario).isNotNull();
            DummyObject jack = dmsScenario.account.add(JACK);
            DummyObject guide = dmsScenario.document.add(GUIDE);
            DummyObject jackCanReadGuide = dmsScenario.access.add("jack-can-read-guide");
            jackCanReadGuide.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_READ);

            dmsScenario.accountAccess.add(jack, jackCanReadGuide);
            dmsScenario.accessDocument.add(jackCanReadGuide, guide);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                ARCHETYPE_DOCUMENT, OBJECT_TEMPLATE_USER);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Resource definition: only accounts and documents, no association types.
     *
     * Checks that accounts can be retrieved.
     */
    @Test
    public void test100ListingAccounts() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("first definition is imported and tested");
        initTestObjects(task, result, RESOURCE_DMS_100);
        initializeDmsObjects();

        when("accounts are retrieved");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DMS_100.get())
                        .queryFor(ACCOUNT, INTENT_DEFAULT)
                        .build(),
                null, task, result);

        then("there are all accounts");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(INITIAL_DMS_ACCOUNTS);

        // check that reference attributes are there
    }

    /**
     * Resource definition: same as above.
     *
     * Checks that accounts and documents can be imported.
     */
    @Test
    public void test110ImportingObjects() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        when("accounts are imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DMS_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
                .withProcessingAllAccounts()
                .execute(result);

        then("the users are there");
        assertUsers(INITIAL_DMS_ACCOUNTS + 1);
        assertUserAfterByUsername(JACK); // TODO some assertions

        importDocuments(result);
    }

    /** Extra method to be ad-hoc callable from the outside. */
    private void importDocuments(OperationResult result) throws CommonException, IOException {
        when("documents are imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DMS_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(GENERIC, INTENT_DOCUMENT))
                .withProcessingAllAccounts()
                .execute(result);

        then("the documents are there");
        assertServices(INITIAL_DMS_DOCUMENTS);
        guideOid = assertServiceAfterByName(GUIDE)
                .assertHasArchetype(ARCHETYPE_DOCUMENT.oid)
                .getOid();
        // TODO more assertions
    }

    /**
     * Resource definition: simple inbound mapping for the association (document -> targetRef); no synchronization.
     *
     * Importing an account in simulation mode.
     */
    @Test
    public void test120SimpleInboundPreview() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_DMS_120, task, result);

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
        reimportDmsResource(RESOURCE_DMS_130, task, result);

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
                                            AssignmentType.F_TARGET_REF, references(guideOid, ServiceType.COMPLEX_TYPE))
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
        reimportDmsResource(RESOURCE_DMS_140, task, result);

        when("account is reimported (real)");
        importJackReal(result);

        then("the assignment is added to the user (real)");
        assertUserAfterByUsername(JACK)
                .assignments()
                .single()
                .assertTargetOid(guideOid)
                .assertTargetType(ServiceType.COMPLEX_TYPE)
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
        reimportDmsResource(RESOURCE_DMS_150, task, result);

        when("account is reimported (real)");
        importJackReal(result);

        then("there is still a single assignment");
        assertUserAfterByUsername(JACK)
                .assignments()
                .single()
                .assertTargetOid(guideOid)
                .assertTargetType(ServiceType.COMPLEX_TYPE)
                .assertTargetRelationMatches(ORG_DEFAULT);
    }

    /**
     * Resource definition: same as above.
     *
     * Adding second access right to the same document and re-importing the account.
     */
    @Test
    public void test160AddingAccessRightAndReimporting() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        given("added second access right to the same document");
        var jack = dmsScenario.account.getByNameRequired(JACK);
        var guide = dmsScenario.document.getByNameRequired(GUIDE);
        var jackCanWriteGuide = dmsScenario.access.add("jack-can-write-guide");
        jackCanWriteGuide.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_WRITE);
        dmsScenario.accountAccess.add(jack, jackCanWriteGuide);
        dmsScenario.accessDocument.add(jackCanWriteGuide, guide);

        when("account is reimported (real)");
        importJackReal(result);

        then("there is still only one assignment (shared by both accesses)");
        assertUserAfterByUsername(JACK)
                .assignments()
                .single()
                .assertTargetOid(guideOid)
                .assertTargetType(ServiceType.COMPLEX_TYPE)
                .assertTargetRelationMatches(ORG_DEFAULT);
    }

    /**
     * Resource definition: added `level` to `relation` mapping.
     *
     * Reimporting the jack (simulated).
     */
    @Test
    public void test170AddingLevelMappingAndReimportingSimulated() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_DMS_170, task, result);

        when("account is reimported (simulation)");
        var simResult = importJackSimulated(task, result);

        then("two assignments are added, the original one removed (simulation)");
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
                            .valuesToAdd().assertSize(2).end() // relation: read, write
                            .valuesToDelete().assertSize(1).end() // relation: default
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).find()
                    .assertState(ObjectProcessingStateType.UNMODIFIED)
                .end();
        // @formatter:on
    }

    /**
     * Resource definition: unchanged.
     *
     * Reimporting the jack (real).
     */
    @Test
    public void test180Reimporting() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        when("account is reimported (real)");
        importJackReal(result);

        then("the assignments are updated (real)");
        assertUserAfterByUsername(JACK)
                .assignments()
                .assertAssignments(2)
                .by().targetOid(guideOid).targetType(ServiceType.COMPLEX_TYPE).targetRelation(RELATION_READ).find().end()
                .by().targetOid(guideOid).targetType(ServiceType.COMPLEX_TYPE).targetRelation(RELATION_WRITE).find().end();
    }

    /**
     * Resource definition: added outbounds.
     *
     * Provisioning user `jim` with `read` access to `guide` (simulated).
     */
    @Test
    public void test300ProvisionJimWithOutboundsSimulated() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is reimported");
        reimportDmsResource(RESOURCE_DMS_300, task, result);

        and("user is created");
        var jim = new UserType()
                .name(JIM)
                .assignment(new AssignmentType()
                        .construction(RESOURCE_DMS_300.construction(ACCOUNT, INTENT_DEFAULT)));
        addObject(jim, task, result);
        assertUserBeforeByUsername(JIM)
                .singleLink();
        displayDumpable("account before", dmsScenario.account.getByNameRequired(JIM));

        when("assignment providing read access to guide is created (simulation)");
        var simResult = executeWithSimulationResult(
                List.of(deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(new AssignmentType()
                                .targetRef(guideOid, ServiceType.COMPLEX_TYPE, RELATION_READ))
                        .asObjectDelta(jim.getOid())),
                task, result);

        then("all is OK");
        assertProcessedObjects(simResult, "after")
                .display();
    }

    /**
     * Resource definition: same as above.
     *
     * Provisioning user `jim` with `read` access to `guide` (real).
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

        displayDumpable("account before", dmsScenario.account.getByNameRequired(JIM));

        when("assignment providing read access to guide is created (real)");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(new AssignmentType()
                                .targetRef(guideOid, ServiceType.COMPLEX_TYPE, RELATION_READ))
                        .asObjectDelta(jimOid),
                null, task, result);

        then("all is OK");
        assertUserAfterByUsername(JIM)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertAssignments(2)
                .singleLink()
                .resolveTarget()
                .display();

        displayDumpable("account after", dmsScenario.account.getByNameRequired(JIM));

        when("assignment providing write access to guide is created (real)");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(new AssignmentType()
                                .targetRef(guideOid, ServiceType.COMPLEX_TYPE, RELATION_WRITE))
                        .asObjectDelta(jimOid),
                null, task, result);

        then("all is OK");
        var newAssignment = assertUserAfterByUsername(JIM)
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .end()
                .end()
                .assignments()
                .assertAssignments(3)
                .by().targetRelation(RELATION_WRITE).find().getAssignment();

        displayDumpable("account after", dmsScenario.account.getByNameRequired(JIM));

        when("assignment providing write access to guide is deleted (real)");
        traced(() -> executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(newAssignment.clone())
                        .asObjectDelta(jimOid),
                null, task, result));

        then("all is OK");
        displayDumpable("account after", dmsScenario.account.getByNameRequired(JIM));
        assertUserAfterByUsername(JIM)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertAssignments(2)
                .singleLink()
                .resolveTarget()
                .display();
    }

    @Test
    public void test350ProvisionNewAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("reusing resource definition from previous test");

        when("user with access to guide is created");
        var alice = new UserType()
                .name(ALICE)
                .assignment(new AssignmentType()
                        .construction(RESOURCE_DMS_300.construction(ACCOUNT, INTENT_DEFAULT)))
                .assignment(new AssignmentType()
                        .targetRef(guideOid, ServiceType.COMPLEX_TYPE, RELATION_READ));
        addObject(alice, task, result);

        then("all is OK");
        assertUserAfterByUsername(ALICE)
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display();

        displayDumpable("account after", dmsScenario.account.getByNameRequired(ALICE));

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

    private void reimportDmsResource(DummyTestResource resource, Task task, OperationResult result) throws Exception {
        deleteObject(ResourceType.class, RESOURCE_DMS_OID, task, result);
        initTestObjects(task, result, resource);
    }

    private TestSimulationResult importJackSimulated(Task task, OperationResult result) throws Exception {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_DMS_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
                .withNameValue(JACK)
                .simulatedDevelopment()
                .withTracing()
                .executeOnForegroundSimulated(null, task, result);
    }

    private void importJackReal(OperationResult result) throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DMS_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
                .withNameValue(JACK)
                .executeOnForeground(result);
    }
}
