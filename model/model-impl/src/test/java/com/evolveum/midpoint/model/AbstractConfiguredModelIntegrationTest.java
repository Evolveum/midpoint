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
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
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
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
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
public class AbstractConfiguredModelIntegrationTest extends AbstractModelIntegrationTest {
			
	public static final String SYSTEM_CONFIGURATION_FILENAME = COMMON_DIR_NAME + "/system-configuration.xml";
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final String USER_ADMINISTRATOR_FILENAME = COMMON_DIR_NAME + "/user-administrator.xml";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
		
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
	
	// White dummy resource has almost no configuration: no schema, no schemahandling, no synchronization, ...
	protected static final String RESOURCE_DUMMY_WHITE_FILENAME = COMMON_DIR_NAME + "/resource-dummy-white.xml";
	protected static final String RESOURCE_DUMMY_WHITE_OID = "10000000-0000-0000-0000-000000000304";
	protected static final String RESOURCE_DUMMY_WHITE_NAME = "white";
	protected static final String RESOURCE_DUMMY_WHITE_NAMESPACE = MidPointConstants.NS_RI;
	
	// Green dummy resource is authoritative
	protected static final String RESOURCE_DUMMY_GREEN_FILENAME = COMMON_DIR_NAME + "/resource-dummy-green.xml";
	protected static final String RESOURCE_DUMMY_GREEN_OID = "10000000-0000-0000-0000-000000000404";
	protected static final String RESOURCE_DUMMY_GREEN_NAME = "green";
	protected static final String RESOURCE_DUMMY_GREEN_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final String RESOURCE_DUMMY_SCHEMALESS_FILENAME = COMMON_DIR_NAME + "/resource-dummy-schemaless-no-schema.xml";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0000";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAME = "schemaless";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final String RESOURCE_DUMMY_FAKE_FILENAME = COMMON_DIR_NAME + "/resource-dummy-fake.xml";
	protected static final String RESOURCE_DUMMY_FAKE_OID = "10000000-0000-0000-0000-00000000000f";

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
	
	// Elaine has account on all the resources where she can have an account
	// The accounts are also assigned
	static final String USER_ELAINE_FILENAME = COMMON_DIR_NAME + "/user-elaine.xml";
	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_ELAINE_USERNAME = "elaine";
	
	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-hbarbossa-opendj.xml";
	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_OID = "c0c010c0-d34d-b33f-f00d-222211111112";
	
	public static final String ACCOUNT_JACK_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-jack-dummy.xml";
	public static final String ACCOUNT_JACK_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/account-jack-dummy-red.xml";
	public static final String ACCOUNT_JACK_DUMMY_USERNAME = "jack";
	
	public static final String ACCOUNT_HERMAN_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-herman-dummy.xml";
	public static final String ACCOUNT_HERMAN_DUMMY_OID = "22220000-2200-0000-0000-444400004444";
	public static final String ACCOUNT_HERMAN_DUMMY_USERNAME = "ht";
	
	public static final String ACCOUNT_HERMAN_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-herman-opendj.xml";
	public static final String ACCOUNT_HERMAN_OPENDJ_OID = "22220000-2200-0000-0000-333300003333";
	
	public static final String ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-guybrush-dummy.xml";
	public static final String ACCOUNT_SHADOW_GUYBRUSH_OID = "22226666-2200-6666-6666-444400004444";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-guybrush-dummy.xml";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/account-guybrush-dummy-red.xml";
	
	public static final String ACCOUNT_SHADOW_JACK_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-jack-dummy.xml";
	
	public static final String ACCOUNT_DAVIEJONES_DUMMY_USERNAME = "daviejones";
	public static final String ACCOUNT_CALYPSO_DUMMY_USERNAME = "calypso";
	
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-elaine-dummy.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-22220004000e";
	public static final String ACCOUNT_ELAINE_DUMMY_USERNAME = USER_ELAINE_USERNAME;
	
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/account-elaine-dummy-red.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID = "c0c010c0-d34d-b33f-f00d-22220104000e";
	public static final String ACCOUNT_ELAINE_DUMMY_RED_USERNAME = USER_ELAINE_USERNAME;

	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/account-elaine-dummy-blue.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID = "c0c010c0-d34d-b33f-f00d-22220204000e";
	public static final String ACCOUNT_ELAINE_DUMMY_BLUE_USERNAME = USER_ELAINE_USERNAME;
	
	protected static final String PASSWORD_POLICY_GLOBAL_FILENAME = COMMON_DIR_NAME + "/password-policy-global.xml";
	protected static final String PASSWORD_POLICY_GLOBAL_OID = "12344321-0000-0000-0000-000000000003";
	
	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME = "fullname";
	protected static final QName DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
	protected static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH = new ItemPath(
			AccountShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME);

	protected static final String ORG_MONKEY_ISLAND_FILENAME = COMMON_DIR_NAME + "/org-monkey-island.xml";
	protected static final String ORG_GOVERNOR_OFFICE_OID = "00000000-8888-6666-0000-100000000001";
	protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
	protected static final String ORG_MINISTRY_OF_OFFENSE_OID = "00000000-8888-6666-0000-100000000003";
	protected static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";
	protected static final String ORG_SAVE_ELAINE_OID = "00000000-8888-6666-0000-200000000001";
	
	protected static final String TASK_RECONCILE_DUMMY_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy.xml";
	protected static final String TASK_RECONCILE_DUMMY_OID = "91919191-76e0-59e2-86d6-3d4f02d3dddd";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_FILENAME = COMMON_DIR_NAME + "/task-dumy-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_OID = "10000000-0000-0000-5555-555500000004";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/task-dumy-blue-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_OID = "10000000-0000-0000-5555-555500000204";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_FILENAME = COMMON_DIR_NAME + "/task-dumy-green-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_OID = "10000000-0000-0000-5555-555500000404";
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractConfiguredModelIntegrationTest.class);
	
	protected PrismObject<UserType> userAdministrator;
		
	public AbstractConfiguredModelIntegrationTest() {
		super();
	}

	@Override
	public void initSystem(Task initTask,  OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
				
		// System Configuration
		try {
			addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		// Users
		userAdministrator = addObjectFromFile(USER_ADMINISTRATOR_FILENAME, UserType.class, initResult);
		
	}
		
	protected LensContext<UserType, AccountShadowType> createUserAccountContext() {
		return new LensContext<UserType, AccountShadowType>(UserType.class, AccountShadowType.class, prismContext);
	}
	
	protected LensFocusContext<UserType> fillContextWithUser(LensContext<UserType, AccountShadowType> context, PrismObject<UserType> user) throws SchemaException, ObjectNotFoundException {
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setObjectOld(user);
		return focusContext;
	}
	
	protected LensFocusContext<UserType> fillContextWithUser(LensContext<UserType, AccountShadowType> context, String userOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
        return fillContextWithUser(context, user);
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

	protected LensProjectionContext<AccountShadowType> fillContextWithAccount(LensContext<UserType, AccountShadowType> context, String accountOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<AccountShadowType> account = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        provisioningService.applyDefinition(account, result);
        return fillContextWithAccount(context, account, result);
	}

	protected LensProjectionContext<AccountShadowType> fillContextWithAccountFromFile(LensContext<UserType, AccountShadowType> context, String filename, OperationResult result) throws SchemaException,
	ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(filename));
		provisioningService.applyDefinition(account, result);
		return fillContextWithAccount(context, account, result);
	}

    protected LensProjectionContext<AccountShadowType> fillContextWithAccount(LensContext<UserType, AccountShadowType> context, PrismObject<AccountShadowType> account, OperationResult result) throws SchemaException,
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
		return accountSyncContext;
    }
    
    protected void makeImportSyncDelta(LensProjectionContext<AccountShadowType> accContext) {
    	PrismObject<AccountShadowType> syncAccountToAdd = accContext.getObjectOld().clone();
    	ObjectDelta<AccountShadowType> syncDelta = ObjectDelta.createAddDelta(syncAccountToAdd);
    	accContext.setSyncDelta(syncDelta);
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
		
	protected void addDummyAccount(DummyResource resource, String userId, String fullName) throws com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, SchemaViolationException {
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		resource.addAccount(account);
	}
	
	protected Task createTask(String operationName) {
		Task task = taskManager.createTaskInstance(operationName);
		task.setOwner(userAdministrator);
		return task;
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
}
