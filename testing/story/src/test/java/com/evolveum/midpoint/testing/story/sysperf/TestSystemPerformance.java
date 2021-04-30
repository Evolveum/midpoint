/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.midpoint.tools.testng.TestMonitor.PERF_REPORT_PREFIX_PROPERTY_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.tools.testng.TestReportSection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.tools.testng.PerformanceTestMixin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the overall system performance. Can be parameterized using Java properties.
 *
 * Examples:
 *
 *   -Dpopulation=10k -Dthreading=16t -Dschema=basic -Dsource=ms2-5
 *   -Dpopulation=100k -Dthreading=4t -Dschema=indexed -Dsource=ms110-5
 *   -Dpopulation=10k -Dthreading=16t -Dschema=basic -Dsource=ms110-1000
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSystemPerformance extends AbstractStoryTest implements PerformanceTestMixin {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "system-perf");
    static final String TARGET_DIR = "./target";

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    static final ExtensionSchemaVariant EXTENSION_SCHEMA_VARIANT;
    static final SourceVariant SOURCE_VARIANT;
    static final ThreadingVariant THREADING_VARIANT;
    static final TemplateVariant TEMPLATE_VARIANT;
    static final PopulationVariant POPULATION_VARIANT;
    static final OtherParameters OTHER_PARAMETERS;

    private static final DummyTestResource RESOURCE_SOURCE;
    private static final TestResource<TaskType> TASK_IMPORT;
    private static final TestResource<TaskType> TASK_RECOMPUTE;

    static final long START = System.currentTimeMillis();

    private static final String REPORT_FILE_PREFIX = TARGET_DIR + "/" + START + "-report";
    private static final String REPORT_SECTION_SUMMARY_NAME = "summary";
    private static final String REPORT_SECTION_TEST_SUMMARY_NAME = "testSummary";

    private TestReportSection testSummaryReportSection;

    private final ProgressOutputFile progressOutputFile = new ProgressOutputFile();
    private final SummaryOutputFile summaryOutputFile = new SummaryOutputFile();
    private final DetailsOutputFile detailsOutputFile = new DetailsOutputFile();

    private long lastProgress;

    private SourceInitializer sourceInitializer;

    static {
        EXTENSION_SCHEMA_VARIANT = ExtensionSchemaVariant.setup();
        SOURCE_VARIANT = SourceVariant.setup();
        THREADING_VARIANT = ThreadingVariant.setup();
        TEMPLATE_VARIANT = TemplateVariant.setup();
        POPULATION_VARIANT = PopulationVariant.setup();
        OTHER_PARAMETERS = OtherParameters.setup();

        RESOURCE_SOURCE = SOURCE_VARIANT.createDummyTestResource();
        TASK_IMPORT = THREADING_VARIANT.getImportTaskResource();
        TASK_RECOMPUTE = THREADING_VARIANT.getRecomputeTaskResource();

        System.setProperty(PERF_REPORT_PREFIX_PROPERTY_NAME, REPORT_FILE_PREFIX);
    }

    public TestSystemPerformance() throws IOException {
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyAuditService.getInstance().setEnabled(false);

        InternalsConfig.turnOffAllChecks();

        sourceInitializer = new SourceInitializer(this, RESOURCE_SOURCE, SOURCE_VARIANT, POPULATION_VARIANT, initTask);
        sourceInitializer.run(initResult);

        addObject(TASK_IMPORT.file, initTask, initResult, workerThreadsCustomizer(THREADING_VARIANT.getThreads()));
        addObject(TASK_RECOMPUTE.file, initTask, initResult, workerThreadsCustomizer(THREADING_VARIANT.getThreads()));
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false; // we want logging from our system config
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) {
        // nothing here
    }

    @Test
    public void test000LogStart() {

        testMonitor().addReportSection(REPORT_SECTION_SUMMARY_NAME)
                .withColumns("schema", "source", "singleValuedAttributes", "multiValuedAttributes", "attributeValues",
                        "threading", "threads", "tasks",
                        "population", "accounts")
                .addRow(EXTENSION_SCHEMA_VARIANT.getName(),
                        SOURCE_VARIANT.getName(),
                        SOURCE_VARIANT.getSingleValuedAttributes(),
                        SOURCE_VARIANT.getMultiValuedAttributes(),
                        SOURCE_VARIANT.getAttributeValues(),
                        THREADING_VARIANT.getName(),
                        THREADING_VARIANT.getThreads(),
                        THREADING_VARIANT.getTasks(),
                        POPULATION_VARIANT.getName(),
                        POPULATION_VARIANT.getAccounts());


        testSummaryReportSection = testMonitor().addReportSection(REPORT_SECTION_TEST_SUMMARY_NAME)
                .withColumns("test", "time", "timePerAccount");

        logger.info("********** STARTED **********\n");
        logger.info("Extension schema variant: {}", EXTENSION_SCHEMA_VARIANT);
        logger.info("Source variant: {}", SOURCE_VARIANT);
        logger.info("Threading variant: {}", THREADING_VARIANT);
        logger.info("Population variant: {}", POPULATION_VARIANT);
        logger.info("Progress file: {}", ProgressOutputFile.FILE);

        summaryOutputFile.logStart();
    }

    @Test
    public void test001DumpExtensionSchema() {
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismContainerDefinition<?> userExtDef = userDef.getExtensionDefinition();
        System.out.println(userExtDef.debugDump());
    }

    @Test
    public void test100Import() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        int usersBefore = repositoryService.countObjects(UserType.class, null, null, result);

        when();

        restartTask(TASK_IMPORT.oid, result);
        Thread.sleep(500);

        waitForTaskFinish(TASK_IMPORT.oid, false, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                builder -> builder.taskConsumer(this::recordProgress));

        then();

        int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
        assertThat(usersAfter - usersBefore).as("users created").isEqualTo(POPULATION_VARIANT.getAccounts());

        assertUserAfterByUsername(sourceInitializer.getAccountName(0))
                .assertLinks(1)
                .extension()
                    .assertSize(SOURCE_VARIANT.getSingleValuedAttributes() + SOURCE_VARIANT.getMultiValuedAttributes())
                    .property(ItemPath.create(getSingleValuedPropertyName(0)))
                        .assertSize(1)
                        .end()
                    .property(ItemPath.create(getMultiValuedPropertyName(0)))
                        .assertSize(SOURCE_VARIANT.getAttributeValues());

        PrismObject<TaskType> taskAfter = assertTask(TASK_IMPORT.oid, "after")
                .display()
                .getObject();

        logTestFinish(taskAfter);
    }

    @Test
    public void test110ImportAgain() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        int usersBefore = repositoryService.countObjects(UserType.class, null, null, result);

        when();

        restartTask(TASK_IMPORT.oid, result);
        Thread.sleep(500);

        lastProgress = 0;
        waitForTaskFinish(TASK_IMPORT.oid, false, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                builder -> builder.taskConsumer(this::recordProgress));

        then();

        int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
        assertThat(usersAfter - usersBefore).as("users created").isEqualTo(0);

        PrismObject<TaskType> taskAfter = assertTask(TASK_IMPORT.oid, "after")
                .display()
                .getObject();

        logTestFinish(taskAfter);
    }

    @Test
    public void test120RecomputeUsers() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        restartTask(TASK_RECOMPUTE.oid, result);
        Thread.sleep(500);

        lastProgress = 0;
        waitForTaskFinish(TASK_RECOMPUTE.oid, false, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                builder -> builder.taskConsumer(this::recordProgress));

        then();

        PrismObject<TaskType> taskAfter = assertTask(TASK_RECOMPUTE.oid, "after")
                .display()
                .getObject();

        logTestFinish(taskAfter);
    }

    private long getExecutionTime(PrismObject<TaskType> taskAfter) {
        long start = XmlTypeConverter.toMillis(taskAfter.asObjectable().getLastRunStartTimestamp());
        long end = XmlTypeConverter.toMillis(taskAfter.asObjectable().getLastRunFinishTimestamp());
        return end - start;
    }

    private String getSingleValuedPropertyName(int i) {
        return String.format("p-single-%04d", i);
    }

    private String getMultiValuedPropertyName(int i) {
        return String.format("p-multi-%04d", i);
    }

    private void logTestFinish(PrismObject<TaskType> taskAfter) {
        long executionTime = getExecutionTime(taskAfter);
        double timePerAccount = (double) executionTime / (double) POPULATION_VARIANT.getAccounts();

        String testName = getTestNameShort();

        logger.info("********** {} FINISHED **********\n", testName);
        logger.info(String.format("Task execution time: %,d ms", executionTime));
        logger.info(String.format("Time per account: %,.1f ms", timePerAccount));
        logger.info(StatisticsUtil.format(taskAfter.asObjectable().getOperationStats()));

        summaryOutputFile.logTestFinish(testName, executionTime, timePerAccount);
        detailsOutputFile.logTestFinish(testName, taskAfter.asObjectable());

        testSummaryReportSection
                .addRow(testName, executionTime, timePerAccount);
    }

    private void recordProgress(Task task) {
        Long start = task.getLastRunStartTimestamp();
        long progress = task.getProgress();

        if (start == null || progress == lastProgress) {
            return;
        }
        lastProgress = progress;

        progressOutputFile.recordProgress(getTestNameShort(), task);

        OperationStatsType stats = task.getStoredOperationStats();
        logger.info("\n{}", StatisticsUtil.format(stats));
    }
}
