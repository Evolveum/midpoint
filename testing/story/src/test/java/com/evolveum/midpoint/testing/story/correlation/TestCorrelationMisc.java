/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import java.io.File;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyDefaultScenario.Account;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Miscellaneous correlation tests.
 */
public class TestCorrelationMisc extends AbstractCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "misc");

    private static final DummyTestResource RESOURCE_DUMMY_SOURCE = new DummyTestResource(
            TEST_DIR, "resource-dummy-source.xml", "96abc133-8ea2-460c-afad-f8cc668aa215", "source");

    private static final String INTENT_MID_10501 = "mid-10501";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY_SOURCE.initAndTest(this, initTask, initResult);
    }

    /**
     * Tests solo correlator with `composition` item. Such correlator should be treated as part of the (implicit) composition,
     * so its weight will be taken into account.
     *
     * MID-10501
     */
    @Test
    public void test100SoloCorrelatorWithCompositionItem() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var userName = "u0001";
        var accountName = "a9999";
        var fullName = "John Smith";

        given("an account and a user with the same full name");
        var user = new UserType()
                .name(userName)
                .fullName(fullName);
        addObject(user, task, result);

        RESOURCE_DUMMY_SOURCE.addAccount(accountName, fullName)
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), INTENT_MID_10501);

        when("import is run");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_SOURCE.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_MID_10501))
                .withProcessingAllAccounts()
                .execute(result);

        then("the shadow should be in DISPUTED state");
        assertRepoShadow(findShadowByPrismName(accountName, RESOURCE_DUMMY_SOURCE.get(), result).getOid(), "after")
                .assertSynchronizationSituation(SynchronizationSituationType.DISPUTED);

        and("user is not linked");
        assertUserAfter(user.getOid())
                .assertLinks(0, 0);
    }
}
