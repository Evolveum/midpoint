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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

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
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
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
public class TestDummy extends AbstractBasicDummyTest {

	protected static final String BLACKBEARD_USERNAME = "BlackBeard";
	protected static final String DRAKE_USERNAME = "Drake";
	// Make this ugly by design. it check for some caseExact/caseIgnore cases
	protected static final String ACCOUNT_MURRAY_USERNAME = "muRRay";

	protected static final long VALID_FROM_MILLIS = 12322342345435L;
	protected static final long VALID_TO_MILLIS = 3454564324423L;

	private static final String GROUP_CORSAIRS_NAME = "corsairs";

	private static final Trace LOGGER = TraceManager.getTrace(TestDummy.class);

	private String drakeAccountOid;

	protected String morganIcfUid;
	private String williamIcfUid;
	protected String piratesIcfUid;
	private String pillageIcfUid;
    private String bargainIcfUid;
	private String leChuckIcfUid;
	private String blackbeardIcfUid;
	private String drakeIcfUid;
	private String corsairsIcfUid;
	private String corsairsShadowOid;
	private String meathookAccountOid;

	protected String getMurrayRepoIcfName() {
		return ACCOUNT_MURRAY_USERNAME;
	}

	protected String getBlackbeardRepoIcfName() {
		return BLACKBEARD_USERNAME;
	}

	protected String getDrakeRepoIcfName() {
		return DRAKE_USERNAME;
	}

	protected ItemComparisonResult getExpectedPasswordComparisonResultMatch() {
		return ItemComparisonResult.NOT_APPLICABLE;
	}

	protected ItemComparisonResult getExpectedPasswordComparisonResultMismatch() {
		return ItemComparisonResult.NOT_APPLICABLE;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
//		InternalMonitor.setTraceConnectorOperation(true);
	}

	// test000-test100 in the superclasses

	@Test
	public void test101AddAccountWithoutName() throws Exception {
		final String TEST_NAME = "test101AddAccountWithoutName";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);
		syncServiceMock.reset();

		ShadowType account = parseObjectType(ACCOUNT_MORGAN_FILE, ShadowType.class);

		display("Adding shadow", account.asPrismObject());

		// WHEN
		displayWhen(TEST_NAME, "add");
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, syncTask, result);

		// THEN
		displayThen(TEST_NAME, "add");
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

		ShadowType accountType = getShadowRepo(ACCOUNT_MORGAN_OID).asObjectable();
		PrismAsserts.assertEqualsPolyString("Account name was not generated (repository)", ACCOUNT_MORGAN_NAME, accountType.getName());
		morganIcfUid = getIcfUid(accountType);

		syncServiceMock.assertNotifySuccessOnly();

		// WHEN
		displayWhen(TEST_NAME, "get");
		PrismObject<ShadowType> provisioningAccount = provisioningService.getObject(ShadowType.class,
				ACCOUNT_MORGAN_OID, null, syncTask, result);
		
		// THEN
		displayThen(TEST_NAME, "get");
		display("account from provisioning", provisioningAccount);
		ShadowType provisioningAccountType = provisioningAccount.asObjectable();
		PrismAsserts.assertEqualsPolyString("Account name was not generated (provisioning)", transformNameFromResource(ACCOUNT_MORGAN_NAME),
				provisioningAccountType.getName());
		// MID-4751
		assertAttribute(provisioningAccount, 
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME,
				XmlTypeConverter.createXMLGregorianCalendar(ZonedDateTime.parse(ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP)));
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				provisioningAccountType, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		// Check if the account was created in the dummy resource
		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_MORGAN_NAME), getIcfUid(provisioningAccountType));
		display("Dummy account", dummyAccount);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Captain Morgan", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", ACCOUNT_MORGAN_PASSWORD, dummyAccount.getPassword());
		// MID-4751
		ZonedDateTime enlistTimestamp = dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME, ZonedDateTime.class);
		assertNotNull("No enlistTimestamp in dummy account", enlistTimestamp);
		assertEqualTime("Wrong enlistTimestamp in dummy account", ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP, enlistTimestamp);

		// Check if the shadow is in the repo
		PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.debugDump());

		checkRepoAccountShadow(shadowFromRepo);
		// MID-4397
		assertRepoShadowCredentials(shadowFromRepo, ACCOUNT_MORGAN_PASSWORD);

		checkConsistency(account.asPrismObject());

		assertSteadyResource();
	}
	
	// test102-test106 in the superclasses

	/**
	 * Make a native modification to an account and read it with max staleness option.
	 * As there is no caching enabled this should throw an error.
	 *
	 * Note: This test is overridden in TestDummyCaching
	 *
	 * MID-3481
	 */
	@Test
	public void test107AGetModifiedAccountFromCacheMax() throws Exception {
		final String TEST_NAME = "test107AGetModifiedAccountFromCacheMax";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		DummyAccount accountWill = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Nice Pirate");
		accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Interceptor");
		accountWill.setEnabled(true);

		Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(GetOperationOptions.createMaxStaleness());

		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);

		try {

			ShadowType shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null,
				result).asObjectable();

			AssertJUnit.fail("Unexpected success");
		} catch (ConfigurationException e) {
			// Caching is disabled, this is expected.
			displayThen(TEST_NAME);
			display("Expected exception", e);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}

		PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
		checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.DISABLED);

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		assertSteadyResource();
	}

	/**
	 * Make a native modification to an account and read it with high staleness option.
	 * In this test there is no caching enabled, so this should return fresh data.
	 *
	 * Note: This test is overridden in TestDummyCaching
	 *
	 * MID-3481
	 */
	@Test
	public void test107BGetModifiedAccountFromCacheHighStaleness() throws Exception {
		final String TEST_NAME = "test107BGetModifiedAccountFromCacheHighStaleness";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		DummyAccount accountWill = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
		accountWill.setEnabled(true);

		Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(GetOperationOptions.createStaleness(1000000L));

		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);

		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");

		PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
		checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		assertSteadyResource();
	}

	/**
	 * Staleness of one millisecond is too small for the cache to work.
	 * Fresh data should be returned - both in case the cache is enabled and disabled.
	 * MID-3481
	 */
	@Test
	public void test108GetAccountLowStaleness() throws Exception {
		final String TEST_NAME = "test106GetModifiedAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(GetOperationOptions.createStaleness(1L));

		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkAccountShadow(shadow, result, true, startTs, endTs);
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Interceptor");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 7, attributes.size());

		PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
		checkRepoAccountShadowWillBasic(shadowRepo, startTs, endTs, null);

		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Interceptor");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);

		checkConsistency(shadow);

		assertCachingMetadata(shadow, false, startTs, endTs);

		assertSteadyResource();
	}

	/**
	 * Clean up after caching tests so we won't break subsequent tests.
	 * MID-3481
	 */
	@Test
	public void test109ModifiedAccountCleanup() throws Exception {
		final String TEST_NAME = "test109ModifiedAccountCleanup";
		displayTestTitle(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		DummyAccount accountWill = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		// Modify this back so won't break subsequent tests
		accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
		accountWill.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
		accountWill.setEnabled(true);

		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, result);

		// THEN
		assertSuccess(result);
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkAccountWill(shadow, result, startTs, endTs);
		PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
		checkRepoAccountShadowWill(shadowRepo, startTs, endTs);

		checkConsistency(shadow);

		assertCachingMetadata(shadow, false, startTs, endTs);

		assertSteadyResource();
	}

	@Test
	public void test110SeachIterative() throws Exception {
		final String TEST_NAME = "test110SeachIterative";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// Make sure there is an account on resource that the provisioning has
		// never seen before, so there is no shadow
		// for it yet.
		DummyAccount newAccount = new DummyAccount("meathook");
		newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Meathook");
		newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Sea Monkey");
		newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "hook");
		newAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 666);
		newAccount.setEnabled(true);
		newAccount.setPassword("parrotMonster");
		dummyResource.addAccount(newAccount);

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID,
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
						SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext);

		final XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		final Holder<Boolean> seenMeathookHolder = new Holder<>(false);
		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>();
		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				foundObjects.add(object);
				display("Found", object);

				XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

				assertTrue(object.canRepresent(ShadowType.class));
				try {
					checkAccountShadow(object, parentResult, true, startTs, endTs);
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(), e);
				}

				assertCachingMetadata(object, false, startTs, endTs);

				if (object.asObjectable().getName().getOrig().equals("meathook")) {
					meathookAccountOid = object.getOid();
					seenMeathookHolder.setValue(true);
					try {
						Integer loot = ShadowUtil.getAttributeValue(object, dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME));
						assertEquals("Wrong meathook's loot", 666, (int) loot);
					} catch (SchemaException e) {
						throw new SystemException(e.getMessage(), e);
					}
				}

				return true;
			}
		};
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, null, result);

		// THEN

		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertSuccess(result);
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		assertEquals(4, foundObjects.size());
		checkConsistency(foundObjects);
		assertProtected(foundObjects, 1);

		PrismObject<ShadowType> shadowWillRepo = getShadowRepo(ACCOUNT_WILL_OID);
		assertRepoShadowCachedAttributeValue(shadowWillRepo,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
		checkRepoAccountShadowWill(shadowWillRepo, startTs, endTs);

		PrismObject<ShadowType> shadowMeathook = getShadowRepo(meathookAccountOid);
		display("Meathook shadow", shadowMeathook);
		assertRepoShadowCachedAttributeValue(shadowMeathook,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "hook");
		assertRepoCachingMetadata(shadowMeathook, startTs, endTs);


		// And again ...

		foundObjects.clear();
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		XMLGregorianCalendar startTs2 = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, null, result);

		// THEN

		XMLGregorianCalendar endTs2 = clock.currentTimeXMLGregorianCalendar();
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		display("Found shadows", foundObjects);

		assertEquals(4, foundObjects.size());
		checkConsistency(foundObjects);
		assertProtected(foundObjects, 1);

		shadowWillRepo = getShadowRepo(ACCOUNT_WILL_OID);
		checkRepoAccountShadowWill(shadowWillRepo, startTs2, endTs2);

		shadowMeathook = getShadowRepo(meathookAccountOid);
		assertRepoShadowCachedAttributeValue(shadowMeathook,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "hook");
		assertRepoCachingMetadata(shadowMeathook, startTs2, endTs2);

		assertSteadyResource();
	}

	@Test
	public void test111SeachIterativeNoFetch() throws Exception {
		final String TEST_NAME = "test111SeachIterativeNoFetch";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID,
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
						SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext);

		final XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>();
		ResultHandler<ShadowType> handler = (shadow, parentResult) -> {
				foundObjects.add(shadow);

				assertTrue(shadow.canRepresent(ShadowType.class));
				try {
					checkCachedAccountShadow(shadow, parentResult, false, null, startTs);
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(), e);
				}

				assertRepoCachingMetadata(shadow, null, startTs);

				if (shadow.asObjectable().getName().getOrig().equals("meathook")) {
					assertRepoShadowCachedAttributeValue(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Sea Monkey");
				}

				return true;
			};
			
		Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		provisioningService.searchObjectsIterative(ShadowType.class, query, options, handler, null, result);

		// THEN
		assertSuccess(result);
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		display("Found shadows", foundObjects);

		assertEquals(4, foundObjects.size());
		checkConsistency(foundObjects);
		assertProtected(foundObjects, 1);       // MID-1640

		assertSteadyResource();
	}

	@Test
	public void test112SeachIterativeKindIntent() throws Exception {
		final String TEST_NAME = "test112SeachIterativeKindIntent";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_DUMMY_OID,
				ShadowKindType.ACCOUNT, "default", prismContext);
		display("query", query);

		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>();

		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		provisioningService.searchObjectsIterative(ShadowType.class, query, null,
			(object, parentResult) -> {
				foundObjects.add(object);
				return true;
				}, 
			null, result);

		// THEN
		result.computeStatus();
		display("searchObjectsIterative result", result);
		TestUtil.assertSuccess(result);
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		display("Found shadows", foundObjects);

		assertEquals(4, foundObjects.size());
		checkConsistency(foundObjects);
		assertProtected(foundObjects, 1);       // MID-1640

		assertSteadyResource();
	}

	protected <T extends ShadowType> void assertProtected(List<PrismObject<T>> shadows, int expectedNumberOfProtectedShadows) {
		int actual = countProtected(shadows);
		assertEquals("Unexpected number of protected shadows", expectedNumberOfProtectedShadows, actual);
	}

	private <T extends ShadowType> int countProtected(List<PrismObject<T>> shadows) {
		int count = 0;
		for (PrismObject<T> shadow: shadows) {
			if (shadow.asObjectable().isProtectedObject() != null && shadow.asObjectable().isProtectedObject()) {
				count ++;
			}
		}
		return count;
	}

	@Test
	public void test113SearchAllShadowsInRepository() throws Exception {
		displayTestTitle("test113SearchAllShadowsInRepository");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test113SearchAllShadowsInRepository");
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<ShadowType>> allShadows = repositoryService.searchObjects(ShadowType.class,
				query, null, result);

		// THEN
		assertSuccess(result);

		display("Found " + allShadows.size() + " shadows");
		display("Found shadows", allShadows);

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 4, allShadows.size());

		assertSteadyResource();
	}

	@Test
	public void test114SearchAllAccounts() throws Exception {
		final String TEST_NAME = "test114SearchAllAccounts";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
				query, null, null, result);

		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		TestUtil.assertSuccess(result);

		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 4, allShadows.size());

		checkConsistency(allShadows);
		assertProtected(allShadows, 1);

		assertSteadyResource();
	}

	@Test
	public void test115CountAllAccounts() throws Exception {
		final String TEST_NAME = "test115CountAllAccounts";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		displayWhen(TEST_NAME);
		Integer count = provisioningService.countObjects(ShadowType.class, query, null, null, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Found " + count + " shadows");

		assertEquals("Wrong number of count results", getTest115ExpectedCount(), count);

		assertSteadyResource();
	}
	
	protected Integer getTest115ExpectedCount() {
		return 4;
	}

	@Test
	public void test116SearchNullQueryResource() throws Exception {
		final String TEST_NAME = "test116SearchNullQueryResource";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		List<PrismObject<ResourceType>> allResources = provisioningService.searchObjects(ResourceType.class,
				new ObjectQuery(), null, null, result);

		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		TestUtil.assertSuccess(result);

		display("Found " + allResources.size() + " resources");

		assertFalse("No resources found", allResources.isEmpty());
		assertEquals("Wrong number of results", 1, allResources.size());

		assertSteadyResource();
	}

	@Test
	public void test117CountNullQueryResource() throws Exception {
		displayTestTitle("test117CountNullQueryResource");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test117CountNullQueryResource");

		// WHEN
		int count = provisioningService.countObjects(ResourceType.class, new ObjectQuery(), null, null, result);

		// THEN
		result.computeStatus();
		display("countObjects result", result);
		TestUtil.assertSuccess(result);

		display("Counted " + count + " resources");

		assertEquals("Wrong count", 1, count);

		assertSteadyResource();
	}

	/**
	 * Search for all accounts with long staleness option. This is a search,
	 * so we cannot evaluate whether our data are fresh enough. Therefore
	 * search on resource will always be performed.
	 * MID-3481
	 */
	@Test
	public void test118SearchAllAccountsLongStaleness() throws Exception {
		final String TEST_NAME = "test118SearchAllAccountsLongStaleness";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(GetOperationOptions.createStaleness(1000000L));

		// WHEN
		List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
				query, options, null, result);

		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		TestUtil.assertSuccess(result);

		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 4, allShadows.size());

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		checkConsistency(allShadows);
		assertProtected(allShadows, 1);

		assertSteadyResource();
	}

	/**
	 * Search for all accounts with maximum staleness option.
	 * This is supposed to return only cached data. Therefore
	 * repo search is performed. But as caching is
	 * not enabled in this test only errors will be returned.
	 *
	 * Note: This test is overridden in TestDummyCaching
	 *
	 * MID-3481
	 */
	@Test
	public void test119SearchAllAccountsMaxStaleness() throws Exception {
		final String TEST_NAME = "test119SearchAllAccountsMaxStaleness";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(GetOperationOptions.createMaxStaleness());

		// WHEN
		List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
				query, options, null, result);

		// THEN
		display("searchObjects result", result);
		result.computeStatus();
		TestUtil.assertFailure(result);

		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 4, allShadows.size());

		for (PrismObject<ShadowType> shadow: allShadows) {
			display("Found shadow (error expected)", shadow);
			OperationResultType fetchResult = shadow.asObjectable().getFetchResult();
			assertNotNull("No fetch result status in "+shadow, fetchResult);
			assertEquals("Wrong fetch result status in "+shadow, OperationResultStatusType.FATAL_ERROR, fetchResult.getStatus());
		}

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		assertProtected(allShadows, 1);

		assertSteadyResource();
	}

	@Test
	public void test120ModifyObjectReplace() throws Exception {
		final String TEST_NAME = "test120ModifyObjectReplace";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Pirate Will Turner");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		delta.checkConsistence();
		assertDummyAccountAttributeValues(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Pirate Will Turner");

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test121ModifyObjectAddPirate() throws Exception {
		final String TEST_NAME = "test121ModifyObjectAddPirate";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddProperty(ShadowType.class,
				ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
				prismContext, "Pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid,
				DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test122ModifyObjectAddCaptain() throws Exception {
		final String TEST_NAME = "test122ModifyObjectAddCaptain";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddProperty(ShadowType.class,
				ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
				prismContext, "Captain");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid,
				DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate", "Captain");

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test123ModifyObjectDeletePirate() throws Exception {
		final String TEST_NAME = "test123ModifyObjectDeletePirate";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
				ACCOUNT_WILL_OID, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME), prismContext, "Pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid,
				DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Captain");

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	/**
	 * Try to add the same value that the account attribute already has. Resources that do not tolerate this will fail
	 * unless the mechanism to compensate for this works properly.
	 */
	@Test
	public void test124ModifyAccountWillAddCaptainAgain() throws Exception {
		final String TEST_NAME = "test124ModifyAccountWillAddCaptainAgain";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddProperty(ShadowType.class,
				ACCOUNT_WILL_OID, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME), prismContext, "Captain");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid,
				DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Captain");

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	/**
	 * MID-4397
	 */
	@Test
	public void test125CompareAccountWillPassword() throws Exception {
		final String TEST_NAME = "test125CompareAccountWillPassword";
		displayTestTitle(TEST_NAME);

		testComparePassword(TEST_NAME, "match", ACCOUNT_WILL_OID, accountWillCurrentPassword, getExpectedPasswordComparisonResultMatch());
		testComparePassword(TEST_NAME, "mismatch", ACCOUNT_WILL_OID, "I woulD NeVeR ever USE this PASSword", getExpectedPasswordComparisonResultMismatch());

		assertSteadyResource();
	}
	
	/**
	 * MID-4397
	 */
	@Test
	public void test126ModifyAccountWillPassword() throws Exception {
		final String TEST_NAME = "test126ModifyAccountWillPassword";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = createAccountPaswordDelta(ACCOUNT_WILL_OID, ACCOUNT_WILL_PASSWORD_NEW);
		display("ObjectDelta", delta);

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		// Check if the account was created in the dummy resource
		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD_NEW, dummyAccount.getPassword());
		accountWillCurrentPassword = ACCOUNT_WILL_PASSWORD_NEW;

		// Check if the shadow is in the repo
		PrismObject<ShadowType> repoShadow = getShadowRepo(ACCOUNT_WILL_OID);
		assertNotNull("Shadow was not created in the repository", repoShadow);
		display("Repository shadow", repoShadow);

		checkRepoAccountShadow(repoShadow);
		assertRepoShadowCredentials(repoShadow, ACCOUNT_WILL_PASSWORD_NEW);

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	/**
	 * MID-4397
	 */
	@Test
	public void test127CompareAccountWillPassword() throws Exception {
		final String TEST_NAME = "test125CompareAccountWillPassword";
		displayTestTitle(TEST_NAME);

		testComparePassword(TEST_NAME, "match", ACCOUNT_WILL_OID, accountWillCurrentPassword, getExpectedPasswordComparisonResultMatch());
		testComparePassword(TEST_NAME, "mismatch old password", ACCOUNT_WILL_OID, ACCOUNT_WILL_PASSWORD, getExpectedPasswordComparisonResultMismatch());
		testComparePassword(TEST_NAME, "mismatch", ACCOUNT_WILL_OID, "I woulD NeVeR ever USE this PASSword", getExpectedPasswordComparisonResultMismatch());

		assertSteadyResource();
	}
	
	protected void testComparePassword(final String TEST_NAME, String tag, String shadowOid, String expectedPassword, ItemComparisonResult expectedResult) throws Exception {
		Task task = createTask(TEST_NAME+".tag");
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		// WHEN (match)
		displayWhen(TEST_NAME);
		ItemComparisonResult comparisonResult = provisioningService.compare(ShadowType.class, shadowOid, SchemaConstants.PATH_PASSWORD_VALUE, 
				expectedPassword, task, result);

		// THEN (match)
		displayThen(TEST_NAME);
		assertSuccess(result);

		display("Comparison result ("+tag+")", comparisonResult);
		assertEquals("Wrong comparison result ("+tag+")", expectedResult, comparisonResult);
		
		syncServiceMock.assertNoNotifcations();		
	}

	/**
	 * Set a null value to the (native) dummy attribute. The UCF layer should filter that out.
	 */
	@Test
	public void test129NullAttributeValue() throws Exception {
		final String TEST_NAME = "test129NullAttributeValue";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		DummyAccount willDummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		willDummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, null);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> accountWill = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(accountWill);
		ResourceAttribute<Object> titleAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME));
		assertNull("Title attribute sneaked in", titleAttribute);

		accountWill.checkConsistence();

		assertSteadyResource();
	}

	@Test
	public void test131AddScript() throws Exception {
		final String TEST_NAME = "test131AddScript";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		ShadowType account = parseObjectType(ACCOUNT_SCRIPT_FILE, ShadowType.class);
		display("Account before add", account);

		OperationProvisioningScriptsType scriptsType = unmarshallValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_NEW_SCRIPT_OID, addedObjectOid);

		ShadowType accountType = getShadowRepo(ACCOUNT_NEW_SCRIPT_OID).asObjectable();
		assertShadowName(accountType, "william");

		syncServiceMock.assertNotifySuccessOnly();

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, null, task, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", transformNameFromResource("william"), provisioningAccountType.getName());
		williamIcfUid = getIcfUid(accountType);

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource("william"), williamIcfUid);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "William Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());

		ProvisioningScriptSpec beforeScript = new ProvisioningScriptSpec("In the beginning ...");
		beforeScript.addArgSingle("HOMEDIR", "jbond");
		ProvisioningScriptSpec afterScript = new ProvisioningScriptSpec("Hello World");
		afterScript.addArgSingle("which", "this");
		afterScript.addArgSingle("when", "now");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), beforeScript, afterScript);

		assertSteadyResource();
	}

	// MID-1113
	@Test
	public void test132ModifyScript() throws Exception {
		final String TEST_NAME = "test132ModifyScript";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Will Turner");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID, delta.getModifications(),
				scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = getDummyAccountAssert("william", williamIcfUid);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());

		ProvisioningScriptSpec beforeScript = new ProvisioningScriptSpec("Where am I?");
		ProvisioningScriptSpec afterScript = new ProvisioningScriptSpec("Still here");
		afterScript.addArgMulti("status", "dead", "alive");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), beforeScript, afterScript);

		assertSteadyResource();
	}

	/**
	 * This test modifies account shadow property that does NOT result in account modification
	 * on resource. The scripts must not be executed.
	 */
	@Test
	public void test133ModifyScriptNoExec() throws Exception {
		final String TEST_NAME = "test133ModifyScriptNoExec";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, ShadowType.F_DESCRIPTION, prismContext, "Blah blah");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID, delta.getModifications(),
				scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = getDummyAccountAssert("william", williamIcfUid);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());

		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory());

		assertSteadyResource();
	}

	@Test
	public void test134DeleteScript() throws Exception {
		final String TEST_NAME = "test134DeleteScript";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

		// WHEN
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID, null, scriptsType,
				task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = getDummyAccount("william", williamIcfUid);
		assertNull("Dummy account not gone", dummyAccount);

		ProvisioningScriptSpec beforeScript = new ProvisioningScriptSpec("Goodbye World");
		beforeScript.addArgMulti("what", "cruel");
		ProvisioningScriptSpec afterScript = new ProvisioningScriptSpec("R.I.P.");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), beforeScript, afterScript);

		assertSteadyResource();
	}

	@Test
	public void test135ExecuteScript() throws Exception {
		final String TEST_NAME = "test135ExecuteScript";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

		ProvisioningScriptType script = scriptsType.getScript().get(0);

		// WHEN
		provisioningService.executeScript(RESOURCE_DUMMY_OID, script, task, result);

		// THEN
		result.computeStatus();
		display("executeScript result", result);
		TestUtil.assertSuccess("executeScript has failed (result)", result);

		ProvisioningScriptSpec expectedScript = new ProvisioningScriptSpec("Where to go now?");
		expectedScript.addArgMulti("direction", "left", "right");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), expectedScript);

		assertSteadyResource();
	}

	@Test
	public void test150DisableAccount() throws Exception {
		final String TEST_NAME = "test150DisableAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertTrue(dummyAccount.isEnabled());

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext,
				ActivationStatusType.DISABLED);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertFalse("Dummy account "+transformNameFromResource(ACCOUNT_WILL_USERNAME)+" is enabled, expected disabled", dummyAccount.isEnabled());

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test151SearchDisabledAccounts() throws Exception {
		final String TEST_NAME = "test151SearchDisabledAccounts";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(),
				QueryBuilder.queryFor(ShadowType.class, prismContext)
						.item(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.DISABLED)
						.buildFilter());

		syncServiceMock.reset();

		// WHEN
		SearchResultList<PrismObject<ShadowType>> resultList = provisioningService.searchObjects(ShadowType.class, query, null, null, result);

		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		assertEquals("Unexpected number of search results", 1, resultList.size());
		PrismObject<ShadowType> shadow = resultList.get(0);
        display("Shadow", shadow);
        assertActivationAdministrativeStatus(shadow, ActivationStatusType.DISABLED);

		assertSteadyResource();
	}

	@Test
	public void test152ActivationStatusUndefinedAccount() throws Exception {
		final String TEST_NAME = "test152ActivationStatusUndefinedAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
				result).asObjectable();
		assertNotNull(accountType);
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertFalse("Account is not disabled", dummyAccount.isEnabled());

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext,
				ActivationStatusType.DISABLED);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertEquals("Wrong dummy account "+transformNameFromResource(ACCOUNT_WILL_USERNAME)+" enabled flag",
				null, dummyAccount.isEnabled());

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test154EnableAccount() throws Exception {
		final String TEST_NAME = "test154EnableAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
				result).asObjectable();
		assertNotNull(accountType);
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertEquals("Wrong dummy account enabled flag", null, dummyAccount.isEnabled());

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext,
				ActivationStatusType.ENABLED);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertTrue("Dummy account "+transformNameFromResource(ACCOUNT_WILL_USERNAME)+" is disabled, expected enabled", dummyAccount.isEnabled());

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test155SearchDisabledAccounts() throws Exception {
		final String TEST_NAME = "test155SearchDisabledAccounts";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(),
				QueryBuilder.queryFor(ShadowType.class, prismContext)
						.item(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.DISABLED)
						.buildFilter());

		syncServiceMock.reset();

		// WHEN
		SearchResultList<PrismObject<ShadowType>> resultList = provisioningService.searchObjects(ShadowType.class, query, null, null, result);

		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		assertEquals("Unexpected number of search results", 0, resultList.size());

		assertSteadyResource();
	}


	@Test
	public void test156SetValidFrom() throws Exception {
		final String TEST_NAME = "test156SetValidFrom";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertTrue(dummyAccount.isEnabled());

		syncServiceMock.reset();

		long millis = VALID_FROM_MILLIS;

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, prismContext,
				XmlTypeConverter.createXMLGregorianCalendar(VALID_FROM_MILLIS));
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertEquals("Wrong account validFrom in account "+transformNameFromResource(ACCOUNT_WILL_USERNAME), new Date(VALID_FROM_MILLIS), dummyAccount.getValidFrom());
		assertTrue("Dummy account "+transformNameFromResource(ACCOUNT_WILL_USERNAME)+" is disabled, expected enabled", dummyAccount.isEnabled());

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test157SetValidTo() throws Exception {
		final String TEST_NAME = "test157SetValidTo";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertTrue(dummyAccount.isEnabled());

		syncServiceMock.reset();

		long millis = VALID_TO_MILLIS;

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, prismContext,
				XmlTypeConverter.createXMLGregorianCalendar(VALID_TO_MILLIS));
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertEquals("Wrong account validFrom in account "+transformNameFromResource(ACCOUNT_WILL_USERNAME), new Date(VALID_FROM_MILLIS), dummyAccount.getValidFrom());
		assertEquals("Wrong account validTo in account "+transformNameFromResource(ACCOUNT_WILL_USERNAME), new Date(VALID_TO_MILLIS), dummyAccount.getValidTo());
		assertTrue("Dummy account "+transformNameFromResource(ACCOUNT_WILL_USERNAME)+" is disabled, expected enabled", dummyAccount.isEnabled());

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test158DeleteValidToValidFrom() throws Exception {
		final String TEST_NAME = "test158DeleteValidToValidFrom";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertTrue(dummyAccount.isEnabled());

		syncServiceMock.reset();

//		long millis = VALID_TO_MILLIS;

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, prismContext,
				XmlTypeConverter.createXMLGregorianCalendar(VALID_TO_MILLIS));
		PrismObjectDefinition<ShadowType> def = accountType.asPrismObject().getDefinition();
		PropertyDelta<XMLGregorianCalendar> validFromDelta = PropertyDelta.createModificationDeleteProperty(
				SchemaConstants.PATH_ACTIVATION_VALID_FROM,
				def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_VALID_FROM),
				XmlTypeConverter.createXMLGregorianCalendar(VALID_FROM_MILLIS));
		delta.addModification(validFromDelta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNull("Unexpected account validTo in account "+transformNameFromResource(ACCOUNT_WILL_USERNAME) + ": " + dummyAccount.getValidTo(), dummyAccount.getValidTo());
		assertNull("Unexpected account validFrom in account "+transformNameFromResource(ACCOUNT_WILL_USERNAME) + ": " + dummyAccount.getValidFrom(), dummyAccount.getValidFrom());
		assertTrue("Dummy account "+transformNameFromResource(ACCOUNT_WILL_USERNAME)+" is disabled, expected enabled", dummyAccount.isEnabled());

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test159GetLockedoutAccount() throws Exception {
		final String TEST_NAME = "test159GetLockedoutAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		dummyAccount.setLockout(true);

		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		if (supportsActivation()) {
			PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
				LockoutStatusType.LOCKED);
		} else {
			PrismAsserts.assertNoItem(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
		}

		checkAccountWill(shadow, result, startTs, endTs);

		checkConsistency(shadow);

		assertSteadyResource();
	}

	@Test
	public void test160SearchLockedAccounts() throws Exception {
		final String TEST_NAME = "test160SearchLockedAccounts";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(),
				QueryBuilder.queryFor(ShadowType.class, prismContext)
						.item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).eq(LockoutStatusType.LOCKED)
						.buildFilter());

		syncServiceMock.reset();

		// WHEN
		SearchResultList<PrismObject<ShadowType>> resultList = provisioningService.searchObjects(ShadowType.class, query, null, null, result);

		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		assertEquals("Unexpected number of search results", 1, resultList.size());
		PrismObject<ShadowType> shadow = resultList.get(0);
        display("Shadow", shadow);
        assertShadowLockout(shadow, LockoutStatusType.LOCKED);

		assertSteadyResource();
	}

	@Test
	public void test162UnlockAccount() throws Exception {
		final String TEST_NAME = "test162UnlockAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
				result).asObjectable();
		assertNotNull(accountType);
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertTrue("Account is not locked", dummyAccount.isLockout());

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, prismContext,
				LockoutStatusType.NORMAL);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertFalse("Dummy account "+transformNameFromResource(ACCOUNT_WILL_USERNAME)+" is locked, expected unlocked", dummyAccount.isLockout());

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	@Test
	public void test163GetAccount() throws Exception {
		final String TEST_NAME = "test163GetAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		if (supportsActivation()) {
			PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
				ActivationStatusType.ENABLED);
			PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
					LockoutStatusType.NORMAL);
		} else {
			PrismAsserts.assertNoItem(shadow, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
			PrismAsserts.assertNoItem(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
		}

		checkAccountWill(shadow, result, startTs, endTs);

		checkConsistency(shadow);

		assertSteadyResource();
	}

	@Test
	public void test163SearchLockedAccounts() throws Exception {
		final String TEST_NAME = "test163SearchLockedAccounts";
		displayTestTitle(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
		ObjectQueryUtil.filterAnd(query.getFilter(),
				QueryBuilder.queryFor(ShadowType.class, prismContext)
						.item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).eq(LockoutStatusType.LOCKED)
						.buildFilter());

		syncServiceMock.reset();

		// WHEN
		SearchResultList<PrismObject<ShadowType>> resultList = provisioningService.searchObjects(ShadowType.class, query, null, null, result);

		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		assertEquals("Unexpected number of search results", 0, resultList.size());

		assertSteadyResource();
	}

	@Test
	public void test170SearchNull() throws Exception {
		final String TEST_NAME = "test170SearchNull";
		displayTestTitle(TEST_NAME);
		testSeachIterative(TEST_NAME, null, null, true, true, false,
				"meathook", "daemon", transformNameFromResource("morgan"), transformNameFromResource("Will"));
	}

	@Test
	public void test171SearchShipSeaMonkey() throws Exception {
		final String TEST_NAME = "test171SearchShipSeaMonkey";
		displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Sea Monkey", null, true,
				"meathook");
	}

	// See MID-1460
	@Test(enabled=false)
	public void test172SearchShipNull() throws Exception {
		final String TEST_NAME = "test172SearchShipNull";
		displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, null, null, true,
				"daemon", "Will");
	}

	@Test
	public void test173SearchWeaponCutlass() throws Exception {
		final String TEST_NAME = "test173SearchWeaponCutlass";
		displayTestTitle(TEST_NAME);

		// Make sure there is an account on resource that the provisioning has
		// never seen before, so there is no shadow
		// for it yet.
		DummyAccount newAccount = new DummyAccount("carla");
		newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Carla");
		newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Sea Monkey");
		newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "cutlass");
		newAccount.setEnabled(true);
		dummyResource.addAccount(newAccount);

		IntegrationTestTools.display("dummy", dummyResource.debugDump());

		testSeachIterativeSingleAttrFilter(TEST_NAME,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "cutlass", null, true,
				transformNameFromResource("morgan"), "carla");
	}

	@Test
	public void test175SearchUidExact() throws Exception {
		final String TEST_NAME = "test175SearchUidExact";
		displayTestTitle(TEST_NAME);
		dummyResource.setDisableNameHintChecks(true);
		testSeachIterativeSingleAttrFilter(TEST_NAME,
				SchemaConstants.ICFS_UID, willIcfUid, null, true,
				transformNameFromResource("Will"));
		dummyResource.setDisableNameHintChecks(false);
	}

	@Test
	public void test176SearchUidExactNoFetch() throws Exception {
		final String TEST_NAME = "test176SearchUidExactNoFetch";
		displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME, SchemaConstants.ICFS_UID, willIcfUid,
				GetOperationOptions.createNoFetch(), false,
				transformNameFromResource("Will"));
	}

	@Test
	public void test177SearchIcfNameRepoized() throws Exception {
		final String TEST_NAME = "test177SearchIcfNameRepoized";
		displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME,
				SchemaConstants.ICFS_NAME, getWillRepoIcfName(), null, true,
				transformNameFromResource(ACCOUNT_WILL_USERNAME));
	}
	
	@Test
	public void test180SearchNullPagingOffset0Size3() throws Exception {
		final String TEST_NAME = "test180SearchNullPagingSize5";
		displayTestTitle(TEST_NAME);
		ObjectPaging paging = ObjectPaging.createPaging(0,3);
		paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME));
		SearchResultMetadata searchMetadata = testSeachIterativePaging(TEST_NAME, null, paging, null,
				getSortedUsernames18x(0,3));
		assertApproxNumberOfAllResults(searchMetadata, getTest18xApproxNumberOfSearchResults());
	}
	
	/**
	 * Reverse sort order, so we are sure that this thing is really sorting
	 * and not just returning data in alphabetical order by default.
	 */
	@Test
	public void test181SearchNullPagingOffset0Size3Desc() throws Exception {
		final String TEST_NAME = "test181SearchNullPagingOffset0Size3Desc";
		displayTestTitle(TEST_NAME);
		ObjectPaging paging = ObjectPaging.createPaging(0,3);
		paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME, OrderDirection.DESCENDING));
		SearchResultMetadata searchMetadata = testSeachIterativePaging(TEST_NAME, null, paging, null,
				getSortedUsernames18xDesc(0,3));
		assertApproxNumberOfAllResults(searchMetadata, getTest18xApproxNumberOfSearchResults());
	}
	
	@Test
	public void test182SearchNullPagingOffset1Size2() throws Exception {
		final String TEST_NAME = "test182SearchNullPagingOffset1Size2";
		displayTestTitle(TEST_NAME);
		ObjectPaging paging = ObjectPaging.createPaging(1,2);
		paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME));
		SearchResultMetadata searchMetadata = testSeachIterativePaging(TEST_NAME, null, paging, null,
				getSortedUsernames18x(1,2));
		assertApproxNumberOfAllResults(searchMetadata, getTest18xApproxNumberOfSearchResults());
	}
	
	@Test
	public void test183SearchNullPagingOffset2Size3Desc() throws Exception {
		final String TEST_NAME = "test183SearchNullPagingOffset1Size3Desc";
		displayTestTitle(TEST_NAME);
		ObjectPaging paging = ObjectPaging.createPaging(2,3);
		paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME, OrderDirection.DESCENDING));
		SearchResultMetadata searchMetadata = testSeachIterativePaging(TEST_NAME, null, paging, null,
				getSortedUsernames18xDesc(2,3));
		assertApproxNumberOfAllResults(searchMetadata, getTest18xApproxNumberOfSearchResults());
	}
	
	protected Integer getTest18xApproxNumberOfSearchResults() {
		return 5;
	}
	
	protected String[] getSortedUsernames18x(int offset, int pageSize) {
		return Arrays.copyOfRange(getSortedUsernames18x(), offset, offset + pageSize);
	}
	
	protected String[] getSortedUsernames18xDesc(int offset, int pageSize) {
		String[] usernames = getSortedUsernames18x();
		ArrayUtils.reverse(usernames);
		return Arrays.copyOfRange(usernames, offset, offset + pageSize);
	}
	
	protected String[] getSortedUsernames18x() {
		return new String[] { transformNameFromResource("Will"), "carla", "daemon", "meathook", transformNameFromResource("morgan") };
	}
	
	protected ObjectOrdering createAttributeOrdering(QName attrQname) {
		return createAttributeOrdering(attrQname, OrderDirection.ASCENDING);
	}
	
	protected ObjectOrdering createAttributeOrdering(QName attrQname, OrderDirection direction) {
		return ObjectOrdering.createOrdering(new ItemPath(ShadowType.F_ATTRIBUTES, attrQname), direction);
	}

	@Test
	public void test194SearchIcfNameRepoizedNoFetch() throws Exception {
		final String TEST_NAME = "test194SearchIcfNameRepoizedNoFetch";
		displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME, SchemaConstants.ICFS_NAME, getWillRepoIcfName(),
				GetOperationOptions.createNoFetch(), false,
				transformNameFromResource(ACCOUNT_WILL_USERNAME));
	}

	@Test
	public void test195SearchIcfNameExact() throws Exception {
		final String TEST_NAME = "test195SearchIcfNameExact";
		displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME,
				SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_WILL_USERNAME), null, true,
				transformNameFromResource(ACCOUNT_WILL_USERNAME));
	}

	@Test
	public void test196SearchIcfNameExactNoFetch() throws Exception {
		final String TEST_NAME = "test196SearchIcfNameExactNoFetch";
		displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME, SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_WILL_USERNAME),
				GetOperationOptions.createNoFetch(), false,
				transformNameFromResource(ACCOUNT_WILL_USERNAME));
	}

    // TEMPORARY todo move to more appropriate place (model-intest?)
    @Test
    public void test197SearchIcfNameAndUidExactNoFetch() throws Exception {
        final String TEST_NAME = "test197SearchIcfNameAndUidExactNoFetch";
        displayTestTitle(TEST_NAME);
        testSeachIterativeAlternativeAttrFilter(TEST_NAME, SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_WILL_USERNAME),
        		SchemaConstants.ICFS_UID, willIcfUid,
                GetOperationOptions.createNoFetch(), false,
                transformNameFromResource(ACCOUNT_WILL_USERNAME));
    }


    @Test
	public void test198SearchNone() throws Exception {
		final String TEST_NAME = "test198SearchNone";
		displayTestTitle(TEST_NAME);
		ObjectFilter attrFilter = NoneFilter.createNone();
		testSeachIterative(TEST_NAME, attrFilter, null, true, true, false);
	}

    /**
     * Search with query that queries both the repository and the resource.
     * We cannot do this. This should fail.
     * MID-2822
     */
    @Test
	public void test199SearchOnAndOffResource() throws Exception {
		final String TEST_NAME = "test199SearchOnAndOffResource";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = createOnOffQuery();

		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				AssertJUnit.fail("Handler called: "+object);
				return false;
			}
		};

		try {
			// WHEN
			provisioningService.searchObjectsIterative(ShadowType.class, query,
					null, handler, task, result);

			AssertJUnit.fail("unexpected success");

		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

		// THEN
		result.computeStatus();
		TestUtil.assertFailure(result);

	}

    /**
     * Search with query that queries both the repository and the resource.
     * NoFetch. This should go OK.
     * MID-2822
     */
    @Test
	public void test196SearchOnAndOffResourceNoFetch() throws Exception {
		final String TEST_NAME = "test196SearchOnAndOffResourceNoFetch";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = createOnOffQuery();

		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				AssertJUnit.fail("Handler called: "+object);
				return false;
			}
		};

		// WHEN
		provisioningService.searchObjectsIterative(ShadowType.class, query,
				SelectorOptions.createCollection(GetOperationOptions.createNoFetch()),
				handler, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

	}

    private ObjectQuery createOnOffQuery() throws SchemaException {
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition objectClassDef = resourceSchema.findObjectClassDefinition(SchemaTestConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
		ResourceAttributeDefinition<String> attrDef = objectClassDef.findAttributeDefinition(
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME))
				.and().itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getName()).eq("Sea Monkey")
				.and().item(ShadowType.F_DEAD).eq(true)
				.build();
		display("Query", query);
		return query;
	}

	protected <T> void testSeachIterativeSingleAttrFilter(final String TEST_NAME, String attrName, T attrVal,
			GetOperationOptions rootOptions, boolean fullShadow, String... expectedAccountIds) throws Exception {
		testSeachIterativeSingleAttrFilter(TEST_NAME, dummyResourceCtl.getAttributeQName(attrName), attrVal,
				rootOptions, fullShadow, expectedAccountIds);
	}

	protected <T> void testSeachIterativeSingleAttrFilter(final String TEST_NAME, QName attrQName, T attrVal,
			GetOperationOptions rootOptions, boolean fullShadow, String... expectedAccountNames) throws Exception {
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition objectClassDef = resourceSchema.findObjectClassDefinition(SchemaTestConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
		ResourceAttributeDefinition<T> attrDef = objectClassDef.findAttributeDefinition(attrQName);
		ObjectFilter filter = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getName()).eq(attrVal)
				.buildFilter();
		testSeachIterative(TEST_NAME, filter, rootOptions, fullShadow, true, false, expectedAccountNames);
	}

    protected <T> void testSeachIterativeAlternativeAttrFilter(final String TEST_NAME, QName attr1QName, T attr1Val,
                                                               QName attr2QName, T attr2Val,
                                                          GetOperationOptions rootOptions, boolean fullShadow, String... expectedAccountNames) throws Exception {
        ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        ObjectClassComplexTypeDefinition objectClassDef = resourceSchema.findObjectClassDefinition(SchemaTestConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
        ResourceAttributeDefinition<T> attr1Def = objectClassDef.findAttributeDefinition(attr1QName);
        ResourceAttributeDefinition<T> attr2Def = objectClassDef.findAttributeDefinition(attr2QName);
		ObjectFilter filter = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(attr1Def, ShadowType.F_ATTRIBUTES, attr1Def.getName()).eq(attr1Val)
				.or().itemWithDef(attr2Def, ShadowType.F_ATTRIBUTES, attr2Def.getName()).eq(attr2Val)
				.buildFilter();
        testSeachIterative(TEST_NAME, filter, rootOptions, fullShadow, false, true, expectedAccountNames);
    }


    private SearchResultMetadata testSeachIterative(final String TEST_NAME, ObjectFilter attrFilter, GetOperationOptions rootOptions,
			final boolean fullShadow, boolean useObjectClassFilter, final boolean useRepo, String... expectedAccountNames) throws Exception {
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		ObjectQuery query;
        if (useObjectClassFilter) {
            query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
            		SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext);
            if (attrFilter != null) {
                AndFilter filter = (AndFilter) query.getFilter();
                filter.getConditions().add(attrFilter);
            }
        } else {
            query = ObjectQueryUtil.createResourceQuery(RESOURCE_DUMMY_OID, prismContext);
            if (attrFilter != null) {
                query.setFilter(AndFilter.createAnd(query.getFilter(), attrFilter));
            }
        }

		display("Query", query);

		final XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>();
		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
				foundObjects.add(shadow);

				XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

				assertTrue(shadow.canRepresent(ShadowType.class));
                if (!useRepo) {
                    try {
						checkAccountShadow(shadow, parentResult, fullShadow, startTs, endTs);
					} catch (SchemaException e) {
						throw new SystemException(e.getMessage(), e);
					}
                }
				return true;
			}
		};

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);


		// WHEN
		displayWhen(TEST_NAME);
		SearchResultMetadata searchMetadata;
        if (useRepo) {
        	searchMetadata = repositoryService.searchObjectsIterative(ShadowType.class, query, handler, null, false, result);
        } else {
            searchMetadata = provisioningService.searchObjectsIterative(ShadowType.class, query, options, handler, null, result);
        }

		// THEN
        displayThen(TEST_NAME);
		result.computeStatus();
		display("searchObjectsIterative result", result);
		TestUtil.assertSuccess(result);

		display("found shadows", foundObjects);

		for (String expectedAccountId: expectedAccountNames) {
			boolean found = false;
			for (PrismObject<ShadowType> foundObject: foundObjects) {
				if (expectedAccountId.equals(foundObject.asObjectable().getName().getOrig())) {
					found = true;
					break;
				}
			}
			if (!found) {
				AssertJUnit.fail("Account "+expectedAccountId+" was expected to be found but it was not found (found "+foundObjects.size()+", expected "+expectedAccountNames.length+")");
			}
		}

		assertEquals("Wrong number of found objects ("+foundObjects+"): "+foundObjects, expectedAccountNames.length, foundObjects.size());
        if (!useRepo) {
            checkConsistency(foundObjects);
        }
        assertSteadyResource();
        
        return searchMetadata;
	}
    
    // This has to be a different method than ordinary search. We care about ordering here.
    // Also, paged search without sorting does not make much sense anyway.
    private SearchResultMetadata testSeachIterativePaging(final String TEST_NAME, ObjectFilter attrFilter, ObjectPaging paging, GetOperationOptions rootOptions, String... expectedAccountNames) throws Exception {
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
            		SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext);
        if (attrFilter != null) {
            AndFilter filter = (AndFilter) query.getFilter();
            filter.getConditions().add(attrFilter);
        }
    	query.setPaging(paging);


		display("Query", query);

		final XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>();
		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
				foundObjects.add(shadow);

				XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

				assertTrue(shadow.canRepresent(ShadowType.class));
                try {
					checkAccountShadow(shadow, parentResult, true, startTs, endTs);
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(), e);
				}
				return true;
			}
		};

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);


		// WHEN
		displayWhen(TEST_NAME);
		SearchResultMetadata searchMetadata = provisioningService.searchObjectsIterative(ShadowType.class, query, options, handler, null, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("searchObjectsIterative result", result);
		TestUtil.assertSuccess(result);

		display("found shadows", foundObjects);

		int i = 0;
		for (String expectedAccountId: expectedAccountNames) {
			PrismObject<ShadowType> foundObject = foundObjects.get(i);
			if (!expectedAccountId.equals(foundObject.asObjectable().getName().getOrig())) {
				fail("Account "+expectedAccountId+" was expected to be found on "+i+" position, but it was not found (found "+foundObject.asObjectable().getName().getOrig()+")");
			}
			i++;
		}

		assertEquals("Wrong number of found objects ("+foundObjects+"): "+foundObjects, expectedAccountNames.length, foundObjects.size());
        checkConsistency(foundObjects);
        assertSteadyResource();
        
        return searchMetadata;
	}

	@Test
	public void test200AddGroup() throws Exception {
		final String TEST_NAME = "test200AddGroup";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> group = prismContext.parseObject(GROUP_PIRATES_FILE);
		group.checkConsistence();

		rememberDummyResourceGroupMembersReadCount(null);

		display("Adding group", group);

		// WHEN
		String addedObjectOid = provisioningService.addObject(group, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertSuccess("addObject has failed (result)", result);
		assertEquals(GROUP_PIRATES_OID, addedObjectOid);

		group.checkConsistence();
		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		ShadowType groupRepoType = getShadowRepo(GROUP_PIRATES_OID).asObjectable();
		display("group from repo", groupRepoType);
		PrismAsserts.assertEqualsPolyString("Name not equal.", GROUP_PIRATES_NAME, groupRepoType.getName());
		assertEquals("Wrong kind (repo)", ShadowKindType.ENTITLEMENT, groupRepoType.getKind());

		syncServiceMock.assertNotifySuccessOnly();
		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		PrismObject<ShadowType> groupProvisioning = provisioningService.getObject(ShadowType.class, GROUP_PIRATES_OID, null, task, result);
		display("group from provisioning", groupProvisioning);
		checkGroupPirates(groupProvisioning, result);
		piratesIcfUid = getIcfUid(groupRepoType);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Check if the group was created in the dummy resource

		DummyGroup dummyGroup = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertNotNull("No dummy group "+GROUP_PIRATES_NAME, dummyGroup);
		assertEquals("Description is wrong", "Scurvy pirates", dummyGroup.getAttributeValue("description"));
		assertTrue("The group is not enabled", dummyGroup.isEnabled());

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.debugDump());

		checkRepoEntitlementShadow(shadowFromRepo);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		checkConsistency(group);
		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		assertSteadyResource();
	}

	@Test
	public void test202GetGroup() throws Exception {
		final String TEST_NAME = "test202GetGroup";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		rememberDummyResourceGroupMembersReadCount(null);

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, GROUP_PIRATES_OID, null, null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		display("Retrieved group shadow", shadow);

		assertNotNull("No dummy group", shadow);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		checkGroupPirates(shadow, result);

		checkConsistency(shadow);

		assertSteadyResource();
	}

	private void checkGroupPirates(PrismObject<ShadowType> shadow, OperationResult result) throws SchemaException {
		checkGroupShadow(shadow, result);
		PrismAsserts.assertEqualsPolyString("Name not equal.", transformNameFromResource(GROUP_PIRATES_NAME), shadow.getName());
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ENTITLEMENT, shadow.asObjectable().getKind());
		assertAttribute(shadow, DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Scurvy pirates");
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 3, attributes.size());

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadow, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
	}

	@Test
	public void test203GetGroupNoFetch() throws Exception {
		final String TEST_NAME="test203GetGroupNoFetch";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "."+TEST_NAME);

		GetOperationOptions rootOptions = new GetOperationOptions();
		rootOptions.setNoFetch(true);
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);

		rememberDummyResourceGroupMembersReadCount(null);

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, GROUP_PIRATES_OID, options, null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		display("Retrieved group shadow", shadow);

		assertNotNull("No dummy group", shadow);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		checkGroupShadow(shadow, result, false);

		checkConsistency(shadow);

		assertSteadyResource();
	}

	@Test
	public void test205ModifyGroupReplace() throws Exception {
		final String TEST_NAME = "test205ModifyGroupReplace";
		displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();

		rememberDummyResourceGroupMembersReadCount(null);
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				GROUP_PIRATES_OID,
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
				prismContext, "Bloodthirsty pirates");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertDummyAttributeValues(group, DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Bloodthirsty pirates");

		if (isAvoidDuplicateValues()) {
			assertDummyResourceGroupMembersReadCountIncrement(null, 1);
		} else {
			assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		}

		syncServiceMock.assertNotifySuccessOnly();
		assertSteadyResource();
	}

	@Test
	public void test210AddPrivilege() throws Exception {
		final String TEST_NAME = "test210AddPrivilege";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> priv = prismContext.parseObject(PRIVILEGE_PILLAGE_FILE);
		priv.checkConsistence();

		display("Adding priv", priv);

		// WHEN
		String addedObjectOid = provisioningService.addObject(priv, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertSuccess("addObject has failed (result)", result);
		assertEquals(PRIVILEGE_PILLAGE_OID, addedObjectOid);

		priv.checkConsistence();

		ShadowType groupRepoType = getShadowRepo(PRIVILEGE_PILLAGE_OID).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", PRIVILEGE_PILLAGE_NAME, groupRepoType.getName());
		assertEquals("Wrong kind (repo)", ShadowKindType.ENTITLEMENT, groupRepoType.getKind());

		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> privProvisioning = provisioningService.getObject(ShadowType.class,
				PRIVILEGE_PILLAGE_OID, null, task, result);
		display("priv from provisioning", privProvisioning);
		checkPrivPillage(privProvisioning, result);
		pillageIcfUid = getIcfUid(privProvisioning);

		// Check if the priv was created in the dummy resource

		DummyPrivilege dummyPriv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("No dummy priv "+PRIVILEGE_PILLAGE_NAME, dummyPriv);
		assertEquals("Wrong privilege power", (Integer)100, dummyPriv.getAttributeValue(DummyResourceContoller.DUMMY_PRIVILEGE_ATTRIBUTE_POWER, Integer.class));

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.debugDump());

		checkRepoEntitlementShadow(shadowFromRepo);

		checkConsistency(priv);
		assertSteadyResource();
	}

	@Test
	public void test212GetPriv() throws Exception {
		final String TEST_NAME = "test212GetPriv";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, null, null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		display("Retrieved priv shadow", shadow);

		assertNotNull("No dummy priv", shadow);

		checkPrivPillage(shadow, result);

		checkConsistency(shadow);

		assertSteadyResource();
	}

	private void checkPrivPillage(PrismObject<ShadowType> shadow, OperationResult result) throws SchemaException {
		checkEntitlementShadow(shadow, result, OBJECTCLAS_PRIVILEGE_LOCAL_NAME, true);
		assertShadowName(shadow, PRIVILEGE_PILLAGE_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ENTITLEMENT, shadow.asObjectable().getKind());
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		assertAttribute(shadow, DummyResourceContoller.DUMMY_PRIVILEGE_ATTRIBUTE_POWER, 100);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadow, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
	}

    @Test
    public void test214AddPrivilegeBargain() throws Exception {
        final String TEST_NAME = "test214AddPrivilegeBargain";
        displayTestTitle(TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> priv = prismContext.parseObject(PRIVILEGE_BARGAIN_FILE);
        priv.checkConsistence();

        rememberDummyResourceGroupMembersReadCount(null);

        display("Adding priv", priv);

        // WHEN
        String addedObjectOid = provisioningService.addObject(priv, null, null, task, result);

        // THEN
        result.computeStatus();
        display("add object result", result);
        TestUtil.assertSuccess("addObject has failed (result)", result);
        assertEquals(PRIVILEGE_BARGAIN_OID, addedObjectOid);

        priv.checkConsistence();
        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

        ShadowType groupRepoType = getShadowRepo(PRIVILEGE_BARGAIN_OID).asObjectable();
        PrismAsserts.assertEqualsPolyString("Name not equal.", PRIVILEGE_BARGAIN_NAME, groupRepoType.getName());
        assertEquals("Wrong kind (repo)", ShadowKindType.ENTITLEMENT, groupRepoType.getKind());

        syncServiceMock.assertNotifySuccessOnly();
        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

        PrismObject<ShadowType> privProvisioningType = provisioningService.getObject(ShadowType.class,
                PRIVILEGE_BARGAIN_OID, null, task, result);
        display("priv from provisioning", privProvisioningType);
        checkPrivBargain(privProvisioningType, result);
        bargainIcfUid = getIcfUid(privProvisioningType);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

        // Check if the group was created in the dummy resource

        DummyPrivilege dummyPriv = getDummyPrivilegeAssert(PRIVILEGE_BARGAIN_NAME, bargainIcfUid);
        assertNotNull("No dummy priv "+PRIVILEGE_BARGAIN_NAME, dummyPriv);

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
        assertNotNull("Shadow was not created in the repository", shadowFromRepo);
        display("Repository shadow", shadowFromRepo.debugDump());

        checkRepoEntitlementShadow(shadowFromRepo);

        checkConsistency(priv);
        assertDummyResourceGroupMembersReadCountIncrement(null, 0);
        assertSteadyResource();
    }

    private void checkPrivBargain(PrismObject<ShadowType> shadow, OperationResult result) throws SchemaException {
        checkEntitlementShadow(shadow, result, OBJECTCLAS_PRIVILEGE_LOCAL_NAME, true);
        assertShadowName(shadow, PRIVILEGE_BARGAIN_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ENTITLEMENT, shadow.asObjectable().getKind());
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertEquals("Unexpected number of attributes", 2, attributes.size());

        assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
                shadow, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
    }


    @Test
	public void test220EntitleAccountWillPirates() throws Exception {
		final String TEST_NAME = "test220EntitleAccountWillPirates";
		displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		rememberDummyResourceGroupMembersReadCount(null);
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		if (isAvoidDuplicateValues()) {
			assertDummyResourceGroupMembersReadCountIncrement(null, 1);
		} else {
			assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		}

		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, transformNameToResource(ACCOUNT_WILL_USERNAME));

		syncServiceMock.assertNotifySuccessOnly();
		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		assertSteadyResource();
	}

	/**
	 * Reads the will accounts, checks that the entitlement is there.
	 */
	@Test
	public void test221GetPirateWill() throws Exception {
		final String TEST_NAME = "test221GetPirateWill";
		displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();

		rememberDummyResourceGroupMembersReadCount(null);
		syncServiceMock.reset();

		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

		// THEN
		result.computeStatus();
		display("Account", account);

		display(result);
		TestUtil.assertSuccess(result);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		assertEntitlementGroup(account, GROUP_PIRATES_OID);

		// Just make sure nothing has changed
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, transformNameToResource(ACCOUNT_WILL_USERNAME));

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		assertSteadyResource();
	}

	@Test
	public void test222EntitleAccountWillPillage() throws Exception {
		final String TEST_NAME = "test222EntitleAccountWillPillage";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		rememberDummyResourceGroupMembersReadCount(null);
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
				ASSOCIATION_PRIV_NAME, PRIVILEGE_PILLAGE_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);

		delta.checkConsistence();
		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Make sure that the groups is still there and will is a member
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, transformNameToResource(ACCOUNT_WILL_USERNAME));

		syncServiceMock.assertNotifySuccessOnly();
		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow after", shadow);
		assertEntitlementGroup(shadow, GROUP_PIRATES_OID);
		assertEntitlementPriv(shadow, PRIVILEGE_PILLAGE_OID);

		assertSteadyResource();
	}

    @Test
    public void test223EntitleAccountWillBargain() throws Exception {
        final String TEST_NAME = "test223EntitleAccountWillBargain";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
        		ASSOCIATION_PRIV_NAME, PRIVILEGE_BARGAIN_OID, prismContext);
        display("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertNotNull("Account will is gone!", dummyAccount);
        Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
        PrismAsserts.assertSets("account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME, PRIVILEGE_BARGAIN_NAME);

        // Make sure that privilege object is still there
        DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
        assertNotNull("Privilege object (pillage) is gone!", priv);
        DummyPrivilege priv2 = getDummyPrivilegeAssert(PRIVILEGE_BARGAIN_NAME, bargainIcfUid);
        assertNotNull("Privilege object (bargain) is gone!", priv2);

        delta.checkConsistence();

        // Make sure that the groups is still there and will is a member
        DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
        assertMember(group, transformNameToResource(ACCOUNT_WILL_USERNAME));

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    /**
	 * Reads the will accounts, checks that both entitlements are there.
	 */
	@Test
	public void test224GetPillagingPirateWill() throws Exception {
		final String TEST_NAME = "test224GetPillagingPirateWill";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		rememberDummyResourceGroupMembersReadCount(null);
		syncServiceMock.reset();

		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

		// THEN
		result.computeStatus();
		display("Account", account);

		display(result);
		TestUtil.assertSuccess(result);

		assertEntitlementGroup(account, GROUP_PIRATES_OID);
		assertEntitlementPriv(account, PRIVILEGE_PILLAGE_OID);
        assertEntitlementPriv(account, PRIVILEGE_BARGAIN_OID);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Just make sure nothing has changed
		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("Wrong account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME, PRIVILEGE_BARGAIN_NAME);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);
        DummyPrivilege priv2 = getDummyPrivilegeAssert(PRIVILEGE_BARGAIN_NAME, bargainIcfUid);
        assertNotNull("Privilege object (bargain) is gone!", priv2);

		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, transformNameToResource(ACCOUNT_WILL_USERNAME));

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		assertSteadyResource();
	}

	/**
	 * Create a fresh group directly on the resource. So we are sure there is no shadow
	 * for it yet. Add will to this group. Get will account. Make sure that the group is
	 * in the associations.
	 */
	@Test
	public void test225GetFoolishPirateWill() throws Exception {
		final String TEST_NAME = "test225GetFoolishPirateWill";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		DummyGroup groupFools = new DummyGroup("fools");
		dummyResource.addGroup(groupFools);
		groupFools.addMember(transformNameFromResource(ACCOUNT_WILL_USERNAME));

		syncServiceMock.reset();
		rememberDummyResourceGroupMembersReadCount(null);
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);

		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

		// THEN
		result.computeStatus();
		display("Account", account);

		display(result);
		TestUtil.assertSuccess(result);
		assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 2);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		PrismObject<ShadowType> foolsShadow = findShadowByName(new QName(RESOURCE_DUMMY_NS, OBJECTCLAS_GROUP_LOCAL_NAME), "fools", resource, result);
		assertNotNull("No shadow for group fools", foolsShadow);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		assertEntitlementGroup(account, GROUP_PIRATES_OID);
		assertEntitlementGroup(account, foolsShadow.getOid());
		assertEntitlementPriv(account, PRIVILEGE_PILLAGE_OID);
        assertEntitlementPriv(account, PRIVILEGE_BARGAIN_OID);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Just make sure nothing has changed
		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("Wrong account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME, PRIVILEGE_BARGAIN_NAME);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);
        DummyPrivilege priv2 = getDummyPrivilegeAssert(PRIVILEGE_BARGAIN_NAME, bargainIcfUid);
        assertNotNull("Privilege object (bargain) is gone!", priv2);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, transformNameToResource(ACCOUNT_WILL_USERNAME));

		String foolsIcfUid = getIcfUid(foolsShadow);
		groupFools = getDummyGroupAssert("fools", foolsIcfUid);
		assertMember(groupFools, transformNameToResource(ACCOUNT_WILL_USERNAME));

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		assertSteadyResource();
	}

	/**
	 * Make the account point to a privilege that does not exist.
	 * MidPoint should ignore such privilege.
	 */
	@Test
    public void test226WillNonsensePrivilege() throws Exception {
        final String TEST_NAME = "test226WillNonsensePrivilege";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        dummyAccount.addAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, PRIVILEGE_NONSENSE_NAME);

        syncServiceMock.reset();

        // WHEN
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

		// THEN
		result.computeStatus();
		display("Account", shadow);

		display(result);
		TestUtil.assertSuccess(result);
		assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 3);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		PrismObject<ShadowType> foolsShadow = findShadowByName(new QName(RESOURCE_DUMMY_NS, OBJECTCLAS_GROUP_LOCAL_NAME), "fools", resource, result);
		assertNotNull("No shadow for group fools", foolsShadow);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		assertEntitlementGroup(shadow, GROUP_PIRATES_OID);
		assertEntitlementGroup(shadow, foolsShadow.getOid());
		assertEntitlementPriv(shadow, PRIVILEGE_PILLAGE_OID);
        assertEntitlementPriv(shadow, PRIVILEGE_BARGAIN_OID);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Just make sure nothing has changed
		dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("Wrong account privileges", accountProvileges,
				PRIVILEGE_PILLAGE_NAME, PRIVILEGE_BARGAIN_NAME, PRIVILEGE_NONSENSE_NAME);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);
        DummyPrivilege priv2 = getDummyPrivilegeAssert(PRIVILEGE_BARGAIN_NAME, bargainIcfUid);
        assertNotNull("Privilege object (bargain) is gone!", priv2);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, transformNameToResource(ACCOUNT_WILL_USERNAME));

		String foolsIcfUid = getIcfUid(foolsShadow);
		DummyGroup groupFools = getDummyGroupAssert("fools", foolsIcfUid);
		assertMember(groupFools, transformNameToResource(ACCOUNT_WILL_USERNAME));

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		assertSteadyResource();
    }

	@Test
	public void test230DetitleAccountWillPirates() throws Exception {
		final String TEST_NAME = "test230DetitleAccountWillPirates";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		rememberDummyResourceGroupMembersReadCount(null);
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		if (isAvoidDuplicateValues()) {
			assertDummyResourceGroupMembersReadCountIncrement(null, 1);
		} else {
			assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		}

		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertNoMember(group, getWillRepoIcfName());

		// Make sure that account is still there and it has the privilege
		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("Wrong account privileges", accountProvileges,
				PRIVILEGE_PILLAGE_NAME, PRIVILEGE_BARGAIN_NAME, PRIVILEGE_NONSENSE_NAME);

		assertDummyResourceGroupMembersReadCountIncrement(null, 0);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);
        DummyPrivilege priv2 = getDummyPrivilegeAssert(PRIVILEGE_BARGAIN_NAME, bargainIcfUid);
        assertNotNull("Privilege object (bargain) is gone!", priv2);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);
		syncServiceMock.assertNotifySuccessOnly();

        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow after", shadow);
		assertEntitlementPriv(shadow, PRIVILEGE_PILLAGE_OID);
		assertEntitlementPriv(shadow, PRIVILEGE_BARGAIN_OID);

		assertSteadyResource();
	}

	@Test
	public void test232DetitleAccountWillPillage() throws Exception {
		final String TEST_NAME = "test232DetitleAccountWillPillage";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
				ASSOCIATION_PRIV_NAME, PRIVILEGE_PILLAGE_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertNoMember(group, getWillRepoIcfName());

		// Make sure that account is still there and it has the privilege
		DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
        PrismAsserts.assertSets("Wrong account privileges", accountProvileges,
        		PRIVILEGE_BARGAIN_NAME, PRIVILEGE_NONSENSE_NAME);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);

		syncServiceMock.assertNotifySuccessOnly();

        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow after", shadow);
		assertEntitlementPriv(shadow, PRIVILEGE_BARGAIN_OID);


		assertSteadyResource();
	}

    @Test
    public void test234DetitleAccountWillBargain() throws Exception {
        final String TEST_NAME = "test234DetitleAccountWillBargain";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
        		ASSOCIATION_PRIV_NAME, PRIVILEGE_BARGAIN_OID, prismContext);
        display("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
        assertNoMember(group, getWillRepoIcfName());

        // Make sure that account is still there and it has the privilege
        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertNotNull("Account will is gone!", dummyAccount);
        Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("Wrong account privileges", accountProvileges, PRIVILEGE_NONSENSE_NAME);

        // Make sure that privilege object is still there
        DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
        assertNotNull("Privilege object is gone!", priv);
        DummyPrivilege priv2 = getDummyPrivilegeAssert(PRIVILEGE_BARGAIN_NAME, bargainIcfUid);
        assertNotNull("Privilege object (bargain) is gone!", priv);

        syncServiceMock.assertNotifySuccessOnly();
        assertSteadyResource();
    }

    /**
	 * LeChuck has both group and priv entitlement. Let's add him together with these entitlements.
	 */
	@Test
	public void test260AddAccountLeChuck() throws Exception {
		final String TEST_NAME = "test260AddAccountLeChuck";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> accountBefore = prismContext.parseObject(ACCOUNT_LECHUCK_FILE);
		accountBefore.checkConsistence();

		display("Adding shadow", accountBefore);

		// WHEN
		String addedObjectOid = provisioningService.addObject(accountBefore, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_LECHUCK_OID, addedObjectOid);

		accountBefore.checkConsistence();

		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, addedObjectOid, null, task, result);
		leChuckIcfUid = getIcfUid(shadow);

		// Check if the account was created in the dummy resource and that it has the entitlements

		DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_LECHUCK_NAME, leChuckIcfUid);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "LeChuck", dummyAccount.getAttributeValue(DummyAccount.ATTR_FULLNAME_NAME));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "und3ad", dummyAccount.getPassword());

		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);

		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, transformNameFromResource(ACCOUNT_LECHUCK_NAME));

		PrismObject<ShadowType> repoAccount = getShadowRepo(ACCOUNT_LECHUCK_OID);
		assertShadowName(repoAccount, ACCOUNT_LECHUCK_NAME);
		assertEquals("Wrong kind (repo)", ShadowKindType.ACCOUNT, repoAccount.asObjectable().getKind());
		assertAttribute(repoAccount, SchemaConstants.ICFS_NAME, ACCOUNT_LECHUCK_NAME);
		if (isIcfNameUidSame()) {
			assertAttribute(repoAccount, SchemaConstants.ICFS_UID, ACCOUNT_LECHUCK_NAME);
		} else {
			assertAttribute(repoAccount, SchemaConstants.ICFS_UID, dummyAccount.getId());
		}

		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> provisioningAccount = provisioningService.getObject(ShadowType.class,
				ACCOUNT_LECHUCK_OID, null, task, result);
		display("account from provisioning", provisioningAccount);
		assertShadowName(provisioningAccount, ACCOUNT_LECHUCK_NAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, provisioningAccount.asObjectable().getKind());
		assertAttribute(provisioningAccount, SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_LECHUCK_NAME));
		if (isIcfNameUidSame()) {
			assertAttribute(provisioningAccount, SchemaConstants.ICFS_UID, transformNameFromResource(ACCOUNT_LECHUCK_NAME));
		} else {
			assertAttribute(provisioningAccount, SchemaConstants.ICFS_UID, dummyAccount.getId());
		}

		assertEntitlementGroup(provisioningAccount, GROUP_PIRATES_OID);
		assertEntitlementPriv(provisioningAccount, PRIVILEGE_PILLAGE_OID);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				provisioningAccount, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		checkConsistency(provisioningAccount);

		assertSteadyResource();
	}

	/**
	 * LeChuck has both group and priv entitlement. If deleted it should be correctly removed from all
	 * the entitlements.
	 */
	@Test
	public void test265DeleteAccountLeChuck() throws Exception {
		final String TEST_NAME = "test265DeleteAccountLeChuck";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		// WHEN
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_LECHUCK_OID, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertSuccess("addObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account is gone and that group membership is gone as well

		DummyAccount dummyAccount = getDummyAccount(ACCOUNT_LECHUCK_NAME, leChuckIcfUid);
		assertNull("Dummy account is NOT gone", dummyAccount);

		// Make sure that privilege object is still there
		DummyPrivilege priv = getDummyPrivilegeAssert(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNotNull("Privilege object is gone!", priv);

		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertNoMember(group, ACCOUNT_LECHUCK_NAME);

		try {
			repositoryService.getObject(ShadowType.class, ACCOUNT_LECHUCK_OID, GetOperationOptions.createRawCollection(), result);

			AssertJUnit.fail("Shadow (repo) is not gone");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		try {
			provisioningService.getObject(ShadowType.class, ACCOUNT_LECHUCK_OID, null, task, result);

			AssertJUnit.fail("Shadow (provisioning) is not gone");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		assertSteadyResource();
	}

	// test28x in TestDummyCaseIgnore

	@Test
	public void test298DeletePrivPillage() throws Exception {
		final String TEST_NAME = "test298DeletePrivPillage";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		// WHEN
		provisioningService.deleteObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, null, null, task, result);

		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		syncServiceMock.assertNotifySuccessOnly();

		try {
			repositoryService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, GetOperationOptions.createRawCollection(), result);
			AssertJUnit.fail("Priv shadow is not gone (repo)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		try {
			provisioningService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, null, task, result);
			AssertJUnit.fail("Priv shadow is not gone (provisioning)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		DummyPrivilege priv = getDummyPrivilege(PRIVILEGE_PILLAGE_NAME, pillageIcfUid);
		assertNull("Privilege object NOT is gone", priv);

		assertSteadyResource();
	}

	@Test
	public void test299DeleteGroupPirates() throws Exception {
		final String TEST_NAME = "test299DeleteGroupPirates";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		// WHEN
		provisioningService.deleteObject(ShadowType.class, GROUP_PIRATES_OID, null, null, task, result);

		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		syncServiceMock.assertNotifySuccessOnly();

		try {
			repositoryService.getObject(ShadowType.class, GROUP_PIRATES_OID, GetOperationOptions.createRawCollection(), result);
			AssertJUnit.fail("Group shadow is not gone (repo)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		try {
			provisioningService.getObject(ShadowType.class, GROUP_PIRATES_OID, null, task, result);
			AssertJUnit.fail("Group shadow is not gone (provisioning)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		DummyGroup dummyAccount = getDummyGroup(GROUP_PIRATES_NAME, piratesIcfUid);
		assertNull("Dummy group '"+GROUP_PIRATES_NAME+"' is not gone from dummy resource", dummyAccount);

		assertSteadyResource();
	}

	@Test
	public void test300AccountRename() throws Exception {
		final String TEST_NAME = "test300AccountRename";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_MORGAN_OID, SchemaTestConstants.ICFS_NAME_PATH, prismContext, ACCOUNT_CPTMORGAN_NAME);
		provisioningService.applyDefinition(delta, task, result);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		delta.checkConsistence();
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, task, result);
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(account);
		assertNotNull("Identifiers must not be null", identifiers);
		assertEquals("Expected one identifier", 1, identifiers.size());

		ResourceAttribute<?> identifier = identifiers.iterator().next();

		String shadowUuid = ACCOUNT_CPTMORGAN_NAME;

		assertDummyAccountAttributeValues(shadowUuid, morganIcfUid,
				DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Captain Morgan");

		PrismObject<ShadowType> repoShadow = getShadowRepo(ACCOUNT_MORGAN_OID);
		assertAccountShadowRepo(repoShadow, ACCOUNT_MORGAN_OID, ACCOUNT_CPTMORGAN_NAME, resourceType);

		if (!isIcfNameUidSame()) {
			shadowUuid = (String) identifier.getRealValue();
		}
		PrismAsserts.assertPropertyValue(repoShadow, SchemaTestConstants.ICFS_UID_PATH, shadowUuid);

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}
	
	/**
	 * MID-4751
	 */
	@Test
	public void test310ModifyMorganEnlistTimestamp() throws Exception {
		final String TEST_NAME = "test310ModifyMorganEnlistTimestamp";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_MORGAN_OID,
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME),
				prismContext, 
				XmlTypeConverter.createXMLGregorianCalendarFromIso8601(ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP_MODIFIED));
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		delta.checkConsistence();
		// check if attribute was changed
		DummyAccount dummyAccount = getDummyAccount(transformNameFromResource(ACCOUNT_CPTMORGAN_NAME), morganIcfUid);
		display("Dummy account", dummyAccount);
		ZonedDateTime enlistTimestamp = dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME, ZonedDateTime.class);
		assertEqualTime("wrong dummy enlist timestamp", ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP_MODIFIED, enlistTimestamp);

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}

	// test4xx reserved for subclasses

	@Test
	public void test500AddProtectedAccount() throws Exception {
		final String TEST_NAME = "test500AddProtectedAccount";
		displayTestTitle(TEST_NAME);
		testAddProtectedAccount(TEST_NAME, ACCOUNT_DAVIEJONES_USERNAME);
	}

	@Test
	public void test501GetProtectedAccountShadow() throws Exception {
		final String TEST_NAME = "test501GetProtectedAccountShadow";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);

		assertEquals(""+account+" is not protected", Boolean.TRUE, account.asObjectable().isProtectedObject());
		checkConsistency(account);

		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		assertSteadyResource();
	}

	/**
	 * Attribute modification should fail.
	 */
	@Test
	public void test502ModifyProtectedAccountShadowAttributes() throws Exception {
		final String TEST_NAME = "test502ModifyProtectedAccountShadowAttributes";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		Collection<? extends ItemDelta> modifications = new ArrayList<>(1);
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		ResourceAttributeDefinition fullnameAttrDef = defaultAccountDefinition.findAttributeDefinition("fullname");
		ResourceAttribute fullnameAttr = fullnameAttrDef.instantiate();
		PropertyDelta fullnameDelta = fullnameAttr.createDelta(new ItemPath(ShadowType.F_ATTRIBUTES,
				fullnameAttrDef.getName()));
		fullnameDelta.setValueToReplace(new PrismPropertyValue<>("Good Daemon"));
		((Collection) modifications).add(fullnameDelta);

		// WHEN
		try {
			provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, modifications, null, null, task, result);
			AssertJUnit.fail("Expected security exception while modifying 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}

		result.computeStatus();
		display("modifyObject result (expected failure)", result);
		TestUtil.assertFailure(result);

		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();

		assertSteadyResource();
	}

	/**
	 * Modification of non-attribute property should go OK.
	 */
	@Test
	public void test503ModifyProtectedAccountShadowProperty() throws Exception {
		final String TEST_NAME = "test503ModifyProtectedAccountShadowProperty";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, ACCOUNT_DAEMON_OID,
				ShadowType.F_SYNCHRONIZATION_SITUATION, prismContext, SynchronizationSituationType.DISPUTED);

		// WHEN
		provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, shadowDelta.getModifications(), null, null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);
		assertEquals("Wrong situation", SynchronizationSituationType.DISPUTED, shadowAfter.asObjectable().getSynchronizationSituation());

		assertSteadyResource();
	}

	@Test
	public void test509DeleteProtectedAccountShadow() throws Exception {
		final String TEST_NAME = "test509DeleteProtectedAccountShadow";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		// WHEN
		try {
			provisioningService.deleteObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, null, task, result);
			AssertJUnit.fail("Expected security exception while deleting 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}

		result.computeStatus();
		display("deleteObject result (expected failure)", result);
		TestUtil.assertFailure(result);

		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();

		assertSteadyResource();
	}

	@Test
	public void test510AddProtectedAccounts() throws Exception {
		final String TEST_NAME = "test510AddProtectedAccounts";
		displayTestTitle(TEST_NAME);
		// GIVEN
		testAddProtectedAccount(TEST_NAME, "Xavier");
		testAddProtectedAccount(TEST_NAME, "Xenophobia");
		testAddProtectedAccount(TEST_NAME, "nobody-adm");
		testAddAccount(TEST_NAME, "abcadm");
		testAddAccount(TEST_NAME, "piXel");
		testAddAccount(TEST_NAME, "supernaturalius");
	}

	@Test
	public void test511AddProtectedAccountCaseIgnore() throws Exception {
		final String TEST_NAME = "test511AddProtectedAccountCaseIgnore";
		displayTestTitle(TEST_NAME);
		// GIVEN
		testAddAccount(TEST_NAME, "xaxa");
		testAddAccount(TEST_NAME, "somebody-ADM");
	}

	private PrismObject<ShadowType> createAccountShadow(String username) throws SchemaException {
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		ShadowType shadowType = new ShadowType();
		PrismTestUtil.getPrismContext().adopt(shadowType);
		shadowType.setName(PrismTestUtil.createPolyStringType(username));
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
		PrismObject<ShadowType> shadow = shadowType.asPrismObject();
		PrismContainer<Containerable> attrsCont = shadow.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
		PrismProperty<String> icfsNameProp = attrsCont.findOrCreateProperty(SchemaConstants.ICFS_NAME);
		icfsNameProp.setRealValue(username);
		return shadow;
	}

	protected void testAddProtectedAccount(final String TEST_NAME, String username) throws SchemaException, ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> shadow = createAccountShadow(username);

		// WHEN
		try {
			provisioningService.addObject(shadow, null, null, task, result);
			AssertJUnit.fail("Expected security exception while adding '"+username+"' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}

		result.computeStatus();
		display("addObject result (expected failure)", result);
		TestUtil.assertFailure(result);

		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();

		assertSteadyResource();
	}

	private void testAddAccount(final String TEST_NAME, String username) throws SchemaException, ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> shadow = createAccountShadow(username);

		// WHEN
		provisioningService.addObject(shadow, null, null, task, result);

		result.computeStatus();
		display("addObject result (expected failure)", result);
		TestUtil.assertSuccess(result);

		syncServiceMock.assertNotifySuccessOnly();

//		checkConsistency();

		assertSteadyResource();
	}

	@Test
	public void test600AddAccountAlreadyExist() throws Exception {
		final String TEST_NAME = "test600AddAccountAlreadyExist";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);
		syncServiceMock.reset();

		dummyResourceCtl.addAccount(ACCOUNT_MURRAY_USERNAME, ACCOUNT_MURRAY_USERNAME);

		PrismObject<ShadowType> account = createShadowNameOnly(resource, ACCOUNT_MURRAY_USERNAME);
		account.checkConsistence();

		display("Adding shadow", account);

		// WHEN
		try {
			provisioningService.addObject(account, null, null, task, result);

			AssertJUnit.fail("Unexpected success");
		} catch (ObjectAlreadyExistsException e) {
			// This is expected
			display("Expected exception", e);
		}

		// THEN
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertFailure(result);

		// Even though the operation failed a shadow should be created for the conflicting object

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername(getMurrayRepoIcfName(), resource, result);
		assertNotNull("Shadow was not created in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoAccountShadow(accountRepo);

		assertEquals("Wrong ICF NAME in murray (repo) shadow", getMurrayRepoIcfName(),  getIcfName(accountRepo));

		assertSteadyResource();
	}

	static Task syncTokenTask = null;

	@Test
	public void test800LiveSyncInit() throws Exception {
		final String TEST_NAME = "test800LiveSyncInit";
		displayTestTitle(TEST_NAME);
		syncTokenTask = taskManager.createTaskInstance(TestDummy.class.getName() + ".syncTask");

		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		syncServiceMock.reset();

		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test800LiveSyncInit");

		// Dry run to remember the current sync token in the task instance.
		// Otherwise a last sync token whould be used and
		// no change would be detected
		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		// No change, no fun
		syncServiceMock.assertNoNotifyChange();

		checkAllShadows();

		assertSteadyResource();
	}

	@Test
	public void test801LiveSyncAddBlackbeard() throws Exception {
		final String TEST_NAME = "test801LiveSyncAddBlackbeard";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount(BLACKBEARD_USERNAME);
		newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Edward Teach");
		newAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 66666);
		newAccount.setEnabled(true);
		newAccount.setPassword("shiverMEtimbers");
		dummyResource.addAccount(newAccount);
		blackbeardIcfUid = newAccount.getId();

		display("Resource before sync", dummyResource.debugDump());

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		TestUtil.assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		PrismObject<ShadowType> currentShadow = lastChange.getCurrentShadow();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadow.getClass().getName(),
				currentShadow.canRepresent(ShadowType.class));

		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(currentShadow);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertAttribute(currentShadow,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Edward Teach");
		assertAttribute(currentShadow,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 66666);
		assertEquals("Unexpected number of attributes", 4, attributes.size());

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername(getBlackbeardRepoIcfName(), resource, result);
		assertNotNull("Shadow was not created in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoAccountShadow(accountRepo);

		checkAllShadows();

		assertSteadyResource();
	}

	@Test
	public void test802LiveSyncModifyBlackbeard() throws Exception {
		final String TEST_NAME = "test802LiveSyncModifyBlackbeard";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		syncServiceMock.reset();

		DummyAccount dummyAccount = getDummyAccountAssert(BLACKBEARD_USERNAME, blackbeardIcfUid);
		dummyAccount.replaceAttributeValue("fullname", "Captain Blackbeard");

		display("Resource before sync", dummyResource.debugDump());

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		TestUtil.assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertSyncOldShadow(oldShadow, getBlackbeardRepoIcfName());

		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		PrismObject<ShadowType> currentShadow = lastChange.getCurrentShadow();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadow.getClass().getName(),
				currentShadow.canRepresent(ShadowType.class));

		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(currentShadow);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertAttribute(currentShadow,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Captain Blackbeard");
		assertAttribute(currentShadow,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 66666);
		assertEquals("Unexpected number of attributes", 4, attributes.size());

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername(getBlackbeardRepoIcfName(), resource, result);
		assertNotNull("Shadow was not created in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoAccountShadow(accountRepo);

		checkAllShadows();

		assertSteadyResource();
	}

	@Test
	public void test810LiveSyncAddDrakeDumbObjectClass() throws Exception {
		testLiveSyncAddDrake("test810LiveSyncAddDrakeDumbObjectClass", DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
	}

	@Test
	public void test812LiveSyncModifyDrakeDumbObjectClass() throws Exception {
		testLiveSyncModifyDrake("test812LiveSyncModifyDrakeDumbObjectClass", DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
	}

	@Test
	public void test815LiveSyncAddCorsairsDumbObjectClass() throws Exception {
		testLiveSyncAddCorsairs("test815LiveSyncAddCorsairsDumbObjectClass", DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
	}

	@Test
	public void test817LiveSyncDeleteCorsairsDumbObjectClass() throws Exception {
		testLiveSyncDeleteCorsairs("test817LiveSyncDeleteCorsairsDumbObjectClass", DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
	}

	@Test
	public void test819LiveSyncDeleteDrakeDumbObjectClass() throws Exception {
		testLiveSyncDeleteDrake("test819LiveSyncDeleteDrakeDumbObjectClass", DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
	}

	@Test
	public void test820LiveSyncAddDrakeSmartObjectClass() throws Exception {
		testLiveSyncAddDrake("test820LiveSyncAddDrakeDumbObjectClass", DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
	}

	@Test
	public void test822LiveSyncModifyDrakeSmartObjectClass() throws Exception {
		testLiveSyncModifyDrake("test822LiveSyncModifyDrakeDumbObjectClass", DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
	}

	@Test
	public void test825LiveSyncAddCorsairsSmartObjectClass() throws Exception {
		testLiveSyncAddCorsairs("test825LiveSyncAddCorsairsDumbObjectClass", DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
	}

	@Test
	public void test827LiveSyncDeleteCorsairsSmartObjectClass() throws Exception {
		testLiveSyncDeleteCorsairs("test827LiveSyncDeleteCorsairsDumbObjectClass", DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
	}

	@Test
	public void test829LiveSyncDeleteDrakeSmartObjectClass() throws Exception {
		testLiveSyncDeleteDrake("test829LiveSyncDeleteDrakeDumbObjectClass", DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
	}

	@Test
	public void test830LiveSyncAddDrakeDumbAny() throws Exception {
		testLiveSyncAddDrake("test830LiveSyncAddDrakeDumbAny", DummySyncStyle.DUMB, null);
	}

	@Test
	public void test832LiveSyncModifyDrakeDumbAny() throws Exception {
		testLiveSyncModifyDrake("test832LiveSyncModifyDrakeDumbAny", DummySyncStyle.DUMB, null);
	}

	@Test
	public void test835LiveSyncAddCorsairsDumbAny() throws Exception {
		testLiveSyncAddCorsairs("test835LiveSyncAddCorsairsDumbAny", DummySyncStyle.DUMB, null, true);
	}

	@Test
	public void test837LiveSyncDeleteCorsairsDumbAny() throws Exception {
		testLiveSyncDeleteCorsairs("test837LiveSyncDeleteCorsairsDumbAny", DummySyncStyle.DUMB, null, true);
	}

	@Test
	public void test839LiveSyncDeleteDrakeDumbAny() throws Exception {
		testLiveSyncDeleteDrake("test839LiveSyncDeleteDrakeDumbAny", DummySyncStyle.DUMB, null);
	}

	@Test
	public void test840LiveSyncAddDrakeSmartAny() throws Exception {
		testLiveSyncAddDrake("test840LiveSyncAddDrakeSmartAny", DummySyncStyle.SMART, null);
	}

	@Test
	public void test842LiveSyncModifyDrakeSmartAny() throws Exception {
		testLiveSyncModifyDrake("test842LiveSyncModifyDrakeSmartAny", DummySyncStyle.SMART, null);
	}

	@Test
	public void test845LiveSyncAddCorsairsSmartAny() throws Exception {
		testLiveSyncAddCorsairs("test845LiveSyncAddCorsairsSmartAny", DummySyncStyle.SMART, null, true);
	}

	@Test
	public void test847LiveSyncDeleteCorsairsSmartAny() throws Exception {
		testLiveSyncDeleteCorsairs("test847LiveSyncDeleteCorsairsSmartAny", DummySyncStyle.SMART, null, true);
	}

	@Test
	public void test849LiveSyncDeleteDrakeSmartAny() throws Exception {
		testLiveSyncDeleteDrake("test849LiveSyncDeleteDrakeSmartAny", DummySyncStyle.SMART, null);
	}

	public void testLiveSyncAddDrake(final String TEST_NAME, DummySyncStyle syncStyle, QName objectClass) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		syncServiceMock.reset();
		dummyResource.setSyncStyle(syncStyle);
		DummyAccount newAccount = new DummyAccount(DRAKE_USERNAME);
		newAccount.addAttributeValues("fullname", "Sir Francis Drake");
		newAccount.setEnabled(true);
		newAccount.setPassword("avast!");
		dummyResource.addAccount(newAccount);
		drakeIcfUid = newAccount.getId();

		display("Resource before sync", dummyResource.debugDump());

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				objectClass);

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("Synchronization result", result);
		TestUtil.assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

		if (syncStyle == DummySyncStyle.DUMB) {
			assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		} else {
			ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
			assertNotNull("Delta present when not expecting it", objectDelta);
			assertTrue("Delta is not add: "+objectDelta, objectDelta.isAdd());
		}

		ShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		PrismAsserts.assertClass("current shadow", ShadowType.class, currentShadowType);

		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Sir Francis Drake",
				fullnameAttribute.getRealValue());

		drakeAccountOid = currentShadowType.getOid();
		PrismObject<ShadowType> repoShadow = getShadowRepo(drakeAccountOid);
		display("Drake repo shadow", repoShadow);

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername(getDrakeRepoIcfName(), resource, result);
		assertNotNull("Shadow was not created in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoAccountShadow(accountRepo);

		checkAllShadows();

		assertSteadyResource();
	}

	public void testLiveSyncModifyDrake(final String TEST_NAME, DummySyncStyle syncStyle, QName objectClass) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		syncServiceMock.reset();
		dummyResource.setSyncStyle(syncStyle);

		DummyAccount dummyAccount = getDummyAccountAssert(DRAKE_USERNAME, drakeIcfUid);
		dummyAccount.replaceAttributeValue("fullname", "Captain Drake");

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				objectClass);

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("Synchronization result", result);
		TestUtil.assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertSyncOldShadow(oldShadow, getDrakeRepoIcfName());

		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		PrismObject<ShadowType> currentShadow = lastChange.getCurrentShadow();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadow.getClass().getName(),
				currentShadow.canRepresent(ShadowType.class));

		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(currentShadow);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertAttribute(currentShadow,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Captain Drake");
		assertEquals("Unexpected number of attributes", 3, attributes.size());

		PrismObject<ShadowType> accountRepo = findAccountShadowByUsername(getDrakeRepoIcfName(), resource, result);
		assertNotNull("Shadow was not created in the repository", accountRepo);
		display("Repository shadow", accountRepo);
		checkRepoAccountShadow(accountRepo);

		checkAllShadows();

		assertSteadyResource();
	}

	public void testLiveSyncAddCorsairs(final String TEST_NAME, DummySyncStyle syncStyle, QName objectClass, boolean expectReaction) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		syncServiceMock.reset();
		dummyResource.setSyncStyle(syncStyle);
		DummyGroup newGroup = new DummyGroup(GROUP_CORSAIRS_NAME);
		newGroup.setEnabled(true);
		dummyResource.addGroup(newGroup);
		corsairsIcfUid = newGroup.getId();

		display("Resource before sync", dummyResource.debugDump());

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				objectClass);

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("Synchronization result", result);
		TestUtil.assertSuccess("Synchronization result is not OK", result);

		if (expectReaction) {

			syncServiceMock.assertNotifyChange();

			ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
			display("The change", lastChange);

			PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
			assertNotNull("Old shadow missing", oldShadow);
			assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

			if (syncStyle == DummySyncStyle.DUMB) {
				assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
			} else {
				ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
				assertNotNull("Delta present when not expecting it", objectDelta);
				assertTrue("Delta is not add: "+objectDelta, objectDelta.isAdd());
			}

			ShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
			assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
			PrismAsserts.assertClass("current shadow", ShadowType.class, currentShadowType);

			ResourceAttributeContainer attributesContainer = ShadowUtil
					.getAttributesContainer(currentShadowType);
			assertNotNull("No attributes container in current shadow", attributesContainer);
			Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
			assertFalse("Attributes container is empty", attributes.isEmpty());
			assertEquals("Unexpected number of attributes", 2, attributes.size());

			corsairsShadowOid = currentShadowType.getOid();
			PrismObject<ShadowType> repoShadow = getShadowRepo(corsairsShadowOid);
			display("Corsairs repo shadow", repoShadow);

			PrismObject<ShadowType> accountRepo = findShadowByName(new QName(RESOURCE_DUMMY_NS, SchemaConstants.GROUP_OBJECT_CLASS_LOCAL_NAME), GROUP_CORSAIRS_NAME, resource, result);
			assertNotNull("Shadow was not created in the repository", accountRepo);
			display("Repository shadow", accountRepo);
			ProvisioningTestUtil.checkRepoShadow(repoShadow, ShadowKindType.ENTITLEMENT);

		} else {
			syncServiceMock.assertNoNotifyChange();
		}

		checkAllShadows();

		assertSteadyResource();
	}

	public void testLiveSyncDeleteCorsairs(final String TEST_NAME, DummySyncStyle syncStyle, QName objectClass, boolean expectReaction) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		syncServiceMock.reset();
		dummyResource.setSyncStyle(syncStyle);
		if (isNameUnique()) {
			dummyResource.deleteGroupByName(GROUP_CORSAIRS_NAME);
		} else {
			dummyResource.deleteGroupById(corsairsIcfUid);
		}

		display("Resource before sync", dummyResource.debugDump());

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				objectClass);

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("Synchronization result", result);
		TestUtil.assertSuccess("Synchronization result is not OK", result);


		if (expectReaction) {

			syncServiceMock.assertNotifyChange();

			ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
			display("The change", lastChange);

			PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
			assertNotNull("Old shadow missing", oldShadow);
			assertNotNull("Old shadow does not have an OID", oldShadow.getOid());
			PrismAsserts.assertClass("old shadow", ShadowType.class, oldShadow);
			ShadowType oldShadowType = oldShadow.asObjectable();
			ResourceAttributeContainer attributesContainer = ShadowUtil
					.getAttributesContainer(oldShadowType);
			assertNotNull("No attributes container in old shadow", attributesContainer);
			Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
			assertFalse("Attributes container is empty", attributes.isEmpty());
			assertEquals("Unexpected number of attributes", 2, attributes.size());
			ResourceAttribute<?> icfsNameAttribute = attributesContainer.findAttribute(SchemaConstants.ICFS_NAME);
			assertNotNull("No ICF name attribute in old  shadow", icfsNameAttribute);
			assertEquals("Wrong value of ICF name attribute in old  shadow", GROUP_CORSAIRS_NAME,
					icfsNameAttribute.getRealValue());

			ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
			assertNotNull("Delta missing", objectDelta);
			assertEquals("Wrong delta changetype", ChangeType.DELETE, objectDelta.getChangeType());
			PrismAsserts.assertClass("delta", ShadowType.class, objectDelta);
			assertNotNull("No OID in delta", objectDelta.getOid());

			assertNull("Unexpected current shadow",lastChange.getCurrentShadow());

			try {
				// The shadow should be gone
				PrismObject<ShadowType> repoShadow = getShadowRepo(corsairsShadowOid);

				AssertJUnit.fail("The shadow "+repoShadow+" is not gone from repo");
			} catch (ObjectNotFoundException e) {
				// This is expected
			}

		} else {
			syncServiceMock.assertNoNotifyChange();
		}

		checkAllShadows();

		assertSteadyResource();
	}

	public void testLiveSyncDeleteDrake(final String TEST_NAME, DummySyncStyle syncStyle, QName objectClass) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		syncServiceMock.reset();
		dummyResource.setSyncStyle(syncStyle);
		if (isNameUnique()) {
			dummyResource.deleteAccountByName(DRAKE_USERNAME);
		} else {
			dummyResource.deleteAccountById(drakeIcfUid);
		}

		display("Resource before sync", dummyResource.debugDump());

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				objectClass);

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		display("Synchronization result", result);
		TestUtil.assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertSyncOldShadow(oldShadow, getDrakeRepoIcfName());

		ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
		assertNotNull("Delta missing", objectDelta);
		assertEquals("Wrong delta changetype", ChangeType.DELETE, objectDelta.getChangeType());
		PrismAsserts.assertClass("delta", ShadowType.class, objectDelta);
		assertNotNull("No OID in delta", objectDelta.getOid());

		assertNull("Unexpected current shadow",lastChange.getCurrentShadow());

		try {
			// The shadow should be gone
			PrismObject<ShadowType> repoShadow = getShadowRepo(drakeAccountOid);

			AssertJUnit.fail("The shadow "+repoShadow+" is not gone from repo");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		checkAllShadows();

		assertSteadyResource();
	}

	@Test
	public void test890LiveSyncModifyProtectedAccount() throws Exception {
		final String TEST_NAME = "test890LiveSyncModifyProtectedAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = syncTask.getResult();

		syncServiceMock.reset();

		DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_DAEMON_USERNAME, daemonIcfUid);
		dummyAccount.replaceAttributeValue("fullname", "Maxwell deamon");

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
				ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.synchronize(coords, syncTokenTask, result);

		// THEN
		displayThen(TEST_NAME);
		display("Synchronization result", result);
		assertSuccess(result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		syncServiceMock.assertNoNotifyChange();

		checkAllShadows();

		assertSteadyResource();
	}

	@Test
	public void test901FailResourceNotFound() throws Exception {
		final String TEST_NAME = "test901FailResourceNotFound";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		try {
			PrismObject<ResourceType> object = provisioningService.getObject(ResourceType.class, NOT_PRESENT_OID, null, null,
					result);
			AssertJUnit.fail("Expected ObjectNotFoundException to be thrown, but getObject returned " + object
					+ " instead");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		result.computeStatus();
		display("getObject result (expected failure)", result);
		TestUtil.assertFailure(result);

		assertSteadyResource();
	}

	// test999 shutdown in the superclass

	protected void checkCachedAccountShadow(PrismObject<ShadowType> shadowType, OperationResult parentResult, boolean fullShadow, XMLGregorianCalendar startTs,
			XMLGregorianCalendar endTs) throws SchemaException {
		checkAccountShadow(shadowType, parentResult, fullShadow, startTs, endTs);
	}

	private void checkGroupShadow(PrismObject<ShadowType> shadow, OperationResult parentResult) throws SchemaException {
		checkEntitlementShadow(shadow, parentResult, SchemaTestConstants.ICF_GROUP_OBJECT_CLASS_LOCAL_NAME, true);
	}

	private void checkGroupShadow(PrismObject<ShadowType> shadow, OperationResult parentResult, boolean fullShadow) throws SchemaException {
		checkEntitlementShadow(shadow, parentResult, SchemaTestConstants.ICF_GROUP_OBJECT_CLASS_LOCAL_NAME, fullShadow);
	}

	private void checkEntitlementShadow(PrismObject<ShadowType> shadow, OperationResult parentResult, String objectClassLocalName, boolean fullShadow) throws SchemaException {
		ObjectChecker<ShadowType> checker = createShadowChecker(fullShadow);
		ShadowUtil.checkConsistence(shadow, parentResult.getOperation());
		IntegrationTestTools.checkEntitlementShadow(shadow.asObjectable(), resourceType, repositoryService, checker, objectClassLocalName, getUidMatchingRule(), prismContext, parentResult);
	}

	private void checkAllShadows() throws SchemaException, ObjectNotFoundException, CommunicationException,
			ConfigurationException {
		ObjectChecker<ShadowType> checker = null;
		IntegrationTestTools.checkAllShadows(resourceType, repositoryService, checker, prismContext);
	}



	protected void checkRepoEntitlementShadow(PrismObject<ShadowType> repoShadow) {
		ProvisioningTestUtil.checkRepoEntitlementShadow(repoShadow);
	}

	protected void assertSyncOldShadow(PrismObject<? extends ShadowType> oldShadow, String repoName) {
		assertSyncOldShadow(oldShadow, repoName, 2);
	}

	protected void assertSyncOldShadow(PrismObject<? extends ShadowType> oldShadow, String repoName, Integer expectedNumberOfAttributes) {
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());
		PrismAsserts.assertClass("old shadow", ShadowType.class, oldShadow);
		ShadowType oldShadowType = oldShadow.asObjectable();
		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(oldShadowType);
		assertNotNull("No attributes container in old shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		if (expectedNumberOfAttributes != null) {
			assertEquals("Unexpected number of attributes", (int)expectedNumberOfAttributes, attributes.size());
		}
		ResourceAttribute<?> icfsNameAttribute = attributesContainer.findAttribute(SchemaConstants.ICFS_NAME);
		assertNotNull("No ICF name attribute in old  shadow", icfsNameAttribute);
		assertEquals("Wrong value of ICF name attribute in old  shadow", repoName,
				icfsNameAttribute.getRealValue());
	}

}
