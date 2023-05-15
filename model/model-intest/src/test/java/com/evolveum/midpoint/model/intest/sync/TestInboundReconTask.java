/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Date;

import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInboundReconTask extends AbstractInboundSyncTest {

    private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
    private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);

    @Override
    protected TestTask getSyncTask() {
        return TASK_RECON_DUMMY_EMERALD;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceEmerald.setSyncStyle(DummySyncStyle.DUMB);
    }

    @Override
    public void test180NoChange() throws Exception {
        // GIVEN
        dummyAuditService.clear();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        /// WHEN
        when();

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountMancomb);
        assertNotNull("Account shadow mancomb gone", accountMancomb);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb is gone", userMancomb);
        assertLiveLinks(userMancomb, 1);
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);

        // MID-7110
        //displayDumpable("Audit", dummyAuditService);
        //dummyAuditService.assertRecords(2);            // reconciliation request + execution
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
                        .display()
                        .assertTombstone();

        assertNoDummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);

    }

}
