/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.util.task.ActivityBasedTaskInformation;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TestTaskInformation extends AbstractSchemaTest {

    private static final String ROOT_OID = "00000000-0000-0000-0000-000000000001";
    private static final String WORKER_1_OID = "00000000-0000-0000-0000-000000000101";
    private static final String WORKER_2_OID = "00000000-0000-0000-0000-000000000102";

    /**
     * Verifies that worker task progress is taken from the matching worker item overview.
     *
     * The root activity overview contains aggregate bucket progress, but worker rows should
     * display worker-local processed-object counts instead of the coordinator progress.
     */
    @Test
    public void testWorkerProgressUsesWorkerItemOverview() {
        TaskType root = createRootTaskWithWorkerOverview();
        TaskType worker = createWorkerTask(WORKER_1_OID);

        TaskInformation information = TaskInformation.createForTask(worker, root);

        assertThat(information).isInstanceOf(ActivityBasedTaskInformation.class);
        assertThat(information.getProgressDescriptionShort())
                .as("worker progress label")
                .isEqualTo("5");
        assertThat(information.getProgress())
                .as("worker progress bar value")
                .isEqualTo(-1);

        ActivityProgressInformation progressInformation =
                ((ActivityBasedTaskInformation) information).getProgressInformation();
        assertThat(progressInformation.getBucketsProgress())
                .as("worker bucket progress")
                .isNull();
        assertThat(progressInformation.getItemsProgress())
                .as("worker item progress")
                .isNotNull();
        assertThat(progressInformation.getItemsProgress().getProgress())
                .as("worker processed items")
                .isEqualTo(5);
        assertThat(progressInformation.getItemsProgress().getErrors())
                .as("worker failed items")
                .isEqualTo(1);

        TaskInformation information2 = TaskInformation.createForTask(createWorkerTask(WORKER_2_OID), root);
        assertThat(information2.getProgressDescriptionShort())
                .as("second worker progress label")
                .isEqualTo("42");
    }

    private static TaskType createRootTaskWithWorkerOverview() {
        ActivityStateOverviewType overview = new ActivityStateOverviewType()
                .bucketProgress(
                        new BucketProgressOverviewType()
                                .completeBuckets(140)
                                .totalBuckets(257))
                .task(workerOverview(WORKER_1_OID, 2, 1, 2))
                .task(workerOverview(WORKER_2_OID, 42, 0, 0));

        return new TaskType()
                .oid(ROOT_OID)
                .activityState(
                        new TaskActivityStateType()
                                .tree(
                                        new ActivityTreeStateType()
                                                .activity(overview)));
    }

    private static ActivityTaskStateOverviewType workerOverview(
            String oid, int successfullyProcessed, int failed, int skipped) {
        return new ActivityTaskStateOverviewType()
                .taskRef(oid, TaskType.COMPLEX_TYPE)
                .bucketsProcessingRole(BucketsProcessingRoleType.WORKER)
                .progress(
                        new ItemsProgressOverviewType()
                                .successfullyProcessed(successfullyProcessed)
                                .failed(failed)
                                .skipped(skipped));
    }

    private static TaskType createWorkerTask(String oid) {
        ActivityBucketingStateType bucketing = new ActivityBucketingStateType()
                .bucketsProcessingRole(BucketsProcessingRoleType.WORKER);

        ActivityStateType activity = new ActivityStateType()
                .realizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                .bucketing(bucketing);

        TaskActivityStateType activityState = new TaskActivityStateType()
                .localRoot(new ActivityPathType())
                .activity(activity);

        return new TaskType()
                .oid(oid)
                .parent(ROOT_OID)
                .activityState(activityState);
    }
}
