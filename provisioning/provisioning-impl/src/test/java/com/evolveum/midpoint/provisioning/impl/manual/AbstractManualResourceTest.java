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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
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
	protected static final String RESOURCE_SEMI_MANUAL_OID = "8a8e19de-1a14-11e7-965f-6f995b457a8b";

	public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = new QName(MidPointConstants.NS_RI, "AccountObjectClass");
	
	protected static final String MANUAL_CONNECTOR_TYPE = "ManualConnector";
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractManualResourceTest.class);

	protected static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/ManualConnector";
	protected static final QName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new QName(NS_MANUAL_CONF, "defaultAssignee");

	protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
	protected static final String ACCOUNT_WILL_OID = "c1add81e-1df7-11e7-bbb7-5731391ba751";
	protected static final String ACCOUNT_WILL_USERNAME = "will";
	protected static final String ACCOUNT_WILL_FULLNAME = "Will Turner";
	protected static final String ACCOUNT_WILL_FULLNAME_PIRATE = "Pirate Will Turner";
	protected static final String ACCOUNT_WILL_DESCRIPTION_MANUAL = "manual";
	protected static final String ACCOUNT_WILL_PASSWORD_OLD = "3lizab3th";
	protected static final String ACCOUNT_WILL_PASSWORD_NEW = "ELIZAbeth";

	protected static final String ATTR_USERNAME = "username";
	protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);
	
	protected static final String ATTR_FULLNAME = "fullname";
	protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);
	
	protected static final String ATTR_DESCRIPTION = "description";
	protected static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);


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
		
		InternalsConfig.setSanityChecks(true);
	}
	
	protected abstract File getResourceFile();
	
	protected String getResourceOid() {
		return RESOURCE_MANUAL_OID;
	}
	
	protected boolean supportsBackingStore() {
		return false;
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(TEST_NAME);
		
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
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// Check that there is a schema, but no capabilities before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, getResourceOid(),
				null, result).asObjectable();
		
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		assertResourceSchemaBeforeTest(resourceXsdSchemaElementBefore);
		
		CapabilitiesType capabilities = resourceBefore.getCapabilities();
		if (capabilities != null) {
			AssertJUnit.assertNull("Native capabilities present before test connection. Bad test setup?", capabilities.getNative());
		}

		// WHEN
		OperationResult testResult = provisioningService.testResource(getResourceOid(), task);

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
		TestUtil.displayTestTitle(TEST_NAME);
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
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName() + "." + TEST_NAME);

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchemaImpl.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext);
		
		display("Parsed resource schema", resourceSchema);

		// Check whether it is reusing the existing schema and not parsing it all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching", resourceSchema == RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext));
		
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findObjectClassDefinition(RESOURCE_ACCOUNT_OBJECTCLASS);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		
		assertEquals("Unexpected number of definitions", getNumberOfAccountAttributeDefinitions(), accountDef.getDefinitions().size());
		
		ResourceAttributeDefinition<String> usernameDef = accountDef.findAttributeDefinition(ATTR_USERNAME);
		assertNotNull("No definition for username", usernameDef);
		assertEquals(1, usernameDef.getMaxOccurs());
		assertEquals(1, usernameDef.getMinOccurs());
		assertTrue("No username create", usernameDef.canAdd());
		assertTrue("No username update", usernameDef.canModify());
		assertTrue("No username read", usernameDef.canRead());
		
		ResourceAttributeDefinition<String> fullnameDef = accountDef.findAttributeDefinition(ATTR_FULLNAME);
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(0, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canAdd());
		assertTrue("No fullname update", fullnameDef.canModify());
		assertTrue("No fullname read", fullnameDef.canRead());
	}
	
	protected int getNumberOfAccountAttributeDefinitions() {
		return 4;
	}

	@Test
	public void test006Capabilities() throws Exception {
		final String TEST_NAME = "test006Capabilities";
		TestUtil.displayTestTitle(TEST_NAME);

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

	@Test
	public void test100AddAccountWill() throws Exception {
		final String TEST_NAME = "test100AddAccountWill";
		displayTestTitle(TEST_NAME);
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
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertShadowExists(shadowRepo, false);
		assertNoShadowPassword(shadowRepo);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowProvisioning, ActivationStatusType.ENABLED);
		assertShadowExists(shadowProvisioning, false);
		assertNoShadowPassword(shadowProvisioning);

		assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	

	@Test
	public void test102GetAccountWillFuture() throws Exception {
		final String TEST_NAME = "test102GetAccountWillFuture";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);	
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertNoAttribute(shadowProvisioning, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertShadowExists(shadowProvisioning, true);
		// TODO 
//		assertShadowPassword(shadowProvisioning);
	}

	/**
	 * Case haven't changed. There should be no change in the shadow.
	 */
	@Test
	public void test104RefreshAccountWill() throws Exception {
		final String TEST_NAME = "test104RefreshAccountWill";
		displayTestTitle(TEST_NAME);
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
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, false);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertNoAttribute(shadowProvisioning, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowProvisioning, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowProvisioning);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		assertCase(pendingOperation.getAsynchronousOperationReference(), SchemaConstants.CASE_STATE_OPEN);
	}

	protected void backingStoreAddWill() throws IOException {
		// nothing to do here
	}
	
	@Test
	public void test106AddToBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test106AddToBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		backingStoreAddWill();
		
		syncServiceMock.reset();
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);	
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);		
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertShadowExists(shadowProvisioning, supportsBackingStore());
		assertShadowPassword(shadowProvisioning);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, supportsBackingStore());
	}
	
	@Test
	public void test108GetAccountWillFuture() throws Exception {
		final String TEST_NAME = "test108GetAccountWillFuture";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);	
		
		display("Provisioning shadow", shadowProvisioning);
		assertNotNull("no OID", shadowProvisioning.getOid());
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);		
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertShadowExists(shadowProvisioning, true);
		// TODO
//		assertShadowPassword(shadowProvisioning);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, supportsBackingStore());
	}
	
	@Test
	public void test109GetAccountWillFutureNoFetch() throws Exception {
		final String TEST_NAME = "test109GetAccountWillFutureNoFetch";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		GetOperationOptions options = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
		options.setNoFetch(true);
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(options),
				task, result);
		
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		display("Provisioning shadow (future,noFetch)", shadowProvisioningFuture);
		assertNotNull("no OID", shadowProvisioningFuture.getOid());
		ShadowType shadowTypeProvisioning = shadowProvisioningFuture.asObjectable();
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertNoAttribute(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME);		
		assertShadowActivationAdministrativeStatusFromCache(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertShadowExists(shadowProvisioningFuture, true);
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);
	}
	
	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test110CloseCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test110CloseCaseAndRefreshAccountWill";
		displayTestTitle(TEST_NAME);
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
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
			
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
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertShadowPassword(shadowProvisioning);
		
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
	public void test120RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test120RefreshAccountWillAfter5min";
		displayTestTitle(TEST_NAME);
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
	public void test130RefreshAccountWillAfter25min() throws Exception {
		final String TEST_NAME = "test130RefreshAccountWillAfter25min";
		displayTestTitle(TEST_NAME);
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
	public void test200ModifyAccountWillFullname() throws Exception {
		final String TEST_NAME = "test200ModifyAccountWillFullname";
		displayTestTitle(TEST_NAME);
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
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

		assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	@Test
	public void test202RefreshAccountWill() throws Exception {
		final String TEST_NAME = "test202RefreshAccountWill";
		displayTestTitle(TEST_NAME);
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
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		assertShadowPassword(shadowProvisioning);

		assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		
	}
	
	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test204CloseCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test204CloseCaseAndRefreshAccountWill";
		displayTestTitle(TEST_NAME);
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
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		if (supportsBackingStore()) {
			assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		} else {
			assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		}
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);		

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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
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
	public void test210RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test210RefreshAccountWillAfter5min";
		displayTestTitle(TEST_NAME);
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
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		if (supportsBackingStore()) {
			assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
		} else {
			assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		}
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	protected void backingStoreUpdateWill(String newFullName, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
		// nothing to do here
	}
	
	@Test
	public void test212UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test212UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);
		
		backingStoreUpdateWill(ACCOUNT_WILL_FULLNAME_PIRATE, ActivationStatusType.ENABLED, ACCOUNT_WILL_PASSWORD_OLD);
		
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
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	
	/**
	 * disable - do not complete yet (do not wait for delta to expire, we want several deltas at once).
	 */
	@Test
	public void test220ModifyAccountWillDisable() throws Exception {
		final String TEST_NAME = "test220ModifyAccountWillDisable";
		displayTestTitle(TEST_NAME);
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
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.DISABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * Change password, enable. There is still pending disable delta. Make sure all the deltas are
	 * stored correctly.
	 */
	@Test
	public void test230ModifyAccountWillChangePasswordAndEnable() throws Exception {
		final String TEST_NAME = "test230ModifyAccountWillChangePasswordAndEnable";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext, 
				ActivationStatusType.ENABLED);
		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue(ACCOUNT_WILL_PASSWORD_NEW);
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
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadow(shadowProvisioningFuture);
		
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
	public void test240CloseDisableCaseAndReadAccountWill() throws Exception {
		final String TEST_NAME = "test240CloseDisableCaseAndReadAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		closeCase(willLastCaseOid);
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);

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
		
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.DISABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifySuccessOnly();
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
		}
		
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * lets ff 5min just for fun. Refresh, make sure everything should be the same (grace not expired yet)
	 */
	@Test
	public void test250RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test250RefreshAccountWillAfter5min";
		displayTestTitle(TEST_NAME);
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
		
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.DISABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
		}
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	@Test
	public void test252UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test252UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);
		
		backingStoreUpdateWill(ACCOUNT_WILL_FULLNAME_PIRATE, ActivationStatusType.DISABLED, ACCOUNT_WILL_PASSWORD_OLD);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 3);
		
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
		assertPendingOperationDeltas(shadowProvisioning, 3);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Password change/enable case is closed. The account should be disabled. But there is still the other
	 * delta pending.
	 */
	@Test
	public void test260ClosePasswordChangeCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test260ClosePasswordChangeCaseAndRefreshAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		closeCase(willSecondLastCaseOid);
		
		// Get repo shadow here. Make sure refresh works with this as well.
		PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		provisioningService.applyDefinition(shadowBefore, task, result);
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
		
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		}
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 7min. Refresh. Oldest delta should expire.
	 */
	@Test
	public void test270RefreshAccountWillAfter7min() throws Exception {
		final String TEST_NAME = "test130RefreshAccountWillAfter7min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		clock.overrideDuration("PT7M");

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
		
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		}
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	@Test
	public void test272UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test272UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);
		
		backingStoreUpdateWill(ACCOUNT_WILL_FULLNAME_PIRATE, ActivationStatusType.ENABLED, ACCOUNT_WILL_PASSWORD_NEW);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);
		
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
		assertPendingOperationDeltas(shadowProvisioning, 2);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 5min. Refresh. Another delta should expire.
	 */
	@Test
	public void test280RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test132RefreshAccountWillAfter10min";
		displayTestTitle(TEST_NAME);
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
		
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
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
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 5min. Refresh. All delta should expire.
	 */
	@Test
	public void test290RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test134RefreshAccountWillAfter5min";
		displayTestTitle(TEST_NAME);
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
				
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNoNotifcations();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);
		
		assertPendingOperationDeltas(shadowProvisioning, 0);
		
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	@Test
	public void test300DeleteAccountWill() throws Exception {
		final String TEST_NAME = "test300DeleteAccountWill";
		displayTestTitle(TEST_NAME);
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
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
			
		syncServiceMock.assertNoNotifyChange();
		syncServiceMock.assertNotifyInProgressOnly();

		PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		
		display("Provisioning shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

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
		assertShadowPassword(shadowProvisioningFuture);
		
		assertNotNull("No async reference in result", willLastCaseOid);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	@Test
	public void test302GetAccountWillFuture() throws Exception {
		final String TEST_NAME = "test302GetAccountWillFuture";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		display("Provisioning shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertShadowDead(shadowProvisioningFuture);
		assertShadowPassword(shadowProvisioningFuture);		
	}
	
	@Test
	public void test303GetAccountWillFutureNoFetch() throws Exception {
		final String TEST_NAME = "test303GetAccountWillFutureNoFetch";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		GetOperationOptions options = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
		options.setNoFetch(true);
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, 
				SelectorOptions.createCollection(options),
				task, result);
		
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		display("Provisioning shadow (future,noFetch)", shadowProvisioningFuture);
		assertNotNull("no OID", shadowProvisioningFuture.getOid());
		assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
		assertShadowDead(shadowProvisioningFuture);
		assertNoShadowPassword(shadowProvisioningFuture);		
	}
	
	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test310CloseCaseAndRefreshAccountWill() throws Exception {
		final String TEST_NAME = "test310CloseCaseAndRefreshAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();
		
		closeCase(willLastCaseOid);
		
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow before", shadowBefore);

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
		assertShadowDead(shadowProvisioning);
		assertShadowPassword(shadowProvisioning);

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
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test320RefreshAccountWillAfter5min() throws Exception {
		final String TEST_NAME = "test320RefreshAccountWillAfter5min";
		displayTestTitle(TEST_NAME);
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
		assertShadowDead(shadowProvisioning);
		assertShadowPassword(shadowProvisioning);
		
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
		assertShadowPassword(shadowProvisioningFuture);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	// TODO: create, close case, then update backing store.
	
	// TODO: let grace period expire without updating the backing store (semi-manual-only)

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
	
	protected <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrName, T... expectedValues) {
		assertAttribute(resource, shadow.asObjectable(), attrName, expectedValues);
	}
	
	protected <T> void assertNoAttribute(PrismObject<ShadowType> shadow, QName attrName) {
		assertNoAttribute(resource, shadow.asObjectable(), attrName);
	}
	
	protected void assertAttributeFromCache(PrismObject<ShadowType> shadow, QName attrQName,
			String... attrVals) {
		if (supportsBackingStore()) {
			assertNoAttribute(shadow, attrQName);
		} else {
			assertAttribute(shadow, attrQName, attrVals);
		}
	}
	
	protected void assertAttributeFromBackingStore(PrismObject<ShadowType> shadow, QName attrQName,
			String... attrVals) {
		if (supportsBackingStore()) {
			assertAttribute(shadow, attrQName, attrVals);
		} else {
			assertNoAttribute(shadow, attrQName);
		}
	}
	
	protected void assertShadowActivationAdministrativeStatusFromCache(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadow, null);
		} else {
			assertShadowActivationAdministrativeStatus(shadow, expectedStatus);
		}
	}
	
	protected void assertShadowActivationAdministrativeStatus(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
		assertActivationAdministrativeStatus(shadow, expectedStatus);
	}
	
	protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
		// pure manual resource should never "read" password
		assertNoShadowPassword(shadow);
	}
		
	private void assertManual(AbstractWriteCapabilityType cap) {
		assertEquals("Manual flag not set in capability "+cap, Boolean.TRUE, cap.isManual());
	}
	
}