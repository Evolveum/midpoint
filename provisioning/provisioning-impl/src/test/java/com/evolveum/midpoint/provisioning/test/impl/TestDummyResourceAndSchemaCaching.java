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
import com.evolveum.midpoint.common.monitor.CachingStatistics;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
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
	private long lastConnectorInitializationCount = 0;
	private long lastResourceSchemaParseCount = 0;
	private CachingStatistics lastResourceCacheStats;
	private ResourceSchema lastResourceSchema;
	private RefinedResourceSchema lastRefinedResourceSchema;
	
	@Test
	public void test010GetResource() throws Exception {
		final String TEST_NAME = "test010GetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		
		// Some connector initialization and other things might happen in previous tests.
		// The monitor is static, not part of spring context, it will not be cleared
		rememberConnectorSchemaFetchCount();
		rememberConnectorInitializationCount();
		rememberResourceSchemaParseCount();
		rememberResourceCacheStats();
		
		// Check that there is no schema before test (pre-condition)
		PrismObject<ResourceType> resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);
		ResourceType resourceTypeBefore = resourceBefore.asObjectable();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);
		
		assertVersion(resourceBefore, "0");

		// WHEN
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

		// THEN
		display("Resource", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource");
		rememberSchemaMetadata(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(1);
		assertConnectorInitializationCountIncrement(1);
		assertResourceSchemaParseCountIncrement(1);
				
		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_OID, result);
		assertHasSchema(resourceRepoAfter, "repo resource after");

		assertSchemaMetadataUnchanged(resourceRepoAfter);
		
		display("Resource cache", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(0);
		assertResourceCacheMissesIncrement(1);
		
		rememberResourceSchema(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		rememberRefinedResourceSchema(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		// Just refresh the resource used by other tests. This one has a complete schema.
		resourceType = resourceProvisioning.asObjectable();
	}
	
	@Test
	public void test011GetResourceAgain() throws Exception {
		final String TEST_NAME = "test011GetResourceAgain";
		displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		
		// WHEN
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

		// THEN
		display("Resource(1)", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource(1)");
		assertSchemaMetadataUnchanged(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertResourceSchemaParseCountIncrement(0);
		
		assertVersion(resourceProvisioning, resourceType.getVersion());
		
		display("Resource cache (1)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		assertResourceSchemaUnchanged(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		assertRefinedResourceSchemaUnchanged(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		// WHEN
		resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

		// THEN
		display("Resource(2)", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource(2)");
		assertSchemaMetadataUnchanged(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		
		assertVersion(resourceProvisioning, resourceType.getVersion());
		
		display("Resource cache (1)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		assertResourceSchemaUnchanged(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		assertRefinedResourceSchemaUnchanged(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
	}
	
	
	
	private <T extends ObjectType> void assertVersion(PrismObject<T> object, String expectedVersion) {
		assertEquals("Wrong version of "+object, expectedVersion, object.asObjectable().getVersion());
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

	private void assertConnectorSchemaFetchIncrement(int expectedIncrement) {
		long currentConnectorSchemaFetchCount = InternalMonitor.getConnectorSchemaFetchCount();
		long actualIncrement = currentConnectorSchemaFetchCount - lastConnectorSchemaFetchCount;
		assertEquals("Unexpected increment in connector schema fetch count", (long)expectedIncrement, actualIncrement);
		lastConnectorSchemaFetchCount = currentConnectorSchemaFetchCount;
	}

	private void rememberConnectorInitializationCount() {
		lastConnectorInitializationCount  = InternalMonitor.getConnectorInitializationCount();
	}

	private void assertConnectorInitializationCountIncrement(int expectedIncrement) {
		long currentConnectorInitializationCount = InternalMonitor.getConnectorInitializationCount();
		long actualIncrement = currentConnectorInitializationCount - lastConnectorInitializationCount;
		assertEquals("Unexpected increment in connector initialization count", (long)expectedIncrement, actualIncrement);
		lastConnectorInitializationCount = currentConnectorInitializationCount;
	}
	
	private void rememberResourceSchemaParseCount() {
		lastResourceSchemaParseCount  = InternalMonitor.getResourceSchemaParseCount();
	}

	private void assertResourceSchemaParseCountIncrement(int expectedIncrement) {
		long currentResourceSchemaParseCount = InternalMonitor.getResourceSchemaParseCount();
		long actualIncrement = currentResourceSchemaParseCount - lastResourceSchemaParseCount;
		assertEquals("Unexpected increment in resource schema parse count", (long)expectedIncrement, actualIncrement);
		lastResourceSchemaParseCount = currentResourceSchemaParseCount;
	}
	
	private void rememberResourceCacheStats() {
		lastResourceCacheStats  = InternalMonitor.getResourceCacheStats().clone();
	}
	
	private void assertResourceCacheHitsIncrement(int expectedIncrement) {
		assertCacheHits(lastResourceCacheStats, InternalMonitor.getResourceCacheStats(), "resouce cache", expectedIncrement);
	}
	
	private void assertResourceCacheMissesIncrement(int expectedIncrement) {
		assertCacheMisses(lastResourceCacheStats, InternalMonitor.getResourceCacheStats(), "resouce cache", expectedIncrement);
	}
	
	private void assertCacheHits(CachingStatistics lastStats, CachingStatistics currentStats, String desc, int expectedIncrement) {
		long actualIncrement = currentStats.getHits() - lastStats.getHits();
		assertEquals("Unexpected increment in "+desc+" hit count", (long)expectedIncrement, actualIncrement);
		lastStats.setHits(currentStats.getHits());
	}

	private void assertCacheMisses(CachingStatistics lastStats, CachingStatistics currentStats, String desc, int expectedIncrement) {
		long actualIncrement = currentStats.getMisses() - lastStats.getMisses();
		assertEquals("Unexpected increment in "+desc+" miss count", (long)expectedIncrement, actualIncrement);
		lastStats.setMisses(currentStats.getMisses());
	}
	
	private void rememberResourceSchema(ResourceSchema resourceSchema) {
		lastResourceSchema = resourceSchema;
	}
	
	private void assertResourceSchemaUnchanged(ResourceSchema currentResourceSchema) {
		// We really want == there. We want to make sure that this is actually the same instance and that
		// it was properly cached
		assertTrue("Resource schema has changed", lastResourceSchema == currentResourceSchema);
	}
	
	private void rememberRefinedResourceSchema(RefinedResourceSchema rResourceSchema) {
		lastRefinedResourceSchema = rResourceSchema;
	}
	
	private void assertRefinedResourceSchemaUnchanged(RefinedResourceSchema currentRefinedResourceSchema) {
		// We really want == there. We want to make sure that this is actually the same instance and that
		// it was properly cached
		assertTrue("Refined resource schema has changed", lastRefinedResourceSchema == currentRefinedResourceSchema);
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
