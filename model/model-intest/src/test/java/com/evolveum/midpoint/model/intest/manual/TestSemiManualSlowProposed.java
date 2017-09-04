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

/**
 *
 */
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Tests a slow semi manual resource with the use of proposed shadows.
 * The resource is "slow" in a way that it takes approx. a second to process a ticket.
 * This may cause all sorts of race conditions.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualSlowProposed extends TestSemiManual {

	private static final Trace LOGGER = TraceManager.getTrace(TestSemiManualSlowProposed.class);

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryCache repositoryCache;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		initManualConnector();

		repositoryCache.setModifyRandomDelayRange(150);
	}

	@Override
	protected String getResourceOid() {
		return RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_OID;
	}

	@Override
	protected File getResourceFile() {
		return RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_FILE;
	}

	@Override
	protected String getRoleOneOid() {
		return ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_OID;
	}

	@Override
	protected File getRoleOneFile() {
		return ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_FILE;
	}

	@Override
	protected String getRoleTwoOid() {
		return ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_OID;
	}

	@Override
	protected File getRoleTwoFile() {
		return ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_FILE;
	}

	// Make the test fast ...
	@Override
	protected int getConcurrentTestRandomStartDelayRange() {
		return 300;
	}

	protected int getConcurrentTestRandomStartDelayRangeDelete() {
		return 3;
	}

	// ... and intense ...
	@Override
	protected int getConcurrentTestNumberOfThreads() {
		return 10;
	}

	// TODO: .. and make the resource slow.
	private void initManualConnector() {
		ManualConnectorInstance.setRandomDelayRange(1000);
	}

	/**
	 * Set up roles used in parallel tests.
	 */
	@Test
	public void test900SetUpRoles() throws Exception {
		final String TEST_NAME = "test900SetUpRoles";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		SystemConfigurationType systemConfiguration = getSystemConfiguration();
		display("System config", systemConfiguration);
		assertEquals("Wrong conflict resolution", ConflictResolutionActionType.RECOMPUTE, systemConfiguration.getDefaultObjectPolicyConfiguration().get(0).getConflictResolution().getAction());

		for (int i = 0; i < getConcurrentTestNumberOfThreads(); i++) {
			PrismObject<RoleType> role = parseObject(getRoleOneFile());
			role.setOid(getRoleOid(i));
			role.asObjectable().setName(createPolyStringType(getRoleName(i)));
			List<ResourceAttributeDefinitionType> outboundAttributes = role.asObjectable().getInducement().get(0).getConstruction().getAttribute();
			if (hasMultivalueInterests()) {
				ExpressionType outboundExpression = outboundAttributes.get(0).getOutbound().getExpression();
				JAXBElement jaxbElement = outboundExpression.getExpressionEvaluator().get(0);
				jaxbElement.setValue(getRoleInterest(i));
			} else {
				outboundAttributes.remove(0);
			}
			addObject(role);
		}
	}

	private String getRoleOid(int i) {
		return String.format("f363260a-8d7a-11e7-bd67-%012d", i);
	}

	private String getRoleName(int i) {
		return String.format("role-%012d", i);
	}

	private String getRoleInterest(int i) {
		return String.format("i%012d", i);
	}

	// MID-4047, MID-4112
	@Test
	public void test910ConcurrentRolesAssign() throws Exception {
		final String TEST_NAME = "test910ConcurrentRolesAssign";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		int numberOfCasesBefore = getObjectCount(CaseType.class);
		PrismObject<UserType> userBefore = getUser(USER_BARBOSSA_OID);
		display("user before", userBefore);

		final long TIMEOUT = 60000L;

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					login(userAdministrator);
					Task localTask = createTask(TEST_NAME + ".local");

					assignRole(USER_BARBOSSA_OID, getRoleOid(i), localTask, localTask.getResult());

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestRandomStartDelayRange());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, TIMEOUT);

		PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("user after", userAfter);
		assertAssignments(userAfter, getConcurrentTestNumberOfThreads());
		assertEquals("Wrong # of links", 1, userAfter.asObjectable().getLinkRef().size());
		accountBarbossaOid = userAfter.asObjectable().getLinkRef().get(0).getOid();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountBarbossaOid, null, result);
		display("Repo shadow", shadowRepo);
		assertShadowNotDead(shadowRepo);

		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountBarbossaOid, options, task, result);
		display("Shadow after (model, future)", shadowModel);

//		assertObjects(CaseType.class, numberOfCasesBefore + getConcurrentTestNumberOfThreads());
	}

	// MID-4112
	@Test
	public void test919ConcurrentRoleUnassign() throws Exception {
		final String TEST_NAME = "test919ConcurrentRoleUnassign";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		int numberOfCasesBefore = getObjectCount(CaseType.class);
		PrismObject<UserType> userBefore = getUser(USER_BARBOSSA_OID);
		display("user before", userBefore);
		assertAssignments(userBefore, getConcurrentTestNumberOfThreads());

		final long TIMEOUT = 60000L;

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					display("Thread "+Thread.currentThread().getName()+" START");
					login(userAdministrator);
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();

					unassignRole(USER_BARBOSSA_OID, getRoleOid(i), localTask, localResult);

					localResult.computeStatus();

					display("Thread "+Thread.currentThread().getName()+" DONE, result", localResult);

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestRandomStartDelayRangeDelete());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, TIMEOUT);

		PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("user after", userAfter);
		assertAssignments(userAfter, 0);
		assertEquals("Wrong # of links", 1, userAfter.asObjectable().getLinkRef().size());

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountBarbossaOid, null, result);
		display("Repo shadow", shadowRepo);

		ObjectDeltaType deletePendingDelta = null;
		for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (delta.getChangeType() == ChangeTypeType.DELETE) {
				if (deletePendingDelta != null) {
					fail("More than one delete pending delta found:\n"+deletePendingDelta+"\n"+delta);
				}
				deletePendingDelta = delta;
			}
		}
		assertNotNull("No delete pending delta", deletePendingDelta);

		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class, accountBarbossaOid, options, task, result);
		display("Shadow after (model, future)", shadowModelFuture);
		assertShadowDead(shadowModelFuture);

//		assertObjects(CaseType.class, numberOfCasesBefore + getConcurrentTestNumberOfThreads() + 1);
	}
}