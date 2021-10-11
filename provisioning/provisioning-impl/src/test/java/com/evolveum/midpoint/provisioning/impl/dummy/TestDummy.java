/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.time.ZonedDateTime;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.*;
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
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
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
//        InternalMonitor.setTraceConnectorOperation(true);
    }

    // test000-test100 in the superclasses

    @Test
    public void test101AddAccountWithoutName() throws Exception {
        // GIVEN
        Task syncTask = getTestTask();
        OperationResult result = syncTask.getResult();
        syncServiceMock.reset();

        ShadowType account = parseObjectType(ACCOUNT_MORGAN_FILE, ShadowType.class);

        display("Adding shadow", account.asPrismObject());

        // WHEN
        when("add");
        String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, syncTask, result);

        // THEN
        then("add");
        result.computeStatus();
        display("add object result", result);
        TestUtil.assertSuccess("addObject has failed (result)", result);
        assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

        ShadowType accountType = getShadowRepo(ACCOUNT_MORGAN_OID).asObjectable();
        PrismAsserts.assertEqualsPolyString("Account name was not generated (repository)", ACCOUNT_MORGAN_NAME, accountType.getName());
        morganIcfUid = getIcfUid(accountType);

        syncServiceMock.assertNotifySuccessOnly();

        // WHEN
        when("get");
        PrismObject<ShadowType> provisioningAccount = provisioningService.getObject(ShadowType.class,
                ACCOUNT_MORGAN_OID, null, syncTask, result);

        // THEN
        then("get");
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
        displayDumpable("Dummy account", dummyAccount);
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
        displayValue("Repository shadow", shadowFromRepo.debugDump());

        checkRepoAccountShadow(shadowFromRepo);
        // MID-4397
        assertRepoShadowCredentials(shadowFromRepo, ACCOUNT_MORGAN_PASSWORD);

        checkUniqueness(account.asPrismObject());

        assertSteadyResource();
    }

    // test102-test106 in the superclasses

    /**
     * Make a native modification to an account and read it with max staleness option.
     * As there is no caching enabled this should throw an error.
     * <p>
     * Note: This test is overridden in TestDummyCaching
     * <p>
     * MID-3481
     */
    @Test
    public void test107AGetModifiedAccountFromCacheMax() throws Exception {
        // GIVEN
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

        try {

            provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null, result);

            assertNotReached();
        } catch (ConfigurationException e) {
            then();
            displayExpectedException(e);
            assertFailure(result);
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
     * <p>
     * Note: This test is overridden in TestDummyCaching
     * <p>
     * MID-3481
     */
    @Test
    public void test107BGetModifiedAccountFromCacheHighStaleness() throws Exception {
        // GIVEN
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

        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null, result);

        // THEN
        then();
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
     * Incomplete attributes should not be cached.
     */
    @Test
    public void test107CSkipCachingForIncompleteAttributes() throws Exception {
        // overridden in TestDummyCaching
    }

    /**
     * Staleness of one millisecond is too small for the cache to work.
     * Fresh data should be returned - both in case the cache is enabled and disabled.
     * MID-3481
     */
    @Test
    public void test108GetAccountLowStaleness() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createStaleness(1L));

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, null, result);

        // THEN
        then();
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        checkAccountShadow(shadow, result, true);
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

        checkUniqueness(shadow);

        assertCachingMetadata(shadow, false, startTs, endTs);

        assertSteadyResource();
    }

    /**
     * Clean up after caching tests so we won't break subsequent tests.
     * MID-3481
     */
    @Test
    public void test109ModifiedAccountCleanup() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();
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

        checkUniqueness(shadow);

        assertCachingMetadata(shadow, false, startTs, endTs);

        assertSteadyResource();
    }

    @Test
    public void test110SearchIterative() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

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
                    checkAccountShadow(object, parentResult, true);
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
        checkUniqueness(foundObjects);
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
        checkUniqueness(foundObjects);
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
        // GIVEN
        OperationResult result = createOperationResult();

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
        checkUniqueness(foundObjects);
        assertProtected(foundObjects, 1);       // MID-1640

        assertSteadyResource();
    }

    @Test
    public void test112SeachIterativeKindIntent() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_DUMMY_OID,
                ShadowKindType.ACCOUNT, "default", prismContext);
        displayDumpable("query", query);

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
        checkUniqueness(foundObjects);
        assertProtected(foundObjects, 1);       // MID-1640

        assertSteadyResource();
    }

    protected <T extends ShadowType> void assertProtected(List<PrismObject<T>> shadows, int expectedNumberOfProtectedShadows) {
        int actual = countProtected(shadows);
        assertEquals("Unexpected number of protected shadows", expectedNumberOfProtectedShadows, actual);
    }

    private <T extends ShadowType> int countProtected(List<PrismObject<T>> shadows) {
        int count = 0;
        for (PrismObject<T> shadow : shadows) {
            if (shadow.asObjectable().isProtectedObject() != null && shadow.asObjectable().isProtectedObject()) {
                count++;
            }
        }
        return count;
    }

    @Test
    public void test113SearchAllShadowsInRepository() throws Exception {
        // GIVEN
        OperationResult result = new OperationResult(TestDummy.class.getName()
                + ".test113SearchAllShadowsInRepository");
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType, prismContext);
        displayDumpable("All shadows query", query);

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
        // GIVEN
        OperationResult result = createOperationResult();
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
                SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
        displayDumpable("All shadows query", query);

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

        checkUniqueness(allShadows);
        assertProtected(allShadows, 1);

        assertSteadyResource();
    }

    @Test
    public void test115CountAllAccounts() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
                SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
        displayDumpable("All shadows query", query);

        // WHEN
        when();
        Integer count = provisioningService.countObjects(ShadowType.class, query, null, null, result);

        // THEN
        then();
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
        // GIVEN
        OperationResult result = createOperationResult();

        // WHEN
        List<PrismObject<ResourceType>> allResources = provisioningService.searchObjects(ResourceType.class,
                null, null, null, result);

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
        // GIVEN
        OperationResult result = new OperationResult(TestDummy.class.getName()
                + ".test117CountNullQueryResource");

        // WHEN
        int count = provisioningService.countObjects(ResourceType.class, null, null, null, result);

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
        // GIVEN
        OperationResult result = createOperationResult();
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
                SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
        displayDumpable("All shadows query", query);

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

        checkUniqueness(allShadows);
        assertProtected(allShadows, 1);

        assertSteadyResource();
    }

    /**
     * Search for all accounts with maximum staleness option.
     * This is supposed to return only cached data. Therefore
     * repo search is performed. But as caching is
     * not enabled in this test only errors will be returned.
     * <p>
     * Note: This test is overridden in TestDummyCaching
     * <p>
     * MID-3481
     */
    @Test
    public void test119SearchAllAccountsMaxStaleness() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
                SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
        displayDumpable("All shadows query", query);

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

        for (PrismObject<ShadowType> shadow : allShadows) {
            display("Found shadow (error expected)", shadow);
            OperationResultType fetchResult = shadow.asObjectable().getFetchResult();
            assertNotNull("No fetch result status in " + shadow, fetchResult);
            assertEquals("Wrong fetch result status in " + shadow, OperationResultStatusType.FATAL_ERROR, fetchResult.getStatus());
        }

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertProtected(allShadows, 1);

        assertSteadyResource();
    }

    @Test
    public void test120ModifyWillReplaceFullname() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), "Pirate Will Turner");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        delta.checkConsistence();
        assertDummyAccount(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Pirate Will Turner");

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test121ModifyObjectAddPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationAddProperty(ShadowType.class,
                ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                "Pirate");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
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
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationAddProperty(ShadowType.class,
                ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                "Captain");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
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
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationDeleteProperty(ShadowType.class,
                ACCOUNT_WILL_OID, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                "Pirate");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
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
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationAddProperty(ShadowType.class,
                ACCOUNT_WILL_OID, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                "Captain");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
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
        testComparePassword("match", ACCOUNT_WILL_OID, accountWillCurrentPassword, getExpectedPasswordComparisonResultMatch());
        testComparePassword("mismatch", ACCOUNT_WILL_OID, "I woulD NeVeR ever USE this PASSword", getExpectedPasswordComparisonResultMismatch());

        assertSteadyResource();
    }

    /**
     * MID-4397
     */
    @Test
    public void test126ModifyAccountWillPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = createAccountPaswordDelta(ACCOUNT_WILL_OID, ACCOUNT_WILL_PASSWORD_123, null);
        displayDumpable("ObjectDelta", delta);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccount(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid)
                .assertPassword(ACCOUNT_WILL_PASSWORD_123)
                .assertLastModifier(null);

        accountWillCurrentPassword = ACCOUNT_WILL_PASSWORD_123;

        // Check if the shadow is in the repo
        PrismObject<ShadowType> repoShadow = getShadowRepo(ACCOUNT_WILL_OID);
        assertNotNull("Shadow was not created in the repository", repoShadow);
        display("Repository shadow", repoShadow);

        checkRepoAccountShadow(repoShadow);
        assertRepoShadowCredentials(repoShadow, ACCOUNT_WILL_PASSWORD_123);

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    /**
     * MID-4397
     */
    @Test
    public void test127CompareAccountWillPassword() throws Exception {
        testComparePassword("match", ACCOUNT_WILL_OID, accountWillCurrentPassword, getExpectedPasswordComparisonResultMatch());
        testComparePassword("mismatch old password", ACCOUNT_WILL_OID, ACCOUNT_WILL_PASSWORD, getExpectedPasswordComparisonResultMismatch());
        testComparePassword("mismatch", ACCOUNT_WILL_OID, "I woulD NeVeR ever USE this PASSword", getExpectedPasswordComparisonResultMismatch());

        assertSteadyResource();
    }

    protected void testComparePassword(String tag, String shadowOid,
            String expectedPassword, ItemComparisonResult expectedResult) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        // WHEN (match)
        when();
        ItemComparisonResult comparisonResult = provisioningService.compare(ShadowType.class, shadowOid, SchemaConstants.PATH_PASSWORD_VALUE,
                expectedPassword, task, result);

        // THEN (match)
        then();
        assertSuccess(result);

        displayValue("Comparison result (" + tag + ")", comparisonResult);
        assertEquals("Wrong comparison result (" + tag + ")", expectedResult, comparisonResult);

        syncServiceMock.assertNoNotifcations();
    }

    /**
     * Set a null value to the (native) dummy attribute. The UCF layer should filter that out.
     */
    @Test
    public void test129NullAttributeValue() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        DummyAccount willDummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        willDummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, null);

        // WHEN
        when();
        PrismObject<ShadowType> accountWill = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(accountWill);
        ResourceAttribute<Object> titleAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME));
        assertNull("Title attribute sneaked in", titleAttribute);

        accountWill.checkConsistence();

        assertSteadyResource();
    }

    @Test
    public void test131AddScript() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        dummyResource.purgeScriptHistory();

        ShadowType account = parseObjectType(ACCOUNT_SCRIPT_FILE, ShadowType.class);
        display("Account before add", account);

        OperationProvisioningScriptsType scriptsType = unmarshalValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
        displayValue("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        dummyResource.purgeScriptHistory();

        OperationProvisioningScriptsType scriptsType = unmarshalValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
        displayValue("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_NEW_SCRIPT_OID, dummyResourceCtl.getAttributeFullnamePath(), "Will Turner");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID, delta.getModifications(),
                scriptsType, null, task, result);

        // THEN
        assertSuccess(result);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        dummyResource.purgeScriptHistory();

        OperationProvisioningScriptsType scriptsType = unmarshalValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
        displayValue("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_NEW_SCRIPT_OID, ShadowType.F_DESCRIPTION, "Blah blah");
        displayDumpable("ObjectDelta", delta);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        dummyResource.purgeScriptHistory();

        OperationProvisioningScriptsType scriptsType = unmarshalValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
        displayValue("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        dummyResource.purgeScriptHistory();

        OperationProvisioningScriptsType scriptsType = unmarshalValueFromFile(SCRIPTS_FILE, OperationProvisioningScriptsType.class);
        displayValue("Provisioning scripts", PrismTestUtil.serializeAnyDataWrapped(scriptsType));

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
                result).asObjectable();
        assertNotNull(accountType);

        display("Retrieved account shadow", accountType);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertTrue(dummyAccount.isEnabled());

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.DISABLED);
        displayDumpable("ObjectDelta", delta);
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
        assertFalse("Dummy account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + " is enabled, expected disabled", dummyAccount.isEnabled());

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test151SearchDisabledAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(),
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.DISABLED)
                        .buildFilter(), prismContext);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
                result).asObjectable();
        assertNotNull(accountType);
        display("Retrieved account shadow", accountType);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertFalse("Account is not disabled", dummyAccount.isEnabled());

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationDeleteProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.DISABLED);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        // check if activation was changed
        dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertNull("Wrong dummy account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + " enabled flag",
                dummyAccount.isEnabled());

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test154EnableAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
                result).asObjectable();
        assertNotNull(accountType);
        display("Retrieved account shadow", accountType);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertNull("Wrong dummy account enabled flag", dummyAccount.isEnabled());

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.ENABLED);
        displayDumpable("ObjectDelta", delta);
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
        assertTrue("Dummy account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + " is disabled, expected enabled", dummyAccount.isEnabled());

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test155SearchDisabledAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(),
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.DISABLED)
                        .buildFilter(), prismContext);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
                result).asObjectable();
        assertNotNull(accountType);

        display("Retrieved account shadow", accountType);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertTrue(dummyAccount.isEnabled());

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
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
        assertEquals("Wrong account validFrom in account " + transformNameFromResource(ACCOUNT_WILL_USERNAME), new Date(VALID_FROM_MILLIS), dummyAccount.getValidFrom());
        assertTrue("Dummy account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + " is disabled, expected enabled", dummyAccount.isEnabled());

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test157SetValidTo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
                result).asObjectable();
        assertNotNull(accountType);

        display("Retrieved account shadow", accountType);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertTrue(dummyAccount.isEnabled());

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO,
                XmlTypeConverter.createXMLGregorianCalendar(VALID_TO_MILLIS));
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(),
                delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        // check if activation was changed
        dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertEquals("Wrong account validFrom in account " + transformNameFromResource(ACCOUNT_WILL_USERNAME), new Date(VALID_FROM_MILLIS), dummyAccount.getValidFrom());
        assertEquals("Wrong account validTo in account " + transformNameFromResource(ACCOUNT_WILL_USERNAME), new Date(VALID_TO_MILLIS), dummyAccount.getValidTo());
        assertTrue("Dummy account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + " is disabled, expected enabled", dummyAccount.isEnabled());

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test158DeleteValidToValidFrom() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
                result).asObjectable();
        assertNotNull(accountType);

        display("Retrieved account shadow", accountType);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertTrue(dummyAccount.isEnabled());

        syncServiceMock.reset();

//        long millis = VALID_TO_MILLIS;

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationDeleteProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO,
                XmlTypeConverter.createXMLGregorianCalendar(VALID_TO_MILLIS));
        PrismObjectDefinition<ShadowType> def = accountType.asPrismObject().getDefinition();
        PropertyDelta<XMLGregorianCalendar> validFromDelta = prismContext.deltaFactory().property().createModificationDeleteProperty(
                SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_VALID_FROM),
                XmlTypeConverter.createXMLGregorianCalendar(VALID_FROM_MILLIS));
        delta.addModification(validFromDelta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(),
                delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        // check if activation was changed
        dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertNull("Unexpected account validTo in account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + ": " + dummyAccount.getValidTo(), dummyAccount.getValidTo());
        assertNull("Unexpected account validFrom in account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + ": " + dummyAccount.getValidFrom(), dummyAccount.getValidFrom());
        assertTrue("Dummy account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + " is disabled, expected enabled", dummyAccount.isEnabled());

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test159GetLockedoutAccount() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        dummyAccount.setLockout(true);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, result);

        // THEN
        then();
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

        checkUniqueness(shadow);

        assertSteadyResource();
    }

    @Test
    public void test160SearchLockedAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(),
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).eq(LockoutStatusType.LOCKED)
                        .buildFilter(), prismContext);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task,
                result).asObjectable();
        assertNotNull(accountType);
        display("Retrieved account shadow", accountType);

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertTrue("Account is not locked", dummyAccount.isLockout());

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                LockoutStatusType.NORMAL);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        // check if activation was changed
        dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertFalse("Dummy account " + transformNameFromResource(ACCOUNT_WILL_USERNAME) + " is locked, expected unlocked", dummyAccount.isLockout());

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test163GetAccount() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

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

        checkUniqueness(shadow);

        assertSteadyResource();
    }

    @Test
    public void test163SearchLockedAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(),
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).eq(LockoutStatusType.LOCKED)
                        .buildFilter(), prismContext);

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
        testSearchIterative(null, null, true, true, false,
                "meathook", "daemon", transformNameFromResource("morgan"), transformNameFromResource("Will"));
    }

    @Test
    public void test171SearchShipSeaMonkey() throws Exception {
        testSeachIterativeSingleAttrFilter(
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Sea Monkey", null, true,
                "meathook");
    }

    // See MID-1460
    @Test(enabled = false)
    public void test172SearchShipNull() throws Exception {
        testSeachIterativeSingleAttrFilter(
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, null, null, true,
                "daemon", "Will");
    }

    @Test
    public void test173SearchWeaponCutlass() throws Exception {
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

        testSeachIterativeSingleAttrFilter(
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "cutlass", null, true,
                transformNameFromResource("morgan"), "carla");
    }

    @Test
    public void test175SearchUidExact() throws Exception {
        dummyResource.setDisableNameHintChecks(true);
        testSeachIterativeSingleAttrFilter(
                SchemaConstants.ICFS_UID, willIcfUid, null, true,
                transformNameFromResource("Will"));
        dummyResource.setDisableNameHintChecks(false);
    }

    @Test
    public void test176SearchUidExactNoFetch() throws Exception {
        testSeachIterativeSingleAttrFilter(SchemaConstants.ICFS_UID, willIcfUid,
                GetOperationOptions.createNoFetch(), false,
                transformNameFromResource("Will"));
    }

    @Test
    public void test177SearchIcfNameRepoized() throws Exception {
        testSeachIterativeSingleAttrFilter(
                SchemaConstants.ICFS_NAME, getWillRepoIcfName(), null, true,
                transformNameFromResource(ACCOUNT_WILL_USERNAME));
    }

    @Test
    public void test180SearchNullPagingOffset0Size3() throws Exception {
        ObjectPaging paging = prismContext.queryFactory().createPaging(0, 3);
        paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME));
        SearchResultMetadata searchMetadata = testSeachIterativePaging(null, paging, null,
                getSortedUsernames18x(0, 3));
        assertApproxNumberOfAllResults(searchMetadata, getTest18xApproxNumberOfSearchResults());
    }

    /**
     * Reverse sort order, so we are sure that this thing is really sorting
     * and not just returning data in alphabetical order by default.
     */
    @Test
    public void test181SearchNullPagingOffset0Size3Desc() throws Exception {
        ObjectPaging paging = prismContext.queryFactory().createPaging(0, 3);
        paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME, OrderDirection.DESCENDING));
        SearchResultMetadata searchMetadata = testSeachIterativePaging(null, paging, null,
                getSortedUsernames18xDesc(0, 3));
        assertApproxNumberOfAllResults(searchMetadata, getTest18xApproxNumberOfSearchResults());
    }

    @Test
    public void test182SearchNullPagingOffset1Size2() throws Exception {
        ObjectPaging paging = prismContext.queryFactory().createPaging(1, 2);
        paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME));
        SearchResultMetadata searchMetadata = testSeachIterativePaging(null, paging, null,
                getSortedUsernames18x(1, 2));
        assertApproxNumberOfAllResults(searchMetadata, getTest18xApproxNumberOfSearchResults());
    }

    @Test
    public void test183SearchNullPagingOffset2Size3Desc() throws Exception {
        ObjectPaging paging = prismContext.queryFactory().createPaging(2, 3);
        paging.setOrdering(createAttributeOrdering(SchemaConstants.ICFS_NAME, OrderDirection.DESCENDING));
        SearchResultMetadata searchMetadata = testSeachIterativePaging(null, paging, null,
                getSortedUsernames18xDesc(2, 3));
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
        return prismContext.queryFactory().createOrdering(ItemPath.create(ShadowType.F_ATTRIBUTES, attrQname), direction);
    }

    @Test
    public void test194SearchIcfNameRepoizedNoFetch() throws Exception {
        testSeachIterativeSingleAttrFilter(SchemaConstants.ICFS_NAME, getWillRepoIcfName(),
                GetOperationOptions.createNoFetch(), false,
                transformNameFromResource(ACCOUNT_WILL_USERNAME));
    }

    @Test
    public void test195SearchIcfNameExact() throws Exception {
        testSeachIterativeSingleAttrFilter(
                SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_WILL_USERNAME), null, true,
                transformNameFromResource(ACCOUNT_WILL_USERNAME));
    }

    @Test
    public void test196SearchIcfNameExactNoFetch() throws Exception {
        testSeachIterativeSingleAttrFilter(SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_WILL_USERNAME),
                GetOperationOptions.createNoFetch(), false,
                transformNameFromResource(ACCOUNT_WILL_USERNAME));
    }

    // TEMPORARY todo move to more appropriate place (model-intest?)
    @Test
    public void test197SearchIcfNameAndUidExactNoFetch() throws Exception {
        testSeachIterativeAlternativeAttrFilter(SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_WILL_USERNAME),
                SchemaConstants.ICFS_UID, willIcfUid,
                GetOperationOptions.createNoFetch(), false,
                transformNameFromResource(ACCOUNT_WILL_USERNAME));
    }

    @Test
    public void test198SearchNone() throws Exception {
        ObjectFilter attrFilter = FilterCreationUtil.createNone(prismContext);
        testSearchIterative(attrFilter, null, true, true, false);
    }

    /**
     * Search with query that queries both the repository and the resource.
     * We cannot do this. This should fail.
     * MID-2822
     */
    @Test
    public void test199SearchOnAndOffResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createOnOffQuery();

        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
            @Override
            public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
                AssertJUnit.fail("Handler called: " + object);
                return false;
            }
        };

        try {
            // WHEN
            provisioningService.searchObjectsIterative(ShadowType.class, query,
                    null, handler, task, result);

            AssertJUnit.fail("unexpected success");

        } catch (SchemaException e) {
            displayExpectedException(e);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createOnOffQuery();

        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
            @Override
            public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
                AssertJUnit.fail("Handler called: " + object);
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

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME))
                .and().itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getItemName()).eq("Sea Monkey")
                .and().item(ShadowType.F_DEAD).eq(true)
                .build();
        displayDumpable("Query", query);
        return query;
    }

    protected <T> void testSeachIterativeSingleAttrFilter(String attrName, T attrVal,
            GetOperationOptions rootOptions, boolean fullShadow, String... expectedAccountIds) throws Exception {
        testSeachIterativeSingleAttrFilter(dummyResourceCtl.getAttributeQName(attrName), attrVal,
                rootOptions, fullShadow, expectedAccountIds);
    }

    protected <T> void testSeachIterativeSingleAttrFilter(QName attrQName, T attrVal,
            GetOperationOptions rootOptions, boolean fullShadow, String... expectedAccountNames) throws Exception {
        ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        ObjectClassComplexTypeDefinition objectClassDef = resourceSchema.findObjectClassDefinition(SchemaTestConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
        ResourceAttributeDefinition<T> attrDef = objectClassDef.findAttributeDefinition(attrQName);
        ObjectFilter filter = prismContext.queryFor(ShadowType.class)
                .itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getItemName()).eq(attrVal)
                .buildFilter();
        testSearchIterative(filter, rootOptions, fullShadow, true, false, expectedAccountNames);
    }

    protected <T> void testSeachIterativeAlternativeAttrFilter(QName attr1QName, T attr1Val,
            QName attr2QName, T attr2Val,
            GetOperationOptions rootOptions, boolean fullShadow, String... expectedAccountNames) throws Exception {
        ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        ObjectClassComplexTypeDefinition objectClassDef = resourceSchema.findObjectClassDefinition(SchemaTestConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
        ResourceAttributeDefinition<T> attr1Def = objectClassDef.findAttributeDefinition(attr1QName);
        ResourceAttributeDefinition<T> attr2Def = objectClassDef.findAttributeDefinition(attr2QName);
        ObjectFilter filter = prismContext.queryFor(ShadowType.class)
                .itemWithDef(attr1Def, ShadowType.F_ATTRIBUTES, attr1Def.getItemName()).eq(attr1Val)
                .or().itemWithDef(attr2Def, ShadowType.F_ATTRIBUTES, attr2Def.getItemName()).eq(attr2Val)
                .buildFilter();
        testSearchIterative(filter, rootOptions, fullShadow, false, true, expectedAccountNames);
    }

    private SearchResultMetadata testSearchIterative(
            ObjectFilter attrFilter, GetOperationOptions rootOptions, final boolean fullShadow,
            boolean useObjectClassFilter, final boolean useRepo, String... expectedAccountNames)
            throws Exception {
        OperationResult result = createOperationResult();

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
                query.setFilter(prismContext.queryFactory().createAnd(query.getFilter(), attrFilter));
            }
        }

        displayDumpable("Query", query);

        final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>();
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

            @Override
            public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
                foundObjects.add(shadow);

                assertTrue(shadow.canRepresent(ShadowType.class));
                if (!useRepo) {
                    try {
                        checkAccountShadow(shadow, parentResult, fullShadow);
                    } catch (SchemaException e) {
                        throw new SystemException(e.getMessage(), e);
                    }
                }
                return true;
            }
        };

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);

        // WHEN
        when();
        SearchResultMetadata searchMetadata;
        if (useRepo) {
            searchMetadata = repositoryService.searchObjectsIterative(ShadowType.class, query, handler, null, true, result);
        } else {
            searchMetadata = provisioningService.searchObjectsIterative(ShadowType.class, query, options, handler, null, result);
        }

        // THEN
        then();
        result.computeStatus();
        display("searchObjectsIterative result", result);
        TestUtil.assertSuccess(result);

        display("found shadows", foundObjects);

        for (String expectedAccountId : expectedAccountNames) {
            boolean found = false;
            for (PrismObject<ShadowType> foundObject : foundObjects) {
                if (expectedAccountId.equals(foundObject.asObjectable().getName().getOrig())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                AssertJUnit.fail("Account " + expectedAccountId + " was expected to be found but it was not found (found " + foundObjects.size() + ", expected " + expectedAccountNames.length + ")");
            }
        }

        assertEquals("Wrong number of found objects (" + foundObjects + "): " + foundObjects, expectedAccountNames.length, foundObjects.size());
        if (!useRepo) {
            checkUniqueness(foundObjects);
        }
        assertSteadyResource();

        return searchMetadata;
    }

    // This has to be a different method than ordinary search. We care about ordering here.
    // Also, paged search without sorting does not make much sense anyway.
    private SearchResultMetadata testSeachIterativePaging(ObjectFilter attrFilter,
            ObjectPaging paging, GetOperationOptions rootOptions, String... expectedAccountNames)
            throws Exception {
        OperationResult result = createOperationResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
                SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext);
        if (attrFilter != null) {
            AndFilter filter = (AndFilter) query.getFilter();
            filter.getConditions().add(attrFilter);
        }
        query.setPaging(paging);

        displayDumpable("Query", query);

        final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>();
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

            @Override
            public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
                foundObjects.add(shadow);

                assertTrue(shadow.canRepresent(ShadowType.class));
                try {
                    checkAccountShadow(shadow, parentResult, true);
                } catch (SchemaException e) {
                    throw new SystemException(e.getMessage(), e);
                }
                return true;
            }
        };

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);

        // WHEN
        when();
        SearchResultMetadata searchMetadata = provisioningService.searchObjectsIterative(ShadowType.class, query, options, handler, null, result);

        // THEN
        then();
        result.computeStatus();
        display("searchObjectsIterative result", result);
        TestUtil.assertSuccess(result);

        display("found shadows", foundObjects);

        int i = 0;
        for (String expectedAccountId : expectedAccountNames) {
            PrismObject<ShadowType> foundObject = foundObjects.get(i);
            if (!expectedAccountId.equals(foundObject.asObjectable().getName().getOrig())) {
                fail("Account " + expectedAccountId + " was expected to be found on " + i + " position, but it was not found (found " + foundObject.asObjectable().getName().getOrig() + ")");
            }
            i++;
        }

        assertEquals("Wrong number of found objects (" + foundObjects + "): " + foundObjects, expectedAccountNames.length, foundObjects.size());
        checkUniqueness(foundObjects);
        assertSteadyResource();

        return searchMetadata;
    }

    @Test
    public void test200AddGroup() throws Exception {
        // GIVEN
        Task task = getTestTask();
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
        assertNotNull("No dummy group " + GROUP_PIRATES_NAME, dummyGroup);
        assertEquals("Description is wrong", "Scurvy pirates", dummyGroup.getAttributeValue("description"));
        assertTrue("The group is not enabled", dummyGroup.isEnabled());

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
        assertNotNull("Shadow was not created in the repository", shadowFromRepo);
        displayValue("Repository shadow", shadowFromRepo.debugDump());

        checkRepoEntitlementShadow(shadowFromRepo);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);
        checkUniqueness(group);
        assertDummyResourceGroupMembersReadCountIncrement(null, 0);
        assertSteadyResource();
    }

    @Test
    public void test202GetGroup() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

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

        checkUniqueness(shadow);

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
        // GIVEN
        OperationResult result = createOperationResult();

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

        checkUniqueness(shadow);

        assertSteadyResource();
    }

    @Test
    public void test205ModifyGroupReplace() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                GROUP_PIRATES_OID,
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
                "Bloodthirsty pirates");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        assertSuccess(result);

        delta.checkConsistence();
        DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
        assertDummyAttributeValues(group, DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Bloodthirsty pirates");

        if (isPreFetchResource()) {
            assertDummyResourceGroupMembersReadCountIncrement(null, 1);
        } else {
            assertDummyResourceGroupMembersReadCountIncrement(null, 0);
        }

        syncServiceMock.assertNotifySuccessOnly();
        assertSteadyResource();
    }

    @Test
    public void test210AddPrivilege() throws Exception {
        // GIVEN
        Task task = getTestTask();
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
        assertNotNull("No dummy priv " + PRIVILEGE_PILLAGE_NAME, dummyPriv);
        assertEquals("Wrong privilege power", (Integer) 100, dummyPriv.getAttributeValue(DummyResourceContoller.DUMMY_PRIVILEGE_ATTRIBUTE_POWER, Integer.class));

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
        assertNotNull("Shadow was not created in the repository", shadowFromRepo);
        displayValue("Repository shadow", shadowFromRepo.debugDump());

        checkRepoEntitlementShadow(shadowFromRepo);

        checkUniqueness(priv);
        assertSteadyResource();
    }

    @Test
    public void test212GetPriv() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        // WHEN
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, null, null, result);

        // THEN
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);

        display("Retrieved priv shadow", shadow);

        assertNotNull("No dummy priv", shadow);

        checkPrivPillage(shadow, result);

        checkUniqueness(shadow);

        assertSteadyResource();
    }

    private void checkPrivPillage(PrismObject<ShadowType> shadow, OperationResult result) throws SchemaException {
        checkEntitlementShadow(shadow, result, OBJECTCLASS_PRIVILEGE_LOCAL_NAME, true);
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
        // GIVEN
        Task task = getTestTask();
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
        assertNotNull("No dummy priv " + PRIVILEGE_BARGAIN_NAME, dummyPriv);

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
        assertNotNull("Shadow was not created in the repository", shadowFromRepo);
        displayValue("Repository shadow", shadowFromRepo.debugDump());

        checkRepoEntitlementShadow(shadowFromRepo);

        checkUniqueness(priv);
        assertDummyResourceGroupMembersReadCountIncrement(null, 0);
        assertSteadyResource();
    }

    private void checkPrivBargain(PrismObject<ShadowType> shadow, OperationResult result) throws SchemaException {
        checkEntitlementShadow(shadow, result, OBJECTCLASS_PRIVILEGE_LOCAL_NAME, true);
        assertShadowName(shadow, PRIVILEGE_BARGAIN_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ENTITLEMENT, shadow.asObjectable().getKind());
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertEquals("Unexpected number of attributes", 2, attributes.size());

        assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
                shadow, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
    }

    @Test
    public void test220EntitleAccountWillPirates() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
                GROUP_PIRATES_OID, prismContext);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        if (isPreFetchResource()) {
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
        Task task = getTestTask();
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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
                ASSOCIATION_PRIV_NAME, PRIVILEGE_PILLAGE_OID, prismContext);
        displayDumpable("ObjectDelta", delta);
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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
                ASSOCIATION_PRIV_NAME, PRIVILEGE_BARGAIN_OID, prismContext);
        displayDumpable("ObjectDelta", delta);
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
        Task task = getTestTask();
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
        // GIVEN
        Task task = getTestTask();
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

        PrismObject<ShadowType> foolsShadow = findShadowByName(new QName(RESOURCE_DUMMY_NS, OBJECTCLASS_GROUP_LOCAL_NAME), "fools", resource, result);
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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        dummyAccount.addAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, PRIVILEGE_NONSENSE_NAME);

        syncServiceMock.reset();

        // WHEN
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        // THEN
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 3);

        assertDummyResourceGroupMembersReadCountIncrement(null, 0);

        PrismObject<ShadowType> foolsShadow = findShadowByName(new QName(RESOURCE_DUMMY_NS, OBJECTCLASS_GROUP_LOCAL_NAME), "fools", resource, result);
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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
                GROUP_PIRATES_OID, prismContext);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);
        delta.checkConsistence();
        assertAccountPiratesDetitled();
    }

    /**
     * Entitle will to pirates. But this time use identifiers instead of OID.
     * Relates to MID-5315
     */
    @Test
    public void test232EntitleAccountWillPiratesIdentifiersName() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDeltaIdentifiers(ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
                SchemaConstants.ICFS_NAME, GROUP_PIRATES_NAME, prismContext);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);
        delta.checkConsistence();
        assertAccountPiratesEntitled();
    }

    /**
     * Detitle will to pirates. But this time use identifiers instead of OID.
     * MID-5315
     */
    @Test
    public void test233DetitleAccountWillPiratesIdentifiersName() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDeltaIdentifiers(ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
                SchemaConstants.ICFS_NAME, GROUP_PIRATES_NAME, prismContext);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);
        delta.checkConsistence();
        assertAccountPiratesDetitled();
    }

    /**
     * Entitle will to pirates. But this time use identifiers instead of OID.
     * Relates to MID-5315
     */
    @Test
    public void test234EntitleAccountWillPiratesIdentifiersUid() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDeltaIdentifiers(ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
                SchemaConstants.ICFS_UID, piratesIcfUid, prismContext);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);
        delta.checkConsistence();
        assertAccountPiratesEntitled();
    }

    /**
     * Detitle will to pirates. But this time use identifiers instead of OID.
     * MID-5315
     */
    @Test
    public void test235DetitleAccountWillPiratesIdentifiersUid() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberDummyResourceGroupMembersReadCount(null);
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDeltaIdentifiers(ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
                SchemaConstants.ICFS_UID, piratesIcfUid, prismContext);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);
        delta.checkConsistence();
        assertAccountPiratesDetitled();
    }

    private void assertAccountPiratesDetitled() throws Exception {
        if (isPreFetchResource()) {
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

        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow after", shadow);
        assertEntitlementPriv(shadow, PRIVILEGE_PILLAGE_OID);
        assertEntitlementPriv(shadow, PRIVILEGE_BARGAIN_OID);

        assertSteadyResource();
    }

    private void assertAccountPiratesEntitled() throws Exception {
        if (isPreFetchResource()) {
            assertDummyResourceGroupMembersReadCountIncrement(null, 1);
        } else {
            assertDummyResourceGroupMembersReadCountIncrement(null, 0);
        }

        DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
        assertMember(group, transformNameFromResource(ACCOUNT_WILL_USERNAME));

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

        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow after", shadow);
        assertEntitlementGroup(shadow, GROUP_PIRATES_OID);
        assertEntitlementPriv(shadow, PRIVILEGE_PILLAGE_OID);
        assertEntitlementPriv(shadow, PRIVILEGE_BARGAIN_OID);

        assertSteadyResource();
    }

    @Test
    public void test238DetitleAccountWillPillage() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
                ASSOCIATION_PRIV_NAME, PRIVILEGE_PILLAGE_OID, prismContext);
        displayDumpable("ObjectDelta", delta);
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
    public void test239DetitleAccountWillBargain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
                ASSOCIATION_PRIV_NAME, PRIVILEGE_BARGAIN_OID, prismContext);
        displayDumpable("ObjectDelta", delta);
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
        // GIVEN
        Task task = getTestTask();
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

        checkUniqueness(provisioningAccount);

        assertSteadyResource();
    }

    /**
     * LeChuck has both group and priv entitlement. If deleted it should be correctly removed from all
     * the entitlements.
     */
    @Test
    public void test265DeleteAccountLeChuck() throws Exception {
        // GIVEN
        Task task = getTestTask();
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
        Task task = getTestTask();
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
        Task task = getTestTask();
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
        assertNull("Dummy group '" + GROUP_PIRATES_NAME + "' is not gone from dummy resource", dummyAccount);

        assertSteadyResource();
    }

    @Test
    public void test300AccountRename() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_MORGAN_OID, SchemaTestConstants.ICFS_NAME_PATH_PARTS, ACCOUNT_CPTMORGAN_NAME);
        provisioningService.applyDefinition(delta, task, result);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
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
        PrismAsserts.assertPropertyValue(repoShadow, SchemaTestConstants.ICFS_UID_PATH_PARTS, shadowUuid);

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    /**
     * MID-4751
     */
    @Test
    public void test310ModifyMorganEnlistTimestamp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_MORGAN_OID,
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME),
                XmlTypeConverter.createXMLGregorianCalendarFromIso8601(ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP_MODIFIED));
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        delta.checkConsistence();
        // check if attribute was changed
        DummyAccount dummyAccount = getDummyAccount(transformNameFromResource(ACCOUNT_CPTMORGAN_NAME), morganIcfUid);
        displayDumpable("Dummy account", dummyAccount);
        ZonedDateTime enlistTimestamp = dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME, ZonedDateTime.class);
        assertEqualTime("wrong dummy enlist timestamp", ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP_MODIFIED, enlistTimestamp);

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    /**
     * Change password, using runAsAccountOid option.
     * MID-4397
     */
    @Test
    public void test330ModifyAccountWillPasswordSelfService() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = createAccountPaswordDelta(ACCOUNT_WILL_OID, ACCOUNT_WILL_PASSWORD_321, ACCOUNT_WILL_PASSWORD_123);
        displayDumpable("ObjectDelta", delta);

        ProvisioningOperationOptions options = ProvisioningOperationOptions.createRunAsAccountOid(ACCOUNT_WILL_OID);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), options, task, result);

        // THEN
        then();
        assertSuccess(result);

        // Check if the account was created in the dummy resource
        assertDummyAccount(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid)
                .assertPassword(ACCOUNT_WILL_PASSWORD_321)
                .assertLastModifier(getLastModifierName(ACCOUNT_WILL_USERNAME));

        accountWillCurrentPassword = ACCOUNT_WILL_PASSWORD_321;

        // Check if the shadow is in the repo
        PrismObject<ShadowType> repoShadow = getShadowRepo(ACCOUNT_WILL_OID);
        assertNotNull("Shadow was not created in the repository", repoShadow);
        display("Repository shadow", repoShadow);

        checkRepoAccountShadow(repoShadow);
        assertRepoShadowCredentials(repoShadow, ACCOUNT_WILL_PASSWORD_321);

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    protected static final String WILL_GOSSIP_AVAST = "Aye! Avast!";
    protected static final String WILL_GOSSIP_BLOOD_OF_A_PIRATE = "Blood of a pirate";
    protected static final String WILL_GOSSIP_EUNUCH = "Eunuch!";

    /**
     * Gossip is a multivalue attribute. Make sure that replace operations work
     * as expected also for multivalue.
     */
    @Test
    public void test340ModifyWillReplaceGossipBloodAvast() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        // This turns on recording of the operations
        dummyResourceCtl.setSyncStyle(DummySyncStyle.DUMB);
        dummyResource.clearDeltas();

        List<ItemDelta<?, ?>> mods = getGossipDelta(PlusMinusZero.ZERO, WILL_GOSSIP_BLOOD_OF_A_PIRATE, WILL_GOSSIP_AVAST);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_WILL_OID, mods, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAccountWillGossip(WILL_GOSSIP_BLOOD_OF_A_PIRATE, WILL_GOSSIP_AVAST);
        assertWillDummyGossipRecord(PlusMinusZero.ZERO, WILL_GOSSIP_BLOOD_OF_A_PIRATE, WILL_GOSSIP_AVAST);

        syncServiceMock.assertNotifySuccessOnly();
        assertSteadyResource();
    }

    /**
     * Gossip is a multivalue attribute. Make sure that replace operations work
     * as expected also for multivalue.
     */
    @Test
    public void test342ModifyWillAddGossipEunuch() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        // This turns on recording of the operations
        dummyResourceCtl.setSyncStyle(DummySyncStyle.DUMB);
        dummyResource.clearDeltas();

        List<ItemDelta<?, ?>> mods = getGossipDelta(PlusMinusZero.PLUS, WILL_GOSSIP_EUNUCH);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_WILL_OID, mods, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAccountWillGossip(WILL_GOSSIP_BLOOD_OF_A_PIRATE, WILL_GOSSIP_AVAST, WILL_GOSSIP_EUNUCH);
        assertWillDummyGossipRecord(PlusMinusZero.PLUS, WILL_GOSSIP_EUNUCH);

        syncServiceMock.assertNotifySuccessOnly();
        assertSteadyResource();
    }

    /**
     * Gossip is a multivalue attribute. Make sure that replace operations work
     * as expected also for multivalue.
     */
    @Test
    public void test344ModifyWillDeleteGossipAvast() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        // This turns on recording of the operations
        dummyResourceCtl.setSyncStyle(DummySyncStyle.DUMB);
        dummyResource.clearDeltas();

        List<ItemDelta<?, ?>> mods = getGossipDelta(PlusMinusZero.MINUS, WILL_GOSSIP_AVAST);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_WILL_OID, mods, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAccountWillGossip(WILL_GOSSIP_BLOOD_OF_A_PIRATE, WILL_GOSSIP_EUNUCH);
        assertWillDummyGossipRecord(PlusMinusZero.MINUS, WILL_GOSSIP_AVAST);

        syncServiceMock.assertNotifySuccessOnly();
        assertSteadyResource();
    }

    protected List<ItemDelta<?, ?>> getGossipDelta(PlusMinusZero plusMinusZero, String... values) throws SchemaException {
        List<ItemDelta<?, ?>> mods = prismContext.deltaFor(ShadowType.class)
                .property(dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME), null)
                .mod(plusMinusZero, values)
                .asItemDeltas();
        display("Modifications", mods);
        return mods;
    }

    protected void assertAccountWillGossip(String... values) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        displayDumpable("Account will", getDummyAccount(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid));
        assertDummyAccount(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, values);
    }

    protected void assertWillDummyGossipRecord(PlusMinusZero plusminus, String... expectedValues) {
        displayValue("Dummy resource deltas", dummyResource.dumpDeltas());
        List<DummyDelta> dummyDeltas = dummyResource.getDeltas();
        assertFalse("Empty dummy resource deltas", dummyDeltas.isEmpty());
        assertEquals("Too many dummy resource deltas", 1, dummyDeltas.size());
        DummyDelta dummyDelta = dummyDeltas.get(0);
        assertEquals("Wrong dummy resource delta object name", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyDelta.getObjectName());
        assertEquals("Wrong dummy resource delta type", DummyDeltaType.MODIFY, dummyDelta.getType());
        assertEquals("Wrong dummy resource delta attribute", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, dummyDelta.getAttributeName());
        Collection<String> valuesToConsider = null;
        switch (plusminus) {
            case PLUS:
                valuesToConsider = (Collection) dummyDelta.getValuesAdded();
                break;
            case MINUS:
                valuesToConsider = (Collection) dummyDelta.getValuesDeleted();
                break;
            case ZERO:
                valuesToConsider = (Collection) dummyDelta.getValuesReplaced();
                break;
        }
        PrismAsserts.assertEqualsCollectionUnordered("Wrong values for " + plusminus + " in dummy resource delta", valuesToConsider, expectedValues);
    }

    protected String getLastModifierName(String expected) {
        return transformNameToResource(expected);
    }

    // test4xx reserved for subclasses

    @Test
    public void test500AddProtectedAccount() throws Exception {
        testAddProtectedAccount(ACCOUNT_DAVIEJONES_USERNAME);
    }

    @Test
    public void test501GetProtectedAccountShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);

        assertEquals("" + account + " is not protected", Boolean.TRUE, account.asObjectable().isProtectedObject());
        checkUniqueness(account);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        Collection<? extends ItemDelta> modifications = new ArrayList<>(1);
        ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
        ResourceAttributeDefinition fullnameAttrDef = defaultAccountDefinition.findAttributeDefinition("fullname");
        ResourceAttribute fullnameAttr = fullnameAttrDef.instantiate();
        PropertyDelta fullnameDelta = fullnameAttr.createDelta(ItemPath.create(ShadowType.F_ATTRIBUTES,
                fullnameAttrDef.getItemName()));
        fullnameDelta.setRealValuesToReplace("Good Daemon");
        ((Collection) modifications).add(fullnameDelta);

        // WHEN
        try {
            provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, modifications, null, null, task, result);
            AssertJUnit.fail("Expected security exception while modifying 'daemon' account");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        display("modifyObject result (expected failure)", result);
        TestUtil.assertFailure(result);

        syncServiceMock.assertNotifyFailureOnly();

//        checkConsistency();

        assertSteadyResource();
    }

    /**
     * Modification of non-attribute property should go OK.
     */
    @Test
    public void test503ModifyProtectedAccountShadowProperty() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(ShadowType.class, ACCOUNT_DAEMON_OID,
                        ShadowType.F_SYNCHRONIZATION_SITUATION, SynchronizationSituationType.DISPUTED);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        try {

            // WHEN
            when();

            provisioningService.deleteObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, null, task, result);

            AssertJUnit.fail("Expected security exception while deleting 'daemon' account");
        } catch (SecurityViolationException e) {
            then();
            displayExpectedException(e);
        }

        assertFailure(result);

        syncServiceMock.assertNotifyFailureOnly();

//        checkConsistency();

        assertSteadyResource();
    }

    @Test
    public void test510AddProtectedAccounts() throws Exception {
        // GIVEN
        testAddProtectedAccount("Xavier");
        testAddProtectedAccount("Xenophobia");
        testAddProtectedAccount("nobody-adm");
        testAddAccount("abcadm");
        testAddAccount("piXel");
        testAddAccount("supernaturalius");
    }

    @Test
    public void test511AddProtectedAccountCaseIgnore() throws Exception {
        // GIVEN
        testAddAccount("xaxa");
        testAddAccount("somebody-ADM");
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

    protected void testAddProtectedAccount(String username) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadow = createAccountShadow(username);

        // WHEN
        try {
            provisioningService.addObject(shadow, null, null, task, result);
            AssertJUnit.fail("Expected security exception while adding '" + username + "' account");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        display("addObject result (expected failure)", result);
        TestUtil.assertFailure(result);

        syncServiceMock.assertNotifyFailureOnly();

        assertSteadyResource();
    }

    private void testAddAccount(String username) throws Exception {
        OperationResult result = createSubresult("addAccount");
        syncServiceMock.reset();

        PrismObject<ShadowType> shadow = createAccountShadow(username);

        // WHEN
        provisioningService.addObject(shadow, null, null, getTestTask(), result);

        result.computeStatus();
        display("addObject result (expected failure)", result);
        TestUtil.assertSuccess(result);

        syncServiceMock.assertNotifySuccessOnly();

        assertSteadyResource();
    }

    /**
     * Make sure that refresh of the shadow adds missing primaryIdentifierValue
     * to the shadow.
     * This is a test for migration from previous midPoint versions that haven't
     * had primaryIdentifierValue.
     */
    @Test
    public void test520MigrationPrimaryIdentifierValueRefresh() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = PrismTestUtil.parseObject(ACCOUNT_RELIC_FILE);
        repositoryService.addObject(shadowBefore, null, result);

        // WHEN
        when();

        provisioningService.refreshShadow(shadowBefore, null, task, result);

        then();
        assertSuccess(result);

        assertRepoShadow(ACCOUNT_RELIC_OID)
                .assertName(ACCOUNT_RELIC_USERNAME)
                .assertPrimaryIdentifierValue(ACCOUNT_RELIC_USERNAME);

        assertSteadyResource();
    }

    /**
     * Test for proper handling of "already exists" exception. We try to add a shadow.
     * It fails, because there is unknown conflicting object on the resource. But a new
     * shadow for the conflicting object should be created in the repository.
     * MID-3603
     */
    @Test
    public void test600AddAccountAlreadyExist() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResourceCtl.addAccount(ACCOUNT_MURRAY_USERNAME, ACCOUNT_MURRAY_USERNAME);

        PrismObject<ShadowType> account = createShadowNameOnly(resource, ACCOUNT_MURRAY_USERNAME);
        account.checkConsistence();

        display("Adding shadow", account);

        // WHEN
        when();
        try {
            provisioningService.addObject(account, null, null, task, result);

            assertNotReached();
        } catch (ObjectAlreadyExistsException e) {
            then();
            displayExpectedException(e);
        }

        // THEN
        assertFailure(result);

        syncServiceMock.assertNotifyChange();

        // Even though the operation failed a shadow should be created for the conflicting object
        PrismObject<ShadowType> accountRepo = findAccountShadowByUsername(getMurrayRepoIcfName(), resource, result);
        assertNotNull("Shadow for conflicting object was not created in the repository", accountRepo);
        display("Repository shadow", accountRepo);
        checkRepoAccountShadow(accountRepo);

        assertEquals("Wrong ICF NAME in murray (repo) shadow", getMurrayRepoIcfName(), getIcfName(accountRepo));

        assertSteadyResource();
    }

    static Task syncTokenTask = null;

    @Test
    public void test800LiveSyncInit() throws Exception {
        syncTokenTask = taskManager.createTaskInstance(TestDummy.class.getName() + ".syncTask");

        dummyResource.setSyncStyle(DummySyncStyle.DUMB);
        dummyResource.clearDeltas();
        syncServiceMock.reset();

        OperationResult result = new OperationResult(TestDummy.class.getName()
                + ".test800LiveSyncInit");

        // Dry run to remember the current sync token in the task instance.
        // Otherwise a last sync token whould be used and
        // no change would be detected
        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

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
        // GIVEN
        Task task = getTestTask();
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

        displayValue("Resource before sync", dummyResource.debugDump());

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        result.computeStatus();
        display("Synchronization result", result);
        TestUtil.assertSuccess("Synchronization result is not OK", result);

        syncServiceMock.assertNotifyChange();

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

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
        // GIVEN
        OperationResult result = createOperationResult();

        syncServiceMock.reset();

        DummyAccount dummyAccount = getDummyAccountAssert(BLACKBEARD_USERNAME, blackbeardIcfUid);
        dummyAccount.replaceAttributeValue("fullname", "Captain Blackbeard");

        displayValue("Resource before sync", dummyResource.debugDump());

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        result.computeStatus();
        display("Synchronization result", result);
        TestUtil.assertSuccess("Synchronization result is not OK", result);

        syncServiceMock.assertNotifyChange();

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

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
        testLiveSyncAddDrake(DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
    }

    @Test
    public void test812LiveSyncModifyDrakeDumbObjectClass() throws Exception {
        testLiveSyncModifyDrake(DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
    }

    @Test
    public void test815LiveSyncAddCorsairsDumbObjectClass() throws Exception {
        testLiveSyncAddCorsairs(DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
    }

    @Test
    public void test817LiveSyncDeleteCorsairsDumbObjectClass() throws Exception {
        testLiveSyncDeleteCorsairs(DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
    }

    @Test
    public void test819LiveSyncDeleteDrakeDumbObjectClass() throws Exception {
        testLiveSyncDeleteDrake(DummySyncStyle.DUMB, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
    }

    @Test
    public void test820LiveSyncAddDrakeSmartObjectClass() throws Exception {
        testLiveSyncAddDrake(DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
    }

    @Test
    public void test822LiveSyncModifyDrakeSmartObjectClass() throws Exception {
        testLiveSyncModifyDrake(DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
    }

    @Test
    public void test825LiveSyncAddCorsairsSmartObjectClass() throws Exception {
        testLiveSyncAddCorsairs(DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
    }

    @Test
    public void test827LiveSyncDeleteCorsairsSmartObjectClass() throws Exception {
        testLiveSyncDeleteCorsairs(DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), false);
    }

    @Test
    public void test829LiveSyncDeleteDrakeSmartObjectClass() throws Exception {
        testLiveSyncDeleteDrake(DummySyncStyle.SMART, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));
    }

    @Test
    public void test830LiveSyncAddDrakeDumbAny() throws Exception {
        testLiveSyncAddDrake(DummySyncStyle.DUMB, null);
    }

    @Test
    public void test832LiveSyncModifyDrakeDumbAny() throws Exception {
        testLiveSyncModifyDrake(DummySyncStyle.DUMB, null);
    }

    @Test
    public void test835LiveSyncAddCorsairsDumbAny() throws Exception {
        testLiveSyncAddCorsairs(DummySyncStyle.DUMB, null, true);
    }

    @Test
    public void test837LiveSyncDeleteCorsairsDumbAny() throws Exception {
        testLiveSyncDeleteCorsairs(DummySyncStyle.DUMB, null, true);
    }

    @Test
    public void test839LiveSyncDeleteDrakeDumbAny() throws Exception {
        testLiveSyncDeleteDrake(DummySyncStyle.DUMB, null);
    }

    @Test
    public void test840LiveSyncAddDrakeSmartAny() throws Exception {
        testLiveSyncAddDrake(DummySyncStyle.SMART, null);
    }

    @Test
    public void test842LiveSyncModifyDrakeSmartAny() throws Exception {
        testLiveSyncModifyDrake(DummySyncStyle.SMART, null);
    }

    @Test
    public void test845LiveSyncAddCorsairsSmartAny() throws Exception {
        testLiveSyncAddCorsairs(DummySyncStyle.SMART, null, true);
    }

    @Test
    public void test847LiveSyncDeleteCorsairsSmartAny() throws Exception {
        testLiveSyncDeleteCorsairs(DummySyncStyle.SMART, null, true);
    }

    @Test
    public void test849LiveSyncDeleteDrakeSmartAny() throws Exception {
        testLiveSyncDeleteDrake(DummySyncStyle.SMART, null);
    }

    public void testLiveSyncAddDrake(DummySyncStyle syncStyle, QName objectClass) throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        syncServiceMock.reset();
        dummyResource.setSyncStyle(syncStyle);
        DummyAccount newAccount = new DummyAccount(DRAKE_USERNAME);
        newAccount.addAttributeValues("fullname", "Sir Francis Drake");
        newAccount.setEnabled(true);
        newAccount.setPassword("avast!");
        dummyResource.addAccount(newAccount);
        drakeIcfUid = newAccount.getId();

        displayValue("Resource before sync", dummyResource.debugDump());

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                objectClass);

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        then();
        result.computeStatus();
        display("Synchronization result", result);
        TestUtil.assertSuccess("Synchronization result is not OK", result);

        syncServiceMock.assertNotifyChange();

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
        assertNotNull("Old shadow missing", oldShadow);
        assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

        if (syncStyle == DummySyncStyle.DUMB) {
            assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
        } else {
            ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
            assertNotNull("Delta present when not expecting it", objectDelta);
            assertTrue("Delta is not add: " + objectDelta, objectDelta.isAdd());
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

    public void testLiveSyncModifyDrake(DummySyncStyle syncStyle, QName objectClass) throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        syncServiceMock.reset();
        dummyResource.setSyncStyle(syncStyle);

        DummyAccount dummyAccount = getDummyAccountAssert(DRAKE_USERNAME, drakeIcfUid);
        dummyAccount.replaceAttributeValue("fullname", "Captain Drake");

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                objectClass);

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        then();
        result.computeStatus();
        display("Synchronization result", result);
        TestUtil.assertSuccess("Synchronization result is not OK", result);

        syncServiceMock.assertNotifyChange();

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

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

    public void testLiveSyncAddCorsairs(
            DummySyncStyle syncStyle, QName objectClass, boolean expectReaction) throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        syncServiceMock.reset();
        dummyResource.setSyncStyle(syncStyle);
        DummyGroup newGroup = new DummyGroup(GROUP_CORSAIRS_NAME);
        newGroup.setEnabled(true);
        dummyResource.addGroup(newGroup);
        corsairsIcfUid = newGroup.getId();

        displayValue("Resource before sync", dummyResource.debugDump());

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                objectClass);

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        then();
        result.computeStatus();
        display("Synchronization result", result);
        TestUtil.assertSuccess("Synchronization result is not OK", result);

        if (expectReaction) {

            syncServiceMock.assertNotifyChange();

            ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
            displayDumpable("The change", lastChange);

            PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
            assertNotNull("Old shadow missing", oldShadow);
            assertNotNull("Old shadow does not have an OID", oldShadow.getOid());

            if (syncStyle == DummySyncStyle.DUMB) {
                assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
            } else {
                ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
                assertNotNull("Delta present when not expecting it", objectDelta);
                assertTrue("Delta is not add: " + objectDelta, objectDelta.isAdd());
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

    public void testLiveSyncDeleteCorsairs(
            DummySyncStyle syncStyle, QName objectClass, boolean expectReaction) throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        syncServiceMock.reset();
        dummyResource.setSyncStyle(syncStyle);
        if (isNameUnique()) {
            dummyResource.deleteGroupByName(GROUP_CORSAIRS_NAME);
        } else {
            dummyResource.deleteGroupById(corsairsIcfUid);
        }

        displayValue("Resource before sync", dummyResource.debugDump());

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                objectClass);

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        then();
        result.computeStatus();
        display("Synchronization result", result);
        TestUtil.assertSuccess("Synchronization result is not OK", result);

        if (expectReaction) {

            syncServiceMock.assertNotifyChange();

            syncServiceMock
                    .lastNotifyChange()
                    .display()
                    .oldShadow()
                    .assertOid(corsairsShadowOid)
                    .attributes()
                    .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID)
                    .assertValue(SchemaConstants.ICFS_NAME, GROUP_CORSAIRS_NAME)
                    .end()
                    .end()
                    .delta()
                    .assertChangeType(ChangeType.DELETE)
                    .assertObjectTypeClass(ShadowType.class)
                    .assertOid(corsairsShadowOid)
                    .end()
                    .currentShadow()
                    .assertOid(corsairsShadowOid)
                    .assertTombstone();

            assertRepoShadow(corsairsShadowOid)
                    .assertTombstone();

            // Clean slate for next tests
            repositoryService.deleteObject(ShadowType.class, corsairsShadowOid, result);

        } else {
            syncServiceMock.assertNoNotifyChange();
        }

        checkAllShadows();

        assertSteadyResource();
    }

    private void testLiveSyncDeleteDrake(
            DummySyncStyle syncStyle, QName objectClass) throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        syncServiceMock.reset();
        dummyResource.setSyncStyle(syncStyle);
        if (isNameUnique()) {
            dummyResource.deleteAccountByName(DRAKE_USERNAME);
        } else {
            dummyResource.deleteAccountById(drakeIcfUid);
        }

        displayValue("Resource before sync", dummyResource.debugDump());

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                objectClass);

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        then();
        result.computeStatus();
        display("Synchronization result", result);
        TestUtil.assertSuccess("Synchronization result is not OK", result);

        syncServiceMock.assertNotifyChange();

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
        assertSyncOldShadow(oldShadow, getDrakeRepoIcfName());

        syncServiceMock
                .lastNotifyChange()
                .delta()
                .assertChangeType(ChangeType.DELETE)
                .assertObjectTypeClass(ShadowType.class)
                .assertOid(drakeAccountOid)
                .end()
                .currentShadow()
                .assertTombstone()
                .assertOid(drakeAccountOid);

        assertRepoShadow(drakeAccountOid)
                .assertTombstone();

        checkAllShadows();

        // Clean slate for next tests
        repositoryService.deleteObject(ShadowType.class, drakeAccountOid, result);

        assertSteadyResource();
    }

    @Test
    public void test890LiveSyncModifyProtectedAccount() throws Exception {
        // GIVEN
        Task syncTask = getTestTask();
        OperationResult result = syncTask.getResult();

        syncServiceMock.reset();

        DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_DAEMON_USERNAME, daemonIcfUid);
        dummyAccount.replaceAttributeValue("fullname", "Maxwell deamon");

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID,
                ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType));

        // WHEN
        when();
        provisioningService.synchronize(coords, syncTokenTask, null, result);

        // THEN
        then();
        display("Synchronization result", result);
        assertSuccess(result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        syncServiceMock.assertNoNotifyChange();

        checkAllShadows();

        assertSteadyResource();
    }

    @Test
    public void test901FailResourceNotFound() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

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

//        assertSteadyResource();
    }

    // test999 shutdown in the superclass

    protected void checkCachedAccountShadow(PrismObject<ShadowType> shadowType, OperationResult parentResult, boolean fullShadow, XMLGregorianCalendar startTs,
            XMLGregorianCalendar endTs) throws SchemaException {
        checkAccountShadow(shadowType, parentResult, fullShadow);
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

    private void checkAllShadows() throws SchemaException {
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
            assertEquals("Unexpected number of attributes", (int) expectedNumberOfAttributes, attributes.size());
        }
        ResourceAttribute<?> icfsNameAttribute = attributesContainer.findAttribute(SchemaConstants.ICFS_NAME);
        assertNotNull("No ICF name attribute in old  shadow", icfsNameAttribute);
        assertEquals("Wrong value of ICF name attribute in old  shadow", repoName,
                icfsNameAttribute.getRealValue());
    }

}
