/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;

import com.evolveum.midpoint.prism.polystring.PolyString;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test for various resource-side errors, strange situations, timeouts
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisbehavingResources extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misbehaving-resources");

    protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    protected static final String RESOURCE_DUMMY_OID = "5f9615a2-d05b-11e8-9dab-37186a8ab7ef";

    private static final String USER_JACK_FULL_NAME_CAPTAIN = "Captain Jack Sparrow";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
    }

    @Test
    public void test010SanityAssignJackDummyAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);
    }

    @Test
    public void test019SanityUnassignJackDummyAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoDummyAccount(USER_JACK_USERNAME);
    }

    /**
     * MID-4773, MID-5099
     */
    @Test
    public void test100AssignJackDummyAccountTimeout() throws Exception {
        getDummyResource().setOperationDelayOffset(3000);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertInProgress(result);

        assertNoDummyAccount(USER_JACK_USERNAME);
    }

    @Test
    public void test102AssignJackDummyAccountRetry() throws Exception {
        getDummyResource().setOperationDelayOffset(0);
        clockForward("P1D");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);
    }

    /**
     * MID-5126
     */
    @Test
    public void test110ModifyJackDummyAccountTimeout() throws Exception {
        getDummyResource().setOperationDelayOffset(3000);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, PolyString.fromOrig(USER_JACK_FULL_NAME_CAPTAIN));

        // THEN
        then();
        assertInProgress(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
                // operation timed out, data not updated
                .assertFullName(USER_JACK_FULL_NAME);
    }

    @Test
    public void test112ModifyJackDummyAccountRetry() throws Exception {
        getDummyResource().setOperationDelayOffset(0);
        clockForward("P1D");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Without invalidation, there's no read operation regarding the resource, so the operation will not be retried.
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_OID);

        // WHEN
        when();

        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME_CAPTAIN);
    }
}
