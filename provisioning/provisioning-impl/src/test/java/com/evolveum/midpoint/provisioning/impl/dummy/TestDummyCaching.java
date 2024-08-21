/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.test.DummyResourceContoller.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.RawRepoShadow;

import com.evolveum.midpoint.schema.util.Resource;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
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

    private boolean isAttrCachedInFullObject(String attrName) {
        return isAttrCached(attrName) && !isAttrIndexOnly(attrName);
    }

    boolean isAttrCached(String attrName) {
        return true;
    }

    private boolean isAttrIndexOnly(String attrName) {
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

        DummyAccount accountWill = getDummyAccountAssert(getWillNameOnResource(), willIcfUid);
        accountWill.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Nice Pirate");
        accountWill.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Interceptor");
        accountWill.setEnabled(true);

        var options = SelectorOptions.createCollection(GetOperationOptions.createMaxStaleness());

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        var shadow = provisioningService.getShadow(ACCOUNT_WILL_OID, options, task, result);

        // THEN
        then();
        assertSuccessVerbose(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42L);

        var repoShadow = assertRepoShadowNew(ACCOUNT_WILL_OID)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate")
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl")
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE")
                .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love")
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42L)
                .getRawRepoShadow();
        assertRepoShadowCacheActivation(repoShadow, ActivationStatusType.DISABLED);

        checkRepoAccountShadowWillBasic(repoShadow, null, startTs, false, null);

        checkUniqueness(shadow);

        assertCachingMetadata(shadow.getBean(), null, startTs);

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

        DummyAccount accountWill = getDummyAccountAssert(getWillNameOnResource(), willIcfUid);
        accountWill.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
        accountWill.setEnabled(true);

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createStaleness(1000000L));

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        var shadow = provisioningService.getShadow(ACCOUNT_WILL_OID, options, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
        assertOptionalAttrValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42L);
//        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        //TODO assertEquals("Unexpected number of attributes", 7, attributes.size());

        var repoShadow = assertRepoShadowNew(ACCOUNT_WILL_OID)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate")
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl")
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE")
                .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love")
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42L)
                .getRawRepoShadow();
        checkRepoAccountShadowWillBasic(repoShadow, null, startTs, false, null);
        assertRepoShadowCacheActivation(repoShadow, ActivationStatusType.DISABLED);

        checkUniqueness(shadow);

        assertCachingMetadata(shadow.getBean(), null, startTs);

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

        DummyAccount accountWill = getDummyAccountAssert(getWillNameOnResource(), willIcfUid);
        accountWill.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
        accountWill.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, null);
        accountWill.getAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME).setReturnedAsIncomplete(true);
        accountWill.setEnabled(true);

        try {
            // WHEN
            when();

            var startTs = clock.currentTimeXMLGregorianCalendar();
            var shadow = provisioningService.getShadow(ACCOUNT_WILL_OID, null, task, result);
            var endTs = clock.currentTimeXMLGregorianCalendar();

            // THEN
            then();
            assertSuccess(result);

            assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

            display("Retrieved account shadow", shadow);

            assertNotNull("No dummy account", shadow);

            assertAttribute(shadow, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate");
            assertAttribute(shadow, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
            Collection<ShadowSimpleAttribute<?>> attributes = shadow.getSimpleAttributes();
            assertEquals("Unexpected number of attributes", 7, attributes.size());

            var shadowRepo = assertRepoShadowNew(ACCOUNT_WILL_OID)
                    .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Very Nice Pirate")
                    .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl")
                    .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love")
                    .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42L)
                    .getRawRepoShadow();
            assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.ENABLED);

            //TODO why this fails? checkRepoAccountShadowWillBasic(shadowRepo, null, startTs, null);

            checkUniqueness(shadow);

            if (getCachedAccountAttributes().contains(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME)) {
                // Ship is cached, so we won't update the cached data if it's incomplete
                assertCachingMetadata(shadow.getBean(), null, startTs);
            } else {
                // Ship is not cached, so we can update the cached data even if it's incomplete
                assertCachingMetadata(shadow.getBean(), startTs, endTs);
            }

            assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

            assertSteadyResource();
        } finally {
            // cleanup the state to allow other tests to pass
            accountWill.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Interceptor");
            accountWill.getAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME).setReturnedAsIncomplete(false);
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
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceBean, RI_ACCOUNT_OBJECT_CLASS);
        displayDumpable("All shadows query", query);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createMaxStaleness());

        when();
        List<? extends AbstractShadow> allShadows = provisioningService.searchShadows(query, options, task, result);

        then();
        display("searchObjects result", result);
        assertSuccess(result);

        display("Found " + allShadows.size() + " shadows");

        assertFalse("No shadows found", allShadows.isEmpty());
        assertEquals("Wrong number of results", 4, allShadows.size());

        for (AbstractShadow shadow : allShadows) {
            displayDumpable("Found shadow", shadow);
            OperationResultType fetchResult = shadow.getBean().getFetchResult();
            if (fetchResult != null) {
                display("fetchResult", fetchResult);
                assertEquals("Wrong fetch result status in " + shadow, OperationResultStatusType.SUCCESS, fetchResult.getStatus());
            }
            assertCachingMetadata(shadow.getBean(), null, startTs);

            if (shadow.getName().getOrig().equals("meathook")) {
                assertCachedAttributeValue(shadow, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Sea Monkey");
            }
        }

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertProtected(allShadows, 1);

        assertSteadyResource();
    }


    @Test
    public void test700SearchUsingAssociations() throws Exception {
        skipIfNotNativeRepository();

        given();

        var task = getTestTask();
        var result = task.getResult();
        var options = GetOperationOptionsBuilder.create().noFetch().build();
        // associations/contract/objects/org/@/name = "Law"
        var path = PrismContext.get().itemPathParser().asItemPath("associations/group/objects/group/@/name");
        when("Searching for john using associations " + path.toString());
        var query = Resource.of(resource)
                .queryFor(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "default"))
                .and().item(path).eq("fools")
                .build();
        var objects = provisioningService.searchObjects(ShadowType.class, query, options, task, result);
        then("Will should be found.");
        assertThat(objects).hasSize(1);
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

        DummyAccount will = getDummyAccountAssert(getWillNameOnResource(), willIcfUid);

        updateAndCheckMultivaluedAttribute(will, false, "initial values", List.of("sword", "love"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "replacing by w1-w3", List.of("w1", "w2", "w3"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "adding w4", List.of("w1", "w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "removing w1", List.of("w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, false, "removing all", List.of(), task, result);

        updateAndCheckMultivaluedAttribute(will, true, "adding w1-w3", List.of("w1", "w2", "w3"), task, result);
        updateAndCheckMultivaluedAttribute(will, true, "adding w4", List.of("w1", "w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, true, "removing w1", List.of("w2", "w3", "w4"), task, result);
        updateAndCheckMultivaluedAttribute(will, true, "removing all", List.of(), task, result);
    }

    private void updateAndCheckMultivaluedAttribute(
            DummyAccount account, boolean useSearch, String messagePrefix,
            Collection<Object> values, Task task, OperationResult result) throws Exception {

        String message = messagePrefix + (useSearch ? " (using search)" : " (using get)");
        when(message);

        account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, values);

        if (useSearch) {
            ShadowSimpleAttributeDefinition<?> nameDef = getAccountAttrDef(SchemaConstants.ICFS_NAME);

            // @formatter:off
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(RI_ACCOUNT_OBJECT_CLASS)
                    .and().itemWithDef(nameDef, ShadowType.F_ATTRIBUTES, nameDef.getItemName())
                        .eq(getWillNameOnResource())
                    .build();
            // @formatter:on

            var objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
            assertThat(objects).as("accounts matching will").hasSize(1);

        } else {
            provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        }

        then(message);

        RawRepoShadow shadow = getShadowRepoRetrieveAllAttributes(ACCOUNT_WILL_OID, result);
        assertRepoShadowNew(shadow)
                .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, values.toArray());
    }

    @Override
    protected void checkRepoAccountShadowWill(
            RawRepoShadow repoAccount, XMLGregorianCalendar start, XMLGregorianCalendar end, boolean rightAfterCreate) throws CommonException {

        // Sometimes there are 6 and sometimes 7 attributes. Treasure is not returned by default. It is not normally in the cache.
        // So do not check for number of attributes here. Check for individual values.
        checkRepoAccountShadowWillBasic(repoAccount, start, end, rightAfterCreate, null);

        var asserter =
                assertRepoShadowNew(repoAccount)
                        .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
        if (isWeaponIndexOnly()) {
            asserter.assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
        } else {
            asserter.assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
        }
        asserter.assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42L);

        assertRepoShadowCacheActivation(repoAccount, ActivationStatusType.ENABLED);
    }

    @Override
    protected @NotNull Collection<? extends QName> getCachedAccountAttributes() throws SchemaException, ConfigurationException {
        return getAccountDefaultDefinition().getAttributeNames();
    }

    @Override
    protected void assertRepoShadowCacheActivation(RawRepoShadow repoShadow, ActivationStatusType expectedAdministrativeStatus) {
        assertRepoShadowCacheActivation(repoShadow, expectedAdministrativeStatus, true);
    }

    @Override
    protected void assertRepoShadowPasswordValue(RawRepoShadow shadowRepo, PasswordType passwordBean,
            String expectedPassword) throws SchemaException, EncryptionException {
        ProtectedStringType protectedStringType = passwordBean.getValue();
        assertNotNull("No password value in repo shadow " + shadowRepo, protectedStringType);
        assertProtectedString("Wrong password value in repo shadow " + shadowRepo, expectedPassword, protectedStringType, CredentialsStorageTypeType.HASHING);
    }

    @Override
    protected void assertRepoCachingMetadata(PrismObject<ShadowType> shadowFromRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        assertRepoCachingMetadata(shadowFromRepo, start, end, true);
    }

    @Override
    protected void assertCachingMetadata(ShadowType shadow, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        assertCachingMetadata(shadow, startTs, endTs, true);
    }

    @Override
    protected void checkRepoAccountShadow(RawRepoShadow repoShadow) {
        ProvisioningTestUtil.checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT, null);
    }

    @Override
    protected void checkRepoEntitlementShadow(RawRepoShadow repoShadow) {
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
    protected void assertOptionalAttrValue(
            AbstractShadow shadow, String attrName, Object... attrValues) {
        Object[] reallyExpectedValue = isAttrCachedInFullObject(attrName) ? attrValues : new Object[0];
        assertAttribute(shadow, attrName, reallyExpectedValue);
    }

    @Override
    protected void assertCachedAttributeValue(
            AbstractShadow shadow, String attrName, Object... attrValues) {
        Object[] reallyExpectedValue = isAttrCachedInFullObject(attrName) ? attrValues : new Object[0];
        assertAttribute(shadow, attrName, reallyExpectedValue);
    }
}
