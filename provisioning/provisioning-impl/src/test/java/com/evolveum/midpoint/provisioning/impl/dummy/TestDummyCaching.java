/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Almost the same as TestDummy but this is using a caching configuration.
 *
 * @author Radovan Semancik
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
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected ItemComparisonResult getExpectedPasswordComparisonResultMatch() {
        return ItemComparisonResult.MATCH;
    }

    @Override
    protected ItemComparisonResult getExpectedPasswordComparisonResultMismatch() {
        return ItemComparisonResult.MISMATCH;
    }

    protected boolean isWeaponIndexOnly() {
        return false;
    }

    private boolean isIndexOnly(String attrName) {
        return isWeaponIndexOnly() && DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME.equals(attrName);
    }

    /**
     * Make a native modification to an account and read it from the cache. Make sure that
     * cached data are returned and there is no read from the resource.
     * MID-3481
     */
    @Test
    @Override
    public void test107AGetModifiedAccountFromCacheMax() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        DummyAccount accountWill = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Nice Pirate");
        accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Interceptor");
        accountWill.setEnabled(true);

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createMaxStaleness());

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);

        // THEN
        then();
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertRepoShadowCachedAttributeValue(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertEquals("Unexpected number of attributes", 7, attributes.size());

        PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
        checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertRepoShadowCachedAttributeValue(shadowRepo, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.DISABLED);

        checkUniqueness(shadow);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        DummyAccount accountWill = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
        accountWill.setEnabled(true);

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createStaleness(1000000L));

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertRepoShadowCachedAttributeValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertEquals("Unexpected number of attributes", 7, attributes.size());

        PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
        checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertRepoShadowCachedAttributeValue(shadowRepo, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.DISABLED);

        checkUniqueness(shadow);

        assertCachingMetadata(shadow, true, null, startTs);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertSteadyResource();
    }

    /**
     * Incomplete attributes should not be cached.
     */
    @Test
    public void test107CSkipCachingForIncompleteAttributes() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        DummyAccount accountWill = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
        accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, null);
        accountWill.getAttributeDefinition(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME).setReturnedAsIncomplete(true);
        accountWill.setEnabled(true);

        try {
            XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

            // WHEN
            when();

            PrismObject<ShadowType> shadow = provisioningService
                    .getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

            // THEN
            then();
            assertSuccess(result);

            assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

            display("Retrieved account shadow", shadow);

            assertNotNull("No dummy account", shadow);

            assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
            assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
            Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
            assertEquals("Unexpected number of attributes", 7, attributes.size());

            PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
            checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

            assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                    "Very Nice Pirate");
            assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
                    "Black Pearl");
            assertRepoShadowCachedAttributeValue(shadowRepo, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword",
                    "love");
            assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
            assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.ENABLED);

            checkUniqueness(shadow);

            assertCachingMetadata(shadow, false, null, startTs);

            assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

            assertSteadyResource();
        } finally {
            // cleanup the state to allow other tests to pass
            accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Interceptor");
            accountWill.getAttributeDefinition(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME).setReturnedAsIncomplete(false);
        }
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
        given();
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceBean,
                SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
        displayDumpable("All shadows query", query);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createMaxStaleness());

        when();
        List<PrismObject<ShadowType>> allShadows =
                provisioningService.searchObjects(ShadowType.class, query, options, task, result);

        then();
        display("searchObjects result", result);
        assertSuccess(result);

        display("Found " + allShadows.size() + " shadows");

        assertFalse("No shadows found", allShadows.isEmpty());
        assertEquals("Wrong number of results", 4, allShadows.size());

        for (PrismObject<ShadowType> shadow : allShadows) {
            display("Found shadow", shadow);
            ShadowType shadowType = shadow.asObjectable();
            OperationResultType fetchResult = shadowType.getFetchResult();
            if (fetchResult != null) {
                display("fetchResult", fetchResult);
                assertEquals("Wrong fetch result status in " + shadow, OperationResultStatusType.SUCCESS, fetchResult.getStatus());
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

    /**
     * Checks whether multivalued attributes are correctly cached, especially when deleting values.
     * See MID-7162.
     */
    @Test
    public void test910CacheMultivaluedAttribute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount will = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);

        updateAndCheckMultivaluedAttribute(will, false, "initial values", Arrays.asList("sword", "love"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "replacing by w1-w3", Arrays.asList("w1", "w2", "w3"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "adding w4", Arrays.asList("w1", "w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "removing w1", Arrays.asList("w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "removing all", Collections.emptyList(), task, result);

        updateAndCheckMultivaluedAttribute(will, true, "adding w1-w3", Arrays.asList("w1", "w2", "w3"), task, result);
        updateAndCheckMultivaluedAttribute(will, true, "adding w4", Arrays.asList("w1", "w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, true, "removing w1", Arrays.asList("w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, true, "removing all", Collections.emptyList(), task, result);
    }

    private void updateAndCheckMultivaluedAttribute(DummyAccount account, boolean useSearch, String messagePrefix,
            Collection<Object> values, Task task, OperationResult result) throws Exception {

        String message = messagePrefix + (useSearch ? " (using search)" : " (using get)");
        when(message);

        account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, values);

        if (useSearch) {
            ResourceAttributeDefinition<?> nameDef = getAccountAttrDef(SchemaConstants.ICFS_NAME);

            // @formatter:off
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(RI_ACCOUNT_OBJECT_CLASS)
                    .and().itemWithDef(nameDef, ShadowType.F_ATTRIBUTES, nameDef.getItemName())
                        .eq(transformNameFromResource(ACCOUNT_WILL_USERNAME))
                    .build();
            // @formatter:on

            SearchResultList<PrismObject<ShadowType>> objects
                    = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
            assertThat(objects).as("accounts matching will").hasSize(1);

        } else {
            provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        }

        then(message);

        PrismObject<ShadowType> shadow = getShadowRepoRetrieveAllAttributes(ACCOUNT_WILL_OID, result);
        assertAttribute(shadow, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, values.toArray());
    }

    @Override
    protected void checkRepoAccountShadowWill(
            PrismObject<ShadowType> shadowRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) throws CommonException {
        // Sometimes there are 6 and sometimes 7 attributes. Treasure is not returned by default. It is not normally in the cache.
        // So do not check for number of attributes here. Check for individual values.
        checkRepoAccountShadowWillBasic(shadowRepo, start, end, null);

        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
        if (isWeaponIndexOnly()) {
            assertRepoShadowCachedAttributeValue(shadowRepo, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
        } else {
            // this is shadow, values are normalized
            assertRepoShadowCachedAttributeValue(shadowRepo, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
        }
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);

        assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.ENABLED);
    }

    @Override
    protected void assertRepoShadowCacheActivation(PrismObject<ShadowType> shadowRepo, ActivationStatusType expectedAdministrativeStatus) {
        ActivationType activationType = shadowRepo.asObjectable().getActivation();
        assertNotNull("No activation in repo shadow " + shadowRepo, activationType);
        ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
        assertEquals("Wrong activation administrativeStatus in repo shadow " + shadowRepo, expectedAdministrativeStatus, administrativeStatus);
    }

    @Override
    protected void assertRepoShadowPasswordValue(PrismObject<ShadowType> shadowRepo, PasswordType passwordType,
            String expectedPassword) throws SchemaException, EncryptionException {
        ProtectedStringType protectedStringType = passwordType.getValue();
        assertNotNull("No password value in repo shadow " + shadowRepo, protectedStringType);
        assertProtectedString("Wrong password value in repo shadow " + shadowRepo, expectedPassword, protectedStringType, CredentialsStorageTypeType.HASHING);
    }

    @Override
    protected void assertRepoCachingMetadata(PrismObject<ShadowType> shadowFromRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        CachingMetadataType cachingMetadata = shadowFromRepo.asObjectable().getCachingMetadata();
        assertNotNull("No caching metadata in " + shadowFromRepo, cachingMetadata);

        TestUtil.assertBetween("retrieval timestamp in caching metadata in " + shadowFromRepo,
                start, end, cachingMetadata.getRetrievalTimestamp());
    }

    @Override
    protected void assertCachingMetadata(PrismObject<ShadowType> shadow, boolean expectedCached, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        CachingMetadataType cachingMetadata = shadow.asObjectable().getCachingMetadata();
        if (expectedCached) {
            assertNotNull("No caching metadata in " + shadow, cachingMetadata);
            TestUtil.assertBetween("retrievalTimestamp in caching metadata in " + shadow, startTs, endTs, cachingMetadata.getRetrievalTimestamp());
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
    protected void assertRepoShadowAttributes(Collection<Item<?, ?>> attributes, int expectedNumberOfIdentifiers) {
        // We can only assert that there are at least the identifiers. But we do not know how many attributes should be there
        assertTrue("Unexpected number of attributes in repo shadow, expected at least " +
                expectedNumberOfIdentifiers + ", but was " + attributes.size(), attributes.size() >= expectedNumberOfIdentifiers);
    }

    @Override
    protected void assertSyncOldShadow(PrismObject<? extends ShadowType> oldShadow, String repoName) {
        assertSyncOldShadow(oldShadow, repoName, null);
    }

    @Override
    protected void assertRepoShadowCachedAttributeValue(
            PrismObject<ShadowType> shadowRepo, String attrName, Object... attrValues) {
        Object[] reallyExpectedValue = isIndexOnly(attrName) ? new Object[0] : attrValues;
        assertAttribute(shadowRepo, attrName, reallyExpectedValue);
    }

    @Override
    protected void checkCachedAccountShadow(
            PrismObject<ShadowType> shadow, OperationResult parentResult, boolean fullShadow,
            XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) throws SchemaException, ConfigurationException {
        super.checkAccountShadow(shadow, parentResult, fullShadow);
        if (fullShadow) {
            assertCachingMetadata(shadow, true, startTs, endTs);
        }
    }
}
