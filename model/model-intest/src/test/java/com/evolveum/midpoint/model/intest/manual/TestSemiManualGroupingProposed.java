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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.FilterUtils;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

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
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
			
		// More resources. Not really used. They are here just to confuse propagation task.
		initDummyResourcePirate(RESOURCE_DUMMY_RED_NAME,
				RESOURCE_DUMMY_RED_FILE, RESOURCE_DUMMY_RED_OID, initTask, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_BLUE_NAME,
				RESOURCE_DUMMY_BLUE_FILE, RESOURCE_DUMMY_BLUE_OID, initTask, initResult);
	}
	
	@Test
	public void test020ResourcesSanity() throws Exception {
		final String TEST_NAME = "test020ResourcesSanity";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		SearchResultList<PrismObject<ResourceType>> resources = repositoryService.searchObjects(ResourceType.class, null, null, result);
		display("Resources", resources.size() + ": " + resources);
		assertEquals("Unexpected number of resources", 3, resources.size());
		
		ObjectQuery query = QueryBuilder.queryFor(ResourceType.class, prismContext)
			.item("extension","provisioning").eq("propagated")
			.build();
		SearchResultList<PrismObject<ResourceType>> propagatedResources = repositoryService.searchObjects(ResourceType.class, query, null, result);
		display("Propagated resources", propagatedResources.size() + ": " + propagatedResources);
		assertEquals("Unexpected number of propagated resources", 1, propagatedResources.size());
	}
	
	@Override
	protected void assertNewPropagationTask() throws Exception {
		OperationResult result = new OperationResult("assertNewPropagationTask");
		PrismObject<TaskType> propTask = repositoryService.getObject(TaskType.class, getPropagationTaskOid(), null, result);
		display("Propagation task (new)", propTask);
		SearchFilterType filterType = propTask.asObjectable().getObjectRef().getFilter();
		display("Propagation task filter", filterType);
		assertFalse("Empty filter in propagation task",  FilterUtils.isFilterEmpty(filterType));
	}
	
	@Override
	protected void assertFinishedPropagationTask(Task finishedTask, OperationResultStatusType expectedStatus) {
		super.assertFinishedPropagationTask(finishedTask, expectedStatus);
		SearchFilterType filterType = finishedTask.getTaskType().getObjectRef().getFilter();
		display("Propagation task filter", filterType);
		
		assertEquals("Unexpected propagation task progress", 1, finishedTask.getProgress());
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
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
				new RawType(new PrismPropertyValue(USER_BIGMOUTH_NAME.toLowerCase()), ATTR_USERNAME_QNAME, prismContext));
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
				new RawType(new PrismPropertyValue(USER_BIGMOUTH_FULLNAME), ATTR_FULLNAME_QNAME, prismContext));
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
		assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
				new RawType(new PrismPropertyValue(USER_BIGMOUTH_NAME.toLowerCase()), ATTR_USERNAME_QNAME, prismContext));
		assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
				new RawType(new PrismPropertyValue(USER_BIGMOUTH_FULLNAME), ATTR_FULLNAME_QNAME, prismContext));
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
