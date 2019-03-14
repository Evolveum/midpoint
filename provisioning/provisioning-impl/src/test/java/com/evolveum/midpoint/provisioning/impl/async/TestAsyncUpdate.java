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

package com.evolveum.midpoint.provisioning.impl.async;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil.checkRepoAccountShadow;
import static com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil.checkRepoShadow;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 * @author mederly
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class TestAsyncUpdate extends AbstractProvisioningIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/async/");

	static final File RESOURCE_ASYNC_CACHING_FILE = new File(TEST_DIR, "resource-async-caching.xml");
	static final File RESOURCE_ASYNC_NO_CACHING_FILE = new File(TEST_DIR, "resource-async-no-caching.xml");
	static final File RESOURCE_ASYNC_CACHING_AMQP_FILE = new File(TEST_DIR, "resource-async-caching-amqp.xml");
	private static final String RESOURCE_ASYNC_OID = "fb04d113-ebf8-41b4-b13b-990a597d110b";

	private static final File CHANGE_100 = new File(TEST_DIR, "change-100-banderson-first-occurrence.xml");
	private static final File CHANGE_110 = new File(TEST_DIR, "change-110-banderson-delta.xml");
	private static final File CHANGE_120 = new File(TEST_DIR, "change-120-banderson-new-state.xml");
	private static final File CHANGE_125 = new File(TEST_DIR, "change-125-banderson-notification-only.xml");
	private static final File CHANGE_130 = new File(TEST_DIR, "change-130-banderson-delete.xml");

	private static final QName RESOURCE_ACCOUNT_OBJECTCLASS = new QName(MidPointConstants.NS_RI, "AccountObjectClass");

	static final String ASYNC_CONNECTOR_TYPE = "AsyncUpdateConnector";

	@SuppressWarnings("unused")
	private static final Trace LOGGER = TraceManager.getTrace(TestAsyncUpdate.class);

	private static final long TIMEOUT = 5000L;

	protected PrismObject<ResourceType> resource;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;

		super.initSystem(initTask, initResult);

		syncServiceMock.setSupportActivation(false);
		resource = addResourceFromFile(getResourceFile(), getConnectorTypes(), false, initResult);

		InternalsConfig.setSanityChecks(true);
	}

	@NotNull
	public abstract List<String> getConnectorTypes();

	protected abstract File getResourceFile();

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

		ResourceTypeUtil.getResourceXsdSchema(resourceBefore);

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

		assertEquals("Unexpected number of definitions", 3, accountDef.getDefinitions().size());
	}

	@Test
	public void test100ListeningForShadowAdd() throws Exception {
		final String TEST_NAME = "test100ListeningForShadowAdd";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAsyncUpdate.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		prepareMessage(CHANGE_100);

		syncServiceMock.reset();

		addDummyAccount("banderson");

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_ASYNC_OID);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		syncServiceMock.waitForNotifyChange(TIMEOUT);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

		assertNotNull("Delta is missing", lastChange.getObjectDelta());
		assertNotNull("Current shadow is not present", lastChange.getCurrentShadow());

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername("banderson", resource, result);
		assertNotNull("Shadow was not created in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoAccountShadow(accountRepo);
	}

	@Test
	public void test110ListeningForValueAdd() throws Exception {
		final String TEST_NAME = "test110ListeningForValueAdd";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAsyncUpdate.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		prepareMessage(CHANGE_110);

		syncServiceMock.reset();

		setDummyAccountTestAttribute("banderson", "value1", "value2", "value3");

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_ASYNC_OID);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		syncServiceMock.waitForNotifyChange(TIMEOUT);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

		assertNotNull("Delta is missing", lastChange.getObjectDelta());
		assertTrue("Delta is not a MODIFY one", lastChange.getObjectDelta().isModify());
		Collection<? extends ItemDelta<?, ?>> modifications = lastChange.getObjectDelta().getModifications();
		assertEquals("Wrong # of modifications", 1, modifications.size());
		assertEquals("Wrong # of values added", 3, modifications.iterator().next().getValuesToAdd().size());
		assertNotNull("Current shadow is not present", lastChange.getCurrentShadow());

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername("banderson", resource, result);
		assertNotNull("Shadow is not present in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoShadow(accountRepo, ShadowKindType.ACCOUNT, getNumberOfAccountAttributes());
	}

	@Test
	public void test120ListeningForShadowReplace() throws Exception {
		final String TEST_NAME = "test120ListeningForShadowReplace";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAsyncUpdate.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		prepareMessage(CHANGE_120);

		syncServiceMock.reset();

		setDummyAccountTestAttribute("banderson", "value4");

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_ASYNC_OID);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		syncServiceMock.waitForNotifyChange(TIMEOUT);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

		assertNull("Delta is present although it should not be", lastChange.getObjectDelta());
		assertNotNull("Current shadow is missing", lastChange.getCurrentShadow());

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername("banderson", resource, result);
		assertNotNull("Shadow is not present in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoShadow(accountRepo, ShadowKindType.ACCOUNT, getNumberOfAccountAttributes());
	}


	@Test
	public void test125ListeningForNotificationOnly() throws Exception {

		if (!hasReadCapability()) {
			System.out.println("Skipping this test because there's no real read capability");
			return;
		}

		final String TEST_NAME = "test125ListeningForNotificationOnly";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAsyncUpdate.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		prepareMessage(CHANGE_125);

		syncServiceMock.reset();

		setDummyAccountTestAttribute("banderson", "value125");

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_ASYNC_OID);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		syncServiceMock.waitForNotifyChange(TIMEOUT);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

		assertNull("Delta is present although it should not be", lastChange.getObjectDelta());
		assertNotNull("Current shadow is missing", lastChange.getCurrentShadow());

		display("change current shadow", lastChange.getCurrentShadow());

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername("banderson", resource, result);
		assertNotNull("Shadow is not present in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoShadow(accountRepo, ShadowKindType.ACCOUNT, getNumberOfAccountAttributes());
	}

	@Test
	public void test130ListeningForShadowDelete() throws Exception {
		final String TEST_NAME = "test130ListeningForShadowDelete";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAsyncUpdate.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		prepareMessage(CHANGE_130);

		syncServiceMock.reset();

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_ASYNC_OID);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		syncServiceMock.waitForNotifyChange(TIMEOUT);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

		assertNotNull("Delta is missing", lastChange.getObjectDelta());
		assertTrue("Delta is not a DELETE one", lastChange.getObjectDelta().isDelete());
		//assertNull("Current shadow is present while not expecting it", lastChange.getCurrentShadow());
		//current shadow was added during the processing

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername("banderson", resource, result);
		assertNotNull("Shadow is not present in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoShadow(accountRepo, ShadowKindType.ACCOUNT, getNumberOfAccountAttributes());
	}

	@SuppressWarnings("SameParameterValue")
	void addDummyAccount(String name) {
	}

	@SuppressWarnings("SameParameterValue")
	void setDummyAccountTestAttribute(String name, String... values) {
	}

	abstract int getNumberOfAccountAttributes();

	boolean hasReadCapability() {
		return false;
	}

	void prepareMessage(File messageFile)
			throws java.io.IOException, com.evolveum.midpoint.util.exception.SchemaException, TimeoutException {
		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(messageFile).parseRealValue());
	}
}
