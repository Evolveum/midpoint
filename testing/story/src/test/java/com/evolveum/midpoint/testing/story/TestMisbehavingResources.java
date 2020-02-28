/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

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
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
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
        final String TEST_NAME = "test010SanityAssignJackDummyAccount";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when(TEST_NAME);

        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then(TEST_NAME);
        assertSuccess(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);
    }

    @Test
    public void test019SanityUnassignJackDummyAccount() throws Exception {
        final String TEST_NAME = "test010SanityAssignJackDummyAccount";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when(TEST_NAME);

        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then(TEST_NAME);
        assertSuccess(result);

        assertNoDummyAccount(USER_JACK_USERNAME);
    }

    /**
     * MID-4773, MID-5099
     */
    @Test
    public void test100AssignJackDummyAccountTimeout() throws Exception {
        final String TEST_NAME = "test100AssignJackDummyAccountTimeout";

        getDummyResource().setOperationDelayOffset(3000);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when(TEST_NAME);

        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then(TEST_NAME);
        assertInProgress(result);

        assertNoDummyAccount(USER_JACK_USERNAME);
    }

    @Test
    public void test102AssignJackDummyAccounRetry() throws Exception {
        final String TEST_NAME = "test102AssignJackDummyAccounRetry";

        getDummyResource().setOperationDelayOffset(0);
        clockForward("P1D");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when(TEST_NAME);

        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then(TEST_NAME);
        assertSuccess(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME);
    }

    /**
     * MID-5126
     */
    @Test
    public void test110ModifyJackDummyAccountTimeout() throws Exception {
        final String TEST_NAME = "test110ModifyJackDummyAccountTimeout";

        getDummyResource().setOperationDelayOffset(3000);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when(TEST_NAME);

        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, createPolyString(USER_JACK_FULL_NAME_CAPTAIN));

        // THEN
        then(TEST_NAME);
        assertInProgress(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            // operation timed out, data not updated
            .assertFullName(USER_JACK_FULL_NAME);
    }

    @Test
    public void test112ModifyJackDummyAccounRetry() throws Exception {
        final String TEST_NAME = "test112ModifyJackDummyAccounRetry";

        getDummyResource().setOperationDelayOffset(0);
        clockForward("P1D");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when(TEST_NAME);

        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then(TEST_NAME);
        assertSuccess(result);

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            .assertFullName(USER_JACK_FULL_NAME_CAPTAIN);
    }
}
