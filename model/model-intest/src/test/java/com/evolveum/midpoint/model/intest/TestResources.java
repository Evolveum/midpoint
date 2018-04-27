/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sql.testing.CarefulAnt;
import com.evolveum.midpoint.repo.sql.testing.ResourceCarefulAntUtil;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.FailableFunction;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestResources extends AbstractConfiguredModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static final int MAX_RANDOM_SEQUENCE_ITERATIONS = 15;

	private static List<CarefulAnt<ResourceType>> ants = new ArrayList<>();
	private static CarefulAnt<ResourceType> descriptionAnt;
	private static String lastVersion;
	private static Random rnd = new Random();

	protected DummyResource dummyResource;
	protected DummyResourceContoller dummyResourceCtl;
	protected PrismObject<ResourceType> resourceDummy;

	protected DummyResource dummyResourceRed;
	protected DummyResourceContoller dummyResourceCtlRed;
	protected PrismObject<ResourceType> resourceDummyRed;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		dummyResourceCtl = DummyResourceContoller.create(null);
		dummyResourceCtl.extendSchemaPirate();
		dummyResource = dummyResourceCtl.getDummyResource();
		dummyResourceCtl.addAttrDef(dummyResource.getAccountObjectClass(),
				DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, String.class, false, false);

		// Add resource directly to repo to avoid any initialization
		resourceDummy = PrismTestUtil.parseObject(RESOURCE_DUMMY_FILE);
		PrismObject<ConnectorType> connectorDummy = findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, initResult);
		resourceDummy.asObjectable().getConnectorRef().setOid(connectorDummy.getOid());
		repositoryService.addObject(resourceDummy, null, initResult);

		dummyResourceCtl.setResource(resourceDummy);


		dummyResourceCtlRed = DummyResourceContoller.create(RESOURCE_DUMMY_RED_NAME, resourceDummyRed);
		dummyResourceCtlRed.extendSchemaPirate();
		dummyResourceRed = dummyResourceCtlRed.getDummyResource();

		// Add resource directly to repo to avoid any initialization
		resourceDummyRed = PrismTestUtil.parseObject(RESOURCE_DUMMY_RED_FILE);
		resourceDummyRed.asObjectable().getConnectorRef().setOid(connectorDummy.getOid());
		repositoryService.addObject(resourceDummyRed, null, initResult);

		dummyResourceCtlRed.setResource(resourceDummyRed);

		ResourceCarefulAntUtil.initAnts(ants, RESOURCE_DUMMY_FILE, prismContext);
		descriptionAnt = ants.get(0);
		InternalMonitor.reset();
		InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, true);
		InternalMonitor.setTrace(InternalOperationClasses.RESOURCE_SCHEMA_OPERATIONS, true);
		InternalsConfig.encryptionChecks = false;

		InternalMonitor.setTrace(InternalOperationClasses.PRISM_OBJECT_CLONES, true);
	}

	/**
	 * MID-3424
	 */
	@Test
    public void test050GetResourceRaw() throws Exception {
		final String TEST_NAME = "test050GetResourceRaw";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options , task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

		assertResourceDummy(resource, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchema(resource));

        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 1);
	}

	/**
	 * MID-3424
	 */
	@Test
    public void test052GetResourceNoFetch() throws Exception {
		final String TEST_NAME = "test052GetResourceNoFetch";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				GetOperationOptions.createNoFetch());

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
				task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

		assertResourceDummy(resource, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchema(resource));

        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1); // First "real" read
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
	}
	
	/**
	 * Make sure that resource caching works well even if noFetch is used.
	 */
	@Test
    public void test053GetResourceNoFetchAgain() throws Exception {
		final String TEST_NAME = "test053GetResourceNoFetchAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				GetOperationOptions.createNoFetch());

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
				task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

		assertResourceDummy(resource, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchema(resource));

        // Previous noFetch read did NOT place resource in the cache. Because the resource
        // may not be complete.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
	}

	/**
	 * MID-3424
	 */
	@Test
    public void test055GetResourceNoFetchReadOnly() throws Exception {
		final String TEST_NAME = "test055GetResourceNoFetchReadOnly";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
				task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  0);

		assertResourceDummy(resource, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchema(resource));

        // Previous noFetch read did NOT place resource in the cache. Because the resource
        // may not be complete.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
	}

	/**
	 * MID-3424
	 */
	@Test
    public void test100SearchResourcesNoFetch() throws Exception {
		final String TEST_NAME = "test100SearchResourcesNoFetch";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

		// WHEN
        displayWhen(TEST_NAME);
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  2);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource, false);
        }

        // No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
	}

	/**
	 * MID-3424
	 */
	@Test
    public void test102SearchResourcesNoFetchReadOnly() throws Exception {
		final String TEST_NAME = "test102SearchResourcesNoFetchReadOnly";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);

		// WHEN
        displayWhen(TEST_NAME);
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  0);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource, false);
        }

     	// No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
	}

	/**
	 * MID-3424
	 */
	@Test
    public void test105SearchResourcesIterativeNoFetch() throws Exception {
		final String TEST_NAME = "test105SearchResourcesIterativeNoFetch";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        final List<PrismObject<ResourceType>> resources = new ArrayList<>();

        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
				assertResource(resource, false);
				resources.add(resource);
				return true;
			};

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

		// WHEN
		displayWhen(TEST_NAME);
        modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  2);

        // No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
	}

	/**
	 * MID-3424
	 */
	@Test
    public void test107SearchResourcesIterativeNoFetchReadOnly() throws Exception {
		final String TEST_NAME = "test107SearchResourcesIterativeNoFetchReadOnly";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        final List<PrismObject<ResourceType>> resources = new ArrayList<>();

        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
				assertResource(resource, false);
				resources.add(resource);
				return true;
			};

		GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);

		// WHEN
        displayWhen(TEST_NAME);
        modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  2);

        // No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
	}

	@Test
    public void test110GetResourceDummy() throws Exception {
		final String TEST_NAME = "test110GetResourceDummy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		// WHEN
        displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  4);

        assertResourceDummy(resource, true);

        // TODO not sure why are there 2 read counts. Should be 1. But this is not that important right now.
        // Some overhead on initial resource read is OK. What is important is that it does not increase during 
        // normal account operations.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 2);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 1); // cachingMetadata
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);

        assertEquals("Wrong dummy useless string", RESOURCE_DUMMY_USELESS_STRING, dummyResource.getUselessString());
        
        assertSteadyResources();
	}

	@Test
    public void test112GetResourceDummyReadOnly() throws Exception {
		final String TEST_NAME = "test112GetResourceDummyReadOnly";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				GetOperationOptions.createReadOnly());

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				options , task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
        
        assertSteadyResources();
	}


	@Test
    public void test120SearchResources() throws Exception {
		final String TEST_NAME = "test120SearchResources";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();

		// WHEN
        displayWhen(TEST_NAME);
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, null, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSuccess(result);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource, true);
        }

        // Obviously, there is some uninitialized resource in the system
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 1); // cachingMetadata
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        
        assertSteadyResources();
	}

	@Test
    public void test125SearchResourcesIterative() throws Exception {
		final String TEST_NAME = "test125SearchResourcesIterative";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();

        final List<PrismObject<ResourceType>> resources = new ArrayList<>();

        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
				assertResource(resource, true);
				resources.add(resource);
				return true;
			};

		// WHEN
        modelService.searchObjectsIterative(ResourceType.class, null, handler, null, task, result);

		// THEN
        assertSuccess(result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSteadyResources();
	}

	private void assertResourceDummy(PrismObject<ResourceType> resource, boolean expectSchema) {
		assertResource(resource, expectSchema);

		PrismContainer<ConnectorConfigurationType> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition = configurationContainer.getDefinition();
		assertDummyConfigurationContainerDefinition(configurationContainerDefinition, "from container");

		PrismContainer<Containerable> configurationPropertiesContainer = configurationContainer.findContainer(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No container "+SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME, configurationPropertiesContainer);

		assertConfigurationPropertyDefinition(configurationPropertiesContainer,
				"uselessString", DOMUtil.XSD_STRING, 0, 1, "UI_INSTANCE_USELESS_STRING", "UI_INSTANCE_USELESS_STRING_HELP");

		PrismContainerDefinition<Containerable> configurationPropertiesContainerDefinition = configurationContainerDefinition.findContainerDefinition(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		configurationPropertiesContainerDefinition = configurationPropertiesContainer.getDefinition();
		assertNotNull("No container definition in "+configurationPropertiesContainer);

		assertConfigurationPropertyDefinition(configurationPropertiesContainerDefinition,
				"uselessString", DOMUtil.XSD_STRING, 0, 1, "UI_INSTANCE_USELESS_STRING", "UI_INSTANCE_USELESS_STRING_HELP");

		PrismObjectDefinition<ResourceType> objectDefinition = resource.getDefinition();
		assertNotNull("No object definition in resource", objectDefinition);
		PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinitionFromObjectDefinition = objectDefinition.findContainerDefinition(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertDummyConfigurationContainerDefinition(configurationContainerDefinitionFromObjectDefinition, "from object definition");

	}

	private void assertDummyConfigurationContainerDefinition(
			PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition,
			String desc) {
		display("Dummy configuration container definition "+desc, configurationContainerDefinition);
		PrismContainerDefinition<Containerable> configurationPropertiesContainerDefinition = configurationContainerDefinition.findContainerDefinition(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No container definition for "+SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME+" "+desc, configurationPropertiesContainerDefinition);

		assertConfigurationPropertyDefinition(configurationPropertiesContainerDefinition,
				"uselessString", DOMUtil.XSD_STRING, 0, 1, "UI_INSTANCE_USELESS_STRING", "UI_INSTANCE_USELESS_STRING_HELP");

	}

	private void assertConfigurationPropertyDefinition(PrismContainerDefinition<Containerable> containerDefinition,
			String propertyLocalName, QName expectedType, int expectedMinOccurs, int expectedMaxOccurs, String expectedDisplayName, String expectedHelp) {
		QName propName = new QName(containerDefinition.getTypeName().getNamespaceURI(),propertyLocalName);
		PrismPropertyDefinition propDef = containerDefinition.findPropertyDefinition(propName);
		assertConfigurationPropertyDefinition(propDef, expectedType, expectedMinOccurs, expectedMaxOccurs, expectedDisplayName, expectedHelp);
	}

	private void assertConfigurationPropertyDefinition(PrismContainer container,
			String propertyLocalName, QName expectedType, int expectedMinOccurs, int expectedMaxOccurs, String expectedDisplayName, String expectedHelp) {
		QName propName = new QName(container.getDefinition().getTypeName().getNamespaceURI(),propertyLocalName);
		PrismProperty prop = container.findProperty(propName);
		assertNotNull("No property "+propName, prop);
		PrismPropertyDefinition propDef = prop.getDefinition();
		assertNotNull("No definition for property "+prop, propDef);
		assertConfigurationPropertyDefinition(propDef, expectedType, expectedMinOccurs, expectedMaxOccurs, expectedDisplayName, expectedHelp);
	}

	private void assertConfigurationPropertyDefinition(PrismPropertyDefinition propDef, QName expectedType,
			int expectedMinOccurs, int expectedMaxOccurs, String expectedDisplayName, String expectedHelp) {
		PrismAsserts.assertDefinition(propDef, propDef.getName(), expectedType, expectedMinOccurs, expectedMaxOccurs);
		assertEquals("Wrong displayName in "+propDef.getName()+" definition", expectedDisplayName, propDef.getDisplayName());
		assertEquals("Wrong help in "+propDef.getName()+" definition", expectedHelp, propDef.getHelp());
	}

	private void assertResource(PrismObject<ResourceType> resource, boolean expectSchema) {
		display("Resource", resource);
		display("Resource def", resource.getDefinition());
		PrismContainer<ConnectorConfigurationType> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No Resource connector configuration def", configurationContainer);
		PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition = configurationContainer.getDefinition();
		display("Resource connector configuration def", configurationContainerDefinition);
		display("Resource connector configuration def complex type def", configurationContainerDefinition.getComplexTypeDefinition());
		assertNotNull("Empty Resource connector configuration def", configurationContainer.isEmpty());
		assertEquals("Wrong compile-time class in Resource connector configuration in "+resource, ConnectorConfigurationType.class,
				configurationContainer.getCompileTimeClass());
		assertEquals("configurationContainer maxOccurs", 1, configurationContainerDefinition.getMaxOccurs());

		resource.checkConsistence(true, true);

		Element schema = ResourceTypeUtil.getResourceXsdSchema(resource);
		if (expectSchema) {
			assertNotNull("no schema in "+resource, schema);
		} else {
			assertNull("Unexpected schema in "+resource+": "+schema, schema);
		}
	}

	@Test
    public void test200GetResourceRawAfterSchema() throws Exception {
		final String TEST_NAME = "test200GetResourceRawAfterSchema";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        IntegrationTestTools.assertNoRepoCache();

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		// WHEN
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options , task, result);

		// THEN
		IntegrationTestTools.assertNoRepoCache();
		SqlRepoTestUtil.assertVersionProgress(null, resource.getVersion());
		lastVersion =  resource.getVersion();
		display("Initial version", lastVersion);

		assertSuccess(result);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
	}

	/**
	 * Red resource has an expression for uselessString configuration property. Check that.
	 */
	@Test
    public void test210GetResourceDummyRed() throws Exception {
		final String TEST_NAME = "test210GetResourceDummyRed";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		// WHEN
        displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_RED_OID, null , task, result);

		// THEN
		displayThen(TEST_NAME);
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);

        assertEquals("Wrong RED useless string", RESOURCE_DUMMY_RED_USELESS_STRING, dummyResourceRed.getUselessString());
	}

    @Test
    public void test750GetResourceRaw() throws Exception {
		final String TEST_NAME = "test750GetResourceRaw";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options , task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        display("Resource", resource);
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

		assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
	}

    @Test
    public void test752GetResourceDummy() throws Exception {
		final String TEST_NAME = "test752GetResourceDummy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		// WHEN
        displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
	}

    @Test
    public void test760ModifyConfigurationString() throws Exception {
		final String TEST_NAME = "test760ModifyConfigurationString";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<ResourceType> resourceDelta =  createConfigurationPropertyDelta(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
        		"whatever wherever");

    	// WHEN
    	displayWhen(TEST_NAME);
    	modelService.executeChanges(MiscSchemaUtil.createCollection(resourceDelta), null, task, result);

    	// THEN
    	displayThen(TEST_NAME);
    	assertSuccess(result);

    	PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
    	PrismAsserts.assertPropertyValue(resourceAfter,
    			getConfigurationPropertyPath(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME),
    			"whatever wherever");

    	assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
    }

    @Test
    public void test761ModifyConfigurationStringRaw() throws Exception {
		final String TEST_NAME = "test761ModifyConfigurationStringRaw";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<ResourceType> resourceDelta =  createConfigurationPropertyDelta(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
        		"whatever raw wherever");

    	// WHEN
    	displayWhen(TEST_NAME);
    	modelService.executeChanges(MiscSchemaUtil.createCollection(resourceDelta), ModelExecuteOptions.createRaw(),
    			task, result);

    	// THEN
    	displayThen(TEST_NAME);
    	assertSuccess(result);

    	PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
    	PrismAsserts.assertPropertyValue(resourceAfter,
    			getConfigurationPropertyPath(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME),
    			"whatever raw wherever");

    	assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
    }

    @Test
    public void test765ModifyConfigurationDiffExpressionRawPrismContextParse() throws Exception {
		final String TEST_NAME = "test765ModifyConfigurationDiffExpressionRawPrismContextParse";
		modifyConfigurationDiffExpressionRaw(TEST_NAME, xml -> prismContext.parseObject(xml));
    }

    /**
     * This is what GUI "Repository objects" page really does with XML.
     */
    @Test
    public void test767ModifyConfigurationDiffExpressionRawValidatorParse() throws Exception {
		final String TEST_NAME = "test767ModifyConfigurationDiffExpressionRawValidatorParse";
		modifyConfigurationDiffExpressionRaw(TEST_NAME, xml -> {
			final Holder<PrismObject<ResourceType>> objectHolder = new Holder<>();
			EventHandler handler = new EventHandler() {

				@Override
				public EventResult preMarshall(Element objectElement, Node postValidationTree,
						OperationResult objectResult) {
					return EventResult.cont();
				}

				@Override
				public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
						OperationResult objectResult) {
					objectHolder.setValue((PrismObject<ResourceType>) object);
					return EventResult.cont();
				}

				@Override
				public void handleGlobalError(OperationResult currentResult) {
				}
			};
			Validator validator = new Validator(prismContext, handler);
			validator.setVerbose(true);
			validator.setValidateSchema(false);
			OperationResult result = new OperationResult("validator");
			validator.validateObject(xml, result);
			return objectHolder.getValue();
		});
    }

    public void modifyConfigurationDiffExpressionRaw(final String TEST_NAME, FailableFunction<String, PrismObject<ResourceType>> parser) throws Exception {
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resourceBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        // just to improve readability
        resourceBefore.removeProperty(ObjectType.F_FETCH_RESULT);
        String serializedResource = prismContext.serializerFor(PrismContext.LANG_XML).serialize(resourceBefore);
        String modifiedResourceXml = serializedResource.replace("whatever raw wherever",
        		"<expression><const xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"c:ConstExpressionEvaluatorType\">useless</const></expression>");
        display("New resource XML", modifiedResourceXml);

        PrismObject<ResourceType> modifiedResource = parser.apply(modifiedResourceXml);
        display("New resource", modifiedResource);

        // just for fun
        String serializedModifiedResource = prismContext.serializerFor(PrismContext.LANG_XML).serialize(modifiedResource);
        assertNotNull(serializedModifiedResource);

        ObjectDelta<ResourceType> diffDelta = resourceBefore.diff(modifiedResource, true, true);
        display("Diff delta", diffDelta);

    	// WHEN
    	displayWhen(TEST_NAME);
    	modelService.executeChanges(MiscSchemaUtil.createCollection(diffDelta), ModelExecuteOptions.createRaw(), task, result);

    	// THEN
    	displayThen(TEST_NAME);
    	assertSuccess(result);

    	// Evaluate expression, re-apply configuration
    	OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OID, task);
    	TestUtil.assertSuccess("Dummy resource test", testResult);

    	assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
    	assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
    	assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
    	assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

    	PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
    	display("Resource after", resourceAfter);

		assertEquals("Wrong default useless string", IntegrationTestTools.CONST_USELESS, dummyResource.getUselessString());

		// TODO: strictly speaking, this should not be necessary.
		// But now the schema is re-parsed a bit more than is needed
    	assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
    }

    private ObjectDelta<ResourceType> createConfigurationPropertyDelta(QName elementQName, String newValue) {
    	ItemPath propPath = getConfigurationPropertyPath(elementQName);
		PrismPropertyDefinition<String> propDef = new PrismPropertyDefinitionImpl<>(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
				DOMUtil.XSD_STRING, prismContext);
		PropertyDelta<String> propDelta = PropertyDelta.createModificationReplaceProperty(propPath, propDef, newValue);
    	ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createModifyDelta(RESOURCE_DUMMY_OID, propDelta, ResourceType.class, prismContext);
    	display("Resource delta", resourceDelta);
    	return resourceDelta;
    }

    private ItemPath getConfigurationPropertyPath(QName elementQName) {
    	return new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
    			elementQName);
    }

	@Test
    public void test800GetResourceDummy() throws Exception {
		final String TEST_NAME = "test800GetResourceDummy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

		// WHEN
        displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);

		// THEN
		displayThen(TEST_NAME);
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT,  1);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
	}

	@Test
    public void test820SingleDescriptionModify() throws Exception {
		final String TEST_NAME = "test820SingleDescriptionModify";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

    	singleModify(descriptionAnt, -1, task, result);
    }

//	@Test
//    public void test835ModifySchemaHandling() throws Exception {
//    	final String TEST_NAME = "test835ModifySchemaHandling";
//    	displayTestTitle(TEST_NAME);
//
//    	Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//		CarefulAnt<ResourceType> ant = ants.get(1);
//		singleModify(ant, 0, task, result);
//    }

	@Test
    public void test840RandomModifySequence() throws Exception {
    	final String TEST_NAME = "test840RandomModifySequence";
    	displayTestTitle(TEST_NAME);

    	Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

    	for(int i=0; i <= MAX_RANDOM_SEQUENCE_ITERATIONS; i++) {
    		singleRandomModify(i, task, result);
    	}
    }

	private void singleRandomModify(int iteration, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
    	int i = rnd.nextInt(ants.size());
    	CarefulAnt<ResourceType> ant = ants.get(i);
    	singleModify(ant, iteration, task, result);
    }

    private void singleModify(CarefulAnt<ResourceType> ant, int iteration, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {

    	// GIVEN
    	ItemDelta<?,?> itemDelta = ant.createDelta(iteration);
		ObjectDelta<ResourceType> objectDelta = ObjectDelta.createModifyDelta(RESOURCE_DUMMY_OID, itemDelta, ResourceType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

		IntegrationTestTools.assertNoRepoCache();

		ModelExecuteOptions options = ModelExecuteOptions.createRaw();
		// WHEN
		modelService.executeChanges(deltas, options , task, result);

		// THEN
		IntegrationTestTools.assertNoRepoCache();
		Collection<SelectorOptions<GetOperationOptions>> getOptions = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, getOptions, task, result);
		SqlRepoTestUtil.assertVersionProgress(lastVersion, resourceAfter.getVersion());
        lastVersion = resourceAfter.getVersion();
        display("Version", lastVersion);

        Element xsdSchema = ResourceTypeUtil.getResourceXsdSchema(resourceAfter);
        if (xsdSchema != null) {
	        String targetNamespace = xsdSchema.getAttribute("targetNamespace");
	        assertNotNull("No targetNamespace in schema after application of "+objectDelta, targetNamespace);
        }

        IntegrationTestTools.assertNoRepoCache();

        ant.assertModification(resourceAfter, iteration);
    }

	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
	}
}
