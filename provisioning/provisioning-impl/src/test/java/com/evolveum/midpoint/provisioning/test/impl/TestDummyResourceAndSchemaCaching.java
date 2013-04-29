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
import java.util.ArrayList;
import java.util.Collection;
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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
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
	private Long lastResourceVersion = null;
	private ConnectorInstance lastConfiguredConnectorInstance;
	
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
		
		rememberConnectorInstance(resourceProvisioning);
				
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
		rememberResourceVersion(resourceType.getVersion());
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
		
		assertResourceVersionIncrement(resourceProvisioning, 0);
		
		display("Resource cache (1)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		assertResourceSchemaUnchanged(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		assertRefinedResourceSchemaUnchanged(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		assertConnectorInstanceUnchanged(resourceProvisioning);
		
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
		
		assertResourceVersionIncrement(resourceProvisioning, 0);
		
		display("Resource cache (1)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		assertResourceSchemaUnchanged(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		assertRefinedResourceSchemaUnchanged(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		assertConnectorInstanceUnchanged(resourceProvisioning);
	}
	
	/**
	 * Add new account. This is connector operation. Check that the initialized connector is
	 * reused and that it is not initialized again. Check that resource is still cached.
	 */
	@Test
	public void test012AddAccountGetResource() throws Exception {
		final String TEST_NAME = "test012AddAccountGetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		
		// WHEN
		addAccount(ACCOUNT_WILL_FILENAME);
		
		// THEN
		display("Resource cache (1)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Resource(2)", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource(2)");
		assertSchemaMetadataUnchanged(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertResourceSchemaParseCountIncrement(0);
		
		assertResourceVersionIncrement(resourceProvisioning, 0);
		
		display("Resource cache (2)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		assertResourceSchemaUnchanged(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		assertRefinedResourceSchemaUnchanged(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		assertConnectorInstanceUnchanged(resourceProvisioning);
	}
	
	
	/**
	 * Change something that is not important. The cached resource should be refreshed, the schema re-parsed
	 * but the connector should still be cached.
	 */
	@Test
	public void test020ModifyAndGetResource() throws Exception {
		final String TEST_NAME = "test020ModifyAndGetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		// Change something that's not that important
		AccountSynchronizationSettingsType accountSynchronizationSettingsType = new AccountSynchronizationSettingsType();
		accountSynchronizationSettingsType.setLegalize(true);
		
		ObjectDelta<ResourceType> objectDelta = ObjectDelta.createModificationReplaceProperty(ResourceType.class, RESOURCE_DUMMY_OID, 
				ResourceType.F_ACCOUNT_SYNCHRONIZATION_SETTINGS, prismContext, accountSynchronizationSettingsType);
		
		// WHEN
		provisioningService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, objectDelta.getModifications(), null, null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);

		String versionAfter = repositoryService.getVersion(ResourceType.class, RESOURCE_DUMMY_OID, result);
		assertResourceVersionIncrement(versionAfter, 1);

		// WHEN
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

		// THEN
		display("Resource", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource");
		assertSchemaMetadataUnchanged(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertResourceSchemaParseCountIncrement(1);
		
		assertResourceVersionIncrement(resourceProvisioning, 0);
		
		display("Resource cache", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(0);
		assertResourceCacheMissesIncrement(1);
		
		// There are expected to be re-parsed
		rememberResourceSchema(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		rememberRefinedResourceSchema(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		assertConnectorInstanceUnchanged(resourceProvisioning);
	}
	
	/**
	 * Get the account. This is connector operation. Check that the initialized connector is
	 * reused and that it is not initialized again. Check that resource is still cached.
	 */
	@Test
	public void test022GetAccountGetResource() throws Exception {
		final String TEST_NAME = "test012AddAccountGetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		
		// WHEN
		getAccount(ACCOUNT_WILL_OID);
		
		// THEN
		display("Resource cache (1)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Resource(2)", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource(2)");
		assertSchemaMetadataUnchanged(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertResourceSchemaParseCountIncrement(0);
		
		assertResourceVersionIncrement(resourceProvisioning, 0);
		
		display("Resource cache (2)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		assertResourceSchemaUnchanged(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		assertRefinedResourceSchemaUnchanged(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		assertConnectorInstanceUnchanged(resourceProvisioning);
	}
	
	/**
	 * Change resource directly in repo. This simulates the change done by other node. The connector cache should
	 * be refreshed.
	 * 
	 * Change something that is not important. The cached resource should be refreshed, the schema re-parsed
	 * but the connector should still be cached.
	 */
	@Test
	public void test023ModifyRepoAndGetResource() throws Exception {
		final String TEST_NAME = "test023ModifyRepoAndGetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		// Change something that's not that important
		AccountSynchronizationSettingsType accountSynchronizationSettingsType = new AccountSynchronizationSettingsType();
		accountSynchronizationSettingsType.setLegalize(true);
		
		ObjectDelta<ResourceType> objectDelta = ObjectDelta.createModificationReplaceProperty(ResourceType.class, RESOURCE_DUMMY_OID, 
				ResourceType.F_ACCOUNT_SYNCHRONIZATION_SETTINGS, prismContext, accountSynchronizationSettingsType);
		
		// WHEN
		repositoryService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, objectDelta.getModifications(), result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);

		String versionAfter = repositoryService.getVersion(ResourceType.class, RESOURCE_DUMMY_OID, result);
		assertResourceVersionIncrement(versionAfter, 1);

		// WHEN
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

		// THEN
		display("Resource", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource");
		assertSchemaMetadataUnchanged(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertResourceSchemaParseCountIncrement(1);
		
		assertResourceVersionIncrement(resourceProvisioning, 0);
		
		display("Resource cache", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(0);
		assertResourceCacheMissesIncrement(1);
		
		// There are expected to be re-parsed
		rememberResourceSchema(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		rememberRefinedResourceSchema(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		assertConnectorInstanceUnchanged(resourceProvisioning);
	}
	
	/**
	 * Change part of connector configuration. The cached resource should be refreshed, the schema re-parsed.
	 * The connector also needs to re-initialized.
	 */
	@Test
	public void test030ModifyConnectorConfigAndGetResource() throws Exception {
		final String TEST_NAME = "test030ModifyConnectorConfigAndGetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		// Change part of connector configuration. We change quite a useless part. But midPoint does not know that
		// it is useless and need to re-initialize the connector
		Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
		PropertyDelta<String> uselessStringDelta = createUselessStringDelta("patlama chamalalija paprtala");
		((Collection)modifications).add(uselessStringDelta);
		
		// WHEN
		provisioningService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, modifications, null, null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		assertConnectorConfigChanged();
	}
	
	/**
	 * Change part of connector configuration. Change it directly in repo to simulate change
	 * from another midPoint node. 
	 * 
	 * The cached resource should be refreshed, the schema re-parsed.
	 * The connector also needs to re-initialized.
	 */
	@Test
	public void test031ModifyConnectorConfigRepoAndGetResource() throws Exception {
		final String TEST_NAME = "test031ModifyConnectorConfigRepoAndGetResource";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyResourceAndSchemaCaching.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		// Change part of connector configuration. We change quite a useless part. But midPoint does not know that
		// it is useless and need to re-initialize the connector
		Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
		PropertyDelta<String> uselessStringDelta = createUselessStringDelta("Rudolfovo Tajemstvi");
		((Collection)modifications).add(uselessStringDelta);
		
		// WHEN
		repositoryService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, modifications, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		assertConnectorConfigChanged();
	}
	
	private PropertyDelta<String> createUselessStringDelta(String newVal) {
		PropertyDelta<String> uselessStringDelta = PropertyDelta.createModificationReplaceProperty(
				new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, 
							 ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME,
							 ProvisioningTestUtil.CONNECTOR_DUMMY_USELESS_STRING_QNAME),
							 new PrismPropertyDefinition(ProvisioningTestUtil.CONNECTOR_DUMMY_USELESS_STRING_QNAME, ProvisioningTestUtil.CONNECTOR_DUMMY_USELESS_STRING_QNAME, DOMUtil.XSD_STRING, prismContext),
							 newVal);
		return uselessStringDelta;
	}
		
	private void assertConnectorConfigChanged() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".assertConnectorConfigChanged");
		OperationResult result = task.getResult();

		String versionAfter = repositoryService.getVersion(ResourceType.class, RESOURCE_DUMMY_OID, result);
		assertResourceVersionIncrement(versionAfter, 1);

		// WHEN
		PrismObject<ResourceType> resourceProvisioning = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

		// THEN
		display("Resource", resource);
		result.computeStatus();
		assertSuccess(result);

		assertHasSchema(resourceProvisioning, "provisioning resource");
		assertSchemaMetadataUnchanged(resourceProvisioning);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertResourceSchemaParseCountIncrement(1);
		
		assertResourceVersionIncrement(resourceProvisioning, 0);
		
		display("Resource cache", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(0);
		assertResourceCacheMissesIncrement(1);
		
		// There are expected to be re-parsed
		rememberResourceSchema(RefinedResourceSchema.getResourceSchema(resourceProvisioning, prismContext));
		rememberRefinedResourceSchema(RefinedResourceSchema.getRefinedSchema(resourceProvisioning));
		assertResourceSchemaParseCountIncrement(0);
		
		// WHEN
		getAccount(ACCOUNT_WILL_OID);
		
		// THEN
		display("Resource cache (2)", InternalMonitor.getResourceCacheStats());
		assertResourceCacheHitsIncrement(1);
		assertResourceCacheMissesIncrement(0);
		
		assertConnectorSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(1);
		assertResourceSchemaParseCountIncrement(0);
		
		assertConnectorInstanceChanged(resourceProvisioning);
	}
	
	private String addAccount(String filename) throws SchemaException, ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".addAccount");
		OperationResult result = task.getResult();
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(filename));
		String oid = provisioningService.addObject(account, null, null, task, result);
		result.computeStatus();
		assertSuccess(result);
		return oid;
	}
	
	private PrismObject<ShadowType> getAccount(String oid) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".getAccount");
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, oid, null, result);
		result.computeStatus();
		assertSuccess(result);
		return account;
	}
	
	private <T extends ObjectType> void assertVersion(PrismObject<T> object, String expectedVersion) {
		assertEquals("Wrong version of "+object, expectedVersion, object.asObjectable().getVersion());
	}

	private void rememberResourceVersion(String version) {
		lastResourceVersion = Long.parseLong(version);
	}
	
	private void assertResourceVersionIncrement(PrismObject<ResourceType> resource, int expectedIncrement) {
		assertResourceVersionIncrement(resource.getVersion(), expectedIncrement);
	}
	
	private void assertResourceVersionIncrement(String currentVersion, int expectedIncrement) {
		long currentVersionLong = Long.parseLong(currentVersion);
		long actualIncrement = currentVersionLong - lastResourceVersion;
		assertEquals("Unexpected increment in resource version", (long)expectedIncrement, actualIncrement);
		lastResourceVersion = currentVersionLong;
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

	private void rememberConnectorInstance(PrismObject<ResourceType> resource) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".rememberConnectorInstance");
		lastConfiguredConnectorInstance = connectorManager.getConfiguredConnectorInstance(resource, false, result);
	}
	
	
	private void assertConnectorInstanceUnchanged(PrismObject<ResourceType> resource) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".rememberConnectorInstance");
		ConnectorInstance currentConfiguredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource, false, result);
		assertTrue("Connector instance has changed", lastConfiguredConnectorInstance == currentConfiguredConnectorInstance);
	}
	
	private void assertConnectorInstanceChanged(PrismObject<ResourceType> resource) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".rememberConnectorInstance");
		ConnectorInstance currentConfiguredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource, false, result);
		assertTrue("Connector instance has NOT changed", lastConfiguredConnectorInstance != currentConfiguredConnectorInstance);
		lastConfiguredConnectorInstance = currentConfiguredConnectorInstance;
	}

}
