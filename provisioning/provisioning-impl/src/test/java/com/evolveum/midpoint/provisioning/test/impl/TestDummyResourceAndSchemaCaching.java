/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;

/**
 * The test of Provisioning service on the API level. It checks proper caching of resource and schemas. 
 * 
 * The test is using dummy resource for speed and flexibility.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyResourceAndSchemaCaching extends AbstractDummyTest {

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyResourceAndSchemaCaching.class);
	
	private CachingMetadataType lastCachingMetadata;
	private long lastConnectorSchemaFetchCount = 0;
	
	@Test
	public void test010GetResource() throws Exception {
		final String TEST_NAME = "test010GetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		
		rememberConnectorSchemaFetchCount();
		
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceTypeBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result)
				.asObjectable();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

		// THEN
		display("Resource", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource");
		rememberSchemaMetadata(resourceProvisioning);
		
		assertConnectorSchemaFetch(1);
				
		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_OID, result);
		assertHasSchema(resourceRepoAfter, "repo resource after");

		assertSchemaMetadataUnchanged(resourceRepoAfter);
		
		// Just refresh the resource used by other tests. This one has a complete schema.
		resourceType = resourceProvisioning.asObjectable();
	}
	
	
	private CachingMetadataType getSchemaCachingMetadata(PrismObject<ResourceType> resource) {
		ResourceType resourceType = resource.asObjectable();
		XmlSchemaType xmlSchemaTypeAfter = resourceType.getSchema();
		assertNotNull("No schema", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceType);
		assertNotNull("No schema XSD element", resourceXsdSchemaElementAfter);
		return xmlSchemaTypeAfter.getCachingMetadata();
	}
	
	private void rememberSchemaMetadata(PrismObject<ResourceType> resource) {
		lastCachingMetadata = getSchemaCachingMetadata(resource);
	}
	
	private void assertSchemaMetadataUnchanged(PrismObject<ResourceType> resource) {
		CachingMetadataType current = getSchemaCachingMetadata(resource);
		assertEquals("Schema caching metadata changed", lastCachingMetadata, current);
	}
	
	private void rememberConnectorSchemaFetchCount() {
		lastConnectorSchemaFetchCount = InternalMonitor.getConnectorSchemaFetchCount();
	}

	private void assertConnectorSchemaFetch(int expectedIncrement) {
		long currentConnectorSchemaFetchCount = InternalMonitor.getConnectorSchemaFetchCount();
		long actualIncrement = currentConnectorSchemaFetchCount - lastConnectorSchemaFetchCount;
		assertEquals("Unexpected increment in connector schema fetch count", (long)expectedIncrement, actualIncrement);
		lastConnectorSchemaFetchCount = currentConnectorSchemaFetchCount;
	}

	
	private void assertHasSchema(PrismObject<ResourceType> resource, String desc) throws SchemaException {
		ResourceType resourceType = resource.asObjectable();
		display("Resource "+desc, resourceType);

		XmlSchemaType xmlSchemaTypeAfter = resourceType.getSchema();
		assertNotNull("No schema in "+desc, xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceType);
		assertNotNull("No schema XSD element in "+desc, resourceXsdSchemaElementAfter);

		String resourceXml = prismContext.getPrismDomProcessor().serializeObjectToString(resource);
//		display("Resource XML", resourceXml);                            

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata in "+desc, cachingMetadata);
		assertNotNull("No retrievalTimestamp in "+desc, cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber in "+desc, cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resource.toString(), prismContext);
		assertNotNull("No schema after parsing in "+desc, parsedSchema);
	}
	
	

//	@Test
//	public void test004Configuration() throws ObjectNotFoundException, CommunicationException, SchemaException,
//			ConfigurationException, SecurityViolationException {
//		displayTestTile("test004Configuration");
//		// GIVEN
//		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
//				+ ".test004Configuration");
//
//		// WHEN
//		resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
//		resourceType = resource.asObjectable();
//		
//		// THEN
//		result.computeStatus();
//		display("getObject result", result);
//		assertSuccess(result);
//
//		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
//		assertNotNull("No configuration container", configurationContainer);
//		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
//		assertNotNull("No configuration container definition", confContDef);
//		PrismContainer confingurationPropertiesContainer = configurationContainer
//				.findContainer(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
//		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
//		PrismContainerDefinition confPropsDef = confingurationPropertiesContainer.getDefinition();
//		assertNotNull("No configuration properties container definition", confPropsDef);
//		List<PrismProperty<?>> configurationProperties = confingurationPropertiesContainer.getValue().getItems();
//		assertFalse("No configuration properties", configurationProperties.isEmpty());
//		for (PrismProperty<?> confProp : configurationProperties) {
//			PrismPropertyDefinition confPropDef = confProp.getDefinition();
//			assertNotNull("No definition for configuration property " + confProp, confPropDef);
//			assertFalse("Configuration property " + confProp + " is raw", confProp.isRaw());
//		}
//		
//		// The useless configuration variables should be reflected to the resource now
//		assertEquals("Wrong useless string", "Shiver me timbers!", dummyResource.getUselessString());
//		assertEquals("Wrong guarded useless string", "Dead men tell no tales", dummyResource.getUselessGuardedString());
//		
//		resource.checkConsistence();
//	}
//
//	@Test
//	public void test005ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
//			ConfigurationException {
//		displayTestTile("test005ParsedSchema");
//		// GIVEN
//
//		// THEN
//		// The returned type should have the schema pre-parsed
//		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceType));
//
//		// Also test if the utility method returns the same thing
//		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
//
//		display("Parsed resource schema", returnedSchema);
//
//		// Check whether it is reusing the existing schema and not parsing it
//		// all over again
//		// Not equals() but == ... we want to really know if exactly the same
//		// object instance is returned
//		assertTrue("Broken caching",
//				returnedSchema == RefinedResourceSchema.getResourceSchema(resourceType, prismContext));
//
//		assertSchemaSanity(returnedSchema, resourceType);
//
//	}
//
//	@Test
//	public void test006RefinedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
//			ConfigurationException {
//		displayTestTile("test006RefinedSchema");
//		// GIVEN
//
//		// WHEN
//		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType, prismContext);
//		display("Refined schema", refinedSchema);
//
//		// Check whether it is reusing the existing schema and not parsing it
//		// all over again
//		// Not equals() but == ... we want to really know if exactly the same
//		// object instance is returned
//		assertTrue("Broken caching",
//				refinedSchema == RefinedResourceSchema.getRefinedSchema(resourceType, prismContext));
//
//		RefinedObjectClassDefinition accountDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
//		assertNotNull("Account definition is missing", accountDef);
//		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
//		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
//		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
//		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
//		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
//		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));
//
//		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
//		assertEquals(1, uidDef.getMaxOccurs());
//		assertEquals(0, uidDef.getMinOccurs());
//		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
//		assertFalse("UID has create", uidDef.canCreate());
//		assertFalse("UID has update", uidDef.canUpdate());
//		assertTrue("No UID read", uidDef.canRead());
//		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));
//
//		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
//		assertEquals(1, nameDef.getMaxOccurs());
//		assertEquals(1, nameDef.getMinOccurs());
//		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
//		assertTrue("No NAME create", nameDef.canCreate());
//		assertTrue("No NAME update", nameDef.canUpdate());
//		assertTrue("No NAME read", nameDef.canRead());
//		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));
//
//		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
//		assertNotNull("No definition for fullname", fullnameDef);
//		assertEquals(1, fullnameDef.getMaxOccurs());
//		assertEquals(1, fullnameDef.getMinOccurs());
//		assertTrue("No fullname create", fullnameDef.canCreate());
//		assertTrue("No fullname update", fullnameDef.canUpdate());
//		assertTrue("No fullname read", fullnameDef.canRead());
//
//		assertNull("The _PASSSWORD_ attribute sneaked into schema",
//				accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));
//
//	}
//
//	@Test
//	public void test006Capabilities() throws Exception {
//		displayTestTile("test006Capabilities");
//
//		// GIVEN
//		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
//				+ ".test006Capabilities");
//
//		// WHEN
//		ResourceType resourceType = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result)
//				.asObjectable();
//
//		// THEN
//		result.computeStatus();
//		display("getObject result", result);
//		assertSuccess(result);
//
//		// Check native capabilities
//		CapabilityCollectionType nativeCapabilities = resourceType.getCapabilities().getNative();
//		display("Native capabilities", PrismTestUtil.marshalWrap(nativeCapabilities));
//		display("Resource", resourceType);
//		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
//		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
//		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				CredentialsCapabilityType.class);
//		assertNotNull("password native capability not present", capCred.getPassword());
//		ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				ActivationCapabilityType.class);
//		assertNotNull("native activation capability not present", capAct);
//		assertNotNull("native activation/enabledisable capability not present", capAct.getEnableDisable());
//		TestConnectionCapabilityType capTest = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				TestConnectionCapabilityType.class);
//		assertNotNull("native test capability not present", capTest);
//		ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				ScriptCapabilityType.class);
//		assertNotNull("native script capability not present", capScript);
//		assertNotNull("No host in native script capability", capScript.getHost());
//		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
//		// TODO: better look inside
//		
//		capabilitiesCachingMetadataType = resourceType.getCapabilities().getCachingMetadata();
//		assertNotNull("No capabilities caching metadata", capabilitiesCachingMetadataType);
//		assertNotNull("No capabilities caching metadata timestamp", capabilitiesCachingMetadataType.getRetrievalTimestamp());
//		assertNotNull("No capabilities caching metadata serial number", capabilitiesCachingMetadataType.getSerialNumber());
//
//		// Check effective capabilites
//		capCred = ResourceTypeUtil.getEffectiveCapability(resourceType, CredentialsCapabilityType.class);
//		assertNotNull("password capability not found", capCred.getPassword());
//		// Although connector does not support activation, the resource
//		// specifies a way how to simulate it.
//		// Therefore the following should succeed
//		capAct = ResourceTypeUtil.getEffectiveCapability(resourceType, ActivationCapabilityType.class);
//		assertNotNull("activation capability not found", capCred.getPassword());
//
//		List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resourceType);
//		for (Object capability : effectiveCapabilities) {
//			System.out.println("Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : "
//					+ capability);
//		}
//	}
//	
//	/**
//	 * Check if the cached native capabilities were properly stored in the repo 
//	 */
//	@Test
//	public void test007CapabilitiesRepo() throws Exception {
//		displayTestTile("test007CapabilitiesRepo");
//
//		// GIVEN
//		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
//				+ ".test007CapabilitiesRepo");
//
//		// WHEN
//		PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);;
//
//		// THEN
//		result.computeStatus();
//		display("getObject result", result);
//		assertSuccess(result);
//
//		// Check native capabilities
//		ResourceType resourceType = resource.asObjectable();
//		CapabilitiesType capabilitiesType = resourceType.getCapabilities();
//		assertNotNull("No capabilities in repo, the capabilities were not cached", capabilitiesType);
//		CapabilityCollectionType nativeCapabilities = capabilitiesType.getNative();
//		System.out.println("Native capabilities: " + PrismTestUtil.marshalWrap(nativeCapabilities));
//		System.out.println("resource: " + resourceType.asPrismObject().dump());
//		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
//		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
//		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				CredentialsCapabilityType.class);
//		assertNotNull("password native capability not present", capCred.getPassword());
//		ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				ActivationCapabilityType.class);
//		assertNotNull("native activation capability not present", capAct);
//		assertNotNull("native activation/enabledisable capability not present", capAct.getEnableDisable());
//		TestConnectionCapabilityType capTest = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				TestConnectionCapabilityType.class);
//		assertNotNull("native test capability not present", capTest);
//		ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList,
//				ScriptCapabilityType.class);
//		assertNotNull("native script capability not present", capScript);
//		assertNotNull("No host in native script capability", capScript.getHost());
//		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
//		// TODO: better look inside
//
//		CachingMetadataType repoCapabilitiesCachingMetadataType = capabilitiesType.getCachingMetadata();
//		assertNotNull("No repo capabilities caching metadata", repoCapabilitiesCachingMetadataType);
//		assertNotNull("No repo capabilities caching metadata timestamp", repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
//		assertNotNull("No repo capabilities caching metadata serial number", repoCapabilitiesCachingMetadataType.getSerialNumber());
//		assertEquals("Repo capabilities caching metadata timestamp does not match previously returned value", 
//				capabilitiesCachingMetadataType.getRetrievalTimestamp(), repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
//		assertEquals("Repo capabilities caching metadata serial does not match previously returned value", 
//				capabilitiesCachingMetadataType.getSerialNumber(), repoCapabilitiesCachingMetadataType.getSerialNumber());
//
//	}
//
//	@Test
//	public void test010ResourceAndConnectorCaching() throws Exception {
//		displayTestTile("test010ResourceAndConnectorCaching");
//
//		// GIVEN
//		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
//				+ ".test010ResourceAndConnectorCaching");
//		ConnectorInstance configuredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
//				resourceType, false, result);
//		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
//		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
//		assertNotNull("No resource schema", resourceSchema);
//
//		// WHEN
//		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
//				null, result);
//
//		// THEN
//		result.computeStatus();
//		display("getObject result", result);
//		assertSuccess(result);
//
//		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
//		assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
//		assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());
//
//		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
//		PrismContainer<Containerable> configurationContainerAgain = resourceAgain
//				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
//		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
//
//		// Check resource schema caching
//		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
//		assertNotNull("No resource schema (again)", resourceSchemaAgain);
//		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
//		
//		// Check capabilities caching
//		
//		CapabilitiesType capabilitiesType = resourceType.getCapabilities();
//		assertNotNull("No capabilities fetched from provisioning", capabilitiesType);
//		CachingMetadataType capCachingMetadataType = capabilitiesType.getCachingMetadata();
//		assertNotNull("No capabilities caching metadata fetched from provisioning", capCachingMetadataType);
//		CachingMetadataType capCachingMetadataTypeAgain = resourceTypeAgain.getCapabilities().getCachingMetadata();
//		assertEquals("Capabilities caching metadata serial number has changed", capCachingMetadataType.getSerialNumber(), 
//				capCachingMetadataTypeAgain.getSerialNumber());
//		assertEquals("Capabilities caching metadata timestamp has changed", capCachingMetadataType.getRetrievalTimestamp(), 
//				capCachingMetadataTypeAgain.getRetrievalTimestamp());
//		
//		// Rough test if everything is fine
//		resource.asObjectable().setFetchResult(null);
//		resourceAgain.asObjectable().setFetchResult(null);
//		ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(resource, resourceAgain);
//        display("Dummy resource diff", dummyResourceDiff);
//        assertTrue("The resource read again is not the same as the original. diff:"+dummyResourceDiff, dummyResourceDiff.isEmpty());
//
//		// Now we stick our nose deep inside the provisioning impl. But we need
//		// to make sure that the
//		// configured connector is properly cached
//		ConnectorInstance configuredConnectorInstanceAgain = connectorManager.getConfiguredConnectorInstance(
//				resourceTypeAgain, false, result);
//		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
//		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);
//
//		// Check if the connector still works.
//		OperationResult testResult = new OperationResult(TestOpenDJ.class.getName()
//				+ ".test010ResourceAndConnectorCaching.test");
//		configuredConnectorInstanceAgain.test(testResult);
//		testResult.computeStatus();
//		assertSuccess("Connector test failed", testResult);
//		
//		// Test connection should also refresh the connector by itself. So check if it has been refreshed
//		
//		ConnectorInstance configuredConnectorInstanceAfterTest = connectorManager.getConfiguredConnectorInstance(
//				resourceTypeAgain, false, result);
//		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAfterTest);
//		assertTrue("Connector instance was not cached", configuredConnectorInstanceAgain == configuredConnectorInstanceAfterTest);
//	}
//
//	@Test
//	public void test011ResourceAndConnectorCachingForceFresh() throws Exception {
//		displayTestTile("test011ResourceAndConnectorCachingForceFresh");
//
//		// GIVEN
//		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
//				+ ".test011ResourceAndConnectorCachingForceFresh");
//		ConnectorInstance configuredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
//				resourceType, false, result);
//		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
//		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
//		assertNotNull("No resource schema", resourceSchema);
//
//		// WHEN
//		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
//				null, result);
//
//		// THEN
//		result.computeStatus();
//		display("getObject result", result);
//		assertSuccess(result);
//
//		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
//		assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
//		assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());
//
//		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
//		PrismContainer<Containerable> configurationContainerAgain = resourceAgain
//				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
//		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
//
//		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
//		assertNotNull("No resource schema (again)", resourceSchemaAgain);
//		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
//
//		// Now we stick our nose deep inside the provisioning impl. But we need
//		// to make sure that the configured connector is properly refreshed
//		// forceFresh = true
//		ConnectorInstance configuredConnectorInstanceAgain = connectorManager.getConfiguredConnectorInstance(
//				resourceTypeAgain, true, result);
//		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
//		assertFalse("Connector instance was not refreshed", configuredConnectorInstance == configuredConnectorInstanceAgain);
//
//		// Check if the connector still works
//		OperationResult testResult = new OperationResult(TestOpenDJ.class.getName()
//				+ ".test011ResourceAndConnectorCachingForceFresh.test");
//		configuredConnectorInstanceAgain.test(testResult);
//		testResult.computeStatus();
//		assertSuccess("Connector test failed", testResult);
//	}
//
//	
//
//	@Test
//	public void test100AddAccount() throws Exception {
//		final String TEST_NAME = "test100AddAccount";
//		displayTestTile(TEST_NAME);
//		// GIVEN
//		Task syncTask = taskManager.createTaskInstance(TestDummyResourceAndSchemaCaching.class.getName()
//				+ "." + TEST_NAME);
//		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
//				+ "." + TEST_NAME);
//		syncServiceMock.reset();
//
//		PrismObject<ShadowType> account = prismContext.parseObject(new File(ACCOUNT_WILL_FILENAME));
//		account.checkConsistence();
//
//		display("Adding shadow", account);
//
//		// WHEN
//		String addedObjectOid = provisioningService.addObject(account, null, null, syncTask, result);
//
//		// THEN
//		result.computeStatus();
//		display("add object result", result);
//		assertSuccess("addObject has failed (result)", result);
//		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);
//
//		account.checkConsistence();
//
//		ShadowType accountType = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, result)
//				.asObjectable();
//		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_WILL_USERNAME, accountType.getName());
//		assertEquals("Wrong kind (repo)", ShadowKindType.ACCOUNT, accountType.getKind());
//		assertAttribute(accountType, ConnectorFactoryIcfImpl.ICFS_NAME, getWillRepoIcfUid());
//		assertAttribute(accountType, ConnectorFactoryIcfImpl.ICFS_UID, getWillRepoIcfUid());
//		
//		syncServiceMock.assertNotifySuccessOnly();
//
//		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class,
//				ACCOUNT_WILL_OID, null, result).asObjectable();
//		display("account from provisioning", provisioningAccountType);
//		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_WILL_USERNAME, provisioningAccountType.getName());
//		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, provisioningAccountType.getKind());
//		assertAttribute(provisioningAccountType, ConnectorFactoryIcfImpl.ICFS_NAME, getWillRepoIcfUid());
//		assertAttribute(provisioningAccountType, ConnectorFactoryIcfImpl.ICFS_UID, getWillRepoIcfUid());
//
//		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
//				provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));
//
//		// Check if the account was created in the dummy resource
//
//		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
//		assertNotNull("No dummy account", dummyAccount);
//		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
//		assertTrue("The account is not enabled", dummyAccount.isEnabled());
//		assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());
//
//		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
//		PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
//				addedObjectOid, result);
//		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
//		display("Repository shadow", shadowFromRepo.dump());
//
//		ProvisioningTestUtil.checkRepoAccountShadow(shadowFromRepo);
//
//		checkConsistency(account);
//	}

}
