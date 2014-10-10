/*
 * Copyright (c) 2010-2014 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.sql.testing.CarefulAnt;
import com.evolveum.midpoint.repo.sql.testing.ResourceCarefulAntUtil;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestResources extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static final int MAX_RANDOM_SEQUENCE_ITERATIONS = 15;
	
	private static List<CarefulAnt<ResourceType>> ants = new ArrayList<CarefulAnt<ResourceType>>();
	private static CarefulAnt<ResourceType> descriptionAnt;
	private static String lastVersion;
	private static Random rnd = new Random();
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		ResourceCarefulAntUtil.initAnts(ants, RESOURCE_DUMMY_FILE, prismContext);
		descriptionAnt = ants.get(0);
		// get resource to make sure it has generated schema
		modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, initTask, initResult);
		InternalMonitor.reset();
		InternalMonitor.setTraceShadowFetchOperation(true);
		InternalMonitor.setTraceResourceSchemaOperations(true);
	}
	
	@Test
    public void test100GetResource() throws Exception {
		final String TEST_NAME = "test100GetResource";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
		// WHEN
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);
		
		// THEN
		assertResourceDummy(resource);
		
        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
        
        assertSteadyResources();
	}
	
	@Test
    public void test110SearchResources() throws Exception {
		final String TEST_NAME = "test110SearchResources";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        
		// WHEN
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, null, task, result);

		// THEN
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 8, resources.size());
        
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource);
        }
        
        assertSteadyResources();
	}
	
	@Test
    public void test120SearchResourcesIterative() throws Exception {
		final String TEST_NAME = "test120SearchResourcesIterative";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);        

        final List<PrismObject<ResourceType>> resources = new ArrayList<PrismObject<ResourceType>>();
        		
        ResultHandler<ResourceType> handler = new ResultHandler<ResourceType>() {
			@Override
			public boolean handle(PrismObject<ResourceType> resource, OperationResult parentResult) {
				try {
					assertResource(resource);
				} catch (JAXBException e) {
					throw new RuntimeException(e.getMessage(),e);
				}
				resources.add(resource);
				return true;
			}
		};
        
		// WHEN
        modelService.searchObjectsIterative(ResourceType.class, null, handler, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects result", result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 8, resources.size());
        
        assertSteadyResources();
	}
	
	private void assertResourceDummy(PrismObject<ResourceType> resource) throws JAXBException {
		assertResource(resource);
		PrismContainer<ConnectorConfigurationType> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition = configurationContainer.getDefinition();
		
	}
	
	private void assertResource(PrismObject<ResourceType> resource) throws JAXBException {
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
		
		// Try to marshal using pure JAXB as a rough test that it is OK JAXB-wise
        // skipped because of parsing changes [pm]
//		Element resourceDomElement = prismContext.getPrismJaxbProcessor().marshalJaxbObjectToDom(resource.asObjectable(), new QName(SchemaConstants.NS_C, "resource"),
//				DOMUtil.getDocument());
//		display("Resouce DOM element after JAXB marshall", resourceDomElement);
	}

	@Test
    public void test200GetResourceRaw() throws Exception {
		final String TEST_NAME = "test200GetResourceRaw";
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
	}
	
	
	@Test
    public void test820SingleDescriptionModify() throws Exception {
		final String TEST_NAME = "test820SingleDescriptionModify";
        TestUtil.displayTestTile(this, TEST_NAME);
    	
        Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
    	singleModify(descriptionAnt, -1, task, result);
    }
	
	@Test
    public void test840RadomModifySequence() throws Exception {
    	final String TEST_NAME = "test840RadomModifySequence";
    	TestUtil.displayTestTile(this, TEST_NAME);
    	
    	Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
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
    	ItemDelta<?> itemDelta = ant.createDelta(iteration);
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
        
        IntegrationTestTools.assertNoRepoCache();
        
        ant.assertModification(resourceAfter, iteration);
    }
    
    @Test
    public void test850ModifyConfiguration() throws Exception {
		final String TEST_NAME = "test850ModifyConfiguration";
        TestUtil.displayTestTile(this, TEST_NAME);
    	
        Task task = taskManager.createTaskInstance(TestResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ItemPath propPath = new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, 
        		IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME);
		PrismPropertyDefinition<String> propDef = new PrismPropertyDefinition<String>(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
				DOMUtil.XSD_STRING, prismContext);
		PropertyDelta<String> propDelta = PropertyDelta.createModificationReplaceProperty(propPath, propDef, "whatever wherever");
    	ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createModifyDelta(RESOURCE_DUMMY_OID, propDelta, ResourceType.class, prismContext);
    	Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
    	
    	// WHEN
    	modelService.executeChanges(deltas, null, task, result);
    	
    	// THEN
    	result.computeStatus();
    	TestUtil.assertSuccess(result);
    	
    	PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
    	PrismAsserts.assertPropertyValue(resourceAfter, propPath, "whatever wherever");
    	
    }

	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        rememberShadowFetchOperationCount();
	}
}
