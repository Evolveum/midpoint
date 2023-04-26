/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInboundLiveSyncTask extends AbstractInboundSyncTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceEmerald.setSyncStyle(DummySyncStyle.SMART);
    }

    @Override
    protected TestTask getSyncTask() {
        return TASK_LIVE_SYNC_DUMMY_EMERALD;
    }

    @Override
    public void test199DeleteDummyEmeraldAccountMancomb() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        /// WHEN
        when();

        dummyResourceEmerald.deleteAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountMancomb);
        assertNull("Account shadow mancomb not gone", accountMancomb);

        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
            .displayWithProjections()
                .activation()
                // Disabled by sync reaction
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .assertValidFrom(ACCOUNT_MANCOMB_VALID_FROM_DATE)
                .assertValidTo(ACCOUNT_MANCOMB_VALID_TO_DATE)
                .end()
            .links()
                .singleDead()
                    .resolveTarget()
                        .assertTombstone()
                        .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

        assertNoDummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);

    }

}
