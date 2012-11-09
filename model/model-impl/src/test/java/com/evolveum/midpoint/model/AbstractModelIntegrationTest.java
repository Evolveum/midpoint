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
import com.evolveum.midpoint.model.test.util.mock.MockClockworkHook;
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
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
public class AbstractModelIntegrationTest extends AbstractIntegrationTest {
	
	protected static final String COMMON_DIR_NAME = "src/test/resources/common";
	
	protected static final String DEFAULT_ACCOUNT_TYPE = "default";
	
	public static final String SYSTEM_CONFIGURATION_FILENAME = COMMON_DIR_NAME + "/system-configuration.xml";
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final String USER_TEMPLATE_FILENAME = COMMON_DIR_NAME + "/user-template.xml";
	protected static final String USER_TEMPLATE_OID = "10000000-0000-0000-0000-000000000002";
	
	protected static final String USER_TEMPLATE_COMPLEX_FILENAME = COMMON_DIR_NAME + "/user-template-complex.xml";
	protected static final String USER_TEMPLATE_COMPLEX_OID = "10000000-0000-0000-0000-000000000222";

	protected static final String CONNECTOR_LDAP_FILENAME = COMMON_DIR_NAME + "/connector-ldap.xml";
	protected static final String CONNECTOR_DBTABLE_FILENAME = COMMON_DIR_NAME + "/connector-dbtable.xml";
	protected static final String CONNECTOR_DUMMY_FILENAME = COMMON_DIR_NAME + "/connector-dummy.xml";
	
	protected static final String RESOURCE_OPENDJ_FILENAME = COMMON_DIR_NAME + "/resource-opendj.xml";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000003";
	
	protected static final String RESOURCE_DUMMY_FILENAME = COMMON_DIR_NAME + "/resource-dummy.xml";
	protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	protected static final String RESOURCE_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004";
	
	protected static final String RESOURCE_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/resource-dummy-red.xml";
	protected static final String RESOURCE_DUMMY_RED_OID = "10000000-0000-0000-0000-000000000104";
	protected static final String RESOURCE_DUMMY_RED_NAME = "red";
	protected static final String RESOURCE_DUMMY_RED_NAMESPACE = MidPointConstants.NS_RI;

	protected static final String RESOURCE_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/resource-dummy-blue.xml";
	protected static final String RESOURCE_DUMMY_BLUE_OID = "10000000-0000-0000-0000-000000000204";
	protected static final String RESOURCE_DUMMY_BLUE_NAME = "blue";
	protected static final String RESOURCE_DUMMY_BLUE_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final String RESOURCE_DUMMY_SCHEMALESS_FILENAME = COMMON_DIR_NAME + "/resource-dummy-schemaless-no-schema.xml";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0000";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAME = "schemaless";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final String ROLE_ALPHA_FILENAME = COMMON_DIR_NAME + "/role-alpha.xml";
	protected static final String ROLE_ALPHA_OID = "12345678-d34d-b33f-f00d-55555555aaaa";

	protected static final String ROLE_BETA_FILENAME = COMMON_DIR_NAME + "/role-beta.xml";
	protected static final String ROLE_BETA_OID = "12345678-d34d-b33f-f00d-55555555bbbb";
	
	protected static final String ROLE_PIRATE_FILENAME = COMMON_DIR_NAME + "/role-pirate.xml";
	protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";

	protected static final String ROLE_JUDGE_FILENAME = COMMON_DIR_NAME + "/role-judge.xml";
	protected static final String ROLE_JUDGE_OID = "12345111-1111-2222-1111-121212111111";
	
	protected static final String ROLE_DUMMIES_FILENAME = COMMON_DIR_NAME + "/role-dummies.xml";
	protected static final String ROLE_DUMMIES_OID = "12345678-d34d-b33f-f00d-55555555dddd";

	protected static final String USER_JACK_FILENAME = COMMON_DIR_NAME + "/user-jack.xml";
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";

	protected static final String USER_BARBOSSA_FILENAME = COMMON_DIR_NAME + "/user-barbossa.xml";
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";

	protected static final String USER_GUYBRUSH_FILENAME = COMMON_DIR_NAME + "/user-guybrush.xml";
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	
	// Largo does not have a full name set, employeeType=PIRATE
	protected static final String USER_LARGO_FILENAME = COMMON_DIR_NAME + "/user-largo.xml";
	protected static final String USER_LARGO_OID = "c0c010c0-d34d-b33f-f00d-111111111118";
	
	// Rapp does not have a full name set, employeeType=COOK
	protected static final String USER_RAPP_FILENAME = COMMON_DIR_NAME + "/user-rapp.xml";
	protected static final String USER_RAPP_OID = "c0c010c0-d34d-b33f-f00d-11111111c008";
	protected static final String USER_RAPP_USERNAME = "rapp";

	// Has null name, doesn not have given name, no employeeType
	protected static final String USER_THREE_HEADED_MONKEY_FILENAME = COMMON_DIR_NAME + "/user-three-headed-monkey.xml";
	protected static final String USER_THREE_HEADED_MONKEY_OID = "c0c010c0-d34d-b33f-f00d-110011001133";
	
	protected static final String USER_ADMINISTRATOR_FILENAME = COMMON_DIR_NAME + "/user-administrator.xml";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	
	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-hbarbossa-opendj.xml";
	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_OID = "c0c010c0-d34d-b33f-f00d-222211111112";
	
	public static final String ACCOUNT_JACK_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-jack-dummy.xml";
	
	public static final String ACCOUNT_HERMAN_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-herman-dummy.xml";
	public static final String ACCOUNT_HERMAN_DUMMY_OID = "22220000-2200-0000-0000-444400004444";
	public static final String ACCOUNT_HERMAN_DUMMY_USERNAME = "ht";
	
	public static final String ACCOUNT_HERMAN_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-herman-opendj.xml";
	public static final String ACCOUNT_HERMAN_OPENDJ_OID = "22220000-2200-0000-0000-333300003333";
	
	public static final String ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-guybrush-dummy.xml";
	public static final String ACCOUNT_SHADOW_GUYBRUSH_OID = "22226666-2200-6666-6666-444400004444";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-guybrush-dummy.xml";
	
	public static final String ACCOUNT_SHADOW_JACK_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-jack-dummy.xml";
	
	public static final String ACCOUNT_DAVIEJONES_DUMMY_USERNAME = "daviejones";
	public static final String ACCOUNT_CALYPSO_DUMMY_USERNAME = "calypso";
	
	protected static final String PASSWORD_POLICY_GLOBAL_FILENAME = COMMON_DIR_NAME + "/password-policy-global.xml";
	protected static final String PASSWORD_POLICY_GLOBAL_OID = "12344321-0000-0000-0000-000000000003";
	
	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME = "fullname";
	protected static final QName DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
	protected static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH = new ItemPath(
			AccountShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME);
	
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
	
	protected static final String ORG_MONKEY_ISLAND_FILENAME = COMMON_DIR_NAME + "/org-monkey-island.xml";
	protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
	
	protected static final String TASK_RECONCILE_DUMMY_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy.xml";
	protected static final String TASK_RECONCILE_DUMMY_OID = "91919191-76e0-59e2-86d6-3d4f02d3dddd";
	
	protected static final String MOCK_CLOCKWORK_HOOK_URL = MidPointConstants.NS_MIDPOINT_TEST_PREFIX + "/mockClockworkHook";
	
	private static final int DEFAULT_TASK_WAIT_TIMEOUT = 10000;
	private static final long DEFAULT_TASK_SLEEP_TIME = 200;

	@Autowired(required = true)
	protected ModelService modelService;
	
	@Autowired(required = true)
	protected ModelInteractionService modelInteractionService;
	
	@Autowired(required = true)
	protected RepositoryService repositoryService;
	
	@Autowired(required = true)
	protected ProvisioningService provisioningService;
	
	@Autowired(required = true)
	protected HookRegistry hookRegistry;
	
	@Autowired(required = true)
	protected PrismContext prismContext;
	
	protected DummyAuditService dummyAuditService;
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);
	
	protected MockClockworkHook mockClockworkHook;
	
	protected PrismObject<UserType> userAdministrator;
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected UserType userTypeGuybrush;
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;
	protected ResourceType resourceDummyRedType;
	protected PrismObject<ResourceType> resourceDummyRed;
	protected ResourceType resourceDummyBlueType;
	protected PrismObject<ResourceType> resourceDummyBlue;
	protected ResourceType resourceDummySchemalessType;
	protected PrismObject<ResourceType> resourceDummySchemaless;
	
	protected static DummyResource dummyResource;
	protected static DummyResource dummyResourceRed;
	protected static DummyResource dummyResourceBlue;
	
	public AbstractModelIntegrationTest() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		
		mockClockworkHook = new MockClockworkHook();
		hookRegistry.registerChangeHook(MOCK_CLOCKWORK_HOOK_URL, mockClockworkHook);
		
		dummyAuditService = DummyAuditService.getInstance();
		
		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();
		extendDummySchema(dummyResource);
		
		addDummyAccount(dummyResource, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", "Monkey Island");
		addDummyAccount(dummyResource, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Melee Island");
		addDummyAccount(dummyResource, ACCOUNT_DAVIEJONES_DUMMY_USERNAME, "Davie Jones", "Davie Jones' Locker");
		addDummyAccount(dummyResource, ACCOUNT_CALYPSO_DUMMY_USERNAME, "Tia Dalma", "Pantano River");
		
		dummyResourceRed = DummyResource.getInstance(RESOURCE_DUMMY_RED_NAME);
		dummyResourceRed.reset();
		dummyResourceRed.populateWithDefaultSchema();
		extendDummySchema(dummyResourceRed);
		
		dummyResourceBlue = DummyResource.getInstance(RESOURCE_DUMMY_BLUE_NAME);
		dummyResourceBlue.reset();
		dummyResourceBlue.populateWithDefaultSchema();
		extendDummySchema(dummyResourceBlue);
		
		postInitDummyResouce();
		
		// System Configuration
		try {
			addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		// User Templates
		addObjectFromFile(USER_TEMPLATE_FILENAME, UserTemplateType.class, initResult);
		addObjectFromFile(USER_TEMPLATE_COMPLEX_FILENAME, UserTemplateType.class, initResult);

		// Connectors
		addObjectFromFile(CONNECTOR_LDAP_FILENAME, ConnectorType.class, initResult);
		addObjectFromFile(CONNECTOR_DBTABLE_FILENAME, ConnectorType.class, initResult);
		addObjectFromFile(CONNECTOR_DUMMY_FILENAME, ConnectorType.class, initResult);
		
		// Resources
		resourceOpenDj = addObjectFromFile(RESOURCE_OPENDJ_FILENAME, ResourceType.class, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		resourceDummy = addObjectFromFile(RESOURCE_DUMMY_FILENAME, ResourceType.class, initResult);
		resourceDummyType = resourceDummy.asObjectable();
		resourceDummyRed = addObjectFromFile(RESOURCE_DUMMY_RED_FILENAME, ResourceType.class, initResult);
		resourceDummyRedType = resourceDummyRed.asObjectable();
		resourceDummyBlue = addObjectFromFile(RESOURCE_DUMMY_BLUE_FILENAME, ResourceType.class, initResult);
		resourceDummyBlueType = resourceDummyBlue.asObjectable();
		resourceDummySchemaless = addObjectFromFile(RESOURCE_DUMMY_SCHEMALESS_FILENAME, ResourceType.class, initResult);
		resourceDummySchemalessType = resourceDummySchemaless.asObjectable();

		// Accounts
		addObjectFromFile(ACCOUNT_HBARBOSSA_OPENDJ_FILENAME, AccountShadowType.class, initResult);
		addObjectFromFile(ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME, AccountShadowType.class, initResult);
		
		// Users
		userAdministrator = addObjectFromFile(USER_ADMINISTRATOR_FILENAME, UserType.class, initResult);
		userTypeJack = addObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		userTypeBarbossa = addObjectFromFile(USER_BARBOSSA_FILENAME, UserType.class, initResult).asObjectable();
		userTypeGuybrush = addObjectFromFile(USER_GUYBRUSH_FILENAME, UserType.class, initResult).asObjectable();
		
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

	private void extendDummySchema(DummyResource dummyResource) {
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
	}

	protected void postInitDummyResouce() {
		// Do nothing be default. Concrete tests may override this.
	}
	
	protected void importObjectFromFile(String filename) throws FileNotFoundException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".importObjectFromFile");
		importObjectFromFile(filename, result);
		result.computeStatus();
		assertSuccess(result);
	}

	protected void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
		LOGGER.trace("importObjectFromFile: {}", filename);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(filename);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);
	}
	
	protected LensContext<UserType, AccountShadowType> createUserAccountContext() {
		return new LensContext<UserType, AccountShadowType>(UserType.class, AccountShadowType.class, prismContext);
	}
	
	protected void fillContextWithUser(LensContext<UserType, AccountShadowType> context, PrismObject<UserType> user) throws SchemaException, ObjectNotFoundException {
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setObjectOld(user);
	}
	
	protected void fillContextWithUser(LensContext<UserType, AccountShadowType> context, String userOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
        fillContextWithUser(context, user);
    }
	
	protected void fillContextWithUserFromFile(LensContext<UserType, AccountShadowType> context, String filename) throws SchemaException,
	ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(filename));
		fillContextWithUser(context, user);
	}
	
	protected void fillContextWithEmtptyAddUserDelta(LensContext<UserType, AccountShadowType> context, OperationResult result) throws SchemaException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyAddDelta(UserType.class, null, prismContext);
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setPrimaryDelta(userDelta);
	}
	
	protected void fillContextWithAddUserDelta(LensContext<UserType, AccountShadowType> context, PrismObject<UserType> user) throws SchemaException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setPrimaryDelta(userDelta);
	}

	protected void fillContextWithAccount(LensContext<UserType, AccountShadowType> context, String accountOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<AccountShadowType> account = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        provisioningService.applyDefinition(account, result);
        fillContextWithAccount(context, account, result);
	}

	protected void fillContextWithAccountFromFile(LensContext<UserType, AccountShadowType> context, String filename, OperationResult result) throws SchemaException,
	ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(filename));
		fillContextWithAccount(context, account, result);
	}

    protected void fillContextWithAccount(LensContext<UserType, AccountShadowType> context, PrismObject<AccountShadowType> account, OperationResult result) throws SchemaException,
		ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	AccountShadowType accountType = account.asObjectable();
        String resourceOid = accountType.getResourceRef().getOid();
        ResourceType resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, result).asObjectable();
        applyResourceSchema(accountType, resourceType);
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, ResourceObjectShadowUtil.getIntent(accountType));
        LensProjectionContext<AccountShadowType> accountSyncContext = context.findOrCreateProjectionContext(rat);
        accountSyncContext.setOid(account.getOid());
		accountSyncContext.setObjectOld(account);
		accountSyncContext.setResource(resourceType);
		context.rememberResource(resourceType);
    }
    
    protected void makeImportSyncDelta(LensProjectionContext<AccountShadowType> accContext) {
    	PrismObject<AccountShadowType> syncAccountToAdd = accContext.getObjectOld().clone();
    	ObjectDelta<AccountShadowType> syncDelta = ObjectDelta.createAddDelta(syncAccountToAdd);
    	accContext.setSyncDelta(syncDelta);
    }
    
    /**
     * This is not the real thing. It is just for the tests. 
     */
    protected void applyResourceSchema(AccountShadowType accountType, ResourceType resourceType) throws SchemaException {
    	IntegrationTestTools.applyResourceSchema(accountType, resourceType, prismContext);
    }

	protected ObjectDelta<UserType> addModificationToContext(LensContext<UserType, AccountShadowType> context, String filename) throws JAXBException,
			SchemaException, FileNotFoundException {
	    ObjectModificationType modElement = PrismTestUtil.unmarshalObject(new File(filename), ObjectModificationType.class);
	    ObjectDelta<UserType> userDelta = DeltaConvertor.createObjectDelta(modElement, UserType.class, prismContext);
	    LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
	    focusContext.addPrimaryDelta(userDelta);
	    return userDelta;
	}
	
	
	protected ObjectDelta<UserType> addModificationToContextReplaceUserProperty(LensContext<UserType, AccountShadowType> context, 
			QName propertyName, Object... propertyValues) throws SchemaException {
		return addModificationToContextReplaceUserProperty(context, new ItemPath(propertyName), propertyValues);
	}
	
	protected ObjectDelta<UserType> addModificationToContextReplaceUserProperty(LensContext<UserType, AccountShadowType> context, 
			ItemPath propertyPath, Object... propertyValues) throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, focusContext.getObjectOld().getOid(), 
				propertyPath, prismContext, propertyValues);
		focusContext.addPrimaryDelta(userDelta);
	    return userDelta;
	}
	
	protected ObjectDelta<UserType> addModificationToContextAddAccountFromFile(LensContext<UserType, AccountShadowType> context, String filename) 
			throws JAXBException, SchemaException, FileNotFoundException {
		PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(filename));
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddReference(UserType.class, focusContext.getObjectOld().getOid(),
				UserType.F_ACCOUNT_REF, prismContext, account);
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected ObjectDelta<AccountShadowType> addModificationToContextDeleteAccount(LensContext<UserType, AccountShadowType> context, 
			String accountOid) 
			throws SchemaException, FileNotFoundException {
		LensProjectionContext<AccountShadowType> accountCtx = context.findProjectionContextByOid(accountOid);
		ObjectDelta<AccountShadowType> deleteAccountDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, accountOid, prismContext);
		accountCtx.addPrimaryDelta(deleteAccountDelta);
		return deleteAccountDelta;
	}

	protected <T> ObjectDelta<AccountShadowType> addModificationToContextReplaceAccountAttribute(LensContext<UserType, AccountShadowType> context, String accountOid, 
			String attributeLocalName, T... propertyValues) throws SchemaException {
		LensProjectionContext<AccountShadowType> accCtx = context.findProjectionContextByOid(accountOid);		
		ObjectDelta<AccountShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName, propertyValues);
		accCtx.addPrimaryDelta(accountDelta);
	    return accountDelta;
	}
	
	protected <T> ObjectDelta<AccountShadowType> addSyncModificationToContextReplaceAccountAttribute(LensContext<UserType, AccountShadowType> context, String accountOid, 
			String attributeLocalName, T... propertyValues) throws SchemaException {
		LensProjectionContext<AccountShadowType> accCtx = context.findProjectionContextByOid(accountOid);		
		ObjectDelta<AccountShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName, propertyValues);
		accCtx.addAccountSyncDelta(accountDelta);
	    return accountDelta;
	}

	protected <T> ObjectDelta<AccountShadowType> createAccountDelta(LensProjectionContext<AccountShadowType> accCtx, String accountOid, 
			String attributeLocalName, T... propertyValues) throws SchemaException {
		ResourceType resourceType = accCtx.getResource();
		QName attrQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeLocalName);
		ItemPath attrPath = new ItemPath(AccountShadowType.F_ATTRIBUTES, attrQName);
		RefinedAccountDefinition refinedAccountDefinition = accCtx.getRefinedAccountDefinition();
		RefinedAttributeDefinition attrDef = refinedAccountDefinition.findAttributeDefinition(attrQName);
		assertNotNull("No definition of attribute "+attrQName+" in account def "+refinedAccountDefinition, attrDef);
		ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createEmptyModifyDelta(AccountShadowType.class, accountOid, prismContext);
		PropertyDelta<T> attrDelta = new PropertyDelta<T>(attrPath, attrDef);
		attrDelta.setValuesToReplace(PrismPropertyValue.createCollection(propertyValues));
		accountDelta.addModification(attrDelta);
		return accountDelta;
	}
	
	protected void assertNoUserPrimaryDelta(LensContext<UserType, AccountShadowType> context) {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		if (userPrimaryDelta == null) {
			return;
		}
		assertTrue("User primary delta is not empty", userPrimaryDelta.isEmpty());
	}

	protected void assertUserPrimaryDelta(LensContext<UserType, AccountShadowType> context) {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		assertNotNull("User primary delta is null", userPrimaryDelta);
		assertFalse("User primary delta is empty", userPrimaryDelta.isEmpty());
	}
	
	protected void assertNoUserSecondaryDelta(LensContext<UserType, AccountShadowType> context) throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		if (userSecondaryDelta == null) {
			return;
		}
		assertTrue("User secondary delta is not empty", userSecondaryDelta.isEmpty());
	}

	protected void assertUserSecondaryDelta(LensContext<UserType, AccountShadowType> context) throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertNotNull("User secondary delta is null", userSecondaryDelta);
		assertFalse("User secondary delta is empty", userSecondaryDelta.isEmpty());
	}
	
	protected void assertUserModificationSanity(LensContext<UserType, AccountShadowType> context) throws JAXBException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
	    PrismObject<UserType> userOld = focusContext.getObjectOld();
	    if (userOld == null) {
	    	return;
	    }
	    ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
	    if (userPrimaryDelta != null) {
		    assertEquals("No OID in userOld", userOld.getOid(), userPrimaryDelta.getOid());
		    assertEquals(ChangeType.MODIFY, userPrimaryDelta.getChangeType());
		    assertNull(userPrimaryDelta.getObjectToAdd());
		    for (ItemDelta itemMod : userPrimaryDelta.getModifications()) {
		        if (itemMod.getValuesToDelete() != null) {
		            Item property = userOld.findItem(itemMod.getPath());
		            assertNotNull("Deleted item " + itemMod.getParentPath() + "/" + itemMod.getName() + " not found in user", property);
		            for (Object valueToDelete : itemMod.getValuesToDelete()) {
		                if (!property.containsRealValue((PrismValue) valueToDelete)) {
		                    display("Deleted value " + valueToDelete + " is not in user item " + itemMod.getParentPath() + "/" + itemMod.getName());
		                    display("Deleted value", valueToDelete);
		                    display("HASHCODE: " + valueToDelete.hashCode());
		                    for (Object value : property.getValues()) {
		                        display("Existing value", value);
		                        display("EQUALS: " + valueToDelete.equals(value));
		                        display("HASHCODE: " + value.hashCode());
		                    }
		                    AssertJUnit.fail("Deleted value " + valueToDelete + " is not in user item " + itemMod.getParentPath() + "/" + itemMod.getName());
		                }
		            }
		        }
		
		    }
	    }
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
	
	protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName) {
		assertEquals("Wrong jack OID (prism)", oid, user.getOid());
		UserType userType = user.asObjectable();
		assertEquals("Wrong jack OID (jaxb)", oid, userType.getOid());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" name", name, userType.getName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" fullName", fullName, userType.getFullName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" givenName", givenName, userType.getGivenName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" familyName", familyName, userType.getFamilyName());
	}
	
	protected void assertUserProperty(String userOid, QName propertyName, Object... expectedPropValues) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("getObject");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertUserProperty(user, propertyName, expectedPropValues);
	}
	
	protected void assertUserProperty(PrismObject<UserType> user, QName propertyName, Object... expectedPropValues) {
		PrismProperty<Object> property = user.findProperty(propertyName);
		assert property != null : "No property "+propertyName+" in "+user;  
		PrismAsserts.assertPropertyValue(property, expectedPropValues);
	}
	
	protected void assertDummyShadowRepo(PrismObject<AccountShadowType> accountShadow, String oid, String username) {
		assertDummyCommon(accountShadow, oid, username);
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES);
		List<Item<?>> attributes = attributesContainer.getValue().getItems();
		assertEquals("Unexpected number of attributes in repo shadow", 2, attributes.size());
	}	
	
	protected void assertDummyShadowModel(PrismObject<AccountShadowType> accountShadow, String oid, String username, String fullname) {
		assertDummyCommon(accountShadow, oid, username);
		IntegrationTestTools.assertProvisioningAccountShadow(accountShadow, resourceDummyType, RefinedAttributeDefinition.class);
	}

	private void assertDummyCommon(PrismObject<AccountShadowType> accountShadow, String oid, String username) {
		assertEquals("Account shadow OID mismatch (prism)", oid, accountShadow.getOid());
		AccountShadowType accountShadowType = accountShadow.asObjectable();
		assertEquals("Account shadow OID mismatch (jaxb)", oid, accountShadowType.getOid());
		assertEquals("Account shadow objectclass", new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"), accountShadowType.getObjectClass());
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES);
		assertNotNull("Null attributes in shadow for "+username, attributesContainer);
		assertFalse("Empty attributes in shadow for "+username, attributesContainer.isEmpty());
		// TODO: assert name and UID
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
	
	protected void assertLinked(String userOid, String accountOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinked");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
		boolean found = false; 
		for (PrismReferenceValue val: accountRef.getValues()) {
			if (val.getOid().equals(accountOid)) {
				found = true;
			}
		}
		assertTrue("User "+userOid+" has not linked to account "+accountOid, found);
	}
	
	protected void assertAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		String accountOid = getUserAccountRef(user, resourceOid);
		assertNotNull("User "+user+" has no account on resource "+resourceOid, accountOid);
	}
	
	protected void assertAccounts(String userOid, int numAccounts) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertAccounts");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAccounts(user, numAccounts);
	}
	
	protected void assertAccounts(PrismObject<UserType> user, int numAccounts) throws ObjectNotFoundException, SchemaException {
		PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
		if (accountRef == null) {
			assert numAccounts == 0 : "Expected "+numAccounts+" but "+user+" has no accountRef";
			return;
		}
		assertEquals("Wrong number of accounts linked to "+user, numAccounts, accountRef.size());
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
	
	protected ObjectDelta<UserType> createModifyUserReplaceDelta(String userOid, QName propertyName, Object... newRealValue) {
		return createModifyUserReplaceDelta(userOid, new ItemPath(propertyName), newRealValue);
	}
	
	protected ObjectDelta<UserType> createModifyUserReplaceDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationReplaceProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
	}
	
	protected ObjectDelta<UserType> createModifyUserAddDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationAddProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
	}
	
	protected ObjectDelta<AccountShadowType> createModifyAccountShadowReplaceDelta(String accountOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationReplaceProperty(AccountShadowType.class, accountOid, propertyName, prismContext, newRealValue);
	}
	
	protected ObjectDelta<AccountShadowType> createModifyAccountShadowAddDelta(String accountOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationAddProperty(AccountShadowType.class, accountOid, propertyName, prismContext, newRealValue);
	}
	
	protected void modifyUserReplace(String userOid, QName propertyName, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserReplace(userOid, new ItemPath(propertyName), task, result, newRealValue);
	}
	
	protected void modifyUserReplace(String userOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(userOid, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected void modifyAccountShadowReplace(String accountOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<AccountShadowType> objectDelta = createModifyAccountShadowReplaceDelta(accountOid, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected void assignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, task, true, result);
	}
	
	protected void unassignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
	SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, task, false, result);
	}
	
	protected void assignOrg(String userOid, String orgOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, task, true, result);
	}

	protected void unassignOrg(String userOid, String orgOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, task, false, result);
	}
	
	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, Task task, boolean add, OperationResult result) 
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, refType, add);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);;
		modelService.executeChanges(deltas, null, task, result);		
	}
	
	protected ContainerDelta<AssignmentType> createAssignmentModification(String roleOid, QName refType, boolean add) throws SchemaException {
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(getUserDefinition(), UserType.F_ASSIGNMENT);
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>();
		if (add) {
			assignmentDelta.addValueToAdd(cval);
		} else {
			assignmentDelta.addValueToDelete(cval);
		}
		PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
		targetRef.getValue().setOid(roleOid);
		targetRef.getValue().setTargetType(refType);
		return assignmentDelta;
	}
	
	protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, boolean add) throws SchemaException {
		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
		modifications.add((createAssignmentModification(roleOid, refType, add)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		return userDelta;
	}
	
	protected ContainerDelta<AssignmentType> createAccountAssignmentModification(String resourceOid, String intent, boolean add) throws SchemaException {
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(getUserDefinition(), UserType.F_ASSIGNMENT);
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>();
		if (add) {
			assignmentDelta.addValueToAdd(cval);
		} else {
			assignmentDelta.addValueToDelete(cval);
		}
		AssignmentType assignmentType = cval.asContainerable();
		AccountConstructionType accountConstructionType = new AccountConstructionType();
		assignmentType.setAccountConstruction(accountConstructionType);
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resourceOid);
		accountConstructionType.setResourceRef(resourceRef);
		accountConstructionType.setIntent(intent);
		return assignmentDelta;
	}
	
	protected ObjectDelta<UserType> createAccountAssignmentUserDelta(String userOid, String resourceOid, String intent, boolean add) throws SchemaException {
		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
		modifications.add((createAccountAssignmentModification(resourceOid, intent, add)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		return userDelta;
	}
	
	protected void assignAccount(String userOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(userOid, resourceOid, intent, true);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void unassignAccount(String userOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(userOid, resourceOid, intent, false);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected PrismObject<UserType> getUser(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getUser");
        OperationResult result = task.getResult();
		PrismObject<UserType> user = modelService.getObject(UserType.class, userOid, null, task, result);
		result.computeStatus();
		IntegrationTestTools.assertSuccess("getObject(User) result not success", result);
		return user;
	}
	
	protected PrismObject<UserType> findUserByUsername(String username) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".findUserByUsername");
        OperationResult result = task.getResult();
        ObjectQuery query = QueryUtil.createNameQuery(PrismTestUtil.createPolyString(username), prismContext);
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);
		if (users.isEmpty()) {
			return null;
		}
		assert users.size() == 1 : "Too many users found for username "+username+": "+users;
		return users.iterator().next();
	}
	
	protected PrismObject<AccountShadowType> getAccount(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		return getAccount(accountOid, false);
	}
	
	protected PrismObject<AccountShadowType> getAccountNoFetch(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		return getAccount(accountOid, true);
	}
	
	protected PrismObject<AccountShadowType> getAccount(String accountOid, boolean noFetch) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getAccount");
        OperationResult result = task.getResult();
		Collection<ObjectOperationOptions> opts = null;
		if (noFetch) {
			opts = ObjectOperationOptions.createCollectionRoot(ObjectOperationOption.NO_FETCH);
		}
		PrismObject<AccountShadowType> account = modelService.getObject(AccountShadowType.class, accountOid, opts , task, result);
		result.computeStatus();
		IntegrationTestTools.assertSuccess("getObject(Account) result not success", result);
		return account;
	}
	
	protected String getSingleUserAccountRef(PrismObject<UserType> user) {
        UserType userType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userType.getAccountRef().size());
        ObjectReferenceType accountRefType = userType.getAccountRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        return accountOid;
	}
	
	protected String getUserAccountRef(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
        UserType userType = user.asObjectable();
        for (ObjectReferenceType accountRefType: userType.getAccountRef()) {
        	String accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
	        PrismObject<AccountShadowType> account = getAccountNoFetch(accountOid);
	        if (resourceOid.equals(account.asObjectable().getResourceRef().getOid())) {
	        	return accountOid;
	        }
        }
        AssertJUnit.fail("Account for resource "+resourceOid+" not found in "+user);
        return null; // Never reached. But compiler complains about missing return 
	}
	
	protected void assertUserNoAccountRefs(PrismObject<UserType> user) {
		UserType userJackType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getAccountRef().size());
	}
	
	protected void assertNoAccountShadow(String accountOid) throws SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".assertNoAccountShadow");
		// Check is shadow is gone
        try {
        	PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        	AssertJUnit.fail("Shadow "+accountOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }
	}
	
	protected void assertAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedRole(user, roleOid);
	}
	
	protected void assertAssignedRole(PrismObject<UserType> user, String roleOid) {
		MidPointAsserts.assertAssignedRole(user, roleOid);
	}

	protected void assertAssignedOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedOrg(user, orgOid);
	}
	
	protected void assertAssignedOrg(PrismObject<UserType> user, String orgOid) {
		MidPointAsserts.assertAssignedOrg(user, orgOid);
	}
	
	protected void assertHasOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedOrg(user, orgOid);
	}
	
	protected void assertHasOrg(PrismObject<UserType> user, String orgOid) {
		MidPointAsserts.assertHasOrg(user, orgOid);
	}
	
	protected void assertHasNoOrg(PrismObject<UserType> user) {
		MidPointAsserts.assertHasNoOrg(user);
	}

	protected void assertAssignments(PrismObject<UserType> user, int expectedNumber) {
		MidPointAsserts.assertAssignments(user, expectedNumber);
	}
	
	protected void assertAssigned(PrismObject<UserType> user, String targetOid, QName refType) {
		MidPointAsserts.assertAssigned(user, targetOid, refType);
	}
	
	protected void assertAssignedNoOrg(String userOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedNoOrg(user);
	}
	
	protected void assertAssignedNoOrg(PrismObject<UserType> user) {
		assertAssignedNo(user, OrgType.COMPLEX_TYPE);
	}
	
	protected void assertAssignedNoRole(String userOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedNoRole(user);
	}
	
	protected void assertAssignedNoRole(PrismObject<UserType> user) {
		assertAssignedNo(user, RoleType.COMPLEX_TYPE);
	}
		
	protected void assertAssignedNo(PrismObject<UserType> user, QName refType) {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					AssertJUnit.fail(user+" has role "+targetRef.getOid()+" while expected no roles");
				}
			}
		}
	}
	
	protected void assertAssignedAccount(String userOid, String resourceOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedAccount(user, resourceOid);
	}
	
	protected void assertAssignedAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			AccountConstructionType accountConstruction = assignmentType.getAccountConstruction();
			if (accountConstruction != null) {
				if (resourceOid.equals(accountConstruction.getResourceRef().getOid())) {
					return;
				}
			}
		}
		AssertJUnit.fail(user.toString()+" does not have account assignment for resource "+resourceOid);
	}
	
	protected void assertAssignedNoAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			AccountConstructionType accountConstruction = assignmentType.getAccountConstruction();
			if (accountConstruction != null) {
				if (resourceOid.equals(accountConstruction.getResourceRef().getOid())) {
					AssertJUnit.fail(user.toString()+" has account assignment for resource "+resourceOid+" while expecting no such assignment");
				}
			}
		}
	}

	protected PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}
	
	protected void applySyncSettings(AccountSynchronizationSettingsType syncSettings)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObjectDefinition<SystemConfigurationType> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class);

		Collection<? extends ItemDelta> modifications = PropertyDelta
				.createModificationReplacePropertyCollection(
						SchemaConstants.C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS,
						objectDefinition, syncSettings);

		OperationResult result = new OperationResult("Aplying sync settings");

		repositoryService.modifyObject(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, result);
		display("Aplying sync settings result", result);
		result.computeStatus();
		assertSuccess("Aplying sync settings failed (result)", result);
	}
	
	protected SystemConfigurationType getSystemConfiguration() throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName()+".getSystemConfiguration");
		PrismObject<SystemConfigurationType> sysConf = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);
		result.computeStatus();
		assertSuccess("getObject(systemConfig) not success", result);
		return sysConf.asObjectable();
	}
	
	protected void assumeAssignmentPolicy(AssignmentPolicyEnforcementType policy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		SystemConfigurationType systemConfiguration = getSystemConfiguration();
		AssignmentPolicyEnforcementType currentPolicy = getAssignmentPolicyEnforcementType(systemConfiguration);
		if (currentPolicy == policy) {
			return;
		}
		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        applySyncSettings(syncSettings);
	}
	
	protected AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType(SystemConfigurationType systemConfiguration) {
		AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfiguration.getGlobalAccountSynchronizationSettings();
		if (globalAccountSynchronizationSettings == null) {
			return null;
		}
		return globalAccountSynchronizationSettings.getAssignmentPolicyEnforcement();
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
	
	protected ItemPath getIcfsNameAttributePath() {
		return new ItemPath(
				ResourceObjectShadowType.F_ATTRIBUTES,
				SchemaTestConstants.ICFS_NAME);
		
	}
	
	protected void assertResolvedResourceRefs(ModelContext<UserType,AccountShadowType> context) {
		for (ModelProjectionContext<AccountShadowType> projectionContext: context.getProjectionContexts()) {
			assertResolvedResourceRefs(projectionContext.getObjectOld(), "objectOld in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getObjectNew(), "objectNew in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getPrimaryDelta(), "primaryDelta in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getSecondaryDelta(), "secondaryDelta in "+projectionContext);
		}
	}

	private void assertResolvedResourceRefs(ObjectDelta<AccountShadowType> delta, String desc) {
		if (delta == null) {
			return;
		}
		if (delta.isAdd()) {
			assertResolvedResourceRefs(delta.getObjectToAdd(), desc);
		} else if (delta.isModify()) {
			ReferenceDelta referenceDelta = delta.findReferenceModification(ResourceObjectShadowType.F_RESOURCE_REF);
			if (referenceDelta != null) {
				assertResolvedResourceRefs(referenceDelta.getValuesToAdd(), "valuesToAdd in "+desc);
				assertResolvedResourceRefs(referenceDelta.getValuesToDelete(), "valuesToDelete in "+desc);
				assertResolvedResourceRefs(referenceDelta.getValuesToReplace(), "valuesToReplace in "+desc);
			}
		}
	}

	private void assertResolvedResourceRefs(PrismObject<AccountShadowType> shadow, String desc) {
		if (shadow == null) {
			return;
		}
		PrismReference resourceRef = shadow.findReference(ResourceObjectShadowType.F_RESOURCE_REF);
		if (resourceRef == null) {
			AssertJUnit.fail("No resourceRef in "+desc);
		}
		assertResolvedResourceRefs(resourceRef.getValues(), desc);
	}

	private void assertResolvedResourceRefs(Collection<PrismReferenceValue> values, String desc) {
		if (values == null) {
			return;
		}
		for (PrismReferenceValue pval: values) {
			assertNotNull("resourceRef in "+desc+" does not contain object", pval.getObject());
		}
	}
	
	/**
	 * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
	 * existing user values. 
	 */
	protected void breakAssignmentDelta(LensContext<UserType, AccountShadowType> context) throws SchemaException {
        LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        breakAssignmentDelta(userPrimaryDelta);		
	}
	
	/**
	 * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
	 * existing user values. 
	 */
	protected void breakAssignmentDelta(Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
		breakAssignmentDelta((ObjectDelta<UserType>)deltas.iterator().next());
	}
	
	/**
	 * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
	 * existing user values. 
	 */
	protected void breakAssignmentDelta(ObjectDelta<UserType> userDelta) throws SchemaException {
        ContainerDelta<?> assignmentDelta = userDelta.findContainerDelta(UserType.F_ASSIGNMENT);
        PrismContainerValue<?> assignmentDeltaValue = null;
        if (assignmentDelta.getValuesToAdd() != null) {
        	assignmentDeltaValue = assignmentDelta.getValuesToAdd().iterator().next();
        }
        if (assignmentDelta.getValuesToDelete() != null) {
        	assignmentDeltaValue = assignmentDelta.getValuesToDelete().iterator().next();
        }
        PrismContainer<ActivationType> activationContainer = assignmentDeltaValue.findOrCreateContainer(AssignmentType.F_ACTIVATION);
        PrismContainerValue<ActivationType> emptyValue = new PrismContainerValue<ActivationType>();
		activationContainer.add(emptyValue);		
	}
	
	protected Task createTask(String operationName) {
		Task task = taskManager.createTaskInstance(operationName);
		task.setOwner(userAdministrator);
		return task;
	}
	
	protected void waitForTaskFinish(Task task, boolean checkSubresult) throws Exception {
		waitForTaskFinish(task, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskFinish(final Task task, final boolean checkSubresult, int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws Exception {
				task.refresh(waitResult);
//				Task freshTask = taskManager.getTask(task.getOid(), waitResult);
				OperationResult result = task.getResult();
//				display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+task+": "+IntegrationTestTools.getErrorMessage(result);
				assert !isUknown(result, checkSubresult) : "Unknown result in "+task+": "+IntegrationTestTools.getErrorMessage(result);
				return !isInProgress(result, checkSubresult);
			}
			@Override
			public void timeout() {
				try {
					task.refresh(waitResult);
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
				OperationResult result = task.getResult();
				LOGGER.debug("Result of timed-out task:\n{}", result.dump());
				assert false : "Timeout while waiting for "+task+" to finish. Last result "+result;
			}
		};
		IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker , timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected void waitForTaskFinish(String taskOid, boolean checkSubresult) throws Exception {
		waitForTaskFinish(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskFinish(final String taskOid, final boolean checkSubresult, int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws Exception {
				Task freshTask = taskManager.getTask(taskOid, waitResult);
				OperationResult result = freshTask.getResult();
				display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+freshTask+": "+IntegrationTestTools.getErrorMessage(result);
				if (isUknown(result, checkSubresult)) {
					return false;
				}
//				assert !isUknown(result, checkSubresult) : "Unknown result in "+freshTask+": "+IntegrationTestTools.getErrorMessage(result);
				return !isInProgress(result, checkSubresult);
			}
			@Override
			public void timeout() {
				try {
					Task freshTask = taskManager.getTask(taskOid, waitResult);
					OperationResult result = freshTask.getResult();
					LOGGER.debug("Result of timed-out task:\n{}", result.dump());
					assert false : "Timeout while waiting for "+freshTask+" to finish. Last result "+result;
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task "+taskOid+" finish", checker , timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	private boolean isError(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult.isError();
	}
	
	private boolean isUknown(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult.isUnknown();
	}

	private boolean isInProgress(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult.isInProgress();
	}

	private OperationResult getSubresult(OperationResult result, boolean checkSubresult) {
		if (checkSubresult) {
			return result.getLastSubresult();
		}
		return result;
	}

}
