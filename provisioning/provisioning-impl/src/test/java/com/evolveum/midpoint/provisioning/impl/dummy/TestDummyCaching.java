/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Almost the same as TestDummy but this is using a caching configuration.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyCaching extends TestDummy {

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-caching");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}

	/**
	 * Make a native modification to an account and read it from the cache. Make sure that
	 * cached data are returned and there is no read from the resource.
	 * MID-3481
	 */
	@Test
	@Override
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
		TestUtil.displayWhen(TEST_NAME);

		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 7, attributes.size());

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.DISABLED);

		checkConsistency(shadow);

		assertCachingMetadata(shadow, true, null, startTs);

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		assertSteadyResource();
	}

	/**
	 * Make a native modification to an account and read it with high staleness option.
	 * This should return cached data.
	 * MID-3481
	 */
	@Test
	@Override
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
		TestUtil.displayWhen(TEST_NAME);

		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 7, attributes.size());

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.DISABLED);

		checkConsistency(shadow);

		assertCachingMetadata(shadow, true, null, startTs);

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		assertSteadyResource();
	}

	/**
	 * Search for all accounts with maximum staleness option.
	 * This is supposed to return only cached data. Therefore
	 * repo search is performed.
	 * MID-3481
	 */
	@Test
	@Override
	public void test119SearchAllAccountsMaxStaleness() throws Exception {
		final String TEST_NAME = "test119SearchAllAccountsMaxStaleness";
		displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(GetOperationOptions.createMaxStaleness());

		// WHEN
		List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
				query, options, null, result);

		// THEN
		display("searchObjects result", result);
		assertSuccess(result);

		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 4, allShadows.size());

		for (PrismObject<ShadowType> shadow: allShadows) {
			display("Found shadow", shadow);
			ShadowType shadowType = shadow.asObjectable();
			OperationResultType fetchResult = shadowType.getFetchResult();
			if (fetchResult != null) {
				display("fetchResult",fetchResult);
				assertEquals("Wrong fetch result status in "+shadow, OperationResultStatusType.SUCCESS, fetchResult.getStatus());
			}
			assertCachingMetadata(shadow, true, null, startTs);

			if (shadow.asObjectable().getName().getOrig().equals("meathook")) {
				assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Sea Monkey");
			}
		}

		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		assertProtected(allShadows, 1);

		assertSteadyResource();
	}

	@Override
	protected void checkRepoAccountShadowWill(PrismObject<ShadowType> shadowRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		// Sometimes there are 6 and sometimes 7 attributes. Treasure is not returned by default. It is not normally in the cache.
		// So do not check for number of attributes here. Check for individual values.
		checkRepoAccountShadowWillBasic(shadowRepo, start, end, null);

		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
		// this is shadow, values are normalized
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);

		assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.ENABLED);
	}

	@Override
	protected void assertRepoShadowCacheActivation(PrismObject<ShadowType> shadowRepo, ActivationStatusType expectedAdministrativeStatus) {
		ActivationType activationType = shadowRepo.asObjectable().getActivation();
		assertNotNull("No activation in repo shadow "+shadowRepo, activationType);
		ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
		assertEquals("Wrong activation administrativeStatus in repo shadow "+shadowRepo, expectedAdministrativeStatus, administrativeStatus);
	}

	@Override
	protected void assertRepoShadowPasswordValue(PrismObject<ShadowType> shadowRepo, PasswordType passwordType,
			String expectedPassword) throws SchemaException, EncryptionException {
		ProtectedStringType protectedStringType = passwordType.getValue();
		assertNotNull("No password value in repo shadow "+shadowRepo, protectedStringType);
		assertProtectedString("Wrong password value in repo shadow "+shadowRepo, expectedPassword, protectedStringType, CredentialsStorageTypeType.HASHING);
	}
	
	/**
	 * We do not know what the timestamp should be. But some timestamp should be there.
	 */
	@Override
	protected void assertRepoCachingMetadata(PrismObject<ShadowType> shadowFromRepo) {
		CachingMetadataType cachingMetadata = shadowFromRepo.asObjectable().getCachingMetadata();
		assertNotNull("No caching metadata in "+shadowFromRepo, cachingMetadata);

		assertNotNull("Missing retrieval timestamp in caching metadata in "+shadowFromRepo,
				cachingMetadata.getRetrievalTimestamp());
	}

	@Override
	protected void assertRepoCachingMetadata(PrismObject<ShadowType> shadowFromRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		CachingMetadataType cachingMetadata = shadowFromRepo.asObjectable().getCachingMetadata();
		assertNotNull("No caching metadata in "+shadowFromRepo, cachingMetadata);

		TestUtil.assertBetween("Wrong retrieval timestamp in caching metadata in "+shadowFromRepo,
				start, end, cachingMetadata.getRetrievalTimestamp());
	}

	@Override
	protected void assertCachingMetadata(PrismObject<ShadowType> shadow, boolean expectedCached, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
		CachingMetadataType cachingMetadata = shadow.asObjectable().getCachingMetadata();
		if (expectedCached) {
			assertNotNull("No caching metadata in "+shadow, cachingMetadata);
			TestUtil.assertBetween("Wrong retrievalTimestamp in caching metadata in "+shadow, startTs, endTs, cachingMetadata.getRetrievalTimestamp());
		} else {
			super.assertCachingMetadata(shadow, expectedCached, startTs, endTs);
		}
	}


	@Override
	protected void checkRepoAccountShadow(PrismObject<ShadowType> repoShadow) {
		ProvisioningTestUtil.checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT, null);
	}

	@Override
	protected void checkRepoEntitlementShadow(PrismObject<ShadowType> repoShadow) {
		ProvisioningTestUtil.checkRepoShadow(repoShadow, ShadowKindType.ENTITLEMENT, null);
	}

	@Override
	protected void assertRepoShadowAttributes(List<Item<?,?>> attributes, int expectedNumberOfIdentifiers) {
		// We can only assert that there are at least the identifiers. But we do not know how many attributes should be there
		assertTrue("Unexpected number of attributes in repo shadow, expected at least "+
		expectedNumberOfIdentifiers+", but was "+attributes.size(), attributes.size() >= expectedNumberOfIdentifiers);
	}

	@Override
	protected void assertSyncOldShadow(PrismObject<? extends ShadowType> oldShadow, String repoName) {
		assertSyncOldShadow(oldShadow, repoName, null);
	}

	@Override
	protected <T> void assertRepoShadowCachedAttributeValue(PrismObject<ShadowType> shadowRepo, String attrName, T... attrValues) {
		assertAttribute(shadowRepo, attrName, attrValues);
	}

	@Override
	protected void checkCachedAccountShadow(PrismObject<ShadowType> shadow, OperationResult parentResult, boolean fullShadow, XMLGregorianCalendar startTs,
			XMLGregorianCalendar endTs) throws SchemaException {
		super.checkAccountShadow(shadow, parentResult, fullShadow, startTs, endTs);
		if (fullShadow) {
			assertCachingMetadata(shadow, true, startTs, endTs);
		}
	}
}
