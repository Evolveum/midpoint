/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.longtest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the "run as" feature, including its performance.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-longtest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRunAs extends AbstractLongTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "runas");

    private static final int NUM_INITIAL_USERS = 4;

    private static final String RESOURCE_DUMMY_NAME = null;
    private static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    private static final String RESOURCE_DUMMY_OID = "2f454e92-c9e8-11e7-8f60-17bc95e695f8";

    protected static final File USER_ROBOT_FILE = new File(TEST_DIR, "user-robot.xml");
    protected static final String USER_ROBOT_USERNAME = "robot";

    protected static final File USER_TEMPLATE_PLAIN_FILE = new File(TEST_DIR, "user-template-plain.xml");
    protected static final String USER_TEMPLATE_PLAIN_OID = "d7b2f8fc-c9ea-11e7-98bd-eb714a446e68";

    protected static final File USER_TEMPLATE_RUNAS_FILE = new File(TEST_DIR, "user-template-runas.xml");
    protected static final String USER_TEMPLATE_RUNAS_OID = "8582e1e2-c9ee-11e7-8fa9-63e7c62604c6";

    protected static final String ORG_PIRATES = "Pirates";

    private static final int NUM_ORG_MAPPINGS = 10;
    private static final int WARM_UP_ROUNDS = 30;

    // fixed time added to the baseline before any % comparison as random stuff like GC can happen
    private static final long BASELINE_RUN_TIME_TOLERANCE = 40;

    private long baselineRunTime;
    private long baselineRepoReadCountIncrement;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        modelService.postInit(initResult);

        // Users
        repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);
        repoAddObjectFromFile(USER_ROBOT_FILE, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_NAME,
                RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);

        repoAddObjectFromFile(USER_TEMPLATE_PLAIN_FILE, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_RUNAS_FILE, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        //initProfiling - start
        ProfilingDataManager profilingManager = ProfilingDataManager.getInstance();

        Map<ProfilingDataManager.Subsystem, Boolean> subsystems = new HashMap<>();
        subsystems.put(ProfilingDataManager.Subsystem.MODEL, true);
        subsystems.put(ProfilingDataManager.Subsystem.REPOSITORY, true);
        profilingManager.configureProfilingDataManagerForTest(subsystems, true);

        profilingManager.appendProfilingToTest();
        //initProfiling - end
    }

    @Test
    public void test000Sanity() throws Exception {
        assertUsers(NUM_INITIAL_USERS);
    }

    /**
     * MID-3816
     */
    @Test
    public void test100AssignAccountDummyToBarbossa() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignAccountToUser(USER_BARBOSSA_OID, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
        display("User after", userAfter);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_NAME, USER_BARBOSSA_USERNAME,
                USER_BARBOSSA_FULL_NAME, true);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Some say robot -- administrator");
    }

    /**
     * MID-3816
     */
    @Test
    public void test109UnassignAccountDummyFromBarbossa() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignAccountFromUser(USER_BARBOSSA_OID, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
        display("User after", userAfter);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_NAME, USER_BARBOSSA_USERNAME);
    }

    @Test
    public void test200CleanupPlain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result /* no value */);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
        display("User after", userAfter);

        PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);
        PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATIONAL_UNIT);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_PLAIN_OID, result);
        assertSuccess(result);
    }

    /**
     * Warm up JVM, so we have stable and comparable results
     */
    @Test
    public void test205WarmUp() throws Exception {
        warmUp();
    }

    /**
     * Set the baseline - no runAs
     */
    @Test
    public void test210BarbossaSetOrganizationPlain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);

        when();
        long starMillis = System.currentTimeMillis();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result, createPolyString(ORG_PIRATES));
        long endMillis = System.currentTimeMillis();

        then();
        assertSuccess(result);

        long readCountIncrement = getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
        display("Run time " + (endMillis - starMillis) + "ms, repo read count increment " + readCountIncrement);
        baselineRunTime = endMillis - starMillis + BASELINE_RUN_TIME_TOLERANCE;
        baselineRepoReadCountIncrement = readCountIncrement;

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
        display("User after", userAfter);

        assertUserOrgs(userAfter, ORG_PIRATES, USER_ADMINISTRATOR_USERNAME);
    }

    @Test
    public void test300CleanupRunAs() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result /* no value */);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
        display("User after", userAfter);

        PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);
        PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATIONAL_UNIT);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_RUNAS_OID, result);
        assertSuccess(result);
    }

    /**
     * Warm up JVM, so we have stable and comparable results
     */
    @Test
    public void test305WarmUp() throws Exception {
        warmUp();
    }

    /**
     * MID-3844
     */
    @Test
    public void test310BarbossaSetOrganizationRunAs() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);

        when();
        long starMillis = System.currentTimeMillis();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result, createPolyString(ORG_PIRATES));
        long endMillis = System.currentTimeMillis();

        then();
        assertSuccess(result);

        long readCountIncrement = getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
        long runTimeMillis = (endMillis - starMillis);
        display("Run time " + runTimeMillis + "ms, repo read count increment " + readCountIncrement);
        long percentRuntimeIncrease = (runTimeMillis - baselineRunTime) * 100 / baselineRunTime;
        long readCountIncrease = readCountIncrement - baselineRepoReadCountIncrement;
        displayValue("Increase over baseline",
                "  run time: " + (runTimeMillis - baselineRunTime) + " (" + percentRuntimeIncrease + "%)\n" +
                        "  repo read: " + readCountIncrease);

        if (readCountIncrease > 2) {
            fail("High increase over repo read count baseline: " + readCountIncrease + " (expected: at most 2)");
        }
        if (percentRuntimeIncrease > 40) {
            fail("Too high run time increase over baseline: " + percentRuntimeIncrease + "% " + baselineRunTime + "ms -> " + runTimeMillis + "ms");
        }

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
        display("User after", userAfter);

        assertUserOrgs(userAfter, ORG_PIRATES, USER_ROBOT_USERNAME);
    }

    private void warmUp() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);

        when();
        long firstTime = warmUpRound(0, task, result);
        long lastTime = 0;
        long sumTime = firstTime;
        for (int i = 1; i < WARM_UP_ROUNDS; i++) {
            lastTime = warmUpRound(i, task, result);
            sumTime += lastTime;
        }

        then();
        assertSuccess(result);

        getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
        display("Warm up run times: first " + (firstTime) + "ms, last " + lastTime + ", average " + (sumTime / WARM_UP_ROUNDS) + "ms");

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
        display("User after", userAfter);

        PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);
        PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATIONAL_UNIT);
    }

    private long warmUpRound(int round, Task task, OperationResult result) throws Exception {
        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);

        long starMillis = System.currentTimeMillis();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result, createPolyString(ORG_PIRATES));
        long endMillis = System.currentTimeMillis();

        assertSuccess(result);

        long readCountIncrement = getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
        display("Warm up round " + round + " run time " + (endMillis - starMillis) + "ms, repo read count increment " + readCountIncrement);

        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result /* no value */);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);

        return endMillis - starMillis;
    }

    @SuppressWarnings("SameParameterValue")
    private void assertUserOrgs(PrismObject<UserType> user, String organization, String principalUsername) {
        PrismAsserts.assertPropertyValue(user, UserType.F_ORGANIZATION, createPolyString(organization));
        PrismAsserts.assertPropertyValue(user, UserType.F_ORGANIZATIONAL_UNIT, expectedOrgUnits(organization, principalUsername));
    }

    private PolyString[] expectedOrgUnits(String organization, String principalUsername) {
        PolyString[] out = new PolyString[NUM_ORG_MAPPINGS];
        for (int i = 0; i < NUM_ORG_MAPPINGS; i++) {
            out[i] = createPolyString(String.format("%03d%s: %s", i + 1, organization, principalUsername));
        }
        return out;
    }
}
