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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
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
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AbstractWriteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

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

	private static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
	private static final String ACCOUNT_WILL_OID = "c1add81e-1df7-11e7-bbb7-5731391ba751";
	private static final String ACCOUNT_WILL_USERNAME = "will";

	private static final String ATTR_USERNAME = "username";
	private static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

	private PrismObject<ResourceType> resource;
	private ResourceType resourceType;
	
	private XMLGregorianCalendar accountWillReqestTimestampStart;
	private XMLGregorianCalendar accountWillReqestTimestampEnd;
	
	private XMLGregorianCalendar accountWillCompletionTimestampStart;
	private XMLGregorianCalendar accountWillCompletionTimestampEnd;
	
	private String willLastCaseOid;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
				
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
        assertManual(capCreate);
        
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNotNull("Missing activation capability", capAct);
        
        ReadCapabilityType capRead = CapabilityUtil.getCapability(nativeCapabilitiesList, ReadCapabilityType.class);
        assertNotNull("Missing read capability" ,capRead);
        assertEquals("Wrong caching-only setting in read capability", Boolean.TRUE, capRead.isCachingOnly());
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }
        
	}

	private void assertManual(AbstractWriteCapabilityType cap) {
		assertEquals("Manual flag not set in capability "+cap, Boolean.TRUE, cap.isManual());
	}

	@Test
	public void test100AddAccountWill() throws Exception {
		final String TEST_NAME = "test100AddAccountWill";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> account = parseObject(ACCOUNT_WILL_FILE);
		account.checkConsistence();

		display("Adding shadow", account);
		
		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);
		
		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);
		account.checkConsistence();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertPendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
			
		syncServiceMock.assertNoNotifyChange();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		assertPendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * Case haven't changed. There should be no change in the shadow.
	 */
	@Test
	public void test102RefreshAccountWill() throws Exception {
		final String TEST_NAME = "test102RefreshAccountWill";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
			
		syncServiceMock.assertNoNotifyChange();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		PendingOperationType pendingOperation = assertPendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		assertCase(pendingOperation.getAsynchronousOperationReference(), SchemaConstants.CASE_STATE_OPEN);
	}
	

	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test104CloseCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test104CloseCaseAndRefreshAccountWill";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		closeCase(willLastCaseOid);
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);
		
		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
			
		syncServiceMock.assertNoNotifyChange();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		PendingOperationType pendingOperation = assertPendingOperation(shadowProvisioning, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test106RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test106RefreshAccountWillAfter5min";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		clock.overrideDuration("PT5M");
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		syncServiceMock.assertNoNotifyChange();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);

		PendingOperationType pendingOperation = assertPendingOperation(shadowProvisioning, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 20min, grace should expire
	 */
	@Test
	public void test108RefreshAccountWillAfter25min() throws Exception {
		final String TEST_NAME = "test108RefreshAccountWillAfter25min";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		clock.overrideDuration("PT20M");
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertNoPendingOperation(shadowRepo);
		syncServiceMock.assertNoNotifyChange();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		assertNoPendingOperation(shadowProvisioning);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	
	private PendingOperationType assertPendingOperation(PrismObject<ShadowType> shadow, 
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertPendingOperation(shadow, requestStart, requestEnd, 
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	private PendingOperationType assertPendingOperation(PrismObject<ShadowType> shadow, 
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			OperationResultStatusType expectedStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		assertEquals("Wroung number of pending operations in "+shadow, 1, pendingOperations.size());
		PendingOperationType pendingOperation = pendingOperations.get(0);
		
		ObjectDeltaType deltaType = pendingOperation.getDelta();
		assertNotNull("No delta in pending operation in "+shadow, deltaType);
		// TODO: check content of pending operations in the shadow
		
		TestUtil.assertBetween("No request timestamp in pending operation in "+shadow, requestStart, requestEnd, pendingOperation.getRequestTimestamp());
		
		OperationResultStatusType status = pendingOperation.getResultStatus();
		assertEquals("Wrong status in pending operation in "+shadow, expectedStatus, status);
		
		if (expectedStatus != OperationResultStatusType.IN_PROGRESS) {
			TestUtil.assertBetween("No completion timestamp in pending operation in "+shadow, completionStart, completionEnd, pendingOperation.getCompletionTimestamp());
		}
		
		return pendingOperation;
	}
	
	private void assertNoPendingOperation(PrismObject<ShadowType> shadow) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		assertEquals("Wroung number of pending operations in "+shadow, 0, pendingOperations.size());
	}

	protected <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow.asObjectable(), attrName, expectedValues);
	}
	
	private void assertCase(String oid, String expectedState) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertCase");
		PrismObject<CaseType> acase = repositoryService.getObject(CaseType.class, oid, null, result);
		display("Case", acase);
		CaseType caseType = acase.asObjectable();
		assertEquals("Wrong state of "+acase, expectedState ,caseType.getState());
	}
	
	private void closeCase(String caseOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		OperationResult result = new OperationResult("closeCase");
		Collection modifications = new ArrayList<>(1);
		
		PrismPropertyDefinition<String> statePropertyDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(CaseType.class).findPropertyDefinition(CaseType.F_STATE);
		PropertyDelta<String> statusDelta = statePropertyDef.createEmptyDelta(new ItemPath(CaseType.F_STATE));
		statusDelta.addValueToReplace(new PrismPropertyValue<>(SchemaConstants.CASE_STATE_CLOSED));
		modifications.add(statusDelta);
		
		PrismPropertyDefinition<String> outcomePropertyDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(CaseType.class).findPropertyDefinition(CaseType.F_OUTCOME);
		PropertyDelta<String> outcomeDelta = outcomePropertyDef.createEmptyDelta(new ItemPath(CaseType.F_OUTCOME));
		outcomeDelta.addValueToReplace(new PrismPropertyValue<>(OperationResultStatusType.SUCCESS.value()));
		modifications.add(outcomeDelta);
		
		repositoryService.modifyObject(CaseType.class, caseOid, modifications, null, result);
		
		PrismObject<CaseType> caseClosed = repositoryService.getObject(CaseType.class, caseOid, null, result);
		display("Case closed", caseClosed);
	}
}