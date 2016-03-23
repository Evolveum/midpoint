/*
 * Copyright (c) 2016 Evolveum
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
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
 * Flat LDAP structure. All accounts in ou=people. The organizational structure is
 * reflected to (non-nested) LDAP groups. Users are members of the groups to reflect
 * the orgstruct.
 *  
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestFlatLdap extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "flat-ldap");
	
	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	protected static final QName OPENDJ_ASSOCIATION_GROUP_NAME = new QName(RESOURCE_OPENDJ_NAMESPACE, "group");

	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";
	
	public static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
	public static final String ROLE_META_ORG_OID = "10000000-0000-0000-0000-000000006601";

	private static final String ORG_ROYULA_CARPATHIA_NAME = "Royula Carpathia";
	private static final String ORG_CORTUV_HRAD_NAME = "Čortův hrád";

	private static final String ORG_TYPE_FUNCTIONAL = "functional";

	private static final String LDAP_GROUP_INTENT = "group";

	private static final String USER_TELEKE_USERNAME = "teleke";
	private static final String USER_TELEKE_GIVEN_NAME = "Felix";
	private static final String USER_TELEKE_FAMILY_NAME = "Teleke z Tölökö";
	
	private static final String USER_GORC_USERNAME = "gorc";
	private static final String USER_GORC_GIVEN_NAME = "Robert";
	private static final String USER_GORC_FAMILY_NAME = "Gorc z Gorců";

		
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;

	private String orgRolyulaCarpathiaOid;

	private String orgCortuvHradOid;

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
		
		
		// Resources	
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

		// Org
		importObjectFromFile(ORG_TOP_FILE, initResult);

		// Role
		importObjectFromFile(ROLE_META_ORG_FILE, initResult);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpOrgTree();
	}
	
	@Test
    public void test100AddOrgRoyulaCarpathia() throws Exception {
		final String TEST_NAME = "test100AddOrgRoyulaCarpathia";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestFlatLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_ROYULA_CARPATHIA_NAME, ORG_TOP_OID);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_ROYULA_CARPATHIA_NAME);
        orgRolyulaCarpathiaOid = orgAfter.getOid();

		dumpOrgTree();

		assertHasOrg(orgAfter, ORG_TOP_OID);
		assertAssignedOrg(orgAfter, ORG_TOP_OID);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
	}
	
	@Test
    public void test110AddUserTeleke() throws Exception {
		final String TEST_NAME = "test110AddUserTeleke";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestFlatLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_TELEKE_USERNAME, 
        		USER_TELEKE_GIVEN_NAME, USER_TELEKE_FAMILY_NAME, orgRolyulaCarpathiaOid);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding user", userBefore);
        addObject(userBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getAndAssertUser(USER_TELEKE_USERNAME, ORG_ROYULA_CARPATHIA_NAME);
        
        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_ROYULA_CARPATHIA_NAME);

		dumpOrgTree();

		assertHasOrg(orgAfter, ORG_TOP_OID);
		assertAssignedOrg(orgAfter, ORG_TOP_OID);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
	}
	
	@Test
    public void test200AddOrgCortuvHrad() throws Exception {
		final String TEST_NAME = "test200AddOrgCortuvHrad";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestFlatLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_CORTUV_HRAD_NAME, orgRolyulaCarpathiaOid);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_CORTUV_HRAD_NAME);
        orgCortuvHradOid = orgAfter.getOid();

		dumpOrgTree();

		assertHasOrg(orgAfter, orgRolyulaCarpathiaOid);
		assertAssignedOrg(orgAfter, orgRolyulaCarpathiaOid);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(orgRolyulaCarpathiaOid, 1);
		assertSubOrgs(ORG_TOP_OID, 1);
	}
	
	@Test
    public void test210AddUserGorc() throws Exception {
		final String TEST_NAME = "test210AddUserGorc";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestFlatLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_GORC_USERNAME, 
        		USER_GORC_GIVEN_NAME, USER_GORC_FAMILY_NAME, orgCortuvHradOid);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding user", userBefore);
        addObject(userBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getAndAssertUser(USER_GORC_USERNAME, ORG_CORTUV_HRAD_NAME, ORG_ROYULA_CARPATHIA_NAME);
        
		dumpOrgTree();
	}

	

	private PrismObject<UserType> createUser(String username, String givenName,
			String familyName, String parentOrgOid) {
		PrismObject<UserType> user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate();
		UserType userType = user.asObjectable();
		userType.setName(new PolyStringType(username));
		userType.setGivenName(new PolyStringType(givenName));
		userType.setFamilyName(new PolyStringType(familyName));
		userType.setFullName(new PolyStringType(givenName + " " + familyName));
		if (parentOrgOid != null) {
			AssignmentType parentAssignment = new AssignmentType();
			ObjectReferenceType parentAssignmentTargetRef = new ObjectReferenceType();
			parentAssignmentTargetRef.setOid(parentOrgOid);
			parentAssignmentTargetRef.setType(OrgType.COMPLEX_TYPE);
			parentAssignment.setTargetRef(parentAssignmentTargetRef);
			userType.getAssignment().add(parentAssignment);
		}
		return user;
	}

	private PrismObject<OrgType> createOrg(String name, String parentOrgOid) {
		PrismObject<OrgType> org = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(OrgType.class).instantiate();
		OrgType orgType = org.asObjectable();
		orgType.setName(new PolyStringType(name));
		orgType.getOrgType().add(ORG_TYPE_FUNCTIONAL);
		AssignmentType metaRoleAssignment = new AssignmentType();
		ObjectReferenceType metaRoleAssignmentTargetRef = new ObjectReferenceType();
		metaRoleAssignmentTargetRef.setOid(ROLE_META_ORG_OID);
		metaRoleAssignmentTargetRef.setType(RoleType.COMPLEX_TYPE);
		metaRoleAssignment.setTargetRef(metaRoleAssignmentTargetRef);
		orgType.getAssignment().add(metaRoleAssignment);
		if (parentOrgOid != null) {
			AssignmentType parentAssignment = new AssignmentType();
			ObjectReferenceType parentAssignmentTargetRef = new ObjectReferenceType();
			parentAssignmentTargetRef.setOid(parentOrgOid);
			parentAssignmentTargetRef.setType(OrgType.COMPLEX_TYPE);
			parentAssignment.setTargetRef(parentAssignmentTargetRef);
			orgType.getAssignment().add(parentAssignment);
		}
		return org;
	}

	private PrismObject<UserType> getAndAssertUser(String username, String... expectedGroupNames) throws SchemaException, CommonException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		PrismObject<UserType> user = findUserByUsername(username);
		display("user", user);

		String shadowOid = getLinkRefOid(user, RESOURCE_OPENDJ_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
		PrismObject<ShadowType> accountShadow = getShadowModel(shadowOid);
		display("Account "+username+" shadow", accountShadow);
		// TODO assert shadow content

		Entry accountEntry = openDJController.searchSingle("uid="+username);
		assertNotNull("No account LDAP entry for "+username, accountEntry);
		display("account entry", accountEntry);
		openDJController.assertObjectClass(accountEntry, "inetOrgPerson");

		for (String expectedGroupName: expectedGroupNames) {
			Entry groupEntry = openDJController.searchSingle("cn="+expectedGroupName);
			assertNotNull("No group LDAP entry for "+expectedGroupName, groupEntry);
			openDJController.assertUniqueMember(groupEntry, accountEntry.getDN().toString());
		}
		
		return user;
	}


	private PrismObject<OrgType> getAndAssertFunctionalOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		PrismObject<OrgType> org = getOrg(orgName);
		display("org", org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_ORG_TYPE, ORG_TYPE_FUNCTIONAL);
		assertAssignedRole(org, ROLE_META_ORG_OID);

		String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
		PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
		display("Org "+orgName+" group shadow", groupShadow);
		// TODO assert shadow content

		Entry groupEntry = openDJController.searchSingle("cn="+orgName);
		assertNotNull("No group LDAP entry for "+orgName, groupEntry);
		display("OU GROUP entry", groupEntry);
		openDJController.assertObjectClass(groupEntry, "groupOfUniqueNames");

		return org;
	}

	private PrismObject<OrgType> getOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
		assertNotNull("The org "+orgName+" is missing!", org);
		display("Org "+orgName, org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, PrismTestUtil.createPolyString(orgName));
		return org;
	}

	private void dumpOrgTree() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		display("Org tree", dumpOrgTree(ORG_TOP_OID));
	}
	

	
	
	
	
	
	
	
	private void assertGroupMembers(PrismObject<OrgType> org, String... members) throws Exception {
		String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
		PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
		assertAttribute(groupShadow, new QName(MidPointConstants.NS_RI, "uniqueMember"), members);
	}

	private void assertNoGroupMembers(PrismObject<OrgType> org) throws Exception {
		String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
		PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
		assertNoAttribute(groupShadow, new QName(MidPointConstants.NS_RI, "uniqueMember"));
	}






	
}
