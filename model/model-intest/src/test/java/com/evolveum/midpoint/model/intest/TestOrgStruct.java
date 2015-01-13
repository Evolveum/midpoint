/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOrgStruct extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/orgstruct");
	
	@Test
    public void test010AddOrgStruct() throws Exception {
		final String TEST_NAME = "test010AddOrgStruct";
        TestUtil.displayTestTile(this, TEST_NAME);
        
		// Dummy, just to be overridden in subclasses
		addOrgStruct();
	}
	
	protected void addOrgStruct() throws Exception {
		// Dummy, just to be overridden in subclasses
	}

	@Test
    public void test051OrgStructSanity() throws Exception {
		final String TEST_NAME = "test051OrgStructSanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // WHEN
        assertMonkeyIslandOrgSanity();
	}
	
	@Test
    public void test052RootOrgQuery() throws Exception {
		final String TEST_NAME = "test052RootOrgQuery";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createRootOrgQuery(prismContext);
        
        // WHEN
        List<PrismObject<OrgType>> rootOrgs = modelService.searchObjects(OrgType.class, query, null, task, result);
        
        // THEN
        assertEquals("Unexpected number of root orgs", 2, rootOrgs.size());
        
        // Post-condition
        assertMonkeyIslandOrgSanity();
	}
	
	/**
	 * Scumm bar org also acts as a role, assigning account on dummy resource.
	 */
	@Test
    public void test101JackAssignScummBar() throws Exception {
		final String TEST_NAME = "test101JackAssignScummBar";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Precondition
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SCUMM_BAR_OID);
        
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test102JackUnassignScummBar() throws Exception {
		final String TEST_NAME = "test102JackUnassignScummBar";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserNoOrg(userJack);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	/**
	 * Assign jack to both functional and project orgstruct.
	 * Assign both orgs at the same time.
	 */
	@Test
    public void test201JackAssignScummBarAndSaveElaine() throws Exception {
		final String TEST_NAME = "test201JackAssignScummBarAndSaveElaine";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
        modifications.add(createAssignmentModification(ORG_SCUMM_BAR_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        modifications.add(createAssignmentModification(ORG_SAVE_ELAINE_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        
        // WHEN
		modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	/**
	 * Assign jack to functional orgstruct again.
	 */
	@Test
    public void test202JackAssignMinistryOfOffense() throws Exception {
		final String TEST_NAME = "test202JackAssignMinistryOfOffense";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test207JackUnAssignScummBar() throws Exception {
		final String TEST_NAME = "test207JackUnAssignScummBar";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SAVE_ELAINE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	@Test
    public void test208JackUnAssignAll() throws Exception {
		final String TEST_NAME = "test208JackUnAssignAll";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignAll(USER_JACK_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserNoOrg(userJack);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	@Test
    public void test210JackAssignMinistryOfOffenseMember() throws Exception {
		final String TEST_NAME = "test210JackAssignMinistryOfOffenseMember";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	@Test
    public void test211JackAssignMinistryOfOffenseMinister() throws Exception {
		final String TEST_NAME = "test211JackAssignMinistryOfOffenseMinister";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	@Test
    public void test212JackUnassignMinistryOfOffenseMember() throws Exception {
		final String TEST_NAME = "test212JackUnassignMinistryOfOffenseMember";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, null, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	@Test
    public void test213JackUnassignMinistryOfOffenseManager() throws Exception {
		final String TEST_NAME = "test213JackUnassignMinistryOfOffenseManager";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserNoOrg(userJack);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	/**
	 * Assign jack to functional orgstruct again. Make him both minister and member.
	 */
	@Test
    public void test301JackAssignMinistryOfOffense() throws Exception {
		final String TEST_NAME = "test301JackAssignMinistryOfOffense";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER, task, result);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	/**
	 * Delete jack while he is still assigned.
	 */
	@Test
    public void test309DeleteJack() throws Exception {
		final String TEST_NAME = "test309DeleteJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        
        // WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        try {
        	PrismObject<UserType> user = getUser(USER_JACK_OID);
        	AssertJUnit.fail("Jack survived!");
        } catch (ObjectNotFoundException e) {
        	// This is expected
        }
	}
	
	protected void assertUserOrg(PrismObject<UserType> user, String... orgOids) throws Exception {
		for (String orgOid: orgOids) {
			assertAssignedOrg(user, orgOid);
	        assertHasOrg(user, orgOid);
		}
		assertHasOrgs(user, orgOids.length);
	}
	
	protected void assertUserNoOrg(PrismObject<UserType> user) throws Exception {
		assertAssignedNoOrg(user);
        assertHasNoOrg(user);
        assertAssignments(user, 0);
        assertHasOrgs(user, 0);

	}

}
