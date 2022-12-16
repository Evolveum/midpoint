/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest.Color.*;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Besides synchronization itself here we test the shadow cleanup activity.
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSyncStoryUsingLiveSync extends AbstractSynchronizationStoryTest {

    private static final TestTask TASK_LIVE_SYNC_DUMMY = new TestTask(
            TEST_DIR, "task-live-sync-dummy.xml", "f5517850-5cd8-46dc-b40e-4816881dc171");
    private static final TestTask TASK_LIVE_SYNC_DUMMY_GREEN = new TestTask(
            TEST_DIR, "task-live-sync-dummy-green.xml", "bf124268-d75a-419d-95e7-00ff95c21924");
    private static final TestTask TASK_LIVE_SYNC_DUMMY_BLUE = new TestTask(
            TEST_DIR, "task-live-sync-dummy-blue.xml", "200c8126-b176-42c4-910d-9f469e96b27b");

    private static final TestTask TASK_SHADOW_CLEANUP_GREEN = new TestTask(
            TEST_DIR, "task-shadow-cleanup-green.xml", "5b2ba49f-6d4f-4618-afdf-4d138117e40a");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).setSyncStyle(DummySyncStyle.SMART);
        getDummyResource().setSyncStyle(DummySyncStyle.DUMB);
        getDummyResource(RESOURCE_DUMMY_BLUE_NAME).setSyncStyle(DummySyncStyle.SMART);

        for (TestTask task : getTaskMap().values()) {
            task.initialize(this, initTask, initResult);
            task.rerun(initResult); // To obtain live sync token
        }

        TASK_SHADOW_CLEANUP_GREEN.initialize(this, initTask, initResult);
    }

    @Override
    protected Map<Color, TestTask> getTaskMap() {
        return Map.of(
                DEFAULT, TASK_LIVE_SYNC_DUMMY,
                GREEN, TASK_LIVE_SYNC_DUMMY_GREEN,
                BLUE, TASK_LIVE_SYNC_DUMMY_BLUE);
    }

    @Override
    protected String getExpectedChannel() {
        return SchemaConstants.CHANNEL_LIVE_SYNC_URI;
    }

    @Test
    public void test999GreenShadowsCleanup() throws Exception {
        OperationResult result = getTestOperationResult();

        String ACCOUNT_JACK_DUMMY_USERNAME = "jack";
        String ACCOUNT_CAROL_DUMMY_USERNAME = "carol";

        rememberTimeBeforeSync();
        prepareNotifications();

        when("jack account is added and sync is run");
        DummyAccount accountJack = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        accountJack.setEnabled(true);
        accountJack.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sevenseas");
        accountJack.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "The Seven Seas");
        getGreenResource().addAccount(accountJack);
        runSyncTasks(GREEN, GREEN, GREEN); // To stabilize the situation, and eat all secondary changes related to jack's account

        and("sleeping 10 seconds to make jack's account rot");
        TimeUnit.SECONDS.sleep(10);

        and("carol account is added and sync is run");
        DummyAccount accountCarol = new DummyAccount(ACCOUNT_CAROL_DUMMY_USERNAME);
        accountCarol.setEnabled(true);
        accountCarol.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Carol Seepgood");
        accountCarol.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        getGreenResource().addAccount(accountCarol);
        runSyncTasks(GREEN);

        and("jack's account is deleted from the resource");
        // This is to check that shadows for existing accounts will not be removed by the cleanup task
        getGreenResource().deleteAccountByName(accountJack.getName());

        and("cleanup task is run");
        TASK_SHADOW_CLEANUP_GREEN.rerun(result);
        TASK_SHADOW_CLEANUP_GREEN.assertAfter();

        then("carol's account should be still there");
        PrismObject<UserType> userCarol = findUserByUsername(ACCOUNT_CAROL_DUMMY_USERNAME);
        display("User carol", userCarol);
        assertNotNull("No carol user", userCarol);

        and("jack should be gone, as the shadow is too old and no longer on the resource");
        PrismObject<UserType> userJack = findUserByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        display("User jack", userJack);
        assertNull("User jack is still there!", userJack);

        and("jojo should be still there, as the Xjojo account was not deleted on the resource");
        PrismObject<UserType> userJojo = findUserByUsername("jojo");
        display("User jojo", userJojo);
        assertNotNull("No jojo user", userJojo);

        displayAllNotifications();
        notificationManager.setDisabled(true);
    }
}
