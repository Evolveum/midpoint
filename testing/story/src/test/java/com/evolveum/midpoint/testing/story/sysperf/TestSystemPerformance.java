/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

import com.google.common.base.MoreObjects;
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
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.schema.util.task.TaskPerformanceInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the overall system performance. Can be parameterized using Java properties.
 *
 * Examples:
 *
 * -Dpopulation=10k -Dthreading=16t -Dschema=basic -Dsource=ms2-5
 * -Dpopulation=100k -Dthreading=4t -Dschema=indexed -Dsource=ms110-5
 * -Dpopulation=10k -Dthreading=16t -Dschema=big-global -Dsource=ms110-1000
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSystemPerformance extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "system-perf");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final ExtensionSchemaVariant EXTENSION_SCHEMA_VARIANT;
    private static final SourceVariant SOURCE_VARIANT;
    private static final ThreadingVariant THREADING_VARIANT;
    private static final TemplateVariant TEMPLATE_VARIANT;
    private static final PopulationVariant POPULATION_VARIANT;

    private static final DummyTestResource RESOURCE_SOURCE;
    private static final TestResource<TaskType> TASK_IMPORT;

    private static final long START = System.currentTimeMillis();

    private static final File SUMMARY_FILE = new File("./target", START + "-summary.txt");
    private PrintWriter summary;

    private static final File PROGRESS_FILE = new File("./target", START + "-progress.csv");
    private PrintWriter progress;

    private long lastProgress;

    private SourceInitializer sourceInitializer;

    static {
        EXTENSION_SCHEMA_VARIANT = ExtensionSchemaVariant.setup();
        SOURCE_VARIANT = SourceVariant.setup();
        THREADING_VARIANT = ThreadingVariant.setup();
        TEMPLATE_VARIANT = TemplateVariant.setup();
        POPULATION_VARIANT = PopulationVariant.setup();

        RESOURCE_SOURCE = SOURCE_VARIANT.createDummyTestResource();
        TASK_IMPORT = THREADING_VARIANT.getImportTaskResource();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyAuditService.getInstance().setEnabled(false);

        InternalsConfig.turnOffAllChecks();

        sourceInitializer = new SourceInitializer(this, RESOURCE_SOURCE, SOURCE_VARIANT, POPULATION_VARIANT, initTask);
        sourceInitializer.run(initResult);

        summary = new PrintWriter(new FileWriter(SUMMARY_FILE));

        progress = new PrintWriter(new FileWriter(PROGRESS_FILE));
        progress.println("time;progress");

        logStart();
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

        addTask(TASK_IMPORT, result);
        waitForTaskFinish(TASK_IMPORT.oid, false, 0, 1000000, false, 0,
                builder -> builder.taskConsumer(this::recordProgress));

        then();

        int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
        assertThat(usersAfter - usersBefore).as("users created").isEqualTo(POPULATION_VARIANT.getAccounts());

        assertUserAfterByUsername(sourceInitializer.getAccountName(0))
                .assertLinks(1, 0)
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

        logFinish(taskAfter);
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

    private void logStart() {
        logger.info("********** STARTED **********\n");
        logger.info("Extension schema variant: {}", EXTENSION_SCHEMA_VARIANT);
        logger.info("Source variant: {}", SOURCE_VARIANT);
        logger.info("Threading variant: {}", THREADING_VARIANT);
        logger.info("Population variant: {}", POPULATION_VARIANT);
        logger.info("Progress file: {}", PROGRESS_FILE);

        summary.println("Started: " + new Date(START) + " (" + START + ")");
        summary.printf("Extension schema variant: %s\n", EXTENSION_SCHEMA_VARIANT);
        summary.printf("Source variant: %s\n", SOURCE_VARIANT);
        summary.printf("Threading variant: %s\n", THREADING_VARIANT);
        summary.printf("Population variant: %s\n", POPULATION_VARIANT);
        summary.printf("Progress file: %s\n\n", PROGRESS_FILE);
        summary.flush();
    }

    private void logFinish(PrismObject<TaskType> taskAfter) {
        TaskPerformanceInformation performanceInformation = TaskPerformanceInformation.fromTaskTree(taskAfter.asObjectable());
        long executionTime = getExecutionTime(taskAfter);

        logger.info("********** FINISHED **********\n");
        logger.info("{}", String.format("Task execution time: %,d ms", executionTime));
        logger.info("{}", String.format("Time per account: %,.1f ms", (double) executionTime / (double) POPULATION_VARIANT.getAccounts()));
        logger.info("{}", (TaskOperationStatsUtil.format(taskAfter.asObjectable().getOperationStats())));
        logger.info("{}", performanceInformation.debugDump());

        summary.printf("********** FINISHED **********\n");
        summary.printf("Task execution time: %,d ms\n", executionTime);
        summary.printf("Time per account: %,.1f ms\n", (double) executionTime / (double) POPULATION_VARIANT.getAccounts());
        summary.println(TaskOperationStatsUtil.format(taskAfter.asObjectable().getOperationStats()));
        summary.println(performanceInformation.debugDump());
        summary.flush();
    }

    private void recordProgress(Task task) {
        Long start = task.getLastRunStartTimestamp();
        long progress = task.getProgress();

        if (start == null || progress == lastProgress) {
            return;
        }
        lastProgress = progress;

        long end = MoreObjects.firstNonNull(task.getLastRunFinishTimestamp(), System.currentTimeMillis());
        long running = end - start;
        this.progress.println(running + ";" + progress);
        this.progress.flush();

        OperationStatsType stats = task.getStoredOperationStatsOrClone();
        logger.info("\n{}", TaskOperationStatsUtil.format(stats));

        TaskPerformanceInformation performanceInformation = TaskPerformanceInformation.fromTaskTree(
                task.getRawTaskObjectClone().asObjectable());
        displayDumpable("performance", performanceInformation);
    }
}
