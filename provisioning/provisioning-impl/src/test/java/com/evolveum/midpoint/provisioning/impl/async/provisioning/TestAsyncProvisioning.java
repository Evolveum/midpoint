/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import static java.util.Collections.singletonList;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.testng.annotations.Test;

/**
 * Tests "asynchronous provisioning" functionality.
 *
 * TODO: name
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class TestAsyncProvisioning extends AbstractProvisioningIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/async/provisioning/");

    private static final String ASYNC_PROVISIONING_CONNECTOR = "AsyncProvisioningConnector";

    private static final ItemName ATTR_DRINK = new ItemName(MidPointConstants.NS_RI, "drink");
    private static final ItemPath ATTR_DRINK_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_DRINK);

    private static final ItemName ATTR_SHOE_SIZE = new ItemName(MidPointConstants.NS_RI, "shoeSize");
    private static final ItemPath ATTR_SHOE_SIZE_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_SHOE_SIZE);

    PrismObject<ResourceType> resource;

    private String jackAccountOid;

    protected boolean isUsingConfirmations() {
        return false;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        InternalsConfig.setSanityChecks(true);
        resource = addResourceFromFile(getResourceFile(), singletonList(ASYNC_PROVISIONING_CONNECTOR), false, initResult);
    }

    protected abstract File getResourceFile();

    @Test
    public void test000Sanity() throws Exception {
        testSanity();
    }

    protected abstract void testSanity() throws Exception;

    @Test
    public void test100AddAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> jack = createShadow(resource, "jack");
        addAttributeValue(resource, jack, ATTR_DRINK, "rum");

        when();
        jackAccountOid = provisioningService.addObject(jack, null, null, task, result);

        then();
        dumpRequests();

        assertRepoShadow(jackAccountOid)
                .display();
        assertShadowFuture(jackAccountOid);
    }

    @Test
    public void test110ModifyAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clearRequests();

        List<ItemDelta<?, ?>> modifications = deltaFor(ShadowType.class)
                .item(ATTR_DRINK_PATH, getAttributeDefinition(resource, ATTR_DRINK)).add("water")
                .item(ATTR_SHOE_SIZE_PATH, getAttributeDefinition(resource, ATTR_SHOE_SIZE)).add(42)
                .asItemDeltas();

        when();
        provisioningService.modifyObject(ShadowType.class, jackAccountOid, modifications, null, null, task, result);

        then();
        dumpRequests();

        assertRepoShadow(jackAccountOid)
                .display();
        assertShadowFuture(jackAccountOid);
    }

    @Test
    public void test120ModifyAccountAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clearRequests();

        List<ItemDelta<?, ?>> modifications = deltaFor(ShadowType.class)
                .item(ATTR_DRINK_PATH, getAttributeDefinition(resource, ATTR_DRINK)).delete("water")
                .item(ATTR_SHOE_SIZE_PATH, getAttributeDefinition(resource, ATTR_SHOE_SIZE)).replace(44)
                .asItemDeltas();

        when();
        provisioningService.modifyObject(ShadowType.class, jackAccountOid, modifications, null, null, task, result);

        then();
        dumpRequests();

        assertRepoShadow(jackAccountOid)
                .display();
        assertShadowFuture(jackAccountOid);
    }

    @Test
    public void test130DeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clearRequests();

        when();
        provisioningService.deleteObject(ShadowType.class, jackAccountOid, null, null, task, result);

        then();
        dumpRequests();

        if (isUsingConfirmations()) {
            assertRepoShadow(jackAccountOid)
                    .display();
            assertShadowFuture(jackAccountOid);
        } else {
            assertNoRepoShadow(jackAccountOid);
        }
    }

    // TODO
    private void dumpRequests() {
        displayMap("Requests", MockAsyncProvisioningTarget.INSTANCE.getRequestsMap());
    }

    private void clearRequests() {
        MockAsyncProvisioningTarget.INSTANCE.clear();
    }
}
