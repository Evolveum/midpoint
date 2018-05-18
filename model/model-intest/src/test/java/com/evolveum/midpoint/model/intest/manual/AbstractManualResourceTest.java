/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AbstractWriteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractManualResourceTest extends AbstractConfiguredModelIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/manual/");

	public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = new QName(MidPointConstants.NS_RI, "AccountObjectClass");

	private static final Trace LOGGER = TraceManager.getTrace(AbstractManualResourceTest.class);

	protected static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/ManualConnector";
	protected static final QName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new QName(NS_MANUAL_CONF, "defaultAssignee");

	protected static final File USER_PHANTOM_FILE = new File(TEST_DIR, "user-phantom.xml");
	protected static final String USER_PHANTOM_OID = "5b12cc6e-575c-11e8-bc16-3744f9bfcac8";
	public static final String USER_PHANTOM_USERNAME = "phantom";
	public static final String USER_PHANTOM_FULL_NAME = "Thomas Phantom";
	public static final String USER_PHANTOM_FULL_NAME_WRONG = "Tom Funtom";
	public static final String ACCOUNT_PHANTOM_DESCRIPTION_MANUAL = "Phantom menace of the opera";
	public static final String ACCOUNT_PHANTOM_PASSWORD_MANUAL = "PhanthomaS";
	
	protected static final String USER_WILL_NAME = "will";
	protected static final String USER_WILL_GIVEN_NAME = "Will";
	protected static final String USER_WILL_FAMILY_NAME = "Turner";
	protected static final String USER_WILL_FULL_NAME = "Will Turner";
	protected static final String USER_WILL_FULL_NAME_PIRATE = "Pirate Will Turner";
	protected static final String ACCOUNT_WILL_DESCRIPTION_MANUAL = "manual";
	protected static final String USER_WILL_PASSWORD_OLD = "3lizab3th";
	protected static final String USER_WILL_PASSWORD_NEW = "ELIZAbeth";

	protected static final String ACCOUNT_JACK_DESCRIPTION_MANUAL = "Manuel";
	protected static final String USER_JACK_PASSWORD_OLD = "deadM3NtellN0tales";

	protected static final File TASK_SHADOW_REFRESH_FILE = new File(TEST_DIR, "task-shadow-refresh.xml");
	protected static final String TASK_SHADOW_REFRESH_OID = "eb8f5be6-2b51-11e7-848c-2fd84a283b03";

	protected static final String ATTR_USERNAME = "username";
	protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

	protected static final String ATTR_FULLNAME = "fullname";
	protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

	protected static final String ATTR_INTERESTS = "interests";
	protected static final QName ATTR_INTERESTS_QNAME = new QName(MidPointConstants.NS_RI, ATTR_INTERESTS);

	protected static final String ATTR_DESCRIPTION = "description";
	protected static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);

	protected static final String INTEREST_ONE = "one";

	protected BackingStore backingStore;
	
	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;

	protected String userWillOid;
	protected String accountWillOid;

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

	protected String accountJackOid;
	protected String accountDrakeOid;

	protected XMLGregorianCalendar accountJackReqestTimestampStart;
	protected XMLGregorianCalendar accountJackReqestTimestampEnd;

	protected XMLGregorianCalendar accountJackCompletionTimestampStart;
	protected XMLGregorianCalendar accountJackCompletionTimestampEnd;

	protected String jackLastCaseOid;

	private String lastResourceVersion;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		backingStore = createBackingStore();
		if (backingStore != null) {
			backingStore.initialize();
		}
		display("Backing store", backingStore);

		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

		importObjectFromFile(getResourceFile(), initResult);
		
		importObjectFromFile(getRoleOneFile(), initResult);
		importObjectFromFile(getRoleTwoFile(), initResult);

		addObject(USER_JACK_FILE);
		addObject(USER_DRAKE_FILE);

		PrismObject<UserType> userWill = createUserWill();
		addObject(userWill, initTask, initResult);
		display("User will", userWill);
		userWillOid = userWill.getOid();

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		setConflictResolutionAction(UserType.COMPLEX_TYPE, null, ConflictResolutionActionType.RECOMPUTE, initResult);

		// Turns on checks for connection in manual connector
		InternalsConfig.setSanityChecks(true);

	}

	protected BackingStore createBackingStore() {
		return null;
	}

	private PrismObject<UserType> createUserWill() throws SchemaException {
		PrismObject<UserType> user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate();
		user.asObjectable()
			.name(USER_WILL_NAME)
			.givenName(USER_WILL_GIVEN_NAME)
			.familyName(USER_WILL_FAMILY_NAME)
			.fullName(USER_WILL_FULL_NAME)
			.beginActivation().administrativeStatus(ActivationStatusType.ENABLED).<UserType>end()
			.beginCredentials().beginPassword().beginValue().setClearValue(USER_WILL_PASSWORD_OLD);
		return user;
	}

	protected abstract File getResourceFile();

	protected abstract String getResourceOid();
	
	protected abstract File getRoleOneFile();

	protected abstract String getRoleOneOid();

	protected abstract File getRoleTwoFile();

	protected abstract String getRoleTwoOid();

	protected boolean supportsBackingStore() {
		return backingStore != null;
	}

	protected boolean hasMultivalueInterests() {
		return true;
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);

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

		PrismObject<UserType> userWill = getUser(userWillOid);
		assertUser(userWill, userWillOid, USER_WILL_NAME, USER_WILL_FULL_NAME, USER_WILL_GIVEN_NAME, USER_WILL_FAMILY_NAME);
		assertAdministrativeStatus(userWill, ActivationStatusType.ENABLED);
		assertUserPassword(userWill, USER_WILL_PASSWORD_OLD);
	}

	@Test
	public void test012TestConnection() throws Exception {
		final String TEST_NAME = "test012TestConnection";
		testConnection(TEST_NAME, false);
	}
	
	public void testConnection(final String TEST_NAME, boolean initialized) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// Check that there is a schema, but no capabilities before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, getResourceOid(),
				null, result).asObjectable();

		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		if (!initialized) {
			assertResourceSchemaBeforeTest(resourceXsdSchemaElementBefore);
		}

		CapabilitiesType capabilitiesBefore = resourceBefore.getCapabilities();
		if (initialized || nativeCapabilitiesEntered()) {
			AssertJUnit.assertNotNull("Native capabilities missing before test connection. Bad test setup?", capabilitiesBefore);
			AssertJUnit.assertNotNull("Native capabilities missing before test connection. Bad test setup?", capabilitiesBefore.getNative());
		} else {
			if (capabilitiesBefore != null) {
				AssertJUnit.assertNull("Native capabilities present before test connection. Bad test setup?", capabilitiesBefore.getNative());
			}
		}

		// WHEN
		displayWhen(TEST_NAME);
		OperationResult testResult = modelService.testResource(getResourceOid(), task);

		// THEN
		displayThen(TEST_NAME);
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
		display("Resource XML after test connection", resourceXml);

		CachingMetadataType schemaCachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", schemaCachingMetadata);
		assertNotNull("No retrievalTimestamp", schemaCachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", schemaCachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchemaImpl.parse(xsdElement, resourceBefore.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		CapabilitiesType capabilitiesRepoAfter = resourceTypeRepoAfter.getCapabilities();
		AssertJUnit.assertNotNull("Capabilities missing after test connection.", capabilitiesRepoAfter);
		AssertJUnit.assertNotNull("Native capabilities missing after test connection.", capabilitiesRepoAfter.getNative());
		AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection.", capabilitiesRepoAfter.getCachingMetadata());
		
		ResourceType resourceModelAfter = modelService.getObject(ResourceType.class, getResourceOid(), null, null, result).asObjectable();
		
		rememberSteadyResources();
		
		CapabilitiesType capabilitiesModelAfter = resourceModelAfter.getCapabilities();
		AssertJUnit.assertNotNull("Capabilities missing after test connection (model)", capabilitiesModelAfter);
		AssertJUnit.assertNotNull("Native capabilities missing after test connection (model)", capabilitiesModelAfter.getNative());
		AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection (model)", capabilitiesModelAfter.getCachingMetadata());
	}

	protected boolean nativeCapabilitiesEntered() {
		return false;
	}
	
	protected abstract void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore);

	@Test
	public void test014Configuration() throws Exception {
		final String TEST_NAME = "test014Configuration";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName()
				+ "." + TEST_NAME);
		
		// WHEN
		resource = modelService.getObject(ResourceType.class, getResourceOid(), null, null, result);
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

		assertSteadyResources();
	}

	@Test
	public void test016ParsedSchema() throws Exception {
		final String TEST_NAME = "test016ParsedSchema";
		displayTestTitle(TEST_NAME);
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
		
		assertSteadyResources();
	}

	@Test
	public void test017Capabilities() throws Exception {
		final String TEST_NAME = "test017Capabilities";
		testCapabilities(TEST_NAME);
	}
	
	public void testCapabilities(final String TEST_NAME) throws Exception {
		displayTestTitle(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName()+"."+TEST_NAME);

		// WHEN
		ResourceType resource = modelService.getObject(ResourceType.class, getResourceOid(), null, null, result).asObjectable();

		// THEN
		display("Resource from model", resource);
		display("Resource from model (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject(), PrismContext.LANG_XML));

		assertSteadyResources();
		
		CapabilitiesType capabilities = resource.getCapabilities();
		assertNotNull("Missing capability caching metadata", capabilities.getCachingMetadata());
		
		CapabilityCollectionType nativeCapabilities = capabilities.getNative();
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
        
        assertSteadyResources();
	}
	
	/**
	 * MID-4472, MID-4174
	 */
	@Test
	public void test018ResourceCaching() throws Exception {
		final String TEST_NAME = "test018ResourceCaching";
		testResourceCaching(TEST_NAME);
	}
	
	public void testResourceCaching(final String TEST_NAME) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ResourceType> resourceReadonlyBefore = modelService.getObject(
				ResourceType.class, getResourceOid(), GetOperationOptions.createReadOnlyCollection(), task, result);
		
		assertSteadyResources();
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resourceReadOnlyAgain = modelService.getObject(
				ResourceType.class, getResourceOid(), GetOperationOptions.createReadOnlyCollection(), task, result);
		
		assertSteadyResources();
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resourceAgain = modelService.getObject(
				ResourceType.class, getResourceOid(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
//		assertTrue("Resource instance changed", resourceBefore == resourceReadOnlyAgain);

		assertSteadyResources();
	}
	
	@Test
	public void test020ReimportResource() throws Exception {
		final String TEST_NAME = "test020ReimportResource";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ImportOptionsType options = new ImportOptionsType();
		options.setOverwrite(true);
		
		// WHEN
		displayWhen(TEST_NAME);
		modelService.importObjectsFromFile(getResourceFile(), options, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
	}
	
	@Test
	public void test022TestConnection() throws Exception {
		final String TEST_NAME = "test022TestConnection";
		testConnection(TEST_NAME, false);
	}
	
	@Test
	public void test027Capabilities() throws Exception {
		final String TEST_NAME = "test027Capabilities";
		testCapabilities(TEST_NAME);
	}

	/**
	 * MID-4472, MID-4174
	 */
	@Test
	public void test028ResourceCaching() throws Exception {
		final String TEST_NAME = "test028ResourceCaching";
		testResourceCaching(TEST_NAME);
	}

	@Test
	public void test030ReimportResourceAgain() throws Exception {
		final String TEST_NAME = "test030ReimportResourceAgain";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ImportOptionsType options = new ImportOptionsType();
		options.setOverwrite(true);
		
		// WHEN
		displayWhen(TEST_NAME);
		modelService.importObjectsFromFile(getResourceFile(), options, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
	}
	
	// This time simply try to use the resource without any attempt to test connection
	@Test
	public void test032UseResource() throws Exception {
		final String TEST_NAME = "test032UseResource";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(getResourceOid(), ShadowKindType.ACCOUNT, null, prismContext);
		
		// WHEN
		displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> accounts = modelService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		display("Found accounts", accounts);
		assertEquals("unexpected accounts: "+accounts, 0, accounts.size());
		
		rememberSteadyResources();
		
		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, getResourceOid(), null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
		
		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		String resourceXml = prismContext.serializeObjectToString(resourceRepoAfter, PrismContext.LANG_XML);
		display("Resource XML after test connection", resourceXml);
		
		CachingMetadataType schemaCachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No schema caching metadata", schemaCachingMetadata);
		assertNotNull("No schema caching metadata retrievalTimestamp", schemaCachingMetadata.getRetrievalTimestamp());
		assertNotNull("No schema caching metadata serialNumber", schemaCachingMetadata.getSerialNumber());
		
		ResourceType resourceModelAfter = modelService.getObject(ResourceType.class, getResourceOid(), null, null, result).asObjectable();
		
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchemaImpl.parse(xsdElement, resourceModelAfter.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);
		CapabilitiesType capabilitiesRepoAfter = resourceTypeRepoAfter.getCapabilities();
		AssertJUnit.assertNotNull("Capabilities missing after test connection.", capabilitiesRepoAfter);
		AssertJUnit.assertNotNull("Native capabilities missing after test connection.", capabilitiesRepoAfter.getNative());
		AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection.", capabilitiesRepoAfter.getCachingMetadata());
				
		CapabilitiesType capabilitiesModelAfter = resourceModelAfter.getCapabilities();
		AssertJUnit.assertNotNull("Capabilities missing after test connection (model)", capabilitiesModelAfter);
		AssertJUnit.assertNotNull("Native capabilities missing after test connection (model)", capabilitiesModelAfter.getNative());
		AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection (model)", capabilitiesModelAfter.getCachingMetadata());
	}
	
	@Test
	public void test037Capabilities() throws Exception {
		final String TEST_NAME = "test037Capabilities";
		testCapabilities(TEST_NAME);
	}
	
	/**
	 * MID-4472, MID-4174
	 */
	@Test
	public void test038ResourceCaching() throws Exception {
		final String TEST_NAME = "test038ResourceCaching";
		testResourceCaching(TEST_NAME);
	}
	
	/**
	 * Make sure the resource is fully initialized, so steady resource asserts 
	 * in subsequent tests will work as expected.
	 */
	@Test
	public void test099TestConnection() throws Exception {
		final String TEST_NAME = "test099TestConnection";
		testConnection(TEST_NAME, true);
	}
	
	@Test
	public void test100AssignWillRoleOne() throws Exception {
		// The count will be checked only after propagation was run, so we can address both direct and grouping cases
		rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);
		assignWillRoleOne("test100AssignWillRoleOne", USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);
	}

	@Test
	public void test101GetAccountWillFuture() throws Exception {
		final String TEST_NAME = "test101GetAccountWillFuture";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, options, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertAttribute(shadowModel, ATTR_INTERESTS_QNAME, INTEREST_ONE);
		assertNoAttribute(shadowModel, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, true);
		// TODO
//		assertShadowPassword(shadowProvisioning);
		
		assertSteadyResources();
	}

	@Test
	public void test102RecomputeWill() throws Exception {
		final String TEST_NAME = "test102RecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);
		
		assertSteadyResources();
	}
	
	/**
	 * ff 2min, run propagation task. Grouping resources should execute the operations now.
	 */
	@Test
	public void test103RunPropagation() throws Exception {
		final String TEST_NAME = "test103RunPropagation";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT2M");

		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		// The count is checked only after propagation was run, so we can address both direct and grouping cases
		assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 1);
		
		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTING);
		
		assertSteadyResources();
	}
	
	@Test
	public void test104RecomputeWill() throws Exception {
		final String TEST_NAME = "test104RecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);
		
		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTING);
		
		assertSteadyResources();
	}

	/**
	 * Operation is executing. Propagation has nothing to do.
	 * Make sure nothing is done even if propagation task is run.
	 */
	@Test
	public void test105RunPropagationAgain() throws Exception {
		final String TEST_NAME = "test105RunPropagationAgain";
		
		displayTestTitle(TEST_NAME);
		// GIVEN
		rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);
		assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);
		
		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTING);
		
		assertSteadyResources();
	}
	
	@Test
	public void test106AddToBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test106AddToBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreProvisionWill(INTEREST_ONE);
		displayBackingStore();

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, supportsBackingStore());
		assertShadowPassword(shadowModel);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, supportsBackingStore());
		
		assertSteadyResources();
	}

	@Test
	public void test108GetAccountWillFuture() throws Exception {
		final String TEST_NAME = "test108GetAccountWillFuture";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, options, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, true);
		// TODO
//		assertShadowPassword(shadowProvisioning);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, supportsBackingStore());
		
		assertSteadyResources();
	}

	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test110CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test110CloseCaseAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		// We need reconcile and not recompute here. We need to fetch the updated case status.
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertWillAfterCreateCaseClosed(TEST_NAME, true);	
		
		assertSteadyResources();
	}
	
	/**
	 * Case is closed. The operation is complete. Nothing to do.
	 * Make sure nothing is done even if propagation task is run.
	 */
	@Test
	public void test114RunPropagation() throws Exception {
		final String TEST_NAME = "test114RunPropagation";
		
		displayTestTitle(TEST_NAME);
		// GIVEN
		rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);
		assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);
		
		assertWillAfterCreateCaseClosed(TEST_NAME, true);
		
		assertSteadyResources();
	}

	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test120RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test120RecomputeWillAfter5min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		
		assertSteadyResources();
	}

	/**
	 * ff 20min, grace should expire
	 */
	@Test
	public void test130RecomputeWillAfter25min() throws Exception {
		final String TEST_NAME = "test130RecomputeWillAfter25min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT20M");

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertNoPendingOperation(shadowRepo);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);
		assertNoPendingOperation(shadowModel);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		
		assertSteadyResources();
	}

	@Test
	public void test200ModifyUserWillFullname() throws Exception {
		final String TEST_NAME = "test200ModifyUserWillFullname";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				accountWillOid, new ItemPath(ShadowType.F_ATTRIBUTES, ATTR_FULLNAME_QNAME), prismContext,
				USER_WILL_FULL_NAME_PIRATE);
		display("ObjectDelta", delta);

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(userWillOid, UserType.F_FULL_NAME, task, result, createPolyString(USER_WILL_FULL_NAME_PIRATE));

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
		
		assertAccountWillAfterFillNameModification(TEST_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);
		
		assertSteadyResources();
	}
	
	@Test
	public void test202RecomputeWill() throws Exception {
		final String TEST_NAME = "test202RecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		assertAccountWillAfterFillNameModification(TEST_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);
		
		assertSteadyResources();
	}
	
	@Test
	public void test203RunPropagation() throws Exception {
		final String TEST_NAME = "test203RunPropagation";
		displayTestTitle(TEST_NAME);
		// GIVEN

		clockForward("PT2M");
		
		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);

		assertAccountWillAfterFillNameModification(TEST_NAME, PendingOperationExecutionStatusType.EXECUTING);
		
		assertSteadyResources();
	}
	
	@Test
	public void test204RecomputeWill() throws Exception {
		final String TEST_NAME = "test204RecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		assertAccountWillAfterFillNameModification(TEST_NAME, PendingOperationExecutionStatusType.EXECUTING);
		
		assertSteadyResources();
	}
	
	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test206CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test206CloseCaseAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowProvisioning = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		if (supportsBackingStore()) {
			assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		} else {
			assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		}
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		
		assertSteadyResources();
	}
	

	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test210RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test210RecomputeWillAfter5min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		if (supportsBackingStore()) {
			assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		} else {
			assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		}
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModelFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		
		assertSteadyResources();
	}

	@Test
	public void test212UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test212UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.ENABLED, USER_WILL_PASSWORD_OLD);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModelFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		
		assertSteadyResources();
	}
	
	/**
	 * Create phantom account in the backing store. MidPoint does not know anything about it.
	 * At the same time, there is phantom user that has the account assigned. But it is not yet
	 * provisioned. MidPoint should find existing account and it it should figure out that
	 * there is nothing to do. Just to link the account.
	 * related to MID-4614
	 */
	@Test
	public void test400PhantomAccount() throws Exception {
		final String TEST_NAME = "test400PhantomAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		setupPhantom(TEST_NAME);

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_PHANTOM_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		// This should theoretically always return IN_PROGRESS, as there is
		// reconciliation operation going on. But due to various "peculiarities"
		// of a consistency mechanism this sometimes returns in progress and
		// sometimes it is just success.
		OperationResultStatus status = result.getStatus();
		if (status != OperationResultStatus.IN_PROGRESS && status != OperationResultStatus.SUCCESS) {
			fail("Unexpected result status in "+result);
		}
		
		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();
		
		// THEN
		displayThen(TEST_NAME);
		
		PrismObject<UserType> userAfter = getUser(USER_PHANTOM_OID);
		display("User after", userAfter);
		String shadowOid = getSingleLinkOid(userAfter);
		PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
		display("Shadow after", shadowModel);

		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME);
		if (supportsBackingStore()) {
			assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHANTOM_FULL_NAME_WRONG);
		} else {
			assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHANTOM_FULL_NAME);
		}
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_PHANTOM_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		if (isDirect()) {
			assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
		} else {
			assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTION_PENDING, null);
		}

		assertSteadyResources();
	}
	
	protected void setupPhantom(final String TEST_NAME) throws Exception {
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		addObject(USER_PHANTOM_FILE);
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_PHANTOM_OID, getResourceOid(), null, true);
		repositoryService.modifyObject(UserType.class, USER_PHANTOM_OID, userDelta.getModifications(), result);
		PrismObject<UserType> userBefore = getUser(USER_PHANTOM_OID);
		display("User before", userBefore);

		backingStoreAddPhantom();
	}
	
	protected boolean are9xxTestsEnabled() {
		// Disabled by default. These are intense parallel tests and they will fail for resources
		// that do not have extra consistency checks and do not use proposed shadows.
		return false;
	}
	

	protected int getConcurrentTestNumberOfThreads() {
		return 4;
	}

	protected int getConcurrentTestRandomStartDelayRangeAssign() { 
		return 1000;
	}

	protected int getConcurrentTestRandomStartDelayRangeUnassign() {
		return 5;
	}
	
	/**
	 * Set up roles used in parallel tests.
	 */
	@Test
	public void test900SetUpRoles() throws Exception {
		final String TEST_NAME = "test900SetUpRoles";
		displayTestTitle(TEST_NAME);
		if (!are9xxTestsEnabled()) {
			displaySkip(TEST_NAME);
			return;
		}
		
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		SystemConfigurationType systemConfiguration = getSystemConfiguration();
		display("System config", systemConfiguration);
		ConflictResolutionActionType conflictResolutionAction = systemConfiguration.getDefaultObjectPolicyConfiguration().get(0).getConflictResolution().getAction();
		if (!ConflictResolutionActionType.RECOMPUTE.equals(conflictResolutionAction) && !ConflictResolutionActionType.RECONCILE.equals(conflictResolutionAction)) {
			fail("Wrong conflict resolution action: " + conflictResolutionAction);
		}
		

		for (int i = 0; i < getConcurrentTestNumberOfThreads(); i++) {
			PrismObject<RoleType> role = parseObject(getRoleOneFile());
			role.setOid(getRoleOid(i));
			role.asObjectable().setName(createPolyStringType(getRoleName(i)));
			List<ResourceAttributeDefinitionType> outboundAttributes = role.asObjectable().getInducement().get(0).getConstruction().getAttribute();
			if (hasMultivalueInterests()) {
				ExpressionType outboundExpression = outboundAttributes.get(0).getOutbound().getExpression();
				JAXBElement jaxbElement = outboundExpression.getExpressionEvaluator().get(0);
				jaxbElement.setValue(getRoleInterest(i));
			} else {
				outboundAttributes.remove(0);
			}
			addObject(role);
		}
	}

	protected String getRoleOid(int i) {
		return String.format("f363260a-8d7a-11e7-bd67-%012d", i);
	}

	protected String getRoleName(int i) {
		return String.format("role-%012d", i);
	}

	protected String getRoleInterest(int i) {
		return String.format("i%012d", i);
	}
	

	// MID-4047, MID-4112
	@Test
	public void test910ConcurrentRolesAssign() throws Exception {
		final String TEST_NAME = "test910ConcurrentRolesAssign";
		displayTestTitle(TEST_NAME);
		if (!are9xxTestsEnabled()) {
			displaySkip(TEST_NAME);
			return;
		}
		
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		int numberOfCasesBefore = getObjectCount(CaseType.class);
		PrismObject<UserType> userBefore = getUser(USER_DRAKE_OID);
		display("user before", userBefore);
		assertLinks(userBefore, 0);

		final long TIMEOUT = 60000L;

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					login(userAdministrator);
					Task localTask = createTask(TEST_NAME + ".local");

					assignRole(USER_DRAKE_OID, getRoleOid(i), localTask, localTask.getResult());

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestRandomStartDelayRangeAssign());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, TIMEOUT);

		PrismObject<UserType> userAfter = getUser(USER_DRAKE_OID);
		display("user after", userAfter);
		assertAssignments(userAfter, getConcurrentTestNumberOfThreads());
		assertEquals("Wrong # of links", 1, userAfter.asObjectable().getLinkRef().size());
		accountDrakeOid = userAfter.asObjectable().getLinkRef().get(0).getOid();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountDrakeOid, null, result);
		display("Repo shadow", shadowRepo);
		assertShadowNotDead(shadowRepo);
		
		assertTest910ShadowRepo(shadowRepo, task, result);

		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountDrakeOid, options, task, result);
		display("Shadow after (model, future)", shadowModel);

//		assertObjects(CaseType.class, numberOfCasesBefore + getConcurrentTestNumberOfThreads());
	}
	
	protected void assertTest910ShadowRepo(PrismObject<ShadowType> shadowRepo, Task task, OperationResult result) throws Exception {
		assertShadowNotDead(shadowRepo);
		ObjectDeltaType addPendingDelta = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (delta.getChangeType() == ChangeTypeType.ADD) {
				ObjectType objectToAdd = delta.getObjectToAdd();
				display("Pending ADD object", objectToAdd.asPrismObject());
				if (addPendingDelta != null) {
					fail("More than one add pending delta found:\n"+addPendingDelta+"\n"+delta);
				}
				addPendingDelta = delta;
			}
			if (delta.getChangeType() == ChangeTypeType.DELETE) {
				fail("Unexpected delete pending delta found:\n"+delta);
			}
			if (isActivationStatusModifyDelta(delta, ActivationStatusType.ENABLED)) {
				fail("Unexpected enable pending delta found:\n"+delta);
			}
		}
		assertNotNull("No add pending delta", addPendingDelta);
	}
	
	// MID-4112
	@Test
	public void test919ConcurrentRoleUnassign() throws Exception {
		final String TEST_NAME = "test919ConcurrentRoleUnassign";
		displayTestTitle(TEST_NAME);
		if (!are9xxTestsEnabled()) {
			displaySkip(TEST_NAME);
			return;
		}
		
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		int numberOfCasesBefore = getObjectCount(CaseType.class);
		PrismObject<UserType> userBefore = getUser(USER_DRAKE_OID);
		display("user before", userBefore);
		assertAssignments(userBefore, getConcurrentTestNumberOfThreads());

		final long TIMEOUT = 60000L;

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					display("Thread "+Thread.currentThread().getName()+" START");
					login(userAdministrator);
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();

					unassignRole(USER_DRAKE_OID, getRoleOid(i), localTask, localResult);

					localResult.computeStatus();

					display("Thread "+Thread.currentThread().getName()+" DONE, result", localResult);

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestRandomStartDelayRangeUnassign());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, TIMEOUT);

		PrismObject<UserType> userAfter = getUser(USER_DRAKE_OID);
		display("user after", userAfter);
		assertAssignments(userAfter, 0);
		assertEquals("Wrong # of links", 1, userAfter.asObjectable().getLinkRef().size());

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountDrakeOid, null, result);
		display("Repo shadow", shadowRepo);
		
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountDrakeOid, null, task, result);
		display("Shadow after (model)", shadowModel);
		
		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class, accountDrakeOid, options, task, result);
		display("Shadow after (model, future)", shadowModelFuture);

		assertTest919ShadowRepo(shadowRepo, task, result);

		assertTest919ShadowFuture(shadowModelFuture, task, result);
		
//		assertObjects(CaseType.class, numberOfCasesBefore + getConcurrentTestNumberOfThreads() + 1);
	}

	protected void assertTest919ShadowRepo(PrismObject<ShadowType> shadowRepo, Task task, OperationResult result) throws Exception {
		ObjectDeltaType deletePendingDelta = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (delta.getChangeType() == ChangeTypeType.ADD) {
				ObjectType objectToAdd = delta.getObjectToAdd();
				display("Pending ADD object", objectToAdd.asPrismObject());
			}
			if (delta.getChangeType() == ChangeTypeType.DELETE) {
				if (deletePendingDelta != null) {
					fail("More than one delete pending delta found:\n"+deletePendingDelta+"\n"+delta);
				}
				deletePendingDelta = delta;
			}
		}
		assertNotNull("No delete pending delta", deletePendingDelta);
	}
	
	protected boolean isActivationStatusModifyDelta(ObjectDeltaType delta, ActivationStatusType expected) throws SchemaException {
		if (delta.getChangeType() != ChangeTypeType.MODIFY) {
			return false;
		}
		for (ItemDeltaType itemDelta: delta.getItemDelta()) {
			ItemPath deltaPath = itemDelta.getPath().getItemPath();
			if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(deltaPath)) {
				List<RawType> value = itemDelta.getValue();
				PrismProperty<ActivationStatusType> parsedItem = (PrismProperty<ActivationStatusType>)(Item)
						value.get(0).getParsedItem(getUserDefinition().findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS));
				ActivationStatusType status = parsedItem.getRealValue();
				display("Delta status " + status, itemDelta);
				if (expected.equals(status)) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected void assertTest919ShadowFuture(PrismObject<ShadowType> shadowModelFuture, Task task,
			OperationResult result) {
		assertShadowDead(shadowModelFuture);
	}
	
	protected void assertAccountWillAfterFillNameModification(final String TEST_NAME, PendingOperationExecutionStatusType executionStage) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd, executionStage);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		PendingOperationType pendingOperationType = assertSinglePendingOperation(shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd, executionStage);

		PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioningFuture);
		
		assertWillCase(SchemaConstants.CASE_STATE_OPEN, pendingOperationType, executionStage);
	}
	
	protected int getNumberOfAccountAttributeDefinitions() {
		return 4;
	}
	
	protected void backingStoreProvisionWill(String interest) throws IOException {
		if (backingStore != null) {
			backingStore.provisionWill(interest);
		}
	}

	protected void backingStoreUpdateWill(String newFullName, String interest, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
		if (backingStore != null) {
			backingStore.updateWill(newFullName, interest, newAdministrativeStatus, password);
		}
	}

	protected void backingStoreDeprovisionWill() throws IOException {
		if (backingStore != null) {
			backingStore.deprovisionWill();
		}
	}

	protected void displayBackingStore() throws IOException {
		if (backingStore != null) {
			backingStore.displayContent();
		}
	}
	
	protected void backingStoreAddJack() throws IOException {
		if (backingStore != null) {
			backingStore.addJack();
		}
	}

	protected void backingStoreDeleteJack() throws IOException {
		if (backingStore != null) {
			backingStore.deleteJack();
		}
	}
	
	protected void backingStoreAddPhantom() throws IOException {
		if (backingStore != null) {
			backingStore.addPhantom();
		}
	}

	protected void assignWillRoleOne(final String TEST_NAME, String expectedFullName, PendingOperationExecutionStatusType executionStage) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(userWillOid, getRoleOneOid(), task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		accountWillOid = getSingleLinkOid(userAfter);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertAccountWillAfterAssign(TEST_NAME, expectedFullName, executionStage);
		
		assertSteadyResources();
	}

	protected void assertAccountWillAfterAssign(final String TEST_NAME, String expectedFullName, PendingOperationExecutionStatusType executionStage) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd, executionStage);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, expectedFullName);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertShadowExists(shadowRepo, false);
		assertNoShadowPassword(shadowRepo);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, expectedFullName);
		assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, false);
		assertNoShadowPassword(shadowModel);
		PendingOperationType pendingOperationType = assertSinglePendingOperation(shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd, executionStage);

		assertWillCase(SchemaConstants.CASE_STATE_OPEN, pendingOperationType, executionStage);
	}
	
	protected void assertWillCase(String expectedCaseState, PendingOperationType pendingOperationType, PendingOperationExecutionStatusType executionStage) throws ObjectNotFoundException, SchemaException {
		String pendingOperationRef = pendingOperationType.getAsynchronousOperationReference();
		if (isDirect()) {
			// Case number should be in willLastCaseOid. It will get there from operation result.
			assertNotNull("No async reference in pending operation", willLastCaseOid);
			assertCase(willLastCaseOid, expectedCaseState);
			assertEquals("Wrong case ID in pending operation", willLastCaseOid, pendingOperationRef);
		} else {
			if (caseShouldExist(executionStage)) {
				assertNotNull("No async reference in pending operation", pendingOperationRef);
				assertCase(pendingOperationRef, expectedCaseState);
			} else {
				assertNull("Unexpected async reference in result", pendingOperationRef);
			}
			willLastCaseOid = pendingOperationRef;
		}
	}
	
	protected boolean caseShouldExist(PendingOperationExecutionStatusType executionStage) {
		return getExpectedExecutionStatus(executionStage) == PendingOperationExecutionStatusType.EXECUTING;
	}
	
	protected abstract boolean isDirect();

	protected abstract OperationResultStatusType getExpectedResultStatus(PendingOperationExecutionStatusType executionStage);
	
	protected abstract PendingOperationExecutionStatusType getExpectedExecutionStatus(PendingOperationExecutionStatusType executionStage);
	

	protected void assertAccountJackAfterAssign(final String TEST_NAME) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountJackReqestTimestampStart, accountJackReqestTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_JACK_USERNAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_JACK_FULL_NAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertShadowExists(shadowRepo, false);
		assertNoShadowPassword(shadowRepo);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountJackOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_JACK_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_JACK_USERNAME);
		assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_JACK_FULL_NAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, false);
		assertNoShadowPassword(shadowModel);

		assertSinglePendingOperation(shadowModel, accountJackReqestTimestampStart, accountJackReqestTimestampEnd);

		assertNotNull("No async reference in result", jackLastCaseOid);

		assertCase(jackLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	protected void assertWillAfterCreateCaseClosed(final String TEST_NAME, boolean backingStoreUpdated) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		if (!supportsBackingStore() || backingStoreUpdated) {
			assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
			assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
			assertShadowPassword(shadowModel);
		}

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	protected void assertWillUnassignedFuture(PrismObject<ShadowType> shadowModelFuture, boolean assertPassword) {
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertUnassignedFuture(shadowModelFuture, assertPassword);
	}

	protected void assertUnassignedFuture(PrismObject<ShadowType> shadowModelFuture, boolean assertPassword) {
		assertShadowDead(shadowModelFuture);
		if (assertPassword) {
			assertShadowPassword(shadowModelFuture);
		}
	}

	protected void assertPendingOperationDeltas(PrismObject<ShadowType> shadow, int expectedNumber) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		assertEquals("Wrong number of pending operations in "+shadow, expectedNumber, pendingOperations.size());
	}

	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd, PendingOperationExecutionStatusType executionStage) {
		assertPendingOperationDeltas(shadow, 1);
		return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0), 
				requestStart, requestEnd, 
				getExpectedExecutionStatus(executionStage), getExpectedResultStatus(executionStage),
				null, null);
	}
	
	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertSinglePendingOperation(shadow, requestStart, requestEnd,
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResultStatus) {
		assertPendingOperationDeltas(shadow, 1);
		return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0), 
				null, null, expectedExecutionStatus, expectedResultStatus, null, null);
	}

	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			OperationResultStatusType expectedStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		assertPendingOperationDeltas(shadow, 1);
		return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0),
				requestStart, requestEnd, expectedStatus, completionStart, completionEnd);
	}

	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertPendingOperation(shadow, pendingOperation, requestStart, requestEnd,
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResultStatus) {
		return assertPendingOperation(shadow, pendingOperation, null, null,
				expectedExecutionStatus, expectedResultStatus, null, null);
	}
	
	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation) {
		return assertPendingOperation(shadow, pendingOperation, null, null,
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			OperationResultStatusType expectedResultStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		PendingOperationExecutionStatusType expectedExecutionStatus;
		if (expectedResultStatus == OperationResultStatusType.IN_PROGRESS) {
			expectedExecutionStatus = PendingOperationExecutionStatusType.EXECUTING;
		} else {
			expectedExecutionStatus = PendingOperationExecutionStatusType.COMPLETED;
		}
		return assertPendingOperation(shadow, pendingOperation, requestStart, requestEnd, expectedExecutionStatus, expectedResultStatus, completionStart, completionEnd);
	}

	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			PendingOperationExecutionStatusType expectedExecutionStatus,
			OperationResultStatusType expectedResultStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		assertNotNull("No operation ", pendingOperation);

		ObjectDeltaType deltaType = pendingOperation.getDelta();
		assertNotNull("No delta in pending operation in "+shadow, deltaType);
		// TODO: check content of pending operations in the shadow

		TestUtil.assertBetween("No request timestamp in pending operation in "+shadow, requestStart, requestEnd, pendingOperation.getRequestTimestamp());

		PendingOperationExecutionStatusType executiontStatus = pendingOperation.getExecutionStatus();
		assertEquals("Wrong execution status in pending operation in "+shadow, expectedExecutionStatus, executiontStatus);
		
		OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
		assertEquals("Wrong result status in pending operation in "+shadow, expectedResultStatus, resultStatus);

		// TODO: assert other timestamps
		
		if (expectedExecutionStatus == PendingOperationExecutionStatusType.COMPLETED) {
			TestUtil.assertBetween("No completion timestamp in pending operation in "+shadow, completionStart, completionEnd, pendingOperation.getCompletionTimestamp());
		}

		return pendingOperation;
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStaus) {
		return findPendingOperation(shadow, expectedExecutionStaus, null, null, null);
	}
	
	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult) {
		return findPendingOperation(shadow, null, expectedResult, null, null);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult, ItemPath itemPath) {
		return findPendingOperation(shadow, null, expectedResult, null, itemPath);
	}
	
	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStaus, ItemPath itemPath) {
		return findPendingOperation(shadow, expectedExecutionStaus, null, null, itemPath);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult, ChangeTypeType expectedChangeType) {
		return findPendingOperation(shadow, null, expectedResult, expectedChangeType, null);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResult,
			ChangeTypeType expectedChangeType, ItemPath itemPath) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		for (PendingOperationType pendingOperation: pendingOperations) {
			if (expectedExecutionStatus != null && !expectedExecutionStatus.equals(pendingOperation.getExecutionStatus())) {
				continue;
			}
			if (expectedResult != null && !expectedResult.equals(pendingOperation.getResultStatus())) {
				continue;
			}
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (expectedChangeType != null) {
				if (!expectedChangeType.equals(delta.getChangeType())) {
					continue;
				}
			}
			if (itemPath == null) {
				return pendingOperation;
			}
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
	
	protected void cleanupUser(final String TEST_NAME, String userOid, String username, String accountOid) throws Exception {
		// nothing to do here
	}

	protected void assertCase(String oid, String expectedState, PendingOperationExecutionStatusType executionStage) throws ObjectNotFoundException, SchemaException {
		if (caseShouldExist(executionStage)) {
			assertCase(oid, expectedState);
		}
	}

	protected void runPropagation() throws Exception {
		runPropagation(null);
	}
	
	protected void runPropagation(OperationResultStatusType expectedStatus) throws Exception {
		// nothing by default
	}
	
	@Override
	protected void rememberSteadyResources() {
		super.rememberSteadyResources();
		OperationResult result = new OperationResult("rememberSteadyResources");
		try {
			lastResourceVersion = repositoryService.getVersion(ResourceType.class, getResourceOid(), result);
		} catch (ObjectNotFoundException | SchemaException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	@Override
	protected void assertSteadyResources() {
		super.assertSteadyResources();
		String currentResourceVersion;
		try {
			OperationResult result = new OperationResult("assertSteadyResources");
			currentResourceVersion = repositoryService.getVersion(ResourceType.class, getResourceOid(), result);
		} catch (ObjectNotFoundException | SchemaException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		assertEquals("Resource version mismatch", lastResourceVersion, currentResourceVersion);
	}
	
	protected void assertHasModification(ObjectDeltaType deltaType, ItemPath itemPath) {
		for (ItemDeltaType itemDelta: deltaType.getItemDelta()) {
			if (itemPath.equivalent(itemDelta.getPath().getItemPath())) {
				return;
			}
		}
		fail("No modification for "+itemPath+" in delta");
	}

}