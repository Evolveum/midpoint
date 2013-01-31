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
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.testng.IHookCallBack;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

import javax.xml.namespace.QName;

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
	
	// Assigns dummy resource, sets some attributes
	protected static final String ROLE_PIRATE_FILENAME = COMMON_DIR_NAME + "/role-pirate.xml";
	protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";

	// Excludes role "pirate"
	protected static final String ROLE_JUDGE_FILENAME = COMMON_DIR_NAME + "/role-judge.xml";
	protected static final String ROLE_JUDGE_OID = "12345111-1111-2222-1111-121212111111";
	
	// Assigns default dummy resource and red dummy resource
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
	
	// Elaine has account on the dummy resources (default, red, blue)
	// The accounts are also assigned
	static final String USER_ELAINE_FILENAME = COMMON_DIR_NAME + "/user-elaine.xml";
	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_ELAINE_USERNAME = "elaine";
	
	// Captain Kate Capsize does not exist in the repo. This user is designed to be added. 
	// She has account on dummy resources (default, red, blue)
	// The accounts are also assigned
	static final String USER_CAPSIZE_FILENAME = COMMON_DIR_NAME + "/user-capsize.xml";
	protected static final String USER_CAPSIZE_OID = "c0c010c0-d34d-b33f-f00d-11c1c1c1c11c";
	protected static final String USER_CAPSIZE_USERNAME = "capsize";
	
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
	
	protected static final String ORG_MONKEY_ISLAND_FILENAME = COMMON_DIR_NAME + "/org-monkey-island.xml";
	protected static final String ORG_GOVERNOR_OFFICE_OID = "00000000-8888-6666-0000-100000000001";
	protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
	protected static final String ORG_MINISTRY_OF_OFFENSE_OID = "00000000-8888-6666-0000-100000000003";
	protected static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";
	protected static final String ORG_SAVE_ELAINE_OID = "00000000-8888-6666-0000-200000000001";
	
	protected static final String TASK_RECONCILE_DUMMY_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy.xml";
	protected static final String TASK_RECONCILE_DUMMY_OID = "10000000-0000-0000-5656-565600000004";
	
	protected static final String TASK_RECONCILE_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy-blue.xml";
	protected static final String TASK_RECONCILE_DUMMY_BLUE_OID = "10000000-0000-0000-5656-565600000204";
	
	protected static final String TASK_RECONCILE_DUMMY_GREEN_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy-green.xml";
	protected static final String TASK_RECONCILE_DUMMY_GREEN_OID = "10000000-0000-0000-5656-565600000404";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_FILENAME = COMMON_DIR_NAME + "/task-dumy-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_OID = "10000000-0000-0000-5555-555500000004";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/task-dumy-blue-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_OID = "10000000-0000-0000-5555-555500000204";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_FILENAME = COMMON_DIR_NAME + "/task-dumy-green-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_OID = "10000000-0000-0000-5555-555500000404";
	
	protected static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
	protected static final QName PIRACY_SHIP = new QName(NS_PIRACY, "ship");
	protected static final QName PIRACY_TALES = new QName(NS_PIRACY, "tales");
	protected static final QName PIRACY_WEAPON = new QName(NS_PIRACY, "weapon");
	protected static final QName PIRACY_LOOT = new QName(NS_PIRACY, "loot");
	protected static final QName PIRACY_BAD_LUCK = new QName(NS_PIRACY, "badLuck");
	
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
    	
	protected Task createTask(String operationName) {
		Task task = taskManager.createTaskInstance(operationName);
		task.setOwner(userAdministrator);
		return task;
	}
	

    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> run start");
        super.run(callBack, testResult);
        LOGGER.info("###>>> run end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @AfterClass
    @Override
    protected void springTestContextAfterTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestClass start");
        super.springTestContextAfterTestClass();
        LOGGER.info("###>>> springTestContextAfterTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @AfterMethod
    @Override
    protected void springTestContextAfterTestMethod(Method testMethod) throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestMethod start");
        super.springTestContextAfterTestMethod(testMethod);
        LOGGER.info("###>>> springTestContextAfterTestMethod end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeClass
    @Override
    protected void springTestContextBeforeTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextBeforeTestClass start");
        super.springTestContextBeforeTestClass();
        LOGGER.info("###>>> springTestContextBeforeTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeMethod
    @Override
    protected void springTestContextBeforeTestMethod(Method testMethod) throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextBeforeTestMethod start");
        super.springTestContextBeforeTestMethod(testMethod);
        LOGGER.info("###>>> springTestContextBeforeTestMethod end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeClass
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextPrepareTestInstance start");
        super.springTestContextPrepareTestInstance();
        LOGGER.info("###>>> springTestContextPrepareTestInstance end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }
}
