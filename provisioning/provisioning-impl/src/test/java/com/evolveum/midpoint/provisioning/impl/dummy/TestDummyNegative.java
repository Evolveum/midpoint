/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.identityconnectors.framework.common.objects.OperationalAttributes.ENABLE_DATE_NAME;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Tests the behavior of provisioning module under erroneous conditions
 * (including invalid API calls).
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyNegative extends AbstractDummyTest {

    private static final File ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILE =
            new File(TEST_DIR, "account-elaine-resource-not-found.xml");

    private static final String ATTR_NUMBER = "number";

    private static final DummyTestResource RESOURCE_DUMMY_BROKEN_ACCOUNTS = new DummyTestResource(
            TEST_DIR, "resource-dummy-broken-accounts.xml", "202db5cf-f3c2-437c-9354-64054343d37d", "broken-accounts",
            controller -> {
                // This gives us a potential to induce exceptions during ConnId->object conversion.
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ENABLE_DATE_NAME, Long.class, false, false);

                // This is a secondary identifier which gives us a potential to induce exceptions during repo shadow manipulation.
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_NUMBER, Integer.class, false, false);
            }
    );
    private static final String GOOD_ACCOUNT = "good";
    private static final String INCONVERTIBLE_ACCOUNT = "inconvertible";
    private static final String UNSTORABLE_ACCOUNT = "unstorable";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_BROKEN_ACCOUNTS, initResult);
        testResourceAssertSuccess(RESOURCE_DUMMY_BROKEN_ACCOUNTS.oid, initTask);
    }

    //region Tests for broken schema (in various ways)
    @Test
    public void test110GetResourceBrokenSchemaNetwork() throws Exception {
        testGetResourceBrokenSchema(BreakMode.NETWORK);
    }

    @Test
    public void test111GetResourceBrokenSchemaGeneric() throws Exception {
        testGetResourceBrokenSchema(BreakMode.GENERIC);
    }

    @Test
    public void test112GetResourceBrokenSchemaIo() throws Exception {
        testGetResourceBrokenSchema(BreakMode.IO);
    }

    @Test
    public void test113GetResourceBrokenSchemaRuntime() throws Exception {
        testGetResourceBrokenSchema(BreakMode.RUNTIME);
    }

    private void testGetResourceBrokenSchema(BreakMode breakMode) throws Exception {
        given();
        OperationResult result = createOperationResult();

        // precondition
        PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        display("Repo resource (before)", repoResource);
        PrismContainer<Containerable> schema = repoResource.findContainer(ResourceType.F_SCHEMA);
        assertTrue("Schema found in resource before the test (precondition)", schema == null || schema.isEmpty());

        dummyResource.setSchemaBreakMode(breakMode);
        try {

            when();
            PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, null, result);

            then();
            display("Resource with broken schema", resource);
            OperationResultType fetchResult = resource.asObjectable().getFetchResult();

            result.computeStatus();
            display("getObject result", result);
            assertEquals("Unexpected result of getObject operation", OperationResultStatus.PARTIAL_ERROR, result.getStatus());

            assertNotNull("No fetch result", fetchResult);
            display("fetchResult", fetchResult);
            assertEquals("Unexpected result of fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());

        } finally {
            dummyResource.setSchemaBreakMode(BreakMode.NONE);
        }
    }

    /**
     * Finally, no errors! Here we simply get a resource with no obstacles.
     * This also prepares the stage for further tests.
     */
    @Test
    public void test190GetResource() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyResource.setSchemaBreakMode(BreakMode.NONE);
        syncServiceMock.reset();

        when();
        PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        display("Resource after", resource);
        IntegrationTestTools.displayXml("Resource after (XML)", resource);
        assertHasSchema(resource, "dummy");
    }
    //endregion

    //region Tests for adding/removing/getting/searching for broken accounts (in various ways)
    @Test
    public void test200AddAccountNullAttributes() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();
        account.removeContainer(ShadowType.F_ATTRIBUTES);
        display("Adding shadow", account);

        try {
            when();
            provisioningService.addObject(account, null, null, task, result);

            assertNotReached();
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        then();
        syncServiceMock.assertSingleNotifyFailureOnly();
    }

    @Test
    public void test201AddAccountEmptyAttributes() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();

        account.findContainer(ShadowType.F_ATTRIBUTES).getValue().clear();

        display("Adding shadow", account);

        try {
            when();
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        then();
        syncServiceMock.assertSingleNotifyFailureOnly();
    }

    @Test
    public void test210AddAccountNoObjectClass() throws Exception {
        given();
        Task task =getTestTask();
        OperationResult result = getTestOperationResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();

        // IMPORTANT: deliberately violating the schema
        accountType.setObjectClass(null);
        accountType.setKind(null);

        display("Adding shadow", account);

        try {
            when();
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        then();
        syncServiceMock.assertSingleNotifyFailureOnly();
    }

    /**
     * Adding an account without resourceRef.
     */
    @Test
    public void test220AddAccountNoResourceRef() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();

        accountType.setResourceRef(null);

        display("Adding shadow", account);

        try {
            when();
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        //FIXME: not sure, if this check is needed..if the resource is not specified, provisioning probably will be not called.
//        syncServiceMock.assertNotifyFailureOnly();
    }

    /**
     * Deleting an account with resourceRef pointing to non-existent resource.
     */
    @Test
    public void test221DeleteAccountResourceNotFound() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILE);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();

        display("Adding shadow", account);

        try {
            when();
            String oid = repositoryService.addObject(account, null, result);
            ProvisioningOperationOptions options = ProvisioningOperationOptions.createForce(true);
            provisioningService.deleteObject(ShadowType.class, oid, options, null, task, result);
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        //FIXME: is this really notify failure? the resource does not exist but shadow is deleted. maybe other case of notify?
//        syncServiceMock.assertNotifyFailureOnly();
    }

    /**
     * Try to get an account when a shadow has been deleted (but the account exists).
     * Proper ObjectNotFoundException is expected, compensation should not run.
     */
    @Test
    public void test230GetAccountDeletedShadow() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_MORGAN_FILE);
        String shadowOid = provisioningService.addObject(account, null, null, task, result);

        repositoryService.deleteObject(ShadowType.class, shadowOid, result);

        syncServiceMock.reset();

        try {
            when();
            provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);

            assertNotReached();
        } catch (ObjectNotFoundException e) {
            displayExpectedException(e);
        }

        then();
        assertFailure(result);

        syncServiceMock.assertNoNotifyChange();
    }

    /**
     * Checks the behaviour when getting broken accounts, i.e. accounts that cannot be retrieved
     * because of e.g.
     * - inability to convert from ConnId to resource object (ShadowType)
     * - inability to create or update midPoint shadow
     *
     * The current behavior is that getObject throws an exception in these cases.
     * This may or may not be ideal. We can consider changing that (to signalling via fetchResult)
     * later. But that would mean adapting the clients so that they would check the fetchResult
     * and use the resulting shadow only if it's OK.
     *
     * ---
     * Because the getObject operation requires a shadow to exists, we have to create these objects
     * in a good shape, retrieve them, and break them afterwards.
     */
    @Test
    public void test240GetBrokenAccounts() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        DummyResourceContoller controller = RESOURCE_DUMMY_BROKEN_ACCOUNTS.controller;
        DummyResource resource = controller.getDummyResource();

        // create good accounts
        createAccount(GOOD_ACCOUNT, 1, null);
        createAccount(INCONVERTIBLE_ACCOUNT, 2, null);
        createAccount(UNSTORABLE_ACCOUNT, 3, null);

        // here we create the shadows
        SearchResultList<PrismObject<ShadowType>> accounts =
                provisioningService.searchObjects(ShadowType.class, getAllAccountsQuery(RESOURCE_DUMMY_BROKEN_ACCOUNTS),
                        null, task, result);
        String goodOid = selectAccount(accounts, GOOD_ACCOUNT).getOid();
        String inconvertibleOid = selectAccount(accounts, INCONVERTIBLE_ACCOUNT).getOid();
        String unstorableOid = selectAccount(accounts, UNSTORABLE_ACCOUNT).getOid();

        // break the accounts
        resource.getAccountByUsername(INCONVERTIBLE_ACCOUNT).replaceAttributeValue(ENABLE_DATE_NAME, "WRONG");
        resource.getAccountByUsername(UNSTORABLE_ACCOUNT).replaceAttributeValue(ATTR_NUMBER, "WRONG");

        when(GOOD_ACCOUNT);
        PrismObject<ShadowType> goodReloaded = provisioningService.getObject(ShadowType.class, goodOid, null, task, result);

        then(GOOD_ACCOUNT);
        assertShadow(goodReloaded, GOOD_ACCOUNT)
                .assertSuccessOrNoFetchResult();

        when(INCONVERTIBLE_ACCOUNT);
        try {
            provisioningService.getObject(ShadowType.class, inconvertibleOid, null, task, result);
            assertNotReached();
        } catch (SchemaException e) {
            then(INCONVERTIBLE_ACCOUNT);
            displayExpectedException(e);

            // Note: this is the current implementation. We might change it to return something,
            // and fill-in fetchResult appropriately.
        }

        when(UNSTORABLE_ACCOUNT);
        try {
            provisioningService.getObject(ShadowType.class, unstorableOid, null, task, result);
            assertNotReached();
        } catch (Exception e) {
            then(UNSTORABLE_ACCOUNT);
            displayExpectedException(e);

            // Note: this is the current implementation. We might change it to return something,
            // and fill-in fetchResult appropriately.
        }
    }

    @Test
    public void test250SearchForBrokenAccounts() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        cleanupAccounts(RESOURCE_DUMMY_BROKEN_ACCOUNTS, result);

        createAccount(GOOD_ACCOUNT, 1, null);
        createAccount(INCONVERTIBLE_ACCOUNT, 2, "WRONG");
        createAccount(UNSTORABLE_ACCOUNT, "WRONG", null);

        when();

        List<PrismObject<ShadowType>> objects = new ArrayList<>();

        ResultHandler<ShadowType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };
        Collection<SelectorOptions<GetOperationOptions>> options =
                schemaHelper.getOperationOptionsBuilder()
                        .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                        .build();
        provisioningService.searchObjectsIterative(ShadowType.class, getAllAccountsQuery(RESOURCE_DUMMY_BROKEN_ACCOUNTS),
                options, handler, task, result);

        then();
        display("objects", objects);
        assertThat(objects.size()).as("objects found").isEqualTo(3);

        // TODO asserts on the result and object content
    }

    private PrismObject<ShadowType> selectAccount(SearchResultList<PrismObject<ShadowType>> accounts, String name) {
        return accounts.stream()
                .filter(a -> name.equals(a.getName().getOrig()))
                .findAny()
                .orElseThrow(() -> new AssertionError("Account '" + name + "' was not found"));
    }

    private void createAccount(String name, Object number, Object enableDate) throws Exception {
        DummyAccount account = RESOURCE_DUMMY_BROKEN_ACCOUNTS.controller.addAccount(name);
        account.addAttributeValue(ATTR_NUMBER, number);
        account.addAttributeValue(ENABLE_DATE_NAME, enableDate);
    }
    //endregion
}
