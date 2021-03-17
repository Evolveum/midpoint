/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.schema.util.task.ItemsProgressInformation;
import com.evolveum.midpoint.schema.util.task.TaskPartProgressInformation;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;

/**
 *  Asserter that checks {@link TaskPartProgressInformation} objects.
 */
@SuppressWarnings("unused")
public class TaskPartProgressInformationAsserter<RA> extends AbstractAsserter<RA> {

    private final TaskPartProgressInformation information;

    TaskPartProgressInformationAsserter(TaskPartProgressInformation information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public TaskPartProgressInformationAsserter<RA> assertUri(String expected) {
        assertThat(information.getPartOrHandlerUri()).as("URI").isEqualTo(expected);
        return this;
    }

    public TaskPartProgressInformationAsserter<RA> assertComplete() {
        assertThat(information.isComplete()).as("Complete").isTrue();
        return this;
    }

    public TaskPartProgressInformationAsserter<RA> assertNotComplete() {
        assertThat(information.isComplete()).as("Complete").isFalse();
        return this;
    }

    public TaskPartProgressInformationAsserter<RA> assertExpectedBuckets(Integer expectedTotal) {
        Integer total = information.getBucketsProgress() != null ? information.getBucketsProgress().getExpectedBuckets() : null;
        assertThat(total).as("total").isEqualTo(expectedTotal);
        return this;
    }

    public TaskPartProgressInformationAsserter<RA> assertBuckets(int expectedCompleted, Integer expectedTotal) {
        int completed = getCompletedBuckets();
        Integer total = information.getBucketsProgress() != null ? information.getBucketsProgress().getExpectedBuckets() : null;
        assertThat(completed).as("completed").isEqualTo(expectedCompleted);
        assertThat(total).as("total").isEqualTo(expectedTotal);
        return this;
    }

    private int getCompletedBuckets() {
        return information.getBucketsProgress() != null ? information.getBucketsProgress().getCompletedBuckets() : 0;
    }

    public TaskPartProgressInformationAsserter<RA> assertItems(int expectedProgress, Integer expectedTotal) {
        int progress = information.getItemsProgress() != null ? information.getItemsProgress().getProgress() : 0;
        Integer total = information.getItemsProgress() != null ? information.getItemsProgress().getExpectedTotal() : null;
        assertThat(progress).as("progress").isEqualTo(expectedProgress);
        assertThat(total).as("total").isEqualTo(expectedTotal);
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public TaskPartProgressInformationAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(information));
        return this;
    }

    public TaskPartProgressInformation get() {
        return information;
    }

    public TaskPartProgressInformationAsserter<RA> assertBucketsItemsConsistency(int bucketSize, int workers) {
        int progress = information.getItemsProgress().getProgress();
        int completedBuckets = getCompletedBuckets();
        if (progress < completedBuckets * bucketSize) {
            fail("Progress (" + progress + ") is lower than buckets (" + completedBuckets + ") x size (" + bucketSize + ")");
        } else if (progress > completedBuckets * bucketSize + workers *bucketSize) {
            fail("Progress (" + progress + ") is greater than buckets (" + completedBuckets + ") x size (" + bucketSize
                    + ") + workers (" + workers + ") x size (" + bucketSize + ")");
        }
        return this;
    }

    public TaskPartProgressInformationAsserter<RA> assertCompletedBucketsAtLeast(int expected) {
        int completedBuckets = getCompletedBuckets();
        assertThat(completedBuckets).as("completed buckets").isGreaterThanOrEqualTo(expected);
        return this;
    }

    public TaskPartProgressInformationAsserter<RA> assertNoBucketInformation() {
        assertThat(information.getBucketsProgress()).isNull();
        return this;
    }

    public ItemsProgressInformationAsserter<TaskPartProgressInformationAsserter<RA>> items() {
        ItemsProgressInformation items = information.getItemsProgress();
        assertThat(items).as("items progress information").isNotNull();
        ItemsProgressInformationAsserter<TaskPartProgressInformationAsserter<RA>> asserter =
                new ItemsProgressInformationAsserter<>(items, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
