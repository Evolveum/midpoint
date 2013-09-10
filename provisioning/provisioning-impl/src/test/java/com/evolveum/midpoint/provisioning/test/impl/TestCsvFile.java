/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.provisioning.test.impl;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ConnectorManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType.Host;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

/**
 * The test of Provisioning service on the API level. The test is using CSV resource.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestCsvFile extends AbstractIntegrationTest {

	private static final String FILENAME_RESOURCE_CSV = "src/test/resources/object/resource-csv.xml";
	private static final String RESOURCE_CSV_OID = "ef2bc95b-76e0-59e2-86d6-9999cccccccc";
	
	private static final String CSV_CONNECTOR_TYPE = "org.forgerock.openicf.csvfile.CSVFileConnector";
	
	private static final String CSV_SOURCE_FILE_PATH = "src/test/resources/midpoint-flatfile.csv";
	private static final String CSV_TARGET_FILE_PATH = "target/midpoint-flatfile.csv";

	private static final Trace LOGGER = TraceManager.getTrace(TestCsvFile.class);

	private PrismObject<ResourceType> resource;
	private ResourceType resourceType;
	
	@Autowired(required=true)
	private ProvisioningService provisioningService;
		

	/**
	 * @throws JAXBException
	 */
	public TestCsvFile() throws JAXBException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		resource = addResourceFromFile(FILENAME_RESOURCE_CSV, CSV_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();
		
		FileUtils.copyFile(new File(CSV_SOURCE_FILE_PATH), new File(CSV_TARGET_FILE_PATH));
	}

	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		TestUtil.displayTestTile("test000Integrity");
		
		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(TestCsvFile.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_OID,
				null, result).asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(connector);
		display("CSVFile Connector", connector);
		
		// Check connector schema
		ProvisioningTestUtil.assertConnectorSchemaSanity(connector, prismContext);
	}

	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test003Connection() throws ObjectNotFoundException, SchemaException {
		TestUtil.displayTestTile("test003Connection");
		// GIVEN
		OperationResult result = new OperationResult(TestCsvFile.class.getName()
				+ ".test003Connection");
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_OID,
				null, result).asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore
				.getConnectorRef().getOid(), null, result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_CSV_OID);

		// THEN
		display("Test result", testResult);
		TestUtil.assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable(); 
		display("Resource after test", resourceTypeRepoAfter);

		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		String resourceXml = prismContext.getPrismDomProcessor().serializeObjectToString(resourceRepoAfter);
		display("Resource XML", resourceXml);

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resourceBefore.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		// schema will be checked in next test
	}
	
	@Test
	public void test004Configuration() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTile("test004Configuration");
		// GIVEN
		OperationResult result = new OperationResult(TestCsvFile.class.getName()
				+ ".test004Configuration");

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, RESOURCE_CSV_OID, null, null, result);
		resourceType = resource.asObjectable();

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
		assertNotNull("No configuration container definition", confContDef);
		PrismContainer confingurationPropertiesContainer = 
			configurationContainer.findContainer(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
		PrismContainerDefinition confPropDef = confingurationPropertiesContainer.getDefinition();
		assertNotNull("No configuration properties container definition", confPropDef);
		
	}

	@Test
	public void test005ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		TestUtil.displayTestTile("test005ParsedSchema");
		// GIVEN
		OperationResult result = new OperationResult(TestCsvFile.class.getName()
				+ ".test005ParsedSchema");

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
		
		display("Parsed resource schema", returnedSchema);

		// Check whether it is reusing the existing schema and not parsing it all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching", returnedSchema == RefinedResourceSchema.getResourceSchema(resourceType, prismContext));
		
		IntegrationTestTools.assertIcfResourceSchemaSanity(returnedSchema, resourceType);

	}
	
	@Test
	public void test006Capabilities() throws Exception {
		final String TEST_NAME = "test006Capabilities";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+"."+TEST_NAME);

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_CSV_OID, null, null, result).asObjectable();
		
		// THEN
		display("Resource from provisioninig", resource);
		display("Resource from provisioninig (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject()));
		
		CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",nativeCapabilitiesList.isEmpty());
        
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it" ,capAct);
        
        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList, ScriptCapabilityType.class);
        assertNotNull("No script capability", capScript);
        List<Host> scriptHosts = capScript.getHost();
        assertEquals("Wrong number of script hosts", 2, scriptHosts.size());
        assertScriptHost(capScript, ProvisioningScriptHostType.RESOURCE);
        assertScriptHost(capScript, ProvisioningScriptHostType.CONNECTOR);
                
        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }
        
	}

	private void assertScriptHost(ScriptCapabilityType capScript, ProvisioningScriptHostType expectedHostType) {
		for (Host host: capScript.getHost()) {
			if (host.getType() == expectedHostType) {
				return;
			}
		}
		AssertJUnit.fail("No script capability with host type "+expectedHostType);
	}
	
	@Test
	public void test500ExeucuteScript() throws Exception {
		final String TEST_NAME = "test500ExeucuteScript";
		TestUtil.displayTestTile(TEST_NAME);
		
		String osName = System.getProperty("os.name");
		IntegrationTestTools.display("OS", osName);
		if (!"Linux".equals(osName)) {
			display("SKIPPING test, cannot execute on "+osName);
			return;
		}

		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		ProvisioningScriptType script = new ProvisioningScriptType();
		script.setHost(ProvisioningScriptHostType.RESOURCE);
		script.setLanguage("exec");
		script.setCode("src/test/script/csvscript.sh");
		ProvisioningScriptArgumentType argName = new ProvisioningScriptArgumentType();
		argName.setName("NAME");
		JAXBElement<Object> valueEvaluator = new ObjectFactory().createValue(null);
		Element domElement = DOMUtil.createElement(valueEvaluator.getName());
		domElement.setTextContent("World");
		valueEvaluator.setValue(domElement);
		argName.getExpressionEvaluator().add(valueEvaluator);
		script.getArgument().add(argName);

		// WHEN
		provisioningService.executeScript(RESOURCE_CSV_OID, script, task, result);
		
		// THEN
		result.computeStatus();
		display("executeScript result", result);
		TestUtil.assertSuccess("executeScript has failed (result)", result);
		
		File scriptOutFile = new File("target/hello.txt");
		assertTrue("Script haven't created the file", scriptOutFile.exists());
		String fileContent = MiscUtil.readFile(scriptOutFile);
		assertEquals("Wrong script output", "Hello World", fileContent);
		
	}
	
}
