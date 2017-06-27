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
package com.evolveum.midpoint.model.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.impl.util.mock.MockClockworkHook;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class AbstractInternalModelIntegrationTest extends AbstractModelImplementationIntegrationTest {
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	public static final File SECURITY_POLICY_FILE = new File(COMMON_DIR, "security-policy.xml");
	public static final String SECURITY_POLICY_OID = "28bf845a-b107-11e3-85bc-001e8c717e5b";
	
	public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_NAME = "administrator";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	
	protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";
	protected static final String USER_JACK_PASSWORD = "deadmentellnotales";
	
	protected static final File USER_BARBOSSA_FILE = new File(COMMON_DIR, "user-barbossa.xml");
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	
	protected static final File USER_GUYBRUSH_FILE = new File(COMMON_DIR, "user-guybrush.xml");
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
	
	static final File USER_ELAINE_FILE = new File(COMMON_DIR, "user-elaine.xml");
	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_ELAINE_USERNAME = "elaine";
	
	// Largo does not have a full name set, employeeType=PIRATE
	protected static final File USER_LARGO_FILE = new File(COMMON_DIR,  "user-largo.xml");
	protected static final String USER_LARGO_OID = "c0c010c0-d34d-b33f-f00d-111111111118";
	
	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";
	
	protected static final File ACCOUNT_HBARBOSSA_DUMMY_FILE = new File(COMMON_DIR, "account-hbarbossa-dummy.xml");
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-222211111112";
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_USERNAME = "hbarbossa";

	public static final File ACCOUNT_SHADOW_JACK_DUMMY_FILE = new File(COMMON_DIR, "account-shadow-jack-dummy.xml");
	public static final String ACCOUNT_JACK_DUMMY_USERNAME = "jack";
	
	public static final File ACCOUNT_HERMAN_DUMMY_FILE = new File(COMMON_DIR, "account-herman-dummy.xml");
	public static final String ACCOUNT_HERMAN_DUMMY_OID = "22220000-2200-0000-0000-444400004444";
	public static final String ACCOUNT_HERMAN_DUMMY_USERNAME = "ht";
	
	public static final File ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILE = new File(COMMON_DIR, "account-shadow-guybrush-dummy.xml");
	public static final String ACCOUNT_SHADOW_GUYBRUSH_OID = "22226666-2200-6666-6666-444400004444";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_FULLNAME = "Guybrush Threepwood";
	public static final File ACCOUNT_GUYBRUSH_DUMMY_FILE = new File(COMMON_DIR, "account-guybrush-dummy.xml");
	
	public static final File ACCOUNT_SHADOW_ELAINE_DUMMY_FILE = new File(COMMON_DIR, "account-elaine-dummy.xml");
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-22220004000e";
	public static final String ACCOUNT_ELAINE_DUMMY_USERNAME = USER_ELAINE_USERNAME;
	
	public static final File ENTITLEMENT_SHADOW_PIRATE_DUMMY_FILE = new File(COMMON_DIR, "entitlement-shadow-pirate-dummy.xml");
	public static final String ENTITLEMENT_PIRATE_DUMMY_NAME = "pirate";

	public static final File ACCOUNT_SHADOW_CALYPSO_DUMMY_FILE = new File(COMMON_DIR, "account-shadow-calypso-dummy.xml");
	public static final String ACCOUNT_CALYPSO_DUMMY_USERNAME = "calypso";
	
	public static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
	public static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	public static final String RESOURCE_DUMMY_NAMESPACE = MidPointConstants.NS_RI;
	public static final QName RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass");
	public static final QName RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, "CustomprivilegeObjectClass");
	
	public static final File USER_TEMPLATE_FILE = new File(COMMON_DIR, "user-template.xml");
	public static final String USER_TEMPLATE_OID = "10000000-0000-0000-0000-000000000002";
	
	protected static final File PASSWORD_POLICY_GLOBAL_FILE = new File(COMMON_DIR, "password-policy-global.xml");
	protected static final String PASSWORD_POLICY_GLOBAL_OID = "12344321-0000-0000-0000-000000000003";
	
	protected static final String MOCK_CLOCKWORK_HOOK_URL = MidPointConstants.NS_MIDPOINT_TEST_PREFIX + "/mockClockworkHook";
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);

	protected PrismObject<UserType> userAdministrator;
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected UserType userTypeGuybrush;
	protected UserType userTypeElaine;
		
	protected MockClockworkHook mockClockworkHook;
			
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		// We want logging config from logback-test.xml and not from system config object
		InternalsConfig.setAvoidLoggingChange(true);
		
		mockClockworkHook = new MockClockworkHook();
		hookRegistry.registerChangeHook(MOCK_CLOCKWORK_HOOK_URL, mockClockworkHook);
		
		modelService.postInit(initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
				
		// Administrator
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
		login(userAdministrator);
		
		// User Templates
		repoAddObjectFromFile(USER_TEMPLATE_FILE, initResult);

		// Resources
		
		initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
				
		// We need to create Barbossa's account in exactly the shape that is given by his existing assignments
		// otherwise any substantial change will trigger reconciliation and the recon changes will interfere with
		// the tests
		DummyAccount account = new DummyAccount(ACCOUNT_HBARBOSSA_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Hector Barbossa");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Caribbean");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Pirate Brethren, Inc.");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Undead Monkey");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Arr!");
		getDummyResource().addAccount(account);
		
		getDummyResourceController().addAccount(ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", "Monkey Island");
		getDummyResourceController().addAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Melee Island");
				
		// Accounts
		repoAddObjectFromFile(ACCOUNT_HBARBOSSA_DUMMY_FILE, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILE, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_FILE, initResult);
		
		// Users
		userTypeJack = repoAddObjectFromFile(USER_JACK_FILE, UserType.class, initResult).asObjectable();
		userTypeBarbossa = repoAddObjectFromFile(USER_BARBOSSA_FILE, UserType.class, initResult).asObjectable();
		userTypeGuybrush = repoAddObjectFromFile(USER_GUYBRUSH_FILE, UserType.class, initResult).asObjectable();
		userTypeElaine = repoAddObjectFromFile(USER_ELAINE_FILE, UserType.class, initResult).asObjectable();
				
	}

}
