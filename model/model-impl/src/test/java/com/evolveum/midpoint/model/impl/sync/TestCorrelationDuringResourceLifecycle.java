/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests how the correlation behaves in `proposed` vs `active` lifecycle state of the resource and/or its object types.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCorrelationDuringResourceLifecycle extends AbstractInternalModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/sync");

    private static final DummyTestResource RESOURCE_DUMMY_ACTIVE = new DummyTestResource(
            TEST_DIR, "resource-dummy-active.xml", "91b45a33-e180-4988-9b27-14c28b545bdd", "active");
    private static final DummyTestResource RESOURCE_DUMMY_PROPOSED = new DummyTestResource(
            TEST_DIR, "resource-dummy-proposed.xml", "de6961d6-6da1-41b7-b8a5-e34dde0eba9f", "proposed");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initAndTestDummyResource(RESOURCE_DUMMY_ACTIVE, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_PROPOSED, initTask, initResult);
    }

    /**
     * Note that until the account<->user link is created, the correlation is re-executed on each import.
     * This is true regardless of the resource lifecycle state, i.e. also for "active" resources.
     *
     * This test checks that.
     */
    @Test
    public void test100ReCorrelationForActive() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = "test100";

        given("an account");
        RESOURCE_DUMMY_ACTIVE.controller.addAccount(accountName);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ACTIVE.oid)
                .withNameValue(accountName)
                .execute(result);

        then("the shadow is there, with 'no owner' correlation state and 'unmatched' situation");
        PrismObject<ShadowType> shadow =
                MiscUtil.requireNonNull(
                        findShadowByPrismName(accountName, RESOURCE_DUMMY_ACTIVE.getResource(), result),
                        () -> "no shadow '" + accountName + "' is there");
        String shadowOid = shadow.getOid();
        assertShadowAfter(shadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(SynchronizationSituationType.UNMATCHED);
        assertNoObjectByName(UserType.class, accountName, task, result);

        when("a user is created");
        repositoryService.addObject(
                new UserType()
                        .name(accountName)
                        .asPrismObject(),
                null,
                result);

        and("the account is re-imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ACTIVE.oid)
                .withNameValue(accountName)
                .execute(result);

        then("the shadow has updated correlation state and synchronization situation");
        assertRepoShadow(shadowOid)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER)
                .assertSynchronizationSituation(SynchronizationSituationType.UNLINKED);
    }

    /**
     * As test100, but with "proposed" resource.
     * Currently, the behavior is exactly the same.
     */
    @Test
    public void test110ReCorrelationForProposed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = "test110";

        given("an account");
        RESOURCE_DUMMY_PROPOSED.controller.addAccount(accountName);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_PROPOSED.oid)
                .withNameValue(accountName)
                .execute(result);

        then("the shadow is there, with 'no owner' correlation state and 'unmatched' situation");
        PrismObject<ShadowType> shadow =
                MiscUtil.requireNonNull(
                        findShadowByPrismName(accountName, RESOURCE_DUMMY_PROPOSED.getResource(), result),
                        () -> "no shadow '" + accountName + "' is there");
        String shadowOid = shadow.getOid();
        assertShadowAfter(shadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(SynchronizationSituationType.UNMATCHED);
        assertNoObjectByName(UserType.class, accountName, task, result);

        when("a user is created");
        repositoryService.addObject(
                new UserType()
                        .name(accountName)
                        .asPrismObject(),
                null,
                result);

        and("the account is re-imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_PROPOSED.oid)
                .withNameValue(accountName)
                .execute(result);

        then("the shadow has updated correlation state and synchronization situation");
        assertRepoShadow(shadowOid)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER)
                .assertSynchronizationSituation(SynchronizationSituationType.UNLINKED);
    }
}
