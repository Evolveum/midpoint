/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
 * The test of Provisioning service on the API level. The test is using dummy
 * resource WITHOUT A SCHEMA. It checks if the system is still able to basically operate.
 * Even though the resource will not be usable until the schema is specified manually.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml", "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml", "classpath:application-context-repository.xml",
		"classpath:application-context-repo-cache.xml", "classpath:application-context-configuration-test.xml" })
@DirtiesContext
public class TestProvisioningDummySchemalessNoSchema extends AbstractIntegrationTest {

	private static final String TEST_DIR = "src/test/resources/impl/dummy/";

	private static final String FILENAME_RESOURCE_DUMMY = ProvisioningTestUtil.COMMON_TEST_DIR_FILENAME + "resource-dummy-schemaless-no-schema.xml";
	private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0000";

	private static final Trace LOGGER = TraceManager.getTrace(TestProvisioningDummySchemalessNoSchema.class);

	private PrismObject<ResourceType> resource;
	private ResourceType resourceType;
	private static DummyResource dummyResource;
	private static Task syncTask;

	@Autowired(required = true)
	private ProvisioningService provisioningService;

	/**
	 * @throws JAXBException
	 */
	public TestProvisioningDummySchemalessNoSchema() throws JAXBException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		resource = addResourceFromFile(FILENAME_RESOURCE_DUMMY, ProvisioningTestUtil.DUMMY_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();

		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();

	}

	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");

		display("Dummy resource instance", dummyResource.toString());

		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(TestProvisioningDummySchemalessNoSchema.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result)
				.asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, result).asObjectable();
		assertNotNull(connector);
		display("Dummy Connector", connector);

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
		displayTestTile("test003Connection");
		// GIVEN
		OperationResult result = new OperationResult(TestProvisioningDummySchemalessNoSchema.class.getName()
				+ ".test003Connection");
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result)
				.asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class,
				resourceBefore.getConnectorRef().getOid(), result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);

		// THEN
		display("Test result", testResult);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONFIGURATION_VALIDATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertTestResourceFailure(testResult, ConnectorTestOperation.CONNECTOR_SCHEMA);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_OID, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
	}

	/**
	 * This basically checks if the methods do not die on NPE
	 */
	@Test
	public void test005ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException {
		displayTestTile("test005ParsedSchema");
		// GIVEN
		OperationResult result = new OperationResult(TestProvisioningDummySchemalessNoSchema.class.getName()
				+ ".test005ParsedSchema");

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);

		display("Parsed resource schema", returnedSchema);
		
		assertNull("Unexpected schema after parsing", returnedSchema);
	}

	@Test
	public void test004GetObject() throws Exception {
		displayTestTile("test004GetObject");
		// GIVEN
		OperationResult result = new OperationResult(TestProvisioningDummySchemalessNoSchema.class.getName()
				+ ".test004GetObject");

		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		assertNotNull("Resource is null", resource);
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("No connector ref", resourceType.getConnectorRef());
		assertNotNull("No connector ref OID", resourceType.getConnectorRef().getOid());		
	}
	
}
