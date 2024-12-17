
/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mining;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import com.evolveum.midpoint.model.impl.mining.utils.DebugOutlierDetectionEvaluation;
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
//TODO move to longTest suite
/**
 * Role analysis tests (role mining and outlier detection).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRoleAnalysis extends AbstractInitializedModelIntegrationTest {

    private record RoleMiningResult(
            Integer processedObjectCount,
            Integer clusterCount,
            Double meanDensity,
            Integer reduction
    ) {
    }

    private record OutlierDetectionResult(
            Integer processedObjectCount,
            Integer innerOutlierCount,
            Integer outerOutlierCount,
            Double f1score,
            Double maxOutlierConfidence
    ) {
    }

    //TODO think about default timeout per specific test
    private static final long DEFAULT_TIMEOUT = 600_000;

    public static final File TEST_DIR = new File("src/test/resources/mining/");

    public static final Integer FINAL_TASK_STAGE = 7;
    public static final Integer LOADING_DATA_TASK_STAGE = 1;

    // RBAC generated data
    // - -m /path/to/midpoint generate-rbac-data --import --transform --role-multiplier 5 --users-count 400 --archetype-role-enabled --archetype-user-enabled --user-division "10:10:20:20:20:10:10" --forget-noise 10 -op 30 -om 5 -oj 5 -oz 5 -oe 5 -an 5
    // - Recompute users (in order to test indirect mode): Server task -> Recomputation tasks -> Type: User
    // - download from UI and clean up to reduce file size (remove _metadata, operationExecution, description, ...)
    // - manually remove administrator user and role from the xml
    // - convert tabs to spaces in xml files in order to pass checkstyle
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
                    "7db12c2a-d431-4587-aa8d-55d76e4401da", DEFAULT_TIMEOUT);

    // Role mining in role mode without attribute rules
    private static final String SESSION_ROLE_MINING_ROLE_MODE_1_OID = "96632490-60be-42b1-b054-f0ac8ae04df3";
    private static final TestObject<RoleAnalysisSessionType> SESSION_ROLE_MINING_ROLE_MODE_1 = TestObject.file(
            TEST_DIR, "session/session-role-mining-role-mode-1.xml", SESSION_ROLE_MINING_ROLE_MODE_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_ROLE_MODE_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-role-mode-1.xml",
                    "813b8407-adb0-4575-b40c-35e06573c20e", DEFAULT_TIMEOUT);

    // Role mining direct mode
    private static final String SESSION_ROLE_MINING_DIRECT_1_OID = "7eb32d16-b0d5-4149-834d-4a80872db920";
    private static final TestObject<RoleAnalysisSessionType> SESSION_ROLE_MINING_DIRECT_1 = TestObject.file(
            TEST_DIR, "session/session-role-mining-direct-1.xml", SESSION_ROLE_MINING_DIRECT_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_DIRECT_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-direct-1.xml",
                    "67aae68a-dc30-4df1-bfc8-de42b9aee9d6", DEFAULT_TIMEOUT);

    // Outlier org attribute rule (partial analysis - not outlier cluster excluded)
    private static final String SESSION_OUTLIER_PART_1_OID = "6cd71dab-993a-4dea-aeb4-b8bdcad81ddc";
    private static final TestObject<RoleAnalysisSessionType> SESSION_OUTLIER_PART_1 = TestObject.file(
            TEST_DIR, "session/session-outlier-part-1.xml", SESSION_OUTLIER_PART_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_PART_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-outlier-part-1.xml",
                    "89c856dc-13f8-43ac-99b2-4bf2654f94ca", DEFAULT_TIMEOUT);

    // Outlier org attribute rule (full analysis - outlier cluster included)
    private static final String SESSION_OUTLIER_FULL_1_OID = "0fc912c8-794a-4add-ad61-a7013d6abd4a";
    private static final TestObject<RoleAnalysisSessionType> SESSION_OUTLIER_FULL_1 = TestObject.file(
            TEST_DIR, "session/session-outlier-full-1.xml", SESSION_OUTLIER_FULL_1_OID);
    private static final TestTask TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_FULL_1 =
            new TestTask(TEST_DIR, "task/task-role-analysis-process-session-outlier-full-1.xml",
                    "55e836f6-4e43-485f-bdc3-a95858d3492f", DEFAULT_TIMEOUT);

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
                    SESSION_ROLE_MINING_DIRECT_1,
                    SESSION_OUTLIER_PART_1,
                    SESSION_OUTLIER_FULL_1);
        }
    }

    /**
     * Test role mining session defined in {@code session-role-mining-1.xml}.
     * - user-based, grouped by org assignment
     */
    @Test
    public void test010RoleAnalysisSessionRoleMining1() throws Exception {
        Integer expectedObjectsCount = 410;
        Integer expectedClusterCount = 6;
        Double expectedMeanDensity = 93.67271227874105;
        Integer expectedReduction = 856;

        RoleMiningResult expectedResult = new RoleMiningResult(
                expectedObjectsCount,
                expectedClusterCount,
                expectedMeanDensity,
                expectedReduction
        );

        runRoleMiningTest(
                SESSION_ROLE_MINING_1_OID,
                TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_1,
                expectedResult
        );
    }

    /**
     * Test role mining session defined in {@code session-role-mining-role-mode-1.xml}.
     * - role-based, no grouping
     */
    @Test
    public void test020RoleAnalysisSessionRoleMiningRoleMode1() throws Exception {
        Integer expectedObjectsCount = 182;
        Integer expectedClusterCount = 9;
        Double expectedMeanDensity = 92.73360744156639;
        Integer expectedReduction = 3133;

        RoleMiningResult expectedResult = new RoleMiningResult(
                expectedObjectsCount,
                expectedClusterCount,
                expectedMeanDensity,
                expectedReduction
        );

        runRoleMiningTest(
                SESSION_ROLE_MINING_ROLE_MODE_1_OID,
                TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_ROLE_MODE_1,
                expectedResult
        );
    }

    /**
     * Test role mining session defined in {@code session-role-mining-direct-1.xml}.
     * - user-based, direct mode
     */
    @Test
    public void test025RoleAnalysisSessionRoleMiningDirectMode() throws Exception {
        Integer expectedObjectsCount = 412;
        Integer expectedClusterCount = 7;
        Double expectedMeanDensity = 100.0;
        Integer expectedReduction = 856;

        RoleMiningResult expectedResult = new RoleMiningResult(
                expectedObjectsCount,
                expectedClusterCount,
                expectedMeanDensity,
                expectedReduction
        );

        runRoleMiningTest(
                SESSION_ROLE_MINING_DIRECT_1_OID,
                TASK_ROLE_ANALYSIS_PROCESS_SESSION_ROLE_MINING_DIRECT_1,
                expectedResult
        );
    }

    /**
     * Test outlier detection session defined in {@code session-outlier-part-1.xml}.
     * - analyzes only in-cluster outliers
     */
    @Test
    public void test030RoleAnalysisSessionOutlierPart1() throws Exception {
        Integer expectedObjectsCount = 410;
        Integer expectedInnerOutlierCount = 29;
        Integer expectedOuterOutlierCount = 0;
        Double expectedF1score = 0.4999999999999999;
        Double expectedTopOutlierConfidence = 84.37738080562278;

        OutlierDetectionResult expectedResult = new OutlierDetectionResult(
                expectedObjectsCount,
                expectedInnerOutlierCount,
                expectedOuterOutlierCount,
                expectedF1score,
                expectedTopOutlierConfidence
        );

        runOutlierDetectionTest(
                SESSION_OUTLIER_PART_1_OID,
                TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_PART_1,
                expectedResult
        );
    }

    /**
     * Test outlier detection session defined in {@code session-outlier-full-1.xml}.
     * - detailed analysis
     * - analyzes both in-cluster and out-cluster outliers
     */
    @Test
    public void test040RoleAnalysisSessionOutlierFull1() throws Exception {
        Integer expectedObjectsCount = 410;
        Integer expectedInnerOutlierCount = 29;
        Integer expectedOuterOutlierCount = 241;
        Double expectedF1score = 0.3586626139817629;
        Double expectedTopOutlierConfidence = 84.37738080562278;

        OutlierDetectionResult expectedResult = new OutlierDetectionResult(
                expectedObjectsCount,
                expectedInnerOutlierCount,
                expectedOuterOutlierCount,
                expectedF1score,
                expectedTopOutlierConfidence
        );

        runOutlierDetectionTest(
                SESSION_OUTLIER_FULL_1_OID,
                TASK_ROLE_ANALYSIS_PROCESS_SESSION_OUTLIER_FULL_1,
                expectedResult
        );
    }

    private void runRoleMiningTest(String sessionId, TestTask testTask, RoleMiningResult expectedResult) throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run");
        testTask.init(this, task, result);
        testTask.rerun(result);

        then("task is OK and result is as expected");
        testTask.assertAfter()
                .display()
                .assertProgress(FINAL_TASK_STAGE);

        RoleAnalysisSessionType session = getSession(sessionId);
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        List<RoleAnalysisClusterType> clusters = getClusters(sessionId);
        Integer actualReduction = clusters
                .stream()
                .mapToInt(cluster -> cluster.getClusterStatistics().getDetectedReductionMetric().intValue())
                .sum();

        RoleMiningResult actualResult = new RoleMiningResult(
                sessionStatistic.getProcessedObjectCount(),
                sessionStatistic.getClusterCount(),
                sessionStatistic.getMeanDensity(),
                actualReduction
        );

        assertEquals(expectedResult, actualResult);
        assertObjects(RoleAnalysisClusterType.class, buildClustersQuery(sessionId), expectedResult.clusterCount());
    }

    private void runOutlierDetectionTest(String sessionId, TestTask testTask, OutlierDetectionResult expectedResult) throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run");
        testTask.init(this, task, result);
        testTask.rerun(result); // asserts success

        then("task is OK and result is as expected");
        testTask.assertAfter()
                .display()
                .assertProgress(FINAL_TASK_STAGE);

        RoleAnalysisSessionType session = getSession(sessionId);
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        var innerOutliers = getOutliers(sessionId, OutlierClusterCategoryType.INNER_OUTLIER);
        var outerOutliers = getOutliers(sessionId, OutlierClusterCategoryType.OUTER_OUTLIER);

        Double actualTopOutlierConfidence = Stream.concat(innerOutliers.stream(), outerOutliers.stream())
                .map(RoleAnalysisOutlierType::getOverallConfidence)
                .reduce(Double::max)
                .orElseThrow();

        var evaluation = new DebugOutlierDetectionEvaluation(
                sessionId,
                modelService,
                roleAnalysisService,
                createTask("evaluate outlier detection")
        ).evaluate();
        display(evaluation.toString());

        OutlierDetectionResult actualResult = new OutlierDetectionResult(
                sessionStatistic.getProcessedObjectCount(),
                innerOutliers.size(),
                outerOutliers.size(),
                evaluation.getF1Score(),
                actualTopOutlierConfidence
        );

        assertEquals(expectedResult, actualResult);
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

    private List<RoleAnalysisClusterType> getClusters(String sessionOid) throws Exception {
        Task task = createTask("get clusters");
        ObjectQuery query = buildClustersQuery(sessionOid);
        return modelService
                .searchObjects(RoleAnalysisClusterType.class, query, null, task, task.getResult())
                .stream()
                .map(result -> result.asObjectable())
                .toList();
    }

    private List<RoleAnalysisOutlierType> getOutliers(String sessionOid, OutlierClusterCategoryType category) {
        Task task = createTask("get outliers");
        return roleAnalysisService.getSessionOutliers(sessionOid, category, task, task.getResult());
    }

}
