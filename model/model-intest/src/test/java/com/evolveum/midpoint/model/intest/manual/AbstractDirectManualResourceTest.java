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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractDirectManualResourceTest extends AbstractManualResourceTest {

	protected static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
	protected static final String RESOURCE_MANUAL_OID = "0ef80ab8-2906-11e7-b81a-3f343e28c264";

	protected static final File RESOURCE_SEMI_MANUAL_FILE = new File(TEST_DIR, "resource-semi-manual.xml");
	protected static final String RESOURCE_SEMI_MANUAL_OID = "aea5a57c-2904-11e7-8020-7b121a9e3595";

	protected static final File RESOURCE_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "resource-semi-manual-disable.xml");
	protected static final String RESOURCE_SEMI_MANUAL_DISABLE_OID = "5e497cb0-5cdb-11e7-9cfe-4bfe0342d181";

	protected static final File RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "resource-semi-manual-slow-proposed.xml");
	protected static final String RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_OID = "512d749a-75ff-11e7-8176-8be7fb6f4e45";
	
	protected static final File RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "resource-semi-manual-disable-slow-proposed.xml");
	protected static final String RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "8ed29734-a1ed-11e7-b7f9-7bce8b17fd64";

	protected static final File ROLE_ONE_MANUAL_FILE = new File(TEST_DIR, "role-one-manual.xml");
	protected static final String ROLE_ONE_MANUAL_OID = "9149b3ca-5da1-11e7-8e84-130a91fb5876";

	protected static final File ROLE_ONE_SEMI_MANUAL_FILE = new File(TEST_DIR, "role-one-semi-manual.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_OID = "29eab2c8-5da2-11e7-85d3-c78728e05ca3";

	protected static final File ROLE_ONE_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "role-one-semi-manual-disable.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_DISABLE_OID = "3b8cb17a-5da2-11e7-b260-a760972b54fb";

	protected static final File ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-one-semi-manual-slow-proposed.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_OID = "ca7fefc6-75ff-11e7-9833-572f6bf86a81";
	
	protected static final File ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-one-semi-manual-disable-slow-proposed.xml");
	protected static final String ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "38c9fc7a-a200-11e7-8157-2f9beeb541bc";

	protected static final File ROLE_TWO_MANUAL_FILE = new File(TEST_DIR, "role-two-manual.xml");
	protected static final String ROLE_TWO_MANUAL_OID = "414e3766-775e-11e7-b8cb-c7ca37c1dc9e";

	protected static final File ROLE_TWO_SEMI_MANUAL_FILE = new File(TEST_DIR, "role-two-semi-manual.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_OID = "b95f7252-7776-11e7-bd96-cf05f4c21966";

	protected static final File ROLE_TWO_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "role-two-semi-manual-disable.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_DISABLE_OID = "d1eaa4f4-7776-11e7-bb53-eb1218c49dd9";

	protected static final File ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-two-semi-manual-slow-proposed.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_OID = "eaf3569e-7776-11e7-93f3-3f1b853d6525";

	protected static final File ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-two-semi-manual-disable-slow-proposed.xml");
	protected static final String ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "5ecd6fa6-a200-11e7-b0cb-af5e1792d327";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractDirectManualResourceTest.class);

	private XMLGregorianCalendar roleTwoValidFromTimestamp;
	
	protected String accountBarbossaOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		addObject(USER_BARBOSSA_FILE);
	}
	
	@Override
	protected boolean isDirect() {
		return true;
	}

	/**
	 * disable - do not complete yet (do not wait for delta to expire, we want several deltas at once).
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
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Change password, enable. There is still pending disable delta. Make sure all the deltas are
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
		willSecondLastCaseOid = assertInProgress(result);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 3);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

		assertNotNull("No ID in pending operation", pendingOperation.getId());
		// Still old data in the repo. The operation is not completed yet.
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowProvisioning = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowProvisioning);
		ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
		assertShadowName(shadowProvisioning, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
		assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowProvisioning);

		assertPendingOperationDeltas(shadowProvisioning, 3);
		pendingOperation = findPendingOperation(shadowProvisioning,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowProvisioning, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

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

		assertNotNull("No async reference in result", willSecondLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Disable case is closed. The account should be disabled. But there is still the other
	 * delta pending.
	 * Do NOT explicitly refresh the shadow in this case. Just reading it should cause the refresh.
	 */
	@Test
	public void test240CloseDisableCaseAndReadAccountWill() throws Exception {
		final String TEST_NAME = "test240CloseDisableCaseAndReadAccountWill";
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

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 3);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.DISABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeModel = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		}

		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
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

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 3);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

		pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.DISABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		}
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		assertPendingOperation(shadowModel, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

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
		// TODO
//		assertShadowPassword(shadowProvisioningFuture);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	@Test
	public void test252UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test252UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.DISABLED, USER_WILL_PASSWORD_OLD);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeModel = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 3);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Password change/enable case is closed. The account should be disabled. But there is still the other
	 * delta pending.
	 */
	@Test
	public void test260ClosePasswordChangeCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test260ClosePasswordChangeCaseAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willSecondLastCaseOid);

		accountWillSecondCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillSecondCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 3);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

		pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		}
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 3);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 7min. Refresh. Oldest delta should expire.
	 */
	@Test
	public void test270RecomputeWillAfter7min() throws Exception {
		final String TEST_NAME = "test130RefreshAccountWillAfter7min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT7M");

		PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
		display("Shadow before", shadowBefore);

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 2);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

		pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		if (supportsBackingStore()) {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
		} else {
			assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		}
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 2);
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	@Test
	public void test272UpdateBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test272UpdateBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 5min. Refresh. Another delta should expire.
	 */
	@Test
	public void test280RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test280RecomputeWillAfter5min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 1);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

		pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

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
		pendingOperation = findPendingOperation(shadowModel,
				OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 5min. Refresh. All delta should expire.
	 */
	@Test
	public void test290RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test290RecomputeWillAfter5min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

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
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
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
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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
		pendingOperation = findPendingOperation(shadowModel, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Recompute. Make sure model will not try to delete the account again.
	 */
	@Test
	public void test302RecomputeWill() throws Exception {
		final String TEST_NAME = "test302RecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 1);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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
		pendingOperation = findPendingOperation(shadowModel, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

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

		assertNotNull("No async reference in result", willLastCaseOid);

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

	protected void assertUnassignedShadow(PrismObject<ShadowType> shadow, ActivationStatusType expectAlternativeActivationStatus) {
		assertShadowDead(shadow);
	}

	/**
	 * ff 5min, everything should be the same (grace not expired yet)
	 */
	@Test
	public void test320RecomputeWillAfter5min() throws Exception {
		final String TEST_NAME = "test320RecomputeWillAfter5min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

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

	// TODO: nofetch, nofetch+future

	/**
	 * ff 20min, grace period expired, shadow should be gone, linkRef shoud be gone.
	 */
	@Test
	public void test340RecomputeWillAfter25min() throws Exception {
		final String TEST_NAME = "test340RecomputeWillAfter25min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT20M");

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		assertDeprovisionedTimedOutUser(userAfter, accountWillOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	// TODO: create, close case, then update backing store.

	// TODO: let grace period expire without updating the backing store (semi-manual-only)

	protected void assertDeprovisionedTimedOutUser(PrismObject<UserType> userAfter, String accountOid) throws Exception {
		assertLinks(userAfter, 0);
		assertNoShadow(accountOid);
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

	protected void cleanupUser(final String TEST_NAME, String userOid, String username, String accountOid) throws Exception {
		// nothing to do here
	}

	/**
	 * MID-4037
	 */
	@Test
	public void test500AssignWillRoleOne() throws Exception {
		assignWillRoleOne("test500AssignWillRoleOne", USER_WILL_FULL_NAME_PIRATE, PendingOperationExecutionStatusType.EXECUTION_PENDING);
	}

	/**
	 * Unassign account before anybody had the time to do anything about it.
	 * Create ticket is not closed, the account is not yet created and we
	 * want to delete it.
	 * MID-4037
	 */
	@Test
	public void test510UnassignWillRoleOne() throws Exception {
		final String TEST_NAME = "test510UnassignWillRoleOne";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		unassignRole(userWillOid, getRoleOneOid(), task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willSecondLastCaseOid = assertInProgress(result);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, ChangeTypeType.ADD);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.IN_PROGRESS,
				null, null);
		assertNotNull("No ID in pending operation", pendingOperation.getId());

		assertWillUnassignPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);

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
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);

		assertPendingOperationDeltas(shadowModel, 2);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertWillUnassignedFuture(shadowModelFuture, false);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);

		assertNotNull("No async reference in result", willSecondLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * Just make sure recon does not ruin anything.
	 * MID-4037
	 */
	@Test
	public void test512ReconcileWill() throws Exception {
		final String TEST_NAME = "test512ReconcileWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, ChangeTypeType.ADD);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.IN_PROGRESS,
				null, null);
		assertNotNull("No ID in pending operation", pendingOperation.getId());

		assertWillUnassignPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);

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
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);

		assertPendingOperationDeltas(shadowModel, 2);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_WILL_NAME);
		assertWillUnassignedFuture(shadowModelFuture, false);

		// Make sure that the account is still linked
		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		String accountWillOidAfter = getSingleLinkOid(userAfter);
		assertEquals(accountWillOid, accountWillOidAfter);
		assertNoAssignments(userAfter);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	protected void assertWillUnassignPendingOperation(PrismObject<ShadowType> shadowRepo, OperationResultStatusType expectedStatus) {
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.IN_PROGRESS, ChangeTypeType.DELETE);
		if (expectedStatus == OperationResultStatusType.IN_PROGRESS) {
			assertPendingOperation(shadowRepo, pendingOperation,
					accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
					OperationResultStatusType.IN_PROGRESS,
					null, null);
		} else {
			pendingOperation = findPendingOperation(shadowRepo,
					OperationResultStatusType.SUCCESS, ChangeTypeType.DELETE);
			assertPendingOperation(shadowRepo, pendingOperation,
					accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
					OperationResultStatusType.SUCCESS,
					accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
			assertNotNull("No ID in pending operation", pendingOperation.getId());
		}
		assertNotNull("No ID in pending operation", pendingOperation.getId());
	}

	/**
	 * Close both cases at the same time.
	 * MID-4037
	 */
	@Test
	public void test515CloseCasesAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test515CloseCasesAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);
		closeCase(willSecondLastCaseOid);

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

		assertPendingOperationDeltas(shadowRepo, 2);

		PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
				OperationResultStatusType.SUCCESS, ChangeTypeType.ADD);
		assertPendingOperation(shadowRepo, pendingOperation,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());

		assertWillUnassignPendingOperation(shadowRepo, OperationResultStatusType.SUCCESS);

		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountWillOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertUnassignedShadow(shadowModel, null); // Shadow in not in the backing store

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountWillOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertWillUnassignedFuture(shadowModelFuture, true);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * ff 35min, grace period expired, shadow should be gone, linkRef should be gone.
	 * So we have clean state for next tests.
	 * MID-4037
	 */
	@Test
	public void test517RecomputeWillAfter35min() throws Exception {
		final String TEST_NAME = "test517RecomputeWillAfter35min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT35M");

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		// Shadow will not stay even in the "disable" case.
		// It was never created in the backing store
		assertLinks(userAfter, 0);

		// TODO: why?
//		assertNoShadow(accountWillOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * Put everything in a clean state so we can start over.
	 */
	@Test
	public void test519CleanUp() throws Exception {
		final String TEST_NAME = "test519CleanUp";
		displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		cleanupUser(TEST_NAME, userWillOid, USER_WILL_NAME, accountWillOid);

		// Make sure that all pending operations are expired
		clockForward("PT1H");
		recomputeUser(userWillOid, task, result);
		assertSuccess(result);

		assertNoShadow(accountWillOid);
	}

	/**
	 * MID-4095
	 */
	@Test
	public void test520AssignWillRoleOne() throws Exception {
		assignWillRoleOne("test520AssignWillRoleOne", USER_WILL_FULL_NAME_PIRATE, PendingOperationExecutionStatusType.EXECUTION_PENDING);
	}


	/**
	 * Not much happens here yet. Role two is assigned. But it has validity in the future.
	 * Therefore there are no changes in the account, no new pending deltas.
	 * MID-4095
	 */
	@Test
	public void test522AssignWillRoleTwoValidFrom() throws Exception {
		final String TEST_NAME = "test522AssignWillRoleTwoValidFrom";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ActivationType activationType = new ActivationType();
		roleTwoValidFromTimestamp = getTimestamp("PT2H");
		activationType.setValidFrom(roleTwoValidFromTimestamp);

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(userWillOid, getRoleTwoOid(), activationType, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		accountWillOid = getSingleLinkOid(userAfter);

		accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME_PIRATE, PendingOperationExecutionStatusType.EXECUTION_PENDING);
	}


	/**
	 * Two hours forward. Role two is valid now.
	 * MID-4095
	 */
	@Test
	public void test524TwoHoursForRoleTwo() throws Exception {
		final String TEST_NAME = "test524TwoHoursForRoleTwo";
		displayTestTitle(TEST_NAME);
		// GIVEN

		clockForward("PT2H5M");

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willSecondLastCaseOid = assertInProgress(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		accountWillOid = getSingleLinkOid(userAfter);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);

		ObjectDeltaType deltaModify = null;
		ObjectDeltaType deltaAdd = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			assertEquals("Wrong pending operation result", OperationResultStatusType.IN_PROGRESS, pendingOperation.getResultStatus());
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
				deltaAdd = delta;
			}
			if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
				deltaModify = delta;
				assertEquals("Wrong case ID", willSecondLastCaseOid, pendingOperation.getAsynchronousOperationReference());
			}
		}
		assertNotNull("No add pending delta", deltaAdd);
		assertNotNull("No modify pending delta", deltaModify);

		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertShadowExists(shadowRepo, false);
		assertNoShadowPassword(shadowRepo);

		// TODO: assert future

		assertNotNull("No async reference in result", willSecondLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * MID-4095
	 */
	@Test
	public void test525CloseCasesAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test515CloseCasesAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);
		closeCase(willSecondLastCaseOid);

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

		assertPendingOperationDeltas(shadowRepo, 2);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
		assertCase(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * Unassign both roles at the same time.
	 * Note: In semi-manual cases the backing store is never updated.
	 */
	@Test
	public void test526UnassignWillBothRoles() throws Exception {
		final String TEST_NAME = "test526UnassignWillBothRoles";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBefore = getUser(userWillOid);

		accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);

		unassignAll(userBefore, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);

		assertTest526Deltas(shadowRepo, result);

		assertNotNull("No async reference in result", willLastCaseOid);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	protected void assertTest526Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
		assertPendingOperationDeltas(shadowRepo, 3);

		ObjectDeltaType deltaModify = null;
		ObjectDeltaType deltaAdd = null;
		ObjectDeltaType deltaDelete = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
				deltaAdd = delta;
				assertEquals("Wrong status in add delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
			}
			if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
				deltaModify = delta;
				assertEquals("Wrong status in modify delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
			}
			if (ChangeTypeType.DELETE.equals(delta.getChangeType())) {
				deltaDelete = delta;
				assertEquals("Wrong status in delete delta", OperationResultStatusType.IN_PROGRESS, pendingOperation.getResultStatus());
			}
		}
		assertNotNull("No add pending delta", deltaAdd);
		assertNotNull("No modify pending delta", deltaModify);
		assertNotNull("No delete pending delta", deltaDelete);
	}

	@Test
	public void test528CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test528CloseCaseAndRecomputeWill";
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

		assertTest528Deltas(shadowRepo, result);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	protected void assertTest528Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
		assertPendingOperationDeltas(shadowRepo, 3);

		ObjectDeltaType deltaModify = null;
		ObjectDeltaType deltaAdd = null;
		ObjectDeltaType deltaDelete = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			assertEquals("Wrong status in pending delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
				deltaAdd = delta;
			}
			if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
				deltaModify = delta;
			}
			if (ChangeTypeType.DELETE.equals(delta.getChangeType())) {
				deltaDelete = delta;
			}
		}
		assertNotNull("No add pending delta", deltaAdd);
		assertNotNull("No modify pending delta", deltaModify);
		assertNotNull("No delete pending delta", deltaDelete);

	}

	/**
	 * Put everything in a clean state so we can start over.
	 */
	@Test
	public void test529CleanUp() throws Exception {
		final String TEST_NAME = "test529CleanUp";
		displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		cleanupUser(TEST_NAME, userWillOid, USER_WILL_NAME, accountWillOid);

		// Make sure that all pending operations are expired
		clockForward("PT1H");
		recomputeUser(userWillOid, task, result);
		assertSuccess(result);

		assertNoShadow(accountWillOid);
	}

	// Tests 7xx are in the subclasses

	/**
	 * The 8xx tests is similar routine as 1xx,2xx,3xx, but this time
	 * with automatic updates using refresh task.
	 */
	@Test
	public void test800ImportShadowRefreshTask() throws Exception {
		final String TEST_NAME = "test800ImportShadowRefreshTask";
		displayTestTitle(TEST_NAME);
		// GIVEN

		// WHEN
		displayWhen(TEST_NAME);
		addTask(TASK_SHADOW_REFRESH_FILE);

		// THEN
		displayThen(TEST_NAME);

		waitForTaskStart(TASK_SHADOW_REFRESH_OID, false);
	}

	@Test
	public void test810AssignAccountWill() throws Exception {
		final String TEST_NAME = "test810AssignAccountWill";

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		modifyUserReplace(userWillOid, UserType.F_FULL_NAME, task, result, createPolyString(USER_WILL_FULL_NAME));

		// WHEN
		assignWillRoleOne(TEST_NAME, USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);

		// THEN
		restartTask(TASK_SHADOW_REFRESH_OID);
		waitForTaskFinish(TASK_SHADOW_REFRESH_OID, false);

		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);
	}

	@Test
	public void test820AssignAccountJack() throws Exception {
		final String TEST_NAME = "test820AssignAccountJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clockForward("PT5M");

		accountJackReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, getResourceOid(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		jackLastCaseOid = assertInProgress(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);

		accountJackReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertAccountJackAfterAssign(TEST_NAME);

		// THEN
		restartTask(TASK_SHADOW_REFRESH_OID);
		waitForTaskFinish(TASK_SHADOW_REFRESH_OID, false);

		assertAccountJackAfterAssign(TEST_NAME);
	}

	@Test
	public void test830CloseCaseAndWaitForRefresh() throws Exception {
		final String TEST_NAME = "test830CloseCaseAndWaitForRefresh";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		closeCase(willLastCaseOid);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		restartTask(TASK_SHADOW_REFRESH_OID);
		waitForTaskFinish(TASK_SHADOW_REFRESH_OID, false);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertWillAfterCreateCaseClosed(TEST_NAME, false);

		assertAccountJackAfterAssign(TEST_NAME);
	}

	@Test
	public void test840AddToBackingStoreAndGetAccountWill() throws Exception {
		final String TEST_NAME = "test840AddToBackingStoreAndGetAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreProvisionWill(INTEREST_ONE);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_WILL_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, true);
		assertShadowPassword(shadowModel);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_WILL_NAME);
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertNoShadowPassword(shadowRepo);
		assertShadowExists(shadowRepo, true);
	}
	
	// Direct execution. The operation is always executing immediately after it is requested.
	protected PendingOperationExecutionStatusType getExpectedExecutionStatus(PendingOperationExecutionStatusType executionStage) {
		return PendingOperationExecutionStatusType.EXECUTING;
	}
	
	@Override
	protected OperationResultStatusType getExpectedResultStatus(PendingOperationExecutionStatusType executionStage) {
		return OperationResultStatusType.IN_PROGRESS;
	}

}
