
/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mining;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Role analysis tests (role mining and outlier detection).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRoleAnalysis extends AbstractInitializedModelIntegrationTest {

    private static final long DEFAULT_TIMEOUT = 600_000;

    public static final File TEST_DIR = new File("src/test/resources/mining/");

    public static final Integer FINAL_TASK_STAGE = 7;

    //RBAC generated data
    private static final File TEST_DIR_USERS_FILE = new File(TEST_DIR, "import/users.xml");
    private static final File TEST_DIR_ORGS_FILE = new File(TEST_DIR, "import/orgs.xml");
    private static final File TEST_DIR_ROLES_FILE = new File(TEST_DIR, "import/roles.xml");
    private static final File TEST_DIR_ARCHETYPES_FILE = new File(TEST_DIR, "import/archetypes.xml");


    // Role mining org attribute rule
    private static final String SESSION_ROLE_MINING_1_OID = "32e52e85-d871-4a24-8fa3-31f301bfc58e";
    private static final TestObject<RoleAnalysisSessionType> SESSION_ROLE_MINING_1 = TestObject.file(
            TEST_DIR, "session/session-role-mining-1.xml", SESSION_ROLE_MINING_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-1.xml",
                    "7db12c2a-d431-4587-aa8d-55d76e4401da");

    // Role mining in role mode without attribute rules
    private static final String SESSION_ROLE_MINING_ROLE_MODE_1_OID = "96632490-60be-42b1-b054-f0ac8ae04df3";
    private static final TestObject<RoleAnalysisSessionType> SESSION_ROLE_MINING_ROLE_MODE_1 = TestObject.file(
            TEST_DIR, "session/session-role-mining-role-mode-1.xml", SESSION_ROLE_MINING_ROLE_MODE_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_ROLE_MODE_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-role-mode-1.xml",
                    "813b8407-adb0-4575-b40c-35e06573c20e");

    // Outlier org attribute rule (partial analysis - not outlier cluster excluded)
    private static final String SESSION_OUTLIER_PART_1_OID = "6cd71dab-993a-4dea-aeb4-b8bdcad81ddc";
    private static final TestObject<RoleAnalysisSessionType> SESSION_OUTLIER_PART_1 = TestObject.file(
            TEST_DIR, "session/session-outlier-part-1.xml", SESSION_OUTLIER_PART_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_PART_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-outlier-part-1.xml",
                    "89c856dc-13f8-43ac-99b2-4bf2654f94ca");

    // Outlier org attribute rule (full analysis - outlier cluster included)
    private static final String SESSION_OUTLIER_FULL_1_OID = "0fc912c8-794a-4add-ad61-a7013d6abd4a";
    private static final TestObject<RoleAnalysisSessionType> SESSION_OUTLIER_FULL_1 = TestObject.file(
            TEST_DIR, "session/session-outlier-full-1.xml", SESSION_OUTLIER_FULL_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_FULL_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-outlier-full-1.xml",
                    "55e836f6-4e43-485f-bdc3-a95858d3492f");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        if (isNativeRepository()) {
            repoAddObjectsFromFile(TEST_DIR_ARCHETYPES_FILE, initResult);
            repoAddObjectsFromFile(TEST_DIR_ORGS_FILE, initResult);
            repoAddObjectsFromFile(TEST_DIR_ROLES_FILE, initResult);
            repoAddObjectsFromFile(TEST_DIR_USERS_FILE, initResult);
            initTestObjects(initTask, initResult,
                    SESSION_ROLE_MINING_1,
                    SESSION_ROLE_MINING_ROLE_MODE_1,
                    SESSION_OUTLIER_PART_1,
                    SESSION_OUTLIER_FULL_1);
        }
    }

    /**
     * Test case for role mining process. Runs the role mining session defined
     * in {@code session-role-mining-1.xml} and verifies the task progress and expected result.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void test010RoleAnalysisSessionRoleMining1() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        String sessionId = SESSION_ROLE_MINING_1_OID;

        when("task is run");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_1.init(this, task, result);
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_1.rerunTaskWithinTimeout(result, DEFAULT_TIMEOUT); // asserts success

        then("task is OK and result is as expected");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_1.assertAfter()
                .display()
                .assertProgress(FINAL_TASK_STAGE);

        Integer expectedObjectsCount = 1063;
        Integer expectedClusterCount = 18;
        Double expectedMeanDensity = 89.36643749031973;

        RoleAnalysisSessionType session = getSession(sessionId);
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        assertEquals(expectedObjectsCount, sessionStatistic.getProcessedObjectCount());
        assertEquals(expectedClusterCount, sessionStatistic.getClusterCount());
        assertEquals(expectedMeanDensity, sessionStatistic.getMeanDensity());

        assertObjects(RoleAnalysisClusterType.class, buildClustersQuery(sessionId), expectedClusterCount);
    }

    /**
     * Test case for role mining process. Runs the role mining session defined
     * in {@code session-role-mining-role-mode-1.xml} and verifies the task progress and expected result.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void test020RoleAnalysisSessionRoleMiningRoleMode1() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        String sessionId = SESSION_ROLE_MINING_ROLE_MODE_1_OID;

        when("task is run");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_ROLE_MODE_1.init(this, task, result);
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_ROLE_MODE_1.rerunTaskWithinTimeout(result, DEFAULT_TIMEOUT); // asserts success

        then("task is OK and result is as expected");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_ROLE_MODE_1.assertAfter()
                .display()
                .assertProgress(FINAL_TASK_STAGE);

        Integer expectedObjectsCount = 166;
        Integer expectedClusterCount = 12;
        Double expectedMeanDensity = 97.93252608203476;

        RoleAnalysisSessionType session = getSession(sessionId);
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        assertEquals(expectedObjectsCount, sessionStatistic.getProcessedObjectCount());
        assertEquals(expectedClusterCount, sessionStatistic.getClusterCount());
        assertEquals(expectedMeanDensity, sessionStatistic.getMeanDensity());

        assertObjects(RoleAnalysisClusterType.class, buildClustersQuery(sessionId), expectedClusterCount);
    }

    /**
     * Test case for partial outlier detection process (clustering_noise analysis is excluded). Runs the outlier session
     * defined in {@code session-outlier-part-1.xml} and verifies the task progress and expected result.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void test030RoleAnalysisSessionOutlierPart1() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        String sessionId = SESSION_OUTLIER_PART_1_OID;

        when("task is run");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_PART_1.init(this, task, result);
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_PART_1.rerunTaskWithinTimeout(result, DEFAULT_TIMEOUT); // asserts success

        then("task is OK and result is as expected");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_PART_1.assertAfter()
                .display()
                .assertProgress(FINAL_TASK_STAGE);

        Integer expectedObjectsCount = 1063;
        Integer expectedClusterCount = 18;
        Double expectedMeanDensity = 89.36643749031973;
        Integer expectedInnerOutlierCount = 12;
        Integer expectedOuterOutlierCount = 0;

        RoleAnalysisSessionType session = getSession(sessionId);
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        assertEquals(expectedObjectsCount, sessionStatistic.getProcessedObjectCount());
        assertEquals(expectedClusterCount, sessionStatistic.getClusterCount());
        assertEquals(expectedMeanDensity, sessionStatistic.getMeanDensity());

        assertObjects(RoleAnalysisClusterType.class, buildClustersQuery(sessionId), expectedClusterCount);

        assertEquals(expectedInnerOutlierCount, getOutlierCount(sessionId, OutlierClusterCategoryType.INNER_OUTLIER));
        assertEquals(expectedOuterOutlierCount, getOutlierCount(sessionId, OutlierClusterCategoryType.OUTER_OUTLIER));
    }

    /**
     * Test case for full outlier detection process. Runs the outlier session
     * defined in {@code session-outlier-full-1.xml} and verifies the task progress and expected result.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void test040RoleAnalysisSessionOutlierFull1() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        String sessionId = SESSION_OUTLIER_FULL_1_OID;

        when("task is run");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_FULL_1.init(this, task, result);
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_FULL_1.rerunTaskWithinTimeout(result, DEFAULT_TIMEOUT); // asserts success

        then("task is OK and result is as expected");
        TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_FULL_1.assertAfter()
                .display()
                .assertProgress(FINAL_TASK_STAGE);

        Integer expectedObjectsCount = 1063;
        Integer expectedClusterCount = 18;
        Double expectedMeanDensity = 89.36643749031973;
        Integer expectedInnerOutlierCount = 12;
        Integer expectedOuterOutlierCount = 157;

        RoleAnalysisSessionType session = getSession(sessionId);
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        assertEquals(expectedObjectsCount, sessionStatistic.getProcessedObjectCount());
        assertEquals(expectedClusterCount, sessionStatistic.getClusterCount());
        assertEquals(expectedMeanDensity, sessionStatistic.getMeanDensity());

        assertObjects(RoleAnalysisClusterType.class, buildClustersQuery(sessionId), expectedClusterCount);

        assertEquals(expectedInnerOutlierCount, getOutlierCount(sessionId, OutlierClusterCategoryType.INNER_OUTLIER));
        assertEquals(expectedOuterOutlierCount, getOutlierCount(sessionId, OutlierClusterCategoryType.OUTER_OUTLIER));
    }

    private RoleAnalysisSessionType getSession(String sessionOid) throws Exception {
        return getObject(RoleAnalysisSessionType.class, sessionOid).getValue().asObjectable();
    }

    private ObjectQuery buildClustersQuery(String sessionOid) {
        return queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(sessionOid)
                .build();
    }

    private Integer getOutlierCount(String sessionOid, OutlierClusterCategoryType category) {
        Task task = createTask("get outliers");
        List<RoleAnalysisOutlierType> outliers = roleAnalysisService.getSessionOutliers(sessionOid, category, task, task.getResult());
        return outliers.size();
    }

}
