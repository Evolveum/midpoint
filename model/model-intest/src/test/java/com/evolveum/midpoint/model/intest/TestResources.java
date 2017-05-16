/*
 * Copyright (c) 2010-2017 Evolveum
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
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
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

import com.evolveum.icf.dummy.resource.DummyResource;
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
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
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
	
	private static List<CarefulAnt<ResourceType>> ants = new ArrayList<CarefulAnt<ResourceType>>();
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
		InternalMonitor.setTraceShadowFetchOperation(true);
		InternalMonitor.setTraceResourceSchemaOperations(true);
		InternalsConfig.encryptionChecks = false;
		
		InternalMonitor.setTracePrismObjectClone(true);
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test050GetResourceRaw() throws Exception {
		final String TEST_NAME = "test050GetResourceRaw";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        rememberPrismObjectCloneCount();
        
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options , task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
        
        display("Resource", resource);
        
        assertPrismObjectCloneIncrement(1);
        
		assertResourceDummy(resource, false);
        
        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchema(resource));
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(1);
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test052GetResourceNoFetch() throws Exception {
		final String TEST_NAME = "test052GetResourceNoFetch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        rememberPrismObjectCloneCount();
        
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				GetOperationOptions.createNoFetch());
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
				task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
        
        display("Resource", resource);
        
        assertPrismObjectCloneIncrement(1);
        
		assertResourceDummy(resource, false);
        
        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchema(resource));
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test053GetResourceNoFetchReadOnly() throws Exception {
		final String TEST_NAME = "test053GetResourceNoFetchReadOnly";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        rememberPrismObjectCloneCount();
        
        GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
				task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
        
        display("Resource", resource);
        
        assertPrismObjectCloneIncrement(0);
        
		assertResourceDummy(resource, false);
        
        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchema(resource));
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test100SearchResourcesNoFetch() throws Exception {
		final String TEST_NAME = "test100SearchResourcesNoFetch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertSteadyResources();
        rememberPrismObjectCloneCount();
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());
        
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);
        
        assertPrismObjectCloneIncrement(2);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource, false);
        }
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        assertSteadyResources();
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test102SearchResourcesNoFetchReadOnly() throws Exception {
		final String TEST_NAME = "test102SearchResourcesNoFetchReadOnly";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertSteadyResources();
        rememberPrismObjectCloneCount();
        
        GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());
        
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);
        
        assertPrismObjectCloneIncrement(0);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource, false);
        }
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        assertSteadyResources();
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test105SearchResourcesIterativeNoFetch() throws Exception {
		final String TEST_NAME = "test105SearchResourcesIterativeNoFetch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertSteadyResources();
        rememberPrismObjectCloneCount();

        final List<PrismObject<ResourceType>> resources = new ArrayList<PrismObject<ResourceType>>();
        		
        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
				assertResource(resource, false);
				resources.add(resource);
				return true;
			};
		
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
        modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());
        
        assertPrismObjectCloneIncrement(2);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        assertSteadyResources();
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test107SearchResourcesIterativeNoFetchReadOnly() throws Exception {
		final String TEST_NAME = "test107SearchResourcesIterativeNoFetchReadOnly";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertSteadyResources();
        rememberPrismObjectCloneCount();

        final List<PrismObject<ResourceType>> resources = new ArrayList<PrismObject<ResourceType>>();
        		
        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
				assertResource(resource, false);
				resources.add(resource);
				return true;
			};
		
		GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());
        
        assertPrismObjectCloneIncrement(2);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        assertSteadyResources();
	}
	
	@Test
    public void test110GetResourceDummy() throws Exception {
		final String TEST_NAME = "test110GetResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        rememberPrismObjectCloneCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        assertPrismObjectCloneIncrement(4);
        
        assertResourceDummy(resource, true);
        
        assertResourceSchemaFetchIncrement(1);
        assertResourceSchemaParseCountIncrement(1);
        assertConnectorCapabilitiesFetchIncrement(1);
		assertConnectorInitializationCountIncrement(1);
        assertConnectorSchemaParseIncrement(0);
        
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
        
        assertEquals("Wrong dummy useless string", RESOURCE_DUMMY_USELESS_STRING, dummyResource.getUselessString());
	}
	
	@Test
    public void test112GetResourceDummyReadOnly() throws Exception {
		final String TEST_NAME = "test112GetResourceDummyReadOnly";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        rememberPrismObjectCloneCount();
        
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				GetOperationOptions.createReadOnly());
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, 
				options , task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        assertPrismObjectCloneIncrement(1);
        
        assertResourceDummy(resource, true);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
	}
	
	
	@Test
    public void test120SearchResources() throws Exception {
		final String TEST_NAME = "test120SearchResources";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertSteadyResources();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, null, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());
        
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource, true);
        }
        
        assertResourceSchemaFetchIncrement(1);
        assertResourceSchemaParseCountIncrement(1);
        assertConnectorCapabilitiesFetchIncrement(1);
		assertConnectorInitializationCountIncrement(1);
        assertConnectorSchemaParseIncrement(0);
	}
	
	@Test
    public void test125SearchResourcesIterative() throws Exception {
		final String TEST_NAME = "test125SearchResourcesIterative";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertSteadyResources();

        final List<PrismObject<ResourceType>> resources = new ArrayList<PrismObject<ResourceType>>();
        		
        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
				assertResource(resource, true);
				resources.add(resource);
				return true;
			};
        
		// WHEN
        modelService.searchObjectsIterative(ResourceType.class, null, handler, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);

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
        TestUtil.displayTestTile(this, TEST_NAME);

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
		
        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
        
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
	}
	
	/**
	 * Red resource has an expression for uselessString configuration property. Check that.
	 */
	@Test
    public void test210GetResourceDummyRed() throws Exception {
		final String TEST_NAME = "test210GetResourceDummyRed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        rememberPrismObjectCloneCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_RED_OID, null , task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        assertSuccess(result);

        assertPrismObjectCloneIncrement(1);
        
        assertResourceDummy(resource, true);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
        
        assertEquals("Wrong RED useless string", RESOURCE_DUMMY_RED_USELESS_STRING, dummyResourceRed.getUselessString());
	}
	
    @Test
    public void test750GetResourceRaw() throws Exception {
		final String TEST_NAME = "test750GetResourceRaw";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        // precondition
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        rememberPrismObjectCloneCount();
        
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options , task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
        
        display("Resource", resource);
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
        
        assertPrismObjectCloneIncrement(1);
        
		assertResourceDummy(resource, true);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
	}
    
    @Test
    public void test752GetResourceDummy() throws Exception {
		final String TEST_NAME = "test752GetResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        rememberPrismObjectCloneCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertPrismObjectCloneIncrement(1);
        
        assertResourceDummy(resource, true);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
	}
    
    @Test
    public void test760ModifyConfiguration() throws Exception {
		final String TEST_NAME = "test760ModifyConfiguration";
        TestUtil.displayTestTile(this, TEST_NAME);
    	
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ItemPath propPath = new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
        		IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME);
		PrismPropertyDefinition<String> propDef = new PrismPropertyDefinitionImpl<>(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
				DOMUtil.XSD_STRING, prismContext);
		PropertyDelta<String> propDelta = PropertyDelta.createModificationReplaceProperty(propPath, propDef, "whatever wherever");
    	ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createModifyDelta(RESOURCE_DUMMY_OID, propDelta, ResourceType.class, prismContext);
    	display("Resource delta", resourceDelta);
    	Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
    	
    	// WHEN
    	TestUtil.displayWhen(TEST_NAME);
    	modelService.executeChanges(deltas, null, task, result);
    	
    	// THEN
    	TestUtil.displayThen(TEST_NAME);
    	result.computeStatus();
    	TestUtil.assertSuccess(result);
    	
    	PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
    	PrismAsserts.assertPropertyValue(resourceAfter, propPath, "whatever wherever");
    	
    }
	
	@Test
    public void test800GetResourceDummy() throws Exception {
		final String TEST_NAME = "test800GetResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
        rememberPrismObjectCloneCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertPrismObjectCloneIncrement(1);
        
        assertResourceDummy(resource, true);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(1);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
	}
	
	@Test
    public void test820SingleDescriptionModify() throws Exception {
		final String TEST_NAME = "test820SingleDescriptionModify";
        TestUtil.displayTestTile(this, TEST_NAME);
    	
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
    	singleModify(descriptionAnt, -1, task, result);
    }
	
	@Test
    public void test840RadomModifySequence() throws Exception {
    	final String TEST_NAME = "test840RadomModifySequence";
    	TestUtil.displayTestTile(this, TEST_NAME);
    	
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
        rememberShadowFetchOperationCount();
	}
}
