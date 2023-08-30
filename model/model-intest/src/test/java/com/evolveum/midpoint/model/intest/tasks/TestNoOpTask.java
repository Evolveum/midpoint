/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.repo.common.activity.run.reports.SimpleReportReader;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests various activity/task aspects using NoOp activity.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNoOpTask extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/noop");

    private static final TestObject<TaskType> TASK_BUCKET_ANALYSIS_WITH_CONDITION =
            TestObject.file(TEST_DIR, "task-bucket-analysis-with-condition.xml", "a0e77ba4-ca5c-41e7-8020-e4d48f4165c5");
    private static final TestObject<TaskType> TASK_BUCKET_ANALYSIS_WITH_REGULAR_SAMPLING =
            TestObject.file(TEST_DIR, "task-bucket-analysis-with-regular-sampling.xml", "61534931-bee7-4dbf-b5c8-8710bf61489d");
    private static final TestObject<TaskType> TASK_BUCKET_ANALYSIS_WITH_RANDOM_SAMPLING =
            TestObject.file(TEST_DIR, "task-bucket-analysis-with-random-sampling.xml", "8c515cb9-19f9-434e-88f4-6bc2519cd9cf");

    @Test
    public void test100BucketAnalysisUsingCondition() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        addTask(TASK_BUCKET_ANALYSIS_WITH_CONDITION, result);
        waitForTaskCloseOrSuspend(TASK_BUCKET_ANALYSIS_WITH_CONDITION.oid, 10000);

        then();

        // @formatter:off
        TaskType taskAfter = assertTask(TASK_BUCKET_ANALYSIS_WITH_CONDITION.oid, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .rootActivityState()
                    .bucketManagementStatistics()
                        .display()
                        .assertComplete(20) // These are all the buckets. We must complete also those that are skipped.
                    .end()
                .end()
                .getObjectable();
        // @formatter:on

        String reportOid = getBucketReportDataOid(taskAfter, ActivityPath.empty());

        try (var reader = SimpleReportReader.createForLocalReportData(
                reportOid, List.of("sequentialNumber", "content-from", "content-to"), commonTaskBeans, result)) {
            List<List<String>> rows = reader.getRows();
            displayValue("rows of bucket analysis report", rows);
            assertThat(rows).as("rows").hasSize(5);
            assertThat(rows.get(0)).as("row 0").containsExactly("4", "1500", "2000");
            assertThat(rows.get(1)).as("row 1").containsExactly("8", "3500", "4000");
            assertThat(rows.get(2)).as("row 2").containsExactly("12", "5500", "6000");
            assertThat(rows.get(3)).as("row 3").containsExactly("16", "7500", "8000");
            assertThat(rows.get(4)).as("row 4").containsExactly("20", "9500", "10000");
        }
    }

    @Test
    public void test110BucketAnalysisUsingRegularSampling() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        addTask(TASK_BUCKET_ANALYSIS_WITH_REGULAR_SAMPLING, result);
        waitForTaskCloseOrSuspend(TASK_BUCKET_ANALYSIS_WITH_REGULAR_SAMPLING.oid, 10000);

        then();

        // @formatter:off
        TaskType taskAfter = assertTask(TASK_BUCKET_ANALYSIS_WITH_REGULAR_SAMPLING.oid, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .rootActivityState()
                    .bucketManagementStatistics()
                        .display()
                        .assertComplete(10) // Great! Skipped buckets do not undergo get->complete process.
                    .end()
                .end()
                .getObjectable();
        // @formatter:on

        String reportOid = getBucketReportDataOid(taskAfter, ActivityPath.empty());

        try (var reader = SimpleReportReader.createForLocalReportData(
                reportOid, List.of("sequentialNumber", "content-from", "content-to"), commonTaskBeans, result)) {
            List<List<String>> rows = reader.getRows();
            displayValue("rows of bucket analysis report", rows);
            assertThat(rows).as("rows").hasSize(10);
            assertThat(rows.get(0)).as("row 0").containsExactly("20", "950", "1000");
            assertThat(rows.get(1)).as("row 1").containsExactly("40", "1950", "2000");
            assertThat(rows.get(2)).as("row 2").containsExactly("60", "2950", "3000");
            assertThat(rows.get(3)).as("row 3").containsExactly("80", "3950", "4000");
            assertThat(rows.get(9)).as("row 9").containsExactly("200", "9950", "10000");
        }
    }

    @Test
    public void test120BucketAnalysisUsingRandomSampling() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        addTask(TASK_BUCKET_ANALYSIS_WITH_RANDOM_SAMPLING, result);
        waitForTaskCloseOrSuspend(TASK_BUCKET_ANALYSIS_WITH_RANDOM_SAMPLING.oid, 10000);

        then();

        // @formatter:off
        TaskType taskAfter = assertTask(TASK_BUCKET_ANALYSIS_WITH_RANDOM_SAMPLING.oid, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .rootActivityState()
                    .bucketManagementStatistics()
                        .display() // We don't know how many are these (because of the randomness)
                    .end()
                .end()
                .getObjectable();
        // @formatter:on

        String reportOid = getBucketReportDataOid(taskAfter, ActivityPath.empty());

        try (var reader = SimpleReportReader.createForLocalReportData(
                reportOid, List.of("sequentialNumber", "content-from", "content-to"), commonTaskBeans, result)) {
            List<List<String>> rows = reader.getRows();
            displayValue("rows of bucket analysis report", rows);
            // We don't know how many are these (because of the randomness)
        }
    }
}
