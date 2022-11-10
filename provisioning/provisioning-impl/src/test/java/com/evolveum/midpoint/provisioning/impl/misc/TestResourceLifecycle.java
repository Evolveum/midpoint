/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.misc;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.provisioning.api.Resource;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
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
    private static final DummyTestResource RESOURCE_DUMMY_SEMI_ACTIVE = new DummyTestResource(
            TEST_DIR, "resource-dummy-semi-active.xml", "a193c17a-c755-4373-8a20-9827f2910c61", "semi-active");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY_PROPOSED.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_ACTIVE.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_SEMI_ACTIVE.initAndTest(this, initTask, initResult);
    }

    /** Shadows for resource in `proposed` mode should be created as "simulated" and reclassified each time they are seen. */
    @Test
    public void test100SearchesOnProposed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String testName = "test100";
        //noinspection UnnecessaryLocalVariable
        String regular = testName;
        String testing = "_" + testName;

        given("two accounts exist");
        RESOURCE_DUMMY_PROPOSED.controller.addAccount(regular);
        RESOURCE_DUMMY_PROPOSED.controller.addAccount(testing);

        when("they are retrieved the first time");
        List<PrismObject<ShadowType>> accounts = retrieveAndCheckAccounts(
                RESOURCE_DUMMY_PROPOSED, testName,
                ExpectedAccount.of(regular, "default", false),
                ExpectedAccount.of(testing, "testing", false));

        when("their intents are broken (swapped)");
        swapIntents(accounts, result);

        and("they are retrieved again (by search)");
        retrieveAndCheckAccounts(
                RESOURCE_DUMMY_PROPOSED, testName,
                ExpectedAccount.of(regular, "default", false),
                ExpectedAccount.of(testing, "testing", false));

        when("their intents are broken (swapped) again");
        swapIntents(accounts, result);

        and("they are retrieved again (by get)");
        retrieveAndCheckAccountsByGet(
                accounts,
                ExpectedAccount.of(regular, "default", false),
                ExpectedAccount.of(testing, "testing", false));
    }

    /** New shadow created on the resource in `proposed` mode should be created as non-production one. */
    @Test
    public void test110CreateOnProposed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String testName = "test110";

        when("an account is created");
        String oid = provisioningService.addObject(
                createShadow(RESOURCE_DUMMY_PROPOSED, SchemaConstants.INTENT_DEFAULT, testName),
                null, null, task, result);

        then("it is non-production (in repo)");
        assertRepoShadow(oid, "repo shadow after")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(SchemaConstants.INTENT_DEFAULT)
                .assertInProduction(false);
    }

    /** Shadows for resource in `active` mode should be created as "production" and should NOT be reclassified each time. */
    @Test
    public void test150NoReclassificationForActive() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String testName = "test150";
        //noinspection UnnecessaryLocalVariable
        String regular = testName;
        String testing = "_" + testName;

        given("two accounts exist");
        RESOURCE_DUMMY_ACTIVE.controller.addAccount(regular);
        RESOURCE_DUMMY_ACTIVE.controller.addAccount(testing);

        when("they are retrieved the first time");
        List<PrismObject<ShadowType>> accounts = retrieveAndCheckAccounts(
                RESOURCE_DUMMY_ACTIVE, testName,
                ExpectedAccount.of(regular, "default", true),
                ExpectedAccount.of(testing, "testing", true));

        when("their types are broken (swapped)");
        swapIntents(accounts, result);

        and("they are retrieved again (by search)");
        retrieveAndCheckAccounts(
                RESOURCE_DUMMY_ACTIVE, testName,
                ExpectedAccount.of(regular, "testing", true),
                ExpectedAccount.of(testing, "default", true));

        // No swapping of intents, as they should be still swapped

        and("they are retrieved again (by get)");
        retrieveAndCheckAccountsByGet(
                accounts,
                ExpectedAccount.of(regular, "testing", true),
                ExpectedAccount.of(testing, "default", true));
    }

    /** New shadow created on the resource in `active` mode should be created as production one. */
    @Test
    public void test160CreateOnActive() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String testName = "test160";

        when("an account is created");
        String oid = provisioningService.addObject(
                createShadow(RESOURCE_DUMMY_ACTIVE, SchemaConstants.INTENT_DEFAULT, testName),
                null, null, task, result);

        then("it is production (in repo)");
        assertRepoShadow(oid, "repo shadow after")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(SchemaConstants.INTENT_DEFAULT)
                .assertInProduction(true);
    }

    /**
     * Two accounts: one is categorized as "account/default" (active state), one is "account/testing" (proposed state).
     */
    @Test
    public void test200SearchingOnSemiActive() throws Exception {
        given("two accounts exist");
        String regular = "test120";
        String testing = "_" + regular;
        RESOURCE_DUMMY_SEMI_ACTIVE.controller.addAccount(regular);
        RESOURCE_DUMMY_SEMI_ACTIVE.controller.addAccount(testing);

        when("they are retrieved");
        retrieveAndCheckAccounts(
                RESOURCE_DUMMY_SEMI_ACTIVE, regular,
                ExpectedAccount.of(regular, "default", true),
                ExpectedAccount.of(testing, "testing", false));
    }

    private List<PrismObject<ShadowType>> retrieveAndCheckAccounts(
            DummyTestResource resource, String nameSubstring, ExpectedAccount... expectedAccounts)
            throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var accounts = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(resource.getResourceBean()).queryForAccountDefault()
                        .and().item(ShadowType.F_ATTRIBUTES, ICFS_NAME).contains(nameSubstring)
                        .build(),
                null,
                task,
                result);

        then("there is the expected number of accounts");
        assertThat(accounts).as("accounts").hasSize(expectedAccounts.length);

        for (ExpectedAccount expectedAccount : expectedAccounts) {
            and(String.format("expected account '%s' is OK", expectedAccount.name));
            var account = accounts.stream()
                    .filter(a -> a.asObjectable().getName().getOrig().equals(expectedAccount.name))
                    .findFirst()
                    .orElseThrow();
            assertShadowAfter(account)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(expectedAccount.intent)
                    .assertInProduction(expectedAccount.production);
        }
        return accounts;
    }

    /** Modes for new shadows created should correspond to states of their respective object types. */
    @Test
    public void test210CreateOnSemiActive() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String regular = "test210";
        String testing = "_test210";

        when("accounts are created");
        String oidRegular = provisioningService.addObject(
                createShadow(RESOURCE_DUMMY_SEMI_ACTIVE, SchemaConstants.INTENT_DEFAULT, regular),
                null, null, task, result);
        String oidTesting = provisioningService.addObject(
                createShadow(RESOURCE_DUMMY_SEMI_ACTIVE, "testing", testing),
                null, null, task, result);

        then("their states are correct");
        assertRepoShadow(oidRegular, "repo shadow after (regular)")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(SchemaConstants.INTENT_DEFAULT)
                .assertInProduction(true);
        assertRepoShadow(oidTesting, "repo shadow after (testing)")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("testing")
                .assertInProduction(false);
    }

    private void retrieveAndCheckAccountsByGet(List<PrismObject<ShadowType>> accounts, ExpectedAccount... expectedAccounts)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        then("accounts are OK when obtained by getObject");

        assertThat(accounts).as("accounts").hasSize(expectedAccounts.length);
        for (ExpectedAccount expectedAccount : expectedAccounts) {
            var oid = accounts.stream()
                    .filter(a -> a.asObjectable().getName().getOrig().equals(expectedAccount.name))
                    .findFirst()
                    .map(PrismObject::getOid)
                    .orElseThrow();
            PrismObject<ShadowType> fetched = provisioningService.getObject(ShadowType.class, oid, null, task, result);
            assertShadowAfter(fetched)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(expectedAccount.intent)
                    .assertInProduction(expectedAccount.production);
        }
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

    private @NotNull PrismObject<ShadowType> createShadow(DummyTestResource resource, String intent, String name)
            throws SchemaException, ConfigurationException {
        var shadow = new ShadowType()
                .resourceRef(resource.oid, ResourceType.COMPLEX_TYPE)
                .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent(intent)
                .beginAttributes()
                .<ShadowType>end()
                .asPrismObject();
        ResourceAttribute<String> nameAttr = resource.controller.createAccountAttribute(SchemaConstants.ICFS_NAME);
        nameAttr.setRealValue(name);
        shadow.findContainer(ShadowType.F_ATTRIBUTES).getValue().add(nameAttr);
        return shadow;
    }

    private static class ExpectedAccount {
        private final String name;
        private final String intent;
        private final boolean production;

        private ExpectedAccount(String name, String intent, boolean production) {
            this.name = name;
            this.intent = intent;
            this.production = production;
        }

        static ExpectedAccount of(String name, String intent, boolean production) {
            return new ExpectedAccount(name, intent, production);
        }
    }
}
