/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.wf.impl.manual;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.AbstractWfTest;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AbstractWriteCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.*;

/**
 * This is an adaptation of model-intest manual resource test(s) aimed to verify workflow-related aspects
 * (e.g. completion, auditing, notifications) of manual provisioning cases.
 *
 * @author Radovan Semancik
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class ManualResourceTest extends AbstractWfTest {

	protected static final File TEST_DIR = new File("src/test/resources/manual/");

	private static final Trace LOGGER = TraceManager.getTrace(ManualResourceTest.class);

	private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

	public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = new QName(MidPointConstants.NS_RI, "AccountObjectClass");

	protected static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
	protected static final String RESOURCE_MANUAL_OID = "ffb52158-63dc-4f5b-b998-fb6bfcfc546e";

	protected static final File ROLE_ONE_MANUAL_FILE = new File(TEST_DIR, "role-one-manual.xml");
	protected static final String ROLE_ONE_MANUAL_OID = "0379f623-a3e9-405a-ba52-b1f7ecd2d46f";

	protected static final String USER_WILL_NAME = "will";
	protected static final String USER_WILL_GIVEN_NAME = "Will";
	protected static final String USER_WILL_FAMILY_NAME = "Turner";
	protected static final String USER_WILL_FULL_NAME = "Will Turner";
	protected static final String USER_WILL_FULL_NAME_PIRATE = "Pirate Will Turner";
	protected static final String ACCOUNT_WILL_DESCRIPTION_MANUAL = "manual";
	protected static final String USER_WILL_PASSWORD_OLD = "3lizab3th";
	protected static final String USER_WILL_PASSWORD_NEW = "ELIZAbeth";

	protected static final String ACCOUNT_JACK_DESCRIPTION_MANUAL = "Manuel";
	protected static final String USER_JACK_PASSWORD_OLD = "deadM3NtellN0tales";

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

	protected String userWillOid;
	protected String accountWillOid;

	protected XMLGregorianCalendar accountWillRequestTimestampStart;
	protected XMLGregorianCalendar accountWillRequestTimestampEnd;

	protected XMLGregorianCalendar accountWillCompletionTimestampStart;
	protected XMLGregorianCalendar accountWillCompletionTimestampEnd;

	protected XMLGregorianCalendar accountWillSecondReqestTimestampStart;
	protected XMLGregorianCalendar accountWillSecondReqestTimestampEnd;

	protected XMLGregorianCalendar accountWillSecondCompletionTimestampStart;
	protected XMLGregorianCalendar accountWillSecondCompletionTimestampEnd;

	protected String willLastCaseOid;
	protected String willSecondLastCaseOid;

	protected String accountJackOid;
	protected String accountDrakeOid;

	protected XMLGregorianCalendar accountJackReqestTimestampStart;
	protected XMLGregorianCalendar accountJackReqestTimestampEnd;

	protected XMLGregorianCalendar accountJackCompletionTimestampStart;
	protected XMLGregorianCalendar accountJackCompletionTimestampEnd;

	protected String jackLastCaseOid;

	private String lastResourceVersion;

	protected String phoenixLastCaseOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		importObjectFromFile(RESOURCE_MANUAL_FILE, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		PrismObject<UserType> userWill = createUserWill();
		addObject(userWill, initTask, initResult);
		display("User will", userWill);
		userWillOid = userWill.getOid();

		importObjectFromFile(ROLE_ONE_MANUAL_FILE, initResult);

		// Turns on checks for connection in manual connector
		InternalsConfig.setSanityChecks(true);
	}

	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	private PrismObject<UserType> createUserWill() throws SchemaException {
		PrismObject<UserType> user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate();
		user.asObjectable()
			.name(USER_WILL_NAME)
			.givenName(USER_WILL_GIVEN_NAME)
			.familyName(USER_WILL_FAMILY_NAME)
			.fullName(USER_WILL_FULL_NAME)
			.beginActivation().administrativeStatus(ActivationStatusType.ENABLED).<UserType>end()
			.beginCredentials().beginPassword().beginValue().setClearValue(USER_WILL_PASSWORD_OLD);
		return user;
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);

		OperationResult result = new OperationResult(ManualResourceTest.class.getName() + "." + TEST_NAME);

		ResourceType repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_MANUAL_OID,
				null, result).asObjectable();
		assertNotNull("No connector ref", repoResource.getConnectorRef());

		String connectorOid = repoResource.getConnectorRef().getOid();
		assertNotNull("No connector ref OID", connectorOid);
		ConnectorType repoConnector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(repoConnector);
		display("Manual Connector", repoConnector);

		// Check connector schema
		IntegrationTestTools.assertConnectorSchemaSanity(repoConnector, prismContext);

		PrismObject<UserType> userWill = getUser(userWillOid);
		assertUser(userWill, userWillOid, USER_WILL_NAME, USER_WILL_FULL_NAME, USER_WILL_GIVEN_NAME, USER_WILL_FAMILY_NAME);
		assertAdministrativeStatus(userWill, ActivationStatusType.ENABLED);
		assertUserPassword(userWill, USER_WILL_PASSWORD_OLD);
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
		OperationResult testResult = modelService.testResource(RESOURCE_MANUAL_OID, task);

		// THEN
		displayThen(TEST_NAME);
		display("Test result", testResult);
		TestUtil.assertSuccess("Test resource failed (result)", testResult);
	}
	
	@Test
	public void test100AssignWillRoleOne() throws Exception {
		final String TEST_NAME = "test100AssignWillRoleOne";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		dummyAuditService.clear();
		dummyTransport.clearMessages();

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(userWillOid, ROLE_ONE_MANUAL_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		willLastCaseOid = assertInProgress(result);

		PrismObject<UserType> userAfter = getUser(userWillOid);
		display("User after", userAfter);
		accountWillOid = getSingleLinkOid(userAfter);

		assertAccountWillAfterAssign(TEST_NAME, USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);

		display("dummy audit", dummyAuditService);
		display("dummy transport", dummyTransport);
	}

	/**
	 * Case is closed. The operation is complete.
	 */
	@Test
	public void test110CloseCaseAndRecomputeWill() throws Exception {
		final String TEST_NAME = "test110CloseCaseAndRecomputeWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		dummyAuditService.clear();
		dummyTransport.clearMessages();

		// WHEN
		displayWhen(TEST_NAME);

		CaseType caseBefore = getCase(willLastCaseOid);
		display("Case before work item completion", caseBefore);

		List<CaseWorkItemType> workItems = caseBefore.getWorkItem();
		assertEquals("Wrong # of work items", 2, workItems.size());
		assertEquals("Wrong assignees", new HashSet<>(Arrays.asList(USER_ADMINISTRATOR_OID, userJackOid)), workItems.stream()
				.map(wi -> wi.getOriginalAssigneeRef().getOid())
				.collect(Collectors.toSet()));

		Optional<CaseWorkItemType> adminWorkItem = workItems.stream()
				.filter(wi -> USER_ADMINISTRATOR_OID.equals(wi.getOriginalAssigneeRef().getOid())).findAny();

		assertTrue("no admin work item", adminWorkItem.isPresent());

		workflowManager.completeWorkItem(WorkItemId.of(adminWorkItem.get()),
				new AbstractWorkItemOutputType(prismContext)
						.outcome(OperationResultStatusType.SUCCESS.value()),
				null, task, result);

		accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// We need reconcile and not recompute here. We need to fetch the updated case status.
		reconcileUser(userWillOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		CaseType caseAfter = getCase(willLastCaseOid);
		display("Case after work item completion", caseAfter);

		accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		assertWillAfterCreateCaseClosed(TEST_NAME);

		display("dummy audit", dummyAuditService);
		display("dummy transport", dummyTransport);
	}

	protected void assertAccountWillAfterAssign(final String TEST_NAME, String expectedFullName,
			PendingOperationExecutionStatusType propagationExecutionStage) throws Exception {
		ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
			.assertConception()
			.pendingOperations()
				.singleOperation()
					.assertId()
					.assertRequestTimestamp(accountWillRequestTimestampStart, accountWillRequestTimestampEnd)
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.delta()
						.display()
						.end()
					.end()
				.end()
			.attributes()
				.assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
				.end()
			.assertNoPassword();
		assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, expectedFullName);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter.getObject(), ActivationStatusType.ENABLED);

		ShadowAsserter<Void> shadowModelAsserter =  assertModelShadow(accountWillOid)
			.assertName(USER_WILL_NAME)
			.assertKind(ShadowKindType.ACCOUNT)
			.assertConception()
			.attributes()
				.assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
				.end()
			.assertNoPassword()
			.pendingOperations()
				.singleOperation()
					.assertRequestTimestamp(accountWillRequestTimestampStart, accountWillRequestTimestampEnd)
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.end()
				.end();
		assertAttributeFromCache(shadowModelAsserter, ATTR_FULLNAME_QNAME, expectedFullName);
		assertShadowActivationAdministrativeStatusFromCache(shadowModelAsserter.getObject(), ActivationStatusType.ENABLED);

		assertWillCase(SchemaConstants.CASE_STATE_OPEN, 
				shadowModelAsserter.pendingOperations().singleOperation().getOperation(),
				propagationExecutionStage);
	}
	
	protected void assertWillCase(String expectedCaseState, PendingOperationType pendingOperation, 
			PendingOperationExecutionStatusType propagationExecutionStage) throws ObjectNotFoundException, SchemaException {
		String pendingOperationRef = pendingOperation.getAsynchronousOperationReference();
		// Case number should be in willLastCaseOid. It will get there from operation result.
		assertNotNull("No async reference in pending operation", willLastCaseOid);
		assertCase(willLastCaseOid, expectedCaseState);
		assertEquals("Wrong case ID in pending operation", willLastCaseOid, pendingOperationRef);
	}
	
	protected void assertWillAfterCreateCaseClosed(final String TEST_NAME) throws Exception {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
			.pendingOperations()
				.singleOperation()
					.assertRequestTimestamp(accountWillRequestTimestampStart, accountWillRequestTimestampEnd)
					.assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
					.assertResultStatus(OperationResultStatusType.SUCCESS)
					.assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
					.end()
				.end()
			.attributes()
				.assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
				.end();
		assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
		assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter, ActivationStatusType.ENABLED);

		ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
			.assertName(USER_WILL_NAME)
			.assertKind(ShadowKindType.ACCOUNT)
			.attributes()
				.assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
				.end();
		shadowRepoAsserter
			.assertLife();

		shadowModelAsserter
			.assertLife()
			.assertAdministrativeStatus(ActivationStatusType.ENABLED)
			.attributes()
				.assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME)
				.end()
			.pendingOperations()
				.singleOperation()
					.assertRequestTimestamp(accountWillRequestTimestampStart, accountWillRequestTimestampEnd)
					.assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
					.assertResultStatus(OperationResultStatusType.SUCCESS)
					.assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
					.end()
				.end();
		assertAttributeFromBackingStore(shadowModelAsserter, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModelAsserter);

		assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	protected void assertWillUnassignedFuture(ShadowAsserter<?> shadowModelAsserterFuture, boolean assertPassword) {
		shadowModelAsserterFuture
			.assertName(USER_WILL_NAME);
		assertUnassignedFuture(shadowModelAsserterFuture, assertPassword);
	}

	protected void assertUnassignedFuture(ShadowAsserter<?> shadowModelAsserterFuture, boolean assertPassword) {
		shadowModelAsserterFuture
			.assertDead();
		if (assertPassword) {
			assertShadowPassword(shadowModelAsserterFuture);
		}
	}


	protected <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrName, T... expectedValues) {
		assertAttribute(resource, shadow.asObjectable(), attrName, expectedValues);
	}

	protected <T> void assertNoAttribute(PrismObject<ShadowType> shadow, QName attrName) {
		assertNoAttribute(resource, shadow.asObjectable(), attrName);
	}

	protected void assertAttributeFromCache(ShadowAsserter<?> shadowAsserter, QName attrQName,
			Object... attrVals) {
		assertAttributeFromCache(shadowAsserter.getObject(), attrQName, attrVals);
	}
	
	protected void assertAttributeFromCache(PrismObject<ShadowType> shadow, QName attrQName,
			Object... attrVals) {
		assertAttribute(shadow, attrQName, attrVals);
	}

	protected void assertAttributeFromBackingStore(ShadowAsserter<?> shadowAsserter, QName attrQName,
			String... attrVals) {
		assertAttributeFromBackingStore(shadowAsserter.getObject(), attrQName, attrVals);
	}
	
	protected void assertAttributeFromBackingStore(PrismObject<ShadowType> shadow, QName attrQName,
			String... attrVals) {
		assertNoAttribute(shadow, attrQName);
	}
	
	protected void assertShadowActivationAdministrativeStatusFromCache(ShadowAsserter<?> shadowAsserter, ActivationStatusType expectedStatus) {
		assertShadowActivationAdministrativeStatusFromCache(shadowAsserter.getObject(), expectedStatus);
	}

	protected void assertShadowActivationAdministrativeStatusFromCache(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
		assertShadowActivationAdministrativeStatus(shadow, expectedStatus);
	}

	protected void assertShadowActivationAdministrativeStatus(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
		assertActivationAdministrativeStatus(shadow, expectedStatus);
	}

	protected void assertShadowPassword(ShadowAsserter<?> shadowAsserter) {
		assertShadowPassword(shadowAsserter.getObject());
	}
	
	protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
		// pure manual resource should never "read" password
		assertNoShadowPassword(shadow);
	}

	private void assertManual(AbstractWriteCapabilityType cap) {
		assertEquals("Manual flag not set in capability "+cap, Boolean.TRUE, cap.isManual());
	}
	protected void assertCase(String oid, String expectedState, PendingOperationExecutionStatusType executionStage) throws ObjectNotFoundException, SchemaException {
		assertCase(oid, expectedState);
	}

	protected void assertHasModification(ObjectDeltaType deltaType, ItemPath itemPath) {
		for (ItemDeltaType itemDelta: deltaType.getItemDelta()) {
			if (itemPath.equivalent(itemDelta.getPath().getItemPath())) {
				return;
			}
		}
		fail("No modification for "+itemPath+" in delta");
	}

	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd, PendingOperationExecutionStatusType executionStage) {
		assertPendingOperationDeltas(shadow, 1);
		return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0), 
				requestStart, requestEnd,
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
				null, null);
	}

}
