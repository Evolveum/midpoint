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
package com.evolveum.midpoint.model.intest.password;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;

/**
 * Sketchy tests for deprecated password policy settings.
 * Modified subset of AbstractPasswordTest. Just makes sure that the
 * password policy configured in a deprecated way is applied and that it
 * roughly works. It is not meant to be comprehensive.
 * 
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordDeprecated extends AbstractInitializedModelIntegrationTest {
		
	protected static final String USER_PASSWORD_0_CLEAR = "d3adM3nT3llN0Tal3s";
	protected static final String USER_PASSWORD_JACK_CLEAR = "12jAcK34"; // contains username
	protected static final String USER_PASSWORD_SPARROW_CLEAR = "saRRow123"; // contains familyName
	protected static final String USER_PASSWORD_VALID_1 = "abcd123";
	protected static final String USER_PASSWORD_VALID_2 = "abcd223";
	protected static final String USER_PASSWORD_VALID_3 = "abcd323";
	protected static final String USER_PASSWORD_VALID_4 = "abcd423";

	protected static final File TEST_DIR = AbstractPasswordTest.TEST_DIR;

	protected static final File PASSWORD_POLICY_DEPRECATED_FILE = new File(TEST_DIR, "password-policy-deprecated.xml");
	protected static final String PASSWORD_POLICY_DEPRECATED_OID = "44bb6516-0d61-11e7-af71-73b639b25b04";
	
	protected String accountJackOid;
	protected XMLGregorianCalendar lastPasswordChangeStart;
	protected XMLGregorianCalendar lastPasswordChangeEnd;
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		importObjectFromFile(PASSWORD_POLICY_DEPRECATED_FILE);
		
		setGlobalSecurityPolicy(null, initResult);
		
		login(USER_ADMINISTRATOR_USERNAME);
	}

	@Test
    public void test051ModifyUserJackPassword() throws Exception {
		final String TEST_NAME = "test051ModifyUserJackPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_0_CLEAR, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertUserPassword(userJack, USER_PASSWORD_0_CLEAR);
		assertPasswordMetadata(userJack, false, startCal, endCal);
		// Password policy is not active yet. No history should be kept.
		assertPasswordHistoryEntries(userJack);
	}
	
	@Test
    public void test100ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackAssignAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
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
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDummyPassword(null, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_0_CLEAR);
	}
	
	@Test
    public void test200ApplyPasswordPolicy() throws Exception {
		final String TEST_NAME = "test200ApplyPasswordPolicy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		PrismReferenceValue passPolicyRef = new PrismReferenceValue(PASSWORD_POLICY_DEPRECATED_OID, ValuePolicyType.COMPLEX_TYPE);
		
		// WHEN
		modifyObjectReplaceReference(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				new ItemPath(SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF),
        		task, result, passPolicyRef);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	
	/**
	 * Change to password that complies with password policy.
	 */
	@Test
    public void test210ModifyUserJackPasswordGood() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test210ModifyUserJackPasswordGood",
				USER_PASSWORD_VALID_1, USER_PASSWORD_0_CLEAR);
	}
	
	/**
	 * Reconcile user. Nothing should be changed.
	 * MID-3567
	 */
	@Test
    public void test212ReconcileUserJack() throws Exception {
		final String TEST_NAME = "test212ReconcileUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_0_CLEAR);
	}
	
	/**
	 * Recompute user. Nothing should be changed.
	 * MID-3567
	 */
	@Test
    public void test214RecomputeUserJack() throws Exception {
		final String TEST_NAME = "test214RecomputeUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        recomputeUser(USER_JACK_OID, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_0_CLEAR);
	}
	
	/**
	 * Change to password that violates the password policy (but is still OK for yellow resource).
	 */
	@Test
    public void test220ModifyUserJackPasswordBadA() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test220ModifyUserJackPasswordBadA",
				USER_PASSWORD_0_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_0_CLEAR);
	}
	
	/**
	 * Change to password that violates the password policy (contains username)
	 * MID-1657
	 */
	@Test
    public void test224ModifyUserJackPasswordBadJack() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test224ModifyUserJackPasswordBadJack",
				USER_PASSWORD_JACK_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_0_CLEAR);
	}
		
	/**
	 * Change to password that complies with password policy. Again. See that 
	 * the change is applied correctly and that it is included in the history.
	 */
	@Test
    public void test230ModifyUserJackPasswordGoodAgain() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test230ModifyUserJackPasswordGoodAgain",
				USER_PASSWORD_VALID_2, USER_PASSWORD_0_CLEAR, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * Change to password that complies with password policy. Again.
	 * This time there are enough passwords in the history. So the history should
	 * be truncated.
	 */
	@Test
    public void test240ModifyUserJackPasswordGoodAgainOverHistory() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test240ModifyUserJackPasswordGoodAgainOverHistory",
				USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_2);
	}
	
	/**
	 * Change to password that complies with password policy. Again.
	 * This time there are enough passwords in the history. So the history should
	 * be truncated.
	 */
	@Test
    public void test241ModifyUserJackPasswordGoodAgainOverHistoryAgain() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test241ModifyUserJackPasswordGoodAgainOverHistoryAgain",
				USER_PASSWORD_VALID_4, USER_PASSWORD_VALID_2, USER_PASSWORD_VALID_3);
	}
	
	/**
	 * Reuse old password. Now the password should be out of the history, so
	 * the system should allow its reuse.
	 */
	@Test
    public void test248ModifyUserJackPasswordGoodReuse() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test248ModifyUserJackPasswordGoodReuse",
				USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_4);
	}
	
	private void doTestModifyUserJackPasswordSuccessWithHistory(final String TEST_NAME, 
			String newPassword, String... expectedPasswordHistory) throws Exception {
		TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();
                        
		// WHEN
        modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();
        
        assertJackPasswordsWithHistory(newPassword, expectedPasswordHistory);
	}
	
	private void doTestModifyUserJackPasswordFailureWithHistory(final String TEST_NAME, 
			String newPassword, String oldPassword, String... expectedPasswordHistory) throws Exception {
		TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        try {
			// WHEN
	        modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
	        
        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Exected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertJackPasswordsWithHistory(oldPassword, expectedPasswordHistory);
	}
	
	private void assertJackPasswordsWithHistory(String expectedCurrentPassword, String... expectedPasswordHistory) throws Exception {
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 1);

        assertUserPassword(userJack, expectedCurrentPassword);
        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
        
        assertDummyPassword(null, ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);
        		
		assertPasswordHistoryEntries(userJack, expectedPasswordHistory);
	}
	
}
