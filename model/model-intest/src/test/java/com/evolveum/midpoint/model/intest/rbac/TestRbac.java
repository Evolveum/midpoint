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
package com.evolveum.midpoint.model.intest.rbac;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRbac extends AbstractInitializedModelIntegrationTest {
	
	protected static final File TEST_DIR = new File("src/test/resources", "rbac");
	
	protected static final File ROLE_ADRIATIC_PIRATE_FILE = new File(TEST_DIR, "role-adriatic-pirate.xml");
	protected static final String ROLE_ADRIATIC_PIRATE_OID = "12345678-d34d-b33f-f00d-5555555566aa";

	protected static final File ROLE_BLACK_SEA_PIRATE_FILE = new File(TEST_DIR, "role-black-sea-pirate.xml");
	protected static final String ROLE_BLACK_SEA_PIRATE_OID = "12345678-d34d-b33f-f00d-5555555566bb";
	
	protected static final File ROLE_INDIAN_OCEAN_PIRATE_FILE = new File(TEST_DIR, "role-indian-ocean-pirate.xml");
	protected static final String ROLE_INDIAN_OCEAN_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556610";
	
	protected static final File ROLE_HONORABILITY_FILE = new File(TEST_DIR, "role-honorability.xml");
	protected static final String ROLE_HONORABILITY_OID = "12345678-d34d-b33f-f00d-555555557701";
	
	protected static final File ROLE_CLERIC_FILE = new File(TEST_DIR, "role-cleric.xml");
	protected static final String ROLE_CLERIC_OID = "12345678-d34d-b33f-f00d-555555557702";
	
	protected static final File ROLE_WANNABE_FILE = new File(TEST_DIR, "role-wannabe.xml");
	protected static final String ROLE_WANNABE_OID = "12345678-d34d-b33f-f00d-555555557703";
	
	protected static final File ROLE_HONORABLE_WANNABE_FILE = new File(TEST_DIR, "role-honorable-wannabe.xml");
	protected static final String ROLE_HONORABLE_WANNABE_OID = "12345678-d34d-b33f-f00d-555555557704";

	protected static final File ROLE_GOVERNOR_FILE = new File(TEST_DIR, "role-governor.xml");
	protected static final String ROLE_GOVERNOR_OID = "12345678-d34d-b33f-f00d-555555557705";
	
	protected static final File ROLE_CANNIBAL_FILE = new File(TEST_DIR, "role-cannibal.xml");
	protected static final String ROLE_CANNIBAL_OID = "12345678-d34d-b33f-f00d-555555557706";
	
	protected static final File ROLE_PROJECT_OMNINAMAGER_FILE = new File(TEST_DIR, "role-project-omnimanager.xml");
	protected static final String ROLE_PROJECT_OMNINAMAGER_OID = "f23ab26c-69df-11e6-8330-979c643ea51c";
	
	protected static final File ROLE_WEAK_GOSSIPER_FILE = new File(TEST_DIR, "role-weak-gossiper.xml");
	protected static final String ROLE_WEAK_GOSSIPER_OID = "e8fb2226-7f48-11e6-8cf1-630ce5c3f80b";

	protected static final File ROLE_WEAK_SINGER_FILE = new File(TEST_DIR, "role-weak-singer.xml");
	protected static final String ROLE_WEAK_SINGER_OID = "caa7daf2-dd68-11e6-a780-ef610c7c3a06";

	protected static final File ROLE_IMMUTABLE_FILE = new File(TEST_DIR, "role-immutable.xml");
	protected static final String ROLE_IMMUTABLE_OID = "e53baf94-aa99-11e6-962a-5362ec2dd7df";
	private static final String ROLE_IMMUTABLE_DESCRIPTION = "Role that cannot be modified because there is a modification rule with enforcement action.";

	protected static final File ROLE_IMMUTABLE_GLOBAL_FILE = new File(TEST_DIR, "role-immutable-global.xml");
	protected static final String ROLE_IMMUTABLE_GLOBAL_OID = "e7ba8884-b2f6-11e6-a0b9-d3540dd687d6";
	private static final String ROLE_IMMUTABLE_GLOBAL_DESCRIPTION = "Thou shalt not modify this role!";
	private static final String ROLE_IMMUTABLE_GLOBAL_IDENTIFIER = "GIG001";
	
	protected static final File ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_FILE = new File(TEST_DIR, "role-immutable-description-global.xml");
	protected static final String ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID = "b7ea1c0c-31d6-445e-8949-9f8f4a665b3b";
	private static final String ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_DESCRIPTION = "Thou shalt not modify description of this role!";
	private static final String ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_IDENTIFIER = "GIG001D";

	protected static final File ROLE_NON_ASSIGNABLE_FILE = new File(TEST_DIR, "role-non-assignable.xml");
	protected static final String ROLE_NON_ASSIGNABLE_OID = "db67d2f0-abd8-11e6-9c30-b35abe3e4e3a";

	protected static final File ROLE_SCREAMING_FILE = new File(TEST_DIR, "role-screaming.xml");
	protected static final String ROLE_SCREAMING_OID = "024e204d-ce40-4fe9-ae5a-0da3cbce989a";

	protected static final File ROLE_NON_CREATEABLE_FILE = new File(TEST_DIR, "role-non-createable.xml");
	protected static final String ROLE_NON_CREATEABLE_OID = "c45a25ce-b2e8-11e6-923e-938d2c54d334";
	
	protected static final File ROLE_IMMUTABLE_ASSIGN_FILE = new File(TEST_DIR, "role-immutable-assign.xml");
	protected static final String ROLE_IMMUTABLE_ASSIGN_OID = "a6b10a7c-b57e-11e6-bcb3-1ba47cb07e2e";

	protected static final File ROLE_META_UNTOUCHABLE_FILE = new File(TEST_DIR, "role-meta-untouchable.xml");
	protected static final String ROLE_META_UNTOUCHABLE_OID = "a80c9572-b57d-11e6-80a9-6fdae1dc39bc";

	protected static final File ROLE_META_FOOL_FILE = new File(TEST_DIR, "role-meta-fool.xml");
	protected static final String ROLE_META_FOOL_OID = "2edc5fe4-af3c-11e6-a81e-eb332578ec4f";

	protected static final File ROLE_BLOODY_FOOL_FILE = new File(TEST_DIR, "role-bloody-fool.xml");
	protected static final String ROLE_BLOODY_FOOL_OID = "0a0ac150-af3d-11e6-9901-67fbcbd5bb25";
	
	protected static final File ROLE_TREASURE_GOLD_FILE = new File(TEST_DIR, "role-treasure-gold.xml");
	protected static final String ROLE_TREASURE_GOLD_OID = "00a1db70-5817-11e7-93eb-9305bec579fe";

	protected static final File ROLE_TREASURE_SILVER_FILE = new File(TEST_DIR, "role-treasure-silver.xml");
	protected static final String ROLE_TREASURE_SILVER_OID = "4e237ee4-5817-11e7-8345-8731a29a1fcb";

	protected static final File ROLE_TREASURE_BRONZE_FILE = new File(TEST_DIR, "role-treasure-bronze.xml");
	protected static final String ROLE_TREASURE_BRONZE_OID = "60f9352c-5817-11e7-bc1e-f7e208714c43";

	protected static final File ROLE_ALL_TREASURE_FILE = new File(TEST_DIR, "role-all-treasure.xml");
	protected static final String ROLE_ALL_TREASURE_OID = "7fda5d86-5817-11e7-ac85-3b1cba81d3ef";
	
	protected static final File ROLE_LOOT_DIAMONDS_FILE = new File(TEST_DIR, "role-loot-diamonds.xml");
	protected static final String ROLE_LOOT_DIAMONDS_OID = "974d7156-581c-11e7-916d-03ed3d47d102";
	
	protected static final File ROLE_ALL_LOOT_FILE = new File(TEST_DIR, "role-all-loot.xml");
	protected static final String ROLE_ALL_LOOT_OID = "aaede614-581c-11e7-91bf-db837eb406b7";
	
	protected static final File ROLE_ALL_YOU_CAN_GET_FILE = new File(TEST_DIR, "role-all-you-can-get.xml");
	protected static final String ROLE_ALL_YOU_CAN_GET_OID = "4671874e-5822-11e7-a571-8b43dc7d2876";
	
	protected static final File ORG_PROJECT_RECLAIM_BLACK_PEARL_FILE = new File(TEST_DIR, "org-project-reclaim-black-pearl.xml");
	protected static final String ORG_PROJECT_RECLAIM_BLACK_PEARL_OID = "00000000-8888-6666-0000-200000005000";

	private static final String USER_LEMONHEAD_NAME = "lemonhead";
	private static final String USER_LEMONHEAD_FULLNAME = "Cannibal Lemonhead";
	
	private static final String USER_SHARPTOOTH_NAME = "sharptooth";
	private static final String USER_SHARPTOOTH_FULLNAME = "Cannibal Sharptooth";

	private static final String USER_REDSKULL_NAME = "redskull";
	private static final String USER_REDSKULL_FULLNAME = "Cannibal Redskull";

	private static final String USER_BIGNOSE_NAME = "bignose";
	private static final String USER_BIGNOSE_FULLNAME = "Bignose the Noncannibal";

	private static final String GROUP_FOOLS_NAME = "fools";
	private static final String GROUP_SIMPLETONS_NAME = "simpletons";

	/**
	 * Undefined relation. It is not standard relation not a relation that is in any way configured.
	 */
	private static final QName RELATION_COMPLICATED_QNAME = new QName("http://exmple.com/relation", "complicated");

	private String userLemonheadOid;
	private String userSharptoothOid;
	private String userRedskullOid;
	private String userBignoseOid;
	
	private final String EXISTING_GOSSIP = "Black spot!"; 
	
	protected File getRoleGovernorFile() {
		return ROLE_GOVERNOR_FILE;
	}
	
	protected File getRoleCannibalFile() {
		return ROLE_CANNIBAL_FILE;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		
		repoAddObjectFromFile(ROLE_ADRIATIC_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BLACK_SEA_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_INDIAN_OCEAN_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_HONORABILITY_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_CLERIC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_WANNABE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_HONORABLE_WANNABE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(getRoleGovernorFile(), RoleType.class, initResult);
		repoAddObjectFromFile(getRoleCannibalFile(), RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_PROJECT_OMNINAMAGER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_WEAK_GOSSIPER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_WEAK_SINGER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_IMMUTABLE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_NON_ASSIGNABLE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_SCREAMING_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_META_UNTOUCHABLE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_META_FOOL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BLOODY_FOOL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_TREASURE_SILVER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_TREASURE_BRONZE_FILE, RoleType.class, initResult);
		// ROLE_TREASURE_GOLD is NOT loaded by purpose. It will come in later.
		repoAddObjectFromFile(ROLE_LOOT_DIAMONDS_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_ALL_LOOT_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_ALL_YOU_CAN_GET_FILE, RoleType.class, initResult);

		repoAddObjectFromFile(USER_RAPP_FILE, initResult);

		dummyResourceCtl.addGroup(GROUP_FOOLS_NAME);
		dummyResourceCtl.addGroup(GROUP_SIMPLETONS_NAME);

	}
	
	@Test
    public void test000SanityRolePirate() throws Exception {
		final String TEST_NAME = "test000SanityRolePirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
		PrismObject<RoleType> rolePirate = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);
        
        // THEN
        display("Role pirate", rolePirate);
        IntegrationTestTools.displayXml("Role pirate", rolePirate);
        assertNotNull("No pirate", rolePirate);
        
        PrismAsserts.assertEquivalent(ROLE_PIRATE_FILE, rolePirate);
	}
	
	@Test
    public void test001SanityRoleProjectOmnimanager() throws Exception {
		final String TEST_NAME = "test001SanityRoleProjectOmnimanager";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
		// WHEN
		PrismObject<RoleType> roleOmnimanager = modelService.getObject(RoleType.class, ROLE_PROJECT_OMNINAMAGER_OID, null, task, result);
        
        // THEN
        display("Role omnimanager", roleOmnimanager);
        IntegrationTestTools.displayXml("Role omnimanager", roleOmnimanager);
        assertNotNull("No omnimanager", roleOmnimanager);
        
        ObjectReferenceType targetRef = roleOmnimanager.asObjectable().getInducement().get(0).getTargetRef();
        assertEquals("Wrong targetRef resolutionTime", EvaluationTimeType.RUN, targetRef.getResolutionTime());
        assertNull("targetRef is resolved", targetRef.getOid());
	}
	
	@Test
    public void test010SearchRequestableRoles() throws Exception {
		final String TEST_NAME = "test010SearchRequestableRoles";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = QueryBuilder.queryFor(RoleType.class, prismContext)
				.item(RoleType.F_REQUESTABLE).eq(true)
				.build();

		// WHEN
        List<PrismObject<RoleType>> requestableRoles = modelService.searchObjects(RoleType.class, query, null, task, result);
        
        // THEN
        display("Requestable roles", requestableRoles);
        
        assertEquals("Unexpected number of requestable roles", 3, requestableRoles.size());
	}

	@Test
    public void test101JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test101JackAssignRolePirate";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);

        AssignmentType assignmentType = assertAssignedRole(userAfter, ROLE_PIRATE_OID, task, result);
        assertCreateMetadata(assignmentType, startTs, endTs);
        assertEffectiveActivation(assignmentType, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Caribbean");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}

	protected ModelExecuteOptions getDefaultOptions() {
		return null;
	}

	/**
	 * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
	 * be updated as well. 
	 */
	@Test
    public void test102JackModifyUserLocality() throws Exception {
		final String TEST_NAME = "test102JackModifyUserLocality";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
 		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
 		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
 				EXISTING_GOSSIP);

 		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, getDefaultOptions(), task, result, PrismTestUtil.createPolyString("Tortuga"));
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        AssignmentType assignmentType = assertAssignedRole(userAfter, ROLE_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test110UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test110UnAssignRolePirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedNoRole(userAfter, task, result);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	@Test
    public void test120JackAssignRolePirateWhileAlreadyHasAccount() throws Exception {
		final String TEST_NAME = "test120JackAssignRolePirateWhileAlreadyHasAccount";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
        // Make sure that the account has explicit intent
        account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);
        
        // Make sure that the existing account has the same value as is set by the role
        // This causes problems if the resource does not tolerate duplicate values in deltas. But provisioning
        // should work around that.
        TestUtil.setAttribute(account, 
        		getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
        		DOMUtil.XSD_STRING, prismContext, "Bloody Pirate");
        
		ObjectDelta<UserType> delta = ObjectDelta.createModificationAddReference(UserType.class, USER_JACK_OID, 
				UserType.F_LINK_REF, prismContext, account);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		// We need to switch off the enforcement for this operation. Otherwise we won't be able to create the account
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		modelService.executeChanges(deltas, getDefaultOptions(), task, result);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		        
        // Precondition (simplified)
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "rum");
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
  		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
  		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
  				EXISTING_GOSSIP);
        
        // WHEN
  		displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
		assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // The account already has a value for 'weapon', it should be unchanged.
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "rum");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	
	
	@Test
    public void test121JackAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test121JackAssignAccountImplicitIntent";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        		        
        // Precondition (simplified)
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        
        // WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);		// TODO options?
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
		assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	@Test
    public void test122JackAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test122JackAssignAccountExplicitIntent";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        		        
        // Precondition (simplified)
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        
        // WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);		// TODO options?
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 3);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
		assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	@Test
    public void test127UnAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test127UnAssignAccountImplicitIntent";
        displayTestTile(TEST_NAME);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);			// TODO options?
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
		assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test128UnAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test128UnAssignAccountExplicitIntent";
        displayTestTile(TEST_NAME);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);		// TODO options?
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
		assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test129UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test129UnAssignRolePirate";
        displayTestTile(TEST_NAME);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test130JackAssignRolePirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test130JackAssignRolePirateWithSeaInAssignment";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, extension, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Caribbean, role , with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test132JackUnAssignRolePirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test132JackUnAssignRolePirateWithSeaInAssignment";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, extension, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * The value for sea is set in Adriatic Pirate role extension.
	 */
	@Test
    public void test134JackAssignRoleAdriaticPirate() throws Exception {
		final String TEST_NAME = "test134JackAssignRoleAdriaticPirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ADRIATIC_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_ADRIATIC_PIRATE_OID, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Adriatic, immediately Adriatic, role , with this The Seven Seas while focused on  (in Pirate)");
	}
	
	/**
	 * Check if all the roles are visible in preview changes
	 */
	@Test
    public void test135PreviewChangesEmptyDelta() throws Exception {
		final String TEST_NAME = "test135PreviewChangesEmptyDelta";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        ObjectDelta<UserType> delta = user.createModifyDelta();
        
		// WHEN
        ModelContext<ObjectType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta), getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
        PrismAsserts.assertTripleNoPlus(evaluatedAssignmentTriple);
        PrismAsserts.assertTripleNoMinus(evaluatedAssignmentTriple);
        Collection<? extends EvaluatedAssignment> evaluatedAssignments = evaluatedAssignmentTriple.getZeroSet();
        assertEquals("Wrong number of evaluated assignments", 1, evaluatedAssignments.size());
        EvaluatedAssignment<UserType> evaluatedAssignment = evaluatedAssignments.iterator().next();
        DeltaSetTriple<? extends EvaluatedAssignmentTarget> rolesTriple = evaluatedAssignment.getRoles();
        PrismAsserts.assertTripleNoPlus(rolesTriple);
        PrismAsserts.assertTripleNoMinus(rolesTriple);
        Collection<? extends EvaluatedAssignmentTarget> evaluatedRoles = rolesTriple.getZeroSet();
        assertEquals("Wrong number of evaluated role", 2, evaluatedRoles.size());
        assertEvaluatedRole(evaluatedRoles, ROLE_ADRIATIC_PIRATE_OID);
        assertEvaluatedRole(evaluatedRoles, ROLE_PIRATE_OID);
        
	}

	@Test
    public void test136JackUnAssignRoleAdriaticPirate() throws Exception {
		final String TEST_NAME = "test136JackUnAssignRoleAdriaticPirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Even though we assign Adriatic Pirate role which has a sea set in its extension the
	 * sea set in user's extension should override it.
	 */
	@Test
    public void test137JackAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test137JackAssignRoleAdriaticPirateWithSeaInAssignment";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, extension, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ADRIATIC_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_ADRIATIC_PIRATE_OID, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Adriatic, role , with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test139JackUnAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test139JackUnAssignRoleAdriaticPirateWithSeaInAssignment";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, extension, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test144JackAssignRoleBlackSeaPirate() throws Exception {
		final String TEST_NAME = "test144JackAssignRoleBlackSeaPirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_BLACK_SEA_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_BLACK_SEA_PIRATE_OID, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Marmara Sea, immediately Marmara Sea, role Black Sea, with this The Seven Seas while focused on  (in Pirate)");
	}
	
	@Test
    public void test146JackUnAssignRoleBlackSeaPirate() throws Exception {
		final String TEST_NAME = "test146JackUnAssignRoleBlackSeaPirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test147JackAssignRoleBlackSeaPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test147JackAssignRoleBlackSeaPirateWithSeaInAssignment";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, extension, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_BLACK_SEA_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_BLACK_SEA_PIRATE_OID, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Marmara Sea, role Black Sea, with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test149JackUnAssignRoleBlackSeaPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test149JackUnAssignRoleBlackSeaPirateWithSeaInAssignment";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, extension, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test154JackAssignRoleIndianOceanPirate() throws Exception {
		final String TEST_NAME = "test154JackAssignRoleIndianOceanPirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_INDIAN_OCEAN_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        display("Result", result);
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_INDIAN_OCEAN_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_INDIAN_OCEAN_PIRATE_OID, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Indian Ocean, immediately , role Indian Ocean, with this The Seven Seas while focused on  (in Pirate)");
	}
	
	@Test
    public void test156JackUnAssignRoleIndianOceanPirate() throws Exception {
		final String TEST_NAME = "test156JackUnAssignRoleIndianOceanPirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_INDIAN_OCEAN_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	
	/**
	 * Approver relation is not supposed to give any role privileges.
	 * MID-3580
	 */
	@Test
    public void test160JackAssignRolePirateApprover() throws Exception {
		testJackAssignRolePirateRelationNoPrivs("test160JackAssignRolePirateApprover", SchemaConstants.ORG_APPROVER);
	}
	
	/**
	 * MID-3580
	 */
	@Test
    public void test162JackUnassignRolePirateApprover() throws Exception {
		testJackUnassignRolePirateRelationNoPrivs("test160JackAssignRolePirateApprover", SchemaConstants.ORG_APPROVER);
		
	}

	/**
	 * Owner relation is not supposed to give any role privileges.
	 * MID-3580
	 */
	@Test
    public void test164JackAssignRolePirateOwner() throws Exception {
		testJackAssignRolePirateRelationNoPrivs("test164JackAssignRolePirateOwner", SchemaConstants.ORG_OWNER);
	}
	
	/**
	 * MID-3580
	 */
	@Test
    public void test166JackUnassignRolePirateOwner() throws Exception {
		testJackUnassignRolePirateRelationNoPrivs("test166JackUnassignRolePirateOwner", SchemaConstants.ORG_OWNER);		
	}

	/**
	 * Unknown custom relation is not supposed to give any role privileges.
	 * MID-3580
	 */
	@Test
    public void test168JackAssignRolePirateComplicated() throws Exception {
		testJackAssignRolePirateRelationNoPrivs("test168JackAssignRolePirateComplicated", RELATION_COMPLICATED_QNAME);
	}
	
	/**
	 * MID-3580
	 */
	@Test
    public void test169JackUnassignRolePirateComplicated() throws Exception {
		testJackUnassignRolePirateRelationNoPrivs("test169JackUnassignRolePirateComplicated", RELATION_COMPLICATED_QNAME);		
	}

	public void testJackAssignRolePirateRelationNoPrivs(final String TEST_NAME, QName relation) throws Exception {
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertNoAssignments(userBefore);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN (1)
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, relation, getDefaultOptions(), task, result);
        
        // THEN (1)
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertJackAssignRolePirateRelationNoPrivs(relation);
        
        // WHEN (2)
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);
        
        // THEN (2)
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertJackAssignRolePirateRelationNoPrivs(relation);
	}
	
	private void assertJackAssignRolePirateRelationNoPrivs(QName relation) throws Exception {
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        // Still needs to be as a member, although with the right relation.
        assertRoleMembershipRef(userAfter, relation, ROLE_PIRATE_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
    public void testJackUnassignRolePirateRelationNoPrivs(final String TEST_NAME, QName relation) throws Exception {
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, relation, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertNoAssignments(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	// TODO: assign with owner relation
	// TODO: assign with custom(unknown) relation
    
    /**
     * Import role with dynamic target resolution.
     * Make sure that the reference is NOT resolved at import time.
     */
    @Test
    public void test200ImportRoleAllTreasure() throws Exception {
		final String TEST_NAME = "test200ImportRoleAllTreasure";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        importObjectFromFile(ROLE_ALL_TREASURE_FILE, task, result);
        
        // THEN
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_ALL_TREASURE_OID);
        display("Role after", roleAfter);
        ObjectReferenceType targetRef = roleAfter.asObjectable().getInducement().get(0).getTargetRef();
        assertNull("Unexpected OID in targetRef", targetRef.getOid());
	}
    
    /**
     * General simple test for roles with dynamic target resolution.
     */
    @Test
    public void test202JackAssignRoleAllTreasure() throws Exception {
		final String TEST_NAME = "test202JackAssignRoleAllTreasure";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignments(userBefore, 0);
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ALL_TREASURE_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID, 
        		ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", 
        		"Silver treasure", "Bronze treasure");
	}
    
    /**
     * Add gold treasure role. This should be picked up by the dynamic
     * "all treasure" role.
     */
    @Test
    public void test204AddGoldTreasureAndRecomputeJack() throws Exception {
		final String TEST_NAME = "test204AddGoldTreasureAndRecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        addObject(ROLE_TREASURE_GOLD_FILE);
                
		// WHEN
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID, 
        		ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", 
        		"Silver treasure", "Bronze treasure", "Golden treasure");
	}
    
    /**
     * MID-3966
     */
    @Test
    public void test206JackAssignRoleAllLoot() throws Exception {
		final String TEST_NAME = "test206JackAssignRoleAllLoot";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ALL_LOOT_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID, 
        		ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID,
        		ROLE_ALL_LOOT_OID, ROLE_LOOT_DIAMONDS_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", 
        		"Silver treasure", "Bronze treasure", "Golden treasure", "Diamond loot");
	}
    
    @Test
    public void test208JackUnassignRoleAllLoot() throws Exception {
		final String TEST_NAME = "test208JackUnassignRoleAllLoot";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ALL_LOOT_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID, 
        		ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", 
        		"Silver treasure", "Bronze treasure", "Golden treasure");
	}
    
    @Test
    public void test209JackUnassignRoleAllTreasure() throws Exception {
		final String TEST_NAME = "test209JackUnassignRoleAllTreasure";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ALL_TREASURE_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 0);

        assertNoDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
	}
    
    /**
     * MID-3966
     */
    @Test(enabled=false) // MID-3966
    public void test210JackAssignRoleAllYouCanGet() throws Exception {
		final String TEST_NAME = "test210JackAssignRoleAllYouCanGet";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ALL_YOU_CAN_GET_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_YOU_CAN_GET_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_ALL_YOU_CAN_GET_OID, 
        		ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID,
        		ROLE_LOOT_DIAMONDS_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", 
        		"Silver treasure", "Bronze treasure", "Golden treasure", "Diamond loot");
	}
    
    @Test(enabled=false) // MID-3966
    public void test219JackUnassignRoleAllYouCanGet() throws Exception {
		final String TEST_NAME = "test219JackUnassignRoleAllYouCanGet";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ALL_YOU_CAN_GET_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 0);

        assertNoDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	//////////////////////
	// Following tests use POSITIVE enforcement mode
	/////////////////////
	
	@Test
    public void test501JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test501JackAssignRolePirate";
        displayTestTile(TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
	}
	
	/**
	 * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
	 * be updated as well. 
	 */
	@Test
    public void test502JackModifyUserLocality() throws Exception {
		final String TEST_NAME = "test502JackModifyUserLocality";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
  		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
  		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
  				EXISTING_GOSSIP);
        
        // WHEN
  		displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, getDefaultOptions(), task, result, createPolyString("Isla de Muerta"));
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);

		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen",
        		"Jack Sparrow is the best pirate Tortuga has ever seen", // Positive enforcement. This vales are not removed.
        		EXISTING_GOSSIP);
	}
	
	/**
	 * Assignment policy is POSITIVE, therefore the account should remain.
	 */
	@Test
    public void test510UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test510UnAssignRolePirate";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);

		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen", // Positive enforcement. This vales are not removed.
        		"Jack Sparrow is the best pirate Tortuga has ever seen", // Positive enforcement. This vales are not removed.
        		EXISTING_GOSSIP);
	}
	
	/**
	 * This should go fine without any policy violation error.
	 */
	@Test
    public void test511DeleteAccount() throws Exception {
		final String TEST_NAME = "test511DeleteAccount";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = userJack.asObjectable().getLinkRef().iterator().next().getOid();
        
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountOid, prismContext);
        // Use modification of user to delete account. Deleting account directly is tested later.
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteReference(UserType.class, USER_JACK_OID, UserType.F_LINK_REF, prismContext, accountOid);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
		// WHEN
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test520JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test520JackAssignRolePirate";
        displayTestTile(TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen");
	}
	
	@Test
    public void test521JackUnassignRolePirateDeleteAccount() throws Exception {
		final String TEST_NAME = "test521JackUnassignRolePirateDeleteAccount";
        displayTestTile(TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, false));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = userJack.asObjectable().getLinkRef().iterator().next().getOid();        
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountOid, prismContext);
        // This all goes in the same context with user, explicit unlink should not be necessary
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
        // WHEN
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test530JackAssignRoleCleric() throws Exception {
		final String TEST_NAME = "test530JackAssignRoleCleric";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_CLERIC_OID, getDefaultOptions(), task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_CLERIC_OID, task, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Holy man");
	}
	
	@Test
    public void test532JackModifyAssignmentRoleCleric() throws Exception {
		final String TEST_NAME = "test532JackModifyAssignmentRoleCleric";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = getObject(UserType.class, USER_JACK_OID);
        
        ItemPath itemPath = new ItemPath(
        		new NameItemPathSegment(UserType.F_ASSIGNMENT),
        		new IdItemPathSegment(user.asObjectable().getAssignment().get(0).getId()),
        		new NameItemPathSegment(AssignmentType.F_DESCRIPTION));
		ObjectDelta<UserType> assignmentDelta = ObjectDelta.createModificationReplaceProperty(
        		UserType.class, USER_JACK_OID, itemPath, prismContext, "soul");
        
        // WHEN
		displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(assignmentDelta), getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_CLERIC_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_CLERIC_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Holy soul");
	}
	
	@Test
    public void test539JackUnAssignRoleCleric() throws Exception {
		final String TEST_NAME = "test539JackUnAssignRoleCleric";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = getObject(UserType.class, USER_JACK_OID);
        
        AssignmentType assignmentType = new AssignmentType();
        assignmentType.setId(user.asObjectable().getAssignment().get(0).getId());
		ObjectDelta<UserType> assignmentDelta = ObjectDelta.createModificationDeleteContainer(
        		UserType.class, USER_JACK_OID, UserType.F_ASSIGNMENT, prismContext, assignmentType);
        
        // WHEN
		displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(assignmentDelta), getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
		assertDelegatedRef(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Wannabe role is conditional. All conditions should be false now. So no provisioning should happen.
	 */
	@Test
    public void test540JackAssignRoleWannabe() throws Exception {
		final String TEST_NAME = "test540JackAssignRoleWannabe";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WANNABE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID, task, result);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Remove honorificSuffix. This triggers a condition in Wannabe role inducement.
	 * But as the whole role has false condition nothing should happen.
	 */
	@Test
    public void test541JackRemoveHonorificSuffixWannabe() throws Exception {
		final String TEST_NAME = "test541JackRemoveHonorificSuffixWannabe";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID, task, result);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);

		assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Modify employeeType. This triggers a condition in Wannabe role.
	 */
	@Test
    public void test542JackModifyEmployeeTypeWannabe() throws Exception {
		final String TEST_NAME = "test542JackModifyEmployeeTypeWannabe";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, getDefaultOptions(), task, result, "wannabe");
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID);
		assertDelegatedRef(userAfter);

		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Wannabe Cpt. Where's the rum?");
	}
	
	/**
	 * Remove honorificPrefix. This triggers a condition in Wannabe role and should remove an account.
	 */
	@Test
    public void test543JackRemoveHonorificPrefixWannabe() throws Exception {
		final String TEST_NAME = "test543JackRemoveHonorificPrefixWannabe";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID);
		assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Set honorificSuffix. This triggers conditions and adds a sub-role Honorable Wannabe.
	 */
	@Test
    public void test544JackSetHonorificSuffixWannabe() throws Exception {
		final String TEST_NAME = "test544JackSetHonorificSuffixWannabe";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, getDefaultOptions(), task, result,
				PrismTestUtil.createPolyString("PhD."));
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID, ROLE_HONORABLE_WANNABE_OID);
		assertDelegatedRef(userAfter);

		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Arr!", "Whatever. -- jack");
	}
	
	/**
	 * Restore honorificPrefix. The title should be replaced again.
	 */
	@Test
    public void test545JackRestoreHonorificPrefixWannabe() throws Exception {
		final String TEST_NAME = "test545JackRestoreHonorificPrefixWannabe";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, getDefaultOptions(), task, result,
				PrismTestUtil.createPolyString("captain"));
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID, ROLE_HONORABLE_WANNABE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Wannabe captain Where's the rum?");
	}
	
	/**
	 * The account should be gone - regardless of the condition state.
	 */
	@Test
    public void test549JackUnassignRoleWannabe() throws Exception {
		final String TEST_NAME = "test549JackUnassignRoleWannabe";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_WANNABE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_WANNABE_OID, task, result);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test600JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test600JackAssignRoleJudge";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_JUDGE_OID);
		assertDelegatedRef(userAfter);

        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
//        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
//        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}
	
	/**
	 * Judge and pirate are excluded roles. This should fail.
	 */
	@Test
    public void test602JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test602JackAssignRolePirate";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
	        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_JUDGE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
	}
	
	@Test
    public void test605JackUnAssignRoleJudgeAssignRolePirate() throws Exception {
		final String TEST_NAME = "test605JackUnAssignRoleJudgeAssignRolePirate";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true));
        
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        // Even though Jack is a pirate now the mapping from the role is not applied.
        // the mapping is weak and the account already has a value for weapon from the times
        // Jack was a judge. So it is unchanged
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen");
	}
	
	@Test
    public void test609JackUnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test609JackUnAssignRolePirate";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test610ElaineAssignRoleGovernor() throws Exception {
		final String TEST_NAME = "test610ElaineAssignRoleGovernor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_ELAINE_OID);
        display("User before", userBefore);
        
        assertAssignees(ROLE_GOVERNOR_OID, 0);
        
        // WHEN
        assignRole(USER_ELAINE_OID, ROLE_GOVERNOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_GOVERNOR_OID, task, result);
        assertRoleMembershipRef(userAfter, ROLE_GOVERNOR_OID);
		assertDelegatedRef(userAfter);

		assertAssignedRole(USER_ELAINE_OID, ROLE_GOVERNOR_OID, task, result);
        assertDefaultDummyAccount(ACCOUNT_ELAINE_DUMMY_USERNAME, ACCOUNT_ELAINE_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_ELAINE_DUMMY_USERNAME, "title", "Her Excellency Governor");
        
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}
	
	/**
	 * Governor has maxAssignees=1
	 */
	@Test
    public void test612JackAssignRoleGovernor() throws Exception {
		final String TEST_NAME = "test612JackAssignRoleGovernor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
	        assignRole(USER_JACK_OID, ROLE_GOVERNOR_OID, getDefaultOptions(), task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertNoAssignments(USER_JACK_OID);
        
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}
	
	/**
	 * Governor has maxAssignees=0 for 'approver'
	 */
	@Test
    public void test613JackAssignRoleGovernorAsApprover() throws Exception {

		if (!testMultiplicityConstraintsForNonDefaultRelations()) {
			return;
		}

		final String TEST_NAME = "test613JackAssignRoleGovernorAsApprover";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        try {
	        // WHEN
	        assignRole(USER_JACK_OID, ROLE_GOVERNOR_OID, SchemaConstants.ORG_APPROVER, getDefaultOptions(), task, result);

	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);

        assertNoAssignments(USER_JACK_OID);

        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}

	/**
	 * Role cannibal has minAssignees=2. It is assigned to nobody. Even though assigning
	 * it to lemonhead would result in assignees=1 which violates the policy, the assignment
	 * should pass because it makes the situation better.
	 */
	@Test
    public void test620LemonheadAssignRoleCanibal() throws Exception {
		final String TEST_NAME = "test620LemonheadAssignRoleCanibal";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createUser(USER_LEMONHEAD_NAME, USER_LEMONHEAD_FULLNAME, true);
        addObject(user);
        userLemonheadOid = user.getOid();
        
        assertAssignees(ROLE_CANNIBAL_OID, 0);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(user.getOid(), ROLE_CANNIBAL_OID, task, result);
        assertDefaultDummyAccount(USER_LEMONHEAD_NAME, USER_LEMONHEAD_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_LEMONHEAD_NAME, "title", "Voracious Cannibal");
        
        assertAssignees(ROLE_CANNIBAL_OID, 1);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}
	
	@Test
    public void test622SharptoothAssignRoleCanibal() throws Exception {
		final String TEST_NAME = "test622SharptoothAssignRoleCanibal";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createUser(USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        addObject(user);
        userSharptoothOid = user.getOid();
        
        assertAssignees(ROLE_CANNIBAL_OID, 1);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(user.getOid(), ROLE_CANNIBAL_OID, task, result);
        assertDefaultDummyAccount(USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_SHARPTOOTH_NAME, "title", "Voracious Cannibal");
        
        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}
	
	@Test
    public void test624RedskullAssignRoleCanibal() throws Exception {
		final String TEST_NAME = "test624RedskullAssignRoleCanibal";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createUser(USER_REDSKULL_NAME, USER_REDSKULL_FULLNAME, true);
        addObject(user);
        userRedskullOid = user.getOid();
        
        assertAssignees(ROLE_CANNIBAL_OID, 2);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(user.getOid(), ROLE_CANNIBAL_OID, task, result);
        assertDefaultDummyAccount(USER_REDSKULL_NAME, USER_REDSKULL_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_REDSKULL_NAME, "title", "Voracious Cannibal");
        
        assertAssignees(ROLE_CANNIBAL_OID, 3);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}

	@Test
    public void test625BignoseAssignRoleCanibal() throws Exception {
		final String TEST_NAME = "test625BignoseAssignRoleCanibal";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createUser(USER_BIGNOSE_NAME, USER_BIGNOSE_FULLNAME, true);
        addObject(user);
        userBignoseOid = user.getOid();
        
        assertAssignees(ROLE_CANNIBAL_OID, 3);
        
        try {
	        // WHEN
        	displayWhen(TEST_NAME);
	        assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertNoAssignments(user.getOid());
        
        assertAssignees(ROLE_CANNIBAL_OID, 3);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}
	
	@Test
    public void test627SharptoothUnassignRoleCanibal() throws Exception {
		final String TEST_NAME = "test627SharptoothUnassignRoleCanibal";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        assertAssignees(ROLE_CANNIBAL_OID, 3);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(userSharptoothOid, ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoAssignments(userSharptoothOid);
        assertNoDummyAccount(USER_SHARPTOOTH_NAME);
        
        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}
	
	@Test
    public void test628RedskullUnassignRoleCanibal() throws Exception {
		final String TEST_NAME = "test628RedskullUnassignRoleCanibal";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        assertAssignees(ROLE_CANNIBAL_OID, 2);
        
        try {
	        // WHEN
        	displayWhen(TEST_NAME);
	        unassignRole(userRedskullOid, ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertAssignedRole(userRedskullOid, ROLE_CANNIBAL_OID, task, result);
        assertDefaultDummyAccount(USER_REDSKULL_NAME, USER_REDSKULL_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_REDSKULL_NAME, "title", "Voracious Cannibal");
        
        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}

	@Test
    public void test630RappAssignRoleCannibalAsOwner() throws Exception {

		if (!testMultiplicityConstraintsForNonDefaultRelations()) {
			return;
		}

		final String TEST_NAME = "test630RappAssignRoleCannibalAsOwner";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assertAssignees(ROLE_CANNIBAL_OID, 2);

        // WHEN
		assignRole(USER_RAPP_OID, ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, getDefaultOptions(), task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);
	}

	/**
	 * We are going to violate minAssignees constraint in cannibal role.
	 */
	@Test
    public void test632RappUnassignRoleCannibalAsOwner() throws Exception {

		if (!testMultiplicityConstraintsForNonDefaultRelations()) {
			return;
		}

		final String TEST_NAME = "test632RappUnassignRoleCannibalAsOwner";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		assertAssignees(ROLE_CANNIBAL_OID, 2);
		assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);

        try {
	        // WHEN
        	displayWhen(TEST_NAME);
        	// null namespace to test no-namespace "owner" relation
	        unassignRole(USER_RAPP_OID, ROLE_CANNIBAL_OID, QNameUtil.nullNamespace(SchemaConstants.ORG_OWNER), getDefaultOptions(), task, result);

	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);

		assertAssignees(ROLE_CANNIBAL_OID, 2);
		assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);
	}

	/**
	 * Preparing for MID-3979: adding second owner
	 */
	@Test
	public void test634BignoseAssignRoleCannibalAsOwner() throws Exception {

		if (!testMultiplicityConstraintsForNonDefaultRelations()) {
			return;
		}

		final String TEST_NAME = "test634BignoseAssignRoleCannibalAsOwner";
		displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assertAssignees(ROLE_CANNIBAL_OID, 2);
		assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);

		// WHEN
		assignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, getDefaultOptions(), task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignees(ROLE_CANNIBAL_OID, 2);
		assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 2);
	}

	/**
	 * MID-3979: removing second owner should go well
	 */
	@Test
	public void test636BignoseUnassignRoleCannibalAsOwner() throws Exception {

		if (!testMultiplicityConstraintsForNonDefaultRelations()) {
			return;
		}

		final String TEST_NAME = "test636BignoseUnassignRoleCannibalAsOwner";
		displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assertAssignees(ROLE_CANNIBAL_OID, 2);
		assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 2);

		// WHEN
		unassignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, getDefaultOptions(), task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignees(ROLE_CANNIBAL_OID, 2);
		assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);
	}

	@Test
    public void test649ElaineUnassignRoleGovernor() throws Exception {
		final String TEST_NAME = "test649ElaineUnassignRoleGovernor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_ELAINE_OID);
        display("User before", userBefore);
        
        assertAssignees(ROLE_GOVERNOR_OID, 1);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_ELAINE_OID, ROLE_GOVERNOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
        display("User after", userAfter);
        assertAssignedNoRole(userAfter);
        
        assertAssignees(ROLE_GOVERNOR_OID, 0);
	}

	@Test
    public void test700JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test700JackModifyJudgeRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
	}
	
	@Test
    public void test701JackModifyJudgeDeleteConstructionRecompute() throws Exception {
		final String TEST_NAME = "test701JackModifyJudgeDeleteConstructionRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyRoleDeleteInducement(ROLE_JUDGE_OID, 1111L, true, getDefaultOptions(), task);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        assertTrue("task is not persistent", task.isPersistent());
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);

        waitForTaskFinish(task.getOid(), true);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test702JackModifyJudgeAddInducementHonorabilityRecompute() throws Exception {
		final String TEST_NAME = "test702JackModifyJudgeAddInducementHonorabilityRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyRoleAddInducementTarget(ROLE_JUDGE_OID, ROLE_HONORABILITY_OID, true, getDefaultOptions(), task);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        assertTrue("task is not persistent", task.isPersistent());

        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);

        waitForTaskFinish(task.getOid(), true);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
        
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Honorable");
	}
	
	@Test
    public void test703JackModifyJudgeDeleteInducementHonorabilityRecompute() throws Exception {
		final String TEST_NAME = "test703JackModifyJudgeDeleteInducementHonorabilityRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        modifyRoleDeleteInducementTarget(ROLE_JUDGE_OID, ROLE_HONORABILITY_OID, getDefaultOptions());
        PrismObject<RoleType> roleJudge = modelService.getObject(RoleType.class, ROLE_JUDGE_OID, null, task, result);
        display("Role judge", roleJudge);
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, ModelExecuteOptions.createReconcile(getDefaultOptions()), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Honorable");
	}
	
	@Test
    public void test709JackUnAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test709JackUnAssignRoleJudge";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedNoRole(USER_JACK_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test710JackAssignRoleEmpty() throws Exception {
		final String TEST_NAME = "test710JackAssignRoleEmpty";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_EMPTY_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test712JackModifyEmptyRoleAddInducementPirateRecompute() throws Exception {
		final String TEST_NAME = "test712JackModifyEmptyRoleAddInducementPirateRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyRoleAddInducementTarget(ROLE_EMPTY_OID, ROLE_PIRATE_OID, true, getDefaultOptions(), task);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        assertTrue("task is not persistent", task.isPersistent());

        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);

        waitForTaskFinish(task.getOid(), true);
        
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
	}
	
	@Test
    public void test714JackModifyEmptyRoleDeleteInducementPirateRecompute() throws Exception {
		final String TEST_NAME = "test714JackModifyEmptyRoleDeleteInducementPirateRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        modifyRoleDeleteInducementTarget(ROLE_EMPTY_OID, ROLE_PIRATE_OID, getDefaultOptions());
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test719JackUnAssignRoleEmpty() throws Exception {
		final String TEST_NAME = "test719JackUnAssignRoleEmpty";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_EMPTY_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	@Test
    public void test720JackAssignRoleGovernorTenantRef() throws Exception {
		final String TEST_NAME = "test720JackAssignRoleGovernorTenantRef";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        assertAssignees(ROLE_GOVERNOR_OID, 0);
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignParametricRole(USER_JACK_OID, ROLE_GOVERNOR_OID, null, ORG_SCUMM_BAR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_GOVERNOR_OID, task, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Her Excellency Governor of Scumm Bar");
        
        assertAssignees(ROLE_GOVERNOR_OID, 1);
	}
	
	@Test
    public void test729JackUnassignRoleGovernorTenantRef() throws Exception {
		final String TEST_NAME = "test729JackUnassignRoleGovernorTenantRef";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        assertAssignees(ROLE_GOVERNOR_OID, 1);
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignParametricRole(USER_JACK_OID, ROLE_GOVERNOR_OID, null, ORG_SCUMM_BAR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertAssignees(ROLE_GOVERNOR_OID, 0);
	}
	
	/**
	 * MID-3365
	 */
	@Test
    public void test750JackAssignRoleOmnimanager() throws Exception {
		final String TEST_NAME = "test750JackAssignRoleOmnimanager";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PROJECT_OMNINAMAGER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_PROJECT_OMNINAMAGER_OID, task, result);
        
        assertHasOrg(userAfter, ORG_SAVE_ELAINE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userAfter, ORG_KIDNAP_AND_MARRY_ELAINE_OID, SchemaConstants.ORG_MANAGER);
	}
	
	/**
	 * MID-3365
	 */
	@Test
    public void test755AddProjectAndRecomputeJack() throws Exception {
		final String TEST_NAME = "test755AddProjectAndRecomputeJack";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_PROJECT_OMNINAMAGER_OID, task, result);
        
        addObject(ORG_PROJECT_RECLAIM_BLACK_PEARL_FILE);
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, ModelExecuteOptions.createReconcile(getDefaultOptions()), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PROJECT_OMNINAMAGER_OID, task, result);
        
        assertHasOrg(userAfter, ORG_SAVE_ELAINE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userAfter, ORG_KIDNAP_AND_MARRY_ELAINE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userAfter, ORG_PROJECT_RECLAIM_BLACK_PEARL_OID, SchemaConstants.ORG_MANAGER);
	}

	/**
	 * MID-3365
	 */
	@Test
    public void test759JackUnassignRoleOmnimanager() throws Exception {
		final String TEST_NAME = "test759JackUnassignRoleOmnimanager";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_PROJECT_OMNINAMAGER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertNotAssignedRole(userAfter, ROLE_PROJECT_OMNINAMAGER_OID, task, result);
        
        assertHasNoOrg(userAfter);
	}
	
	/**
	 * Assign role with weak construction. Nothing should happen (no account).
	 * MID-2850
	 */
	@Test
    public void test760JackAssignRoleWeakGossiper() throws Exception {
		final String TEST_NAME = "test760JackAssignRoleWeakGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedNoRole(userBefore);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * MID-2850
	 */
	@Test
    public void test762JackRecompute() throws Exception {
		final String TEST_NAME = "test762JackRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * MID-2850
	 */
	@Test
    public void test763JackReconcile() throws Exception {
		final String TEST_NAME = "test763JackReconcile";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Assign role with normal construction. Account should be created.
	 * Both the normal and the weak construction should be applied.
	 * MID-2850
	 */
	@Test
    public void test764JackAssignRoleSailor() throws Exception {
		final String TEST_NAME = "test764JackAssignRoleSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

	}
	
	/**
	 * MID-2850
	 */
	@Test
    public void test765JackRecompute() throws Exception {
		final String TEST_NAME = "test765JackRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

	}
	
	/**
	 * MID-2850
	 */
	@Test
    public void test766JackReconcile() throws Exception {
		final String TEST_NAME = "test766JackReconcile";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

	}
	
	/**
	 * Unassign role with weak construction. The values given by this construction
	 * should be removed, but the other values should remain.
	 * MID-2850
	 */
	@Test
    public void test767JackUnAssignRoleWeakGossiper() throws Exception {
		final String TEST_NAME = "test767JackUnAssignRoleWeakGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);

	}
	
	/**
	 * MID-2850
	 */
	@Test
    public void test768JackRecompute() throws Exception {
		final String TEST_NAME = "test768JackRecompute";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);

	}
	
	/**
	 * MID-2850
	 */
	@Test
    public void test769JackUnAssignRoleSailor() throws Exception {
		final String TEST_NAME = "test762JackAssignRoleSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedNoRole(userAfter);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

	}
	
	/**
	 * Now assign the normal role first.
	 * MID-2850
	 */
	@Test
    public void test770JackAssignRoleSailor() throws Exception {
		final String TEST_NAME = "test770JackAssignRoleSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);

	}
	
	/**
	 * Assign role with weak construction. It should be activated in a
	 * usual way.
	 * MID-2850
	 */
	@Test
    public void test772JackAssignRoleGossiper() throws Exception {
		final String TEST_NAME = "test772JackAssignRoleGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

	}
	
	/**
	 * Unassign normal role. Even though the role with weak construction remains,
	 * it should not be applied. The account should be gone.
	 * MID-2850
	 */
	@Test
    public void test774JackUnAssignRoleSailor() throws Exception {
		final String TEST_NAME = "test774JackUnAssignRoleSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

	}
	
	/**
	 * Unassign role with weak construction. Nothing should really happen.
	 * MID-2850
	 */
	@Test
    public void test775JackUnAssignRoleGossiper() throws Exception {
		final String TEST_NAME = "test775JackUnAssignRoleGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedNoRole(userAfter);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

	}
	
	
	/**
	 * Assign both roles together (weak and normal).
	 * MID-2850
	 */
	@Test
    public void test778JackAssignRoleGossiperAndSailor() throws Exception {
		final String TEST_NAME = "test778JackAssignRoleGossiperAndSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

	}
	
	/**
	 * Unassign both roles together (weak and normal).
	 * MID-2850
	 */
	@Test
    public void test779JackUnassignRoleGossiperAndSailor() throws Exception {
		final String TEST_NAME = "test779JackUnassignRoleGossiperAndSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedNoRole(userAfter);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

	}
	
	/**
	 * Assign role with weak construction. Nothing should happen (no account).
	 * MID-2850, MID-3662
	 */
	@Test
    public void test780JackAssignRoleWeakSinger() throws Exception {
		final String TEST_NAME = "test780JackAssignRoleWeakSinger";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedNoRole(userBefore);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Assign another role with weak construction. Still nothing 
	 * should happen (no account).
	 * MID-2850, MID-3662
	 */
	@Test
    public void test781JackAssignRoleWeakGossiper() throws Exception {
		final String TEST_NAME = "test781JackAssignRoleWeakGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_SINGER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Assign role with normal construction. Account should be created.
	 * Both the normal and both weak constructions should be applied.
	 * MID-2850, MID-3662
	 */
	@Test
    public void test782JackAssignRoleSailor() throws Exception {
		final String TEST_NAME = "test782JackAssignRoleSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Singer");

	}
	
	/**
	 * Unassign role with normal construction. The account should be gone,
	 * the two weak roles should not be applied. 
	 * MID-2850, MID-3662
	 */
	@Test
    public void test783JackUnassignRoleSailor() throws Exception {
		final String TEST_NAME = "test783JackUnassignRoleSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_SINGER_OID);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * MID-2850, MID-3662
	 */
	@Test
    public void test784JackUnAssignRoleWeakSinger() throws Exception {
		final String TEST_NAME = "test784JackUnAssignRoleWeakSinger";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * MID-2850, MID-3662
	 */
	@Test
    public void test785JackUnAssignRoleGossiper() throws Exception {
		final String TEST_NAME = "test785JackUnAssignRoleGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedNoRole(userAfter);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

	}
	
	/**
	 * Assign both roles with weak construction together. Nothing should happen.
	 * MID-2850, MID-3662
	 */
	@Test
    public void test786JackAssignRoleGossiperAndSinger() throws Exception {
		final String TEST_NAME = "test786JackAssignRoleGossiperAndSinger";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Unassign both roles with weak construction together. Nothing should happen.
	 * MID-2850, MID-3662
	 */
	@Test
    public void test788JackUnassignRoleGossiperAndSinger() throws Exception {
		final String TEST_NAME = "test788JackUnassignRoleGossiperAndSinger";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedNoRole(userAfter);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Assign role with weak construction. Nothing should happen (no account).
	 * Preparation for following tests.
	 * MID-2850, MID-3662
	 */
	@Test
    public void test790JackAssignRoleWeakSinger() throws Exception {
		final String TEST_NAME = "test780JackAssignRoleWeakSinger";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedNoRole(userBefore);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Switch: weak -> weak (strong absent)
	 * Switch one role with weak construction for another role with weak
	 * construction (in one operation). Still nothing should happen.
	 * This is the test that really reproduces MID-3662.
	 * MID-2850, MID-3662
	 */
	@Test
    public void test791JackSwitchRolesGossiperAndSinger() throws Exception {
		final String TEST_NAME = "test791JackSwitchRolesGossiperAndSinger";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * MID-2850, MID-3662
	 */
	@Test
    public void test792JackAssignRoleSailor() throws Exception {
		final String TEST_NAME = "test792JackAssignRoleSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

	}
	
	/**
	 * Switch: weak -> weak (strong present)
	 * Switch one role with weak construction for another role with weak
	 * construction (in one operation). There is also strong construction.
	 * Therefore the account should remain, just the attributes should be
	 * changed.
	 * MID-2850, MID-3662
	 */
	@Test
    public void test793JackSwitchRolesSingerAndGossiper() throws Exception {
		final String TEST_NAME = "test793JackSwitchRolesSingerAndGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_SINGER_OID, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Singer");
	}
	
	/**
	 * Switch: strong -> weak
	 * MID-2850, MID-3662
	 */
	@Test
    public void test794JackSwitchRolesSailorAndGossiper() throws Exception {
		final String TEST_NAME = "test793JackSwitchRolesSingerAndGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_SINGER_OID, ROLE_WEAK_GOSSIPER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Switch: weak -> strong
	 * MID-2850, MID-3662
	 */
	@Test
    public void test795JackSwitchRolesSingerAndSailor() throws Exception {
		final String TEST_NAME = "test795JackSwitchRolesSingerAndSailor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");
	}
	
	/**
	 * Switch: strong -> strong (weak present)
	 * MID-2850, MID-3662
	 */
	@Test
    public void test796JackSwitchRolesSailorAndGovernor() throws Exception {
		final String TEST_NAME = "test796JackSwitchRolesSailorAndGovernor";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_GOVERNOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, true));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_GOVERNOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Her Excellency Governor");
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");
	}
	
	/**
	 * Cleanup
	 * MID-2850, MID-3662
	 */
	@Test
    public void test799JackUnassignGovernorAndWeakGossiper() throws Exception {
		final String TEST_NAME = "test799JackUnassignGovernorAndWeakGossiper";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_GOVERNOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, null, false));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedNoRole(userAfter);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	@Test
    public void test800ModifyRoleImmutable() throws Exception {
		final String TEST_NAME = "test800ModifyRoleImmutable";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
			displayWhen(TEST_NAME);
			modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_OID, RoleType.F_DESCRIPTION,
					getDefaultOptions(), task, result, "whatever");
			
			AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// THEN
            displayThen(TEST_NAME);
            result.computeStatus();
        	TestUtil.assertFailure(result);
        }

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_DESCRIPTION);
	}
	
	/**
	 * This should go well. The global immutable role has enforced modification,
	 * but not addition.
	 */
	@Test
    public void test802AddGlobalImmutableRole() throws Exception {
		final String TEST_NAME = "test802AddGlobalImmutableRole";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_IMMUTABLE_GLOBAL_FILE);
        display("Role before", role);
        
        // WHEN
		displayWhen(TEST_NAME);
		addObject(role, getDefaultOptions(), task, result);

		// THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID);
        display("Role before", roleAfter);
        assertNotNull("No role added", roleAfter);
	}
	
	@Test
    public void test804ModifyRoleImmutableGlobalIdentifier() throws Exception {
		final String TEST_NAME = "test804ModifyRoleImmutableGlobalIdentifier";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
			displayWhen(TEST_NAME);
			modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID, RoleType.F_IDENTIFIER,
					getDefaultOptions(), task, result, "whatever");
			
			AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// THEN
            displayThen(TEST_NAME);
            result.computeStatus();
        	TestUtil.assertFailure(result);
        }

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_GLOBAL_DESCRIPTION);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_IDENTIFIER, ROLE_IMMUTABLE_GLOBAL_IDENTIFIER);
	}
	
	@Test
    public void test805ModifyRoleImmutableGlobalDescription() throws Exception {
		final String TEST_NAME = "test805ModifyRoleImmutableGlobalDescription";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
			displayWhen(TEST_NAME);
			modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID, RoleType.F_DESCRIPTION,
					getDefaultOptions(), task, result, "whatever");
			
			AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// THEN
            displayThen(TEST_NAME);
            result.computeStatus();
        	TestUtil.assertFailure(result);
        }

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_GLOBAL_DESCRIPTION);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_IDENTIFIER, ROLE_IMMUTABLE_GLOBAL_IDENTIFIER);
	}

	/**
	 * This should go well. The global immutable role has enforced modification,
	 * but not addition.
	 */
	@Test
	public void test812AddGlobalImmutableDescriptionRole() throws Exception {
		final String TEST_NAME = "test812AddGlobalImmutableDescriptionRole";
		displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_FILE);
		display("Role before", role);

		// WHEN
		displayWhen(TEST_NAME);
		addObject(role, getDefaultOptions(), task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID);
		display("Role after", roleAfter);
		assertNotNull("No role added", roleAfter);
	}

	/**
	 * This should go well again. The constraint is related to modification of description, not identifier.
	 */
	@Test
	public void test814ModifyRoleImmutableDescriptionGlobalIdentifier() throws Exception {
		final String TEST_NAME = "test814ModifyRoleImmutableDescriptionGlobalIdentifier";
		displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		final String NEW_VALUE = "whatever";
		modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID, RoleType.F_IDENTIFIER,
				getDefaultOptions(), task, result, NEW_VALUE);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID);
		display("Role after", roleAfter);
		assertEquals("Wrong new identifier value", NEW_VALUE, roleAfter.asObjectable().getIdentifier());
	}

	@Test
	public void test815ModifyRoleImmutableGlobalDescription() throws Exception {
		final String TEST_NAME = "test815ModifyRoleImmutableGlobalDescription";
		displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		try {
			// WHEN
			displayWhen(TEST_NAME);
			modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID, RoleType.F_DESCRIPTION,
					getDefaultOptions(), task, result, "whatever");

			AssertJUnit.fail("Unexpected success");
		} catch (PolicyViolationException e) {
			// THEN
			displayThen(TEST_NAME);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}

		PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID);
		PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_DESCRIPTION);
	}
	
	@Test
    public void test826AddNonCreateableRole() throws Exception {
		final String TEST_NAME = "test826AddNonCreateableRole";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_NON_CREATEABLE_FILE);
        display("Role before", role);
        
        try {
	        // WHEN
			displayWhen(TEST_NAME);
			addObject(role, getDefaultOptions(), task, result);
			
			AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// THEN
            displayThen(TEST_NAME);
            result.computeStatus();
        	TestUtil.assertFailure(result);
        }

        assertNoObject(RoleType.class, ROLE_NON_CREATEABLE_OID);
	}

	/**
	 * This role has a metarole which has immutable policy rule in the
	 * inducement.
	 */
	@Test
    public void test827AddImmutableAssignRole() throws Exception {
		final String TEST_NAME = "test827AddImmutableAssignRole";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_IMMUTABLE_ASSIGN_FILE);
        display("Role before", role);
        
        try {
	        // WHEN
			displayWhen(TEST_NAME);
			addObject(role, getDefaultOptions(), task, result);
			
			AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// THEN
            displayThen(TEST_NAME);
            result.computeStatus();
        	TestUtil.assertFailure(result);
        }

        assertNoObject(RoleType.class, ROLE_IMMUTABLE_ASSIGN_OID);
	}
	
	/**
	 * The untouchable metarole has immutable policy rule in the
	 * inducement. So it will apply to member roles, but not to the
	 * metarole itself. Try if we can modify the metarole.
	 */
	@Test
    public void test828ModifyUntouchableMetarole() throws Exception {
		final String TEST_NAME = "test828ModifyUntouchableMetarole";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
		modifyObjectReplaceProperty(RoleType.class, ROLE_META_UNTOUCHABLE_OID, RoleType.F_DESCRIPTION,
				getDefaultOptions(), task, result, "Touche!");
			
		// THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_META_UNTOUCHABLE_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, "Touche!");
	}

	@Test
    public void test830ModifyRoleJudge() throws Exception {
		final String TEST_NAME = "test830ModifyRoleJudge";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		displayWhen(TEST_NAME);
		modifyObjectReplaceProperty(RoleType.class, ROLE_JUDGE_OID, RoleType.F_DESCRIPTION,
				getDefaultOptions(), task, result, "whatever");
			
		// THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_JUDGE_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, "whatever");
	}
	
	@Test
    public void test840AssignRoleNonAssignable() throws Exception {
		final String TEST_NAME = "test840AssignRoleNonAssignable";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("user jack", userJackBefore);
        assertNoAssignments(userJackBefore);
        
        try {
	        // WHEN
			displayWhen(TEST_NAME);
			assignRole(USER_JACK_OID, ROLE_NON_ASSIGNABLE_OID, getDefaultOptions(), task, result);
			
			AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// THEN
            displayThen(TEST_NAME);
            result.computeStatus();
        	TestUtil.assertFailure(result);
        }

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("user after", userJackAfter);
        assertNoAssignments(userJackAfter);
	}

	@Test
    public void test850JackAssignRoleBloodyFool() throws Exception {
		final String TEST_NAME = "test850JackAssignRoleBloodyFool";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_BLOODY_FOOL_OID, getDefaultOptions(), task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_BLOODY_FOOL_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Fool", "Simpleton");

        display("Simpleton groups", getDummyResource().getGroupByName(GROUP_SIMPLETONS_NAME));

        assertDummyGroupMember(null, GROUP_FOOLS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertDummyGroupMember(null, GROUP_SIMPLETONS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

	}

	@Test
    public void test855JackModifyFoolMetaroleDeleteInducement() throws Exception {
		final String TEST_NAME = "test855JackModifyFoolMetaroleDeleteInducement";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<RoleType> roleBefore = getObject(RoleType.class, ROLE_META_FOOL_OID);
        display("Role meta fool before", roleBefore);
        assertInducements(roleBefore, 2);

        // WHEN
        displayWhen(TEST_NAME);
        modifyRoleDeleteInducement(ROLE_META_FOOL_OID, 10002L, false, getDefaultOptions(), task);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_META_FOOL_OID);
        display("Role meta fool after", roleAfter);
        assertInducements(roleAfter, 1);
	}

	@Test
    public void test857JackReconcile() throws Exception {
		final String TEST_NAME = "test857JackReconcile";
        displayTestTile(TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = createTask(TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_BLOODY_FOOL_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

        // Title attribute is tolerant. As there is no delta then there is no reason to remove
        // the Simpleton value.
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Fool", "Simpleton");

        display("Simpleton groups", getDummyResource().getGroupByName(GROUP_SIMPLETONS_NAME));

        assertDummyGroupMember(null, GROUP_FOOLS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        // Group association is non-tolerant. It should be removed.
        assertNoDummyGroupMember(null, GROUP_SIMPLETONS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}

	@Test
	public void test870AssignRoleScreaming() throws Exception {
		final String TEST_NAME = "test870AssignRoleScreaming";
		displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		notificationManager.setDisabled(false);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
		display("user jack", userJackBefore);

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_SCREAMING_OID, getDefaultOptions(), task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
		display("user after", userJackAfter);

		display("dummy transport", dummyTransport);
		List<Message> messages = dummyTransport.getMessages("dummy:policyRuleNotifier");
		assertNotNull("No notification messages", messages);
		assertEquals("Wrong # of notification messages", 1, messages.size());
	}

	protected boolean testMultiplicityConstraintsForNonDefaultRelations() {
		return true;
	}
}
