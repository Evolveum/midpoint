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
package com.evolveum.midpoint.model.intest.mapping;

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
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
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
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
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
public class TestMapping extends AbstractMappingTest {

	// CRIMSON resource has STRONG mappings, non-tolerant attributes, absolute-like mappings
	protected static final File RESOURCE_DUMMY_CRIMSON_FILE = new File(TEST_DIR, "resource-dummy-crimson.xml");
	protected static final String RESOURCE_DUMMY_CRIMSON_OID = "10000000-0000-0000-0000-0000000001c4";
	protected static final String RESOURCE_DUMMY_CRIMSON_NAME = "crimson";
	protected static final String RESOURCE_DUMMY_CRIMSON_NAMESPACE = MidPointConstants.NS_RI;

	// LIGHT CRIMSON is like CRIMSON but slightly stripped down
	protected static final File RESOURCE_DUMMY_LIGHT_CRIMSON_FILE = new File(TEST_DIR, "resource-dummy-light-crimson.xml");
	protected static final String RESOURCE_DUMMY_LIGHT_CRIMSON_OID = "aa5d09b4-54d9-11e7-8ece-576137828ab7";
	protected static final String RESOURCE_DUMMY_LIGHT_CRIMSON_NAME = "lightCrimson";
	protected static final String RESOURCE_DUMMY_LIGHT_CRIMSON_NAMESPACE = MidPointConstants.NS_RI;
	
	// CUSTOM FUNCTION CRIMSON is like CRIMSON but using custom library in script expressions
	protected static final File RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_FILE = new File(TEST_DIR, "resource-dummy-custom-function-crimson.xml");
	protected static final String RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_OID = "aa5d09b4-54d9-11e7-8888-576137828ab7";
	protected static final String RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME = "customFunction";
	protected static final String RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAMESPACE = MidPointConstants.NS_RI;

	protected static final File ROLE_ANTINIHILIST_FILE = new File(TEST_DIR, "role-antinihilist.xml");
	protected static final String ROLE_ANTINIHILIST_OID = "4c5c6c44-bd7d-11e7-99ef-9b82464da93d";
	
	protected static final String USER_GUYBRUSH_PASSWORD_1_CLEAR = "1wannaBEaP1rat3";
	protected static final String USER_GUYBRUSH_PASSWORD_2_CLEAR = "1wannaBEtheP1rat3";

	protected static final String LOCALITY_BLOOD_ISLAND = "Blood Island";
	protected static final String LOCALITY_BOOTY_ISLAND = "Booty Island";
	protected static final String LOCALITY_SCABB_ISLAND = "Scabb Island";

	protected static final String DRINK_VODKA = "vodka";
	protected static final String DRINK_WHISKY = "whisky";
	protected static final String DRINK_BRANDY = "brandy";
	protected static final String DRINK_GRAPPA = "grappa";
	protected static final String DRINK_GIN = "gin";
	protected static final String DRINK_MEZCAL = "mezcal";


	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_CRIMSON_NAME,
				RESOURCE_DUMMY_CRIMSON_FILE, RESOURCE_DUMMY_CRIMSON_OID, initTask, initResult);
		initDummyResourcePirate(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME,
				RESOURCE_DUMMY_LIGHT_CRIMSON_FILE, RESOURCE_DUMMY_LIGHT_CRIMSON_OID, initTask, initResult);
		initDummyResourcePirate(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME,
				RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_FILE, RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_OID, initTask, initResult);
		
		repoAddObjectFromFile(ROLE_ANTINIHILIST_FILE, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
	}

	/**
	 * Blue dummy has WEAK mappings. Let's play a bit with that.
	 */
	@Test
    public void test100ModifyUserAssignAccountDummyBlue() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackAssignAccountDummyBlue";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "SystemConfiguration");
        DummyAccount accountJackBlue = getDummyResource(RESOURCE_DUMMY_BLUE_NAME).getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        String drinkBlue = accountJackBlue.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
        assertNotNull("No blue drink", drinkBlue);
        UUID drinkUuidBlue = UUID.fromString(drinkBlue);
        assertNotNull("No drink UUID", drinkUuidBlue);
        display("Drink UUID", drinkUuidBlue.toString());

        assertAccountShip(userJack, ACCOUNT_JACK_DUMMY_FULLNAME, null, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Cpt. Jack Sparrow"));

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Cpt. Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Jack Sparrow", null, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        displayWhen(TEST_NAME);
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Jack Sparrow", null, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		PrismTestUtil.createPolyString("Black Pearl"));

		// THEN
        assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "Flying Dutchman");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        assertSuccess(result);

		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Jack Sparrow", "Flying Dutchman", getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);

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
	 * There is a weak mapping for ship attribute.
	 * Therefore try to remove the value. The weak mapping should be applied.
	 */
	@Test
    public void test106ModifyAccountShipReplaceEmpty() throws Exception {
		final String TEST_NAME = "test106ModifyAccountShipReplaceEmpty";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext);
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        assertSuccess(result);

		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);

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
    public void test107ModifyAccountShipAgain() throws Exception {
		final String TEST_NAME = "test107ModifyAccountShipAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "HMS Dauntless");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        assertSuccess(result);

		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Jack Sparrow", "HMS Dauntless", getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);

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
	 * There is a weak mapping for ship attribute.
	 * Therefore try to remove the value. The weak mapping should be applied.
	 */
	@Test
    public void test108ModifyAccountShipDelete() throws Exception {
		final String TEST_NAME = "test108ModifyAccountShipDelete";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
        		accountOid, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "HMS Dauntless");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        assertSuccess(result);

		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME), task);

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
    public void test109ModifyUserUnassignAccountBlue() throws Exception {
		final String TEST_NAME = "test109ModifyUserUnassignAccountBlue";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
		assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
        		RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));

		// THEN
        assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Captain Jack Sparrow", null, getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		PrismTestUtil.createPolyString("Black Pearl"));

		// THEN
        assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, getDummyResourceController(RESOURCE_DUMMY_RED_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
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

		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, getDummyResourceController(RESOURCE_DUMMY_RED_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext);
        deltas.add(accountDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        assertSuccess(result);

 		userJack = getUser(USER_JACK_OID);
 		display("User after change execution", userJack);
 		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

 		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

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
    public void test126ModifyAccountShipDelete() throws Exception {
		final String TEST_NAME = "test126ModifyAccountShipDelete";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
        		accountOid, getDummyResourceController(RESOURCE_DUMMY_RED_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
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

		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATION, task, result,
        		PrismTestUtil.createPolyString("Brethren of the Coast"));

		// THEN
        assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");

		assertAccountShip(userJack, "Captain Jack Sparrow", "Brethren of the Coast / Black Pearl", getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);

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

		assertAccountShip(userJack, "Captain Jack Sparrow", "Brethren of the Coast / Black Pearl", false, getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
		assertSuccess(result);

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
        dummyAuditService.assertExecutionDeltas(3);
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
		assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));

		// THEN
        assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result,
        		PrismTestUtil.createPolyString("Fountain of Youth"));

		// THEN
        assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
        assertSuccess(result);

 		userJack = getUser(USER_JACK_OID);
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
    public void test145ModifyAccountLocationDelete() throws Exception {
		final String TEST_NAME = "test145ModifyAccountLocationDelete";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_NAME, task, result,
        		PrismTestUtil.createPolyString("renamedJack"));

		// THEN
        assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);

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
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, true, resourceCtl, task);
	}

	private void assertAccountShip(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, expectedEnabled, resourceCtl, task);
	}

	private void assertAccountLocation(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, expectedShip, true, resourceCtl, task);
	}

	private void assertAccountRename(PrismObject<UserType> userJack, String name, String expectedFullName,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
		assertAccount(userJack, name, expectedFullName, null, null, true, resourceCtl, task);
	}

	private void assertAccount(PrismObject<UserType> userJack, String name, String expectedFullName, String shipAttributeName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
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
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
		assertAccount(userJack, "jack", expectedFullName, attributeName, expectedShip, expectedEnabled, resourceCtl, task);
	}


	@Test
    public void test200ModifyUserAssignAccountDummyCrimson() throws Exception {
		final String TEST_NAME = "test200ModifyUserAssignAccountDummyCrimson";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CRIMSON_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_VODKA, DRINK_WHISKY);

        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

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
        		DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));

	}

	/**
	 * Just make sure that plain recon does not destroy anything.
	 * MID-3661
	 */
	@Test
    public void test204DummyCrimsonReconcile() throws Exception {
		final String TEST_NAME = "test204DummyCrimsonReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));
	}

	/**
	 * IO Error on the resource. The account is not fetched. The operation should fail
	 * and nothing should be destroyed.
	 * MID-3661
	 */
	@Test
    public void test206DummyCrimsonReconcileIOError() throws Exception {
		final String TEST_NAME = "test206DummyCrimsonReconcileIOError";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
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
        		DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));
	}

	/**
	 * Just make sure that second recon run does not destroy anything.
	 * MID-3661
	 */
	@Test
    public void test208DummyCrimsonReconcileAgain() throws Exception {
		final String TEST_NAME = "test208DummyCrimsonReconcileAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));
	}

	/**
	 * MID-3661
	 */
	@Test
    public void test210ModifyUserLocality() throws Exception {
		final String TEST_NAME = "test210ModifyUserLocality";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString(LOCALITY_BLOOD_ISLAND));

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BLOOD_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_VODKA, DRINK_WHISKY, rumFrom(LOCALITY_BLOOD_ISLAND));
	}

	/**
	 * MID-3661
	 */
	@Test
    public void test212ModifyUserLocalityRecon() throws Exception {
		final String TEST_NAME = "test212ModifyUserLocalityRecon";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_GUYBRUSH_OID, new ItemPath(UserType.F_LOCALITY),
        		PrismTestUtil.createPolyString(LOCALITY_SCABB_ISLAND));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		ModelExecuteOptions options = ModelExecuteOptions.createReconcile();
		modelService.executeChanges(deltas, options, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_SCABB_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_VODKA, DRINK_WHISKY, "rum from Scabb Island");
	}

	/**
	 * MID-3661
	 */
	@Test
    public void test214ModifyUserLocalityIOError() throws Exception {
		final String TEST_NAME = "test214ModifyUserLocalityIOError";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString(LOCALITY_BOOTY_ISLAND));

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_VODKA, DRINK_WHISKY, "rum from Scabb Island");
	}

	/**
	 * MID-3661
	 */
	@Test
    public void test220NativeModifyDummyCrimsonThenChangePassword() throws Exception {
		final String TEST_NAME = "test220NativeModifyDummyCrimsonThenChangePassword";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_BRANDY, DRINK_GRAPPA);
        display("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserChangePassword(USER_GUYBRUSH_OID, USER_GUYBRUSH_PASSWORD_1_CLEAR, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        assertEncryptedUserPassword(userAfter, USER_GUYBRUSH_PASSWORD_1_CLEAR);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        // location haven't changed and recon was not requested. The mapping was not evaluated.
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_BRANDY, DRINK_GRAPPA);
	}

	@Test
    public void test229ModifyUserUnassignAccountDummyCrimson() throws Exception {
		final String TEST_NAME = "test229ModifyUserUnassignAccountDummyCrimson";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CRIMSON_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		assertNoAssignments(userAfter);
		assertLinks(userAfter, 0);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

	}

	@Test
    public void test250ModifyUserAssignAccountDummyLightCrimson() throws Exception {
		final String TEST_NAME = "test250ModifyUserAssignAccountDummyLightCrimson";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // preconditions
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_LOCALITY, createPolyString(LOCALITY_BOOTY_ISLAND));
        assertNoDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_LIGHT_CRIMSON_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);

	}

	/**
	 * MID-3661, MID-3674
	 */
	@Test
    public void test252NativeModifyDummyLightCrimsonThenReconcile() throws Exception {
		final String TEST_NAME = "test252NativeModifyDummyLightCrimsonThenReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL);

        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));

	}

	/**
	 * Just make sure that plain recon does not destroy anything.
	 * MID-3661, MID-3674
	 */
	@Test
    public void test254DummyLightCrimsonReconcile() throws Exception {
		final String TEST_NAME = "test254DummyLightCrimsonReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));

	}

	/**
	 * IO Error on the resource. The account is not fetched. The operation should fail
	 * and nothing should be destroyed.
	 * MID-3661, MID-3674
	 */
	@Test
    public void test256DummyLightCrimsonReconcileIOError() throws Exception {
		final String TEST_NAME = "test256DummyLightCrimsonReconcileIOError";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertPartialError(result);

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));
	}

	/**
	 * Just make sure that second recon run does not destroy anything.
	 * MID-3661, MID-3674
	 */
	@Test
    public void test258DummyLightCrimsonReconcileAgain() throws Exception {
		final String TEST_NAME = "test258DummyLightCrimsonReconcileAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));
	}

	/**
	 * MID-3661, MID-3674
	 */
	@Test
    public void test260ModifyUserLocality() throws Exception {
		final String TEST_NAME = "test260ModifyUserLocality";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString(LOCALITY_BLOOD_ISLAND));

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BLOOD_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BLOOD_ISLAND));
	}

	/**
	 * MID-3661, MID-3674
	 */
	@Test
    public void test262ModifyUserLocalityRecon() throws Exception {
		final String TEST_NAME = "test262ModifyUserLocalityRecon";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

		// WHEN
        displayWhen(TEST_NAME);
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_GUYBRUSH_OID, new ItemPath(UserType.F_LOCALITY),
        		PrismTestUtil.createPolyString(LOCALITY_SCABB_ISLAND));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		ModelExecuteOptions options = ModelExecuteOptions.createReconcile();
		modelService.executeChanges(deltas, options, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_SCABB_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_SCABB_ISLAND));
	}

	/**
	 * MID-3661, MID-3674
	 */
	@Test
    public void test264ModifyUserLocalityIOError() throws Exception {
		final String TEST_NAME = "test264ModifyUserLocalityIOError";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString(LOCALITY_BOOTY_ISLAND));

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        // TODO: How? Why?
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_SCABB_ISLAND));
	}

	/**
	 * MID-3661, MID-3674
	 */
	@Test
    public void test270NativeModifyDummyLightCrimsonThenChangePassword() throws Exception {
		final String TEST_NAME = "test270NativeModifyDummyLightCrimsonThenChangePassword";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_BRANDY, DRINK_GRAPPA);
        display("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserChangePassword(USER_GUYBRUSH_OID, USER_GUYBRUSH_PASSWORD_2_CLEAR, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        assertEncryptedUserPassword(userAfter, USER_GUYBRUSH_PASSWORD_2_CLEAR);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        // location haven't changed and recon was not requested. The mapping was not evaluated.
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		DRINK_BRANDY, DRINK_GRAPPA);
	}

	@Test
    public void test279ModifyUserUnassignAccountDummyLightCrimson() throws Exception {
		final String TEST_NAME = "test279ModifyUserUnassignAccountDummyLightCrimson";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_LIGHT_CRIMSON_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		assertNoAssignments(userAfter);
		assertLinks(userAfter, 0);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

	}

	/**
	 * MID-3816, MID-4008
	 */
	@Test
    public void test300AssignGuybrushDummyYellow() throws Exception {
		final String TEST_NAME = "test300AssignGuybrushDummyYellow";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		IntegrationTestTools.CONST_DRINK);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
        		IntegrationTestTools.CONST_BLABLA + " administrator -- administrator");
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
        		"Some say elaine -- administrator");
	}

	@Test
    public void test309UnassignGuybrushDummyYellow() throws Exception {
		final String TEST_NAME = "test309UnassignGuybrushDummyYellow";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
		assertNoAssignments(userAfter);
		assertLinks(userAfter, 0);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}
	
	@Test
    public void test400ModifyUserAssignAccountDummyCrimsonCustomFunction() throws Exception {
		final String TEST_NAME = "test400ModifyUserAssignAccountDummyCrimsonCustomFunction";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, USER_GUYBRUSH_USERNAME.toUpperCase(),
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account", dummyAccount);

	}
	
	@Test
	public void test401ModifyUserLocalityDummyCrisomCustomFunction() throws Exception {
		final String TEST_NAME = "test401ModifyUserLocalityDummyCrisomCustomFunction";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
		modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString(LOCALITY_SCABB_ISLAND));
		
		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, USER_GUYBRUSH_USERNAME.toUpperCase(),
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, USER_GUYBRUSH_USERNAME.toUpperCase(),
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_SCABB_ISLAND);
	}
	
	@Test
    public void test402ModifyDrinkDummyCustomFunctionCrimson() throws Exception {
		final String TEST_NAME = "test402modifyDrinkDummyCustomFunctionCrimson";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, createPolyString(LOCALITY_BLOOD_ISLAND));

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
				USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

		
        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME.toUpperCase(),
        		ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        display("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME.toUpperCase(),
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BLOOD_ISLAND);
        // location haven't changed and recon was not requested. The mapping was not evaluated.
        assertDummyAccountAttribute(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME.toUpperCase(),
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		"rum from " + LOCALITY_BLOOD_ISLAND);
	}

	/**
	 * MID-2860
	 */
	@Test
    public void test420AssignAntinihilistToJack() throws Exception {
		final String TEST_NAME = "test420AssignAntinihilistToJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertNoAssignments(userBefore);
        assertLinks(userBefore, 0);

        try {
        	
	        // WHEN
	        displayWhen(TEST_NAME);
	        assignRole(USER_JACK_OID, ROLE_ANTINIHILIST_OID, task, result);
	        
        } catch (ExpressionEvaluationException e) {
        	display("Exception", e);
        	Throwable cause = e.getCause();
        	if (!(cause instanceof AssertionError)) {
        		throw e;
        	}
        }

		// THEN
        displayThen(TEST_NAME);
        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertNoAssignments(userAfter);
        assertLinks(userAfter, 0);
	}
	
	/**
	 * MID-2860
	 */
	@Test
    public void test422AssignAccountAndAntinihilistToJack() throws Exception {
		final String TEST_NAME = "test422AssignAccountAndAntinihilistToJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        assertLinks(userBefore, 1);

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_ANTINIHILIST_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 2);
		assertAssignedRole(userAfter, ROLE_ANTINIHILIST_OID);
        assertLinks(userAfter, 1);
	}

	/**
	 * MID-2860
	 */
	@Test
    public void test425UnassignAntinihilistFromJack() throws Exception {
		final String TEST_NAME = "test425UnassignAntinihilistFromJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);
        assertLinks(userBefore, 1);

        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_ANTINIHILIST_OID, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 1);
		assertNotAssignedRole(userAfter, ROLE_ANTINIHILIST_OID);
        assertLinks(userAfter, 1);
	}
	
	/**
	 * MID-2860
	 */
	@Test
    public void test427UnassignAccountFromJack() throws Exception {
		final String TEST_NAME = "test427UnassignAccountFromJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        assertLinks(userBefore, 1);

        // WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 0);
        assertLinks(userAfter, 0);
	}


	private String rumFrom(String locality) {
		return "rum from " + locality;
	}
}
