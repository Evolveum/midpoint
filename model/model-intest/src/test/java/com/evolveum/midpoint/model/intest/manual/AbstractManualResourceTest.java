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
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
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
import com.evolveum.midpoint.prism.util.PrismTestUtil;
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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
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
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractManualResourceTest extends AbstractConfiguredModelIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/manual/");

	protected static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
	protected static final String RESOURCE_MANUAL_OID = "0ef80ab8-2906-11e7-b81a-3f343e28c264";

	protected static final File RESOURCE_SEMI_MANUAL_FILE = new File(TEST_DIR, "resource-semi-manual.xml");
	protected static final String RESOURCE_SEMI_MANUAL_OID = "aea5a57c-2904-11e7-8020-7b121a9e3595";

	protected static final File RESOURCE_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "resource-semi-manual-disable.xml");
	protected static final String RESOURCE_SEMI_MANUAL_DISABLE_OID = "5e497cb0-5cdb-11e7-9cfe-4bfe0342d181";

	protected static final File RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "resource-semi-manual-slow-proposed.xml");
	protected static final String RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_OID = "512d749a-75ff-11e7-8176-8be7fb6f4e45";
	
	protected static final File RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "resource-semi-manual-disable-slow-proposed.xml");
	protected static final String RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "8ed29734-a1ed-11e7-b7f9-7bce8b17fd64";

	protected static final File ROLE_ONE_MANUAL_FILE = new File(TEST_DIR, "role-one-manual.xml");
	protected static final String ROLE_ONE_MANUAL_OID = "9149b3ca-5da1-11e7-8e84-130a91fb5876";

	protected static final File ROLE_ONE_SEMI_MANUAL_FILE = new File(TEST_DIR, "role-one-semi-manual.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_OID = "29eab2c8-5da2-11e7-85d3-c78728e05ca3";

	protected static final File ROLE_ONE_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "role-one-semi-manual-disable.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_DISABLE_OID = "3b8cb17a-5da2-11e7-b260-a760972b54fb";

	protected static final File ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-one-semi-manual-slow-proposed.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_OID = "ca7fefc6-75ff-11e7-9833-572f6bf86a81";
	
	protected static final File ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-one-semi-manual-disable-slow-proposed.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "38c9fc7a-a200-11e7-8157-2f9beeb541bc";

	protected static final File ROLE_TWO_MANUAL_FILE = new File(TEST_DIR, "role-two-manual.xml");
	protected static final String ROLE_TWO_MANUAL_OID = "414e3766-775e-11e7-b8cb-c7ca37c1dc9e";

	protected static final File ROLE_TWO_SEMI_MANUAL_FILE = new File(TEST_DIR, "role-two-semi-manual.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_OID = "b95f7252-7776-11e7-bd96-cf05f4c21966";

	protected static final File ROLE_TWO_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "role-two-semi-manual-disable.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_DISABLE_OID = "d1eaa4f4-7776-11e7-bb53-eb1218c49dd9";

	protected static final File ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-two-semi-manual-slow-proposed.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_OID = "eaf3569e-7776-11e7-93f3-3f1b853d6525";

	protected static final File ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-two-semi-manual-disable-slow-proposed.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "5ecd6fa6-a200-11e7-b0cb-af5e1792d327";

	public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = new QName(MidPointConstants.NS_RI, "AccountObjectClass");

	private static final Trace LOGGER = TraceManager.getTrace(AbstractManualResourceTest.class);

	protected static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/ManualConnector";
	protected static final QName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new QName(NS_MANUAL_CONF, "defaultAssignee");

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

	private static final File TASK_SHADOW_REFRESH_FILE = new File(TEST_DIR, "task-shadow-refresh.xml");
	private static final String TASK_SHADOW_REFRESH_OID = "eb8f5be6-2b51-11e7-848c-2fd84a283b03";

	protected static final String ATTR_USERNAME = "username";
	protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

	protected static final String ATTR_FULLNAME = "fullname";
	protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

	protected static final String ATTR_INTERESTS = "interests";
	protected static final QName ATTR_INTERESTS_QNAME = new QName(MidPointConstants.NS_RI, ATTR_INTERESTS);

	protected static final String ATTR_DESCRIPTION = "description";
	protected static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);

	protected static final String INTEREST_ONE = "one";

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

	protected XMLGregorianCalendar accountJackReqestTimestampStart;
	protected XMLGregorianCalendar accountJackReqestTimestampEnd;

	protected XMLGregorianCalendar accountJackCompletionTimestampStart;
	protected XMLGregorianCalendar accountJackCompletionTimestampEnd;

	protected String jackLastCaseOid;

	private XMLGregorianCalendar roleTwoValidFromTimestamp;

	protected String accountBarbossaOid;
	protected String accountDrakeOid;


	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

		importObjectFromFile(getResourceFile(), initResult);

		importObjectFromFile(getRoleOneFile(), initResult);
		importObjectFromFile(getRoleTwoFile(), initResult);

		addObject(USER_JACK_FILE);
		addObject(USER_BARBOSSA_FILE);
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
		return false;
	}

	protected boolean hasMultivalueInterests() {
		return true;
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
	
	protected boolean are9xxTestsEnabled() {
		// Disabled by default. These are intense parallel tests and they will fail for resources
		// that do not have extra consistency checks and do not use proposed shadows.
		return false;
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(TEST_NAME);

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
		OperationResult testResult = modelService.testResource(getResourceOid(), task);

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
		OperationResult result = new OperationResult(AbstractManualResourceTest.class.getName()+"."+TEST_NAME);

		// WHEN
		ResourceType resource = modelService.getObject(ResourceType.class, getResourceOid(), null, null, result).asObjectable();

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
	public void test100AssignWillRoleOne() throws Exception {
		assignWillRoleOne("test100AssignWillRoleOne", USER_WILL_FULL_NAME);
	}

	@Test
	public void test102GetAccountWillFuture() throws Exception {
		final String TEST_NAME = "test102GetAccountWillFuture";
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
	}

	@Test
	public void test104RecomputeWill() throws Exception {
		final String TEST_NAME = "test104RecomputeWill";
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

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_INTERESTS_QNAME, INTEREST_ONE);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, false);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertAttributeFromCache(shadowModel, ATTR_INTERESTS_QNAME, INTEREST_ONE);
		assertNoAttribute(shadowModel, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowModel);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		assertCase(pendingOperation.getAsynchronousOperationReference(), SchemaConstants.CASE_STATE_OPEN);
	}

	@Test
	public void test106AddToBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test106AddToBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreProvisionWill(INTEREST_ONE);

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

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
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

		assertSinglePendingOperation(shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
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

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
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
		assertShadowPassword(shadowModel);

		assertSinglePendingOperation(shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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
		assertShadowPassword(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

	}

	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test204CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test204CloseCaseAndRecomputeWill";
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
	}


	/**
	 * disable - do not complete yet (do not wait for delta to expire, we want several deltas at once).
	 */
	@Test
	public void test220ModifyUserWillDisable() throws Exception {
		final String TEST_NAME = "test220ModifyUserWillDisable";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(userWillOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 2);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.DISABLED);
		assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModelFuture);

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

		ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class,
				userWillOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext,
				ActivationStatusType.ENABLED);
		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue(USER_WILL_PASSWORD_NEW);
		delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, ps);
		display("ObjectDelta", delta);

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		executeChanges(delta, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willSecondLastCaseOid = assertInProgress(result);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 3);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
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
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

		assertPendingOperationDeltas(shadowProvisioning, 3);
		pendingOperation = findPendingOperation(shadowProvisioning,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowProvisioning, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

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

		closeCase(willLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
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
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeModel = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		}

		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation,
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
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * lets ff 5min just for fun. Refresh, make sure everything should be the same (grace not expired yet)
	 */
	@Test
	public void test250RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test250RecomputeWillAfter5min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

		PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
		display("Shadow before", shadowBefore);

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
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
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		}
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation,
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

		backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.DISABLED, USER_WILL_PASSWORD_OLD);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeModel = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 3);

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
	public void test260ClosePasswordChangeCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test260ClosePasswordChangeCaseAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willSecondLastCaseOid);

		accountWillSecondCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillSecondCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
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
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		}
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 7min. Refresh. Oldest delta should expire.
	 */
	@Test
	public void test270RecomputeWillAfter7min() throws Exception {
		final String TEST_NAME = "test130RefreshAccountWillAfter7min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT7M");

		PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
		display("Shadow before", shadowBefore);

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
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
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		}
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 2);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	@Test
	public void test272UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test272UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.ENABLED, USER_WILL_PASSWORD_NEW);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 2);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

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
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 5min. Refresh. Another delta should expire.
	 */
	@Test
	public void test280RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test280RecomputeWillAfter5min";
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
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 1);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 5min. Refresh. All delta should expire.
	 */
	@Test
	public void test290RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test290RecomputeWillAfter5min";
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

		assertPendingOperationDeltas(shadowRepo, 0);

		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 0);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	@Test
	public void test300UnassignAccountWill() throws Exception {
		final String TEST_NAME = "test300UnassignAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		unassignRole(userWillOid, getRoleOneOid(), task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 1);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 1);
		pendingOperation = findPendingOperation(shadowModel, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, true);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Recompute. Make sure model will not try to delete the account again.
	 */
	@Test
	public void test302RecomputeWill() throws Exception {
		final String TEST_NAME = "test302RecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 1);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 1);
		pendingOperation = findPendingOperation(shadowModel, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, true);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test310CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test310CloseCaseAndRecomputeWill";
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
		display("result", result);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertUnassignedShadow(shadowModel, ActivationStatusType.ENABLED); // backing store not yet updated
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
		assertWillUnassignedFuture(shadowModelFuture, true);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	protected void assertUnassignedShadow(PrismObject<ShadowType> shadow, ActivationStatusType expectAlternativeActivationStatus) {
		assertShadowDead(shadow);
	}

	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test320RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test320RecomputeWillAfter5min";
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
		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertUnassignedShadow(shadowModel, ActivationStatusType.ENABLED); // backing store not yet updated
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
		assertWillUnassignedFuture(shadowModelFuture, true);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	@Test
	public void test330UpdateBackingStoreAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test330UpdateBackingStoreAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreDeprovisionWill();
		displayBackingStore();

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
		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertUnassignedShadow(shadowModel, ActivationStatusType.DISABLED);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, false);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	// TODO: nofetch, nofetch+future

	/**
	 * ff 20min, grace period expired, shadow should be gone, linkRef shoud be gone.
	 */
	@Test
	public void test340RecomputeWillAfter25min() throws Exception {
		final String TEST_NAME = "test340RecomputeWillAfter25min";
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

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		assertDeprovisionedTimedOutUser(userAfter, accountWillOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	// TODO: create, close case, then update backing store.

	// TODO: let grace period expire without updating the backing store (semi-manual-only)

	protected void assertDeprovisionedTimedOutUser(PrismObject<UserType> userAfter, String accountOid) throws Exception {
		assertLinks(userAfter, 0);
		assertNoShadow(accountOid);
	}

	/**
	 * Put everything in a clean state so we can start over.
	 */
	@Test
	public void test349CleanUp() throws Exception {
		final String TEST_NAME = "test349CleanUp";
		displayTestTitle(TEST_NAME);

		cleanupUser(TEST_NAME, userWillOid, USER_WILL_NAME, accountWillOid);
	}

	protected void cleanupUser(final String TEST_NAME, String userOid, String username, String accountOid) throws Exception {
		// nothing to do here
	}

	/**
	 * MID-4037
	 */
	@Test
	public void test500AssignWillRoleOne() throws Exception {
		assignWillRoleOne("test500AssignWillRoleOne", USER_WILL_FULL_NAME_PIRATE);
	}

	/**
	 * Unassign account before anybody had the time to do anything about it.
	 * Create ticket is not closed, the account is not yet created and we
	 * want to delete it.
	 * MID-4037
	 */
	@Test
	public void test510UnassignWillRoleOne() throws Exception {
		final String TEST_NAME = "test510UnassignWillRoleOne";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		unassignRole(userWillOid, getRoleOneOid(), task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willSecondLastCaseOid = assertInProgress(result);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, ChangeTypeType.ADD);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.IN_PROGRESS,
				null, null);
		assertNotNull("No ID in pending operation", pendingOperation.getId());

		assertWillUnassignPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);

		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);

		assertPendingOperationDeltas(shadowModel, 2);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertWillUnassignedFuture(shadowModelFuture, false);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);

		assertNotNull("No async reference in result", willSecondLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Just make sure recon does not ruin anything.
	 * MID-4037
	 */
	@Test
	public void test512ReconcileWill() throws Exception {
		final String TEST_NAME = "test512ReconcileWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, ChangeTypeType.ADD);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.IN_PROGRESS,
				null, null);
		assertNotNull("No ID in pending operation", pendingOperation.getId());

		assertWillUnassignPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);

		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);

		assertPendingOperationDeltas(shadowModel, 2);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertWillUnassignedFuture(shadowModelFuture, false);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	protected void assertWillUnassignPendingOperation(PrismObject<ShadowType> shadowRepo, OperationResultStatusType expectedStatus) {
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, ChangeTypeType.DELETE);
		if (expectedStatus == OperationResultStatusType.IN_PROGRESS) {
			assertPendingOperation(shadowRepo, pendingOperation,
					accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
					OperationResultStatusType.IN_PROGRESS,
					null, null);
		} else {
			pendingOperation = findPendingOperation(shadowRepo,
					OperationResultStatusType.SUCCESS, ChangeTypeType.DELETE);
			assertPendingOperation(shadowRepo, pendingOperation,
					accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
					OperationResultStatusType.SUCCESS,
					accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
			assertNotNull("No ID in pending operation", pendingOperation.getId());
		}
		assertNotNull("No ID in pending operation", pendingOperation.getId());
	}

	/**
	 * Close both cases at the same time.
	 * MID-4037
	 */
	@Test
	public void test515CloseCasesAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test515CloseCasesAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);
		closeCase(willSecondLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		// We need reconcile and not recompute here. We need to fetch the updated case status.
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 2);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, ChangeTypeType.ADD);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());

		assertWillUnassignPendingOperation(shadowRepo, OperationResultStatusType.SUCCESS);

		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertUnassignedShadow(shadowModel, null); // Shadow in not in the backing store

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, true);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 35min, grace period expired, shadow should be gone, linkRef should be gone.
	 * So we have clean state for next tests.
	 * MID-4037
	 */
	@Test
	public void test517RecomputeWillAfter35min() throws Exception {
		final String TEST_NAME = "test517RecomputeWillAfter35min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT35M");

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		// Shadow will not stay even in the "disable" case.
		// It was never created in the backing store
		assertLinks(userAfter, 0);

		// TODO: why?
//		assertNoShadow(accountWillOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * Put everything in a clean state so we can start over.
	 */
	@Test
	public void test519CleanUp() throws Exception {
		final String TEST_NAME = "test519CleanUp";
		displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		cleanupUser(TEST_NAME, userWillOid, USER_WILL_NAME, accountWillOid);

		// Make sure that all pending operations are expired
		clockForward("PT1H");
		recomputeUser(userWillOid, task, result);
		assertSuccess(result);

		assertNoShadow(accountWillOid);
	}

	/**
	 * MID-4095
	 */
	@Test
	public void test520AssignWillRoleOne() throws Exception {
		assignWillRoleOne("test520AssignWillRoleOne", USER_WILL_FULL_NAME_PIRATE);
	}


	/**
	 * Not much happens here yet. Role two is assigned. But it has validity in the future.
	 * Therefore there are no changes in the account, no new pending deltas.
	 * MID-4095
	 */
	@Test
	public void test522AssignWillRoleTwoValidFrom() throws Exception {
		final String TEST_NAME = "test522AssignWillRoleTwoValidFrom";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ActivationType activationType = new ActivationType();
		roleTwoValidFromTimestamp = getTimestamp("PT2H");
		activationType.setValidFrom(roleTwoValidFromTimestamp);

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(userWillOid, getRoleTwoOid(), activationType, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		accountWillOid = getSingleLinkOid(userAfter);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME_PIRATE);
	}


	/**
	 * Two hours forward. Role two is valid now.
	 * MID-4095
	 */
	@Test
	public void test524TwoHoursForRoleTwo() throws Exception {
		final String TEST_NAME = "test524TwoHoursForRoleTwo";
		displayTestTitle(TEST_NAME);
		// GIVEN

		clockForward("PT2H5M");

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willSecondLastCaseOid = assertInProgress(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		accountWillOid = getSingleLinkOid(userAfter);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

		ObjectDeltaType deltaModify = null;
		ObjectDeltaType deltaAdd = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			assertEquals("Wrong pending operation result", OperationResultStatusType.IN_PROGRESS, pendingOperation.getResultStatus());
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
				deltaAdd = delta;
			}
			if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
				deltaModify = delta;
				assertEquals("Wrong case ID", willSecondLastCaseOid, pendingOperation.getAsynchronousOperationReference());
			}
		}
		assertNotNull("No add pending delta", deltaAdd);
		assertNotNull("No modify pending delta", deltaModify);

		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertShadowExists(shadowRepo, false);
		assertNoShadowPassword(shadowRepo);

		// TODO: assert future

		assertNotNull("No async reference in result", willSecondLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * MID-4095
	 */
	@Test
	public void test525CloseCasesAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test515CloseCasesAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);
		closeCase(willSecondLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		// We need reconcile and not recompute here. We need to fetch the updated case status.
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 2);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * Unassign both roles at the same time.
	 * Note: In semi-manual cases the backing store is never updated.
	 */
	@Test
	public void test526UnassignWillBothRoles() throws Exception {
		final String TEST_NAME = "test526UnassignWillBothRoles";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBefore = getUser(userWillOid);

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);

		unassignAll(userBefore, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertTest526Deltas(shadowRepo, result);

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	protected void assertTest526Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
		assertPendingOperationDeltas(shadowRepo, 3);

		ObjectDeltaType deltaModify = null;
		ObjectDeltaType deltaAdd = null;
		ObjectDeltaType deltaDelete = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
				deltaAdd = delta;
				assertEquals("Wrong status in add delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
			}
			if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
				deltaModify = delta;
				assertEquals("Wrong status in modify delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
			}
			if (ChangeTypeType.DELETE.equals(delta.getChangeType())) {
				deltaDelete = delta;
				assertEquals("Wrong status in delete delta", OperationResultStatusType.IN_PROGRESS, pendingOperation.getResultStatus());
			}
		}
		assertNotNull("No add pending delta", deltaAdd);
		assertNotNull("No modify pending delta", deltaModify);
		assertNotNull("No delete pending delta", deltaDelete);
	}

	@Test
	public void test528CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test528CloseCaseAndRecomputeWill";
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
		display("result", result);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertTest528Deltas(shadowRepo, result);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	protected void assertTest528Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
		assertPendingOperationDeltas(shadowRepo, 3);

		ObjectDeltaType deltaModify = null;
		ObjectDeltaType deltaAdd = null;
		ObjectDeltaType deltaDelete = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			assertEquals("Wrong status in pending delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
				deltaAdd = delta;
			}
			if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
				deltaModify = delta;
			}
			if (ChangeTypeType.DELETE.equals(delta.getChangeType())) {
				deltaDelete = delta;
			}
		}
		assertNotNull("No add pending delta", deltaAdd);
		assertNotNull("No modify pending delta", deltaModify);
		assertNotNull("No delete pending delta", deltaDelete);

	}

	/**
	 * Put everything in a clean state so we can start over.
	 */
	@Test
	public void test529CleanUp() throws Exception {
		final String TEST_NAME = "test529CleanUp";
		displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		cleanupUser(TEST_NAME, userWillOid, USER_WILL_NAME, accountWillOid);

		// Make sure that all pending operations are expired
		clockForward("PT1H");
		recomputeUser(userWillOid, task, result);
		assertSuccess(result);

		assertNoShadow(accountWillOid);
	}

	// Tests 7xx are in the subclasses

	/**
	 * The 8xx tests is similar routine as 1xx,2xx,3xx, but this time
	 * with automatic updates using refresh task.
	 */
	@Test
	public void test800ImportShadowRefreshTask() throws Exception {
		final String TEST_NAME = "test800ImportShadowRefreshTask";
		displayTestTitle(TEST_NAME);
		// GIVEN

		// WHEN
		displayWhen(TEST_NAME);
		addTask(TASK_SHADOW_REFRESH_FILE);

		// THEN
		displayThen(TEST_NAME);

		waitForTaskStart(TASK_SHADOW_REFRESH_OID, false);
	}

	@Test
	public void test810AssignAccountWill() throws Exception {
		final String TEST_NAME = "test810AssignAccountWill";

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		modifyUserReplace(userWillOid, UserType.F_FULL_NAME, task, result, createPolyString(USER_WILL_FULL_NAME));

		// WHEN
		assignWillRoleOne(TEST_NAME, USER_WILL_FULL_NAME);

		// THEN
		restartTask(TASK_SHADOW_REFRESH_OID);
		waitForTaskFinish(TASK_SHADOW_REFRESH_OID, false);

		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME);
	}

	@Test
	public void test820AssignAccountJack() throws Exception {
		final String TEST_NAME = "test820AssignAccountJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

		accountJackReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, getResourceOid(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		jackLastCaseOid = assertInProgress(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);

		accountJackReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertAccountJackAfterAssign(TEST_NAME);

		// THEN
		restartTask(TASK_SHADOW_REFRESH_OID);
		waitForTaskFinish(TASK_SHADOW_REFRESH_OID, false);

		assertAccountJackAfterAssign(TEST_NAME);
	}

	@Test
	public void test830CloseCaseAndWaitForRefresh() throws Exception {
		final String TEST_NAME = "test830CloseCaseAndWaitForRefresh";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		restartTask(TASK_SHADOW_REFRESH_OID);
		waitForTaskFinish(TASK_SHADOW_REFRESH_OID, false);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertWillAfterCreateCaseClosed(TEST_NAME, false);

		assertAccountJackAfterAssign(TEST_NAME);
	}

	@Test
	public void test840AddToBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test840AddToBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreProvisionWill(INTEREST_ONE);

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
		assertShadowExists(shadowModel, true);
		assertShadowPassword(shadowModel);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, true);
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

	private String getRoleOid(int i) {
		return String.format("f363260a-8d7a-11e7-bd67-%012d", i);
	}

	private String getRoleName(int i) {
		return String.format("role-%012d", i);
	}

	private String getRoleInterest(int i) {
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


	protected void backingStoreProvisionWill(String interest) throws IOException {
		// nothing to do here
	}

	protected void backingStoreUpdateWill(String newFullName, String interest, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
		// nothing to do here
	}

	protected void backingStoreDeprovisionWill() throws IOException {
		// Nothing to do here
	}

	protected void displayBackingStore() throws IOException {
		// Nothing to do here
	}

	private void assignWillRoleOne(final String TEST_NAME, String expectedFullName) throws Exception {
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

		assertAccountWillAfterAssign(TEST_NAME, expectedFullName);
	}

	private void assertAccountWillAfterAssign(final String TEST_NAME, String expectedFullName) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
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

		assertSinglePendingOperation(shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

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

	private void assertWillAfterCreateCaseClosed(final String TEST_NAME, boolean backingStoreUpdated) throws Exception {
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
		assertEquals("Wroung number of pending operations in "+shadow, expectedNumber, pendingOperations.size());
	}

	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertSinglePendingOperation(shadow, requestStart, requestEnd,
				OperationResultStatusType.IN_PROGRESS, null, null);
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

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult) {
		return findPendingOperation(shadow, expectedResult, null, null);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult, ItemPath itemPath) {
		return findPendingOperation(shadow, expectedResult, null, itemPath);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult, ChangeTypeType expectedChangeType) {
		return findPendingOperation(shadow, expectedResult, expectedChangeType, null);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult, ChangeTypeType expectedChangeType, ItemPath itemPath) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		for (PendingOperationType pendingOperation: pendingOperations) {
			OperationResultStatusType result = pendingOperation.getResultStatus();
			if (result == null) {
				result = OperationResultStatusType.IN_PROGRESS;
			}
			if (pendingOperation.getResultStatus() != expectedResult) {
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

}