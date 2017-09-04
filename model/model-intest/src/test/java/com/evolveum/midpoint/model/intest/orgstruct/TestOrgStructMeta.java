/*
 * Copyright (c) 2014-2015 Evolveum
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
package com.evolveum.midpoint.model.intest.orgstruct;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Orgstruct test with a meta-role and focus mappings.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOrgStructMeta extends TestOrgStruct {

	private static final File OBJECT_TEMPLATE_ORG_FILE = new File(TEST_DIR, "object-template-org.xml");
	protected static final String OBJECT_TEMPLATE_ORG_OID = "3e62558c-ca0f-11e3-ba83-001e8c717e5b";

	protected static final File ROLE_META_FUNCTIONAL_ORG_FILE = new File(TEST_DIR, "role-meta-functional-org.xml");
    protected static final String ROLE_META_FUNCTIONAL_ORG_OID = "74aac2c8-ca0f-11e3-bb29-001e8c717e5b";

	protected static final File ROLE_ORGANIZED_FILE = new File(TEST_DIR, "role-organized.xml");
    protected static final String ROLE_ORGANIZED_OID = "12345111-1111-2222-1111-121212111001";

    @Override
	protected boolean doAddOrgstruct() {
		return false;
	}

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, initResult);
        setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);

        repoAddObjectFromFile(ROLE_META_FUNCTIONAL_ORG_FILE, initResult);
        repoAddObjectFromFile(ROLE_ORGANIZED_FILE, initResult);
    }

	@Override
	protected ResultHandler<OrgType> getOrgSanityCheckHandler() {
		return (org, parentResult) -> {
			OrgType orgType = org.asObjectable();
			if (orgType.getOrgType().contains("functional")) {
				assertAssigned(org, ROLE_META_FUNCTIONAL_ORG_OID, RoleType.COMPLEX_TYPE);
			} else if (orgType.getOrgType().contains("project")) {
				// Nothing to check (yet)
			} else if (orgType.getOrgType().contains("fictional")) {
				// Nothing to check (yet)
			} else {
				AssertJUnit.fail("Unexpected orgType in "+org);
			}
			return true;
		};
	}

	/**
	 * Add org struct after the object template and metarole has been initialized.
	 */
	@Override
    protected void addOrgStruct() throws Exception {
        List<PrismObject<OrgType>> orgs = (List) PrismTestUtil.parseObjects(ORG_MONKEY_ISLAND_FILE);

        // WHEN
        for (PrismObject<OrgType> org: orgs) {
        	display("Adding", org);
        	addObject(org);
        }

        // Sanity is asserted in the inherited tests
	}

	@Override
	protected void assertUserOrg(PrismObject<UserType> user, String... orgOids) throws Exception {
		super.assertUserOrg(user, orgOids);
		List<PolyStringType> userOrganizations = user.asObjectable().getOrganization();
		List<PolyStringType> expextedOrgs = new ArrayList<PolyStringType>();
		for (String orgOid: orgOids) {
			PrismObject<OrgType> org = getObject(OrgType.class, orgOid);
			List<String> orgType = org.asObjectable().getOrgType();
			if (orgType.contains("functional")) {
				PolyStringType orgName = org.asObjectable().getName();
				assertTrue("Value "+orgName+" not found in user organization property: "+userOrganizations, userOrganizations.contains(orgName));
				if (!expextedOrgs.contains(orgName)) {
					expextedOrgs.add(orgName);
				}
			}
		}
		assertEquals("Wrong number of user organization property values: "+userOrganizations,  expextedOrgs.size(), userOrganizations.size());
	}

	@Override
	protected void assertUserNoOrg(PrismObject<UserType> user) throws Exception {
		super.assertUserNoOrg(user);
		List<PolyStringType> userOrganizations = user.asObjectable().getOrganization();
		assertTrue("Unexpected value in user organization property: "+userOrganizations, userOrganizations.isEmpty());
	}

	// test05x - test7xx inherited from superclass

	@Test
    public void test800JackAssignScummBar() throws Exception {
		final String TEST_NAME = "test800JackAssignScummBar";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        addObject(USER_JACK_FILE);

        // Precondition
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        assignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertUserOrg(user, ORG_SCUMM_BAR_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test802JackAssignOrganized() throws Exception {
		final String TEST_NAME = "test802JackAssignOrganized";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_ORGANIZED_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertUserOrg(user, ORG_SCUMM_BAR_OID);
        assertAssignedRole(user, ROLE_ORGANIZED_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Proud member of F0006");
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Arr!", "I say: Hosting the worst scumm of the Caribbean.");

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test804JackUnAssignOrganized() throws Exception {
		final String TEST_NAME = "test804JackUnAssignOrganized";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_ORGANIZED_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertUserOrg(user, ORG_SCUMM_BAR_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Arr!");

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test809JackUnassignScummBar() throws Exception {
		final String TEST_NAME = "test809JackUnassignScummBar";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        unassignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserNoOrg(userJack);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	/**
	 * Now do the same things as 80x but do it all at once.
	 */
	@Test
    public void test810JackAssignScummBarOrganized() throws Exception {
		final String TEST_NAME = "test810JackAssignScummBarOrganized";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // Precondition
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(ROLE_ORGANIZED_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		modifications.add((createAssignmentModification(ORG_SCUMM_BAR_OID, OrgType.COMPLEX_TYPE, null, null, null, true)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);

        // WHEN
		modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);

        // THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertUserOrg(user, ORG_SCUMM_BAR_OID);
        assertAssignedRole(user, ROLE_ORGANIZED_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Proud member of F0006");

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

    @Test
    public void test890AddFictionalOrg() throws Exception {
        final String TEST_NAME = "test890AddFictionalOrg";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(ORG_FICTIONAL_FILE, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<OrgType> org = getObject(OrgType.class, ORG_FICTIONAL_OID);
        assertNotNull("No fictional org", org);
        display("Fictional org", org);
        PrismAsserts.assertReferenceValue(org.findReference(OrgType.F_PARENT_ORG_REF), ORG_SCUMM_BAR_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity(1);
    }

}
