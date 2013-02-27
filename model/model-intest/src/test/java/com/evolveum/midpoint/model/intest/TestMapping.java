/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.notifications.notifiers.DummyNotifier;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.test.DummyResourceContoller;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMapping extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
	}

	/**
	 * Blue dummy has WEAK mappings. Let's play a bit with that.
	 */
	@Test
    public void test100ModifyUserAssignAccountDummyBlue() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackAssignAccountDummyBlue";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, 
        		RESOURCE_DUMMY_BLUE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        assertShadowRepo(accountShadow, accountOid, "jack", resourceDummyBlueType);
        
        // Check account
        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "jack", resourceDummyBlueType);
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, "jack", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test101ModifyUserFullName() throws Exception {
		final String TEST_NAME = "test101ModifyUserFullName";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Jack Sparrow", null, dummyResourceCtlBlue, task);

		// Check audit
      display("Audit", dummyAuditService);
      dummyAuditService.assertSimpleRecordSanity();
      dummyAuditService.assertRecords(2);
      dummyAuditService.assertAnyRequestDeltas();
      dummyAuditService.assertExecutionDeltas(1);
      dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
      dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test102ModifyUserOrganizationalUnit() throws Exception {
		final String TEST_NAME = "test102ModifyUserOrganizationalUnit";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		PrismTestUtil.createPolyString("Black Pearl"));
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Jack Sparrow", "Black Pearl", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test103ModifyAccountShip() throws Exception {
		final String TEST_NAME = "test103ModifyAccountShip";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleUserAccountRef(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(AccountShadowType.class,
        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "Flying Dutchman");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Jack Sparrow", "Flying Dutchman", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test104ModifyAccountShipReplaceEmpty() throws Exception {
		final String TEST_NAME = "test104ModifyAccountShipReplaceEmpty";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleUserAccountRef(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(AccountShadowType.class,
        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext);
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Jack Sparrow", null, dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test105ModifyUserOrganizationalUnitAgain() throws Exception {
		final String TEST_NAME = "test105ModifyUserOrganizationalUnitAgain";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		PrismTestUtil.createPolyString("HMS Dauntless"));
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Jack Sparrow", "HMS Dauntless", dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test106ModifyAccountShipDelete() throws Exception {
		final String TEST_NAME = "test106ModifyAccountShipDelete";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleUserAccountRef(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(AccountShadowType.class,
        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext, "HMS Dauntless");
        deltas.add(accountDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Jack Sparrow", null, dummyResourceCtlBlue, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test109ModifyUserUnassignAccountBlue() throws Exception {
		final String TEST_NAME = "test109ModifyUserUnassignAccountBlue";
        displayTestTile(this, TEST_NAME);

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
        IntegrationTestTools.assertSuccess(result);
        
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
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	
	/**
	 * Red dummy has STRONG mappings.
	 */
	@Test
    public void test120ModifyUserAssignAccountDummyRed() throws Exception {
		final String TEST_NAME = "test120ModifyUserAssignAccountDummyRed";
        displayTestTile(this, TEST_NAME);

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
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        assertShadowRepo(accountShadow, accountOid, "jack", resourceDummyRedType);
        
        // Check account
        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "jack", resourceDummyRedType);
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test121ModifyUserFullName() throws Exception {
		final String TEST_NAME = "test121ModifyUserFullName";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Captain Jack Sparrow", null, dummyResourceCtlRed, task);

		// Check audit
      display("Audit", dummyAuditService);
      dummyAuditService.assertSimpleRecordSanity();
      dummyAuditService.assertRecords(2);
      dummyAuditService.assertAnyRequestDeltas();
      dummyAuditService.assertExecutionDeltas(2);
      dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
      dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
      dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test122ModifyUserOrganizationalUnit() throws Exception {
		final String TEST_NAME = "test122ModifyUserOrganizationalUnit";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		PrismTestUtil.createPolyString("Black Pearl"));
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test123ModifyAccountShip() throws Exception {
		final String TEST_NAME = "test123ModifyAccountShip";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleUserAccountRef(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(AccountShadowType.class,
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
        IntegrationTestTools.assertFailure(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);

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
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleUserAccountRef(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(AccountShadowType.class,
        		accountOid, dummyResourceCtlRed.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		prismContext);
        deltas.add(accountDelta);
        
        // WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
 		result.computeStatus();
         IntegrationTestTools.assertSuccess(result);
         
 		userJack = getUser(USER_JACK_OID);
 		display("User after change execution", userJack);
 		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
 		
 		assertAccount(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);

         // Check audit
         display("Audit", dummyAuditService);
         dummyAuditService.assertSimpleRecordSanity();
         dummyAuditService.assertRecords(2);
         dummyAuditService.assertAnyRequestDeltas();
         dummyAuditService.assertExecutionDeltas(1);
         dummyAuditService.asserHasDelta(ChangeType.MODIFY, AccountShadowType.class);
         dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test126ModifyAccountShipDelete() throws Exception {
		final String TEST_NAME = "test126ModifyAccountShipDelete";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleUserAccountRef(userJack);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(AccountShadowType.class,
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
        IntegrationTestTools.assertFailure(result);
        
		userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
		
		assertAccount(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
	}
	
	
	@Test
    public void test129ModifyUserUnassignAccountBlue() throws Exception {
		final String TEST_NAME = "test109ModifyUserUnassignAccountBlue";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
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
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.DELETE, AccountShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	private void assertAccount(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException {		
		// ship inbound mapping is used, it is strong 
        String accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, task.getResult());
        assertShadowRepo(accountShadow, accountOid, "jack", resourceCtl.getResource().asObjectable());
        
        // Check account
        // All the changes should be reflected to the account
        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, task.getResult());
        assertShadowModel(accountModel, accountOid, "jack", resourceCtl.getResource().asObjectable());
        PrismAsserts.assertPropertyValue(accountModel, 
        		resourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
        		expectedFullName);
        if (expectedShip == null) {
        	PrismAsserts.assertNoItem(accountModel, 
            		resourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));        	
        } else {
        	PrismAsserts.assertPropertyValue(accountModel, 
        		resourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		expectedShip);
        }
        
        // Check account in dummy resource
        assertDummyAccount(resourceCtl.getName(), USER_JACK_USERNAME, expectedFullName, true);
	}
	

}
