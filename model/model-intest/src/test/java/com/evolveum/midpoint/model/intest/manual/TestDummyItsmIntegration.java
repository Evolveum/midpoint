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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyItsmIntegration extends AbstractConfiguredModelIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/manual/");

	public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = new QName(MidPointConstants.NS_RI, "AccountObjectClass");

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyItsmIntegration.class);

	protected static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.model.intest.manual/DummyItsmIntegrationConnector";
	
	protected static final File RESOURCE_DUMMY_ITSM_FILE = new File(TEST_DIR, "resource-dummy-itsm.xml");
	protected static final String RESOURCE_DUMMY_ITSM_OID = "4d2131d8-5f75-11e8-8b1d-e73768ded4fc";
	
	protected static final String ATTR_USERNAME = "username";
	protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

	protected static final String ATTR_FULLNAME = "fullname";
	protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

	protected static final String ATTR_INTERESTS = "interests";
	protected static final QName ATTR_INTERESTS_QNAME = new QName(MidPointConstants.NS_RI, ATTR_INTERESTS);

	protected static final String ATTR_DESCRIPTION = "description";
	protected static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);

	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;

	private String jackLastTicketIdentifier;

	private String accountJackOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
		
		importObjectFromFile(RESOURCE_DUMMY_ITSM_FILE, initResult);
		
		addObject(USER_JACK_FILE);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);

		OperationResult result = new OperationResult(TestDummyItsmIntegration.class.getName()
				+ "." + TEST_NAME);

		ResourceType repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_ITSM_OID,
				null, result).asObjectable();
		assertNotNull("No connector ref", repoResource.getConnectorRef());

		String connectorOid = repoResource.getConnectorRef().getOid();
		assertNotNull("No connector ref OID", connectorOid);
		ConnectorType repoConnector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(repoConnector);
		display("ITSM intetegration connector", repoConnector);

		// Check connector schema
		IntegrationTestTools.assertConnectorSchemaSanity(repoConnector, prismContext);
	}

	@Test
	public void test012TestConnection() throws Exception {
		final String TEST_NAME = "test012TestConnection";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_ITSM_OID, task);

		// THEN
		displayThen(TEST_NAME);
		display("Test result", testResult);
		TestUtil.assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_ITSM_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
		
		rememberSteadyResources();		
	}

	/**
	 * Tests 10x are tests for basic successful ITSM operations (no errors).
	 * Mostly just to set the baseline, because most of the cases are already
	 * (at least partially) covered by manual connector tests.
	 */
	@Test
	public void test100AssignAccountToJack() throws Exception {
		final String TEST_NAME = "test100AssignAccountToJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		displayWhen(TEST_NAME);
		assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_ITSM_OID, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		jackLastTicketIdentifier = assertInProgress(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		
		assertDummyTicket(jackLastTicketIdentifier, DummyItsmTicketStatus.OPEN, "ADD");
	}
	

	@Test
	public void test102CloseTicketAndRecomputeJack() throws Exception {
		final String TEST_NAME = "test102CloseTicketAndRecomputeJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		closeDummyTicket(jackLastTicketIdentifier);
		
		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);
		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);
	}
	
	@Test
	public void test104UnassignAccountFromJack() throws Exception {
		final String TEST_NAME = "test104UnassignAccountFromJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		displayWhen(TEST_NAME);
		unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_ITSM_OID, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		jackLastTicketIdentifier = assertInProgress(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);
		findPendingOperation(shadowRepo, PendingOperationExecutionStatusType.COMPLETED);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, PendingOperationExecutionStatusType.EXECUTING);
		assertNotNull("No ID in pending operation", pendingOperation.getId());
		
		assertDummyTicket(jackLastTicketIdentifier, DummyItsmTicketStatus.OPEN, "DEL");
	}
	
	@Test
	public void test108CloseTicketAndRecomputeJack() throws Exception {
		final String TEST_NAME = "test108CloseTicketAndRecomputeJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		closeDummyTicket(jackLastTicketIdentifier);
		
		dumpItsm();
		
		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);
		
		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 2);
		List<PendingOperationType> pendingOperations = shadowRepo.asObjectable().getPendingOperation();
		for (PendingOperationType pendingOperation : pendingOperations) {
			assertPendingOperation(shadowRepo, pendingOperation, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);
		}
		assertShadowDead(shadowRepo);
	}
	
	/**
	 * Let everything expire, so we have a clean slate for next tests.
	 */
	@Test
	public void test109LetItExpire() throws Exception {
		final String TEST_NAME = "test109LetItExpire";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT3H");
		
		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertUserAfter(USER_JACK_OID)
			.assertLinks(0);
	}
	
	/**
	 * Tests 11x are tests for operations with ITSM errors and basic error recovery.
	 */
	@Test
	public void test110AssignItsmAccountToJackCommunicationError() throws Exception {
		final String TEST_NAME = "test110AssignItsmAccountToJackCommunicationError";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		DummyItsm.getInstance().setFailureClass(CommunicationException.class);
		
		// WHEN
		displayWhen(TEST_NAME);
		assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_ITSM_OID, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		DummyItsm.getInstance().clearFailureClass();
		assertInProgress(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);
		
		assertRepoShadow(accountJackOid)
			.pendingOperations()
				.singleOperation()
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.FATAL_ERROR)
					.assertAttemptNumber(1)
					.delta()
						.assertAdd()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.RETRY);
	}
	
	@Test
	public void test111ReconcileJackFixed() throws Exception {
		final String TEST_NAME = "test111ReconcileJackFixed";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// Give consistency a time re-try operation again.
		clockForward("PT32M");
		
		DummyItsm.getInstance().clearFailureClass();
		
		PrismObject<ShadowType> shadowRepoBefore = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow before", shadowRepoBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		// This in fact should be a call to reconcile, not refresh directly (TODO: MID-4542)
		provisioningService.refreshShadow(shadowRepoBefore, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		jackLastTicketIdentifier = assertInProgress(result);
		
		assertUserAfter(USER_JACK_OID)
			.singleLink()
				.assertOid(accountJackOid);
		
		assertRepoShadow(accountJackOid)
			.pendingOperations()
				.singleOperation()
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.IN_PROGRESS)
					.assertAsynchronousOperationReference(jackLastTicketIdentifier)
					.delta()
						.assertAdd()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.MANUAL);
		
		assertDummyTicket(jackLastTicketIdentifier, DummyItsmTicketStatus.OPEN, "ADD");
	}
	
	@Test
	public void test112CloseTicketAndRecomputeJackCommunicationError() throws Exception {
		final String TEST_NAME = "test112CloseTicketAndRecomputeJackCommunicationError";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		closeDummyTicket(jackLastTicketIdentifier);
		DummyItsm.getInstance().setFailureClass(CommunicationException.class);
		
		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		DummyItsm.getInstance().clearFailureClass();
		assertPartialError(result);
		
		assertUserAfter(USER_JACK_OID)
			.singleLink()
				.assertOid(accountJackOid);

		// Communication error, cannot update status. Still old data here.
		assertRepoShadow(accountJackOid)
			.pendingOperations()
				.singleOperation()
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.IN_PROGRESS)
					.assertAsynchronousOperationReference(jackLastTicketIdentifier)
					.delta()
						.assertAdd()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.MANUAL);
	}
	
	@Test
	public void test113RecomputeJackFixed() throws Exception {
		final String TEST_NAME = "test113RecomputeJackFixed";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		DummyItsm.getInstance().clearFailureClass();
		
		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertUserAfter(USER_JACK_OID)
			.singleLink()
				.assertOid(accountJackOid);
	
		assertRepoShadow(accountJackOid)
			.pendingOperations()
				.singleOperation()
					.assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
					.assertResultStatus(OperationResultStatusType.SUCCESS)
					.assertAsynchronousOperationReference(jackLastTicketIdentifier)
					.delta()
						.assertAdd()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.MANUAL);
	}
	
	@Test
	public void test114UnassignAccountFromJackCommunicationError() throws Exception {
		final String TEST_NAME = "test114UnassignAccountFromJackCommunicationError";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		DummyItsm.getInstance().setFailureClass(CommunicationException.class);
		
		// WHEN
		displayWhen(TEST_NAME);
		unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_ITSM_OID, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		DummyItsm.getInstance().clearFailureClass();
		assertResultStatus(result, OperationResultStatus.IN_PROGRESS);
		
		assertUserAfter(USER_JACK_OID)
			.singleLink()
				.assertOid(accountJackOid);
	
		assertRepoShadow(accountJackOid)
			.pendingOperations()
				.by()
					.executionStatus(PendingOperationExecutionStatusType.COMPLETED)
				.find()
					.assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
					.assertResultStatus(OperationResultStatusType.SUCCESS)
					.assertAsynchronousOperationReference(jackLastTicketIdentifier)
					.delta()
						.assertAdd()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.MANUAL)
					.end()
				.by()
					.executionStatus(PendingOperationExecutionStatusType.EXECUTING)
				.find()
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.FATAL_ERROR)
					.assertAttemptNumber(1)
					.delta()
						.assertDelete()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.RETRY)
					.end();
		
	}
	
	@Test
	public void test115ReconcileJackFixed() throws Exception {
		final String TEST_NAME = "test115ReconcileJackFixed";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// Give consistency a time re-try operation again.
		clockForward("PT32M");
		
		DummyItsm.getInstance().clearFailureClass();
		PrismObject<ShadowType> shadowRepoBefore = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow before", shadowRepoBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		// This in fact should be a call to reconcile, not refresh directly (TODO: MID-4542)
		provisioningService.refreshShadow(shadowRepoBefore, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		jackLastTicketIdentifier = assertInProgress(result);
		
		assertUserAfter(USER_JACK_OID)
			.singleLink()
				.assertOid(accountJackOid);
	
		// First operation has expired already
		assertRepoShadow(accountJackOid)
			.pendingOperations()
				.singleOperation()
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.IN_PROGRESS)
					.delta()
						.assertDelete()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.MANUAL)
					.end();
		
		assertDummyTicket(jackLastTicketIdentifier, DummyItsmTicketStatus.OPEN, "DEL");
	}
	
	@Test
	public void test117CloseTicketAndRecomputeJackCommunicationError() throws Exception {
		final String TEST_NAME = "test117CloseTicketAndRecomputeJackCommunicationError";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		closeDummyTicket(jackLastTicketIdentifier);
		DummyItsm.getInstance().setFailureClass(CommunicationException.class);
		
		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		DummyItsm.getInstance().clearFailureClass();
		assertPartialError(result);
		
		assertUserAfter(USER_JACK_OID)
			.singleLink()
				.assertOid(accountJackOid);
	
		// Communication error. Still old data.
		assertRepoShadow(accountJackOid)
			.pendingOperations()
				.singleOperation()
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.IN_PROGRESS)
					.delta()
						.assertDelete()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.MANUAL)
					.end();
	}
	
	@Test
	public void test118RecomputeJackFixed() throws Exception {
		final String TEST_NAME = "test118RecomputeJackFixed";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		DummyItsm.getInstance().clearFailureClass();
		
		PrismObject<ShadowType> shadowRepoBefore = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow before", shadowRepoBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		// This in fact should be a call to reconcile, not refresh directly (TODO: MID-4542)
		provisioningService.refreshShadow(shadowRepoBefore, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertUserAfter(USER_JACK_OID)
			.singleLink()
				.assertOid(accountJackOid);
		
		assertRepoShadow(accountJackOid)
			.assertTombstone()
			.pendingOperations()
				.singleOperation()
					.assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
					.assertResultStatus(OperationResultStatusType.SUCCESS)
					.delta()
						.assertDelete()
						.end()
					.assertId()
					.assertType(PendingOperationTypeType.MANUAL);
	}
	
	/**
	 * Let everything expire, so we have a clean slate for next tests.
	 */
	@Test
	public void test119LetItExpire() throws Exception {
		final String TEST_NAME = "test119LetItExpire";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT3H");
		
		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertUserAfter(USER_JACK_OID)
			.assertLinks(0);
	}
	
	private void assertDummyTicket(String identifier, DummyItsmTicketStatus expectedStatus, String expectedBodyStart) throws Exception {
		DummyItsm itsm = DummyItsm.getInstance();
		DummyItsmTicket ticket = itsm.findTicket(identifier);
		assertNotNull("No ticket in ITSM: " + identifier, ticket);
		assertEquals("Unexpected ITSM ticket "+identifier+" status", expectedStatus,  ticket.getStatus());
		assertTrue("Expected that ticket body will start with '"+expectedBodyStart+"'. But it did not: "+ticket.getBody(), ticket.getBody().startsWith(expectedBodyStart));
	}
	
	private void closeDummyTicket(String identifier) throws Exception {
		DummyItsm itsm = DummyItsm.getInstance();
		DummyItsmTicket ticket = itsm.findTicket(identifier);
		assertNotNull("No ticket in ITSM: " + identifier, ticket);
		assertEquals("Attempt to close ITSM ticket "+identifier+" which is not open", DummyItsmTicketStatus.OPEN, ticket.getStatus());
		ticket.setStatus(DummyItsmTicketStatus.CLOSED);
	}
	
	private void dumpItsm() {
		display("ITSM", DummyItsm.getInstance());
	}

}
