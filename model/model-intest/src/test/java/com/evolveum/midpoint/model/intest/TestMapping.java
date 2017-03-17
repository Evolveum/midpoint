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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMapping extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/mapping");
	
	// RED resource has STRONG mappings
	protected static final File RESOURCE_DUMMY_CRIMSON_FILE = new File(TEST_DIR, "resource-dummy-crimson.xml");
	protected static final String RESOURCE_DUMMY_CRIMSON_OID = "10000000-0000-0000-0000-0000000001c4";
	protected static final String RESOURCE_DUMMY_CRIMSON_NAME = "crimson";
	protected static final String RESOURCE_DUMMY_CRIMSON_NAMESPACE = MidPointConstants.NS_RI;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		initDummyResourcePirate(RESOURCE_DUMMY_CRIMSON_NAME, 
				RESOURCE_DUMMY_CRIMSON_FILE, RESOURCE_DUMMY_CRIMSON_OID, initTask, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
	}

	/**
	 * Blue dummy has WEAK mappings. Let's play a bit with that.
	 */
	@Test
    public void test100ModifyUserAssignAccountDummyBlue() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackAssignAccountDummyBlue";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyBlueType);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyBlueType);
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "SystemConfiguration");
        DummyAccount accountJackBlue = dummyResourceBlue.getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        String drinkBlue = accountJackBlue.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
        assertNotNull("No blue drink", drinkBlue);
        UUID drinkUuidBlue = UUID.fromString(drinkBlue);
        assertNotNull("No drink UUID", drinkUuidBlue);
        display("Drink UUID", drinkUuidBlue.toString());
        
        assertAccountShip(userJack, ACCOUNT_JACK_DUMMY_FULLNAME, null, dummyResourceCtlBlue, task);
		assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, 
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Where's the rum? -- Jack Sparrow");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test101ModifyUserFullName() throws Exception {
		final String TEST_NAME = "test101ModifyUserFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Cpt. Jack Sparrow"));
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Cpt. Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Jack Sparrow", null, dummyResourceCtlBlue, task);
		
		// 'quote' is non-tolerant attribute. As the source of the mapping is changed, the current
		// value is no longer legal and the reconciliation has to remove it.
		assertNoDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, 
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test102ModifyUserFullNameRecon() throws Exception {
		final String TEST_NAME = "test102ModifyUserFullNameRecon";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME, 
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Jack Sparrow", null, dummyResourceCtlBlue, task);
		
		// The quote attribute was empty before this operation. So the weak mapping kicks in
		// and sets a new value.
		assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, 
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Where's the rum? -- Captain Jack Sparrow");


        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test104ModifyUserOrganizationalUnit() throws Exception {
		final String TEST_NAME = "test104ModifyUserOrganizationalUnit";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		PrismTestUtil.createPolyString("Black Pearl"));
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test105ModifyAccountShip() throws Exception {
		final String TEST_NAME = "test105ModifyAccountShip";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "Flying Dutchman");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Jack Sparrow", "Flying Dutchman", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * There is a weak mapping for ship attribute. 
	 * Therefore try to remove the value. The weak mapping should be applied.
	 */
	@Test
    public void test106ModifyAccountShipReplaceEmpty() throws Exception {
		final String TEST_NAME = "test106ModifyAccountShipReplaceEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext);
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test107ModifyAccountShipAgain() throws Exception {
		final String TEST_NAME = "test107ModifyAccountShipAgain";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "HMS Dauntless");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Jack Sparrow", "HMS Dauntless", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * There is a weak mapping for ship attribute. 
	 * Therefore try to remove the value. The weak mapping should be applied.
	 */
	@Test
    public void test108ModifyAccountShipDelete() throws Exception {
		final String TEST_NAME = "test108ModifyAccountShipDelete";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "HMS Dauntless");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test109ModifyUserUnassignAccountBlue() throws Exception {
		final String TEST_NAME = "test109ModifyUserUnassignAccountBlue";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, false);
        userDelta.addModificationReplaceProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
        userDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);
                
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	
	/**
	 * Red dummy has STRONG mappings.
	 */
	@Test
    public void test120ModifyUserAssignAccountDummyRed() throws Exception {
		final String TEST_NAME = "test120ModifyUserAssignAccountDummyRed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, 
        		RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);
        
 		assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, 
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "mouth", "pistol");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, 
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Where's the rum? -- red resource");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test121ModifyUserFullName() throws Exception {
		final String TEST_NAME = "test121ModifyUserFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Captain Jack Sparrow", null, dummyResourceCtlRed, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test122ModifyUserOrganizationalUnit() throws Exception {
		final String TEST_NAME = "test122ModifyUserOrganizationalUnit";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		PrismTestUtil.createPolyString("Black Pearl"));
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test123ModifyAccountShip() throws Exception {
		final String TEST_NAME = "test123ModifyAccountShip";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtlRed.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "Flying Dutchman");
        deltas.add(accountDelta);

		// WHEN
        try {
        	modelService.executeChanges(deltas, null, task, result);
        	
        	AssertJUnit.fail("Unexpected success");
        } catch (SchemaException e) {
        	// This is expected
        	display("Expected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
	}

	/**
	 * This test will not fail. It will splice the strong mapping into an empty replace delta.
	 * That still results in a single value and is a valid operation, although it really changes nothing
	 * (replace with the same value that was already there).
	 */
	@Test
    public void test124ModifyAccountShipReplaceEmpty() throws Exception {
		final String TEST_NAME = "test124ModifyAccountShipReplaceEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtlRed.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext);
        deltas.add(accountDelta);
        
        // WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
 		result.computeStatus();
         TestUtil.assertSuccess(result);
         
 		userJack = getUser(USER_JACK_OID);
 		display("User after change execution", userJack);
 		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
 		
 		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);
 		
         // Check audit
         display("Audit", dummyAuditService);
         dummyAuditService.assertSimpleRecordSanity();
         dummyAuditService.assertRecords(2);
         dummyAuditService.assertAnyRequestDeltas();
         dummyAuditService.assertExecutionDeltas(1);
         dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
         dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test126ModifyAccountShipDelete() throws Exception {
		final String TEST_NAME = "test126ModifyAccountShipDelete";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
        		accountOid, dummyResourceCtlRed.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "Black Pearl");
        deltas.add(accountDelta);

     // WHEN
        try {
        	modelService.executeChanges(deltas, null, task, result);
        	
        	AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Expected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
	}
	
	/**
	 * Organization is used in the expression for "ship" attribute. But it is not specified as a source.
	 * Nevertheless the mapping is strong, therefore the result should be applied anyway. 
	 * Reconciliation should be triggered.
	 */
	@Test
    public void test128ModifyUserOrganization() throws Exception {
		final String TEST_NAME = "test128ModifyUserOrganization";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATION, task, result,
        		PrismTestUtil.createPolyString("Brethren of the Coast"));
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Captain Jack Sparrow", "Brethren of the Coast / Black Pearl", dummyResourceCtlRed, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Note: red resource disables account on unsassign, does NOT delete it
	 */
	@Test
    public void test138ModifyUserUnassignAccountRed() throws Exception {
		final String TEST_NAME = "test138ModifyUserUnassignAccountRed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
        
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		String accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
		PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
		
		XMLGregorianCalendar trigStart = clock.currentTimeXMLGregorianCalendar();
        trigStart.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar trigEnd = clock.currentTimeXMLGregorianCalendar();
        trigEnd.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
		assertTrigger(accountRed, RecomputeTriggerHandler.HANDLER_URI, trigStart, trigEnd);
		
		XMLGregorianCalendar disableTimestamp = accountRed.asObjectable().getActivation().getDisableTimestamp();
		TestUtil.assertBetween("Wrong disableTimestamp", start, end, disableTimestamp);

		assertAccountShip(userJack, "Captain Jack Sparrow", "Brethren of the Coast / Black Pearl", false, dummyResourceCtlRed, task);
                
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Note: red resource disables account on unsassign, does NOT delete it
	 * So let's delete the account explicitly to make room for the following tests
	 */
	@Test
    public void test139DeleteAccountRed() throws Exception {
		final String TEST_NAME = "test139DeleteAccountRed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String acccountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createDeleteDelta(ShadowType.class, acccountRedOid, prismContext);
        deltas.add(shadowDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		assertNoLinkedAccount(userJack);
                
        // Check if dummy resource accounts are gone
        assertNoDummyAccount("jack");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	
	/**
	 * Default dummy has combination of NORMAL, WEAK and STRONG mappings.
	 */
	@Test
    public void test140ModifyUserAssignAccountDummyDefault() throws Exception {
		final String TEST_NAME = "test140ModifyUserAssignAccountDummyDefault";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, 
        		RESOURCE_DUMMY_OID, null, true);
        userDelta.addModificationReplaceProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
        userDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType());
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType());
        
        // Check account in dummy resource
        assertDummyAccount(null, "jack", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * fullName mapping is NORMAL, the change should go through
	 */
	@Test
    public void test141ModifyUserFullName() throws Exception {
		final String TEST_NAME = "test141ModifyUserFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccountShip(userJack, "Captain Jack Sparrow", null, dummyResourceCtl, task);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * location mapping is STRONG
	 */
	@Test
    public void test142ModifyUserLocality() throws Exception {
		final String TEST_NAME = "test142ModifyUserLocality";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result,
        		PrismTestUtil.createPolyString("Fountain of Youth"));
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
		
		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test143ModifyAccountLocation() throws Exception {
		final String TEST_NAME = "test143ModifyAccountLocation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME),
        		prismContext, "Davie Jones Locker");
        deltas.add(accountDelta);

		// WHEN
        try {
        	modelService.executeChanges(deltas, null, task, result);
        	
        	AssertJUnit.fail("Unexpected success");
        } catch (SchemaException e) {
        	// This is expected
        	display("Expected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
		
		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
	}

	/**
	 * This test will not fail. It will splice the strong mapping into an empty replace delta.
	 * That still results in a single value and is a valid operation, although it really changes nothing
	 * (replace with the same value that was already there).
	 */
	@Test
    public void test144ModifyAccountLocationReplaceEmpty() throws Exception {
		final String TEST_NAME = "test144ModifyAccountLocationReplaceEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME),
        		prismContext);
        deltas.add(accountDelta);
        
        // WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
 		result.computeStatus();
         TestUtil.assertSuccess(result);
         
 		userJack = getUser(USER_JACK_OID);
 		display("User after change execution", userJack);
 		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
 		
 		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);

         // Check audit
         display("Audit", dummyAuditService);
         dummyAuditService.assertSimpleRecordSanity();
         dummyAuditService.assertRecords(2);
         dummyAuditService.assertAnyRequestDeltas();
         dummyAuditService.assertExecutionDeltas(1);
         dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
         dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test145ModifyAccountLocationDelete() throws Exception {
		final String TEST_NAME = "test145ModifyAccountLocationDelete";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
        		accountOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME),
        		prismContext, "Fountain of Youth");
        deltas.add(accountDelta);

     // WHEN
        try {
        	modelService.executeChanges(deltas, null, task, result);
        	
        	AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Expected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
		
		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
	}
	
	@Test
    public void test148ModifyUserRename() throws Exception {
		final String TEST_NAME = "test148ModifyUserRename";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_NAME, task, result,
        		PrismTestUtil.createPolyString("renamedJack"));
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "renamedJack", "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
		
		assertAccountRename(userJack, "renamedJack", "Captain Jack Sparrow", dummyResourceCtl, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	
	@Test
    public void test149ModifyUserUnassignAccountDummy() throws Exception {
		final String TEST_NAME = "test149ModifyUserUnassignAccountDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "renamedJack", "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
		// Check accountRef
        assertUserNoAccountRefs(userJack);
                
        // Check if dummy resource account is gone
        assertNoDummyAccount("renamedJack");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	
	private void assertAccountShip(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, true, resourceCtl, task);
	}
	
	private void assertAccountShip(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, expectedEnabled, resourceCtl, task);
	}
	
	private void assertAccountLocation(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, expectedShip, true, resourceCtl, task);
	}
	
	private void assertAccountRename(PrismObject<UserType> userJack, String name, String expectedFullName,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException {
		assertAccount(userJack, name, expectedFullName, null, null, true, resourceCtl, task);
	}
	
	private void assertAccount(PrismObject<UserType> userJack, String name, String expectedFullName, String shipAttributeName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException {
		// ship inbound mapping is used, it is strong 
        String accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, task.getResult());
        display("Repo shadow", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, name, resourceCtl.getResource().asObjectable());
        
        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, task.getResult());
        display("Model shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, name, resourceCtl.getResource().asObjectable());
        PrismAsserts.assertPropertyValue(accountModel, 
        		resourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
        		expectedFullName);
        if (shipAttributeName != null) {
	        if (expectedShip == null) {
	        	PrismAsserts.assertNoItem(accountModel, 
	            		resourceCtl.getAttributePath(shipAttributeName));        	
	        } else {
	        	PrismAsserts.assertPropertyValue(accountModel, 
	        		resourceCtl.getAttributePath(shipAttributeName),
	        		expectedShip);
	        }
        }
        
        // Check account in dummy resource
        assertDummyAccount(resourceCtl.getName(), name, expectedFullName, expectedEnabled);
	}
	
	private void assertAccount(PrismObject<UserType> userJack, String expectedFullName, String attributeName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException {
		assertAccount(userJack, "jack", expectedFullName, attributeName, expectedShip, expectedEnabled, resourceCtl, task);
	}
	

	@Test
    public void test200ModifyUserAssignAccountDummyCrimson() throws Exception {
		final String TEST_NAME = "test200ModifyUserAssignAccountDummyCrimson";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CRIMSON_OID, null, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        
        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        
	}
	
	/**
	 * MID-3661
	 */
	@Test
    public void test202NativeModifyDummyCrimsonThenReconcile() throws Exception {
		final String TEST_NAME = "test202NativeModifyDummyCrimsonThenReconcile";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky");
        
        display("Dummy account before", dummyAccountBefore);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky", "rum from Melee Island");
        
	}
	
	/**
	 * Just make sure that plain recon does not destroy anything.
	 * MID-3661
	 */
	@Test
    public void test204DummyCrimsonReconcile() throws Exception {
		final String TEST_NAME = "test204DummyCrimsonReconcile";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		
		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky", "rum from Melee Island");
        
	}

	/**
	 * IO Error on the resource. The account is not fetched. The operation should fail
	 * and nothing should be destroyed.
	 * MID-3661
	 */
	@Test
    public void test206DummyCrimsonReconcileIOError() throws Exception {
		final String TEST_NAME = "test206DummyCrimsonReconcileIOError";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);
        
        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertPartialError(result);
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		
		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky", "rum from Melee Island");
        
	}
	
	/**
	 * Just make sure that second recon run does not destroy anything.
	 * MID-3661
	 */
	@Test
    public void test208DummyCrimsonReconcileAgain() throws Exception {
		final String TEST_NAME = "test208DummyCrimsonReconcileAgain";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		
		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky", "rum from Melee Island");
        
	}

	/**
	 * MID-3661
	 */
	@Test
    public void test210ModifyUserLocality() throws Exception {
		final String TEST_NAME = "test210ModifyUserLocality";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString("Blood Island"));
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		
		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Blood Island");
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky", "rum from Blood Island");
        
	}
	
	/**
	 * MID-3661
	 */
	@Test
    public void test212ModifyUserLocalityRecon() throws Exception {
		final String TEST_NAME = "test212ModifyUserLocalityRecon";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_GUYBRUSH_OID, new ItemPath(UserType.F_LOCALITY), 
        		PrismTestUtil.createPolyString("Scabb Island"));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		ModelExecuteOptions options = ModelExecuteOptions.createReconcile();
		modelService.executeChanges(deltas, options, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		
		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Scabb Island");
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky", "rum from Scabb Island");
        
	}
	
	/**
	 * MID-3661
	 */
	@Test
    public void test214ModifyUserLocalityIOError() throws Exception {
		final String TEST_NAME = "test214ModifyUserLocalityIOError";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);
        
        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString("Booty Island"));
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		
		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        // TODO: How? Why?
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Booty Island");
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"vodka", "whisky", "rum from Scabb Island");
        
	}
	
	/**
	 * MID-3661
	 */
	@Test
    public void test220NativeModifyDummyCrimsonThenChangePassword() throws Exception {
		final String TEST_NAME = "test220NativeModifyDummyCrimsonThenChangePassword";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"brandy", "grappa");
        display("Dummy account before", dummyAccountBefore);
        
        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserChangePassword(USER_GUYBRUSH_OID, "1wannaBEaP1rat3", task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		
		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);
        
        assertEncryptedUserPassword(userAfter, "1wannaBEaP1rat3");
        
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Booty Island");
        // location haven't changed and recon was not requested. The mapping was not evaluated.
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"brandy", "grappa");
	}
	
	@Test
    public void test299ModifyUserUnassignAccountDummyCrimson() throws Exception {
		final String TEST_NAME = "test299ModifyUserUnassignAccountDummyCrimson";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CRIMSON_OID, null, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		assertNoAssignments(userAfter);
		assertLinks(userAfter, 0);
        
        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
	}

	/**
	 * MID-3816
	 */
	@Test
    public void test300AssignGuybrushDummyYellow() throws Exception {
		final String TEST_NAME = "test300AssignGuybrushDummyYellow";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        
        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Bla bla bla administrator -- administrator");
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Some say elaine -- administrator");
	}
	
	@Test
    public void test309UnassignGuybrushDummyYellow() throws Exception {
		final String TEST_NAME = "test309UnassignGuybrushDummyYellow";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, 
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		assertNoAssignments(userAfter);
		assertLinks(userAfter, 0);
        
        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
	}
}
