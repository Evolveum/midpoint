/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.*;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyNegative extends AbstractDummyTest {

    private static final File ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILE =
            new File(TEST_DIR, "account-elaine-resource-not-found.xml");

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

    public void testGetResourceBrokenSchema(BreakMode breakMode) throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        // precondition
        PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        display("Repo resource (before)", repoResource);
        PrismContainer<Containerable> schema = repoResource.findContainer(ResourceType.F_SCHEMA);
        assertTrue("Schema found in resource before the test (precondition)", schema == null || schema.isEmpty());

        dummyResource.setSchemaBreakMode(breakMode);
        try {

            // WHEN
            PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, null, result);

            // THEN
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

    @Test
    public void test190GetResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyResource.setSchemaBreakMode(BreakMode.NONE);
        syncServiceMock.reset();

        // WHEN
        when();
        PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        display("Resource after", resource);
        IntegrationTestTools.displayXml("Resource after (XML)", resource);
        assertHasSchema(resource, "dummy");
    }

    @Test
    public void test200AddAccountNullAttributes() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();
        account.removeContainer(ShadowType.F_ATTRIBUTES);
        display("Adding shadow", account);

        try {
            // WHEN
            when();
            provisioningService.addObject(account, null, null, task, result);

            assertNotReached();
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        then();
        syncServiceMock.assertNotifyFailureOnly();
    }

    @Test
    public void test201AddAccountEmptyAttributes() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();

        account.findContainer(ShadowType.F_ATTRIBUTES).getValue().clear();

        display("Adding shadow", account);

        try {
            // WHEN
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        syncServiceMock.assertNotifyFailureOnly();
    }

    @Test
    public void test210AddAccountNoObjectClass() throws Exception {
        // GIVEN
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
            // WHEN
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        syncServiceMock.assertNotifyFailureOnly();
    }

    @Test
    public void test220AddAccountNoResourceRef() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();

        accountType.setResourceRef(null);

        display("Adding shadow", account);

        try {
            // WHEN
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        //FIXME: not sure, if this check is needed..if the resource is not specified, provisioning probably will be not called.
//        syncServiceMock.assertNotifyFailureOnly();
    }

    @Test
    public void test221DeleteAccountResourceNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ShadowType accountType = parseObjectType(ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILE);
        PrismObject<ShadowType> account = accountType.asPrismObject();
        account.checkConsistence();

//        accountType.setResourceRef(null);

        display("Adding shadow", account);

        try {
            // WHEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_MORGAN_FILE);
        String shadowOid = provisioningService.addObject(account, null, null, task, result);

        repositoryService.deleteObject(ShadowType.class, shadowOid, result);

        // reset
        task = createPlainTask();
        result = task.getResult();
        syncServiceMock.reset();

        try {
            // WHEN
            provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);

            assertNotReached();
        } catch (ObjectNotFoundException e) {
            displayExpectedException(e);
            result.computeStatus();
            display("Result", result);
            TestUtil.assertFailure(result);
        }

        syncServiceMock.assertNoNotifyChange();
    }

}
