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
package com.evolveum.midpoint.provisioning.impl.csv;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;

/**
 * The test of Provisioning service on the API level. The test is using CSV resource.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class AbstractCsvTest extends AbstractProvisioningIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/csv/");
	
	protected static final String RESOURCE_NS = MidPointConstants.NS_RI;
	
	public static final QName RESOURCE_CSV_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_NS, "AccountObjectClass");
	
	protected static final String CSV_CONNECTOR_TYPE = "com.evolveum.polygon.connector.csv.CsvConnector";
	
	private static final File CSV_TARGET_FILE = new File("target/midpoint.csv");
	
	protected static final String ACCOUNT_JACK_FIRSTNAME = "Jack";
	protected static final String ACCOUNT_JACK_LASTNAME = "Sparrow";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractCsvTest.class);

	protected static final String ATTR_FIRSTNAME = "firstname";
	protected static final QName ATTR_FIRSTNAME_QNAME = new QName(RESOURCE_NS, ATTR_FIRSTNAME);
	
	protected static final String ATTR_LASTNAME = "lastname";
	protected static final QName ATTR_LASTNAME_QNAME = new QName(RESOURCE_NS, ATTR_LASTNAME);

	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		resource = addResourceFromFile(getResourceFile(), CSV_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();
		
		FileUtils.copyFile(getSourceCsvFile(), CSV_TARGET_FILE);
	}

	protected abstract File getResourceFile();
	
	protected abstract String getResourceOid();
	
	protected abstract File getSourceCsvFile();
	
	protected abstract File getAccountJackFile();
	
	protected abstract String getAccountJackOid();

	@Test
	public void test000Integrity() throws Exception {
		final String TEST_NAME = "test000Integrity";
		TestUtil.displayTestTile(TEST_NAME);
		
		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(AbstractCsvTest.class.getName()
				+ "." + TEST_NAME);

		ResourceType resource = repositoryService.getObject(ResourceType.class, getResourceOid(),
				null, result).asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(connector);
		display("CSVFile Connector", connector);
		
		// Check connector schema
		IntegrationTestTools.assertConnectorSchemaSanity(connector, prismContext);
	}

	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test003Connection() throws Exception {
		final String TEST_NAME = "test003Connection";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(AbstractCsvTest.class.getName()
				+ "." + TEST_NAME);
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, getResourceOid(),
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
		OperationResult testResult = provisioningService.testResource(getResourceOid());

		// THEN
		display("Test result", testResult);
		TestUtil.assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, getResourceOid(), null, result);
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
	public void test004Configuration() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTile("test004Configuration");
		// GIVEN
		OperationResult result = new OperationResult(AbstractCsvTest.class.getName()
				+ ".test004Configuration");

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, null, result);
		resourceType = resource.asObjectable();

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
		assertNotNull("No configuration container definition", confContDef);
		PrismContainer confingurationPropertiesContainer = 
			configurationContainer.findContainer(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
		PrismContainerDefinition confPropDef = confingurationPropertiesContainer.getDefinition();
		assertNotNull("No configuration properties container definition", confPropDef);
		
	}

	@Test
	public void test005ParsedSchema() throws Exception {
		final String TEST_NAME = "test005ParsedSchema";
		TestUtil.displayTestTile(TEST_NAME);

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchemaImpl.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext);
		
		display("Parsed resource schema", resourceSchema);
		assertNotNull("No resource schema", resourceSchema);

		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findObjectClassDefinition(RESOURCE_CSV_ACCOUNT_OBJECTCLASS);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		assertAccountDefinition(accountDef);
		
		ResourceAttributeDefinition<String> icfsNameDef = accountDef.findAttributeDefinition(SchemaConstants.ICFS_NAME);
		assertNull("ICFS NAME definition sneaked in", icfsNameDef);
		
		ResourceAttributeDefinition<String> icfsUidDef = accountDef.findAttributeDefinition(SchemaConstants.ICFS_UID);
		assertNull("ICFS UID definition sneaked in", icfsUidDef);
		
		// Check whether it is reusing the existing schema and not parsing it all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching", resourceSchema == RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext));

	}
	
	protected abstract void assertAccountDefinition(ObjectClassComplexTypeDefinition accountDef);

	@Test
	public void test006Capabilities() throws Exception {
		final String TEST_NAME = "test006Capabilities";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDj.class.getName()+"."+TEST_NAME);

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, null, result).asObjectable();
		
		// THEN
		display("Resource from provisioninig", resource);
		display("Resource from provisioninig (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject(), PrismContext.LANG_XML));
		
		CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",nativeCapabilitiesList.isEmpty());
        
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it" ,capAct);
        
        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList, ScriptCapabilityType.class);
        assertNotNull("No script capability", capScript);
        List<Host> scriptHosts = capScript.getHost();
        assertEquals("Wrong number of script hosts", 1, scriptHosts.size());
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
	public void test100AddAccountJack() throws Exception {
		final String TEST_NAME = "test100AddAccountJack";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ShadowType> shadowBefore = parseObject(getAccountJackFile());
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.addObject(shadowBefore, null, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertSuccess(result);		
	}
	
	@Test
	public void test110GetAccountJack() throws Exception {
		final String TEST_NAME = "test110GetAccountJack";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, getAccountJackOid(), null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertNotNull(shadow);

		display("Shadow after", shadow);
		
		ShadowType shadowType = shadow.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", "jack", shadow.getName());
        assertNotNull(shadow.getOid());
        assertEquals(RESOURCE_CSV_ACCOUNT_OBJECTCLASS, shadowType.getObjectClass());
        assertEquals(getResourceOid(), shadowType.getResourceRef().getOid());
        assertAccountJackAttributes(shadowType);
        assertNotNull("Missing activation", shadowType.getActivation());
        assertNotNull("Missing activation status", shadowType.getActivation().getAdministrativeStatus());
        assertEquals("Not enabled", ActivationStatusType.ENABLED, shadowType.getActivation().getAdministrativeStatus());
		
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadow.getOid(), null, result);
        ShadowType repoShadowType = repoShadow.asObjectable();
        assertEquals(RESOURCE_CSV_ACCOUNT_OBJECTCLASS, repoShadowType.getObjectClass());
        assertEquals(getResourceOid(), repoShadowType.getResourceRef().getOid());
        assertAccountJackAttributesRepo(shadowType);
        
	}

	protected abstract void assertAccountJackAttributes(ShadowType shadowType);
	
	protected abstract void assertAccountJackAttributesRepo(ShadowType shadowType);

}
