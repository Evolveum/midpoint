/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.associations;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.asserter.predicates.ReferenceAssertionPredicates.references;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.GENERIC;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

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
    private static final String GUIDE = "guide";

    /** Initialized for each test anew (when the specific resource is initialized). */
    private static DummyDmsScenario dmsScenario;

    private static final TestObject<ArchetypeType> ARCHETYPE_DOCUMENT = TestObject.file(
            TEST_DIR, "archetype-document.xml", "8c0b32f9-fadc-42cb-bc05-878ecfe001e8");

    private static final DummyTestResource RESOURCE_DMS_100 = createDmsResource("resource-dms-100.xml");
    private static final DummyTestResource RESOURCE_DMS_120 = createDmsResource("resource-dms-120.xml");
    private static final DummyTestResource RESOURCE_DMS_130 = createDmsResource("resource-dms-130.xml");

    private String guideOid;

    @BeforeMethod
    public void onNativeOnly() {
        skipIfNotNativeRepository();
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
//            DummyObject jackCanWriteGuide = dmsScenario.access.add("jack-can-write-guide");
//            jackCanWriteGuide.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_WRITE);

            dmsScenario.accountAccess.add(jack, jackCanReadGuide);
            dmsScenario.accessDocument.add(jackCanReadGuide, guide);

//            dmsScenario.accountAccess.add(jack, jackCanWriteGuide);
//            dmsScenario.accessDocument.add(jackCanWriteGuide, guide);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult, ARCHETYPE_DOCUMENT);
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
    }

    /**
     * Resource definition: only accounts and documents, no association types.
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

    private TestSimulationResult importJackSimulated(Task task, OperationResult result) throws Exception {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_DMS_OID)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
                .withNameValue(JACK)
                .simulatedDevelopment()
                .executeOnForegroundSimulated(null, task, result);
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

//    @Test
//    public void test150Temp() throws Exception {
//        Task task = getTestTask();
//        OperationResult result = task.getResult();
//
//        given("resource is reimported");
//        reimportDmsResource(RESOURCE_DMS_130, task, result);
//
//        when("account is reimported (simulation)");
//        importAccountsRequest()
//                .withResourceOid(RESOURCE_DMS_OID)
//                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT))
//                .withNameValue(JACK)
//                .withTracing()
//                .executeOnForeground(result);
//    }

    private void reimportDmsResource(DummyTestResource resource, Task task, OperationResult result) throws Exception {
        deleteObject(ResourceType.class, RESOURCE_DMS_OID, task, result);
        initTestObjects(task, result, resource);
    }
}
