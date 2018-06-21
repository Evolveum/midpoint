/*
 * Copyright (c) 2016-2017 Evolveum
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


import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Testing dependencies:
 * There are two meta-roles for orgs.
 * Org Metarole contains two inducements, one for creating organizationalUnit (intent=ou) and one for creating groupOfUniqueNames (intent=group) in ldap.
 * group depends on ou (since it is created in ou)
 *
 * Org Metarole VIP is very similar it also contains two inducements, one for creating (intent=ou-vip) and one for creating groupOfUniqueNames (intent=group-vip) in ldap.
 * group-vip depends on ou-cip (since it is created in ou-vip)
 *
 * @author michael gruber
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public  class TestLdapDependency extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap-dependency");

	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	private static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	private static final QName OPENDJ_ASSOCIATION_GROUP_NAME = new QName(RESOURCE_OPENDJ_NAMESPACE, "group");

	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

	public static final String ROLE_META_ORG_OID = "10000000-0000-0000-0000-000000006601";
	public static final String ROLE_META_ORG_VIP_OID = "10000000-0000-0000-0000-000000006602";
	public static final String ROLE_META_ORG_SUPERVIP_OID = "10000000-0000-0000-0000-000000006603";

	private static final String ORG_IT_NAME = "IT";
	private static final String ORG_HR_NAME = "HR";
	private static String orgItOid;
	private static String orgHrOid;


	private static final String ORG_TYPE_FUNCTIONAL = "functional";

	private static final String LDAP_GROUP_INTENT = "group";
	private static final String LDAP_GROUP_VIP_INTENT = "group-vip";
	private static final String LDAP_GROUP_SUPERVIP_INTENT = "group-supervip";

	private static final String LDAP_OU_INTENT = "ou";
	private static final String LDAP_OU_VIP_INTENT = "ou-vip";


	private ResourceType resourceOpenDjType;
	private PrismObject<ResourceType> resourceOpenDj;


	@Override
	protected String getTopOrgOid() {
		return ORG_TOP_OID;
	}

	private File getTestDir() {
		return TEST_DIR;
	}

	private File getResourceOpenDjFile() {
		return new File(getTestDir(), "resource-opendj.xml");
	}

	private File getOrgTopFile() {
		return new File(getTestDir(), "org-top.xml");
	}

	private File getRoleMetaOrgFile() {
		return new File(getTestDir(), "role-meta-org.xml");
	}

	private File getRoleMetaOrgVipFile() {
		return new File(getTestDir(), "role-meta-org-vip.xml");
	}

	private File getRoleMetaOrgSuperVipFile() {
		return new File(getTestDir(), "role-meta-org-supervip.xml");
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


		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, getResourceOpenDjFile(), RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

		// Org
		importObjectFromFile(getOrgTopFile(), initResult);

		// Roles
		importObjectFromFile(getRoleMetaOrgFile(), initResult);
		importObjectFromFile(getRoleMetaOrgVipFile(), initResult);
		importObjectFromFile(getRoleMetaOrgSuperVipFile(), initResult);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpOrgTree();
        dumpLdap();
	}

	@Test
    public void test100AddOrgIT() throws Exception {
		final String TEST_NAME = "test100AddOrgIT";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_IT_NAME, ORG_TOP_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

		PrismObject<OrgType> orgAfter = getOrg(ORG_IT_NAME);
        orgItOid = orgAfter.getOid();

		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
	}

	@Test
    public void test150AssignFunctionalRoleToITOrg() throws Exception {
		final String TEST_NAME = "test150AssignFunctionalRoleToITOrg";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_IT_NAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("orgBefore: ", orgBefore);
        assignRoleToOrg(orgItOid, ROLE_META_ORG_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_IT_NAME);
        display("AFTER Assigning functional role org", orgAfter);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
		assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ORG_TOP_OID);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
	}

	@Test
    public void test170UnassignFunctionalRoleFromITOrg() throws Exception {
		final String TEST_NAME = "test170UnassignFunctionalRoleFromITOrg";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_IT_NAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("unassigning vip role org", orgBefore);
        unassignRoleFromOrg(orgItOid, ROLE_META_ORG_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_IT_NAME);
        display("AFTER unassigning functional role org", orgAfter);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
		assertRoleMembershipRef(orgAfter, ORG_TOP_OID);
		assertNotAssignedRole(orgAfter, ROLE_META_ORG_OID);
		//TODO: assert ldap objects deleted...
	}


	@Test
    public void test200AddOrgHR() throws Exception {
		final String TEST_NAME = "test200AddOrgHR";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_HR_NAME, ORG_TOP_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

		PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        orgHrOid = orgAfter.getOid();

		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 2);
	}

	@Test
    public void test250AssignFunctionalAndVipRoleToHROrg() throws Exception {
		final String TEST_NAME = "test250AssignFunctionalAndVipRoleToHROrg";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("orgBefore: ", orgBefore);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_OID, task, result);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER Assigning functional and vip role org", orgAfter);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 2);
		assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ROLE_META_ORG_VIP_OID, ORG_TOP_OID);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_VIP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_VIP_INTENT);
	}

	@Test
    public void test270UnassignVipRoleFromHROrg() throws Exception {
		final String TEST_NAME = "test270UnassignVipRoleFromHROrg";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("unassigning vip role org", orgBefore);
        unassignRoleFromOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER unassigning vip role org", orgAfter);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 2);
		assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ORG_TOP_OID);
		assertNotAssignedRole(orgAfter, ROLE_META_ORG_VIP_OID);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
		//TODO: assert ldap vip objects deleted...
	}

	//test280AssignVipAndSuperVipRoleToHROrg required for  test290UnassignVipRoleFromHROrg
	@Test
    public void test280AssignVipAndSuperVipRoleToHROrg() throws Exception {
		final String TEST_NAME = "test280AssignVipAndSuperVipRoleToHROrg";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("orgBefore: ", orgBefore);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_SUPERVIP_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER Assigning supervip role org", orgAfter);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 2);
		assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ROLE_META_ORG_VIP_OID, ROLE_META_ORG_SUPERVIP_OID, ORG_TOP_OID);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_VIP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_SUPERVIP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_VIP_INTENT);
	}

	@Test
    public void test290UnassignVipRoleFromHROrg() throws Exception {
		final String TEST_NAME = "test290UnassignVipRoleFromHROrg";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestLdapDependency.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        display("unassigning vip role org", orgBefore);

        try {
	        // WHEN
	        TestUtil.displayWhen(TEST_NAME);
	        unassignRoleFromOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);

	        assertNotReached();

        } catch (PolicyViolationException e) {
        	// this is expected
        }

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER unassigning vip role org", orgAfter);
		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 2);
		assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ROLE_META_ORG_VIP_OID, ROLE_META_ORG_SUPERVIP_OID, ORG_TOP_OID);
		assertAssignedRole(orgAfter, ROLE_META_ORG_VIP_OID);
		assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
		assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
		//TODO: assert ldap vip objects deleted...
	}

	private PrismObject<OrgType> createOrg(String name, String parentOrgOid) throws SchemaException {
		PrismObject<OrgType> org = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(OrgType.class).instantiate();
		OrgType orgType = org.asObjectable();
		orgType.setName(new PolyStringType(name));
		orgType.getOrgType().add(ORG_TYPE_FUNCTIONAL);
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

	private void assertLdapObjectsFunctionalOrg(PrismObject<OrgType> org) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		String orgName =  org.getName().toString();
		display("assert org", org.getName());

		String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
		PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
		display("Org "+orgName+" group shadow", groupShadow);
		// TODO assert shadow content

		Entry groupEntry = openDJController.searchSingle("cn="+orgName);
		assertNotNull("No group LDAP entry for "+orgName, groupEntry);
		display("OU GROUP entry", openDJController.toHumanReadableLdifoid(groupEntry));
		openDJController.assertObjectClass(groupEntry, "groupOfUniqueNames");

	}


	private void assertLdapObject(PrismObject<OrgType> org, ShadowKindType kind, String intent) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		String orgName =  org.getName().toString();
		display("assert org", orgName);

		String objOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, kind, intent);
		PrismObject<ShadowType> objShadow = getShadowModel(objOid);
		display("Org "+orgName+" kind " +kind + " intent " + intent +" shadow", objShadow);
		// TODO assert shadow content

		String search = "";
		if (kind.equals(ShadowKindType.ENTITLEMENT)){
			if (LDAP_GROUP_INTENT.equals(intent)) search = "cn="+orgName;
			if (LDAP_GROUP_VIP_INTENT.equals(intent)) search = "cn="+orgName+"-vip";
			if (LDAP_GROUP_SUPERVIP_INTENT.equals(intent)) search = "cn="+orgName+"-supervip";
		}
		if (kind.equals(ShadowKindType.GENERIC)){
			if (LDAP_OU_INTENT.equals(intent)) search = "ou="+orgName;
			if (LDAP_OU_VIP_INTENT.equals(intent)) search = "ou="+orgName+"-vip";
		}
		Entry objEntry = openDJController.searchSingle(search);
		assertNotNull("No LDAP entry for "+orgName, objEntry);;
		display("LDAP entry kind " +kind + " inten " + intent +" ldapObj", objEntry);

		if (kind.equals(ShadowKindType.ENTITLEMENT)){
			openDJController.assertObjectClass(objEntry, "groupOfUniqueNames");
		}
		if (kind.equals(ShadowKindType.GENERIC)){
			openDJController.assertObjectClass(objEntry, "organizationalUnit");
		}

	}

	private void dumpLdap() throws DirectoryException {
		display("LDAP server tree", openDJController.dumpTree());
		display("LDAP server content", openDJController.dumpEntries());
	}

	////should be in AbstractModelIntegrationTest

	private void modifyOrgAssignment(String orgOid, String roleOid, QName refType, QName relation, Task task,
			PrismContainer<?> extension, ActivationType activationType, boolean add, OperationResult result)
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<OrgType> orgDelta = createAssignmentOrgDelta(orgOid, roleOid, refType, relation, extension, activationType, add);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(orgDelta);
		modelService.executeChanges(deltas, null, task, result);
	}

	private ObjectDelta<OrgType> createAssignmentOrgDelta(String orgOid, String roleOid, QName refType, QName relation,
			PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, extension, activationType, add)));
		ObjectDelta<OrgType> userDelta = ObjectDelta.createModifyDelta(orgOid, modifications, OrgType.class, prismContext);
		return userDelta;
	}

	private void assignRoleToOrg(String orgOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
	SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
	assignRoleToOrg(orgOid, roleOid, (ActivationType) null, task, result);
	}

	private void assignRoleToOrg(String orgOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
	SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
	modifyOrgAssignment(orgOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, true, result);
	}

	private void unassignRoleFromOrg(String orgOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
	SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
	unassignRoleFromOrg(orgOid, roleOid, (ActivationType) null, task, result);
	}

	private void unassignRoleFromOrg(String orgOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
	SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
	modifyOrgAssignment(orgOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, false, result);
	}

	public static <F extends FocusType> void assertOrgNotAssignedRole(PrismObject<F> focus, String roleOid) {
		assertNotAssigned(focus, roleOid, RoleType.COMPLEX_TYPE);
		//assertNotAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
	}

	//TODO: maybe a replacement for MidpointAsserts.assertNotAssigned()
	//it can be used not only for user
	public static <F extends FocusType> void assertNotAssigned(PrismObject<F> focus, String targetOid, QName refType) {
		F focusType = focus.asObjectable();
		for (AssignmentType assignmentType: focusType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					if (targetOid.equals(targetRef.getOid())) {
						AssertJUnit.fail(focus + " does have assigned "+refType.getLocalPart()+" "+targetOid+" while not expecting it");
					}
				}
			}
		}
	}




}