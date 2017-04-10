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

/**
 * 
 */
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestManual extends AbstractProvisioningIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/manual/");
	
	private static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
	private static final String RESOURCE_MANUAL_OID = "8a8e19de-1a14-11e7-965f-6f995b457a8b";
	
	private static final String MANUAL_CONNECTOR_TYPE = "ManualConnector";
	
	private static final Trace LOGGER = TraceManager.getTrace(TestManual.class);

	private static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/ManualConnector";
	private static final QName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new QName(NS_MANUAL_CONF, "defaultAssignee");

	private PrismObject<ResourceType> resource;
	private ResourceType resourceType;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		resource = addResourceFromFile(RESOURCE_MANUAL_FILE, MANUAL_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTile(TEST_NAME);
		
		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(TestManual.class.getName()
				+ "." + TEST_NAME);

		ResourceType repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_MANUAL_OID,
				null, result).asObjectable();
		assertNotNull("No connector ref", repoResource.getConnectorRef());
		String connectorOid = repoResource.getConnectorRef().getOid();
		assertNotNull("No connector ref OID", connectorOid);
		ConnectorType repoConnector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(repoConnector);
		display("Manual Connector", repoConnector);
		
		// Check connector schema
		IntegrationTestTools.assertConnectorSchemaSanity(repoConnector, prismContext);
	}

	@Test
	public void test003Connection() throws Exception {
		final String TEST_NAME = "test003Connection";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestManual.class.getName() + "." + TEST_NAME);
		
		// Check that there is a schema, but no capabilities before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_MANUAL_OID,
				null, result).asObjectable();
		
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNotNull("No schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);
		
		CapabilitiesType capabilities = resourceBefore.getCapabilities();
		AssertJUnit.assertNull("Capabilities present before test connection. Bad test setup?", capabilities);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_MANUAL_OID);

		// THEN
		display("Test result", testResult);
		TestUtil.assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_MANUAL_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable(); 
		display("Resource after test", resourceTypeRepoAfter);

		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		String resourceXml = prismContext.serializeObjectToString(resourceRepoAfter, PrismContext.LANG_XML);
		display("Resource XML", resourceXml);

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchemaImpl.parse(xsdElement, resourceBefore.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		// schema will be checked in next test
	}
	
	@Test
	public void test004Configuration() throws Exception {
		final String TEST_NAME = "test004Configuration";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestManual.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, RESOURCE_MANUAL_OID, null, null, result);
		resourceType = resource.asObjectable();

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
		assertNotNull("No configuration container definition", confContDef);
		PrismProperty<String> propDefaultAssignee = configurationContainer.findProperty(CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME);
		assertNotNull("No defaultAssignee conf prop", propDefaultAssignee);
		
//		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
//		PrismContainerDefinition confPropDef = confingurationPropertiesContainer.getDefinition();
//		assertNotNull("No configuration properties container definition", confPropDef);
		
	}

	@Test
	public void test005ParsedSchema() throws Exception {
		final String TEST_NAME = "test005ParsedSchema";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestManual.class.getName() + "." + TEST_NAME);

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchemaImpl.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext);
		
		display("Parsed resource schema", returnedSchema);

		// Check whether it is reusing the existing schema and not parsing it all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching", returnedSchema == RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext));

		// TODO: assert schema
	}
	
	@Test
	public void test006Capabilities() throws Exception {
		final String TEST_NAME = "test006Capabilities";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDj.class.getName()+"."+TEST_NAME);

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_MANUAL_OID, null, null, result).asObjectable();
		
		// THEN
		display("Resource from provisioninig", resource);
		display("Resource from provisioninig (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject(), PrismContext.LANG_XML));
		
		CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",nativeCapabilitiesList.isEmpty());

        CreateCapabilityType capCreate = CapabilityUtil.getCapability(nativeCapabilitiesList, CreateCapabilityType.class);
        assertNotNull("Missing create capability", capCreate);
        
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNotNull("Missing activation capability", capAct);
        
        ReadCapabilityType capRead = CapabilityUtil.getCapability(nativeCapabilitiesList, ReadCapabilityType.class);
        assertNull("Found read capability while not expecting it" ,capRead);
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }
        
	}

	
}
