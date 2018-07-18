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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.asserter.PendingOperationsAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyFailureAndRetry extends AbstractDummyTest {
	
	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "retry");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-retry.xml");

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyFailureAndRetry.class);
	
	private XMLGregorianCalendar lastRequestStartTs;
	private XMLGregorianCalendar lastRequestEndTs;
	private XMLGregorianCalendar lastAttemptStartTs;
	private XMLGregorianCalendar lastAttemptEndTs;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
//		DebugUtil.setDetailedDebugDump(true);
//		InternalMonitor.setTraceConnectorOperation(true);
	}

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}
	
	@Test
	public void test000Integrity() throws Exception {
		final String TEST_NAME = "test000Integrity";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		display("Dummy resource instance", dummyResource.toString());

		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		assertSuccess(result);
		
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, task);
		assertSuccess(testResult);
	}
	
	/**
	 * Mostly just a sanity test. Make sure that normal (non-failure) operation works.
	 * Also prepares some state for later tests.
	 */
	@Test
	public void test050AddAccountWill() throws Exception {
		final String TEST_NAME = "test050AddAccountWill";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(getAccountWillFile());
		account.checkConsistence();
		display("Adding shadow", account);

		// WHEN
		displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);

		display("Account provisioning", accountProvisioning);
		ShadowAsserter.forShadow(accountProvisioning)
			.assertNoLegacyConsistency()
			.pendingOperations()
				.none();

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(transformNameFromResource(ACCOUNT_WILL_USERNAME));
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Username is wrong", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyAccount.getName());
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, dummyAccount.getPassword());

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo);

		checkRepoAccountShadow(shadowFromRepo);
		ShadowAsserter.forShadow(shadowFromRepo)
			.assertNoLegacyConsistency()
			.pendingOperations()
				.none();

		checkUniqueness(accountProvisioning);
		rememberSteadyResources();
	}
	
	@Test
	public void test100AddAccountMorganCommunicationFailure() throws Exception {
		final String TEST_NAME = "test100AddAccountMorganCommunicationFailure";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		dummyResource.setBreakMode(BreakMode.NETWORK);

		PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_MORGAN_FILE);
		account.checkConsistence();
		display("Adding shadow", account);
		
		lastRequestStartTs = lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("Result", result);
		assertInProgress(result);
		assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);
		account.checkConsistence();
		lastRequestEndTs = lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
		syncServiceMock.assertNotifyInProgressOnly();
		
		assertUncreatedMorgan(1);
		
		// Resource -> down
		assertResourceStatusChangeCounterIncrements();
		assertSteadyResources();
	}
	
	

	/**
	 * Test add with pending operation and recovered resource. This happens when re-try task
	 * does not have a chance to run yet.
	 * Nothing significant should happen.
	 */
	@Test
	public void test102GetAccountMorganRecovery() throws Exception {
		final String TEST_NAME = "test102GetAccountMorganRecovery";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		dummyResource.resetBreakMode();

		// WHEN
		displayWhen(TEST_NAME);
		assertGetUncreatedShadow(ACCOUNT_MORGAN_OID);

		// THEN
		displayThen(TEST_NAME);
		syncServiceMock.assertNoNotifcations();
		
		assertUncreatedMorgan(1);
		
		assertSteadyResources();
	}
	
	/**
	 * Refresh while the resource is down. Retry interval is not yet reached.
	 * Nothing should really happen yet.
	 */
	@Test
	public void test104RefreshAccountMorganCommunicationFailure() throws Exception {
		final String TEST_NAME = "test104RefreshAccountMorganCommunicationFailure";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		dummyResource.setBreakMode(BreakMode.NETWORK);

		PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("Result", result);
		assertSuccess(result);
		syncServiceMock.assertNoNotifcations();
		
		assertUncreatedMorgan(1);
		
		assertSteadyResources();
	}
	
	/**
	 * Wait for retry interval to pass. Now provisioning should retry add operation.
	 * But no luck yet. Resource is still down.
	 */
	@Test
	public void test106RefreshAccountMorganCommunicationFailureRetry() throws Exception {
		final String TEST_NAME = "test106RefreshAccountMorganCommunicationFailureRetry";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT17M");
		
		syncServiceMock.reset();
		
		dummyResource.setBreakMode(BreakMode.NETWORK);

		PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);
		
		lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("Result", result);
		result.computeStatus();
		TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
		lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
		syncServiceMock.assertNotifyInProgressOnly();
		
		assertUncreatedMorgan(2);
		
		assertSteadyResources();
	}
	
	/**
	 * Wait for yet another retry interval to pass. Now provisioning should retry add operation
	 * again. Still no luck. As this is the third and last attempt the operation should now be
	 * completed, fatal error recorded and the shadow should be dead.
	 */
	@Test
	public void test108RefreshAccountMorganCommunicationFailureRetryAgain() throws Exception {
		final String TEST_NAME = "test108RefreshAccountMorganCommunicationFailureRetryAgain";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		clockForward("PT17M");
		
		syncServiceMock.reset();
		
		dummyResource.setBreakMode(BreakMode.NETWORK);

		PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);
		
		lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("Result", result);
		result.computeStatus();
		TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
		lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
		syncServiceMock.assertNotifyFailureOnly();
		
		PrismObject<ShadowType> repoShadow = getShadowRepo(ACCOUNT_MORGAN_OID);
		assertNotNull("Shadow was not created in the repository", repoShadow);
		
		ShadowAsserter shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
		shadowAsserter
			.display()
			.pendingOperations()
				.singleOperation()
					.display()
					.assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
					.assertResultStatus(OperationResultStatusType.FATAL_ERROR)
					.assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertAttemptNumber(3)
					.assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
					.delta()
						.display()
						.assertAdd();
		shadowAsserter
			.assertDead()
			.assertIsNotExists()
			.assertNoLegacyConsistency()
			.attributes()
				.assertAttributes(SchemaConstants.ICFS_NAME);
		
		assertPostponedOperationRepoShadow(repoShadow);
		
		PrismObject<ShadowType> shadowNoFetch = getShadowNoFetch(ACCOUNT_MORGAN_OID);
		shadowAsserter = ShadowAsserter.forShadow(shadowNoFetch, "noFetch");
		shadowAsserter
			.display()
			.pendingOperations()
				.singleOperation()
					.assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
					.assertResultStatus(OperationResultStatusType.FATAL_ERROR)
					.assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertAttemptNumber(3)
					.assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
					.delta()
						.assertAdd();
		shadowAsserter
			.assertDead()
			.assertIsNotExists()
			.assertNoLegacyConsistency()
			.attributes()
				.assertResourceAttributeContainer()
				.assertNoPrimaryIdentifier()
				.assertHasSecondaryIdentifier()
				.assertSize(1);
		
		PrismObject<ShadowType> accountProvisioningFuture = getShadowFuture(ACCOUNT_MORGAN_OID);
		shadowAsserter = ShadowAsserter.forShadow(accountProvisioningFuture,"future");
		shadowAsserter
			.display()
			.assertDead()
			.assertIsNotExists()
			.assertNoLegacyConsistency()
			.attributes()
				.assertResourceAttributeContainer()
				.assertNoPrimaryIdentifier()
				.assertHasSecondaryIdentifier();
		
		dummyResource.resetBreakMode();
		
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(transformNameFromResource(ACCOUNT_WILL_USERNAME));
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Username is wrong", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyAccount.getName());
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, dummyAccount.getPassword());
		
		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		
		checkUniqueness(accountProvisioningFuture);
		
		assertSteadyResources();
	}
	
	private void assertUncreatedMorgan(int expectedAttemptNumber) throws Exception {
		
		PrismObject<ShadowType> repoShadow = getShadowRepo(ACCOUNT_MORGAN_OID);
		assertNotNull("Shadow was not created in the repository", repoShadow);
		
		ShadowAsserter shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
		shadowAsserter
			.display()
			.pendingOperations()
				.singleOperation()
					.display()
					.assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.FATAL_ERROR)
					.assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertAttemptNumber(expectedAttemptNumber)
					.assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
					.delta()
						.display()
						.assertAdd();
		shadowAsserter
			.assertIsNotExists()
			.assertNotDead()
			.assertNoLegacyConsistency()
			.attributes()
				.assertAttributes(SchemaConstants.ICFS_NAME);
		
		assertPostponedOperationRepoShadow(repoShadow);
		
		PrismObject<ShadowType> shadowNoFetch = getShadowNoFetch(ACCOUNT_MORGAN_OID);
		shadowAsserter = ShadowAsserter.forShadow(shadowNoFetch, "noFetch");
		shadowAsserter
			.display()
			.pendingOperations()
				.singleOperation()
					.assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
					.assertResultStatus(OperationResultStatusType.FATAL_ERROR)
					.assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
					.assertAttemptNumber(expectedAttemptNumber)
					.assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
					.delta()
						.assertAdd();
		shadowAsserter
			.assertIsNotExists()
			.assertNotDead()
			.assertNoLegacyConsistency()
			.attributes()
				.assertResourceAttributeContainer()
				.assertNoPrimaryIdentifier()
				.assertHasSecondaryIdentifier()
				.assertSize(1);
		
		PrismObject<ShadowType> accountProvisioningFuture = getShadowFuture(ACCOUNT_MORGAN_OID);
		shadowAsserter = ShadowAsserter.forShadow(accountProvisioningFuture,"future");
		shadowAsserter
			.display()
			.assertIsExists()
			.assertNotDead()
			.assertNoLegacyConsistency()
			.attributes()
				.assertResourceAttributeContainer()
				.assertNoPrimaryIdentifier()
				.assertHasSecondaryIdentifier()
				.assertSize(4)
				.assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_MORGAN_FULLNAME);
		
		dummyResource.resetBreakMode();

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(transformNameFromResource(ACCOUNT_WILL_USERNAME));
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Username is wrong", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyAccount.getName());
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, dummyAccount.getPassword());

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)

		checkUniqueness(accountProvisioningFuture);
	}
	
	
	
	private void assertResourceStatusChangeCounterIncrements() {
		assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 1);
		assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
	}
	

	private void assertPostponedOperationRepoShadow(PrismObject<ShadowType> repoShadow) {
		ProvisioningTestUtil.checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT, 1);
		assertFalse("No postponed operation in "+repoShadow, repoShadow.asObjectable().getPendingOperation().isEmpty());
	}

	private void assertGetUncreatedShadow(String oid) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask("assertGetUncreatedShadow");
		OperationResult result = task.getResult();
		try {
			PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
				oid, null, task, result);
			fail("Expected that get of uncreated shadow fails, but it was successful: "+shadow);
		} catch (GenericConnectorException e) {
			// Expected
		}
	}
	
	private PrismObject<ShadowType> getShadowNoFetch(String oid) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask("getShadowNoFetch");
		OperationResult result = task.getResult();
		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
				oid, options, task, result);
		assertSuccess(result);
		return shadow;
	}
	
	private PrismObject<ShadowType> getShadowFuture(String oid) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask("getShadowFuture");
		OperationResult result = task.getResult();
		Collection<SelectorOptions<GetOperationOptions>> options =  SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
				oid, options, task, result);
		assertSuccess(result);
		return shadow;
	}

	// TODO: shadow with legacy postponed operation: shoudl be cleaned up

	// TODO: retries of propagated operations
}
