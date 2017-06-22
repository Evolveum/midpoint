package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2013-2017 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOrgSync extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "orgsync");

	public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";

	public static final File OBJECT_TEMPLATE_ORG_FILE = new File(TEST_DIR, "object-template-org.xml");
	public static final String OBJECT_TEMPLATE_ORG_OID = "10000000-0000-0000-0000-000000000231";

	public static final File OBJECT_TEMPLATE_ROLE_FILE = new File(TEST_DIR, "object-template-role.xml");
	public static final String OBJECT_TEMPLATE_ROLE_OID = "10000000-0000-0000-0000-000000000241";

	protected static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");
	protected static final String RESOURCE_DUMMY_HR_ID = "HR";
	protected static final String RESOURCE_DUMMY_HR_OID = "10000000-0000-0000-0000-000000000001";
	protected static final String RESOURCE_DUMMY_HR_NAMESPACE = MidPointConstants.NS_RI;

	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	protected static final QName OPENDJ_ASSOCIATION_GROUP_NAME = new QName(RESOURCE_OPENDJ_NAMESPACE,
			"group");

	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME = "firstname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME = "lastname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH = "orgpath";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES = "responsibilities";

	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

	public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	public static final String ROLE_BASIC_OID = "10000000-0000-0000-0000-000000000601";

	public static final File ROLE_META_REPLICATED_ORG_FILE = new File(TEST_DIR,
			"role-meta-replicated-org.xml");
	public static final String ROLE_META_REPLICATED_ORG_OID = "10000000-0000-0000-0000-000000006601";

	public static final File ROLE_META_RESPONSIBILITY_FILE = new File(TEST_DIR,
			"role-meta-responsibility.xml");
	public static final String ROLE_META_RESPONSIBILITY_OID = "10000000-0000-0000-0000-000000006602";

	protected static final File TASK_LIVE_SYNC_DUMMY_HR_FILE = new File(TEST_DIR,
			"task-dumy-hr-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_HR_OID = "10000000-0000-0000-5555-555500000001";

	protected static final File TASK_RECON_OPENDJ_DEFAULT_SINGLE_FILE = new File(TEST_DIR,
			"task-reconcile-opendj-default-single.xml");
	protected static final String TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID = "10000000-0000-0000-5555-555500000004";

	protected static final File TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_FILE = new File(TEST_DIR,
			"task-reconcile-opendj-ldapgroup-single.xml");
	protected static final String TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_OID = "10000000-0000-0000-5555-555500000014";

	private static final String ACCOUNT_HERMAN_USERNAME = "ht";
	private static final String ACCOUNT_HERMAN_FIST_NAME = "Herman";
	private static final String ACCOUNT_HERMAN_LAST_NAME = "Toothrot";

	private static final String ACCOUNT_LEMONHEAD_USERNAME = "lemonhead";
	private static final String ACCOUNT_LEMONHEAD_FIST_NAME = "Lemonhead";
	private static final String ACCOUNT_LEMONHEAD_LAST_NAME = "Canibal";
	private static final String ACCOUNT_LEMONHEAD_DN = "uid=lemonhead,ou=Monkey Island,dc=example,dc=com";

	private static final String ACCOUNT_SHARPTOOTH_USERNAME = "sharptooth";
	private static final String ACCOUNT_SHARPTOOTH_FIST_NAME = "Sharptooth";
	private static final String ACCOUNT_SHARPTOOTH_LAST_NAME = "Canibal";

	private static final String ACCOUNT_REDSKULL_USERNAME = "redskull";
	private static final String ACCOUNT_REDSKULL_FIST_NAME = "Redskull";
	private static final String ACCOUNT_REDSKULL_LAST_NAME = "Canibal";

	private static final String ACCOUNT_GUYBRUSH_USERNAME = "guybrush";
	private static final String ACCOUNT_GUYBRUSH_FIST_NAME = "Guybrush";
	private static final String ACCOUNT_GUYBRUSH_LAST_NAME = "Threepwood";

	private static final String ACCOUNT_MANCOMB_USERNAME = "mancomb";
	private static final String ACCOUNT_MANCOMB_FIST_NAME = "Mancomb";
	private static final String ACCOUNT_MANCOMB_LAST_NAME = "Seepgood";

	private static final String ACCOUNT_COBB_USERNAME = "cobb";
	private static final String ACCOUNT_COBB_FIST_NAME = "Cobb";
	private static final String ACCOUNT_COBB_LAST_NAME = "Loom";

	private static final String ACCOUNT_LARGO_USERNAME = "largo";
	private static final String ACCOUNT_LARGO_FIST_NAME = "Largo";
	private static final String ACCOUNT_LARGO_LAST_NAME = "LaGrande";

	private static final String ACCOUNT_STAN_USERNAME = "stan";
	private static final String ACCOUNT_STAN_FIST_NAME = "Stan";
	private static final String ACCOUNT_STAN_LAST_NAME = "Salesman";

	private static final String ACCOUNT_CAPSIZE_USERNAME = "capsize";
	private static final String ACCOUNT_CAPSIZE_FIST_NAME = "Kate";
	private static final String ACCOUNT_CAPSIZE_LAST_NAME = "Capsize";

	private static final String ACCOUNT_WALLY_USERNAME = "wally";
	private static final String ACCOUNT_WALLY_FIST_NAME = "Wally";
	private static final String ACCOUNT_WALLY_LAST_NAME = "Feed";

	private static final String ACCOUNT_AUGUSTUS_USERNAME = "augustus";
	private static final String ACCOUNT_AUGUSTUS_FIST_NAME = "Augustus";
	private static final String ACCOUNT_AUGUSTUS_LAST_NAME = "DeWaat";

	private static final String ACCOUNT_ROGERSSR_USERNAME = "rogers,sr";
	private static final String ACCOUNT_ROGERSSR_FIST_NAME = "Rum";
	private static final String ACCOUNT_ROGERSSR_LAST_NAME = "Rogers, Sr.";

	private static final String ACCOUNT_TELEKE_USERNAME = "tőlőkë";
	private static final String ACCOUNT_TELEKE_FIST_NAME = "Félix";
	private static final String ACCOUNT_TELEKE_LAST_NAME = "Tőlőkë";

	private static final String ORGPATH_MONKEY_ISLAND = "Monkey Island";
	private static final String ORGPATH_FREELANCE = "Freelance/Ministry of Rum";
	private static final String ORGPATH_SCUMM_BAR = "Scumm Bar/Ministry of Rum";
	private static final String ORGPATH_BRUTE = "Brute Office/Violence Section/Department of Mischief/Ministry of Offense";
	private static final String ORGPATH_MELEE_ISLAND = "Mêlée Island";
	private static final String ORGPATH_DOCKS = "Docks/Mêlée Island";
	private static final String ORGPATH_SCABB_ISLAND = "Scabb Island";
	private static final String ORGPATH_CARTOGRAPHY = "Cartography/Scabb Island";
	private static final String ORGPATH_BOOTY_ISLAND = "Booty Island";
	private static final String ORGPATH_BOOTY_ISLAND_LOOKOUT = "Lookout/Booty Island";
	private static final String ORGPATH_CAPSIZE = "Cruises, Charter and Capsize/Tourist Industries, Tri-Island Area";

	private static final String ORGPATH_KARPATULA = "Karpátulæ";
	private static final String HRAD = "Čórtúv Hrád";
	private static final String ORGPATH_HRAD = HRAD + "/" + ORGPATH_KARPATULA;

	private static final String RESP_CANIBALISM = "canibalism";
	private static final String RESP_CANIBALISM_DN = "cn=R_canibalism,ou=Groups,dc=example,dc=com";

	private static final File SCABB_OU_LDIF_FILE = new File(TEST_DIR, "scabb.ldif");
	private static final File BOOTY_OU_LDIF_FILE = new File(TEST_DIR, "booty.ldif");
	private static final File BOOTY_LOOKOUT_OU_LDIF_FILE = new File(TEST_DIR, "booty-lookout.ldif");
	
	protected static final int TASK_WAIT_TIMEOUT = 40000;

	@Autowired(required = true)
	private ReconciliationTaskHandler reconciliationTaskHandler;

	private DebugReconciliationTaskResultListener reconciliationTaskResultListener;

	protected static DummyResource dummyResourceHr;
	protected static DummyResourceContoller dummyResourceCtlHr;
	protected ResourceType resourceDummyHrType;
	protected PrismObject<ResourceType> resourceDummyHr;

	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;

	private String orgMonkeyIslandOid;
	private String orgMoROid;
	private String orgScummBarOid;

	private String roleCanibalismOid;

	@Override
	protected String getTopOrgOid() {
		return ORG_TOP_OID;
	}
	
	@Override
	protected void startResources() throws Exception {
		openDJController.startCleanServer();
	}

	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		reconciliationTaskResultListener = new DebugReconciliationTaskResultListener();
		reconciliationTaskHandler.setReconciliationTaskResultListener(reconciliationTaskResultListener);

		// Resources
		dummyResourceCtlHr = DummyResourceContoller.create(RESOURCE_DUMMY_HR_ID, resourceDummyHr);
		DummyObjectClass dummyAdAccountObjectClass = dummyResourceCtlHr.getDummyResource()
				.getAccountObjectClass();
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME,
				String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME,
				String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH,
				String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES,
				String.class, false, true);
		dummyResourceHr = dummyResourceCtlHr.getDummyResource();
		resourceDummyHr = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_HR_FILE,
				RESOURCE_DUMMY_HR_OID, initTask, initResult);
		resourceDummyHrType = resourceDummyHr.asObjectable();
		dummyResourceCtlHr.setResource(resourceDummyHr);
		dummyResourceHr.setSyncStyle(DummySyncStyle.SMART);

		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE,
				RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

		// LDAP content
		openDJController.addEntryFromLdifFile(SCABB_OU_LDIF_FILE);
		openDJController.addEntryFromLdifFile(BOOTY_OU_LDIF_FILE);
		openDJController.addEntryFromLdifFile(BOOTY_LOOKOUT_OU_LDIF_FILE);

		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);

		importObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, initResult);
		setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);

		importObjectFromFile(OBJECT_TEMPLATE_ROLE_FILE, initResult);
		setDefaultObjectTemplate(RoleType.COMPLEX_TYPE, OBJECT_TEMPLATE_ROLE_OID);

		// Org
		importObjectFromFile(ORG_TOP_FILE, initResult);

		// Role
		importObjectFromFile(ROLE_BASIC_FILE, initResult);
		importObjectFromFile(ROLE_META_REPLICATED_ORG_FILE, initResult);
		importObjectFromFile(ROLE_META_RESPONSIBILITY_FILE, initResult);

		// Tasks
		importObjectFromFile(TASK_LIVE_SYNC_DUMMY_HR_FILE, initResult);

	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		OperationResult testResultHr = modelService.testResource(RESOURCE_DUMMY_HR_OID, task);
		TestUtil.assertSuccess(testResultHr);

		OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
		TestUtil.assertSuccess(testResultOpenDj);

		waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
		waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
		waitForTaskStart(TASK_LIVE_SYNC_DUMMY_HR_OID, false);

		dumpOrgTree();
	}

	/**
	 * First account on Monkey Island. The Monkey Island org should be created.
	 */
	@Test
	public void test100AddHrAccountHerman() throws Exception {
		final String TEST_NAME = "test100AddHrAccountHerman";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_HERMAN_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_HERMAN_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_HERMAN_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_MONKEY_ISLAND);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_HERMAN_USERNAME);
		assertNotNull("No herman user", user);
		display("User", user);
		assertUserHerman(user);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
		orgMonkeyIslandOid = org.getOid();
		assertAssignedOrg(user, org.getOid());
		assertHasOrg(user, org.getOid());
		assertHasOrg(org, ORG_TOP_OID);

		assertSubOrgs(org, 0);
		assertSubOrgs(ORG_TOP_OID, 1);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Second account on Monkey Island. The existing org should be reused.
	 */
	@Test
	public void test105AddHrAccountLemonhead() throws Exception {
		final String TEST_NAME = "test105AddHrAccountLemonhead";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_LEMONHEAD_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_LEMONHEAD_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_LEMONHEAD_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_MONKEY_ISLAND);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES, RESP_CANIBALISM);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_LEMONHEAD_USERNAME);
		assertNotNull("No lemonhead user", user);
		display("User", user);
		assertUser(user, ACCOUNT_LEMONHEAD_USERNAME, ACCOUNT_LEMONHEAD_FIST_NAME,
				ACCOUNT_LEMONHEAD_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
		assertAssignedOrg(user, org.getOid());
		assertHasOrg(user, org.getOid());
		assertHasOrg(org, ORG_TOP_OID);

		assertSubOrgs(org, 0);
		assertSubOrgs(ORG_TOP_OID, 1);

		assertEquals("Monkey island Org OID has changed", orgMonkeyIslandOid, org.getOid());

		assertBasicRoleAndResources(user);
		roleCanibalismOid = assertResponsibility(user, RESP_CANIBALISM);
		assertAssignments(user, 3);
	}

	/**
	 * Yet another canibal. Same org, same responsibility. Make sure everything
	 * is reused and not created again.
	 */
	@Test
	public void test106AddHrAccountSharptooth() throws Exception {
		final String TEST_NAME = "test106AddHrAccountSharptooth";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_SHARPTOOTH_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_SHARPTOOTH_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_SHARPTOOTH_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_MONKEY_ISLAND);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES, RESP_CANIBALISM);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_SHARPTOOTH_USERNAME);
		assertNotNull("No sharptooth user", user);
		display("User", user);
		assertUser(user, ACCOUNT_SHARPTOOTH_USERNAME, ACCOUNT_SHARPTOOTH_FIST_NAME,
				ACCOUNT_SHARPTOOTH_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
		assertAssignedOrg(user, org.getOid());
		assertHasOrg(user, org.getOid());
		assertHasOrg(org, ORG_TOP_OID);
		assertEquals("Monkey island Org OID has changed", orgMonkeyIslandOid, org.getOid());

		assertSubOrgs(org, 0);
		assertSubOrgs(ORG_TOP_OID, 1);

		assertBasicRoleAndResources(user);
		String thisRoleCanibalismOid = assertResponsibility(user, RESP_CANIBALISM);
		assertEquals("Canibalism role OID has changed", roleCanibalismOid, thisRoleCanibalismOid);
		assertAssignments(user, 3);
	}

	/**
	 * Yet another canibal to play with.
	 */
	@Test
	public void test107AddHrAccountRedskull() throws Exception {
		final String TEST_NAME = "test107AddHrAccountRedskull";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_REDSKULL_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_REDSKULL_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_REDSKULL_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_MONKEY_ISLAND);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES, RESP_CANIBALISM);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_REDSKULL_USERNAME);
		assertNotNull("No redskull user", user);
		display("User", user);
		assertUser(user, ACCOUNT_REDSKULL_USERNAME, ACCOUNT_REDSKULL_FIST_NAME, ACCOUNT_REDSKULL_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
		assertAssignedOrg(user, org.getOid());
		assertHasOrg(user, org.getOid());
		assertHasOrg(org, ORG_TOP_OID);
		assertEquals("Monkey island Org OID has changed", orgMonkeyIslandOid, org.getOid());

		assertSubOrgs(org, 0);
		assertSubOrgs(ORG_TOP_OID, 1);

		assertBasicRoleAndResources(user);
		String thisRoleCanibalismOid = assertResponsibility(user, RESP_CANIBALISM);
		assertEquals("Canibalism role OID has changed", roleCanibalismOid, thisRoleCanibalismOid);
		assertAssignments(user, 3);
	}

	/**
	 * Remove "canibalism" responsibility from redskull.
	 */
	@Test
	public void test108RedskullGoesVegeratian() throws Exception {
		final String TEST_NAME = "test108RedskullGoesVegeratian";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount account = dummyResourceHr.getAccountByUsername(ACCOUNT_REDSKULL_USERNAME);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		account.removeAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES, RESP_CANIBALISM);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_REDSKULL_USERNAME);
		assertNotNull("No redskull user", user);
		display("User", user);
		assertUser(user, ACCOUNT_REDSKULL_USERNAME, ACCOUNT_REDSKULL_FIST_NAME, ACCOUNT_REDSKULL_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
		assertAssignedOrg(user, org.getOid());
		assertHasOrg(user, org.getOid());
		assertHasOrg(org, ORG_TOP_OID);

		assertSubOrgs(org, 0);
		assertSubOrgs(ORG_TOP_OID, 1);

		assertEquals("Monkey island Org OID has changed", orgMonkeyIslandOid, org.getOid());

		assertBasicRoleAndResources(user);
		roleCanibalismOid = assertNoResponsibility(user, RESP_CANIBALISM);
		assertAssignments(user, 2);
	}

	/**
	 * Vegetarian cannibal? Not really!
	 */
	@Test
	public void test109HrDeleteRedskull() throws Exception {
		final String TEST_NAME = "test109HrDeleteRedskull";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		dummyResourceHr.deleteAccountByName(ACCOUNT_REDSKULL_USERNAME);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_REDSKULL_USERNAME);
		display("User", user);
		assertNull("Redskull user not gone", user);

		dumpOrgTree();

		PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
		assertHasOrg(org, ORG_TOP_OID);
		assertSubOrgs(org, 0);
		assertSubOrgs(ORG_TOP_OID, 1);

		assertEquals("Monkey island Org OID has changed", orgMonkeyIslandOid, org.getOid());
	}

	/**
	 * Two-level orgpath. Both orgs should be created.
	 */
	@Test
	public void test110AddHrAccountGuybrush() throws Exception {
		final String TEST_NAME = "test110AddHrAccountGuybrush";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_GUYBRUSH_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_GUYBRUSH_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_GUYBRUSH_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_FREELANCE);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_GUYBRUSH_USERNAME);
		assertNotNull("No guybrush user", user);
		display("User", user);
		assertUserGuybrush(user);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgFreelance = getAndAssertReplicatedOrg("Freelance");
		PrismObject<OrgType> orgMoR = getAndAssertReplicatedOrg("Ministry of Rum");
		orgMoROid = orgMoR.getOid();

		assertAssignedOrg(user, orgFreelance.getOid());
		assertHasOrg(user, orgFreelance.getOid());
		assertHasOrg(orgFreelance, orgMoR.getOid());
		assertHasOrg(orgMoR, ORG_TOP_OID);

		assertSubOrgs(orgFreelance, 0);
		assertSubOrgs(orgMoR, 1);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 2);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Two-level orgpath, partially created. Only scumm bar should be crated.
	 * Ministry should be reused.
	 */
	@Test
	public void test115AddHrAccountMancomb() throws Exception {
		final String TEST_NAME = "test115AddHrAccountMancomb";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_MANCOMB_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_MANCOMB_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_MANCOMB_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_SCUMM_BAR);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_MANCOMB_USERNAME);
		assertNotNull("No mancomb user", user);
		display("User", user);
		assertUser(user, ACCOUNT_MANCOMB_USERNAME, ACCOUNT_MANCOMB_FIST_NAME, ACCOUNT_MANCOMB_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgScummBar = getAndAssertReplicatedOrg("Scumm Bar");
		orgScummBarOid = orgScummBar.getOid();
		PrismObject<OrgType> orgMoR = getAndAssertReplicatedOrg("Ministry of Rum");

		assertAssignedOrg(user, orgScummBar.getOid());
		assertHasOrg(user, orgScummBar.getOid());
		assertHasOrg(orgScummBar, orgMoR.getOid());
		assertHasOrg(orgMoR, ORG_TOP_OID);

		assertSubOrgs(orgScummBar, 0);
		assertSubOrgs(orgMoR, 2);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 2);

		assertEquals("MoR Org OID has changed", orgMoROid, orgMoR.getOid());

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Two-level orgpath, completely created. No new orgs should be created.
	 */
	@Test
	public void test117AddHrAccountCobb() throws Exception {
		final String TEST_NAME = "test117AddHrAccountCobb";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_COBB_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_COBB_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_COBB_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_SCUMM_BAR);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_COBB_USERNAME);
		assertNotNull("No cobb user", user);
		display("User", user);
		assertUser(user, ACCOUNT_COBB_USERNAME, ACCOUNT_COBB_FIST_NAME, ACCOUNT_COBB_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgScummBar = getAndAssertReplicatedOrg("Scumm Bar");
		PrismObject<OrgType> orgMoR = getAndAssertReplicatedOrg("Ministry of Rum");

		assertAssignedOrg(user, orgScummBar.getOid());
		assertHasOrg(user, orgScummBar.getOid());
		assertHasOrg(orgScummBar, orgMoR.getOid());
		assertHasOrg(orgMoR, ORG_TOP_OID);

		assertEquals("MoR Org OID has changed", orgMoROid, orgMoR.getOid());
		assertEquals("Scumm bar Org OID has changed", orgScummBarOid, orgScummBar.getOid());

		assertSubOrgs(orgScummBar, 0);
		assertSubOrgs(orgMoR, 2);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 2);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Four-level orgpath, completely new.
	 */
	@Test
	public void test130AddHrAccountLargo() throws Exception {
		final String TEST_NAME = "test130AddHrAccountLargo";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_LARGO_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_LARGO_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_LARGO_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_BRUTE);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_LARGO_USERNAME);
		assertNotNull("No largo user", user);
		display("User", user);
		assertUser(user, ACCOUNT_LARGO_USERNAME, ACCOUNT_LARGO_FIST_NAME, ACCOUNT_LARGO_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgMoO = getAndAssertReplicatedOrg("Ministry of Offense");
		PrismObject<OrgType> orgDoM = getAndAssertReplicatedOrg("Department of Mischief");
		PrismObject<OrgType> orgVSec = getAndAssertReplicatedOrg("Violence Section");
		PrismObject<OrgType> orgBOff = getAndAssertReplicatedOrg("Brute Office");

		assertAssignedOrg(user, orgBOff.getOid());
		assertHasOrg(user, orgBOff.getOid());
		assertHasOrg(orgBOff, orgVSec.getOid());
		assertHasOrg(orgVSec, orgDoM.getOid());
		assertHasOrg(orgDoM, orgMoO.getOid());
		assertHasOrg(orgMoO, ORG_TOP_OID);

		assertSubOrgs(orgBOff, 0);
		assertSubOrgs(orgVSec, 1);
		assertSubOrgs(orgDoM, 1);
		assertSubOrgs(orgMoO, 1);
		assertSubOrgs(orgScummBarOid, 0);
		assertSubOrgs(orgMoROid, 2);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 3);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Two-level orgpath, upper org is only as ou in LDAP, it is not in
	 * midpoint.
	 */
	@Test
	public void test140AddHrAccountWally() throws Exception {
		final String TEST_NAME = "test140AddHrAccountWally";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_WALLY_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_WALLY_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_WALLY_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_CARTOGRAPHY);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_WALLY_USERNAME);
		assertNotNull("No cobb user", user);
		display("User", user);
		assertUser(user, ACCOUNT_WALLY_USERNAME, ACCOUNT_WALLY_FIST_NAME, ACCOUNT_WALLY_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgCartography = getAndAssertReplicatedOrg("Cartography");
		PrismObject<OrgType> orgScabb = getAndAssertReplicatedOrg(ORGPATH_SCABB_ISLAND);

		assertAssignedOrg(user, orgCartography.getOid());
		assertHasOrg(user, orgCartography.getOid());
		assertHasOrg(orgCartography, orgScabb.getOid());
		assertHasOrg(orgScabb, ORG_TOP_OID);

		assertSubOrgs(orgCartography, 0);
		assertSubOrgs(orgScabb, 1);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 4);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Two-level orgpath, both orgs are only as ou in LDAP, not in midpoint.
	 */
	@Test
	public void test142AddHrAccountAugustus() throws Exception {
		final String TEST_NAME = "test142AddHrAccountAugustus";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_AUGUSTUS_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_AUGUSTUS_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_AUGUSTUS_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_BOOTY_ISLAND_LOOKOUT);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_AUGUSTUS_USERNAME);
		assertNotNull("No cobb user", user);
		display("User", user);
		assertUser(user, ACCOUNT_AUGUSTUS_USERNAME, ACCOUNT_AUGUSTUS_FIST_NAME, ACCOUNT_AUGUSTUS_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgLookout = getAndAssertReplicatedOrg("Lookout");
		PrismObject<OrgType> orgBooty = getAndAssertReplicatedOrg(ORGPATH_BOOTY_ISLAND);

		assertAssignedOrg(user, orgLookout.getOid());
		assertHasOrg(user, orgLookout.getOid());
		assertHasOrg(orgLookout, orgBooty.getOid());
		assertHasOrg(orgBooty, ORG_TOP_OID);

		assertSubOrgs(orgLookout, 0);
		assertSubOrgs(orgBooty, 1);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 5);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Some national characters there.
	 */
	@Test
	public void test185AddHrAccountStan() throws Exception {
		final String TEST_NAME = "test185AddHrAccountStan";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_STAN_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_STAN_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_STAN_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_DOCKS);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_STAN_USERNAME);
		assertNotNull("No largo user", user);
		display("User", user);
		assertUser(user, ACCOUNT_STAN_USERNAME, ACCOUNT_STAN_FIST_NAME, ACCOUNT_STAN_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgDocks = getAndAssertReplicatedOrg("Docks");
		PrismObject<OrgType> orgMelee = getAndAssertReplicatedOrg(ORGPATH_MELEE_ISLAND);

		assertAssignedOrg(user, orgDocks.getOid());
		assertHasOrg(user, orgDocks.getOid());
		assertHasOrg(orgDocks, orgMelee.getOid());
		assertHasOrg(orgMelee, ORG_TOP_OID);

		assertSubOrgs(orgDocks, 0);
		assertSubOrgs(orgMelee, 1);
		assertSubOrgs(orgScummBarOid, 0);
		assertSubOrgs(orgMoROid, 2);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 6);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Commas in the org structure.
	 */
	@Test
	public void test186AddHrAccountCapsize() throws Exception {
		final String TEST_NAME = "test186AddHrAccountCapsize";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_CAPSIZE_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_CAPSIZE_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_CAPSIZE_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_CAPSIZE);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_CAPSIZE_USERNAME);
		assertNotNull("No largo user", user);
		display("User", user);
		assertUser(user, ACCOUNT_CAPSIZE_USERNAME, ACCOUNT_CAPSIZE_FIST_NAME, ACCOUNT_CAPSIZE_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgCapsize = getAndAssertReplicatedOrg("Cruises, Charter and Capsize");
		PrismObject<OrgType> orgTour = getAndAssertReplicatedOrg("Tourist Industries, Tri-Island Area");

		assertAssignedOrg(user, orgCapsize.getOid());
		assertHasOrg(user, orgCapsize.getOid());
		assertHasOrg(orgCapsize, orgTour.getOid());
		assertHasOrg(orgTour, ORG_TOP_OID);

		assertSubOrgs(orgCapsize, 0);
		assertSubOrgs(orgTour, 1);
		assertSubOrgs(orgScummBarOid, 0);
		assertSubOrgs(orgMoROid, 2);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 7);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Comma in username
	 */
	@Test
	public void test187AddHrAccountRogersSr() throws Exception {
		final String TEST_NAME = "test187AddHrAccountRogersSr";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_ROGERSSR_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_ROGERSSR_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_ROGERSSR_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_SCUMM_BAR);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_ROGERSSR_USERNAME);
		assertNotNull("No largo user", user);
		display("User", user);
		assertUser(user, ACCOUNT_ROGERSSR_USERNAME, ACCOUNT_ROGERSSR_FIST_NAME, ACCOUNT_ROGERSSR_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgScummBar = getAndAssertReplicatedOrg("Scumm Bar");
		PrismObject<OrgType> orgMoR = getAndAssertReplicatedOrg("Ministry of Rum");

		assertAssignedOrg(user, orgScummBar.getOid());
		assertHasOrg(user, orgScummBar.getOid());
		assertHasOrg(orgScummBar, orgMoR.getOid());
		assertHasOrg(orgMoR, ORG_TOP_OID);

		assertEquals("MoR Org OID has changed", orgMoROid, orgMoR.getOid());
		assertEquals("Scumm bar Org OID has changed", orgScummBarOid, orgScummBar.getOid());

		assertSubOrgs(orgScummBarOid, 0);
		assertSubOrgs(orgMoROid, 2);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 7);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	/**
	 * Lot of national characters here.
	 */
	@Test
	public void test190AddHrAccountTeleke() throws Exception {
		final String TEST_NAME = "test190AddHrAccountTeleke";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		DummyAccount newAccount = new DummyAccount(ACCOUNT_TELEKE_USERNAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_TELEKE_FIST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_TELEKE_LAST_NAME);
		newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_HRAD);

		// WHEN
		dummyResourceHr.addAccount(newAccount);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		PrismObject<UserType> user = findUserByUsername(ACCOUNT_TELEKE_USERNAME);
		assertNotNull("No largo user", user);
		display("User", user);
		assertUser(user, ACCOUNT_TELEKE_USERNAME, ACCOUNT_TELEKE_FIST_NAME, ACCOUNT_TELEKE_LAST_NAME);
		assertAccount(user, RESOURCE_DUMMY_HR_OID);

		dumpOrgTree();

		PrismObject<OrgType> orgHrad = getAndAssertReplicatedOrg(HRAD);
		PrismObject<OrgType> orgKarpatula = getAndAssertReplicatedOrg(ORGPATH_KARPATULA);

		assertAssignedOrg(user, orgHrad.getOid());
		assertHasOrg(user, orgHrad.getOid());
		assertHasOrg(orgHrad, orgKarpatula.getOid());
		assertHasOrg(orgKarpatula, ORG_TOP_OID);

		assertSubOrgs(orgHrad, 0);
		assertSubOrgs(orgKarpatula, 1);
		assertSubOrgs(orgScummBarOid, 0);
		assertSubOrgs(orgMoROid, 2);
		assertSubOrgs(orgMonkeyIslandOid, 0);
		assertSubOrgs(ORG_TOP_OID, 8);

		assertBasicRoleAndResources(user);
		assertAssignments(user, 2);
	}

	@Test
	public void test500ReconcileOpenDJDefault() throws Exception {
		final String TEST_NAME = "test500ReconcileOpenDJDefault";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = createTask(TestOrgSync.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task,
				result);
		display("Users before recon", users);
		assertUsers(15);

		reconciliationTaskResultListener.clear();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		importObjectFromFile(TASK_RECON_OPENDJ_DEFAULT_SINGLE_FILE);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		waitForTaskFinish(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID, false, TASK_WAIT_TIMEOUT);
		waitForTaskCloseOrSuspend(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		reconciliationTaskResultListener.assertResult(RESOURCE_OPENDJ_OID, 0, 17, 0, 0);

		users = modelService.searchObjects(UserType.class, null, null, task, result);
		display("Users after recon", users);

		assertUsers(18);

		// Task result
		PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
		display("Recon task after", reconTaskAfter);
		assertTaskClosed(reconTaskAfter);
		OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
		TestUtil.assertSuccess(reconTaskResult);
	}

	@Test
	public void test502ReconcileOpenDJDefaultAgain() throws Exception {
		final String TEST_NAME = "test502ReconcileOpenDJDefaultAgain";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = createTask(TestOrgSync.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		assertUsers(18);
		reconciliationTaskResultListener.clear();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		restartTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		waitForTaskFinish(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID, false, TASK_WAIT_TIMEOUT);
		waitForTaskCloseOrSuspend(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		reconciliationTaskResultListener.assertResult(RESOURCE_OPENDJ_OID, 0, 17, 0, 0);

		assertUsers(18);

		// Task result
		PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
		OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
		display("Recon task result", reconTaskResult);
		TestUtil.assertSuccess(reconTaskResult);
	}

	@Test
	public void test510ReconcileOpenDJLdapGroup() throws Exception {
		final String TEST_NAME = "test510ReconcileOpenDJLdapGroup";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = createTask(TestOrgSync.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task,
				result);
		display("Users before recon", users);
		assertUsers(18);

		reconciliationTaskResultListener.clear();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		importObjectFromFile(TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_FILE);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		waitForTaskFinish(TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_OID, false);
		waitForTaskCloseOrSuspend(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		reconciliationTaskResultListener.assertResult(RESOURCE_OPENDJ_OID, 0, 2, 0, 0);

		users = modelService.searchObjects(UserType.class, null, null, task, result);
		display("Users after recon", users);

		assertUsers(18);

		// Task result
		PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_OID);
		OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
		display("Recon task result", reconTaskResult);
		TestUtil.assertSuccess(reconTaskResult);
	}

	@Test
	public void test550ReconcileOpenDJAfterMembershipChange() throws Exception {
		final String TEST_NAME = "test550ReconcileOpenDJAfterMembershipChange";
		TestUtil.displayTestTile(this, TEST_NAME);

		// We manually remove Lemonhead from R_canibalism group
		// And check whether reconciliation re-adds him again

		// GIVEN
		Task task = createTask(TestOrgSync.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		Collection<String> membersBeforeTest = openDJController.getGroupUniqueMembers(RESP_CANIBALISM_DN);
		System.out.println("group members before test = " + membersBeforeTest);
		assertTrue(RESP_CANIBALISM_DN + " does not contain " + ACCOUNT_LEMONHEAD_DN,
				membersBeforeTest.contains(ACCOUNT_LEMONHEAD_DN));

		openDJController.removeGroupUniqueMember(RESP_CANIBALISM_DN, ACCOUNT_LEMONHEAD_DN);

		System.out.println("group members after removal = "
				+ openDJController.getGroupUniqueMembers(RESP_CANIBALISM_DN));

		openDJController.assertNoUniqueMember(RESP_CANIBALISM_DN, ACCOUNT_LEMONHEAD_DN);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		restartTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		waitForTaskFinish(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID, false, TASK_WAIT_TIMEOUT);
		waitForTaskCloseOrSuspend(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		// Task result
		PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
		OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
		display("Recon task result", reconTaskResult);
		TestUtil.assertSuccess(reconTaskResult);

		Collection<String> membersAfterTest = openDJController.getGroupUniqueMembers(RESP_CANIBALISM_DN);
		System.out.println("group members after test = " + membersAfterTest);
		assertTrue(RESP_CANIBALISM_DN + " does not contain " + ACCOUNT_LEMONHEAD_DN,
				membersAfterTest.contains(ACCOUNT_LEMONHEAD_DN.toLowerCase())); // ...it
																				// seems
																				// to
																				// get
																				// lowercased
																				// during
																				// the
																				// reconciliation
	}

	protected void assertUserGuybrush(PrismObject<UserType> user) {
		assertUser(user, ACCOUNT_GUYBRUSH_USERNAME, ACCOUNT_GUYBRUSH_FIST_NAME, ACCOUNT_GUYBRUSH_LAST_NAME);
	}

	protected void assertUserHerman(PrismObject<UserType> user) {
		assertUser(user, ACCOUNT_HERMAN_USERNAME, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
	}

	protected void assertUser(PrismObject<UserType> user, String username, String firstName,
			String lastName) {
		assertUser(user, user.getOid(), username, firstName + " " + lastName, firstName, lastName);
	}

	private PrismObject<OrgType> getAndAssertReplicatedOrg(String orgName)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException,
			CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		PrismObject<OrgType> org = getOrg(orgName);
		PrismAsserts.assertPropertyValue(org, OrgType.F_ORG_TYPE, "replicated");
		assertAssignedRole(org, ROLE_META_REPLICATED_ORG_OID);
		PrismReferenceValue linkRef = getSingleLinkRef(org);
		// We are bold enough to get the whole shadow
		PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("Org " + orgName + " shadow", shadow);
		// TODO assert shadow content

		Entry ouEntry = openDJController.searchSingle("ou=" + orgName);
		assertNotNull("No ou LDAP entry for " + orgName);
		display("OU entry", ouEntry);
		openDJController.assertObjectClass(ouEntry, "organizationalUnit");

		return org;
	}

	private void assertBasicRoleAndResources(PrismObject<UserType> user) throws ObjectNotFoundException,
			SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		assertAssignedRole(user, ROLE_BASIC_OID);
		PrismReferenceValue linkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("OpenDJ shadow linked to " + user, shadow);
		// TODO assert shadow content
	}

	private String assertResponsibility(PrismObject<UserType> user, String respName)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException,
			CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		String respRoleName = "R_" + respName;
		PrismObject<RoleType> respRole = searchObjectByName(RoleType.class, respRoleName);
		assertNotNull("No role for responsibility " + respName);
		display("Responsibility role for " + respName, respRole);
		assertAssignedRole(user, respRole.getOid());

		PrismReferenceValue linkRef = getSingleLinkRef(respRole);
		PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("Role " + respRoleName + " shadow", shadow);
		// TODO assert shadow content

		String groupDn = "cn=" + respRoleName + ",ou=groups," + openDJController.getSuffix();
		Entry groupEntry = openDJController.fetchAndAssertEntry(groupDn, "groupOfUniqueNames");
		display("Group entry", groupEntry);

		PrismReferenceValue accountLinkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> accountShadow = getShadowModel(accountLinkRef.getOid());
		String accountDn = IntegrationTestTools.getSecondaryIdentifier(accountShadow);
		openDJController.assertUniqueMember(groupEntry, accountDn);

		IntegrationTestTools.assertAssociation(accountShadow, OPENDJ_ASSOCIATION_GROUP_NAME, shadow.getOid());

		return respRole.getOid();
	}

	private String assertNoResponsibility(PrismObject<UserType> user, String respName)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException,
			CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		String respRoleName = "R_" + respName;
		PrismObject<RoleType> respRole = searchObjectByName(RoleType.class, respRoleName);
		assertNotNull("No role for responsibility " + respName);
		display("Responsibility role for " + respName, respRole);
		assertNotAssignedRole(user, respRole.getOid());

		PrismReferenceValue linkRef = getSingleLinkRef(respRole);
		PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("Role " + respRoleName + " shadow", shadow);
		// TODO assert shadow content

		String groupDn = "cn=" + respRoleName + ",ou=groups," + openDJController.getSuffix();
		Entry groupEntry = openDJController.fetchAndAssertEntry(groupDn, "groupOfUniqueNames");
		display("Group entry", groupEntry);

		PrismReferenceValue accountLinkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> accountShadow = getShadowModel(accountLinkRef.getOid());
		String accountDn = IntegrationTestTools.getSecondaryIdentifier(accountShadow);
		openDJController.assertNoUniqueMember(groupEntry, accountDn);

		IntegrationTestTools.assertNoAssociation(accountShadow, OPENDJ_ASSOCIATION_GROUP_NAME,
				shadow.getOid());

		return respRole.getOid();
	}
}
