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
package com.evolveum.midpoint.provisioning.impl.manual;

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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
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
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class AbstractManualResourceTest extends AbstractProvisioningIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/manual/");
	
	protected static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
	protected static final String RESOURCE_MANUAL_OID = "8a8e19de-1a14-11e7-965f-6f995b457a8b";
	
	protected static final File RESOURCE_SEMI_MANUAL_FILE = new File(TEST_DIR, "resource-semi-manual.xml");
	protected static final String RESOURCE_SEMI_MANUAL_OID = "b6eb1e50-2414-11e7-bf12-579151795f29";
	
	protected static final String MANUAL_CONNECTOR_TYPE = "ManualConnector";
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractManualResourceTest.class);

	protected static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/ManualConnector";
	protected static final QName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new QName(NS_MANUAL_CONF, "defaultAssignee");

	protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
	protected static final String ACCOUNT_WILL_OID = "c1add81e-1df7-11e7-bbb7-5731391ba751";
	protected static final String ACCOUNT_WILL_USERNAME = "will";
	protected static final String ACCOUNT_WILL_FULLNAME = "Will Turner";
	protected static final String ACCOUNT_WILL_FULLNAME_PIRATE = "Pirate Will Turner";

	protected static final String ATTR_USERNAME = "username";
	protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);
	
	protected static final String ATTR_FULLNAME = "fullname";
	protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;
	
	protected XMLGregorianCalendar accountWillReqestTimestampStart;
	protected XMLGregorianCalendar accountWillReqestTimestampEnd;
	
	protected XMLGregorianCalendar accountWillCompletionTimestampStart;
	protected XMLGregorianCalendar accountWillCompletionTimestampEnd;
	
	protected XMLGregorianCalendar accountWillSecondReqestTimestampStart;
	protected XMLGregorianCalendar accountWillSecondReqestTimestampEnd;

	protected XMLGregorianCalendar accountWillSecondCompletionTimestampStart;
	protected XMLGregorianCalendar accountWillSecondCompletionTimestampEnd;

	protected String willLastCaseOid;
	protected String willSecondLastCaseOid;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
				
		super.initSystem(initTask, initResult);
	}
	
	protected abstract File getResourceFile();
	
	protected abstract String getResourceOid();

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTile(TEST_NAME);
		
		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName()
				+ "." + TEST_NAME);

		ResourceType repoResource = repositoryService.getObject(ResourceType.class, getResourceOid(),
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
		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName() + "." + TEST_NAME);
		
		// Check that there is a schema, but no capabilities before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, getResourceOid(),
				null, result).asObjectable();
		
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		assertResourceSchemaBeforeTest(resourceXsdSchemaElementBefore);
		
		CapabilitiesType capabilities = resourceBefore.getCapabilities();
		AssertJUnit.assertNull("Capabilities present before test connection. Bad test setup?", capabilities);

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
	
	protected abstract void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore);

	@Test
	public void test004Configuration() throws Exception {
		final String TEST_NAME = "test004Configuration";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, null, result);
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
		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName() + "." + TEST_NAME);

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
		ResourceType resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, null, result).asObjectable();
		
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
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
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
		assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
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
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);

		closeCase(willLastCaseOid);
		
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
		assertSinglePendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, 
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
		assertSinglePendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		syncServiceMock.assertNoNotifyChange();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();
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
		
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();
	}

	@Test
	public void test110ModifyAccountWillFullname() throws Exception {
		final String TEST_NAME = "test110ModifyAccountWillFullname";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, new ItemPath(ShadowType.F_ATTRIBUTES, ATTR_FULLNAME_QNAME), prismContext, 
				ACCOUNT_WILL_FULLNAME_PIRATE);
		display("ObjectDelta", delta);

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				null, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);
		
		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
				
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	@Test
	public void test112RefreshAccountWill() throws Exception {
		final String TEST_NAME = "test112RefreshAccountWill";
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
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		
	}
	
	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test114CloseCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test114CloseCaseAndRefreshAccountWill";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);

		closeCase(willLastCaseOid);
		
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
		assertSinglePendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

		// In this case check notifications at the end. There were some reads that
		// internally triggered refresh. Maku sure no extra notifications were sent.
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifySuccessOnly();
	}
	
	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test116RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test116RefreshAccountWillAfter5min";
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
		assertSinglePendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * disable - do not complete yet (do not wait for delta to expire, we want several deltas at once).
	 */
	@Test
	public void test120ModifyAccountWillDisable() throws Exception {
		final String TEST_NAME = "test120ModifyAccountWillDisable";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext, 
				ActivationStatusType.DISABLED);
		display("ObjectDelta", delta);

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				null, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);
		
		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
				
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		assertPendingOperationDeltas(shadowProvisioning, 2);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowProvisioning, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.DISABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * Change password, enable. There is still pending disable delta. Make sure all the deltas are
	 * stored correctly.
	 */
	@Test
	public void test122ModifyAccountWillChangePasswordAndEnable() throws Exception {
		final String TEST_NAME = "test122ModifyAccountWillChangePasswordAndEnable";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext, 
				ActivationStatusType.ENABLED);
		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue("ELIZAbeth");
		delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, ps);
		display("ObjectDelta", delta);

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				null, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willSecondLastCaseOid = assertInProgress(result);
		
		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
				
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 3);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);
		
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		assertPendingOperationDeltas(shadowProvisioning, 3);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowProvisioning, pendingOperation, 
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertNotNull("No async reference in result", willSecondLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * Disable case is closed. The account should be disabled. But there is still the other
	 * delta pending.
	 * Do NOT explicitly refresh the shadow in this case. Just reading it should cause the refresh.
	 */
	@Test
	public void test124CloseDisableCaseAndReadAccountWill() throws Exception {
		final String TEST_NAME = "test124CloseDisableCaseAndReadAccountWill";
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
		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		
		assertPendingOperationDeltas(shadowRepo, 3);
		
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.DISABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifySuccessOnly();
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertPendingOperationDeltas(shadowProvisioning, 3);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowProvisioning, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * lets ff 5min just for fun. Refresh, make sure everything should be the same (grace not expired yet)
	 */
	@Test
	public void test126RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test126RefreshAccountWillAfter5min";
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
		
		assertPendingOperationDeltas(shadowRepo, 3);
		
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.DISABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertPendingOperationDeltas(shadowProvisioning, 3);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowProvisioning, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Password change/enable case is closed. The account should be disabled. But there is still the other
	 * delta pending.
	 */
	@Test
	public void test128ClosePasswordChangeCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test128ClosePasswordChangeCaseAndRefreshAccountWill";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		closeCase(willSecondLastCaseOid);
		
		// Get repo shadow here. Make sure refresh works with this as well.
		PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		provisioningService.applyDefinition(shadowBefore, result);
		display("Shadow before", shadowBefore);
		
		accountWillSecondCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		accountWillSecondCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		
		assertPendingOperationDeltas(shadowRepo, 3);
		
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertPendingOperationDeltas(shadowProvisioning, 3);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 10min. Refresh. Oldest delta should expire.
	 */
	@Test
	public void test130RefreshAccountWillAfter10min() throws Exception {
		final String TEST_NAME = "test130RefreshAccountWillAfter10min";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		clock.overrideDuration("PT10M");
		
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
		
		assertPendingOperationDeltas(shadowRepo, 2);
		
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertPendingOperationDeltas(shadowProvisioning, 2);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 5min. Refresh. Another delta should expire.
	 */
	@Test
	public void test132RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test132RefreshAccountWillAfter10min";
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
		
		assertPendingOperationDeltas(shadowRepo, 1);
		
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertPendingOperationDeltas(shadowProvisioning, 1);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 5min. Refresh. All delta should expire.
	 */
	@Test
	public void test134RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test134RefreshAccountWillAfter5min";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		clock.overrideDuration("PT5M");
		
		PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Shadow before", shadowBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		
		assertPendingOperationDeltas(shadowRepo, 0);
				
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertPendingOperationDeltas(shadowProvisioning, 0);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowProvisioningFuture.asObjectable(), new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	@Test
	public void test140DeleteAccountWill() throws Exception {
		final String TEST_NAME = "test140DeleteAccountWill";
		displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);
		
		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
				
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 1);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, 
				OperationResultStatusType.IN_PROGRESS, null);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertActivationAdministrativeStatus(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		assertPendingOperationDeltas(shadowProvisioning, 1);
		pendingOperation = findPendingOperation(shadowProvisioning, 
				OperationResultStatusType.IN_PROGRESS, null);
		assertPendingOperation(shadowProvisioning, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertShadowDead(shadowProvisioningFuture);
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test144CloseCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test144CloseCaseAndRefreshAccountWill";
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
		assertSinglePendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertShadowDead(shadowRepo);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowDead(shadowRepo);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertShadowDead(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test146RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test146RefreshAccountWillAfter5min";
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
		assertSinglePendingOperation(shadowRepo, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertShadowDead(shadowRepo);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowDead(shadowRepo);
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadowTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertShadowDead(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	private void assertPendingOperationDeltas(PrismObject<ShadowType> shadow, int expectedNumber) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		assertEquals("Wroung number of pending operations in "+shadow, expectedNumber, pendingOperations.size());
	}

	private PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow, 
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertSinglePendingOperation(shadow, requestStart, requestEnd, 
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	private PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow, 
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			OperationResultStatusType expectedStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		assertPendingOperationDeltas(shadow, 1);
		return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0),
				requestStart, requestEnd, expectedStatus, completionStart, completionEnd);
	}
	
	private PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation, 
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertPendingOperation(shadow, pendingOperation, requestStart, requestEnd, 
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	private PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			OperationResultStatusType expectedStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		assertNotNull("No operation ", pendingOperation);
		
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
	
	private PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow, 
			OperationResultStatusType expectedResult, ItemPath itemPath) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		for (PendingOperationType pendingOperation: pendingOperations) {
			OperationResultStatusType result = pendingOperation.getResultStatus();
			if (result == null) {
				result = OperationResultStatusType.IN_PROGRESS;
			}
			if (pendingOperation.getResultStatus() != expectedResult) {
				continue;
			}
			if (itemPath == null) {
				return pendingOperation;
			}
			ObjectDeltaType delta = pendingOperation.getDelta();
			assertNotNull("No delta in pending operation in "+shadow, delta);
			for (ItemDeltaType itemDelta: delta.getItemDelta()) {
				ItemPath deltaPath = itemDelta.getPath().getItemPath();
				if (itemPath.equivalent(deltaPath)) {
					return pendingOperation;
				}
			}			
		}
		return null;
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