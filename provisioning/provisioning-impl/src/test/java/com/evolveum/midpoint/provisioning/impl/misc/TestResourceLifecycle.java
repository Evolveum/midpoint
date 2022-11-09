/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.misc;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestResourceLifecycle extends AbstractProvisioningIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "misc/lifecycle");

    private static final DummyTestResource RESOURCE_DUMMY_PROPOSED = new DummyTestResource(
            TEST_DIR, "resource-dummy-proposed.xml", "e2180cc3-365a-4c3d-81c3-d3407fbb722f", "proposed");
    private static final DummyTestResource RESOURCE_DUMMY_ACTIVE = new DummyTestResource(
            TEST_DIR, "resource-dummy-active.xml", "7e07397d-392d-438b-91ea-00e53a6e521c", "active");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY_PROPOSED.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_ACTIVE.initAndTest(this, initTask, initResult);
    }

    /** Shadows for resource in `proposed` mode should be reclassified each time. */
    @Test
    public void test100ReclassificationForProposed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("two accounts exist");
        RESOURCE_DUMMY_PROPOSED.controller.addAccount("test100");
        RESOURCE_DUMMY_PROPOSED.controller.addAccount("_test100");

        when("they are retrieved the first time");
        List<PrismObject<ShadowType>> accounts = retrieveAndCheckAccounts(
                RESOURCE_DUMMY_PROPOSED,
                "test100", "default", false,
                "_test100", "testing", false,
                task, result);

        when("their intents are broken (swapped)");
        swapIntents(accounts, result);

        and("they are retrieved again (by search)");
        retrieveAndCheckAccounts(
                RESOURCE_DUMMY_PROPOSED,
                "test100", "default", false,
                "_test100", "testing", false,
                task, result);

        when("their intents are broken (swapped) again");
        swapIntents(accounts, result);

        and("they are retrieved again (by get)");
        retrieveAndCheckAccountsByGet(
                accounts,
                "test100", "default", false,
                "_test100", "testing", false,
                task, result);
    }

    private List<PrismObject<ShadowType>> retrieveAndCheckAccounts(
            DummyTestResource resource,
            String firstName, String firstIntent, boolean firstProduction,
            String secondName, String secondIntent, boolean secondProduction,
            Task task, OperationResult result)
            throws CommonException {
        var accounts = provisioningService.searchObjects(
                ShadowType.class,
                createAllAccountsQuery(resource.getResourceBean()),
                null,
                task,
                result);

        then("there are two of them");
        assertThat(accounts).as("accounts").hasSize(2);
        var first = accounts.stream()
                .filter(a -> a.asObjectable().getName().getOrig().equals(firstName))
                .findFirst()
                .orElseThrow();
        var second = accounts.stream()
                .filter(a -> a.asObjectable().getName().getOrig().equals(secondName))
                .findFirst()
                .orElseThrow();

        and(String.format(
                "their classification is as expected (%s: %s, %s: %s)", firstName, firstIntent, secondName, secondIntent));

        assertShadowAfter(first)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(firstIntent)
                .assertInProduction(firstProduction);
        assertShadowAfter(second)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(secondIntent)
                .assertInProduction(secondProduction);
        return accounts;
    }

    private void retrieveAndCheckAccountsByGet(
            List<PrismObject<ShadowType>> accounts,
            String firstName, String firstIntent, boolean firstProduction,
            String secondName, String secondIntent, boolean secondProduction,
            Task task, OperationResult result)
            throws CommonException {

        assertThat(accounts).as("accounts").hasSize(2);
        var firstOid = accounts.stream()
                .filter(a -> a.asObjectable().getName().getOrig().equals(firstName))
                .findFirst()
                .map(PrismObject::getOid)
                .orElseThrow();
        var secondOid = accounts.stream()
                .filter(a -> a.asObjectable().getName().getOrig().equals(secondName))
                .findFirst()
                .map(PrismObject::getOid)
                .orElseThrow();

        PrismObject<ShadowType> first = provisioningService.getObject(ShadowType.class, firstOid, null, task, result);
        PrismObject<ShadowType> second = provisioningService.getObject(ShadowType.class, secondOid, null, task, result);

        then(String.format(
                "their classification is as expected (%s: %s, %s: %s)", firstName, firstIntent, secondName, secondIntent));

        assertShadowAfter(first)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(firstIntent)
                .assertInProduction(firstProduction);
        assertShadowAfter(second)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(secondIntent)
                .assertInProduction(secondProduction);
    }

    private void swapIntents(List<PrismObject<ShadowType>> accounts, OperationResult result) throws CommonException {
        ShadowType first = accounts.get(0).asObjectable();
        ShadowType second = accounts.get(1).asObjectable();
        changeIntent(first.getOid(), second.getIntent(), result);
        changeIntent(second.getOid(), first.getIntent(), result);
    }

    private void changeIntent(String shadowOid, String newIntent, OperationResult result) throws CommonException {
        repositoryService.modifyObject(
                ShadowType.class,
                shadowOid,
                deltaFor(ShadowType.class)
                        .item(ShadowType.F_INTENT)
                        .replace(newIntent)
                        .asItemDeltas(),
                result);
    }

    /** Shadows for resource in `active` mode should NOT be reclassified each time. */
    @Test
    public void test110NoReclassificationForActive() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("two accounts exist");
        RESOURCE_DUMMY_ACTIVE.controller.addAccount("test110");
        RESOURCE_DUMMY_ACTIVE.controller.addAccount("_test110");

        when("they are retrieved the first time");
        List<PrismObject<ShadowType>> accounts = retrieveAndCheckAccounts(
                RESOURCE_DUMMY_ACTIVE,
                "test110", "default", true,
                "_test110", "testing", true,
                task, result);

        when("their types are broken (swapped)");
        swapIntents(accounts, result);

        and("they are retrieved again (by search)");
        retrieveAndCheckAccounts(
                RESOURCE_DUMMY_ACTIVE,
                "test110", "testing", true,
                "_test110", "default", true,
                task, result);

        // No swapping of intents, as they should be still swapped

        and("they are retrieved again (by get)");
        retrieveAndCheckAccountsByGet(
                accounts,
                "test110", "testing", true,
                "_test110", "default", true,
                task, result);
    }
}
