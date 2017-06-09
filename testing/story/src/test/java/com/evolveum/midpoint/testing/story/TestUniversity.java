/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.story;


import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import java.io.File;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Radovan Semancik
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUniversity extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "university");
	
	public static final File OBJECT_TEMPLATE_ORG_FILE = new File(TEST_DIR, "object-template-org.xml");
	public static final String OBJECT_TEMPLATE_ORG_OID = "10000000-0000-0000-0000-000000000231";
	
	protected static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");
	protected static final String RESOURCE_DUMMY_HR_ID = "HR";
	protected static final String RESOURCE_DUMMY_HR_OID = "10000000-0000-0000-0000-000000000001";

	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	protected static final QName OPENDJ_ASSOCIATION_GROUP_NAME = new QName(RESOURCE_OPENDJ_NAMESPACE, "group");

	private static final String DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH = "orgpath";

	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";
	
	public static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
	public static final String ROLE_META_ORG_OID = "10000000-0000-0000-0000-000000006601";
	
	protected static final File TASK_LIVE_SYNC_DUMMY_HR_FILE = new File(TEST_DIR, "task-dummy-hr-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_HR_OID = "10000000-0000-0000-5555-555500000001";

	protected static final File TASK_RECON_OPENDJ_DEFAULT_SINGLE_FILE = new File(TEST_DIR, "task-reconcile-opendj-default-single.xml");
	protected static final String TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID = "10000000-0000-0000-5555-555500000004";

	protected static final File TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_FILE = new File(TEST_DIR, "task-reconcile-opendj-ldapgroup-single.xml");
	protected static final String TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_OID = "10000000-0000-0000-5555-555500000014";

	private static final File SCABB_OU_LDIF_FILE = new File(TEST_DIR, "scabb.ldif");
	private static final File BOOTY_OU_LDIF_FILE = new File(TEST_DIR, "booty.ldif");
	private static final File BOOTY_LOOKOUT_OU_LDIF_FILE = new File(TEST_DIR, "booty-lookout.ldif");

    @Autowired
	private ReconciliationTaskHandler reconciliationTaskHandler;
	
	private DebugReconciliationTaskResultListener reconciliationTaskResultListener;
	
	protected static DummyResource dummyResourceHr;
	protected static DummyResourceContoller dummyResourceCtlHr;
	protected ResourceType resourceDummyHrType;
	protected PrismObject<ResourceType> resourceDummyHr;
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;

	@Override
	protected String getTopOrgOid() {
		return ORG_TOP_OID;
	}
	
	@Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
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
		DummyObjectClass privilegeObjectClass = dummyResourceCtlHr.getDummyResource().getPrivilegeObjectClass();
		dummyResourceCtlHr.addAttrDef(privilegeObjectClass, DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, String.class, false, false);
		dummyResourceHr = dummyResourceCtlHr.getDummyResource();
		resourceDummyHr = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_HR_FILE, RESOURCE_DUMMY_HR_OID, initTask, initResult);
		resourceDummyHrType = resourceDummyHr.asObjectable();
		dummyResourceCtlHr.setResource(resourceDummyHr);
		dummyResourceHr.setSyncStyle(DummySyncStyle.SMART);
		
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

//		// LDAP content
//		openDJController.addEntryFromLdifFile(SCABB_OU_LDIF_FILE);
//		openDJController.addEntryFromLdifFile(BOOTY_OU_LDIF_FILE);
//		openDJController.addEntryFromLdifFile(BOOTY_LOOKOUT_OU_LDIF_FILE);
//
		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, initResult);
		setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);

		// Org
		importObjectFromFile(ORG_TOP_FILE, initResult);

		// Role
		importObjectFromFile(ROLE_META_ORG_FILE, initResult);

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

        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_HR_OID, false);

        dumpOrgTree();
	}
	
	@Test
    public void test100AddComeniusUniversity() throws Exception {
		final String TEST_NAME = "test100AddComeniusUniversity";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestUniversity.class.getName() + "." + TEST_NAME);

		DummyPrivilege comenius = new DummyPrivilege("UK");

        // WHEN
        dummyResourceHr.addPrivilege(comenius);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

        // THEN
        PrismObject<OrgType> org = getAndAssertFunctionalOrg("UK");
        assertNotNull("Comenius University was not found", org);
        display("Org", org);

		dumpOrgTree();

		assertHasOrg(org, ORG_TOP_OID);
		assertAssignedOrg(org, ORG_TOP_OID);
		assertSubOrgs(org, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
	}

	@Test
	public void test110AddComeniusStructure() throws Exception {
		final String TEST_NAME = "test110AddComeniusStructure";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestUniversity.class.getName() + "." + TEST_NAME);

		DummyPrivilege srcFmfi = new DummyPrivilege("FMFI");
		srcFmfi.addAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK");

		DummyPrivilege srcVc = new DummyPrivilege("VC");
		srcVc.addAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK:FMFI");

		DummyPrivilege srcPrif = new DummyPrivilege("PRIF");
		srcPrif.addAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK");

		// WHEN
		dummyResourceHr.addPrivilege(srcFmfi);
		dummyResourceHr.addPrivilege(srcVc);
		dummyResourceHr.addPrivilege(srcPrif);
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

		// THEN
		dumpOrgTree();

		PrismObject<OrgType> uk = getAndAssertFunctionalOrg("UK");
		assertNotNull("UK was not found", uk);
		display("Org UK", uk);

		assertHasOrg(uk, ORG_TOP_OID);
		assertAssignedOrg(uk, ORG_TOP_OID);
		assertSubOrgs(uk, 2);
		assertSubOrgs(ORG_TOP_OID, 1);
		assertGroupMembers(uk, "cn=DL-FMFI,ou=FMFI,ou=UK,dc=example,dc=com", "cn=DL-PRIF,ou=PRIF,ou=UK,dc=example,dc=com");

		PrismObject<OrgType> fmfi = getAndAssertFunctionalOrg("FMFI");
		assertNotNull("FMFI was not found", fmfi);
		display("Org FMFI", fmfi);

		assertHasOrg(fmfi, uk.getOid());
		assertAssignedOrg(fmfi, uk.getOid());
		assertSubOrgs(fmfi, 1);
		assertGroupMembers(fmfi, "cn=DL-VC,ou=VC,ou=FMFI,ou=UK,dc=example,dc=com");

		PrismObject<OrgType> prif = getAndAssertFunctionalOrg("PRIF");
		assertNotNull("PRIF was not found", prif);
		display("Org PRIF", prif);

		assertHasOrg(prif, uk.getOid());
		assertAssignedOrg(prif, uk.getOid());
		assertSubOrgs(prif, 0);
		assertNoGroupMembers(prif);

		PrismObject<OrgType> vc = getAndAssertFunctionalOrg("VC");
		assertNotNull("VC was not found", vc);
		display("Org VC", vc);

		assertHasOrg(vc, fmfi.getOid());
		assertAssignedOrg(vc, fmfi.getOid());
		assertSubOrgs(vc, 0);
		assertNoGroupMembers(vc);
	}

	private void assertGroupMembers(PrismObject<OrgType> org, String... members) throws Exception {
		String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
		PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
		assertAttribute(resourceOpenDj, groupShadow.asObjectable(), new QName(MidPointConstants.NS_RI, "uniqueMember"), members);
	}

	private void assertNoGroupMembers(PrismObject<OrgType> org) throws Exception {
		String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
		PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
		assertNoAttribute(resourceOpenDj, groupShadow.asObjectable(), new QName(MidPointConstants.NS_RI, "uniqueMember"));
	}


	@Test
	public void test120MoveComputingCentre() throws Exception {
		final String TEST_NAME = "test120MoveComputingCentre";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestUniversity.class.getName() + "." + TEST_NAME);

		DummyPrivilege srcVc = dummyResourceHr.getPrivilegeByName("VC");

		// WHEN
		srcVc.replaceAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK:PRIF");
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true, 999999999);

		// THEN
		dumpOrgTree();

		PrismObject<OrgType> uk = getAndAssertFunctionalOrg("UK");
		assertNotNull("UK was not found", uk);
		display("Org UK", uk);

		assertHasOrg(uk, ORG_TOP_OID);
		assertAssignedOrg(uk, ORG_TOP_OID);
		assertSubOrgs(uk, 2);
		assertSubOrgs(ORG_TOP_OID, 1);
		assertGroupMembers(uk, "cn=DL-FMFI,ou=FMFI,ou=UK,dc=example,dc=com", "cn=DL-PRIF,ou=PRIF,ou=UK,dc=example,dc=com");

		PrismObject<OrgType> fmfi = getAndAssertFunctionalOrg("FMFI");
		assertNotNull("FMFI was not found", fmfi);
		display("Org FMFI", fmfi);

		assertHasOrg(fmfi, uk.getOid());
		assertAssignedOrg(fmfi, uk.getOid());
		assertSubOrgs(fmfi, 0);
		assertNoGroupMembers(fmfi);

		PrismObject<OrgType> prif = getAndAssertFunctionalOrg("PRIF");
		assertNotNull("PRIF was not found", prif);
		display("Org PRIF", prif);

		assertHasOrg(prif, uk.getOid());
		assertAssignedOrg(prif, uk.getOid());
		assertSubOrgs(prif, 1);
		assertGroupMembers(prif, "cn=dl-vc,ou=vc,ou=prif,ou=uk,dc=example,dc=com");

		PrismObject<OrgType> vc = getAndAssertFunctionalOrg("VC");
		assertNotNull("VC was not found", vc);
		display("Org VC", vc);

		assertHasOrg(vc, prif.getOid());
		assertAssignedOrg(vc, prif.getOid());
		assertSubOrgs(vc, 0);
		assertNoGroupMembers(vc);
	}

//	@Test
//    public void test500ReconcileOpenDJDefault() throws Exception {
//		final String TEST_NAME = "test500ReconcileOpenDJDefault";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = createTask(TestInsurance.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//
//        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
//        display("Users before recon", users);
//        assertUsers(15);
//
//        reconciliationTaskResultListener.clear();
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        importObjectFromFile(TASK_RECON_OPENDJ_DEFAULT_SINGLE_FILE);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        waitForTaskFinish(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID, false);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        reconciliationTaskResultListener.assertResult(RESOURCE_OPENDJ_OID, 0, 17, 0, 0);
//
//        users = modelService.searchObjects(UserType.class, null, null, task, result);
//        display("Users after recon", users);
//
//        assertUsers(18);
//
//        // Task result
//        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
//        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
//        display("Recon task result", reconTaskResult);
//        TestUtil.assertSuccess(reconTaskResult);
//	}
//
//	@Test
//    public void test502ReconcileOpenDJDefaultAgain() throws Exception {
//		final String TEST_NAME = "test502ReconcileOpenDJDefaultAgain";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = createTask(TestInsurance.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//
//        assertUsers(18);
//        reconciliationTaskResultListener.clear();
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        restartTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        waitForTaskFinish(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID, false);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        reconciliationTaskResultListener.assertResult(RESOURCE_OPENDJ_OID, 0, 17, 0, 0);
//
//        assertUsers(18);
//
//        // Task result
//        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
//        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
//        display("Recon task result", reconTaskResult);
//        TestUtil.assertSuccess(reconTaskResult);
//	}
//
//	@Test
//    public void test510ReconcileOpenDJLdapGroup() throws Exception {
//		final String TEST_NAME = "test510ReconcileOpenDJLdapGroup";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = createTask(TestInsurance.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//
//        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
//        display("Users before recon", users);
//        assertUsers(18);
//
//        reconciliationTaskResultListener.clear();
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        importObjectFromFile(TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_FILE);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        waitForTaskFinish(TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_OID, false);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        reconciliationTaskResultListener.assertResult(RESOURCE_OPENDJ_OID, 0, 2, 0, 0);
//
//        users = modelService.searchObjects(UserType.class, null, null, task, result);
//        display("Users after recon", users);
//
//        assertUsers(18);
//
//        // Task result
//        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_LDAPGROUP_SINGLE_OID);
//        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
//        display("Recon task result", reconTaskResult);
//        TestUtil.assertSuccess(reconTaskResult);
//	}
//
//    @Test
//    public void test550ReconcileOpenDJAfterMembershipChange() throws Exception {
//        final String TEST_NAME = "test550ReconcileOpenDJAfterMembershipChange";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // We manually remove Lemonhead from R_canibalism group
//        // And check whether reconciliation re-adds him again
//
//        // GIVEN
//        Task task = createTask(TestInsurance.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//
//        Collection<String> membersBeforeTest = openDJController.getGroupUniqueMembers(RESP_CANIBALISM_DN);
//        System.out.println("group members before test = " + membersBeforeTest);
//        assertTrue(RESP_CANIBALISM_DN + " does not contain " + ACCOUNT_LEMONHEAD_DN, membersBeforeTest.contains(ACCOUNT_LEMONHEAD_DN));
//
//        openDJController.removeGroupUniqueMember(RESP_CANIBALISM_DN, ACCOUNT_LEMONHEAD_DN);
//
//        System.out.println("group members after removal = " + openDJController.getGroupUniqueMembers(RESP_CANIBALISM_DN));
//
//        openDJController.assertNoUniqueMember(RESP_CANIBALISM_DN, ACCOUNT_LEMONHEAD_DN);
//
//        // WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        restartTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        waitForTaskFinish(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID, false);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//
//        // Task result
//        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECON_OPENDJ_DEFAULT_SINGLE_OID);
//        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
//        display("Recon task result", reconTaskResult);
//        TestUtil.assertSuccess(reconTaskResult);
//
//        Collection<String> membersAfterTest = openDJController.getGroupUniqueMembers(RESP_CANIBALISM_DN);
//        System.out.println("group members after test = " + membersAfterTest);
//        assertTrue(RESP_CANIBALISM_DN + " does not contain " + ACCOUNT_LEMONHEAD_DN, membersAfterTest.contains(ACCOUNT_LEMONHEAD_DN.toLowerCase()));    // ...it seems to get lowercased during the reconciliation
//    }

	private PrismObject<OrgType> getAndAssertFunctionalOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		PrismObject<OrgType> org = getOrg(orgName);
		PrismAsserts.assertPropertyValue(org, OrgType.F_ORG_TYPE, "functional");
		assertAssignedRole(org, ROLE_META_ORG_OID);

		String ouOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.GENERIC, "org-ou");
		PrismObject<ShadowType> ouShadow = getShadowModel(ouOid);
		display("Org " + orgName + " OU shadow", ouShadow);
		// TODO assert shadow content

		String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
		PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
		display("Org "+orgName+" group shadow", groupShadow);
		// TODO assert shadow content

		Entry ouEntry = openDJController.searchSingle("ou="+orgName);
		assertNotNull("No ou LDAP entry for "+orgName, ouEntry);
		display("OU entry", ouEntry);
		openDJController.assertObjectClass(ouEntry, "organizationalUnit");

		Entry groupEntry = openDJController.searchSingle("cn=DL-"+orgName);
		assertNotNull("No group LDAP entry for "+orgName, groupEntry);
		display("OU GROUP entry", groupEntry);
		openDJController.assertObjectClass(groupEntry, "groupOfUniqueNames");

		return org;
	}
	
}
