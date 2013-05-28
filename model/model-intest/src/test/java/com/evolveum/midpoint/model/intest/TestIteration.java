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
import static com.evolveum.midpoint.test.IntegrationTestTools.displayWhen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayThen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
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
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
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
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
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
public class TestIteration extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/iteration");
	
	protected static final File RESOURCE_DUMMY_PINK_FILE = new File(TEST_DIR, "resource-dummy-pink.xml");
	protected static final String RESOURCE_DUMMY_PINK_OID = "10000000-0000-0000-0000-00000000a104";
	protected static final String RESOURCE_DUMMY_PINK_NAME = "pink";
	protected static final String RESOURCE_DUMMY_PINK_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static DummyResource dummyResourcePink;
	protected static DummyResourceContoller dummyResourceCtlPink;
	protected ResourceType resourceDummyPinkType;
	protected PrismObject<ResourceType> resourceDummyPink;
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlPink = DummyResourceContoller.create(RESOURCE_DUMMY_PINK_NAME, resourceDummyPink);
		dummyResourceCtlPink.extendDummySchema();
		dummyResourcePink = dummyResourceCtlPink.getDummyResource();
		resourceDummyPink = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_PINK_FILE, RESOURCE_DUMMY_PINK_OID, initTask, initResult); 
		resourceDummyPinkType = resourceDummyPink.asObjectable();
		dummyResourceCtlPink.setResource(resourceDummyPink);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
	}

	/**
	 * The default dummy instance will not iterate. It has correlation rule which will link the account instead.
	 */
	@Test
    public void test100JackAssignAccountDummyConflicting() throws Exception {
		final String TEST_NAME = "test100JackAssignAccountDummyConflicting";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Tortuga");
		dummyResource.addAccount(account);
		repoAddObject(ShadowType.class, createShadow(resourceDummy, ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleUserAccountRef(userJack);
        
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
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test200JackAssignAccountDummyPinkConflicting() throws Exception {
		final String TEST_NAME = "test200JackAssignAccountDummyPinkConflicting";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Red Sea");
		dummyResourcePink.addAccount(account);
		repoAddObject(ShadowType.class, createShadow(resourceDummyPink, ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_PINK_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(userJack, 2);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		
		String accountRedOid = getUserAccountRef(userJack, RESOURCE_DUMMY_PINK_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountRedShadow = repositoryService.getObject(ShadowType.class, accountRedOid, result);
        assertShadowRepo(accountRedShadow, accountRedOid, "jack", resourceDummyPinkType);
        
        // Check account
        PrismObject<ShadowType> accountRedModel = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        assertShadowModel(accountRedModel, accountRedOid, "jack", resourceDummyPinkType);
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "jack", "Jack Sparrow", true);
        
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
	

}
