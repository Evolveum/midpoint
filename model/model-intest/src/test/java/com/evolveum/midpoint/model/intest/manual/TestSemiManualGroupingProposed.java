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
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * MID-4347
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualGroupingProposed extends TestSemiManualGrouping {

	private static final String USER_BIGMOUTH_NAME = "BIGMOUTH";
	private static final String USER_BIGMOUTH_FULLNAME = "Shouty Bigmouth";
	
	private String userBigmouthOid;
	private String accountBigmouthOid;
	private String bigmouthLastCaseOid;

	@Override
	protected File getResourceFile() {
		return RESOURCE_SEMI_MANUAL_GROUPING_PROPOSED_FILE;
	}
	
	/**
	 * The resource has caseIgnore matching rule for username. Make sure everything
	 * works well wih uppercase username.
	 * MID-4393
	 */
	@Test
	public void test500AssignBigmouthRoleOne() throws Exception {
		final String TEST_NAME = "test500AssignBigmouthRoleOne";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> userBefore = createUser(USER_BIGMOUTH_NAME, USER_BIGMOUTH_FULLNAME, true);
		userBigmouthOid = addObject(userBefore);
		display("User before", userBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		assignRole(userBigmouthOid, getRoleOneOid(), task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		bigmouthLastCaseOid = assertInProgress(result);
		
		PrismObject<UserType> userAfter = getUser(userBigmouthOid);
		display("User after", userAfter);
		accountBigmouthOid = getSingleLinkOid(userAfter);
		
		PendingOperationExecutionStatusType executionStage = PendingOperationExecutionStatusType.EXECUTION_PENDING;
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountBigmouthOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, null, null, executionStage);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_BIGMOUTH_NAME.toLowerCase());
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_BIGMOUTH_FULLNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertShadowExists(shadowRepo, false);
		assertNoShadowPassword(shadowRepo);
		
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountBigmouthOid, null, task, result);
		
		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_BIGMOUTH_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_BIGMOUTH_NAME.toLowerCase());
		assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_BIGMOUTH_FULLNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, false);
		assertNoShadowPassword(shadowModel);
		PendingOperationType pendingOperationType = assertSinglePendingOperation(shadowModel, null, null, executionStage);
		
		String pendingOperationRef = pendingOperationType.getAsynchronousOperationReference();
		assertNull("Unexpected async reference in result", pendingOperationRef);
	}
	
	/**
	 * MID-4393
	 */
	@Test
	public void test502RunPropagation() throws Exception {
		final String TEST_NAME = "test502RunPropagation";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT20M");

		// WHEN
		displayWhen(TEST_NAME);
		runPropagation();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PendingOperationExecutionStatusType executionStage = PendingOperationExecutionStatusType.EXECUTING;
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountBigmouthOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, null, null, executionStage);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, USER_BIGMOUTH_NAME.toLowerCase());
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_BIGMOUTH_FULLNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
		assertShadowExists(shadowRepo, false);
		assertNoShadowPassword(shadowRepo);
		
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountBigmouthOid, null, task, result);
		
		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_BIGMOUTH_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_BIGMOUTH_NAME.toLowerCase());
		assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_BIGMOUTH_FULLNAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, false);
		assertNoShadowPassword(shadowModel);
		PendingOperationType pendingOperationType = assertSinglePendingOperation(shadowModel, null, null, executionStage);
		
		String pendingOperationRef = pendingOperationType.getAsynchronousOperationReference();
		assertNotNull("No async reference in pending operation", pendingOperationRef);
		assertCase(pendingOperationRef, SchemaConstants.CASE_STATE_OPEN);
	}
	
	// Note: we have left bigmouth here half-created with open case. If should not do any harm. 

}