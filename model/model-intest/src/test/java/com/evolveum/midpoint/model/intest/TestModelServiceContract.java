/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.notifications.events.AccountEvent;
import com.evolveum.midpoint.notifications.transports.Message;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringStrictMatchingRule;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.test.DummyResourceContoller;
import com.evolveum.midpoint.notifications.notifiers.DummyNotifier;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
	
	private static String accountOid;
	private static String userCharlesOid;
	
	public TestModelServiceContract() throws JAXBException {
		super();
	}
	
	@Test
    public void test040GetResource() throws Exception {
        TestUtil.displayTestTile(this, "test040GetResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test040GetResource");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
		// WHEN
		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null , task, result);
		
		assertResource(resource);
		
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
	}
	
	@Test
    public void test041SearchResources() throws Exception {
        TestUtil.displayTestTile(this, "test041SearchResources");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test041SearchResources");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
		// WHEN

        // opendj is not started, so we do not want to list it (it returns partial_error)
        ObjectQuery query = ObjectQuery.createObjectQuery(
                new NotFilter(
                        EqualsFilter.createEqual(ResourceType.class, prismContext, ResourceType.F_NAME, new PolyString(RESOURCE_OPENDJ_NAME), PolyStringOrigMatchingRule.NAME.getLocalPart())));
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, query, null, task, result);

		// THEN
        assertNotNull("null rearch return", resources);
        assertFalse("Empty rearch return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 7, resources.size());
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("searchObjects result", result);

        for (PrismObject<ResourceType> resource: resources) {
        	assertResource(resource);
        }
	}
	
	private void assertResource(PrismObject<ResourceType> resource) throws JAXBException {
		display("Resource", resource);
		display("Resource def", resource.getDefinition());
		PrismContainer<ConnectorConfigurationType> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No Resource connector configuration def", configurationContainer);
		display("Resource connector configuration def", configurationContainer.getDefinition());
		display("Resource connector configuration def complex type def", configurationContainer.getDefinition().getComplexTypeDefinition());
		assertNotNull("Empty Resource connector configuration def", configurationContainer.isEmpty());
		assertEquals("Wrong compile-time class in Resource connector configuration in "+resource, ConnectorConfigurationType.class, 
				configurationContainer.getCompileTimeClass());
		
		resource.checkConsistence(true, true);
		
		// Try to marshal using pure JAXB as a rough test that it is OK JAXB-wise
		Element resourceDomElement = prismContext.getPrismJaxbProcessor().marshalObjectToDom(resource.asObjectable(), new QName(SchemaConstants.NS_C, "resource"),
				DOMUtil.getDocument());
		display("Resouce DOM element after JAXB marshall", resourceDomElement);
	}
		
	@Test
    public void test050GetUserJack() throws Exception {
		final String TEST_NAME = "test050GetUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // WHEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        
        // THEN
        display("User jack", userJack);
        assertUserJack(userJack);
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
	}
	
	@Test
    public void test051GetUserBarbossa() throws Exception {
		final String TEST_NAME = "test051GetUserBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // WHEN
        PrismObject<UserType> userBarbossa = modelService.getObject(UserType.class, USER_BARBOSSA_OID, null, task, result);
        
        // THEN
        display("User barbossa", userBarbossa);
        assertUser(userBarbossa, USER_BARBOSSA_OID, "barbossa", "Hector Barbossa", "Hector", "Barbossa");
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
        
        userBarbossa.checkConsistence(true, true);
        
        PrismContainer<Containerable> assignmentContainer = userBarbossa.findContainer(UserType.F_ASSIGNMENT);
        assertEquals("Unexpected number of assignment values", 2, assignmentContainer.size());
        PrismAsserts.assertValueId(1001L,assignmentContainer);
	}
	
	@Test
    public void test100ModifyUserAddAccount() throws Exception {
        TestUtil.displayTestTile(this, "test100ModifyUserAddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test100ModifyUserAddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		purgeScriptHistory();
		dummyAuditService.clear();
        dummyNotifier.clearRecords();
        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);
        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
                
        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

        // Check notifications
        display("Notifier", dummyNotifier);
        checkTest100NotificationRecords("newAccounts");
        checkTest100NotificationRecords("newAccountsViaExpression");

        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

//        List<Message> messages = dummyTransport.getMessages("dummy:accountPasswordNotifier");
//        assertNotNull("No messages recorded in dummy transport", messages);
//        assertEquals("Invalid number of messages recorded in dummy transport", 1, messages.size());
//        Message message = messages.get(0);
//        assertEquals("Invalid list of recipients", Arrays.asList(userJackType.getEmailAddress()), message.getTo());
//
//        messages = dummyTransport.getMessages("dummy:newAccountsViaExpression");
//        assertNotNull("No messages recorded in dummy transport (expressions)", messages);
//        assertEquals("Invalid number of messages recorded in dummy transport (expressions)", 1, messages.size());
//        message = messages.get(0);
//        assertEquals("Invalid list of recipients (expressions)", Arrays.asList("test1", "test2"), message.getTo());
//        assertEquals("Invalid message subject", "Changed account for jack", message.getSubject());
//        assertEquals("Invalid message body", "Body: Changed account for jack", message.getBody());

	}

    private void checkTest100NotificationRecords(String notifierName) {
        assertEquals("Invalid number of notification records [" + notifierName + "]", 1, dummyNotifier.getRecords(notifierName).size());
        DummyNotifier.NotificationRecord record = dummyNotifier.getRecords(notifierName).get(0);
        assertEquals("Wrong user in notification record [" + notifierName + "]", USER_JACK_OID, ((AccountEvent) record.getEvent()).getRequestee().getOid());
        assertEquals("Wrong number of account OIDs in notification record [" + notifierName + "]", 1, record.getAccountsOids().size());
        assertEquals("Wrong account OID in notification record [" + notifierName + "]", accountOid, record.getAccountsOids().iterator().next());
        assertEquals("Wrong change type in notification record [" + notifierName + "]", ChangeType.ADD, record.getFirstChangeType());
    }

    @Test
    public void test101GetAccount() throws Exception {
        TestUtil.displayTestTile(this, "test101GetAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test101GetAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        // Let's do some evil things. Like changing some of the attribute on a resource and see if they will be
        // fetched after get.
        // Also set a value for ignored "water" attribute. The system should cope with that.
        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "The best pirate captain ever");
        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, "cold");
        
		// WHEN
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountOid, null , task, result);
		
		// THEN
		display("Account", account);
		display("Account def", account.getDefinition());
		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyShadowModel(account, accountOid, "jack", "Jack Sparrow");
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
        
        account.checkConsistence(true, true);
        
        IntegrationTestTools.assertAttribute(account, getAttributeQName(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME), 
        		"The best pirate captain ever");
        // This one should still be here, even if ignored
        IntegrationTestTools.assertAttribute(account, getAttributeQName(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME), 
        		"cold");
	}

	@Test
    public void test102GetAccountNoFetch() throws Exception {
        TestUtil.displayTestTile(this, "test102GetAccountNoFetch");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test102GetAccountNoFetch");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
		
		// WHEN
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountOid, options , task, result);
		
		display("Account", account);
		display("Account def", account.getDefinition());
		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyShadowRepo(account, accountOid, "jack");
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
	}
	
	@Test
    public void test103GetAccountRaw() throws Exception {
        TestUtil.displayTestTile(this, "test103GetAccountRaw");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test103GetAccountRaw");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		
		// WHEN
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountOid, options , task, result);
		
		display("Account", account);
		display("Account def", account.getDefinition());
		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyShadowRepo(account, accountOid, "jack");
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
	}

	@Test
    public void test108ModifyUserAddAccountAgain() throws Exception {
        TestUtil.displayTestTile(this, "test108ModifyUserAddAccountAgain");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test108ModifyUserAddAccountAgain");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		dummyAuditService.clear();

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
		
		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
    }
	
	@Test
    public void test109ModifyUserAddAccountAgain() throws Exception {
        TestUtil.displayTestTile(this, "test109ModifyUserAddAccountAgain");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test109ModifyUserAddAccountAgain");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        account.setOid(null);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		purgeScriptHistory();
		dummyAuditService.clear();
        
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
		
		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
		
	}

	@Test
    public void test110GetUserResolveAccount() throws Exception {
        TestUtil.displayTestTile(this, "test110GetUserResolveAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test110GetUserResolveAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = 
        	SelectorOptions.createCollection(UserType.F_LINK, GetOperationOptions.createResolve());
        
		// WHEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);
		
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
        ShadowType ResourceObjectShadowType = userJackType.getLink().get(0);
        assertDummyShadowModel(ResourceObjectShadowType.asPrismObject(), accountOid, "jack", "Jack Sparrow");
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
	}


	@Test
    public void test111GetUserResolveAccountResource() throws Exception {
        TestUtil.displayTestTile(this, "test111GetUserResolveAccountResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test111GetUserResolveAccountResource");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = 
            	SelectorOptions.createCollection(GetOperationOptions.createResolve(),
        			new ItemPath(UserType.F_LINK),
    				new ItemPath(UserType.F_LINK, ShadowType.F_RESOURCE)
        	);
        
		// WHEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);
		
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
        ShadowType ResourceObjectShadowType = userJackType.getLink().get(0);
        assertDummyShadowModel(ResourceObjectShadowType.asPrismObject(), accountOid, "jack", "Jack Sparrow");
        
        assertNotNull("Resource in account was not resolved", ResourceObjectShadowType.getResource());
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
        
        userJack.checkConsistence(true, true);
	}

	@Test
    public void test112GetUserResolveAccountNoFetch() throws Exception {
        TestUtil.displayTestTile(this, "test112GetUserResolveAccountNoFetch");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test112GetUserResolveAccountNoFetch");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        GetOperationOptions getOpts = new GetOperationOptions();
        getOpts.setResolve(true);
        getOpts.setNoFetch(true);
		Collection<SelectorOptions<GetOperationOptions>> options = 
        	SelectorOptions.createCollection(UserType.F_LINK, getOpts);
        
		// WHEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);
		
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
        ShadowType ResourceObjectShadowType = userJackType.getLink().get(0);
        assertDummyShadowRepo(ResourceObjectShadowType.asPrismObject(), accountOid, "jack");
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
        
        userJack.checkConsistence(true, true);
	}
	
	@Test
    public void test119ModifyUserDeleteAccount() throws Exception {
		final String TEST_NAME = "test119ModifyUserDeleteAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        purgeScriptHistory();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        account.setOid(accountOid);
        		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        prepareNotifications();

        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result, 2);
        
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of linkRefs", 0, userJackType.getLinkRef().size());
        
		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        	AssertJUnit.fail("Shadow "+accountOid+" still exists");
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
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
    }

    @Test
    public void test120AddAccount() throws Exception {
        TestUtil.displayTestTile(this, "test120AddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test120AddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();

        prepareNotifications();

        purgeScriptHistory();
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createAddDelta(account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        accountOid = accountDelta.getOid();
        assertNotNull("No account OID in resulting delta", accountOid);
		// Check accountRef (should be none)
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        // The user is not associated with the account
        assertDummyScriptsAdd(null, accountModel, resourceDummyType);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);          // there's no password for that account
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);               // account has no owner
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }
	
	@Test
    public void test121ModifyUserAddAccountRef() throws Exception {
        TestUtil.displayTestTile(this, "test121ModifyUserAddAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test121ModifyUserAddAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();

        prepareNotifications();

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountOid);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack);
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);                // some attributes on the account are changed
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }


	
	@Test
    public void test128ModifyUserDeleteAccountRef() throws Exception {
        TestUtil.displayTestTile(this, "test128ModifyUserDeleteAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test128ModifyUserDeleteAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        prepareNotifications();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountOid);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);
		        
		// Check shadow (if it is unchanged)
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account (if it is unchanged)
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource (if it is unchanged)
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }
	
	@Test
    public void test129DeleteAccount() throws Exception {
        TestUtil.displayTestTile(this, "test129DeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test129DeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        prepareNotifications();
        purgeScriptHistory();
        
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountOid, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
		// Check is shadow is gone
        assertNoAccountShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        assertDummyScriptsDelete();
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);           // there's no link user->account (removed in test128)
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
    }

	
	@Test
    public void test130PreviewModifyUserJackAssignAccount() throws Exception {
        TestUtil.displayTestTile(this, "test130PreviewModifyUserJackAssignAccount");

        // GIVEN
        try{
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test130PreviewModifyUserJackAssignAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
        ModelContext<UserType,ShadowType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("previewChanges result", result);
        
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
	}
	
	@Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
        TestUtil.displayTestTile(this, "test131ModifyUserJackAssignAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test131ModifyUserJackAssignAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        prepareNotifications();
        purgeScriptHistory();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
    }
	
	/**
	 * Modify the account. Some of the changes should be reflected back to the user by inbound mapping.
	 */
	@Test
    public void test132ModifyAccountJackDummy() throws Exception {
        TestUtil.displayTestTile(this, "test132ModifyAccountJackDummy");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test132ModifyAccountJackDummy");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		accountOid, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
        accountDelta.addModificationReplaceProperty(
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Queen Anne's Revenge");
        deltas.add(accountDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		// Fullname inbound mapping is not used because it is weak
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// ship inbound mapping is used, it is strong 
		assertEquals("Wrong user locality (orig)", "The crew of Queen Anne's Revenge", 
				userJack.asObjectable().getOrganizationalUnit().iterator().next().getOrig());
		assertEquals("Wrong user locality (norm)", "the crew of queen annes revenge", 
				userJack.asObjectable().getOrganizationalUnit().iterator().next().getNorm());
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Cpt. Jack Sparrow");
        PrismAsserts.assertPropertyValue(accountModel, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Queen Anne's Revenge");
        
        // Check account in dummy resource
        assertDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, 
        		"Queen Anne's Revenge");
        
        assertDummyScriptsModify(userJack);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 1);
        dummyAuditService.asserHasDelta(0, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1, 1);
        dummyAuditService.asserHasDelta(1, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }
	
	@Test
    public void test139ModifyUserJackUnassignAccount() throws Exception {
        TestUtil.displayTestTile(this, "test139ModifyUserJackUnassignAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test139ModifyUserJackUnassignAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
        // Check is shadow is gone
        assertNoAccountShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        assertDummyScriptsDelete();
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }
	
	/**
	 * Assignment enforcement is set to POSITIVE for this test. The account should be added.
	 */
	@Test
    public void test141ModifyUserJackAssignAccountPositiveEnforcement() throws Exception {
		final String TEST_NAME = "test141ModifyUserJackAssignAccountPositiveEnforcement";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }
	
	/**
	 * Assignment enforcement is set to POSITIVE for this test. The account should remain as it is.
	 */
	@Test
    public void test148ModifyUserJackUnassignAccountPositiveEnforcement() throws Exception {
		final String TEST_NAME = "test148ModifyUserJackUnassignAccountPositiveEnforcement";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() 
        		+ "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
        
        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);
                
        //change resource assigment policy to be positive..if they are not applied by projector, the test will fail
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinked(userJack, accountOid);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        assertNoProvisioningScripts();
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
        
        // return resource to the previous state..delete assignment enforcement to prevent next test to fail..
        deleteResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }

	/**
	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
	 * Now we will explicitly delete the account.
	 */
	@Test
    public void test149ModifyUserJackDeleteAccount() throws Exception {
        TestUtil.displayTestTile(this, "test149ModifyUserJackDeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test149ModifyUserJackDeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setOid(accountOid);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountOid);
		userDelta.addModification(accountRefDelta);
        
		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountOid, prismContext);
		
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
        // Check is shadow is gone
        assertNoAccountShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        assertDummyScriptsDelete();
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }

	@Test
    public void test150ModifyUserAddAccountFullEnforcement() throws Exception {
		final String TEST_NAME = "test150ModifyUserAddAccountFullEnforcement";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		dummyAuditService.clear();
		purgeScriptHistory();
        
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
        IntegrationTestTools.assertFailure("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
        // Check that shadow was not created
        assertNoAccountShadow(accountOid);
        
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
	}
	
	@Test
    public void test152ModifyUserAddAndAssignAccountPositiveEnforcement() throws Exception {
		final String TEST_NAME = "test152ModifyUserAddAndAssignAccountPositiveEnforcement";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		dummyAuditService.clear();
		purgeScriptHistory();
        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
			
		// THEN
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
	 * Now we will explicitly delete the account.
	 */
	@Test
    public void test159ModifyUserJackUnassignAndDeleteAccount() throws Exception {
        TestUtil.displayTestTile(this, "test159ModifyUserJackUnassignAndDeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test149ModifyUserJackUnassignAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        purgeScriptHistory();
        
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        // Explicit unlink is not needed here, it should work without it
        
		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountOid, prismContext);
		
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
        // Check is shadow is gone
        assertNoAccountShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        assertDummyScriptsDelete();
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * We try to both assign an account and modify that account in one operation.
	 * Some changes should be reflected to account (e.g.  weapon) as the mapping is weak, other should be
	 * overridded (e.g. fullname) as the mapping is strong.
	 */
	@Test
    public void test160ModifyUserJackAssignAccountAndModify() throws Exception {
        TestUtil.displayTestTile(this, "test160ModifyUserJackAssignAccountAndModify");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test160ModifyUserJackAssignAccountAndModify");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        ShadowDiscriminatorObjectDelta<ShadowType> accountDelta = ShadowDiscriminatorObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		RESOURCE_DUMMY_OID, null, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
        accountDelta.addModificationAddProperty(
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME), 
        		"smell");
        deltas.add(accountDelta);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_JACK_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "smell");
        assertNull("Unexpected loot", dummyAccount.getAttributeValue("loot", Integer.class));
        
        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }

    /**
     * We try to modify an assignment of the account and see whether changes will be recorded in the account itself.
     *
     * Temporarily disabled, as currently fails due to audit service limitation ("java.lang.UnsupportedOperationException: ID not supported in Xpath yet").
     */
    @Test(enabled = false)
    public void test161ModifyUserJackModifyAssignment() throws Exception {
        TestUtil.displayTestTile(this, "test161ModifyUserJackModifyAssignment");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test161ModifyUserJackModifyAssignment");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        purgeScriptHistory();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

        //PrismPropertyDefinition definition = getAssignmentDefinition().findPropertyDefinition(new QName(SchemaConstantsGenerated.NS_COMMON, "accountConstruction"));

        PrismObject<ResourceType> dummyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);
        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(dummyResource, prismContext);
        RefinedObjectClassDefinition accountDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String) null);
        PrismPropertyDefinition gossipDefinition = accountDefinition.findPropertyDefinition(new QName(
                "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME));
        assertNotNull("gossip attribute definition not found", gossipDefinition);

        ConstructionType accountConstruction = createAccountConstruction(RESOURCE_DUMMY_OID, null);
        ResourceAttributeDefinitionType radt = new ResourceAttributeDefinitionType();
        radt.setRef(gossipDefinition.getName());
        MappingType outbound = new MappingType();
        radt.setOutbound(outbound);

        ExpressionType expression = new ExpressionType();
        outbound.setExpression(expression);

        MappingType value = new MappingType();

        PrismProperty property = gossipDefinition.instantiate();
        property.add(new PrismPropertyValue<String>("q"));

        List evaluators = expression.getExpressionEvaluator();
        Collection<?> collection = LiteralExpressionEvaluatorFactory.serializeValueElements(property, null);
        ObjectFactory of = new ObjectFactory();
        for (Object obj : collection) {
            evaluators.add(of.createValue(obj));
        }

        value.setExpression(expression);
        radt.setOutbound(value);
        ObjectDelta<UserType> accountAssignmentUserDelta =
                createReplaceAccountConstructionUserDelta(USER_JACK_OID, 1L, accountConstruction);
        deltas.add(accountAssignmentUserDelta);

        PrismObject<UserType> userJackOld = getUser(USER_JACK_OID);
        display("User before change execution", userJackOld);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");
        accountOid = getSingleUserAccountRef(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_JACK_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
        display(dummyAccount.debugDump());
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "q");
        //assertEquals("Missing or incorrect attribute value", "soda", dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, String.class));

        assertDummyScriptsModify(userJack);
        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        Collection<ObjectDelta<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
//        assertEquals("Wrong number of execution deltas", 3, auditExecutionDeltas.size());
//        PrismAsserts.asserHasDelta("Audit execution deltas", auditExecutionDeltas, ChangeType.MODIFY, UserType.class);
//        PrismAsserts.asserHasDelta("Audit execution deltas", auditExecutionDeltas, ChangeType.ADD, ResourceObjectShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test165ModifyUserJack() throws Exception {
        TestUtil.displayTestTile(this, "test165ModifyUserJack");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test165ModifyUserJack");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, 
        		PrismTestUtil.createPolyString("Magnificent Captain Jack Sparrow"));
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Magnificent Captain Jack Sparrow");
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Magnificent Captain Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
        
        assertDummyScriptsModify(userJack);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }
    
    @Test
    public void test166ModifyUserJackLocationEmpty() throws Exception {
    	final String TEST_NAME = "test166ModifyUserJackLocationEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Magnificent Captain Jack Sparrow", "Jack", "Sparrow", null);
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Magnificent Captain Jack Sparrow");
        IntegrationTestTools.assertNoAttribute(accountModel, dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
        
        assertDummyScriptsModify(userJack);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }

    @Test
    public void test167ModifyUserJackLocationNull() throws Exception {
    	final String TEST_NAME = "test167ModifyUserJackLocationNull";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
               
        try {
			// WHEN
	        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, (PolyString)null);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (IllegalStateException e) {
        	// This is expected
        }
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertFailure(result);
        
        assertNoProvisioningScripts();
        
        // Check audit
        display("Audit", dummyAuditService);
        // This should fail even before the request record is created
        dummyAuditService.assertRecords(0);
	}
    
	@Test
    public void test168ModifyUserJackRaw() throws Exception {
        TestUtil.displayTestTile(this, "test168ModifyUserJackRaw");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test166ModifyUserJackRaw");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
        		PrismTestUtil.createPolyString("Marvelous Captain Jack Sparrow"));
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(objectDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Marvelous Captain Jack Sparrow", "Jack", "Sparrow", null);
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account - the original fullName should not be changed
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Magnificent Captain Jack Sparrow");
        
        // Check account in dummy resource - the original fullName should not be changed
        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
        
        assertNoProvisioningScripts();
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }
		
	@Test
    public void test169DeleteUserJack() throws Exception {
        TestUtil.displayTestTile(this, "test169DeleteUserJack");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test169DeleteUserJack");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		try {
			PrismObject<UserType> userJack = getUser(USER_JACK_OID);
			AssertJUnit.fail("Jack is still alive!");
		} catch (ObjectNotFoundException ex) {
			// This is OK
		}
        
        // Check is shadow is gone
        assertNoAccountShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
        
        assertDummyScriptsDelete();
        
     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 1);

    }
	
	@Test
    public void test200AddUserBlackbeardWithAccount() throws Exception {
        TestUtil.displayTestTile(this, "test200AddUserBlackbeardWithAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test200AddUserBlackbeardWithAccount");
        // Use custom channel to trigger a special outbound mapping
        task.setChannel("http://pirates.net/avast");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-blackbeard-account-dummy.xml"));
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userBlackbeard = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
        UserType userBlackbeardType = userBlackbeard.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userBlackbeardType.getLinkRef().size());
        ObjectReferenceType accountRefType = userBlackbeardType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "blackbeard");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
        
        // Check account in dummy resource
        assertDummyAccount("blackbeard", "Edward Teach", true);
        DummyAccount dummyAccount = getDummyAccount(null, "blackbeard");
        assertEquals("Wrong loot", (Integer)10000, dummyAccount.getAttributeValue("loot", Integer.class));
        
        assertDummyScriptsAdd(userBlackbeard, accountModel, resourceDummyType);
        
        // Check audit        
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 3);
        dummyAuditService.asserHasDelta(0, ChangeType.ADD, UserType.class);
        dummyAuditService.asserHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(0, ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1, 1);
        dummyAuditService.asserHasDelta(1, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 1);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

    }

	
	@Test
    public void test210AddUserMorganWithAssignment() throws Exception {
        TestUtil.displayTestTile(this, "test210AddUserMorganWithAssignment");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test210AddUserMorganWithAssignment");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();
        prepareNotifications();
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-morgan-assignment-dummy.xml"));
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "morgan");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");
        
        // Check account in dummy resource
        assertDummyAccount("morgan", "Sir Henry Morgan", true);
        
        assertDummyScriptsAdd(userMorgan, accountModel, resourceDummyType);
        
     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 1);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

    }
	
	/**
	 * This basically tests for correct auditing.
	 */
	@Test
    public void test220AddUserCharlesRaw() throws Exception {
		final String TEST_NAME = "test220AddUserCharlesRaw";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();

        PrismObject<UserType> user = createUser("charles", "Charles L. Charles");
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);                
		
		// WHEN
		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
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
        dummyAuditService.asserHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * This basically tests for correct auditing.
	 */
	@Test
    public void test221DeleteUserCharlesRaw() throws Exception {
		final String TEST_NAME = "test221DeleteUserCharlesRaw";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        purgeScriptHistory();

        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, userCharlesOid, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);                
		
		// WHEN
		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userAfter = findUserByUsername("charles");
        assertNull("Charles is not gone", userAfter);
        
		assertNoProvisioningScripts();
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	private void assertMessageContains(String message, String string) {
		assert message.contains(string) : "Expected message to contain '"+string+"' but it does not; message: " + message;
	}
	
	private void purgeScriptHistory() {
		dummyResource.purgeScriptHistory();
	}

	private void assertNoProvisioningScripts() {
		if (!dummyResource.getScriptHistory().isEmpty()) {
			IntegrationTestTools.displayScripts(dummyResource.getScriptHistory());
			AssertJUnit.fail(dummyResource.getScriptHistory().size()+" provisioning scripts were executed while not expected any");
		}
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
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), script);
	}

	private void assertDummyScriptsModify(PrismObject<UserType> user) {
		ProvisioningScriptSpec script = new ProvisioningScriptSpec("Beware the Jabberwock, my son!");
		script.addArgSingle("howMuch", "a lot");
		script.addArgSingle("howLong", "from here to there");
		String name = null;
		if (user != null) {
			name = user.asObjectable().getName().getOrig();
		}
		script.addArgSingle("who", name);
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), script);
	}

	private void assertDummyScriptsDelete() {
		ProvisioningScriptSpec script = new ProvisioningScriptSpec("The Jabberwock, with eyes of flame");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), script);
	}

}
