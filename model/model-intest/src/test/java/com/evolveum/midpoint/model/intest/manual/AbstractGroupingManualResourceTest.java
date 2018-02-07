/*
 * Copyright (c) 2010-2018 Evolveum
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

/**
 *
 */
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractGroupingManualResourceTest extends AbstractManualResourceTest {

	protected static final File TEST_DIR = new File("src/test/resources/manual/");

	protected static final File RESOURCE_MANUAL_GROUPING_FILE = new File(TEST_DIR, "resource-manual-grouping.xml");
	protected static final String RESOURCE_MANUAL_GROUPING_OID = "a6e228a0-f092-11e7-b5bc-579f2e54e15c";
	
	protected static final File RESOURCE_SEMI_MANUAL_GROUPING_FILE = new File(TEST_DIR, "resource-semi-manual-grouping.xml");
	protected static final String RESOURCE_SEMI_MANUAL_GROUPING_OID = "9eddca88-f222-11e7-98dc-cb6e4b08800c";

	protected static final File ROLE_ONE_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-one-manual-grouping.xml");
	protected static final String ROLE_ONE_MANUAL_GROUPING_OID = "bc586500-f092-11e7-9cda-f7cd4203a755";
	
	protected static final File ROLE_ONE_SEMI_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-one-semi-manual-grouping.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_GROUPING_OID = "dc961c9a-f222-11e7-b19a-0fa30f483712";

	protected static final File ROLE_TWO_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-two-manual-grouping.xml");
	protected static final String ROLE_TWO_MANUAL_GROUPING_OID = "c9de1300-f092-11e7-8c5f-3ff8ea609a1d";
	
	protected static final File ROLE_TWO_SEMI_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-two-semi-manual-grouping.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_GROUPING_OID = "17fafa4e-f223-11e7-bbee-ff66557fc83f";
	
	protected static final File TASK_PROPAGATION_MANUAL_GROUPING_FILE = new File(TEST_DIR, "task-propagation-manual-grouping.xml");
	protected static final String TASK_PROPAGATION_MANUAL_GROUPING_OID = "b84a2c46-f0b5-11e7-baff-d35c2f14080f";
	
	protected static final File TASK_PROPAGATION_SEMI_MANUAL_GROUPING_FILE = new File(TEST_DIR, "task-propagation-semi-manual-grouping.xml");
	protected static final String TASK_PROPAGATION_SEMI_MANUAL_GROUPING_OID = "01db4542-f224-11e7-8833-bbe6634814e7";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractGroupingManualResourceTest.class);

	protected String propagationTaskOid = null;
	protected XMLGregorianCalendar accountWillExecutionTimestampStart;
	protected XMLGregorianCalendar accountWillExecutionTimestampEnd;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
	
	@Override
	protected boolean isDirect() {
		return false;
	}
	
	@Override
	protected void runPropagation() throws Exception {
		if (propagationTaskOid == null) {
			addTask(getPropagationTaskFile());
			propagationTaskOid = getPropagationTaskOid();
			waitForTaskStart(propagationTaskOid, true);
		} else {
			restartTask(propagationTaskOid);
		}
		waitForTaskFinish(propagationTaskOid, true);
	}

	protected abstract String getPropagationTaskOid();
	
	protected abstract File getPropagationTaskFile();
	
	// Grouping execution. The operation is delayed for a while.
	@Override
	protected PendingOperationExecutionStatusType getExpectedExecutionStatus(PendingOperationExecutionStatusType executionStage) {
		return executionStage;
	}
	
	@Override
	protected OperationResultStatusType getExpectedResultStatus(PendingOperationExecutionStatusType executionStage) {
		if (executionStage == PendingOperationExecutionStatusType.EXECUTION_PENDING) {
			return null;
		} if (executionStage == PendingOperationExecutionStatusType.EXECUTING) {
			return OperationResultStatusType.IN_PROGRESS;
		}
		return null;
	}
	
	/**
	 * disable - do not run propagation yet 
	 * (do not wait for delta to expire, we want several deltas at once so we can test grouping).
	 */
	@Test
	public void test220ModifyUserWillDisable() throws Exception {
		final String TEST_NAME = "test220ModifyUserWillDisable";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(userWillOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null, 
				null, null);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 2);
		pendingOperation = findPendingOperation(shadowModel,
				PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null, 
				null, null);
		
		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.DISABLED);
		assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModelFuture);

		assertNull("Unexpected async reference in result", willLastCaseOid);
	}
	
	/**
	 * Do NOT run propagation yet. Change password, enable. 
	 * There is still pending disable delta. Make sure all the deltas are
	 * stored correctly.
	 */
	@Test
	public void test230ModifyAccountWillChangePasswordAndEnable() throws Exception {
		final String TEST_NAME = "test230ModifyAccountWillChangePasswordAndEnable";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class,
				userWillOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext,
				ActivationStatusType.ENABLED);
		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue(USER_WILL_PASSWORD_NEW);
		delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, ps);
		display("ObjectDelta", delta);

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		executeChanges(delta, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertInProgress(result);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
		
		assertAccountWillAfterChangePasswordAndEnable(TEST_NAME);
	}
	
	protected void assertAccountWillAfterChangePasswordAndEnable(final String TEST_NAME) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 3);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, 
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null, 
				null, null);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		pendingOperation = findPendingOperation(shadowModel,
				PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowModel, pendingOperation, 
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null, 
				null, null);

		PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadow(shadowProvisioningFuture);

		assertNull("Unexpected async reference in result", willSecondLastCaseOid);
	}

	/**
	 * Run propagation before the interval is over. Nothing should happen.
	 */
	@Test
	public void test232RunPropagationBeforeInterval() throws Exception {
		final String TEST_NAME = "test235RunPropagationAfterInterval";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);

		assertAccountWillAfterChangePasswordAndEnable(TEST_NAME);
	}
	
	/**
	 * ff 2min, run propagation. There should be just one operation (one case) that
	 * combines both deltas.
	 */
	@Test
	public void test235RunPropagationAfterInterval() throws Exception {
		final String TEST_NAME = "test235RunPropagationAfterInterval";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT2M");

		accountWillExecutionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);

		accountWillExecutionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		
		assertPendingOperationDeltas(shadowRepo, 3);
		
		PendingOperationType pendingOperation1 = findPendingOperation(shadowRepo,
				PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation1, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
				null, null);
		willLastCaseOid = pendingOperation1.getAsynchronousOperationReference();
		assertNotNull("No case ID in pending operation", willLastCaseOid);
		
		PendingOperationType pendingOperation2 = findPendingOperation(shadowRepo,
				PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation2, 
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
				null, null);
		assertNotNull("No ID in pending operation", pendingOperation2.getId());
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());
		
		// TODO: check execution timestamps
		
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		
		pendingOperation1 = findPendingOperation(shadowModel,
				PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation1, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
				null, null);
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation1.getAsynchronousOperationReference());
		
		pendingOperation2 = findPendingOperation(shadowModel,
				PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowModel, pendingOperation2, 
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd, 
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS, 
				null, null);
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());
		
		// TODO: check execution timestamps

		PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowProvisioningFuture);
		assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadow(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		
		// TODO: check number of cases
	}
	
	/**
	 * Close the case. Both deltas should be marked as completed.
	 * Do NOT explicitly refresh the shadow in this case. Just reading it should cause the refresh.
	 */
	@Test
	public void test240CloseCaseAndReadAccountWill() throws Exception {
		final String TEST_NAME = "test240CloseCaseAndReadAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();
		
		assertAccountWillAfterChangePasswordAndEnableCaseClosed(TEST_NAME, shadowModel);
		
	}
	
	/**
	 * lets ff 5min just for fun. Refresh, make sure everything should be the same (grace not expired yet)
	 */
	@Test
	public void test250RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test250RecomputeWillAfter5min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

		PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
		display("Shadow before", shadowBefore);

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertAccountWillAfterChangePasswordAndEnableCaseClosed(TEST_NAME, null);
	}
	
	@Test
	public void test272UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test272UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		//  ff 7min. Refresh. Oldest delta should expire.
		clockForward("PT7M");

		backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.ENABLED, USER_WILL_PASSWORD_NEW);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 2);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * ff 15min. Refresh. All delta should expire.
	 */
	@Test
	public void test290RecomputeWillAfter15min() throws Exception {
		final String TEST_NAME = "test290RecomputeWillAfter15min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT15M");

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 0);

		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 0);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModelFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	@Test
	public void test300UnassignAccountWill() throws Exception {
		final String TEST_NAME = "test300UnassignAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		unassignRole(userWillOid, getRoleOneOid(), task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 1);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, PendingOperationExecutionStatusType.EXECUTION_PENDING);
		assertPendingOperation(shadowRepo, pendingOperation, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
				null, null);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 1);
		pendingOperation = findPendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTION_PENDING);
		assertPendingOperation(shadowRepo, pendingOperation, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
				null, null);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, true);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);

		assertNull("Unexpected async reference in result", willLastCaseOid);
		
//		assertNotNull("No async reference in result", willLastCaseOid);
//		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * ff 2min, run propagation.
	 */
	@Test
	public void test302RunPropagationAfterInterval() throws Exception {
		final String TEST_NAME = "test302RunPropagationAfterInterval";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT2M");

		accountWillExecutionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);

		accountWillExecutionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 1);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, PendingOperationExecutionStatusType.EXECUTING);
		assertPendingOperation(shadowRepo, pendingOperation, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
				null, null);
		willLastCaseOid = pendingOperation.getAsynchronousOperationReference();
		assertNotNull("No case ID in pending operation", willLastCaseOid);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 1);
		pendingOperation = findPendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING);
		assertPendingOperation(shadowRepo, pendingOperation, 
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
				null, null);
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation.getAsynchronousOperationReference());

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, true);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);
		
		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}
	
	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test310CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test310CloseCaseAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		// We need reconcile and not recompute here. We need to fetch the updated case status.
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertUnassignedShadow(shadowModel, ActivationStatusType.ENABLED); // backing store not yet updated
		assertShadowPassword(shadowModel);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, true);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	@Test
	public void test330UpdateBackingStoreAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test330UpdateBackingStoreAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreDeprovisionWill();
		displayBackingStore();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertUnassignedShadow(shadowModel, ActivationStatusType.DISABLED);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, false);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}
	
	/**
	 * Put everything in a clean state so we can start over.
	 */
	@Test
	public void test349CleanUp() throws Exception {
		final String TEST_NAME = "test349CleanUp";
		displayTestTitle(TEST_NAME);

		cleanupUser(TEST_NAME, userWillOid, USER_WILL_NAME, accountWillOid);
	}
	
	protected void assertUnassignedShadow(PrismObject<ShadowType> shadow, ActivationStatusType expectAlternativeActivationStatus) {
		assertShadowDead(shadow);
	}
	
	protected void assertAccountWillAfterChangePasswordAndEnableCaseClosed(final String TEST_NAME, PrismObject<ShadowType> shadowModel) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 3);

		PendingOperationType pendingOperation1 = findPendingOperation(shadowRepo,
				PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation1,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation1.getAsynchronousOperationReference());

		PendingOperationType pendingOperation2 = findPendingOperation(shadowRepo,
				PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation2, 
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());

		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		if (shadowModel == null) {
			shadowModel = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
		}
		display("Model shadow", shadowModel);
		ShadowType shadowTypeModel = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);

		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		// Password is here, even though it (strictly speaking) should not be here
		// It is here because we have just applied the delta. So we happen to know the password now.
		// The password will NOT be there in next recompute.
//		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		
		pendingOperation1 = findPendingOperation(shadowModel,
				PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation1,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation1.getAsynchronousOperationReference());
		
		pendingOperation2 = findPendingOperation(shadowModel,
				PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation2, 
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());

		
		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
		assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
		assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	
}
