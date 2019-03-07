/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.async;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AbstractWriteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 * @author mederly
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestAsyncUpdate extends AbstractProvisioningIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/async/");

	protected static final File RESOURCE_ASYNC_FILE = new File(TEST_DIR, "resource-async.xml");
	protected static final String RESOURCE_ASYNC_OID = "fb04d113-ebf8-41b4-b13b-990a597d110b";

	public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = new QName(MidPointConstants.NS_RI, "AccountObjectClass");

	protected static final String ASYNC_CONNECTOR_TYPE = "AsyncUpdateConnector";

	private static final Trace LOGGER = TraceManager.getTrace(TestAsyncUpdate.class);

	protected static final String NS_ASYNC_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/AsyncUpdateConnector";
	protected static final ItemName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new ItemName(NS_ASYNC_CONF, "defaultAssignee");

	protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
	protected static final String ACCOUNT_WILL_OID = "c1add81e-1df7-11e7-bbb7-5731391ba751";
	protected static final String ACCOUNT_WILL_USERNAME = "will";

	protected static final String ATTR_USERNAME = "username";
	protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

	protected static final String ATTR_FULLNAME = "fullname";
	protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

	protected static final String ATTR_DESCRIPTION = "description";
	protected static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);

	protected PrismObject<ResourceType> resource;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;

		super.initSystem(initTask, initResult);

		resource = addResourceFromFile(RESOURCE_ASYNC_FILE, ASYNC_CONNECTOR_TYPE, initResult);

		InternalsConfig.setSanityChecks(true);
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(TEST_NAME);

		assertNotNull("Resource is null", resource);

		OperationResult result = new OperationResult(TestAsyncUpdate.class.getName() + "." + TEST_NAME);

		ResourceType repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_ASYNC_OID, null, result).asObjectable();
		assertNotNull("No connector ref", repoResource.getConnectorRef());
		String connectorOid = repoResource.getConnectorRef().getOid();
		assertNotNull("No connector ref OID", connectorOid);
		ConnectorType repoConnector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(repoConnector);
		display("Async Connector", repoConnector);

		// Check connector schema
		IntegrationTestTools.assertConnectorSchemaSanity(repoConnector, prismContext);
	}

	@Test
	public void test003Connection() throws Exception {
		final String TEST_NAME = "test003Connection";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// Check that there is a schema, but no capabilities before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_ASYNC_OID,
				null, result).asObjectable();

		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);

		CapabilitiesType capabilities = resourceBefore.getCapabilities();
		if (capabilities != null) {
			AssertJUnit.assertNull("Native capabilities present before test connection. Bad test setup?", capabilities.getNative());
		}

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_ASYNC_OID, task);

		// THEN
		display("Test result", testResult);
		TestUtil.assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_ASYNC_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);

		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

		String resourceXml = prismContext.xmlSerializer().serialize(resourceRepoAfter);
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
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestAsyncUpdate.class.getName() + "." + TEST_NAME);

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, RESOURCE_ASYNC_OID, null, null, result);

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
		assertNotNull("No configuration container definition", confContDef);
//		PrismProperty<String> propDefaultAssignee = configurationContainer.findProperty(CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME);
//		assertNotNull("No defaultAssignee conf prop", propDefaultAssignee);

//		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
//		PrismContainerDefinition confPropDef = confingurationPropertiesContainer.getDefinition();
//		assertNotNull("No configuration properties container definition", confPropDef);

	}

	@Test
	public void test005ParsedSchema() throws Exception {
		final String TEST_NAME = "test005ParsedSchema";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestAsyncUpdate.class.getName() + "." + TEST_NAME);

		// THEN
		// The returned type should have the schema pre-parsed
		assertTrue(RefinedResourceSchemaImpl.hasParsedSchema(resource.asObjectable()));

		// Also test if the utility method returns the same thing
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource.asObjectable(), prismContext);

		display("Parsed resource schema", resourceSchema);

		// Check whether it is reusing the existing schema and not parsing it all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertSame("Broken caching", resourceSchema,
				RefinedResourceSchemaImpl.getResourceSchema(resource.asObjectable(), prismContext));

		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findObjectClassDefinition(RESOURCE_ACCOUNT_OBJECTCLASS);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());

		assertEquals("Unexpected number of definitions", getNumberOfAccountAttributeDefinitions(), accountDef.getDefinitions().size());
	}

	protected int getNumberOfAccountAttributeDefinitions() {
		return 3;
	}


}
