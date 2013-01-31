/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertProvisioningAccountShadow;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
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
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummy extends AbstractDummyTest {

	private static final String BLACKBEARD_USERNAME = "blackbeard";
	private static final String DRAKE_USERNAME = "drake";

	private static final Trace LOGGER = TraceManager.getTrace(TestDummy.class);
	
//	private Task syncTask = null;
	private CachingMetadataType capabilitiesCachingMetadataType;

	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");

		display("Dummy resource instance", dummyResource.toString());

		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result)
				.asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, result).asObjectable();
		assertNotNull(connector);
		display("Dummy Connector", connector);
		
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);
		
		

		// Check connector schema
		ProvisioningTestUtil.assertConnectorSchemaSanity(connector, prismContext);
	}

	/**
	 * Check whether the connectors were discovered correctly and were added to
	 * the repository.
	 * 
	 * @throws SchemaException
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
		displayTestTile("test001Connectors");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test001Connectors");

		// WHEN
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class,
				new ObjectQuery(), result);

		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);

		assertFalse("No connector found", connectors.isEmpty());
		for (PrismObject<ConnectorType> connPrism : connectors) {
			ConnectorType conn = connPrism.asObjectable();
			display("Found connector " + conn, conn);

			display("XML " + conn, PrismTestUtil.serializeObjectToString(connPrism));

			XmlSchemaType xmlSchemaType = conn.getSchema();
			assertNotNull("xmlSchemaType is null", xmlSchemaType);
			Element connectorXsdSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(conn);
			assertNotNull("No schema", connectorXsdSchemaElement);

			// Try to parse the schema
			PrismSchema schema = PrismSchema.parse(connectorXsdSchemaElement, "connector schema " + conn, prismContext);
			assertNotNull("Cannot parse schema", schema);
			assertFalse("Empty schema", schema.isEmpty());

			display("Parsed connector schema " + conn, schema);

			QName configurationElementQname = new QName(conn.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
			PrismContainerDefinition configurationContainer = schema
					.findContainerDefinitionByElementName(configurationElementQname);
			assertNotNull("No " + configurationElementQname + " element in schema of " + conn, configurationContainer);
			PrismContainerDefinition definition = schema.findItemDefinition(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(),
					PrismContainerDefinition.class);
			assertNotNull("Definition of <configuration> property container not found", definition);
			PrismContainerDefinition pcd = (PrismContainerDefinition) definition;
			assertFalse("Empty definition", pcd.isEmpty());
		}
	}

	/**
	 * Running discovery for a second time should return nothing - as nothing
	 * new was installed in the meantime.
	 */
	@Test
	public void test002ConnectorRediscovery() {
		displayTestTile("test002ConnectorRediscovery");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test002ConnectorRediscovery");

		// WHEN
		Set<ConnectorType> discoverLocalConnectors = connectorTypeManager.discoverLocalConnectors(result);

		// THEN
		result.computeStatus();
		display("discoverLocalConnectors result", result);
		assertSuccess("discoverLocalConnectors failed", result);
		assertTrue("Rediscovered something", discoverLocalConnectors.isEmpty());
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
		OperationResult result = new OperationResult(TestDummy.class.getName()
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
		assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_OID, result);
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
	public void test004Configuration() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {
		displayTestTile("test004Configuration");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test004Configuration");

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		resourceType = resource.asObjectable();
		
		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
		assertNotNull("No configuration container definition", confContDef);
		PrismContainer confingurationPropertiesContainer = configurationContainer
				.findContainer(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
		PrismContainerDefinition confPropsDef = confingurationPropertiesContainer.getDefinition();
		assertNotNull("No configuration properties container definition", confPropsDef);
		List<PrismProperty<?>> configurationProperties = confingurationPropertiesContainer.getValue().getItems();
		assertFalse("No configuration properties", configurationProperties.isEmpty());
		for (PrismProperty<?> confProp : configurationProperties) {
			PrismPropertyDefinition confPropDef = confProp.getDefinition();
			assertNotNull("No definition for configuration property " + confProp, confPropDef);
			assertFalse("Configuration property " + confProp + " is raw", confProp.isRaw());
		}
		
		// The useless configuration variables should be reflected to the resource now
		assertEquals("Wrong useless string", "Shiver me timbers!", dummyResource.getUselessString());
		assertEquals("Wrong guarded useless string", "Dead men tell no tales", dummyResource.getUselessGuardedString());
		
		resource.checkConsistence();
	}

	@Test
	public void test005ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException {
		displayTestTile("test005ParsedSchema");
		// GIVEN

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);

		display("Parsed resource schema", returnedSchema);

		// Check whether it is reusing the existing schema and not parsing it
		// all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching",
				returnedSchema == RefinedResourceSchema.getResourceSchema(resourceType, prismContext));

		ProvisioningTestUtil.assertDummyResourceSchemaSanity(returnedSchema, resourceType);

	}

	@Test
	public void test006RefinedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException {
		displayTestTile("test006RefinedSchema");
		// GIVEN

		// WHEN
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType, prismContext);
		display("Refined schema", refinedSchema);

		// Check whether it is reusing the existing schema and not parsing it
		// all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching",
				refinedSchema == RefinedResourceSchema.getRefinedSchema(resourceType, prismContext));

		RefinedAccountDefinition accountDef = refinedSchema.getDefaultAccountDefinition();
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canCreate());
		assertFalse("UID has update", uidDef.canUpdate());
		assertTrue("No UID read", uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canCreate());
		assertTrue("No NAME update", nameDef.canUpdate());
		assertTrue("No NAME read", nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canCreate());
		assertTrue("No fullname update", fullnameDef.canUpdate());
		assertTrue("No fullname read", fullnameDef.canRead());

		assertNull("The _PASSSWORD_ attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

	}

	@Test
	public void test006Capabilities() throws Exception {
		displayTestTile("test006Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test006Capabilities");

		// WHEN
		ResourceType resourceType = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result)
				.asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		// Check native capabilities
		CapabilityCollectionType nativeCapabilities = resourceType.getCapabilities().getNative();
		System.out.println("Native capabilities: " + PrismTestUtil.marshalWrap(nativeCapabilities));
		System.out.println("resource: " + resourceType.asPrismObject().dump());
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
		CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				CredentialsCapabilityType.class);
		assertNotNull("password native capability not present", capCred.getPassword());
		ActivationCapabilityType capAct = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				ActivationCapabilityType.class);
		assertNotNull("native activation capability not present", capAct);
		assertNotNull("native activation/enabledisable capability not present", capAct.getEnableDisable());
		TestConnectionCapabilityType capTest = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				ScriptCapabilityType.class);
		assertNotNull("native script capability not present", capScript);
		assertNotNull("No host in native script capability", capScript.getHost());
		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
		// TODO: better look inside
		
		capabilitiesCachingMetadataType = resourceType.getCapabilities().getCachingMetadata();
		assertNotNull("No capabilities caching metadata", capabilitiesCachingMetadataType);
		assertNotNull("No capabilities caching metadata timestamp", capabilitiesCachingMetadataType.getRetrievalTimestamp());
		assertNotNull("No capabilities caching metadata serial number", capabilitiesCachingMetadataType.getSerialNumber());

		// Check effective capabilites
		capCred = ResourceTypeUtil.getEffectiveCapability(resourceType, CredentialsCapabilityType.class);
		assertNotNull("password capability not found", capCred.getPassword());
		// Although connector does not support activation, the resource
		// specifies a way how to simulate it.
		// Therefore the following should succeed
		capAct = ResourceTypeUtil.getEffectiveCapability(resourceType, ActivationCapabilityType.class);
		assertNotNull("activation capability not found", capCred.getPassword());

		List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resourceType);
		for (Object capability : effectiveCapabilities) {
			System.out.println("Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : "
					+ capability);
		}
	}
	
	/**
	 * Check if the cached native capabilities were properly stored in the repo 
	 */
	@Test
	public void test007CapabilitiesRepo() throws Exception {
		displayTestTile("test007CapabilitiesRepo");

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test007CapabilitiesRepo");

		// WHEN
		PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);;

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		// Check native capabilities
		ResourceType resourceType = resource.asObjectable();
		CapabilitiesType capabilitiesType = resourceType.getCapabilities();
		assertNotNull("No capabilities in repo, the capabilities were not cached", capabilitiesType);
		CapabilityCollectionType nativeCapabilities = capabilitiesType.getNative();
		System.out.println("Native capabilities: " + PrismTestUtil.marshalWrap(nativeCapabilities));
		System.out.println("resource: " + resourceType.asPrismObject().dump());
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
		CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				CredentialsCapabilityType.class);
		assertNotNull("password native capability not present", capCred.getPassword());
		ActivationCapabilityType capAct = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				ActivationCapabilityType.class);
		assertNotNull("native activation capability not present", capAct);
		assertNotNull("native activation/enabledisable capability not present", capAct.getEnableDisable());
		TestConnectionCapabilityType capTest = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				ScriptCapabilityType.class);
		assertNotNull("native script capability not present", capScript);
		assertNotNull("No host in native script capability", capScript.getHost());
		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
		// TODO: better look inside

		CachingMetadataType repoCapabilitiesCachingMetadataType = capabilitiesType.getCachingMetadata();
		assertNotNull("No repo capabilities caching metadata", repoCapabilitiesCachingMetadataType);
		assertNotNull("No repo capabilities caching metadata timestamp", repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
		assertNotNull("No repo capabilities caching metadata serial number", repoCapabilitiesCachingMetadataType.getSerialNumber());
		assertEquals("Repo capabilities caching metadata timestamp does not match previously returned value", 
				capabilitiesCachingMetadataType.getRetrievalTimestamp(), repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
		assertEquals("Repo capabilities caching metadata serial does not match previously returned value", 
				capabilitiesCachingMetadataType.getSerialNumber(), repoCapabilitiesCachingMetadataType.getSerialNumber());

	}

	@Test
	public void test010ResourceAndConnectorCaching() throws Exception {
		displayTestTile("test010ResourceAndConnectorCaching");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test010ResourceAndConnectorCaching");
		ConnectorInstance configuredConnectorInstance = connectorTypeManager.getConfiguredConnectorInstance(
				resourceType, false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);

		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain
				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));

		// Check resource schema caching
		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
		
		// Check capabilities caching
		
		CapabilitiesType capabilitiesType = resourceType.getCapabilities();
		assertNotNull("No capabilities fetched from provisioning", capabilitiesType);
		CachingMetadataType capCachingMetadataType = capabilitiesType.getCachingMetadata();
		assertNotNull("No capabilities caching metadata fetched from provisioning", capCachingMetadataType);
		CachingMetadataType capCachingMetadataTypeAgain = resourceTypeAgain.getCapabilities().getCachingMetadata();
		assertEquals("Capabilities caching metadata serial number has changed", capCachingMetadataType.getSerialNumber(), 
				capCachingMetadataTypeAgain.getSerialNumber());
		assertEquals("Capabilities caching metadata timestamp has changed", capCachingMetadataType.getRetrievalTimestamp(), 
				capCachingMetadataTypeAgain.getRetrievalTimestamp());
		
		// Rough test if everything is fine
		resource.asObjectable().setFetchResult(null);
		resourceAgain.asObjectable().setFetchResult(null);
		ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(resource, resourceAgain);
        display("Dummy resource diff", dummyResourceDiff);
        assertTrue("The resource read again is not the same as the original. diff:"+dummyResourceDiff, dummyResourceDiff.isEmpty());

		// Now we stick our nose deep inside the provisioning impl. But we need
		// to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = connectorTypeManager.getConfiguredConnectorInstance(
				resourceTypeAgain, false, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);

		// Check if the connector still works.
		OperationResult testResult = new OperationResult(TestOpenDJ.class.getName()
				+ ".test010ResourceAndConnectorCaching.test");
		configuredConnectorInstanceAgain.test(testResult);
		testResult.computeStatus();
		assertSuccess("Connector test failed", testResult);
		
		// Test connection should also refresh the connector by itself. So check if it has been refreshed
		
		ConnectorInstance configuredConnectorInstanceAfterTest = connectorTypeManager.getConfiguredConnectorInstance(
				resourceTypeAgain, false, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAfterTest);
		assertTrue("Connector instance was not cached", configuredConnectorInstanceAgain == configuredConnectorInstanceAfterTest);
	}

	@Test
	public void test011ResourceAndConnectorCachingForceFresh() throws Exception {
		displayTestTile("test011ResourceAndConnectorCachingForceFresh");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test011ResourceAndConnectorCachingForceFresh");
		ConnectorInstance configuredConnectorInstance = connectorTypeManager.getConfiguredConnectorInstance(
				resourceType, false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);

		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain
				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));

		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);

		// Now we stick our nose deep inside the provisioning impl. But we need
		// to make sure that the configured connector is properly refreshed
		// forceFresh = true
		ConnectorInstance configuredConnectorInstanceAgain = connectorTypeManager.getConfiguredConnectorInstance(
				resourceTypeAgain, true, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
		assertFalse("Connector instance was not refreshed", configuredConnectorInstance == configuredConnectorInstanceAgain);

		// Check if the connector still works
		OperationResult testResult = new OperationResult(TestOpenDJ.class.getName()
				+ ".test011ResourceAndConnectorCachingForceFresh.test");
		configuredConnectorInstanceAgain.test(testResult);
		testResult.computeStatus();
		assertSuccess("Connector test failed", testResult);
	}

	
	@Test
	public void test020ApplyDefinitionShadow() throws Exception {
		final String TEST_NAME = "test020ApplyDefinitionShadow";
		displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_WILL_FILENAME));

		// WHEN
		provisioningService.applyDefinition(account, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		account.checkConsistence(true, true);
		ResourceObjectShadowUtil.checkConsistence(account, TEST_NAME);
		assertSuccess("applyDefinition(account) result", result);
	}

	@Test
	public void test021ApplyDefinitionAddDelta() throws Exception {
		displayTestTile("test021ApplyDefinitionAddDelta");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test021ApplyDefinitionAddDelta");

		PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_WILL_FILENAME));

		ObjectDelta<AccountShadowType> delta = account.createAddDelta();

		// WHEN
		provisioningService.applyDefinition(delta, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		delta.checkConsistence(true, true, true);
		assertSuccess("applyDefinition(add d, elta) result", result);
	}

	// The account must exist to test this with modify delta. So we postpone the
	// test when the account actually exists

	@Test
	public void test100AddAccount() throws Exception {
		final String TEST_NAME = "test100AddAccount";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		syncServiceMock.reset();

		PrismObject<AccountShadowType> account = prismContext.parseObject(new File(ACCOUNT_WILL_FILENAME));
		account.checkConsistence();

		display("Adding shadow", account);

		// WHEN
		String addedObjectOid = provisioningService.addObject(account, null, null, syncTask, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		account.checkConsistence();

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, result)
				.asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", "will", accountType.getName());
//		assertEquals("will", accountType.getName());
		
		syncServiceMock.assertNotifySuccessOnly();

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_WILL_OID, null, result).asObjectable();
		display("account from provisioning", provisioningAccountType);
		PrismAsserts.assertEqualsPolyString("Name not equal.", "will", provisioningAccountType.getName());
//		assertEquals("will", provisioningAccountType.getName());

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ResourceObjectShadowUtil.getAttributeValues(
				provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());

		// Check if the shadow is in the repo
		PrismObject<AccountShadowType> shadowFromRepo = repositoryService.getObject(AccountShadowType.class,
				addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());

		ProvisioningTestUtil.checkRepoShadow(shadowFromRepo);

		checkConsistency(account);
	}

	@Test
	public void test101AddAccountWithoutName() throws Exception {
		displayTestTile("test101AddAccountWithoutName");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test101AddAccountWithoutName");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test101AddAccountWithoutName");
		syncServiceMock.reset();

		AccountShadowType account = parseObjectTypeFromFile(ACCOUNT_MORGAN_FILENAME, AccountShadowType.class);

		display("Adding shadow", account.asPrismObject());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, syncTask, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService
				.getObject(AccountShadowType.class, ACCOUNT_MORGAN_OID, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Account name was not generated (repository)", ACCOUNT_MORGAN_NAME, accountType.getName());
//		assertEquals("Account name was not generated (repository)", ACCOUNT_MORGAN_NAME, accountType.getName());
		
		syncServiceMock.assertNotifySuccessOnly();

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_MORGAN_OID, null, result).asObjectable();
		display("account from provisioning", provisioningAccountType);
		PrismAsserts.assertEqualsPolyString("Account name was not generated (provisioning)", ACCOUNT_MORGAN_NAME,
				provisioningAccountType.getName());
//		assertEquals("Account name was not generated (provisioning)", ACCOUNT_MORGAN_NAME,
//				provisioningAccountType.getName());

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ResourceObjectShadowUtil.getAttributeValues(
				provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_MORGAN_NAME);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Captain Morgan", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "sh1verM3T1mb3rs", dummyAccount.getPassword());

		// Check if the shadow is in the repo
		PrismObject<AccountShadowType> shadowFromRepo = repositoryService.getObject(AccountShadowType.class,
				addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());

		ProvisioningTestUtil.checkRepoShadow(shadowFromRepo);

		checkConsistency(account.asPrismObject());
	}

	@Test
	public void test102GetAccount() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {
		displayTestTile("test102GetAccount");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test102GetAccount");

		// WHEN
		AccountShadowType shadow = provisioningService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkShadow(shadow, result);

		checkConsistency(shadow.asPrismObject());
	}

	@Test
	public void test102GetAccountNoFetch() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {
		displayTestTile("test102GetAccountNoFetch");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test102GetAccountNoFetch");

		GetOperationOptions options = new GetOperationOptions();
		options.setNoFetch(true);

		// WHEN
		AccountShadowType shadow = provisioningService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, options,
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkShadow(shadow, result, false);

		checkConsistency(shadow.asPrismObject());
	}

	@Test
	public void test105ApplyDefinitionModifyDelta() throws Exception {
		displayTestTile("test105ApplyDefinitionModifyDelta");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test105ApplyDefinitionModifyDelta");

		// TODO
		// ObjectDelta<AccountShadowType> accountDelta =
		// PrismTestUtil.parseDelta(new File(FILENAME_MODIFY_ACCOUNT));

		ObjectModificationType changeAddRoleCaptain = PrismTestUtil.unmarshalObject(new File(FILENAME_MODIFY_ACCOUNT),
				ObjectModificationType.class);
		ObjectDelta<AccountShadowType> accountDelta = DeltaConvertor.createObjectDelta(changeAddRoleCaptain,
				AccountShadowType.class, prismContext);

		// WHEN
		provisioningService.applyDefinition(accountDelta, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		accountDelta.checkConsistence(true, true, true);
		assertSuccess("applyDefinition(modify delta) result", result);
	}

	@Test
	public void test112SeachIterative() throws Exception {
		displayTestTile("test112SeachIterative");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test112SeachIterative");

		// Make sure there is an account on resource that the provisioning has
		// never seen before, so there is no shadow
		// for it yet.
		DummyAccount newAccount = new DummyAccount("meathook");
		newAccount.addAttributeValues("fullname", "Meathook");
		newAccount.setEnabled(true);
		newAccount.setPassword("parrotMonster");
		dummyResource.addAccount(newAccount);

		ObjectQuery query = ObjectQueryUtil.createResourceAndAccountQuery(RESOURCE_DUMMY_OID, new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
						ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext); 

		final List<AccountShadowType> foundObjects = new ArrayList<AccountShadowType>();
		ResultHandler<AccountShadowType> handler = new ResultHandler<AccountShadowType>() {

			@Override
			public boolean handle(PrismObject<AccountShadowType> object, OperationResult parentResult) {
				foundObjects.add(object.asObjectable());

				ObjectType objectType = object.asObjectable();
				assertTrue(objectType instanceof AccountShadowType);
				AccountShadowType shadow = (AccountShadowType) objectType;
				checkShadow(shadow, parentResult);
				return true;
			}
		};

		// WHEN
		provisioningService.searchObjectsIterative(AccountShadowType.class, query, handler, result);

		// THEN
		result.computeStatus();
		display("searchObjectsIterative result", result);
		assertSuccess(result);

		assertEquals(3, foundObjects.size());

		checkConsistency(foundObjects.get(0).asPrismObject());

		// And again ...

		foundObjects.clear();

		// WHEN
		provisioningService.searchObjectsIterative(AccountShadowType.class, query, handler, result);

		// THEN

		assertEquals(3, foundObjects.size());

		checkConsistency(foundObjects.get(0).asPrismObject());
	}

	@Test
	public void test113SearchAllShadowsInRepository() throws Exception {
		displayTestTile("test113SearchAllShadowsInRepository");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test113SearchAllShadowsInRepository");
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<AccountShadowType>> allShadows = repositoryService.searchObjects(AccountShadowType.class,
				query, result);
		
		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);
		
		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
	}

	@Test
	public void test114SearchAllShadows() throws Exception {
		displayTestTile("test114SearchAllShadows");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test114SearchAllShadows");
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<AccountShadowType>> allShadows = provisioningService.searchObjects(AccountShadowType.class,
				query, result);
		
		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);
		
		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 3, allShadows.size());
	}

	@Test
	public void test115countAllShadows() throws Exception {
		displayTestTile("test115countAllShadows");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test115countAllShadows");
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		int count = provisioningService.countObjects(AccountShadowType.class, query, result);
		
		// THEN
		result.computeStatus();
		display("countObjects result", result);
		assertSuccess(result);
		
		display("Found " + count + " shadows");

		assertEquals("Wrong number of results", 3, count);
	}

	@Test
	public void test116SearchNullQueryResource() throws Exception {
		displayTestTile("test116SearchNullQueryResource");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test116SearchNullQueryResource");

		// WHEN
		List<PrismObject<ResourceType>> allResources = provisioningService.searchObjects(ResourceType.class,
				new ObjectQuery(), result);
		
		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);
		
		display("Found " + allResources.size() + " resources");

		assertFalse("No resources found", allResources.isEmpty());
		assertEquals("Wrong number of results", 1, allResources.size());
	}

	@Test
	public void test117CountNullQueryResource() throws Exception {
		displayTestTile("test117CountNullQueryResource");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test117CountNullQueryResource");

		// WHEN
		int count = provisioningService.countObjects(ResourceType.class, new ObjectQuery(), result);
		
		// THEN
		result.computeStatus();
		display("countObjects result", result);
		assertSuccess(result);
		
		display("Counted " + count + " resources");

		assertEquals("Wrong count", 1, count);
	}

	

	@Test
	public void test121EnableAccount() throws Exception {
		displayTestTile("test121EnableAccount");
		// GIVEN

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test121EnableAccount");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test121EnableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertTrue(dummyAccount.isEnabled());
		
		syncServiceMock.reset();

		ObjectModificationType objectModification = unmarshallJaxbFromFile(FILENAME_DISABLE_ACCOUNT,
				ObjectModificationType.class);
		ObjectDelta<AccountShadowType> delta = DeltaConvertor.createObjectDelta(objectModification,
				AccountShadowType.class, PrismTestUtil.getPrismContext());
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, objectModification.getOid(),
				delta.getModifications(), new ProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername("will");
		assertFalse(dummyAccount.isEnabled());
		
		syncServiceMock.assertNotifySuccessOnly();
	}

	@Test
	public void test122DisableAccount() throws Exception {
		displayTestTile("test122DisableAccount");
		// GIVEN

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test122DisableAccount");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test122DisableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();
		assertNotNull(accountType);
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertFalse("Account is not disabled", dummyAccount.isEnabled());
		
		syncServiceMock.reset();

		ObjectModificationType objectModification = unmarshallJaxbFromFile(FILENAME_ENABLE_ACCOUNT,
				ObjectModificationType.class);
		ObjectDelta<AccountShadowType> delta = DeltaConvertor.createObjectDelta(objectModification,
				AccountShadowType.class, PrismTestUtil.getPrismContext());
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(),
				new ProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername("will");
		assertTrue(dummyAccount.isEnabled());
		
		syncServiceMock.assertNotifySuccessOnly();
	}

	@Test
	public void test123ModifyObjectReplace() throws Exception {
		displayTestTile("test123ModifyObjectReplace");

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test123ModifyObjectReplace");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test123ModifyObjectReplace");
		
		syncServiceMock.reset();

		ObjectDelta<AccountShadowType> delta = ObjectDelta.createModificationReplaceProperty(AccountShadowType.class, 
				ACCOUNT_WILL_OID, RESOURCE_DUMMY_ATTR_FULLNAME_PATH, prismContext, "Pirate Will Turner");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(),
				new ProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if activation was changed
		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertEquals("Wrong fullname", "Pirate Will Turner", dummyAccount.getAttributeValue("fullname"));
		
		assertDummyAttributeValues("will", RESOURCE_DUMMY_ATTR_FULLNAME_LOCALNAME, "Pirate Will Turner");
		
		syncServiceMock.assertNotifySuccessOnly();
	}

	@Test
	public void test124ModifyObjectAddPirate() throws Exception {
		displayTestTile("test124ModifyObjectAddPirate");

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test124ModifyObjectAddPirate");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test124ModifyObjectAddPirate");
		syncServiceMock.reset();

		ObjectDelta<AccountShadowType> delta = ObjectDelta.createModificationAddProperty(AccountShadowType.class, 
				ACCOUNT_WILL_OID, RESOURCE_DUMMY_ATTR_TITLE_PATH, prismContext, "Pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(),
				new ProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAttributeValues("will", RESOURCE_DUMMY_ATTR_TITLE_LOCALNAME, "Pirate");
		
		syncServiceMock.assertNotifySuccessOnly();
	}
	
	@Test
	public void test125ModifyObjectAddCaptain() throws Exception {
		displayTestTile("test125ModifyObjectAddCaptain");

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test125ModifyObjectAddCaptain");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test125ModifyObjectAddCaptain");
		syncServiceMock.reset();

		ObjectDelta<AccountShadowType> delta = ObjectDelta.createModificationAddProperty(AccountShadowType.class, 
				ACCOUNT_WILL_OID, RESOURCE_DUMMY_ATTR_TITLE_PATH, prismContext, "Captain");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(),
				new ProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAttributeValues("will", RESOURCE_DUMMY_ATTR_TITLE_LOCALNAME, "Pirate", "Captain");
		
		syncServiceMock.assertNotifySuccessOnly();
	}

	@Test
	public void test126ModifyObjectDeletePirate() throws Exception {
		displayTestTile("test126ModifyObjectDeletePirate");

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test126ModifyObjectDeletePirate");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test126ModifyObjectDeletePirate");
		syncServiceMock.reset();

		ObjectDelta<AccountShadowType> delta = ObjectDelta.createModificationDeleteProperty(AccountShadowType.class, 
				ACCOUNT_WILL_OID, RESOURCE_DUMMY_ATTR_TITLE_PATH, prismContext, "Pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(),
				new ProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAttributeValues("will", RESOURCE_DUMMY_ATTR_TITLE_LOCALNAME, "Captain");
		
		syncServiceMock.assertNotifySuccessOnly();
	}
	
	/**
	 * Try to add the same value that the account attribute already has. Resources that do not tolerate this will fail
	 * unless the mechanism to compensate for this works properly.
	 */
	@Test
	public void test127ModifyObjectAddCaptainAgain() throws Exception {
		final String TEST_NAME = "test127ModifyObjectAddCaptainAgain";
		displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<AccountShadowType> delta = ObjectDelta.createModificationAddProperty(AccountShadowType.class, 
				ACCOUNT_WILL_OID, RESOURCE_DUMMY_ATTR_TITLE_PATH, prismContext, "Captain");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(),
				new ProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAttributeValues("will", RESOURCE_DUMMY_ATTR_TITLE_LOCALNAME, "Captain");
		
		syncServiceMock.assertNotifySuccessOnly();
	}
	
	/**
	 * Set a null value to the (native) dummy attribute. The UCF layer should filter that out.
	 */
	@Test
	public void test128NullAttributeValue() throws Exception {
		final String TEST_NAME = "test128NullAttributeValue";
		displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		DummyAccount willDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_ICF_UID);
		willDummyAccount.replaceAttributeValue(RESOURCE_DUMMY_ATTR_TITLE_LOCALNAME, null);

		// WHEN
		PrismObject<AccountShadowType> accountWill = provisioningService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);
		
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(accountWill);
		ResourceAttribute<Object> titleAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), RESOURCE_DUMMY_ATTR_TITLE_LOCALNAME));
		assertNull("Title attribute sneaked in", titleAttribute);
		
		accountWill.checkConsistence();		
	}

	@Test
	public void test131AddScript() throws Exception {
		final String TEST_NAME = "test131AddScript";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		AccountShadowType account = parseObjectTypeFromFile(FILENAME_ACCOUNT_SCRIPT, AccountShadowType.class);
		display("Account before add", account);

		ProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILENAME_SCRIPT_ADD, ProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_NEW_SCRIPT_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_SCRIPT_OID,
				result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", "william", accountType.getName());
		
		syncServiceMock.assertNotifySuccessOnly();

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", "william", provisioningAccountType.getName());

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "William Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());
		
		assertScripts(dummyResource.getScriptHistory(), "In the beginning ...", "Hello World");
	}

	// MID-1113
	@Test(enabled=false)
	public void test132ModifyScript() throws Exception {
		final String TEST_NAME = "test132ModifyScript";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		ProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILENAME_SCRIPT_ADD, ProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));
		
		ObjectDelta<AccountShadowType> delta = ObjectDelta.createModificationReplaceProperty(AccountShadowType.class, 
				ACCOUNT_NEW_SCRIPT_OID, RESOURCE_DUMMY_ATTR_FULLNAME_PATH, prismContext, "Will Turner");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(AccountShadowType.class, ACCOUNT_NEW_SCRIPT_OID, delta.getModifications(), 
				scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());
		
		assertScripts(dummyResource.getScriptHistory(), "Where am I?", "Still here");
	}
	
	@Test
	public void test133DeleteScript() throws Exception {
		final String TEST_NAME = "test133DeleteScript";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		ProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILENAME_SCRIPT_ADD, ProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));
		
		// WHEN
		provisioningService.deleteObject(AccountShadowType.class, ACCOUNT_NEW_SCRIPT_OID, null, scriptsType,
				task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNull("Dummy account not gone", dummyAccount);
		
		assertScripts(dummyResource.getScriptHistory(), "Goodbye World", "R.I.P.");
	}
	
	private void assertScripts(List<ScriptHistoryEntry> scriptsHistory, String... expectedScripts) {
		displayScripts(scriptsHistory);
		assertEquals("Wrong number of scripts executed", expectedScripts.length, scriptsHistory.size());
		Iterator<ScriptHistoryEntry> historyIter = scriptsHistory.iterator();
		for (String expecedScript: expectedScripts) {
			ScriptHistoryEntry actualScript = historyIter.next();
			assertEquals("Wrong script code", expecedScript, actualScript.getCode());
			assertEquals("We talk only gibberish here", "Gibberish", actualScript.getLanguage());
		}
	}

	private void displayScripts(List<ScriptHistoryEntry> scriptsHistory) {
		for (ScriptHistoryEntry script : scriptsHistory) {
			display("Script", script);
		}
	}

//	@Test
//	public void test132ModifyScript() {
//		// TODO
//	}
//
//	@Test
//	public void test133DeleteScript() {
//		// TODO
//	}

	@Test
	public void test500AddProtectedAccount() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {
		displayTestTile("test500AddProtectedAccount");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test500AddProtectedAccount");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test500AddProtectedAccount");
		syncServiceMock.reset();

		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultAccountDefinition();
		AccountShadowType shadowType = new AccountShadowType();
		PrismTestUtil.getPrismContext().adopt(shadowType);
		shadowType.setName(PrismTestUtil.createPolyStringType(ACCOUNT_DAVIEJONES_USERNAME));
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
		PrismObject<AccountShadowType> shadow = shadowType.asPrismObject();
		PrismContainer<Containerable> attrsCont = shadow.findOrCreateContainer(AccountShadowType.F_ATTRIBUTES);
		PrismProperty<String> icfsNameProp = attrsCont.findOrCreateProperty(ConnectorFactoryIcfImpl.ICFS_NAME);
		icfsNameProp.setRealValue(ACCOUNT_DAVIEJONES_USERNAME);

		// WHEN
		try {
			provisioningService.addObject(shadow, null, null, syncTask, result);
			AssertJUnit.fail("Expected security exception while adding 'daviejones' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display("addObject result (expected failure)", result);
		assertFailure(result);
		
		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();
	}

	@Test
	public void test501GetProtectedAccountShadow() throws ObjectNotFoundException, CommunicationException,
			SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test501GetProtectedAccount");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test501GetProtectedAccount");

		// WHEN
		try {
			provisioningService.getObject(AccountShadowType.class, ACCOUNT_DAEMON_OID, null, result);
			AssertJUnit.fail("Expected security exception while reading 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display("getObject result (expected failure)", result);
		assertFailure(result);

//		checkConsistency();
	}

	@Test
	public void test502ModifyProtectedAccountShadow() throws Exception {
		displayTestTile("test502ModifyProtectedAccountShadow");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test502ModifyProtectedAccountShadow");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test502ModifyProtectedAccountShadow");
		syncServiceMock.reset();

		Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultAccountDefinition();
		ResourceAttributeDefinition fullnameAttrDef = defaultAccountDefinition.findAttributeDefinition("fullname");
		ResourceAttribute fullnameAttr = fullnameAttrDef.instantiate();
		PropertyDelta fullnameDelta = fullnameAttr.createDelta(new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES,
				fullnameAttrDef.getName()));
		fullnameDelta.setValueToReplace(new PrismPropertyValue<String>("Good Daemon"));
		((Collection) modifications).add(fullnameDelta);

		// WHEN
		try {
			provisioningService.modifyObject(AccountShadowType.class, ACCOUNT_DAEMON_OID, modifications, null, null, syncTask, result);
			AssertJUnit.fail("Expected security exception while modifying 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display("modifyObject result (expected failure)", result);
		assertFailure(result);
		
		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();
	}

	@Test
	public void test503DeleteProtectedAccountShadow() throws ObjectNotFoundException, CommunicationException,
			SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test503DeleteProtectedAccountShadow");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test503DeleteProtectedAccountShadow");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test503DeleteProtectedAccountShadow");
		syncServiceMock.reset();

		// WHEN
		try {
			provisioningService.deleteObject(AccountShadowType.class, ACCOUNT_DAEMON_OID, null, null, syncTask, result);
			AssertJUnit.fail("Expected security exception while deleting 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display("deleteObject result (expected failure)", result);
		assertFailure(result);
		
		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();
	}

	static Task syncTokenTask = null;
	
	@Test
	public void test800LiveSyncInit() throws ObjectNotFoundException, CommunicationException, SchemaException,
			com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException,
			SecurityViolationException {
		displayTestTile("test800LiveSyncInit");
		syncTokenTask = taskManager.createTaskInstance(TestDummy.class.getName() + ".syncTask");

		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		syncServiceMock.reset();

		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test800LiveSyncInit");

		// Dry run to remember the current sync token in the task instance.
		// Otherwise a last sync token whould be used and
		// no change would be detected
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);

		// No change, no fun
		syncServiceMock.assertNoNotifyChange();

		checkAllShadows();
	}

	@Test
	public void test801LiveSyncAddBlackbeard() throws Exception {
		displayTestTile("test801LiveSyncAddBlackbeard");
		// GIVEN
//		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
//				+ ".test801LiveSyncAddBlackbeard");
//		
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test801LiveSyncAddBlackbeard");

		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount(BLACKBEARD_USERNAME);
		newAccount.addAttributeValues("fullname", "Edward Teach");
		newAccount.setEnabled(true);
		newAccount.setPassword("shiverMEtimbers");
		dummyResource.addAccount(newAccount);

		display("Resource before sync", dummyResource.dump());

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		// assertNull("Old shadow present when not expecting it",
		// lastChange.getOldShadow());
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ResourceObjectShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadowType.getClass().getName(),
				currentShadowType instanceof AccountShadowType);

		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil
				.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Edward Teach",
				fullnameAttribute.getRealValue());

		checkAllShadows();
	}

	@Test
	public void test802LiveSyncModifyBlackbeard() throws Exception {
		displayTestTile("test802LiveSyncModifyBlackbeard");
		// GIVEN
//		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
//				+ ".test802LiveSyncModifyBlackbeard");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test802LiveSyncModifyBlackbeard");

		syncServiceMock.reset();

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(BLACKBEARD_USERNAME);
		dummyAccount.replaceAttributeValue("fullname", "Captain Blackbeard");

		display("Resource before sync", dummyResource.dump());

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		// assertNull("Old shadow present when not expecting it",
		// lastChange.getOldShadow());
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ResourceObjectShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadowType.getClass().getName(),
				currentShadowType instanceof AccountShadowType);

		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil
				.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Captain Blackbeard",
				fullnameAttribute.getRealValue());

		checkAllShadows();
	}

	@Test
	public void test803LiveSyncAddDrake() throws Exception {
		displayTestTile("test803LiveSyncAddDrake");
		// GIVEN
//		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
//				+ ".test803LiveSyncAddDrake");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test803LiveSyncAddDrake");

		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount(DRAKE_USERNAME);
		newAccount.addAttributeValues("fullname", "Sir Francis Drake");
		newAccount.setEnabled(true);
		newAccount.setPassword("avast!");
		dummyResource.addAccount(newAccount);

		display("Resource before sync", dummyResource.dump());

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		// assertNull("Old shadow present when not expecting it",
		// lastChange.getOldShadow());
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ResourceObjectShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadowType.getClass().getName(),
				currentShadowType instanceof AccountShadowType);

		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil
				.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Sir Francis Drake",
				fullnameAttribute.getRealValue());

		checkAllShadows();
	}

	@Test
	public void test810LiveSyncModifyProtectedAccount() throws Exception {
		displayTestTile("test810LiveSyncModifyProtectedAccount");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test810LiveSyncModifyProtectedAccount");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test810LiveSyncModifyProtectedAccount");

		syncServiceMock.reset();

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_DAEMON_USERNAME);
		dummyAccount.replaceAttributeValue("fullname", "Maxwell deamon");

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		syncServiceMock.assertNoNotifyChange();

		checkAllShadows();
	}

	@Test
	public void test901FailResourceNotFound() throws FileNotFoundException, JAXBException,
			ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		displayTestTile("test901FailResourceNotFound");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test901FailResourceNotFound");

		// WHEN
		try {
			PrismObject<ResourceType> object = provisioningService.getObject(ResourceType.class, NOT_PRESENT_OID, null,
					result);
			AssertJUnit.fail("Expected ObjectNotFoundException to be thrown, but getObject returned " + object
					+ " instead");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		result.computeStatus();
		display("getObject result (expected failure)", result);
		assertFailure(result);
	}
	
	private void checkShadow(AccountShadowType shadow, OperationResult parentResult) {
		checkShadow(shadow, parentResult, true);
	}

	private void checkShadow(AccountShadowType shadowType, OperationResult parentResult, boolean fullShadow) {
		shadowType.asPrismObject().checkConsistence(true, true);
		ResourceObjectShadowUtil.checkConsistence(shadowType.asPrismObject(), parentResult.getOperation());
		ObjectChecker<AccountShadowType> checker = createShadowChecker(fullShadow);
		IntegrationTestTools.checkShadow(shadowType, resourceType, repositoryService, checker, prismContext, parentResult);
	}

	private void checkAllShadows() throws SchemaException, ObjectNotFoundException, CommunicationException,
			ConfigurationException {
		ObjectChecker<AccountShadowType> checker = null;
		IntegrationTestTools.checkAllShadows(resourceType, repositoryService, checker, prismContext);
	}

	private ObjectChecker<AccountShadowType> createShadowChecker(final boolean fullShadow) {
		return new ObjectChecker<AccountShadowType>() {
			@Override
			public void check(AccountShadowType shadow) {
				String icfName = ResourceObjectShadowUtil.getSingleStringAttributeValue(shadow,
						SchemaTestConstants.ICFS_NAME);
				assertNotNull("No ICF NAME", icfName);
				PrismAsserts.assertEqualsPolyString("Wrong shadow name", icfName, shadow.getName());
//				assertEquals("Wrong shadow name", shadow.getName(), icfName);
				if (fullShadow) {
					assertNotNull(
							"Missing fullname attribute",
							ResourceObjectShadowUtil.getSingleStringAttributeValue(shadow,
									new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "fullname")));
					assertNotNull("no activation", shadow.getActivation());
					assertNotNull("no activation/enabled", shadow.getActivation().isEnabled());
					assertTrue("not enabled", shadow.getActivation().isEnabled());
				}

				assertProvisioningAccountShadow(shadow.asPrismObject(), resourceType, ResourceAttributeDefinition.class);
			}

		};
	}
	
	private <T> void assertDummyAttributeValues(String accountId, String attributeName, T... expectedValues) {
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(accountId);
		assertNotNull("No account '"+accountId+"'", dummyAccount);
		Set<T> attributeValues = (Set<T>) dummyAccount.getAttributeValues(attributeName, expectedValues[0].getClass());
		assertNotNull("No attribute "+attributeName+" in account "+accountId, attributeValues);
		TestUtil.assertSetEquals("Wroung values of attribute "+attributeName+" in account "+accountId, attributeValues, expectedValues);
	}

}
