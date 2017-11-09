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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelServiceContract extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";

	private static String accountJackOid;
	private static String accountJackBlueOid;
	private static String userCharlesOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		InternalMonitor.reset();
//		InternalMonitor.setTraceShadowFetchOperation(true);
//		InternalMonitor.setTraceResourceSchemaOperations(true);
		InternalMonitor.setTrace(InternalOperationClasses.PRISM_OBJECT_CLONES, true);
	}

	@Test
    public void test050GetUserJack() throws Exception {
		final String TEST_NAME = "test050GetUserJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // WHEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);

        // THEN
        display("User jack", userJack);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertUserJack(userJack);

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        assertSteadyResources();
	}

	@Test
    public void test051GetUserBarbossa() throws Exception {
		final String TEST_NAME = "test051GetUserBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // WHEN
        PrismObject<UserType> userBarbossa = modelService.getObject(UserType.class, USER_BARBOSSA_OID, null, task, result);

        // THEN
        display("User barbossa", userBarbossa);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertUser(userBarbossa, USER_BARBOSSA_OID, "barbossa", "Hector Barbossa", "Hector", "Barbossa");

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        userBarbossa.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);

        assertSteadyResources();
	}

    @Test(enabled = true)
    public void test099ModifyUserAddAccountFailing() throws Exception {
        TestUtil.displayTestTitle(this, "test099ModifyUserAddAccountFailing");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test099ModifyUserAddAccountFailing");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        // the following modifies nothing but it is used to produce a user-level notification (LINK_REF by itself causes no such notification)
        PropertyDelta<String> attributeDelta = PropertyDelta.createReplaceDeltaOrEmptyDelta(getUserDefinition(), UserType.F_TELEPHONE_NUMBER, "555-1234");
        userDelta.addModification(attributeDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        getDummyResource().setAddBreakMode(BreakMode.UNSUPPORTED);       // hopefully this does not kick consistency mechanism

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertFailure(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);
        getDummyResource().resetBreakMode();

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);        // actually I don't know why provisioning does not report unsupported operation as a failure...
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        checkDummyTransportMessages("simpleUserNotifier-FAILURE", 1);

        assertSteadyResources();
    }


    @Test
    public void test100ModifyUserAddAccount() throws Exception {
    	final String TEST_NAME = "test100ModifyUserAddAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		// Check accountRef
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User jack after", userAfter);
        assertUserJack(userAfter);
        UserType userJackType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountJackOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountJackOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountJackOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userAfter, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, add account, link
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        List<Message> messages = dummyTransport.getMessages("dummy:accountPasswordNotifier");
        Message message = messages.get(0);          // number of messages was already checked
        assertEquals("Invalid list of recipients", Arrays.asList("recipient@evolveum.com"), message.getTo());
        assertTrue("No account name in account password notification", message.getBody().contains("Password for account jack on Dummy Resource is:"));
//
//        messages = dummyTransport.getMessages("dummy:newAccountsViaExpression");
//        assertNotNull("No messages recorded in dummy transport (expressions)", messages);
//        assertEquals("Invalid number of messages recorded in dummy transport (expressions)", 1, messages.size());
//        message = messages.get(0);
//        assertEquals("Invalid list of recipients (expressions)", Arrays.asList("test1", "test2"), message.getTo());
//        assertEquals("Invalid message subject", "Changed account for jack", message.getSubject());
//        assertEquals("Invalid message body", "Body: Changed account for jack", message.getBody());

        assertSteadyResources();
	}

    @Test
    public void test101GetAccount() throws Exception {
    	final String TEST_NAME = "test101GetAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // Let's do some evil things. Like changing some of the attribute on a resource and see if they will be
        // fetched after get.
        // Also set a value for ignored "water" attribute. The system should cope with that.
        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "The best pirate captain ever");
        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, "cold");
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, null , task, result);

		// THEN
		display("Account", account);
		display("Account def", account.getDefinition());
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyAccountShadowModel(account, accountJackOid, "jack", "Jack Sparrow");

        assertSuccess("getObject result", result);

        account.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);

        IntegrationTestTools.assertAttribute(account,
        		getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
        		"The best pirate captain ever");
        // This one should still be here, even if ignored
        IntegrationTestTools.assertAttribute(account,
        		getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME),
        		"cold");

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(account);
        assertNotNull("No attribute container from "+account, attributesContainer);
        Collection<ResourceAttribute<?>> identifiers = attributesContainer.getPrimaryIdentifiers();
        assertNotNull("No identifiers (null) in attributes container in "+accountJackOid, identifiers);
        assertFalse("No identifiers (empty) in attributes container in "+accountJackOid, identifiers.isEmpty());

        ResourceAttribute<String> fullNameAttr = attributesContainer.findAttribute(dummyResourceCtl.getAttributeFullnameQName());
        PrismAsserts.assertPropertyValue(fullNameAttr, ACCOUNT_JACK_DUMMY_FULLNAME);
        ResourceAttributeDefinition<String> fullNameAttrDef = fullNameAttr.getDefinition();
        display("attribute fullname definition", fullNameAttrDef);
        PrismAsserts.assertDefinition(fullNameAttrDef, dummyResourceCtl.getAttributeFullnameQName(),
        		DOMUtil.XSD_STRING, 1, 1);
        // MID-3144
		if (fullNameAttrDef.getDisplayOrder() == null || fullNameAttrDef.getDisplayOrder() < 100 || fullNameAttrDef.getDisplayOrder() > 400) {
			AssertJUnit.fail("Wrong fullname displayOrder: " + fullNameAttrDef.getDisplayOrder());
		}
		assertEquals("Wrong fullname displayName", "Full Name", fullNameAttrDef.getDisplayName());

        assertSteadyResources();
	}

	@Test
    public void test102GetAccountNoFetch() throws Exception {
        TestUtil.displayTestTitle(this, "test102GetAccountNoFetch");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test102GetAccountNoFetch");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

		// WHEN
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, options , task, result);

		display("Account", account);
		display("Account def", account.getDefinition());
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyAccountShadowRepo(account, accountJackOid, "jack");

        assertSuccess("getObject result", result);

        assertSteadyResources();
	}

	@Test
    public void test103GetAccountRaw() throws Exception {
        TestUtil.displayTestTitle(this, "test103GetAccountRaw");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test103GetAccountRaw");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

		// WHEN
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, options , task, result);

		display("Account", account);
		display("Account def", account.getDefinition());
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyAccountShadowRepo(account, accountJackOid, "jack");

        assertSuccess("getObject result", result);

        assertSteadyResources();
	}

	@Test
	public void test105SearchAccount() throws Exception {
		TestUtil.displayTestTitle(this, "test105SearchAccount");

		// GIVEN
		Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test105SearchAccount");
		OperationResult result = task.getResult();

		// get weapon attribute definition
		PrismObject<ResourceType> dummyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(dummyResource, prismContext);
		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

		QName accountObjectClassQName = dummyResourceCtl.getAccountObjectClassQName();
		ObjectClassComplexTypeDefinition accountObjectClassDefinition = resourceSchema.findObjectClassDefinition(accountObjectClassQName);
		QName weaponQName = dummyResourceCtl.getAttributeWeaponQName();
		ResourceAttributeDefinition<String> weaponDefinition = accountObjectClassDefinition.findAttributeDefinition(weaponQName);

		ObjectQuery q = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(accountObjectClassQName)
				.and().item(new ItemPath(ShadowType.F_ATTRIBUTES, weaponQName), weaponDefinition).eq("rum")
				.build();

		// WHEN
		List<PrismObject<ShadowType>> list = modelService.searchObjects(ShadowType.class, q, null, task, result);

		// THEN
		display("Accounts", list);
		assertEquals("Wrong # of objects returned", 1, list.size());
	}

	@Test
	public void test106SearchAccountWithoutResourceSchema() throws Exception {
		TestUtil.displayTestTitle(this, "test106SearchAccountWithoutResourceSchema");

		// GIVEN
		Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test106SearchAccountWithoutResourceSchema");
		OperationResult result = task.getResult();

		// create weapon attribute definition - NOT SUPPORTED, use only when you know what you're doing!
		QName accountObjectClassQName = dummyResourceCtl.getAccountObjectClassQName();
		QName weaponQName = dummyResourceCtl.getAttributeWeaponQName();
		PrismPropertyDefinition<String> weaponFakeDef = new PrismPropertyDefinitionImpl<>(weaponQName, DOMUtil.XSD_STRING, prismContext);

		ObjectQuery q = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(accountObjectClassQName)
				.and().item(new ItemPath(ShadowType.F_ATTRIBUTES, weaponQName), weaponFakeDef).eq("rum")
				.build();

		// WHEN
		List<PrismObject<ShadowType>> list = modelService.searchObjects(ShadowType.class, q, null, task, result);

		// THEN
		display("Accounts", list);
		assertEquals("Wrong # of objects returned", 1, list.size());
	}

	@Test
    public void test108ModifyUserAddAccountAgain() throws Exception {
        TestUtil.displayTestTitle(this, "test108ModifyUserAddAccountAgain");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test108ModifyUserAddAccountAgain");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

		try {

			// WHEN
			modelService.executeChanges(deltas, null, task, result);

			// THEN
			assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
		} catch (SchemaException e) {
			// This is expected
			e.printStackTrace();
			// THEN
			String message = e.getMessage();
			assertMessageContains(message, "already contains account");
			assertMessageContains(message, "default");
		}

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);

        assertSteadyResources();
    }

	@Test
    public void test109ModifyUserAddAccountAgain() throws Exception {
		final String TEST_NAME = "test109ModifyUserAddAccountAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(null);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

		try {

			// WHEN
			modelService.executeChanges(deltas, null, task, result);

			// THEN
			assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
		} catch (SchemaException e) {
			// This is expected
			// THEN
			String message = e.getMessage();
			assertMessageContains(message, "already contains account");
			assertMessageContains(message, "default");
		}

		assertNoProvisioningScripts();

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);

        assertSteadyResources();
	}

	@Test
    public void test110GetUserResolveAccount() throws Exception {
		final String TEST_NAME = "test110GetUserResolveAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options =
        	SelectorOptions.createCollection(UserType.F_LINK, GetOperationOptions.createResolve());

		// WHEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);

		// THEN
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());

        assertEquals("Unexpected number of accounts", 1, userJackType.getLink().size());
        ShadowType shadow = userJackType.getLink().get(0);
        assertDummyAccountShadowModel(shadow.asPrismObject(), accountOid, "jack", "Jack Sparrow");

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        assertSteadyResources();
	}


	@Test
    public void test111GetUserResolveAccountResource() throws Exception {
		final String TEST_NAME = "test111GetUserResolveAccountResource";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options =
            	SelectorOptions.createCollection(GetOperationOptions.createResolve(),
        			new ItemPath(UserType.F_LINK),
    				new ItemPath(UserType.F_LINK, ShadowType.F_RESOURCE)
        	);

		// WHEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);

		// THEN
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());

        assertEquals("Unexpected number of accounts", 1, userJackType.getLink().size());
        ShadowType shadow = userJackType.getLink().get(0);
        assertDummyAccountShadowModel(shadow.asPrismObject(), accountOid, "jack", "Jack Sparrow");

        assertNotNull("Resource in account was not resolved", shadow.getResource());

        assertSuccess("getObject result", result);

        userJack.checkConsistence(true, true);

        assertSteadyResources();
	}

	@Test
    public void test112GetUserResolveAccountNoFetch() throws Exception {
		final String TEST_NAME = "test112GetUserResolveAccountNoFetch";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        GetOperationOptions getOpts = new GetOperationOptions();
        getOpts.setResolve(true);
        getOpts.setNoFetch(true);
		Collection<SelectorOptions<GetOperationOptions>> options =
        	SelectorOptions.createCollection(UserType.F_LINK, getOpts);

		// WHEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);

		// THEN
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());

        assertEquals("Unexpected number of accounts", 1, userJackType.getLink().size());
        ShadowType shadow = userJackType.getLink().get(0);
        assertDummyAccountShadowRepo(shadow.asPrismObject(), accountOid, "jack");

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        userJack.checkConsistence(true, true);

        assertSteadyResources();
	}

	@Test
    public void test119ModifyUserDeleteAccount() throws Exception {
		final String TEST_NAME = "test119ModifyUserDeleteAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountJackOid);

		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
		displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result, 2);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of linkRefs", 0, userJackType.getLinkRef().size());

		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        	AssertJUnit.fail("Shadow "+accountJackOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, delete account, unlink
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    @Test
    public void test120AddAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test120AddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test120AddAccount");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createAddDelta(account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        accountJackOid = accountDelta.getOid();
        assertNotNull("No account OID in resulting delta", accountJackOid);
		// Check accountRef (should be none)
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // The user is not associated with the account
        assertDummyScriptsAdd(null, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        // We cannot have OID in the request. The OID is not assigned yet at that stage.
        dummyAuditService.assertTarget(accountShadow.getOid(), AuditEventStage.EXECUTION);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);          // there's no password for that account
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);               // account has no owner
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    /**
     * Linking existing account.
     */
	@Test
    public void test121ModifyUserAddAccountRef() throws Exception {
        TestUtil.displayTestTitle(this, "test121ModifyUserAddAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test121ModifyUserAddAccountRef");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);

        // There is strong mapping. Complete account is fetched.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, modify account, link
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }



	@Test
    public void test128ModifyUserDeleteAccountRef() throws Exception {
        TestUtil.displayTestTitle(this, "test128ModifyUserDeleteAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test128ModifyUserDeleteAccountRef");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);

		// Check shadow (if it is unchanged)
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account (if it is unchanged)
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource (if it is unchanged)
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

	@Test
    public void test129DeleteAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test129DeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test129DeleteAccount");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackOid, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);

		// Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(accountJackOid);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);           // there's no link user->account (removed in test128)
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }


	@Test
    public void test130PreviewModifyUserJackAssignAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test130PreviewModifyUserJackAssignAccount");

        // GIVEN
        try{
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test130PreviewModifyUserJackAssignAccount");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("previewChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);

		// TODO: assert context
		// TODO: assert context
		// TODO: assert context

        assertResolvedResourceRefs(modelContext);

        // Check account in dummy resource
        assertNoDummyAccount("jack");

        dummyAuditService.assertNoRecord();
        }catch(Exception ex){
    		LOGGER.info("Exception {}", ex.getMessage(), ex);
    	}

        assertSteadyResources();
	}

	@Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME="test131ModifyUserJackAssignAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 66);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertUserJack(userAfter);
		AssignmentType assignmentType = assertAssignedAccount(userAfter, RESOURCE_DUMMY_OID);
        assertAssignments(userAfter, 1);
        assertModifyMetadata(userAfter, startTime, endTime);
        assertCreateMetadata(assignmentType, startTime, endTime);
        assertLastProvisioningTimestamp(userAfter, startTime, endTime);

        accountJackOid = getSingleLinkOid(userAfter);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userAfter, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

	/**
	 * Modify the account. Some of the changes should be reflected back to the user by inbound mapping.
	 */
	@Test
    public void test132ModifyAccountJackDummy() throws Exception {
		final String TEST_NAME = "test132ModifyAccountJackDummy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountJackOid, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
        accountDelta.addModificationReplaceProperty(
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Queen Anne's Revenge");
        deltas.add(accountDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);
        // There is strong mapping. Complete account is fetched.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		// Fullname inbound mapping is not used because it is weak
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// ship inbound mapping is used, it is strong
		assertEquals("Wrong user locality (orig)", "The crew of Queen Anne's Revenge",
				userJack.asObjectable().getOrganizationalUnit().iterator().next().getOrig());
		assertEquals("Wrong user locality (norm)", "the crew of queen annes revenge",
				userJack.asObjectable().getOrganizationalUnit().iterator().next().getNorm());
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");
        PrismAsserts.assertPropertyValue(accountModel,
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Queen Anne's Revenge");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
        		"Queen Anne's Revenge");

        assertDummyScriptsModify(userJack);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertAnyRequestDeltas();

        dummyAuditService.assertExecutionDeltas(0, 2); // lastProvisioningTimestamp, modify account
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
        		dummyResourceCtl.getAttributeFullnamePath(), "Jack Sparrow");
//        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

        dummyAuditService.assertExecutionDeltas(1, 1);
        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

	/**
	 * MID-3080
	 */
	@Test
    public void test135ModifyUserJackAssignAccountAgain() throws Exception {
		final String TEST_NAME="test135ModifyUserJackAssignAccountAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess("executeChanges result", result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAssignedAccount(userJack, RESOURCE_DUMMY_OID);
        assertAssignments(userJack, 1);

        accountJackOid = getSingleLinkOid(userJack);


		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
        		"Queen Anne's Revenge");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }
	
	@Test
    public void test136JackRecomputeNoChange() throws Exception {
		final String TEST_NAME="test136JackRecomputeNoChange";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
        
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertUserJack(userAfter);
		assertAssignedAccount(userAfter, RESOURCE_DUMMY_OID);
        assertAssignments(userAfter, 1);
        assertLastProvisioningTimestamp(userAfter, null, startTime);

        String accountJackOidAfter = getSingleLinkOid(userAfter);
        assertEquals("Account OID changed", accountJackOid, accountJackOidAfter);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
        		"Queen Anne's Revenge");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
        dummyAuditService.assertSimpleRecordSanity();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

	@Test
    public void test139ModifyUserJackUnassignAccount() throws Exception {
		final String TEST_NAME = "test139ModifyUserJackUnassignAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        assertDummyScriptsDelete();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

	/**
	 * Assignment enforcement is set to POSITIVE for this test. The account should be added.
	 */
	@Test
    public void test141ModifyUserJackAssignAccountPositiveEnforcement() throws Exception {
		final String TEST_NAME = "test141ModifyUserJackAssignAccountPositiveEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

        // This is a second time we assigned this account. Therefore all the scripts in mapping should already
        // be compiled ... check this.
        rememberCounter(InternalCounters.SCRIPT_COMPILE_COUNT);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        // This is a second time we assigned this account. Therefore all the scripts in mapping should already
        // be compiled ... check this.
        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);

        assertSteadyResources();
    }

	/**
	 * Assignment enforcement is set to POSITIVE for this test. The account should remain as it is.
	 */
	@Test
    public void test148ModifyUserJackUnassignAccountPositiveEnforcement() throws Exception {
		final String TEST_NAME = "test148ModifyUserJackUnassignAccountPositiveEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName()
        		+ "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

        //change resource assigment policy to be positive..if they are not applied by projector, the test will fail
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
        // the previous command changes resource, therefore let's explicitly re-read it before test
        // to refresh the cache and not affect the performance results (monitor).
        modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        
        assertSteadyResources();

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        // There is strong mapping. Complete account is fetched.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinked(userJack, accountJackOid);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertNoProvisioningScripts();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();

        // return resource to the previous state..delete assignment enforcement to prevent next test to fail..
        deleteResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
        // the previous command changes resource, therefore let's explicitly re-read it before test
        // to refresh the cache and not affect the performance results (monitor).
        modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        assertSteadyResources();
    }

	/**
	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
	 * Now we will explicitly delete the account.
	 */
	@Test
    public void test149ModifyUserJackDeleteAccount() throws Exception {
		final String TEST_NAME = "test149ModifyUserJackDeleteAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setOid(accountJackOid);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
		userDelta.addModification(accountRefDelta);

		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackOid, prismContext);

		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, delete account, unlink
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	/**
	 * Assignment enforcement is set to RELATTIVE for this test. The account should be added.
	 */
	@Test
    public void test151ModifyUserJackAssignAccountRelativeEnforcement() throws Exception {
		final String TEST_NAME = "test151ModifyUserJackAssignAccountRelativeEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);
		assertResultSerialization(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	/**
	 * Assignment enforcement is set to RELATIVE for this test. The account should be gone.
	 */
	@Test
    public void test158ModifyUserJackUnassignAccountRelativeEnforcement() throws Exception {
		final String TEST_NAME = "test158ModifyUserJackUnassignAccountRelativeEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName()
        		+ "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	/**
	 * Assignment enforcement is set to NONE for this test.
	 */
	@Test
    public void test161ModifyUserJackAssignAccountNoneEnforcement() throws Exception {
		final String TEST_NAME = "test161ModifyUserJackAssignAccountNoneEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyProvisioningScriptsNone();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	@Test
    public void test163ModifyUserJackAddAccountNoneEnforcement() throws Exception {
		final String TEST_NAME = "test163ModifyUserJackAddAccountNoneEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);

		dummyAuditService.clear();
        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, add account, link
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
	}

	@Test
    public void test164ModifyUserJackUnassignAccountNoneEnforcement() throws Exception {
		final String TEST_NAME = "test164ModifyUserJackUnassignAccountNoneEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName()
        		+ "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyProvisioningScriptsNone();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	@Test
    public void test169ModifyUserJackDeleteAccountNoneEnforcement() throws Exception {
		final String TEST_NAME = "test169ModifyUserJackDeleteAccountNoneEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setOid(accountJackOid);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
		userDelta.addModification(accountRefDelta);

		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackOid, prismContext);

		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, delete account, unlink
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	@Test
    public void test180ModifyUserAddAccountFullEnforcement() throws Exception {
		final String TEST_NAME = "test180ModifyUserAddAccountFullEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);

		try {

			// WHEN
			modelService.executeChanges(deltas, null, task, result);

			AssertJUnit.fail("Unexpected executeChanges success");
		} catch (PolicyViolationException e) {
			// This is expected
			display("Expected exception", e);
		}

		// THEN
		result.computeStatus();
        TestUtil.assertFailure("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check that shadow was not created
        assertNoShadow(accountJackOid);

        // Check that dummy resource account was not created
        assertNoDummyAccount("jack");

        assertNoProvisioningScripts();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 0);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
        dummyAuditService.assertTarget(USER_JACK_OID);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
	}

	@Test
    public void test182ModifyUserAddAndAssignAccountPositiveEnforcement() throws Exception {
		final String TEST_NAME = "test182ModifyUserAddAndAssignAccountPositiveEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);

		XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountJackOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountJackOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountJackOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
	}

	/**
	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
	 * Now we will explicitly delete the account.
	 */
	@Test
    public void test189ModifyUserJackUnassignAndDeleteAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test189ModifyUserJackUnassignAndDeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test149ModifyUserJackUnassignAccount");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        // Explicit unlink is not needed here, it should work without it

		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackOid, prismContext);

		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
	}

	/**
	 * We try to both assign an account and modify that account in one operation.
	 * Some changes should be reflected to account (e.g.  weapon) as the mapping is weak, other should be
	 * overridded (e.g. fullname) as the mapping is strong.
	 */
	@Test
    public void test190ModifyUserJackAssignAccountAndModify() throws Exception {
        TestUtil.displayTestTitle(this, "test190ModifyUserJackAssignAccountAndModify");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test190ModifyUserJackAssignAccountAndModify");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        ShadowDiscriminatorObjectDelta<ShadowType> accountDelta = ShadowDiscriminatorObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
        accountDelta.addModificationAddProperty(
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME),
        		"smell");
        deltas.add(accountDelta);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, USER_JACK_USERNAME);
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "smell");
        assertNull("Unexpected loot", dummyAccount.getAttributeValue("loot", Integer.class));

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * We try to modify an assignment of the account and see whether changes will be recorded in the account itself.
     *
     */
    @Test
    public void test191ModifyUserJackModifyAssignment() throws Exception {
    	final String TEST_NAME = "test191ModifyUserJackModifyAssignment";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

        //PrismPropertyDefinition definition = getAssignmentDefinition().findPropertyDefinition(new QName(SchemaConstantsGenerated.NS_COMMON, "accountConstruction"));

        PrismObject<ResourceType> dummyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(dummyResource, prismContext);
        // This explicitly parses the schema, therefore ...
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

        RefinedObjectClassDefinition accountDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String) null);
        PrismPropertyDefinition gossipDefinition = accountDefinition.findPropertyDefinition(new QName(
                "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME));
        assertNotNull("gossip attribute definition not found", gossipDefinition);

        ConstructionType accountConstruction = createAccountConstruction(RESOURCE_DUMMY_OID, null);
        ResourceAttributeDefinitionType radt = new ResourceAttributeDefinitionType();
        radt.setRef(new ItemPathType(new ItemPath(gossipDefinition.getName())));
        MappingType outbound = new MappingType();
        radt.setOutbound(outbound);

        ExpressionType expression = new ExpressionType();
        outbound.setExpression(expression);

        MappingType value = new MappingType();

        PrismProperty<String> property = gossipDefinition.instantiate();
        property.add(new PrismPropertyValue<String>("q"));

        List evaluators = expression.getExpressionEvaluator();
        Collection<JAXBElement<RawType>> collection = StaticExpressionUtil.serializeValueElements(property, null);
        ObjectFactory of = new ObjectFactory();
        for (JAXBElement<RawType> obj : collection) {
            evaluators.add(of.createValue(obj.getValue()));
        }

        value.setExpression(expression);
        radt.setOutbound(value);
        accountConstruction.getAttribute().add(radt);

        PrismObject<UserType> jackBefore = getUserFromRepo(USER_JACK_OID);
        assertEquals("Wrong # of assignments", 1, jackBefore.asObjectable().getAssignment().size());
        Long assignmentId = jackBefore.asObjectable().getAssignment().get(0).getId();
        ObjectDelta<UserType> accountAssignmentUserDelta =
                createReplaceAccountConstructionUserDelta(USER_JACK_OID, assignmentId, accountConstruction);
        deltas.add(accountAssignmentUserDelta);

        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<UserType> userJackOld = getUser(USER_JACK_OID);
        display("User before change execution", userJackOld);
        display("Deltas to execute execution", deltas);

        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        // First fetch: initial account read
        // Second fetch: fetchback after modification to correctly process inbound
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, USER_JACK_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
        display(dummyAccount.debugDump());
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "q");
        //assertEquals("Missing or incorrect attribute value", "soda", dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, String.class));

        assertDummyScriptsModify(userJack, true);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        Collection<ObjectDeltaOperation<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
        assertEquals("Wrong number of execution deltas", 2, auditExecutionDeltas.size());
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test195ModifyUserJack() throws Exception {
    	final String TEST_NAME = "test195ModifyUserJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Magnificent Captain Jack Sparrow"));

		// THEN
        displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Magnificent Captain Jack Sparrow");
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);

        assertDummyScriptsModify(userJack);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);

        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class,
        		UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
        // We have full account here. It is loaded because of strong mapping.
        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
        		dummyResourceCtl.getAttributeFullnamePath(), "Cpt. Jack Sparrow");

        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test196ModifyUserJackLocationEmpty() throws Exception {
    	final String TEST_NAME = "test196ModifyUserJackLocationEmpty";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Magnificent Captain Jack Sparrow", "Jack", "Sparrow", null);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");
        IntegrationTestTools.assertNoAttribute(accountModel, dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);

        assertDummyScriptsModify(userJack);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test197ModifyUserJackLocationNull() throws Exception {
    	final String TEST_NAME = "test197ModifyUserJackLocationNull";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        try {
			// WHEN
	        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, (PolyString)null);

	        AssertJUnit.fail("Unexpected success");
        } catch (IllegalStateException e) {
        	// This is expected
        }
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertNoProvisioningScripts();

        // Check audit
        display("Audit", dummyAuditService);
        // This should fail even before the request record is created
        dummyAuditService.assertRecords(0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
	}

	@Test
    public void test198ModifyUserJackRaw() throws Exception {
        TestUtil.displayTestTitle(this, "test198ModifyUserJackRaw");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test196ModifyUserJackRaw");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
        		PrismTestUtil.createPolyString("Marvelous Captain Jack Sparrow"));
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(objectDelta);

		// WHEN
		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Marvelous Captain Jack Sparrow", "Jack", "Sparrow", null);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account - the original fullName should not be changed
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");

        // Check account in dummy resource - the original fullName should not be changed
        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);

        assertNoProvisioningScripts();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID); // MID-2451
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	@Test
    public void test199DeleteUserJack() throws Exception {
        TestUtil.displayTestTitle(this, "test199DeleteUserJack");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test199DeleteUserJack");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		try {
			PrismObject<UserType> userJack = getUser(USER_JACK_OID);
			AssertJUnit.fail("Jack is still alive!");
		} catch (ObjectNotFoundException ex) {
			// This is OK
		}

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 1);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

	@Test
    public void test200AddUserBlackbeardWithAccount() throws Exception {
		final String TEST_NAME = "test200AddUserBlackbeardWithAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        // Use custom channel to trigger a special outbound mapping
        task.setChannel("http://pirates.net/avast");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-blackbeard-account-dummy.xml"));
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userBlackbeard = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
        UserType userBlackbeardType = userBlackbeard.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userBlackbeardType.getLinkRef().size());
        ObjectReferenceType accountRefType = userBlackbeardType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        assertEncryptedUserPassword(userBlackbeard, "QueenAnne");
        assertPasswordMetadata(userBlackbeard, true, startTime, endTime, USER_ADMINISTRATOR_OID, "http://pirates.net/avast");

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "blackbeard");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("blackbeard", "Edward Teach", true);
        DummyAccount dummyAccount = getDummyAccount(null, "blackbeard");
        assertEquals("Wrong loot", (Integer)10000, dummyAccount.getAttributeValue("loot", Integer.class));

        assertDummyScriptsAdd(userBlackbeard, accountModel, getDummyResourceType());

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 3);
        dummyAuditService.assertHasDelta(0, ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.ADD, ShadowType.class);
        // this one was redundant
//        dummyAuditService.assertExecutionDeltas(1, 1);
//        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
     // raw operation, no target
//        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 1);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

        assertSteadyResources();
    }


	@Test
    public void test210AddUserMorganWithAssignment() throws Exception {
		final String TEST_NAME = "test210AddUserMorganWithAssignment";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-morgan-assignment-dummy.xml"));
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
		display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        AssignmentType assignmentType = assertAssignedAccount(userMorgan, RESOURCE_DUMMY_OID);
        assertLinks(userMorgan, 1);
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        assertCreateMetadata(userMorgan, startTime, endTime);
        assertCreateMetadata(assignmentType, startTime, endTime);

        assertEncryptedUserPassword(userMorgan, "rum");
        assertPasswordMetadata(userMorgan, true, startTime, endTime);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "morgan");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("morgan", "Sir Henry Morgan", true);

        assertDummyScriptsAdd(userMorgan, accountModel, getDummyResourceType());

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_MORGAN_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 1);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

        assertSteadyResources();
    }

	@Test
    public void test212RenameUserMorgan() throws Exception {
		final String TEST_NAME = "test212RenameUserMorgan";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_MORGAN_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("sirhenry"));

		// THEN
        displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

		// Check shadow
        PrismObject<ShadowType> accountShadowRepo = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow repo", accountShadowRepo);
        assertDummyAccountShadowRepo(accountShadowRepo, accountOid, "sirhenry");

        // Check account
        PrismObject<ShadowType> accountShadowModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow model", accountShadowModel);
        assertDummyAccountShadowModel(accountShadowModel, accountOid, "sirhenry", "Sir Henry Morgan");

        // Check account in dummy resource
        assertDefaultDummyAccount("sirhenry", "Sir Henry Morgan", true);

        assertDummyScriptsModify(userMorgan);

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        ObjectDeltaOperation<ShadowType> auditShadowDelta = dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);

        assertEquals("Unexpected number of modifications in shadow audit delta: "+auditShadowDelta.debugDump(), 8, auditShadowDelta.getObjectDelta().getModifications().size());

        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class,
        		UserType.F_NAME, PrismTestUtil.createPolyString("morgan"));
        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
        		new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME), "morgan");
        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
        		new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_UID), "morgan");
        // This is a side-effect change. It is silently done by provisioning. It is not supposed to
        // appear in audit log.
//        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
//        		new ItemPath(ShadowType.F_NAME), PrismTestUtil.createPolyString("morgan"));

        dummyAuditService.assertTarget(USER_MORGAN_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

        assertSteadyResources();
    }

	/**
	 * This basically tests for correct auditing.
	 */
	@Test
    public void test240AddUserCharlesRaw() throws Exception {
		final String TEST_NAME = "test240AddUserCharlesRaw";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = createUser("charles", "Charles L. Charles");
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

		// WHEN
		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userAfter = findUserByUsername("charles");
        assertNotNull("No charles", userAfter);
        userCharlesOid = userAfter.getOid();

        assertNoProvisioningScripts();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
     // raw operation, no target
//        dummyAuditService.assertTarget(userAfter.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
	}

	/**
	 * This basically tests for correct auditing.
	 */
	@Test
    public void test241DeleteUserCharlesRaw() throws Exception {
		final String TEST_NAME = "test241DeleteUserCharlesRaw";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, userCharlesOid, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

		// WHEN
		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);

		// THEN
		assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userAfter = findUserByUsername("charles");
        assertNull("Charles is not gone", userAfter);

		assertNoProvisioningScripts();

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, UserType.class);
     // raw operation, no target
//        dummyAuditService.assertTarget(userCharlesOid);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
	}

	@Test
    public void test300AddUserJackWithAssignmentBlue() throws Exception {
		final String TEST_NAME="test300AddUserJackWithAssignmentBlue";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        PrismObject<UserType> userJack = PrismTestUtil.parseObject(USER_JACK_FILE);
        AssignmentType assignmentBlue = createConstructionAssignment(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null);
		userJack.asObjectable().getAssignment().add(assignmentBlue);

        ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(userJack);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        // Weak activation mapping means account load
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
		display("User after change execution", userJackAfter);
		assertUserJack(userJackAfter);
        accountJackBlueOid = getSingleLinkOid(userJackAfter);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackBlueOid, null, result);
        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackBlueOid, null, task, result);
        assertShadowModel(accountModel, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME),
        		getAccountObjectClass(getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME)));
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, USER_JACK_FULL_NAME, true);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

	/**
	 * modify account blue directly + request reconcile. check old value in delta.
	 */
	@Test
    public void test302ModifyAccountJackDummyBlue() throws Exception {
		final String TEST_NAME = "test302ModifyAccountJackDummyBlue";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountJackBlueOid, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributeFullnamePath(), prismContext,
        		"Cpt. Jack Sparrow");
        accountDelta.addModificationReplaceProperty(
        		getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Queen Anne's Revenge");
        deltas.add(accountDelta);

		ModelExecuteOptions options = ModelExecuteOptions.createReconcile();

		// WHEN
		modelService.executeChanges(deltas, options, task, result);

		// THEN
		assertSuccess(result);
        // Not sure why 2 ... but this is not a big problem now
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		// Fullname inbound mapping is not used because it is weak
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        String accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();

        dummyAuditService.assertExecutionDeltas(0, 2); // lastProvisioningTimestamp, modify account
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
        		getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributeFullnamePath(), "Jack Sparrow");
//        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

	private void assertDummyScriptsAdd(PrismObject<UserType> user, PrismObject<? extends ShadowType> account, ResourceType resource) {
		ProvisioningScriptSpec script = new ProvisioningScriptSpec("\nto spiral :size\n" +
				"   if  :size > 30 [stop]\n   fd :size rt 15\n   spiral :size *1.02\nend\n			");

		String userName = null;
		if (user != null) {
			userName = user.asObjectable().getName().getOrig();
		}
		script.addArgSingle("usr", "user: "+userName);

		// Note: We cannot test for account name as name is only assigned in provisioning
		String accountEnabled = null;
		if (account != null && account.asObjectable().getActivation() != null
				&& account.asObjectable().getActivation().getAdministrativeStatus() != null) {
			accountEnabled = account.asObjectable().getActivation().getAdministrativeStatus().toString();
		}
		script.addArgSingle("acc", "account: "+accountEnabled);

		String resourceName = null;
		if (resource != null) {
			resourceName = resource.getName().getOrig();
		}
		script.addArgSingle("res", "resource: "+resourceName);

		script.addArgSingle("size", "3");
		script.setLanguage("Logo");
		IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), script);
	}

	private void assertDummyScriptsModify(PrismObject<UserType> user) {
		assertDummyScriptsModify(user, false);
	}

	private void assertDummyScriptsModify(PrismObject<UserType> user, boolean recon) {
		ProvisioningScriptSpec modScript = new ProvisioningScriptSpec("Beware the Jabberwock, my son!");
		String name = null;
		String fullName = null;
		String costCenter = null;
		if (user != null) {
			name = user.asObjectable().getName().getOrig();
			fullName = user.asObjectable().getFullName().getOrig();
			costCenter = user.asObjectable().getCostCenter();
		}
		modScript.addArgSingle("howMuch", costCenter);
		modScript.addArgSingle("howLong", "from here to there");
		modScript.addArgSingle("who", name);
		modScript.addArgSingle("whatchacallit", fullName);
		if (recon) {
			ProvisioningScriptSpec reconBeforeScript = new ProvisioningScriptSpec("The vorpal blade went snicker-snack!");
			reconBeforeScript.addArgSingle("who", name);
			ProvisioningScriptSpec reconAfterScript = new ProvisioningScriptSpec("He left it dead, and with its head");
			reconAfterScript.addArgSingle("how", "enabled");
			IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), reconBeforeScript, modScript, reconAfterScript);
		} else {
			IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), modScript);
		}
	}

	private void assertDummyScriptsDelete() {
		ProvisioningScriptSpec script = new ProvisioningScriptSpec("The Jabberwock, with eyes of flame");
		IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), script);
	}

	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        purgeProvisioningScriptHistory();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
	}

	private void assertResultSerialization(OperationResult result) throws SchemaException {
		OperationResultType resultType = result.createOperationResultType();
		String serialized = prismContext.serializerFor(PrismContext.LANG_XML).serializeAnyData(resultType, SchemaConstants.C_RESULT);
		display("OperationResultType serialized", serialized);
	}

}
