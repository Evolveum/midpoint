/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.negative;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_RELATED;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.test.asserter.predicates.StringAssertionPredicates.startsWith;
import static com.evolveum.midpoint.test.asserter.predicates.TimeAssertionPredicates.approximatelyCurrent;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the model service contract by using a broken CSV resource. Tests for negative test cases, mostly
 * correct handling of connector exceptions.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestBrokenResources extends AbstractConfiguredModelIntegrationTest {

    private static final String TEST_DIR = "src/test/resources/negative";
    private static final String TEST_TARGET_DIR = "target/test/negative";

    private static final File CONNECTOR_DUMMY_NOJARS_FILE = new File(TEST_DIR, "connector-dummy-nojars.xml");

    private static final File RESOURCE_CSVFILE_BROKEN_FILE = new File(TEST_DIR, "resource-csvfile-broken.xml");
    private static final String RESOURCE_CSVFILE_BROKEN_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3bbbb";

    private static final File RESOURCE_CSVFILE_NOTFOUND_FILE = new File(TEST_DIR, "resource-csvfile-notfound.xml");
    private static final String RESOURCE_CSVFILE_NOTFOUND_OID = "ef2bc95b-76e0-48e2-86d6-f0f002d3f0f0";

    private static final File RESOURCE_DUMMY_NOJARS_FILE = new File(TEST_DIR, "resource-dummy-nojars.xml");
    private static final String RESOURCE_DUMMY_NOJARS_OID = "10000000-0000-0000-0000-666600660004";

    private static final File RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE = new File(TEST_DIR, "resource-dummy-wrong-connector-oid.xml");
    private static final String RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID = "10000000-0000-0000-0000-666600660005";

    private static final File RESOURCE_DUMMY_NO_CONFIGURATION_FILE = new File(TEST_DIR, "resource-dummy-no-configuration.xml");
    private static final String RESOURCE_DUMMY_NO_CONFIGURATION_OID = "10000000-0000-0000-0000-666600660006";

    private static final File RESOURCE_DUMMY_UNACCESSIBLE_FILE = new File(TEST_DIR, "resource-dummy-unaccessible.xml");
    private static final String RESOURCE_DUMMY_UNACCESSIBLE_NAME = "unaccessible";
    private static final String RESOURCE_DUMMY_UNACCESSIBLE_OID = "10000000-0000-0000-0000-666600660007";

    private static final File RESOURCE_DUMMY_EBONY_FILE = new File(TEST_DIR, "resource-dummy-ebony.xml");
    private static final String RESOURCE_DUMMY_EBONY_NAME = "ebony";
    private static final String RESOURCE_DUMMY_EBONY_OID = "10000000-0000-0000-0000-00000000e305";

    // Broken iteration expressions, see also: violet resource in TestIteration
    protected static final File RESOURCE_DUMMY_BROKEN_VIOLET_FILE = new File(TEST_DIR, "resource-dummy-broken-violet.xml");
    protected static final String RESOURCE_DUMMY_BROKEN_VIOLET_OID = "10000000-0000-0000-0000-0000000ba204";
    protected static final String RESOURCE_DUMMY_BROKEN_VIOLET_NAME = "brokenViolet";

    private static final File ACCOUNT_SHADOW_MURRAY_CSVFILE_FILE = new File(TEST_DIR, "account-shadow-murray-csvfile.xml");
    private static final String ACCOUNT_SHADOW_MURRAY_CSVFILE_OID = "ef2bc95b-76e0-1111-d3ad-3d4f12120666";

    private static final String BROKEN_CSV_FILE_NAME = "broken.csv";
    private static final String BROKEN_CSV_SOURCE_FILE_NAME = TEST_DIR + "/" + BROKEN_CSV_FILE_NAME;
    private static final String BROKEN_CSV_TARGET_FILE_NAME = TEST_TARGET_DIR + "/" + BROKEN_CSV_FILE_NAME;

    private static final int NUMBER_OF_RESOURCES = 7;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        logger.trace("initSystem");

        // Resources
        File targetDir = new File(TEST_TARGET_DIR);
        if (!targetDir.exists()) {
            assertThat(targetDir.mkdirs()).isTrue();
        }

        MiscUtil.copyFile(new File(BROKEN_CSV_SOURCE_FILE_NAME), new File(BROKEN_CSV_TARGET_FILE_NAME));

        repoAddObjectFromFile(CONNECTOR_DUMMY_NOJARS_FILE, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_BLACK_NAME, RESOURCE_DUMMY_BLACK_FILE, RESOURCE_DUMMY_BLACK_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_EBONY_NAME, RESOURCE_DUMMY_EBONY_FILE, RESOURCE_DUMMY_EBONY_OID, initTask, initResult);
        initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_UNACCESSIBLE_NAME, null, null, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_BROKEN_VIOLET_NAME, RESOURCE_DUMMY_BROKEN_VIOLET_FILE, RESOURCE_DUMMY_BROKEN_VIOLET_OID, initTask, initResult);

        importObjectFromFile(RESOURCE_CSVFILE_BROKEN_FILE, initResult);
        importObjectFromFile(RESOURCE_CSVFILE_NOTFOUND_FILE, initResult);
        importObjectFromFile(RESOURCE_DUMMY_NOJARS_FILE, initResult);

        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
        repoAddObjectFromFile(SECURITY_POLICY_BENEVOLENT_FILE, initResult);
        repoAddObjectFromFile(PASSWORD_POLICY_BENEVOLENT_FILE, initResult);

        // Accounts
        repoAddObjectFromFile(ACCOUNT_SHADOW_MURRAY_CSVFILE_FILE, initResult);

        // Users
        repoAddObjectFromFile(USER_JACK_FILE, UserType.class, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, UserType.class, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

    }

    @Override
    public void postInitSystem(Task initTask, OperationResult initResult) throws Exception {
        super.postInitSystem(initTask, initResult);
        // Break it only after resource are reset in super.postInitSystem()
        getDummyResource(RESOURCE_DUMMY_UNACCESSIBLE_NAME).setBreakMode(BreakMode.NETWORK);
    }

    @Test
    public void test010TestResourceBroken() throws Exception {
        given();
        Task task = getTestTask();

        when();
        OperationResult testResult = modelService.testResource(RESOURCE_CSVFILE_BROKEN_OID, task, task.getResult());

        then();
        display("testResource result", testResult);
        TestUtil.assertSuccess("testResource result", testResult);
    }

    @Test
    public void test020GetResourceBroken() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_BROKEN_OID, null, task, result);

        // THEN
        display("getObject resource", resource);
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess("getObject result", result);

        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        if (fetchResult != null) {
            TestUtil.assertSuccess("resource.fetchResult", fetchResult);
        } else {
            // null means success
        }

        // TODO: better asserts
        assertNotNull("Null resource", resource);
    }

    @Test
    public void test030ListResources() throws Exception {
        testListResources(NUMBER_OF_RESOURCES, null);
    }

    @Test
    public void test100GetAccountMurray() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {

            // WHEN
            PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
                    null, task, result);

            display("Account (unexpected)", account);
            AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
            displayExpectedException(e);
            result.computeStatus();
            display("getObject result", result);
            TestUtil.assertFailure("getObject result", result);
        }

    }

    @Test
    public void test101GetAccountMurrayNoFetch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

        // WHEN
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
                options, task, result);

        display("getObject account", account);
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess("getObject result", result);
        // TODO: better asserts
        assertNotNull("Null resource", account);
    }

    @Test
    public void test102GetAccountMurrayRaw() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + ".test102GetAccountMurrayRaw");
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

        // WHEN
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
                options, task, result);

        display("getObject account", account);
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess("getObject result", result);
        // TODO: better asserts
        assertNotNull("Null resource", account);
    }

    @Test
    public void test120SearchAccountByUsernameJack() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + ".test120SearchAccountByUsernameJack");
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_BROKEN_OID, null, task, result);

        try {
            // WHEN
            findAccountByUsername("jack", resource, task, result);

            AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
            // This is expected
            result.computeStatus();
            display("findAccountByUsername result", result);
            TestUtil.assertFailure("findAccountByUsername result", result);
        }
    }

    @Test
    public void test210TestResourceNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();

        // WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_CSVFILE_NOTFOUND_OID, task, task.getResult());

        // THEN
        display("testResource result", testResult);
        TestUtil.assertFailure("testResource result", testResult);
    }

    @Test
    public void test220GetResourceNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_NOTFOUND_OID, null, task, result);

        // THEN
        String localNodeId = taskManager.getNodeId();
        assertResourceAfter(resource)
                .operationalState()
                .assertAny()
                .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.BROKEN)
                .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, approximatelyCurrent(60000))
                .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status set to BROKEN"))
                .end()
                .operationalStateHistory()
                .assertSize(1)
                .value(0)
                .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.BROKEN)
                .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, approximatelyCurrent(60000))
                .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status set to BROKEN"));

        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());

        // TODO: better asserts
        assertNotNull("Null resource", resource);
    }

    @Test
    public void test221GetResourceNotFoundResolveConnector() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_NOTFOUND_OID, options, task, result);

        // THEN
        display("getObject resource", resource);
        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());

        // TODO: better asserts
        assertNotNull("Null resource", resource);

        assertNotNull("Connector was not resolved", resource.asObjectable().getConnectorRef().asReferenceValue().getObject());
    }

    @Test
    public void test310TestResourceNoJars() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + ".test310TestResourceNoJars");

        // WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_NOJARS_OID, task, task.getResult());

        // THEN
        display("testResource result", testResult);
        TestUtil.assertFailure("testResource result", testResult);
    }

    @Test
    public void test320GetResourceNoJars() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_NOJARS_OID, null, task, result);

        // THEN
        display("getObject resource", resource);
        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());

        // TODO: better asserts
        assertNotNull("Null resource", resource);
    }

    @Test
    public void test325SearchResourceNoJars() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("searching for broken resource");
        List<PrismObject<ResourceType>> resources =
                modelService.searchObjects(
                        ResourceType.class,
                        queryFor(ResourceType.class)
                                .id(RESOURCE_DUMMY_NOJARS_OID)
                                .build(),
                        null,
                        task,
                        result);

        then("single resource is found");
        displayCollection("resources", resources);
        assertThat(resources).as("resources").hasSize(1);
        PrismObject<ResourceType> resource = resources.get(0);

        and("status is partial error");
        result.computeStatus();
        display("searchObjects result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        and("fetchResult contains an error");
        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
    }

    @Test
    public void test350AddResourceWrongConnectorOid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE);
        ObjectDelta<ResourceType> delta = DeltaFactory.Object.createAddDelta(resource);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        try {
            // WHEN
            modelService.executeChanges(deltas, null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (ObjectNotFoundException e) {
            // This is expected
        }

        // THEN
        result.computeStatus();
        display(result);
        TestUtil.assertFailure(result);
    }

    /**
     * Even "raw" add should fail. No connector object means no connector schema which means no
     * definitions for configuration properties which means we are not able to store them.
     */
    @Test
    public void test352AddResourceWrongConnectorOidRaw() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE);
        ObjectDelta<ResourceType> delta = DeltaFactory.Object.createAddDelta(resource);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        try {
            // WHEN
            modelService.executeChanges(deltas, null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (ObjectNotFoundException e) {
            // This is expected
        }

        // THEN
        result.computeStatus();
        display(result);
        TestUtil.assertFailure(result);
    }

    /**
     * Store directly to repo. This is not really a test, it is more like a hack to prepare
     * environment for next tests.
     */
    @Test
    public void test355AddResourceWrongConnectorOidRepo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE);

        // WHEN
        repositoryService.addObject(resource, null, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test356GetResourceWrongConnectorOid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID, null, task, result);

        // THEN
        display("getObject resource", resource);
        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());

        // TODO: better asserts
        assertNotNull("Null resource", resource);
    }

    @Test
    public void test357SearchResourceWrongConnectorOid() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("searching for broken resource");
        List<PrismObject<ResourceType>> resources =
                modelService.searchObjects(
                        ResourceType.class,
                        queryFor(ResourceType.class)
                                .id(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID)
                                .build(),
                        null,
                        task,
                        result);

        then("single resource is found");
        displayCollection("resources", resources);
        assertThat(resources).as("resources").hasSize(1);
        PrismObject<ResourceType> resource = resources.get(0);

        and("status is partial error");
        result.computeStatus();
        display("searchObjects result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        and("fetchResult contains an error");
        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
    }

    /**
     * It is a bit questionable if we want to support this mode of operation (asking for resources by specifying type
     * of {@link AssignmentHolderType}).
     */
    @Test
    public void test358SearchResourceWrongConnectorOidUseTypeFilter() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("searching for broken resource");
        ObjectFilter filter = queryFor(ResourceType.class)
                .id(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID)
                .buildFilter();
        List<PrismObject<AssignmentHolderType>> resources =
                modelService.searchObjects(
                        AssignmentHolderType.class,
                        queryFor(AssignmentHolderType.class)
                                .type(ResourceType.class)
                                .filter(filter)
                                .build(),
                        null,
                        task,
                        result);

        then("single resource is found");
        displayCollection("resources", resources);
        assertThat(resources).as("resources").hasSize(1);
        PrismObject<AssignmentHolderType> resource = resources.get(0);

        and("status is partial error");
        result.computeStatus();
        display("searchObjects result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        and("fetchResult contains an error");
        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
    }

    @Test
    public void test359DeleteResourceWrongConnectorOid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ResourceType> delta = prismContext.deltaFactory().object()
                .createDeleteDelta(ResourceType.class, RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        assertNoObject(ResourceType.class, RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID, task, result);
    }

    @Test
    public void test360AddResourceNoConfiguration() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_NO_CONFIGURATION_FILE);
        PrismObject<ConnectorType> connectorDummy = findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, result);
        resource.asObjectable().getConnectorRef().setOid(connectorDummy.getOid());

        ObjectDelta<ResourceType> delta = DeltaFactory.Object.createAddDelta(resource);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test362GetResourceNoConfiguration() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_CONFIGURATION_OID, null, task, result);

        // THEN
        display("getObject resource", resource);
        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());

        // TODO: better asserts
        assertNotNull("Null resource", resource);
    }

    @Test
    public void test368ListResources() throws Exception {
        testListResources(NUMBER_OF_RESOURCES + 1, null);
    }

    @Test
    public void test369DeleteResourceNoConfiguration() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ResourceType> delta = prismContext.deltaFactory().object()
                .createDeleteDelta(ResourceType.class, RESOURCE_DUMMY_NO_CONFIGURATION_OID);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);

        assertNoObject(ResourceType.class, RESOURCE_DUMMY_NO_CONFIGURATION_OID, task, result);
    }

    @Test
    public void test370ListResources() throws Exception {
        testListResources(NUMBER_OF_RESOURCES, null);
    }

    @Test
    public void test371ImportInaccessibleResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        importObjectFromFile(RESOURCE_DUMMY_UNACCESSIBLE_FILE, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    /**
     * No fetch operation should NOT try to read the schema.
     * MID-3509
     */
    @Test
    public void test372GetInaccessibleResourceNoFetch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_UNACCESSIBLE_OID,
                GetOperationOptions.createNoFetchCollection(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        display("Resource after", resource);
        assertNotNull("No resource", resource);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
    }

    /**
     * No fetch operation should NOT try to read the schema.
     * MID-3509
     */
    @Test
    public void test374ListResourcesNoFetch() throws Exception {
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);

        testListResources(NUMBER_OF_RESOURCES + 1, GetOperationOptions.createNoFetchCollection());

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
    }

    @Test
    public void test375ListResources() throws Exception {
        testListResources(NUMBER_OF_RESOURCES + 1, null);
    }

    public void testListResources(
            int expectedNumber, Collection<SelectorOptions<GetOperationOptions>> options)
            throws Exception {

        // GIVEN (1)
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN (1)
        final SearchResultList<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);

        // THEN (1)
        result.computeStatus();
        display("getObject result", result);
        if (options == null) {
            assertEquals("Expected partial error (search)", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
        } else if (GetOperationOptions.isNoFetch(SelectorOptions.findRootOptions(options))) {
            TestUtil.assertSuccess(result);
        } else {
            AssertJUnit.fail("unexpected");
        }
        display("Got resources: " + resources);
        assertEquals("Wrong number of resources", expectedNumber, resources.size());

        // GIVEN (2)
        resources.clear();
        task = getTestTask();
        result = task.getResult();

        ResultHandler<ResourceType> handler = (object, parentResult) -> {
            resources.add(object);
            return true;
        };

        // WHEN (2)
        modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);

        // THEN (2)
        result.computeStatus();
        display("getObject result", result);
        if (options == null) {
            assertEquals("Expected partial error (searchIterative)", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
        } else if (GetOperationOptions.isNoFetch(SelectorOptions.findRootOptions(options))) {
            TestUtil.assertSuccess(result);
        } else {
            AssertJUnit.fail("unexpected");
        }
        display("Got resources: " + resources);
        assertEquals("Wrong number of resources", expectedNumber, resources.size());

    }

    @Test
    public void test377GetResourceNoConfiguration() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_UNACCESSIBLE_OID, null, task, result);

        // THEN
        display("getObject resource", resource);
        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        OperationResultType fetchResult = resource.asObjectable().getFetchResult();
        display("resource.fetchResult", fetchResult);
        assertEquals("Expected partial error in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());

        assertNotNull("Null resource", resource);
    }

    /**
     * Assign two resources to a user. One of them is looney, the other is not. The result should be that
     * the account on the good resource is created.
     * <p>
     * This one dies on the lack of schema.
     * <p>
     * MID-1248
     */
    @Test
    public void test400AssignTwoResourcesNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = createAssignTwoResourcesDelta(RESOURCE_CSVFILE_NOTFOUND_OID);

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("executeChanges result", result);
        assertPartialError(result);

        DummyAccount jackDummyAccount = getDummyResource().getAccountByName(USER_JACK_USERNAME);
        assertNotNull("No jack dummy account", jackDummyAccount);
    }

    /**
     * Assign two resources to a user. One of them is looney, the other is not. The result should be that
     * the account on the good resource is created.
     * <p>
     * This one dies on connector error.
     */
    @Test
    public void test401AssignTwoResourcesBroken() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = createAssignTwoResourcesDelta(RESOURCE_CSVFILE_BROKEN_OID);

        try {
            // WHEN
            when();
            executeChanges(userDelta, null, task, result);

            assertNotReached();
        } catch (GenericConnectorException e) {
            then();
            displayExpectedException(e);
        }

        result.computeStatus();
        display("executeChanges result", result);
        assertFailure(result);

        DummyAccount jackDummyAccount = getDummyResource().getAccountByName(USER_JACK_USERNAME);
        assertNotNull("No jack dummy account", jackDummyAccount);
    }

    private ObjectDelta<UserType> createAssignTwoResourcesDelta(String badResourceOid) throws SchemaException {
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, badResourceOid, null, true);
        userDelta.addModification(createAccountAssignmentModification(RESOURCE_DUMMY_OID, null, true));
        displayDumpable("input delta", userDelta);
        return userDelta;
    }

    /**
     * No error here yet. Provisioning scripts run without a problem.
     * This tests is just used as a control and to prepare the environment.
     * MID-4060
     */
    @Test
    public void test500AssignResourceBlack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);
        assertNoDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // WHEN
        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertThat(getSingleLinkOid(userAfter)).isNotNull();

        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);

        assertDummyScripts(RESOURCE_DUMMY_BLACK_NAME, "add/after", null);
    }

    /**
     * No error here yet. Provisioning scripts run without a problem.
     * This tests is just used as a control and to prepare the environment.
     * MID-4060
     */
    @Test
    public void test502ModifyUserEmployeeNumberNone() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_NUMBER, task, result, "none");

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertThat(getSingleLinkOid(userAfter)).isNotNull();

        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);

        assertDummyScripts(RESOURCE_DUMMY_BLACK_NAME, "modify/after", "none");
    }

    /**
     * No error here yet. Provisioning scripts run without a problem.
     * This tests is just used as a control and to prepare the environment.
     * MID-4060
     */
    @Test
    public void test509UnassignResourceBlack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        // WHEN
        when();
        unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertDummyScripts(RESOURCE_DUMMY_BLACK_NAME, "delete/after", "none");
    }

    /**
     * Causing an error in provisioning script. Default criticality.
     * The error should stop operation.
     * MID-4060
     */
    @Test
    public void test510AssignResourceBlackError() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_NUMBER, task, result, DummyResource.POWERFAIL_ARG_ERROR_GENERIC);
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);
        assertNoDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        try {
            // WHEN
            when();
            assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

            assertNotReached();

        } catch (GenericConnectorException e) {
            then();
            displayExpectedException(e);
        }

        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        // Failed after script. Account is not linked (may not really be created).
        assertLiveLinks(userAfter, 0);

        // But we know that the account was created and that it exists
        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);

        assertDummyScripts(RESOURCE_DUMMY_BLACK_NAME, "add/after", DummyResource.POWERFAIL_ARG_ERROR_GENERIC);
    }

    /**
     * Recon should fix the linking.
     * MID-4060
     */
    @Test
    public void test512ReconcileUser() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        // WHEN
        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        // THEN
        then();
        // Errors deep inside the results are expected
        assertSuccess(result, 2);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertThat(getSingleLinkOid(userAfter)).isNotNull();

        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
    }

    /**
     * Causing an error (RuntimeException) in provisioning script. Default criticality.
     * The error should stop operation.
     * MID-4060
     */
    @Test
    public void test514ModifyUserEmployeeNumberRuntime() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        try {
            // WHEN
            when();
            modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_NUMBER, task, result, DummyResource.POWERFAIL_ARG_ERROR_RUNTIME);

            assertNotReached();

        } catch (RuntimeException e) {
            then();
            displayExpectedException(e);
            assertEquals("Wrong exception message", "Booom! PowerFail script failed (runtime)", e.getMessage());
        }

        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertThat(getSingleLinkOid(userAfter)).isNotNull();

        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);

        assertDummyScripts(RESOURCE_DUMMY_BLACK_NAME, "modify/after", DummyResource.POWERFAIL_ARG_ERROR_RUNTIME);
    }

    /**
     * Causing an error in provisioning script. Default criticality.
     * The error should stop operation.
     * MID-4060
     */
    @Test
    public void test518UnassignResourceBlack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        try {
            // WHEN
            when();
            unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

            assertNotReached();

        } catch (RuntimeException e) {
            then();
            displayExpectedException(e);
            assertEquals("Wrong exception message", "Booom! PowerFail script failed (runtime)", e.getMessage());
        }

        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        // Still linked. There was error, we assume that the account might not be deleted.
        assertThat(getSingleLinkOid(userAfter)).isNotNull();

        // But we know that it is gone ...
        assertNoDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertDummyScripts(RESOURCE_DUMMY_BLACK_NAME, "delete/after", DummyResource.POWERFAIL_ARG_ERROR_RUNTIME);
    }

    /**
     * Recon should fix the linking.
     * MID-4060
     */
    @Test
    public void test519ReconcileUser() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        // WHEN
        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        // THEN
        then();
        if (InternalsConfig.isShadowCachingFullByDefault()) {
            // The 'not found' exception is caught on delta execution in this case, which is reported in more harsh way.
            assertPartialError(result);
        } else {
            assertSuccess(result);
        }

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
    }

    /**
     * Causing an error in provisioning script. Partial criticality.
     * Error should be indicated, but the operation should go on.
     * MID-4060
     */
    @Test
    public void test520AssignResourceEbonyError() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_NUMBER, task, result, DummyResource.POWERFAIL_ARG_ERROR_GENERIC);
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);
        assertNoDummyAccount(RESOURCE_DUMMY_EBONY_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // WHEN
        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_EBONY_OID, null, task, result);

        // THEN
        then();
        assertPartialError(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        // Partial criticality. We assume that the account exists.
        assertLiveLinks(userAfter, 1);

        // But we know that the account was created and that it exists
        assertDummyAccount(RESOURCE_DUMMY_EBONY_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);

        assertDummyScripts(RESOURCE_DUMMY_EBONY_NAME, "add/after", DummyResource.POWERFAIL_ARG_ERROR_GENERIC);
    }

    /**
     * Causing an error in provisioning script. partial criticality.
     * The operation should go on.
     * MID-4060
     */
    @Test
    public void test524ModifyUserEmployeeNumberRuntime() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_NUMBER, task, result, DummyResource.POWERFAIL_ARG_ERROR_RUNTIME);

        // THEN
        then();
        assertPartialError(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertThat(getSingleLiveLinkOid(userAfter)).isNotNull();

        assertDummyAccount(RESOURCE_DUMMY_EBONY_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);

        assertDummyScripts(RESOURCE_DUMMY_EBONY_NAME, "modify/after", DummyResource.POWERFAIL_ARG_ERROR_RUNTIME);
    }

    /**
     * Causing an error in provisioning script. Partial criticality.
     * The operation should go on.
     * MID-4060
     */
    @Test
    public void test528UnassignResourceEbony() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest5xx();

        assertUserBefore(USER_GUYBRUSH_OID)
                .displayWithProjections()
                .assertAssignments(1)
                .assertLiveLinks(1);

        // WHEN
        when();
        unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_EBONY_OID, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertPartialError(result);

        String shadowOid = assertUserAfter(USER_GUYBRUSH_OID)
                .displayWithProjections()
                .assertAssignments(0)
                .links()
                    .assertLinks(0, 2)
                    .by().resourceOid(RESOURCE_DUMMY_BLACK_OID).dead(true).relation(ORG_RELATED).find()
                        .resolveTarget()
                            .display()
                            .assertDead()
                            .assertTombstone()
                            .end()
                        .end()
                    .by().resourceOid(RESOURCE_DUMMY_EBONY_OID).dead(true).relation(ORG_RELATED).find()
                        .resolveTarget()
                            .display()
                            .assertDead() // TODO: not sure whether this should be dead or alive
                            .assertTombstone()
                            .end()
                        .getOid();

        assertNoDummyAccount(RESOURCE_DUMMY_EBONY_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertDummyScripts(RESOURCE_DUMMY_EBONY_NAME, "delete/after", DummyResource.POWERFAIL_ARG_ERROR_RUNTIME);

        // CLEANUP
        displayCleanup();
        forceDeleteShadow(shadowOid);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertAssignments(0)
                .assertLiveLinks(0);

        assertNoShadow(shadowOid);
    }

    /**
     * MID-4255
     * See also TestIteration.test350GuybrushAssignAccountDummyViolet()
     */
    @Test
    public void test600GuybrushAssignAccountDummyViolet() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        try {
            // WHEN
            when();
            assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_BROKEN_VIOLET_OID, null, task, result);

            assertNotReached();
        } catch (ExpressionEvaluationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_BROKEN_VIOLET_NAME, "guybrush.3");
    }

    private void prepareTest5xx() throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        purgeProvisioningScriptHistory(RESOURCE_DUMMY_BLACK_NAME);
        purgeProvisioningScriptHistory(RESOURCE_DUMMY_EBONY_NAME);
    }

    private void assertDummyScripts(String dummyName, String operation, String errorArg) {
        displayProvisioningScripts(dummyName);
        ProvisioningScriptSpec script = new ProvisioningScriptSpec("operation:" + operation);
        script.addArgSingle(DummyResource.POWERFAIL_ARG_ERROR, errorArg);
        script.setLanguage(DummyResource.SCRIPT_LANGUAGE_POWERFAIL);
        IntegrationTestTools.assertScripts(getDummyResource(dummyName).getScriptHistory(), script);
    }

}
