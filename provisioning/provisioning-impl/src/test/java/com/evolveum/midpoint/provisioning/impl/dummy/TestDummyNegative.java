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
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.provisioning.api.LiveSyncEvent;
import com.evolveum.midpoint.provisioning.api.LiveSyncEventHandler;
import com.evolveum.midpoint.provisioning.impl.DummyTokenStorageImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
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
            TestDummyNegative::addFragileAttributes
    );

    private static final DummyTestResource RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID = new DummyTestResource(
            TEST_DIR, "resource-dummy-broken-accounts-external-uid.xml", "6139ea00-fc2a-4a68-b830-3e4012c766ee", "broken-accounts-external-uid",
            TestDummyNegative::addFragileAttributes
    );

    private static final TestResource RESOURCE_WITH_BROKEN_CONNECTOR_REF = TestResource.file(
            TEST_DIR, "resource-with-broken-connector-ref.xml", "fe1de27d-684c-44c8-9cf6-a691fdf392fd");
    private static final TestResource RESOURCE_TEMPLATE_WITH_BROKEN_CONNECTOR_REF = TestResource.file(
            TEST_DIR, "resource-template-with-broken-connector-ref.xml", "c41900ce-6b86-476a-a7c9-6eb797f7d405");
    private static final TestResource RESOURCE_INSTANCE_WITH_BROKEN_CONNECTOR_REF = TestResource.file(
            TEST_DIR, "resource-instance-with-broken-connector-ref.xml", "201e83f6-184e-46b0-92ec-16d1b639f6cb");

    private static final DummyTestResource RESOURCE_DUMMY_BROKEN_ATTRIBUTE_DEF = new DummyTestResource(
            TEST_DIR, "resource-dummy-broken-attribute-def.xml", "04e54cf7-87df-4b19-9548-fc256b7d585a",
            "broken-attribute-def");

    private static void addFragileAttributes(DummyResourceContoller controller)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        // This gives us a potential to induce exceptions during ConnId->object conversion.
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ENABLE_DATE_NAME, Long.class, false, false);

        // This is a secondary identifier which gives us a potential to induce exceptions during repo shadow manipulation.
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_NUMBER, Integer.class, false, false);
    }

    @Autowired RepositoryService repositoryService;

    /** This is an account that can be processed without any issues. */
    private static final String GOOD_ACCOUNT = "good";

    /** This one gets the "enable date" with the unparseable value of `WRONG`, hence ConnId->midPoint conversion will fail. */
    private static final String INCONVERTIBLE_ACCOUNT = "inconvertible";

    /**
     * This one gets the "number" with the unparseable value of `WRONG`, hence the processing will fail.
     * (Repo manipulation before 4.9, and internal processing in 4.9 and later. The reason is that more strict checks
     * were added in 4.9. The name of the account - "unstorable" - is thus no longer precise enough.)
     */
    private static final String UNSTORABLE_ACCOUNT = "unstorable";

    /**
     * Shadow for this account cannot be stored in the generic repo, as the name is longer than 255 characters. The native
     * repo (sqale) can store it, though.
     */
    private static final String TOTALLY_UNSTORABLE_ACCOUNT =
            "totally-unstorable" + StringUtils.repeat("-123456789", 30);

    private static final String EXTERNAL_UID_PREFIX = "uid:";
    private static final String GOOD_ACCOUNT_UID = EXTERNAL_UID_PREFIX + GOOD_ACCOUNT;
    private static final String INCONVERTIBLE_ACCOUNT_UID = EXTERNAL_UID_PREFIX + INCONVERTIBLE_ACCOUNT;
    private static final String UNSTORABLE_ACCOUNT_UID = EXTERNAL_UID_PREFIX + UNSTORABLE_ACCOUNT;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_BROKEN_ACCOUNTS, initResult);
        testResourceAssertSuccess(RESOURCE_DUMMY_BROKEN_ACCOUNTS, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID, initResult);
        testResourceAssertSuccess(RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID, initTask, initResult);

        RESOURCE_WITH_BROKEN_CONNECTOR_REF.initRaw(this, initResult);
        RESOURCE_TEMPLATE_WITH_BROKEN_CONNECTOR_REF.initRaw(this, initResult);
        RESOURCE_INSTANCE_WITH_BROKEN_CONNECTOR_REF.initRaw(this, initResult);

        initDummyResource(RESOURCE_DUMMY_BROKEN_ATTRIBUTE_DEF, initResult); // intentionally not executing the test
    }

    //region Broken connectorRef

    /** MID-8619: resource with broken `connectorRef` must be gettable. */
    @Test
    public void test100GetResourceWithBrokenConnectorRef() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("resource with broken connectorRef is retrieved");
        var resource = provisioningService
                .getObject(ResourceType.class, RESOURCE_WITH_BROKEN_CONNECTOR_REF.oid, null, task, result)
                .asObjectable();

        Consumer<OperationResult> asserter =
                r -> assertThatOperationResult(r)
                        .isPartialError()
                        .hasMessageContaining("An error occurred while applying connector schema")
                        .hasMessageContaining("Object of type 'ConnectorType' with OID 'cb4bcb48-4325-405d-a4a4-260de5640232' was not found.");

        then("the result is partial error");
        asserter.accept(result);

        and("fetchResult is the same");
        asserter.accept(OperationResult.createOperationResult(resource.getFetchResult()));
    }

    /** MID-8619: resource template with broken `connectorRef` must be gettable. */
    @Test
    public void test103GetResourceTemplateWithBrokenConnectorRef() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("resource template with broken connectorRef is retrieved");
        var resource = provisioningService
                .getObject(ResourceType.class, RESOURCE_TEMPLATE_WITH_BROKEN_CONNECTOR_REF.oid, null, task, result)
                .asObjectable();

        then("the result is success (there's no reason to get the connector)");
        assertSuccess(result);

        and("it returns without fetchResult");
        assertThat(resource.getFetchResult()).as("fetch result").isNull();
    }

    /** MID-8619: resource instance pointing to a template with broken `connectorRef` must be gettable. */
    @Test
    public void test106GetResourceInstanceWithBrokenConnectorRef() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("instance of resource template with broken connectorRef is retrieved");
        var resource = provisioningService
                .getObject(ResourceType.class, RESOURCE_INSTANCE_WITH_BROKEN_CONNECTOR_REF.oid, null, task, result)
                .asObjectable();

        Consumer<OperationResult> asserter =
                r -> assertThatOperationResult(r)
                        .isPartialError()
                        .hasMessageContaining("An error occurred while expanding super-resource references")
                        .hasMessageContaining("Object of type 'ConnectorType' with OID 'cb4bcb48-4325-405d-a4a4-260de5640232' was not found.");

        then("the result is partial error");
        asserter.accept(result);

        and("fetchResult is the same");
        asserter.accept(OperationResult.createOperationResult(resource.getFetchResult()));
    }
    //endregion

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
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        // precondition
        PrismObject<ResourceType> repoResource =
                repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        display("Repo resource (before)", repoResource);
        PrismContainer<Containerable> schema = repoResource.findContainer(ResourceType.F_SCHEMA);
        assertTrue("Schema found in resource before the test (precondition)", schema == null || schema.isEmpty());

        dummyResource.setSchemaBreakMode(breakMode);
        try {

            when();
            PrismObject<ResourceType> resource =
                    provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

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
        PrismObject<ResourceType> resource =
                provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

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
            assertExpectedException(e)
                    .hasMessageContaining("without any attributes nor associations");
        }
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
    }

    @Test
    public void test210AddAccountNoObjectClass() throws Exception {
        given();
        Task task = getTestTask();
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
        } catch (IllegalArgumentException | NullPointerException e) {
            // Exception may vary depending on whether we run with @NotNull annotations checked
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
     *
     * - inability to convert from ConnId to resource object (`ShadowType`)
     * - inability to create or update midPoint shadow
     *
     * The current behavior is that `getObject` throws an exception in these cases.
     * This may or may not be ideal. We can consider changing that (to signalling via `fetchResult`)
     * later. But that would mean adapting the clients so that they would check the `fetchResult`
     * and use the resulting shadow only if it's OK.
     *
     * Because the `getObject` operation requires a shadow to exists, we have to create these objects
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
        String goodOid = selectAccountByName(accounts, GOOD_ACCOUNT).getOid();
        String inconvertibleOid = selectAccountByName(accounts, INCONVERTIBLE_ACCOUNT).getOid();
        String unstorableOid = selectAccountByName(accounts, UNSTORABLE_ACCOUNT).getOid();

        // break the accounts
        resource.getAccountByName(INCONVERTIBLE_ACCOUNT).replaceAttributeValue(ENABLE_DATE_NAME, "WRONG");
        resource.getAccountByName(UNSTORABLE_ACCOUNT).replaceAttributeValue(ATTR_NUMBER, "WRONG");

        when(GOOD_ACCOUNT);
        PrismObject<ShadowType> goodReloaded =
                provisioningService.getObject(ShadowType.class, goodOid, null, task, result);

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
        createAccount(TOTALLY_UNSTORABLE_ACCOUNT, 4, null);

        when();

        List<PrismObject<ShadowType>> objects = new ArrayList<>();

        ResultHandler<ShadowType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };
        provisioningService.searchObjectsIterative(
                ShadowType.class, getAllAccountsQuery(RESOURCE_DUMMY_BROKEN_ACCOUNTS),
                createNoExceptionOptions(), handler, task, result);

        then();
        display("objects", objects);
        assertThat(objects.size()).as("objects found").isEqualTo(4);

        assertSelectedAccountByName(objects, GOOD_ACCOUNT)
                .assertOid()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIndexedPrimaryIdentifierValue(GOOD_ACCOUNT)
                .attributes()
                    .assertSize(3)
                    .end()
                .assertSuccessOrNoFetchResult();

        PrismObject<ShadowType> goodAfter = findShadowByPrismName(GOOD_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS.get(), result);
        assertShadow(goodAfter, GOOD_ACCOUNT)
                .display()
                .assertOid()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIndexedPrimaryIdentifierValue(GOOD_ACCOUNT)
                .attributes()
                    .assertSize(3)
                    .end();

        assertSelectedAccountByName(objects, INCONVERTIBLE_ACCOUNT)
                .display()
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(INCONVERTIBLE_ACCOUNT)
                .attributes()
                    .assertSize(3) // uid=inconvertible + name=inconvertible + number=2
                    .end()
                .assertFetchResult(OperationResultStatusType.FATAL_ERROR, "Couldn't convert resource object", INCONVERTIBLE_ACCOUNT);
                // (maybe it's not necessary to provide account attributes in the message - reconsider)

        PrismObject<ShadowType> inconvertibleAfter =
                findShadowByPrismName(INCONVERTIBLE_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS.get(), result);
        assertShadow(inconvertibleAfter, INCONVERTIBLE_ACCOUNT)
                .display()
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(INCONVERTIBLE_ACCOUNT)
                .attributes()
                    .assertSize(3)
                    .end();

        assertSelectedAccountByName(objects, UNSTORABLE_ACCOUNT)
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(UNSTORABLE_ACCOUNT)
                .attributes()
                    .assertSize(2) // uid=unstorable, name=unstorable
                    .end()
                .assertFetchResult(OperationResultStatusType.FATAL_ERROR, "WRONG");
                // (maybe it's not necessary to provide the unconvertible value in the message - reconsider)

        PrismObject<ShadowType> unstorableAfter =
                findShadowByPrismName(UNSTORABLE_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS.get(), result);
        assertShadow(unstorableAfter, UNSTORABLE_ACCOUNT)
                .display()
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(UNSTORABLE_ACCOUNT)
                .attributes()
                    .assertSize(2) // this is the result of the emergency shadow acquisition
                    .end();

        var asserter = assertSelectedAccountByName(objects, TOTALLY_UNSTORABLE_ACCOUNT);
        if (isSqaleRepository()) {
            asserter.assertOid(); // The shadow is in repo, since sqale is able to store large items
        } else {
            asserter.assertNoOid() // Generic repo cannot store names with more than 255 characters
                .assertFetchResult(OperationResultStatusType.FATAL_ERROR, "Error while committing the transaction");
        }
        // Primary identifier value is not here, because it is set as part of object shadowization (which failed)
        asserter.attributes()
                .assertSize(3) // number, name, uid
            .end();

    }

    @NotNull
    private Collection<SelectorOptions<GetOperationOptions>> createNoExceptionOptions() {
        return schemaService.getOperationOptionsBuilder()
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();
    }

    @Test
    public void test260SearchForBrokenAccountsExternalUid() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        cleanupAccounts(RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID, result);

        createAccountExternalUid(GOOD_ACCOUNT, 1, null);
        createAccountExternalUid(INCONVERTIBLE_ACCOUNT, 2, "WRONG");
        createAccountExternalUid(UNSTORABLE_ACCOUNT, "WRONG", null);
        createAccountExternalUid(TOTALLY_UNSTORABLE_ACCOUNT, 4, null);

        when();

        List<PrismObject<ShadowType>> objects = new ArrayList<>();

        ResultHandler<ShadowType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };
        Collection<SelectorOptions<GetOperationOptions>> options = createNoExceptionOptions();
        provisioningService.searchObjectsIterative(ShadowType.class, getAllAccountsQuery(RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID),
                options, handler, task, result);

        then();
        display("objects", objects);
        assertThat(objects.size()).as("objects found").isEqualTo(4);

        assertSelectedAccountByName(objects, GOOD_ACCOUNT)
                .assertOid()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIndexedPrimaryIdentifierValue(GOOD_ACCOUNT_UID)
                .assertName(GOOD_ACCOUNT)
                .attributes()
                    .assertSize(3)
                    .end()
                .assertSuccessOrNoFetchResult();

        PrismObject<ShadowType> goodAfter =
                findShadowByPrismName(GOOD_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.get(), result);
        assertShadow(goodAfter, GOOD_ACCOUNT)
                .display()
                .assertOid()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIndexedPrimaryIdentifierValue(GOOD_ACCOUNT_UID)
                .attributes()
                    .assertSize(3)
                    .end();

        assertSelectedAccountByName(objects, INCONVERTIBLE_ACCOUNT)
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(INCONVERTIBLE_ACCOUNT_UID)
                .assertName(INCONVERTIBLE_ACCOUNT)
                .attributes()
                    .assertSize(3) // uid=uid:inconvertible + name=inconvertible + number=2
                    .end()
                .assertFetchResult(OperationResultStatusType.FATAL_ERROR, "Couldn't convert resource object", INCONVERTIBLE_ACCOUNT);
                // (maybe it's not necessary to provide account attributes in the message - reconsider)

        PrismObject<ShadowType> inconvertibleAfter =
                findShadowByPrismName(INCONVERTIBLE_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.get(), result);
        assertShadow(inconvertibleAfter, INCONVERTIBLE_ACCOUNT)
                .display()
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(INCONVERTIBLE_ACCOUNT_UID)
                .attributes()
                    .assertSize(3)
                    .end();

        assertSelectedAccountByName(objects, UNSTORABLE_ACCOUNT)
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(UNSTORABLE_ACCOUNT_UID)
                .assertName(UNSTORABLE_ACCOUNT)
                .attributes()
                    .assertSize(2) // uid=unstorable, name=unstorable
                    .end()
                .assertFetchResult(OperationResultStatusType.FATAL_ERROR, "WRONG");
                // (maybe it's not necessary to provide the unconvertible value in the message - reconsider)

        // Unstorable account is stored without name: the emergency mode of shadowization uses primary ID-only shadows.
        PrismObject<ShadowType> unstorableAfter =
                findShadowByPrismName(UNSTORABLE_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.get(), result);
        assertShadow(unstorableAfter, UNSTORABLE_ACCOUNT)
                .display()
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(UNSTORABLE_ACCOUNT_UID)
                .attributes()
                    .assertSize(2)
                    .end();
        if (isSqaleRepository()) {
            // Totally unstorable account is storable
            return;
        }
        assertSelectedAccountByName(objects, TOTALLY_UNSTORABLE_ACCOUNT) // it has name, because the name attribute was not removed (why?)
                .assertNoOid()
                // Primary identifier value is not here, because it is set as part of object shadowization (which failed)
                .attributes()
                    .assertSize(3) // number, name, uid
                    .end()
                .assertFetchResult(OperationResultStatusType.FATAL_ERROR, "Error while committing the transaction");
    }

    @Test
    public void test270LiveSyncBrokenAccountsExternalUid() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyTokenStorageImpl tokenStorage = new DummyTokenStorageImpl();

        cleanupAccounts(RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID, result);
        RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.controller.setSyncStyle(DummySyncStyle.SMART);

        ResourceOperationCoordinates coords = ResourceOperationCoordinates.ofObjectClass(
                RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.oid,
                SchemaConstants.RI_ACCOUNT_OBJECT_CLASS);

        List<LiveSyncEvent> events = new ArrayList<>();
        LiveSyncEventHandler handler = new LiveSyncEventHandler() {
            @Override
            public void allEventsSubmitted(OperationResult result) {
            }

            @Override
            public boolean handle(LiveSyncEvent event, OperationResult opResult) {
                events.add(event);
                event.acknowledge(true, opResult);
                return true;
            }
        };
        provisioningService.synchronize(coords, null, tokenStorage, handler, task, result);
        assertThat(events).isEmpty();

        createAccountExternalUid(GOOD_ACCOUNT, 1, null);
        createAccountExternalUid(INCONVERTIBLE_ACCOUNT, 2, "WRONG");
        createAccountExternalUid(UNSTORABLE_ACCOUNT, "WRONG", null);
        createAccountExternalUid(TOTALLY_UNSTORABLE_ACCOUNT, 4, null);

        when();

        provisioningService.synchronize(coords, null, tokenStorage, handler, task, result);

        then();
        display("events", events);
        assertThat(events.size()).as("events found").isEqualTo(4);

        List<PrismObject<ShadowType>> objects = events.stream()
                .filter(event -> event.getChangeDescription() != null)
                .map(event -> event.getChangeDescription().getShadowedResourceObject())
                .collect(Collectors.toList());

        assertSelectedAccountByName(objects, GOOD_ACCOUNT)
                .assertOid()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIndexedPrimaryIdentifierValue(GOOD_ACCOUNT_UID)
                .assertName(GOOD_ACCOUNT)
                .attributes()
                    .assertSize(3)
                    .end()
                .assertSuccessOrNoFetchResult();

        PrismObject<ShadowType> goodAfter =
                findShadowByPrismName(GOOD_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.get(), result);
        assertShadow(goodAfter, GOOD_ACCOUNT)
                .display()
                .assertOid()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIndexedPrimaryIdentifierValue(GOOD_ACCOUNT_UID)
                .attributes()
                    .assertSize(3)
                    .end();

        // The resource object itself is intact, so its name is stored as "inconvertible" (i.e. not from UID)
        assertSelectedAccountByName(objects, INCONVERTIBLE_ACCOUNT)
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(INCONVERTIBLE_ACCOUNT_UID)
                .assertName(INCONVERTIBLE_ACCOUNT)
                .attributes()
                    .assertSize(3)
                    .end();

        PrismObject<ShadowType> inconvertibleAfter =
                findShadowByPrismName(INCONVERTIBLE_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.get(), result);
        assertShadow(inconvertibleAfter, INCONVERTIBLE_ACCOUNT)
                .display()
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(INCONVERTIBLE_ACCOUNT_UID)
                .attributes()
                    .assertSize(3)
                    .end();

        // However, here the problem is while acquiring the shadow; so, the repo will contain only the UID.
        assertSelectedAccountByName(objects, UNSTORABLE_ACCOUNT)
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(UNSTORABLE_ACCOUNT_UID)
                .assertName(UNSTORABLE_ACCOUNT)
                .attributes()
                    .assertSize(2) // uid=uid:unstorable, name=unstorable
                    .end();

        PrismObject<ShadowType> unstorableAfter =
                findShadowByPrismName(UNSTORABLE_ACCOUNT, RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.get(), result);
        assertShadow(unstorableAfter, UNSTORABLE_ACCOUNT)
                .display()
                .assertOid()
                .assertIndexedPrimaryIdentifierValue(UNSTORABLE_ACCOUNT_UID)
                .assertName(UNSTORABLE_ACCOUNT)
                .attributes()
                    .assertSize(2)
                    .end();

        // The fetch result is not in the shadows. The exception is recorded in events.

        List<LiveSyncEvent> noChangeEvents = events.stream()
                .filter(event -> event.getChangeDescription() == null)
                .collect(Collectors.toList());
        if (isSqaleRepository()) {
            // Totally unstorable account is storable
            return;
        }
        assertThat(noChangeEvents).hasSize(1);
        LiveSyncEvent failedEvent = noChangeEvents.get(0);
        displayDumpable("failed event", failedEvent);
        assertThat(failedEvent.isError()).isTrue();
    }

    private void createAccount(String name, Object number, Object enableDate) throws Exception {
        DummyAccount account = RESOURCE_DUMMY_BROKEN_ACCOUNTS.controller.addAccount(name);
        account.addAttributeValue(ATTR_NUMBER, number);
        account.addAttributeValue(ENABLE_DATE_NAME, enableDate);
    }

    private void createAccountExternalUid(String name, Object number, Object enableDate) throws Exception {
        DummyAccount account = new DummyAccount(name);
        account.setId(EXTERNAL_UID_PREFIX + name);
        account.addAttributeValue(ATTR_NUMBER, number);
        account.addAttributeValue(ENABLE_DATE_NAME, enableDate);
        RESOURCE_DUMMY_BROKEN_ACCOUNTS_EXTERNAL_UID.controller.getDummyResource().addAccount(account);
    }
    //endregion

    @Test
    public void test300BrokenAttributeDef() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("testing resource with broken attribute definition");
        var testResult = provisioningService.testResource(RESOURCE_DUMMY_BROKEN_ATTRIBUTE_DEF.oid, task, result);

        then("error is understandable enough");
        displayDumpable("test result", testResult);
        assertThatOperationResult(testResult)
                .isFatalError()
                .hasMessageContaining("Missing 'ref' element in attribute definition in object type ACCOUNT/default definition in schema handling in resource:04e54cf7-87df-4b19-9548-fc256b7d585a(resource-dummy-broken-attribute-def)")
                .hasMessageContaining("schemaHandling/objectType/123/attribute/456");
    }

    private boolean isSqaleRepository() {
        return this.repositoryService instanceof SqaleRepositoryService;
    }
}
