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
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestActivation extends AbstractInitializedModelIntegrationTest {
			
	private String accountOid;
	private String accountRedOid;
	private XMLGregorianCalendar lastValidityChangeTimestamp;

	public TestActivation() throws JAXBException {
		super();
	}
			
	@Test
    public void test050CheckJackEnabled() throws Exception {
        displayTestTile(this, "test050CheckJackEnabled");

        // GIVEN, WHEN
        // this happens during test initialization when user-jack.xml is added
        
        // THEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test050CheckJackEnabled");
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeEnabled(userJack);
		// Cannot assert validity or effective status here. The user was added through repo and was not recomputed yet.
	}

	@Test
    public void test051ModifyUserJackDisable() throws Exception {
        displayTestTile(this, "test051ModifyUserJackDisable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test051ModifyUserJackDisable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertDisabled(userJack);
		assertValidity(userJack, null);
		assertEffectiveStatus(userJack, ActivationStatusType.DISABLED);
	}
	
	@Test
    public void test052ModifyUserJackEnable() throws Exception {
        displayTestTile(this, "test052ModifyUserJackEnable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test052ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeEnabled(userJack);
		assertValidity(userJack, null);
		assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);
	}
	
	@Test
    public void test100ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackAssignAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
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
        
        assertDummyEnabled("jack");
	}
	
	@Test
    public void test101ModifyUserJackDisable() throws Exception {
        displayTestTile(this, "test051ModifyUserJackDisable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test051ModifyUserJackDisable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertDisabled(userJack);
		assertDummyDisabled("jack");
	}
	
	@Test
    public void test102ModifyUserJackEnable() throws Exception {
        displayTestTile(this, "test052ModifyUserJackEnable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test052ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeEnabled(userJack);
		assertDummyEnabled("jack");
	}
	

	/**
	 * Modify account activation. User's activation should be unchanged
	 */
	@Test
    public void test111ModifyAccountJackDisable() throws Exception {
        displayTestTile(this, "test111ModifyAccountJackDisable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test111ModifyAccountJackDisable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyAccountShadowReplace(accountOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeEnabled(userJack);
		assertDummyDisabled("jack");
	}
	
	/**
	 * Re-enabling the user should enable the account sa well. Even if the user is already enabled.
	 */
	@Test
    public void test112ModifyUserJackEnable() throws Exception {
        displayTestTile(this, "test112ModifyUserJackEnable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test112ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeEnabled(userJack);
		assertDummyEnabled("jack");
	}
	
	/**
	 * Modify both user and account activation. As password outbound mapping is weak the user should have its own state
	 * and account should have its own state.
	 */
	@Test
    public void test113ModifyJackActivationUserAndAccount() throws Exception {
        displayTestTile(this, "test113ModifyJackActivationUserAndAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test113ModifyJackActivationUserAndAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountOid, resourceDummy, 
        		ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);        
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeEnabled(userJack);
		assertDummyDisabled("jack");
	}
	
	/**
	 * Add red dummy resource to the mix. This would be fun.
	 */
	@Test
    public void test120ModifyUserJackAssignAccountDummyRed() throws Exception {
        displayTestTile(this, "test120ModifyUserJackAssignAccountDummyRed");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test120ModifyUserJackAssignAccountDummyRed");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getUserAccountRef(userJack, RESOURCE_DUMMY_RED_OID);
                
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);
        
        assertAdministrativeEnabled(userJack);
		assertDummyDisabled("jack");
		assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	/**
	 * Modify both user and account activation. Red dummy has a strong mapping. User change should override account
	 * change.
	 */
	@Test
    public void test121ModifyJackUserAndAccountRed() throws Exception {
		final String TEST_NAME = "test121ModifyJackUserAndAccountRed";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountRedOid, resourceDummy, 
        		ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);        
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

        assertDisabled(userJack);
		assertDummyDisabled("jack");
		assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	@Test
    public void test130ModifyAccountDefaultAndRed() throws Exception {
        displayTestTile(this, "test130ModifyAccountDefaultAndRed");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test121ModifyJackPasswordUserAndAccountRed");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ObjectDelta<ShadowType> accountDeltaDefault = createModifyAccountShadowReplaceDelta(accountOid, 
        		resourceDummy, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> accountDeltaRed = createModifyAccountShadowReplaceDelta(accountRedOid, 
        		resourceDummyRed, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDeltaDefault, accountDeltaRed);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

        assertDisabled(userJack);
		assertDummyEnabled("jack");
		assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	/**
	 * Let's make a clean slate for the next test
	 */
	@Test
    public void test138ModifyJackEnabled() throws Exception {
		final String TEST_NAME = "test138ModifyJackEnabled";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeEnabled(userJack);
		assertDummyEnabled("jack");
		assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	/**
	 * Red dummy resource disables account instead of deleting it.
	 */
	@Test
    public void test139ModifyUserJackUnAssignAccountDummyRed() throws Exception {
		final String TEST_NAME = "test139ModifyUserJackUnAssignAccountDummyRed";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getUserAccountRef(userJack, RESOURCE_DUMMY_RED_OID);
                
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", false);
        
        assertAdministrativeEnabled(userJack);
        assertDummyEnabled("jack");
		assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	@Test
    public void test199DeleteUserJack() throws Exception {
		final String TEST_NAME = "test199DeleteUserJack";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
        deltas.add(userDelta);
                
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
                
        // Check that the accounts are gone
        assertNoDummyAccount(null, "jack");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	@Test
    public void test200AddUserLargo() throws Exception {
		final String TEST_NAME = "test200AddUserLargo";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userLargo = PrismTestUtil.parseObject(new File(USER_LARGO_FILENAME));
        ObjectDelta<UserType> addDelta = userLargo.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(addDelta);
        
        // WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.IN);
		assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
		assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
	}
	
	@Test
    public void test205ModifyUserLargoAssignAccount() throws Exception {
		final String TEST_NAME = "test205ModifyUserLargoAssignAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_LARGO_OID, RESOURCE_DUMMY_OID, null, true);
        accountAssignmentUserDelta.addModificationAddProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Largo LaGrande"));
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
	}
	
	@Test
    public void test210ModifyLargoValidTo10MinsAgo() throws Exception {
		final String TEST_NAME = "test210ModifyLargoValidTo10MinsAgo";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar tenMinutesAgo = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis() - 10 * 60 * 1000);
        
        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, tenMinutesAgo);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.AFTER);
		assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
		assertEffectiveStatus(userLargo, ActivationStatusType.DISABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
	}
	
	@Test
    public void test211ModifyLargoValidToManana() throws Exception {
		final String TEST_NAME = "test211ModifyLargoValidToManana";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar manana = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000);
        
        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, manana);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.IN);
		assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
		lastValidityChangeTimestamp = userLargo.asObjectable().getActivation().getValidityChangeTimestamp();
		assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
	}
	
	/**
	 * Move time to tomorrow. Nothing should change yet. It is not yet manana.
	 */
	@Test
    public void test212SeeLargoTomorrow() throws Exception {
		final String TEST_NAME = "test212SeeLargoTomorrow";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Let's play with the clock, move the time to tomorrow
        long crrentNow = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        clock.override(crrentNow);
        
        // WHEN
        modelService.recompute(UserType.class, USER_LARGO_OID, task, result);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.IN);
		assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
		assertValidityTimestamp(userLargo, lastValidityChangeTimestamp);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
	}
	
	/**
	 * Move time after manana. Largo should be invalid.
	 */
	@Test
    public void test213HastaLaMananaLargo() throws Exception {
		final String TEST_NAME = "test213HastaLaMananaLargo";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Let's play with the clock, move the time forward 20 days
        long crrentNow = System.currentTimeMillis() + 20 *24 * 60 * 60 * 1000;
        clock.override(crrentNow);
        
        // WHEN
        modelService.recompute(UserType.class, USER_LARGO_OID, task, result);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.AFTER);
		assertValidityTimestamp(userLargo, crrentNow);
		assertEffectiveStatus(userLargo, ActivationStatusType.DISABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
	}
	
	
	private void assertDummyActivationEnabledState(String userId, boolean expectedEnabled) {
		assertDummyActivationEnabledState(null, userId, expectedEnabled);
	}
	
	private void assertDummyActivationEnabledState(String instance, String userId, boolean expectedEnabled) {
		DummyAccount account = getDummyAccount(instance, userId);
		assertNotNull("No dummy account "+userId, account);
		assertEquals("Wrong enabled flag in dummy '"+instance+"' account "+userId, expectedEnabled, account.isEnabled());
	}
	
	private void assertDummyEnabled(String userId) {
		assertDummyActivationEnabledState(userId, true);
	}
	
	private void assertDummyDisabled(String userId) {
		assertDummyActivationEnabledState(userId, false);
	}
	
	private void assertDummyEnabled(String instance, String userId) {
		assertDummyActivationEnabledState(instance, userId, true);
	}
	
	private void assertDummyDisabled(String instance, String userId) {
		assertDummyActivationEnabledState(instance, userId, false);
	}
	
	private void assertDisabled(PrismObject<UserType> user) {
		PrismProperty<ActivationStatusType> statusProperty = user.findProperty(ACTIVATION_ADMINISTRATIVE_STATUS_PATH);
		assert statusProperty != null : "No status property in "+user;
		ActivationStatusType status = statusProperty.getRealValue();
		assert status != null : "No status property is null in "+user;
		assert status != ActivationStatusType.ENABLED : "status property is "+status+" in "+user;
	}
	
	private void assertValidity(PrismObject<UserType> user, TimeIntervalStatusType expectedValidityStatus) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		assertEquals("Unexpected validity status in "+user, expectedValidityStatus, activation.getValidityStatus());
	}
	
	private void assertValidityTimestamp(PrismObject<UserType> user, long expected) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
		assertNotNull("No validityChangeTimestamp in "+user, validityChangeTimestamp);
		assertEquals("wrong validityChangeTimestamp", expected, XmlTypeConverter.toMillis(validityChangeTimestamp));
	}
	
	private void assertValidityTimestamp(PrismObject<UserType> user, XMLGregorianCalendar expected) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
		assertNotNull("No validityChangeTimestamp in "+user, validityChangeTimestamp);
		assertEquals("wrong validityChangeTimestamp", expected, validityChangeTimestamp);
	}
	
	private void assertValidityTimestamp(PrismObject<UserType> user, long lowerBound, long upperBound) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
		assertNotNull("No validityChangeTimestamp in "+user, validityChangeTimestamp);
		long validityMillis = XmlTypeConverter.toMillis(validityChangeTimestamp);
		if (validityMillis > lowerBound && validityMillis < upperBound) {
			return;
		}
		AssertJUnit.fail("Expected validityChangeTimestamp to be between "+lowerBound+" and "+upperBound+", but it was "+validityMillis);
	}
	
	private void assertEffectiveStatus(PrismObject<UserType> user, ActivationStatusType expected) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		assertEquals("Unexpected effective activation status in "+user, expected, activation.getEffectiveStatus());
	}

}
