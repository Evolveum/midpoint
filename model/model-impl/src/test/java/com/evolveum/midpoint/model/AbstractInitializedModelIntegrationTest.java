/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.util.mock.MockClockworkHook;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class AbstractInitializedModelIntegrationTest extends AbstractConfiguredModelIntegrationTest {
	
	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME = "location";
	protected static final QName DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME);
	protected static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_PATH = new ItemPath(
			AccountShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME);
	
	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME = "ship";
	protected static final QName DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
	protected static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH = new ItemPath(
			AccountShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME);
	
	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME = "weapon";
	protected static final QName DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
	protected static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH = new ItemPath(
			AccountShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME);
	
	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME = "drink";
	protected static final QName DUMMY_ACCOUNT_ATTRIBUTE_DRINK_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
	protected static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_DRINK_PATH = new ItemPath(
			AccountShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_QNAME);

	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME = "quote";
	protected static final QName DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME);
	protected static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH = new ItemPath(
			AccountShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_QNAME);

	private static final Object NUM_FUNCTIONAL_ORGS = 6;
	private static final Object NUM_PROJECT_ORGS = 3;
	
	protected static final String MOCK_CLOCKWORK_HOOK_URL = MidPointConstants.NS_MIDPOINT_TEST_PREFIX + "/mockClockworkHook";
	
	private static final int DEFAULT_TASK_WAIT_TIMEOUT = 10000;
	private static final long DEFAULT_TASK_SLEEP_TIME = 200;
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractInitializedModelIntegrationTest.class);
	
	protected MockClockworkHook mockClockworkHook;
	protected boolean verbose = false;
		
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected UserType userTypeGuybrush;
	protected UserType userTypeElaine;
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;
	protected ResourceType resourceDummyRedType;
	protected PrismObject<ResourceType> resourceDummyRed;
	protected ResourceType resourceDummyBlueType;
	protected PrismObject<ResourceType> resourceDummyBlue;
	protected ResourceType resourceDummyWhiteType;
	protected PrismObject<ResourceType> resourceDummyWhite;
	protected ResourceType resourceDummyGreenType;
	protected PrismObject<ResourceType> resourceDummyGreen;
	protected ResourceType resourceDummySchemalessType;
	protected PrismObject<ResourceType> resourceDummySchemaless;
	
	protected static DummyResource dummyResource;
	protected static DummyResource dummyResourceRed;
	protected static DummyResource dummyResourceBlue;
	protected static DummyResource dummyResourceWhite;
	protected static DummyResource dummyResourceGreen;
	
	public AbstractInitializedModelIntegrationTest() {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		mockClockworkHook = new MockClockworkHook();
		hookRegistry.registerChangeHook(MOCK_CLOCKWORK_HOOK_URL, mockClockworkHook);
		
		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();
		extendDummySchema(dummyResource);
					
		dummyResourceRed = DummyResource.getInstance(RESOURCE_DUMMY_RED_NAME);
		dummyResourceRed.reset();
		dummyResourceRed.populateWithDefaultSchema();
		extendDummySchema(dummyResourceRed);
		
		dummyResourceBlue = DummyResource.getInstance(RESOURCE_DUMMY_BLUE_NAME);
		dummyResourceBlue.reset();
		dummyResourceBlue.populateWithDefaultSchema();
		extendDummySchema(dummyResourceBlue);
		
		dummyResourceWhite = DummyResource.getInstance(RESOURCE_DUMMY_WHITE_NAME);
		dummyResourceWhite.reset();
		dummyResourceWhite.populateWithDefaultSchema();
		
		dummyResourceGreen = DummyResource.getInstance(RESOURCE_DUMMY_GREEN_NAME);
		dummyResourceGreen.reset();
		dummyResourceGreen.populateWithDefaultSchema();
		extendDummySchema(dummyResourceGreen);
		
		postInitDummyResouce();
		
		addDummyAccount(dummyResource, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", "Monkey Island");
		addDummyAccount(dummyResource, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Melee Island");
		addDummyAccount(dummyResource, ACCOUNT_DAVIEJONES_DUMMY_USERNAME, "Davie Jones", "Davie Jones' Locker");
		addDummyAccount(dummyResource, ACCOUNT_CALYPSO_DUMMY_USERNAME, "Tia Dalma", "Pantano River");
		
		addDummyAccount(dummyResource, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", "Melee Island");
		addDummyAccount(dummyResourceRed, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", "Melee Island");
		addDummyAccount(dummyResourceBlue, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", "Melee Island");
		
		// User Templates
		addObjectFromFile(USER_TEMPLATE_FILENAME, UserTemplateType.class, initResult);
		addObjectFromFile(USER_TEMPLATE_COMPLEX_FILENAME, UserTemplateType.class, initResult);

		// Connectors
		addObjectFromFile(CONNECTOR_LDAP_FILENAME, ConnectorType.class, initResult);
		addObjectFromFile(CONNECTOR_DBTABLE_FILENAME, ConnectorType.class, initResult);
		addObjectFromFile(CONNECTOR_DUMMY_FILENAME, ConnectorType.class, initResult);
		
		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILENAME, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		
		resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILENAME, RESOURCE_DUMMY_OID, initTask, initResult);
		resourceDummyType = resourceDummy.asObjectable();
		resourceDummyRed = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_RED_FILENAME, RESOURCE_DUMMY_RED_OID, initTask, initResult); 
		resourceDummyRedType = resourceDummyRed.asObjectable();
		resourceDummyBlue = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_BLUE_FILENAME, RESOURCE_DUMMY_BLUE_OID, initTask, initResult); 
		resourceDummyBlueType = resourceDummyBlue.asObjectable();
		resourceDummySchemaless = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_SCHEMALESS_FILENAME, RESOURCE_DUMMY_SCHEMALESS_OID, initTask, initResult); 
		resourceDummySchemalessType = resourceDummySchemaless.asObjectable();
		resourceDummyWhite = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_WHITE_FILENAME, RESOURCE_DUMMY_WHITE_OID, initTask, initResult);
		resourceDummyWhiteType = resourceDummyWhite.asObjectable();
		resourceDummyGreen = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_GREEN_FILENAME, RESOURCE_DUMMY_GREEN_OID, initTask, initResult);
		resourceDummyGreenType = resourceDummyGreen.asObjectable();

		// Accounts
		addObjectFromFile(ACCOUNT_HBARBOSSA_OPENDJ_FILENAME, AccountShadowType.class, initResult);
		addObjectFromFile(ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME, AccountShadowType.class, initResult);
		addObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME, AccountShadowType.class, initResult);
		addObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_FILENAME, AccountShadowType.class, initResult);
		addObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_FILENAME, AccountShadowType.class, initResult);
		
		// Users
		userTypeJack = addObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		userTypeBarbossa = addObjectFromFile(USER_BARBOSSA_FILENAME, UserType.class, initResult).asObjectable();
		userTypeGuybrush = addObjectFromFile(USER_GUYBRUSH_FILENAME, UserType.class, initResult).asObjectable();
		userTypeElaine = addObjectFromFile(USER_ELAINE_FILENAME, UserType.class, initResult).asObjectable();
		
		// Roles
		addObjectFromFile(ROLE_PIRATE_FILENAME, RoleType.class, initResult);
		addObjectFromFile(ROLE_JUDGE_FILENAME, RoleType.class, initResult);
		addObjectFromFile(ROLE_DUMMIES_FILENAME, RoleType.class, initResult);
		
		// Orgstruct
		addObjectsFromFile(ORG_MONKEY_ISLAND_FILENAME, OrgType.class, initResult);
		
	}
	
	protected void addDummyAccount(DummyResource resource, String userId, String fullName, String location) throws com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, SchemaViolationException {
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, location);
		resource.addAccount(account);
	}

	private void extendDummySchema(DummyResource dummyResource) throws ConnectException, FileNotFoundException {
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
		DummyAttributeDefinition titleAttrDef = new DummyAttributeDefinition("title", String.class, false, true);
		accountObjectClass.add(titleAttrDef);
		DummyAttributeDefinition shipAttrDef = new DummyAttributeDefinition("ship", String.class, false, false);
		accountObjectClass.add(shipAttrDef);
		DummyAttributeDefinition locationAttrDef = new DummyAttributeDefinition("location", String.class, false, false);
		accountObjectClass.add(locationAttrDef);
		DummyAttributeDefinition lootAttrDef = new DummyAttributeDefinition("loot", Integer.class, false, false);
		accountObjectClass.add(lootAttrDef);
		DummyAttributeDefinition weaponAttrDef = new DummyAttributeDefinition("weapon", String.class, false, true);
		accountObjectClass.add(weaponAttrDef);
		DummyAttributeDefinition drinkAttrDef = new DummyAttributeDefinition("drink", String.class, false, true);
		accountObjectClass.add(drinkAttrDef);
		DummyAttributeDefinition quoteAttrDef = new DummyAttributeDefinition("quote", String.class, false, true);
		accountObjectClass.add(quoteAttrDef);
	}

	protected void postInitDummyResouce() {
		// Do nothing be default. Concrete tests may override this.
	}

	protected void assertDummyRefinedSchemaSanity(RefinedResourceSchema refinedSchema) {
		
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
		assertFalse("UID has update",uidDef.canUpdate());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canCreate());
		assertTrue("No NAME update",nameDef.canUpdate());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canCreate());
		assertTrue("No fullname update", fullnameDef.canUpdate());
		assertTrue("No fullname read", fullnameDef.canRead());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));
		
	}
		
	protected void assertUserJack(PrismObject<UserType> user) {
		assertUserJack(user, "Jack Sparrow", "Jack", "Sparrow");
	}
	
	protected void assertUserJack(PrismObject<UserType> user, String fullName) {
		assertUserJack(user, fullName, "Jack", "Sparrow");
	}
	
	protected void assertUserJack(PrismObject<UserType> user, String fullName, String givenName, String familyName) {
		assertUser(user, USER_JACK_OID, "jack", fullName, givenName, familyName);
		UserType userType = user.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong jack honorificPrefix", "Cpt.", userType.getHonorificPrefix());
		PrismAsserts.assertEqualsPolyString("Wrong jack honorificSuffix", "PhD.", userType.getHonorificSuffix());
		assertEquals("Wrong jack emailAddress", "jack.sparrow@evolveum.com", userType.getEmailAddress());
		assertEquals("Wrong jack telephoneNumber", "555-1234", userType.getTelephoneNumber());
		assertEquals("Wrong jack employeeNumber", "emp1234", userType.getEmployeeNumber());
		assertEquals("Wrong jack employeeType", "CAPTAIN", userType.getEmployeeType().get(0));
		PrismAsserts.assertEqualsPolyString("Wrong jack locality", "Caribbean", userType.getLocality());
	}
	
	protected void assertDummyShadowRepo(PrismObject<AccountShadowType> accountShadow, String oid, String username) {
		assertShadowRepo(accountShadow, oid, username, resourceDummyType);
	}
	
	protected void assertDummyShadowModel(PrismObject<AccountShadowType> accountShadow, String oid, String username, String fullname) {
		assertShadowModel(accountShadow, oid, username, resourceDummyType);
		IntegrationTestTools.assertAttribute(accountShadow, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME, fullname);
	}

	protected DummyAccount getDummyAccount(String dummyInstanceName, String username) {
		DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		return dummyResource.getAccountByUsername(username);
	}
	
	protected void assertDummyAccount(String username, String fullname, boolean active) {
		assertDummyAccount(null, username, fullname, active);
	}
	
	protected void assertDummyAccount(String dummyInstanceName, String username, String fullname, boolean active) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy("+dummyInstanceName+") account for username "+username, account);
		assertEquals("Wrong fullname for dummy("+dummyInstanceName+") account "+username, fullname, account.getAttributeValue("fullname"));
		assertEquals("Wrong activation for dummy("+dummyInstanceName+") account "+username, active, account.isEnabled());
	}

	protected void assertNoDummyAccount(String username) {
		assertNoDummyAccount(null, username);
	}
	
	protected void assertNoDummyAccount(String dummyInstanceName, String username) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNull("Dummy account for username "+username+" exists while not expecting it", account);
	}
	
	protected void assertDefaultDummyAccountAttribute(String username, String attributeName, Object... expectedAttributeValues) {
		assertDummyAccountAttribute(null, username, attributeName, expectedAttributeValues);
	}
	
	protected void assertDummyAccountAttribute(String dummyInstanceName, String username, String attributeName, Object... expectedAttributeValues) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy account for username "+username, account);
		Set<Object> values = account.getAttributeValues(attributeName, Object.class);
		assertNotNull("No values for attribute "+attributeName+" of dummy account "+username, values);
		assertEquals("Unexpected number of values for attribute "+attributeName+" of dummy account "+username+": "+values, expectedAttributeValues.length, values.size());
		for (Object expectedValue: expectedAttributeValues) {
			if (!values.contains(expectedValue)) {
				AssertJUnit.fail("Value '"+expectedValue+"' expected in attribute "+attributeName+" of dummy account "+username+
						" but not found. Values found: "+values);
			}
		}
	}
		
	protected void setDefaultUserTemplate(String userTemplateOid)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObjectDefinition<SystemConfigurationType> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class);

		PrismReferenceValue userTemplateRefVal = new PrismReferenceValue(userTemplateOid);
		
		Collection<? extends ItemDelta> modifications = ReferenceDelta.createModificationReplaceCollection(
						SystemConfigurationType.F_DEFAULT_USER_TEMPLATE_REF,
						objectDefinition, userTemplateRefVal);

		OperationResult result = new OperationResult("Aplying default user template");

		repositoryService.modifyObject(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, result);
		display("Aplying default user template result", result);
		result.computeStatus();
		assertSuccess("Aplying default user template failed (result)", result);
	}

	protected ItemPath getOpenDJAttributePath(String attrName) {
		return new ItemPath(
				ResourceObjectShadowType.F_ATTRIBUTES,
				new QName(RESOURCE_OPENDJ_NAMESPACE, attrName));
		
	}

	protected ItemPath getDummyAttributePath(String attrName) {
		return new ItemPath(
				ResourceObjectShadowType.F_ATTRIBUTES,
				new QName(RESOURCE_DUMMY_NAMESPACE, attrName));
		
	}
	
	protected void assertMonkeyIslandOrgSanity() throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractInitializedModelIntegrationTest.class.getName() + ".assertMonkeyIslandOrgSanity");
        OperationResult result = task.getResult();
        
        PrismObject<OrgType> orgGovernorOffice = modelService.getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID, null, task, result);
        result.computeStatus();
        assertSuccess(result);
        OrgType orgGovernorOfficeType = orgGovernorOffice.asObjectable();
        assertEquals("Wrong governor office name", PrismTestUtil.createPolyStringType("F0001"), orgGovernorOfficeType.getName());
        
        List<PrismObject<OrgType>> governorSubOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, null, 1, task, result);
        if (verbose) display("governor suborgs", governorSubOrgs);
        assertEquals("Unexpected number of governor suborgs", 3, governorSubOrgs.size());
        
        List<PrismObject<OrgType>> functionalOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, null, null, task, result);
        if (verbose) display("functional orgs (null)", functionalOrgs);
        assertEquals("Unexpected number of functional orgs (null)", NUM_FUNCTIONAL_ORGS, functionalOrgs.size());
        
        functionalOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, null, -1, task, result);
        if (verbose) display("functional orgs (-1)", functionalOrgs);
        assertEquals("Unexpected number of functional orgs (-1)", NUM_FUNCTIONAL_ORGS, functionalOrgs.size());
        
        List<PrismObject<OrgType>> prootSubOrgs = searchOrg(ORG_PROJECT_ROOT_OID, null, 1, task, result);
        if (verbose) display("project root suborgs", prootSubOrgs);
        assertEquals("Unexpected number of governor suborgs", 2, prootSubOrgs.size());
        
        List<PrismObject<OrgType>> projectOrgs = searchOrg(ORG_PROJECT_ROOT_OID, null, null, task, result);
        if (verbose) display("project orgs (null)", projectOrgs);
        assertEquals("Unexpected number of functional orgs (null)", NUM_PROJECT_ORGS, projectOrgs.size());
        
        projectOrgs = searchOrg(ORG_PROJECT_ROOT_OID, null, -1, task, result);
        if (verbose) display("project orgs (-1)", projectOrgs);
        assertEquals("Unexpected number of functional orgs (-1)", NUM_PROJECT_ORGS, projectOrgs.size());
	}
     	
}
