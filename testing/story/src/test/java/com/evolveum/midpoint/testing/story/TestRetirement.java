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


import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRetirement extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "retirement");

	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	protected static final QName OPENDJ_ASSOCIATION_GROUP_NAME = new QName(RESOURCE_OPENDJ_NAMESPACE, "group");

	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

	public static final File ORG_RETIRED_FILE = new File(TEST_DIR, "org-retired.xml");
	public static final String ORG_RETIRED_OID = "00000000-8888-6666-0000-100000ffffff";
	public static final String ORG_RETIRED_NAME = "RETIRED";

	public static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
	public static final String ROLE_META_ORG_OID = "10000000-0000-0000-0000-000000006601";

	protected static final String ORG_ROYULA_CARPATHIA_NAME = "Royula Carpathia";
	protected static final String ORG_CORTUV_HRAD_NAME = "Čortův hrád";
	protected static final String ORG_CORTUV_HRAD_NAME2 = "ani zblo";
	protected static final String ORG_VYSNE_VLKODLAKY_NAME = "Vyšné Vlkodlaky";
	protected static final String ORG_ROYULA_DIABOLICA_NAME = "Royula Diábolica";

	protected static final String ORG_TYPE_FUNCTIONAL = "functional";

	protected static final String LDAP_GROUP_INTENT = "group";
	protected static final String LDAP_OU_INTENT = "ou";

	protected static final String USER_TELEKE_USERNAME = "teleke";
	protected static final String USER_TELEKE_GIVEN_NAME = "Felix";
	protected static final String USER_TELEKE_FAMILY_NAME = "Teleke z Tölökö";

	protected static final String USER_GORC_USERNAME = "gorc";
	protected static final String USER_GORC_USERNAME2 = "obluda";
	protected static final String USER_GORC_GIVEN_NAME = "Robert";
	protected static final String USER_GORC_FAMILY_NAME = "Gorc z Gorců";

	protected static final String USER_DEZI_USERNAME = "dezi";
	protected static final String USER_DEZI_GIVEN_NAME = "Vilja";
	protected static final String USER_DEZI_FAMILY_NAME = "Dézi";

	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;

	protected String orgRolyulaCarpathiaOid;
	protected String orgCortuvHradOid;
	protected String orgVysneVlkodlakyOid;
	protected String orgRolyulaDiabolicaOid;
	protected String userGorcOid;

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
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpOrgTree();
        dumpLdap();
	}

	@Test
    public void test050AddOrgRetired() throws Exception {
		final String TEST_NAME = "test050AddOrgRetired";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = PrismTestUtil.parseObject(ORG_RETIRED_FILE);

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

		PrismObject<OrgType> org = getObject(OrgType.class, ORG_RETIRED_OID);
		display("org", org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_ORG_TYPE, ORG_TYPE_FUNCTIONAL);

		String ouShadowOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.GENERIC, LDAP_OU_INTENT);
		PrismObject<ShadowType> ouShadow = getShadowModel(ouShadowOid);
		display("Org retired OU shadow", ouShadow);
		// TODO assert shadow content

		Entry ouEntry = openDJController.fetchEntry(ouShadow.getName().getOrig());
		assertNotNull("No ou LDAP entry for retirement ("+ouShadow.getName().getOrig()+")", ouEntry);
		display("OU retirement entry", openDJController.toHumanReadableLdifoid(ouEntry));
		openDJController.assertObjectClass(ouEntry, "organizationalUnit");

		assertSubOrgs(org, 0);
	}

	@Test
    public void test100AddOrgRoyulaCarpathia() throws Exception {
		final String TEST_NAME = "test100AddOrgRoyulaCarpathia";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
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

        dumpOrgTree();
		dumpLdap();

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_ROYULA_CARPATHIA_NAME, ORG_TOP_OID);
        orgRolyulaCarpathiaOid = orgAfter.getOid();

		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
	}

	@Test
    public void test110AddUserTeleke() throws Exception {
		final String TEST_NAME = "test110AddUserTeleke";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
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

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_ROYULA_CARPATHIA_NAME, ORG_TOP_OID);

		dumpOrgTree();
		dumpLdap();

		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(ORG_TOP_OID, 1);
	}

	@Test
    public void test200AddOrgCortuvHrad() throws Exception {
		final String TEST_NAME = "test200AddOrgCortuvHrad";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
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

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_CORTUV_HRAD_NAME, orgRolyulaCarpathiaOid);
        orgCortuvHradOid = orgAfter.getOid();

		dumpOrgTree();
		dumpLdap();

		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(orgRolyulaCarpathiaOid, 1);
		assertSubOrgs(ORG_TOP_OID, 1);
	}

	@Test
    public void test210AddUserGorc() throws Exception {
		final String TEST_NAME = "test210AddUserGorc";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
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
        userGorcOid = userAfter.getOid();

		dumpOrgTree();
		dumpLdap();
	}

	@Test
    public void test220AddOrgVysneVlkodlaky() throws Exception {
		final String TEST_NAME = "test220AddOrgVysneVlkodlaky";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_VYSNE_VLKODLAKY_NAME, orgCortuvHradOid);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_VYSNE_VLKODLAKY_NAME, orgCortuvHradOid);
        orgVysneVlkodlakyOid = orgAfter.getOid();

		dumpOrgTree();
		dumpLdap();

		assertSubOrgs(orgAfter, 0);
		assertSubOrgs(orgRolyulaCarpathiaOid, 1);
		assertSubOrgs(ORG_TOP_OID, 1);
	}

	@Test
    public void test230AddUserViljaDezi() throws Exception {
		final String TEST_NAME = "test230AddUserViljaDezi";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_DEZI_USERNAME,
        		USER_DEZI_GIVEN_NAME, USER_DEZI_FAMILY_NAME, orgVysneVlkodlakyOid);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Adding user", userBefore);
        addObject(userBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getAndAssertUser(USER_DEZI_USERNAME, ORG_VYSNE_VLKODLAKY_NAME, ORG_CORTUV_HRAD_NAME, ORG_ROYULA_CARPATHIA_NAME);

		dumpOrgTree();
		dumpLdap();
	}

	@Test
    public void test300RetireUserGorc() throws Exception {
		final String TEST_NAME = "test300RetireUserGorc";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add(createAssignmentModification(orgCortuvHradOid, OrgType.COMPLEX_TYPE, null, null, null, false));
		modifications.add(createAssignmentModification(ORG_RETIRED_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userGorcOid, modifications, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<UserType> userAfter = getAndAssertRetiredUser(USER_GORC_USERNAME);
        userGorcOid = userAfter.getOid();
	}

	@Test
    public void test302ReconcileUserGorc() throws Exception {
		final String TEST_NAME = "test302ReconcileUserGorc";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileUser(userGorcOid, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<UserType> userAfter = getAndAssertRetiredUser(USER_GORC_USERNAME);
        userGorcOid = userAfter.getOid();
	}

	@Test
    public void test303ReconcileUserGorcAgain() throws Exception {
		final String TEST_NAME = "test303ReconcileUserGorcAgain";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestRetirement.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileUser(userGorcOid, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
		dumpLdap();

        PrismObject<UserType> userAfter = getAndAssertRetiredUser(USER_GORC_USERNAME);
        userGorcOid = userAfter.getOid();
	}

	private PrismObject<UserType> createUser(String username, String givenName,
			String familyName, String parentOrgOid) throws SchemaException {
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

	private PrismObject<OrgType> createOrg(String name, String parentOrgOid) throws SchemaException {
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

	private PrismObject<UserType> getAndAssertUser(String username, String directOrgGroupname, String... indirectGroupNames) throws SchemaException, CommonException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		PrismObject<UserType> user = findUserByUsername(username);
		display("user", user);

		String shadowOid = getLinkRefOid(user, RESOURCE_OPENDJ_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
		PrismObject<ShadowType> accountShadow = getShadowModel(shadowOid);
		display("Account "+username+" shadow", accountShadow);
		// TODO assert shadow content

		Entry accountEntry = openDJController.searchSingle("uid="+username);
		assertNotNull("No account LDAP entry for "+username, accountEntry);
		display("account entry", openDJController.toHumanReadableLdifoid(accountEntry));
		openDJController.assertObjectClass(accountEntry, "inetOrgPerson");

		return user;
	}

	private PrismObject<UserType> getAndAssertRetiredUser(String username) throws SchemaException, CommonException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		PrismObject<UserType> user = findUserByUsername(username);
		display("user", user);

		String shadowOid = getLinkRefOid(user, RESOURCE_OPENDJ_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
		PrismObject<ShadowType> accountShadow = getShadowModel(shadowOid);
		display("Account "+username+" shadow", accountShadow);
		// TODO assert shadow content

		String dn = "uid=RRR-"+username+",ou=RETIRED,dc=example,dc=com";
		Entry accountEntry = openDJController.fetchEntry(dn);
		assertNotNull("No account LDAP entry for "+username+" ("+dn+")", accountEntry);
		display("account entry", openDJController.toHumanReadableLdifoid(accountEntry));
		openDJController.assertObjectClass(accountEntry, "inetOrgPerson");

		return user;
	}

	private PrismObject<OrgType> getAndAssertFunctionalOrg(String orgName, String directParentOrgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
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
		display("OU GROUP entry", openDJController.toHumanReadableLdifoid(groupEntry));
		openDJController.assertObjectClass(groupEntry, "groupOfUniqueNames");

		assertHasOrg(org, directParentOrgOid);
		assertAssignedOrg(org, directParentOrgOid);

		return org;
	}

	private void dumpLdap() throws DirectoryException {
		display("LDAP server tree", openDJController.dumpTree());
		display("LDAP server content", openDJController.dumpEntries());
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

	private void reconcileAllUsers() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		final Task task = createTask("reconcileAllUsers");
		OperationResult result = task.getResult();
		ResultHandler<UserType> handler = new ResultHandler<UserType>() {
			@Override
			public boolean handle(PrismObject<UserType> object, OperationResult parentResult) {
				try {
					display("reconciling "+object);
					reconcileUser(object.getOid(), task, parentResult);
				} catch (SchemaException | PolicyViolationException | ExpressionEvaluationException
						| ObjectNotFoundException | ObjectAlreadyExistsException | CommunicationException
						| ConfigurationException | SecurityViolationException e) {
					throw new SystemException(e.getMessage(), e);
				}
				return true;
			}
		};
		display("Reconciling all users");
		modelService.searchObjectsIterative(UserType.class, null, handler, null, task, result);
	}

	private void reconcileAllOrgs() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		final Task task = createTask("reconcileAllOrgs");
		OperationResult result = task.getResult();
		ResultHandler<OrgType> handler = new ResultHandler<OrgType>() {
			@Override
			public boolean handle(PrismObject<OrgType> object, OperationResult parentResult) {
				try {
					display("reconciling "+object);
					reconcileOrg(object.getOid(), task, parentResult);
				} catch (SchemaException | PolicyViolationException | ExpressionEvaluationException
						| ObjectNotFoundException | ObjectAlreadyExistsException | CommunicationException
						| ConfigurationException | SecurityViolationException e) {
					throw new SystemException(e.getMessage(), e);
				}
				return true;
			}
		};
		display("Reconciling all orgs");
		modelService.searchObjectsIterative(OrgType.class, null, handler, null, task, result);
	}

}
