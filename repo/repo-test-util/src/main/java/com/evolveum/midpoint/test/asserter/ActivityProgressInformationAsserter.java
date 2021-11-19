/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.schema.util.task.*;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation.RealizationState;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserter that checks {@link ActivityProgressInformation} objects.
 *
 * For checking raw progress data, see {@link ActivityProgressAsserter}.
 */
public class ActivityProgressInformationAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityProgressInformation information;

    public ActivityProgressInformationAsserter(ActivityProgressInformation information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public ActivityProgressInformationAsserter<RA> assertChildren(int expected) {
        assertThat(information.getChildren().size()).as("# of children").isEqualTo(expected);
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertNoChildren() {
        return assertChildren(0);
    }

    public ActivityProgressInformationAsserter<RA> assertComplete() {
        return assertRealizationState(RealizationState.COMPLETE); // equivalent to testing isComplete == true
    }

    public ActivityProgressInformationAsserter<RA> assertInProgress() {
        return assertRealizationState(RealizationState.IN_PROGRESS);
    }

    public ActivityProgressInformationAsserter<RA> assertNotStarted() {
        return assertRealizationState(null);
    }

    @SuppressWarnings("unused")
    public ActivityProgressInformationAsserter<RA> assertNotComplete() {
        assertThat(information.isComplete()).as("Complete").isFalse();
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertRealizationState(RealizationState expected) {
        assertThat(information.getRealizationState()).as("realization state").isEqualTo(expected);
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertIdentifier(String expected) {
        assertThat(information.getActivityIdentifier()).as("activity identifier").isEqualTo(expected);
        return this;
    }

    @SuppressWarnings("unused")
    public ActivityProgressInformationAsserter<RA> assertPath(ActivityPath expected) {
        assertThat(information.getActivityPath()).as("activity path").isEqualTo(expected);
        return this;
    }

    public ActivityProgressInformationAsserter<ActivityProgressInformationAsserter<RA>> child(String identifier) {
        ActivityProgressInformation child = information.getChild(identifier);
        assertThat(child).as("child information for: " + identifier).isNotNull();
        ActivityProgressInformationAsserter<ActivityProgressInformationAsserter<RA>> asserter =
                new ActivityProgressInformationAsserter<>(child, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityProgressInformationAsserter<RA> checkConsistence() {
        information.checkConsistence();
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertExpectedBuckets(Integer expectedTotal) {
        Integer total = information.getBucketsProgress() != null ? information.getBucketsProgress().getExpectedBuckets() : null;
        assertThat(total).as("total").isEqualTo(expectedTotal);
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertBuckets(int expectedCompleted, Integer expectedTotal) {
        int completed = getCompletedBuckets();
        Integer total = information.getBucketsProgress() != null ? information.getBucketsProgress().getExpectedBuckets() : null;
        assertThat(completed).as("completed").isEqualTo(expectedCompleted);
        assertThat(total).as("total").isEqualTo(expectedTotal);
        return this;
    }

    private int getCompletedBuckets() {
        return information.getBucketsProgress() != null ? information.getBucketsProgress().getCompleteBuckets() : 0;
    }

    private int getCompletedItems() {
        return information.getItemsProgress() != null ? information.getItemsProgress().getProgress() : 0;
    }

    public ActivityProgressInformationAsserter<RA> assertItems(int progress, Integer expectedProgress) {
        assertThat(getProgress()).as("progress").isEqualTo(progress);
        assertThat(getExpectedProgress()).as("expected progress").isEqualTo(expectedProgress);
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertItems(int progress, int errors, Integer expectedProgress) {
        assertThat(getProgress()).as("progress").isEqualTo(progress);
        assertThat(getErrors()).as("errors").isEqualTo(errors);
        assertThat(getExpectedProgress()).as("expected progress").isEqualTo(expectedProgress);
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertExpectedItems(Integer expectedTotal) {
        assertThat(getExpectedProgress()).as("expected progress").isEqualTo(expectedTotal);
        return this;
    }

    private int getProgress() {
        ItemsProgressInformation itemsProgress = information.getItemsProgress();
        return itemsProgress != null ? itemsProgress.getProgress() : 0;
    }

    private int getErrors() {
        ItemsProgressInformation itemsProgress = information.getItemsProgress();
        return itemsProgress != null ? itemsProgress.getErrors() : 0;
    }

    private @Nullable Integer getExpectedProgress() {
        ItemsProgressInformation itemsProgress = information.getItemsProgress();
        return itemsProgress != null ? itemsProgress.getExpectedProgress() : null;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityProgressInformationAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(information));
        return this;
    }

    public ActivityProgressInformation get() {
        return information;
    }

    public ActivityProgressInformationAsserter<RA> assertBucketsItemsConsistency(int bucketSize, int workers) {
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

    public ActivityProgressInformationAsserter<RA> assertCompletedBucketsAtLeast(int expected) {
        int completedBuckets = getCompletedBuckets();
        assertThat(completedBuckets).as("completed buckets").isGreaterThanOrEqualTo(expected);
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertItemsProgressAtLeast(int expected) {
        int completedItems = getCompletedItems();
        assertThat(completedItems).as("completed items").isGreaterThanOrEqualTo(expected);
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertNoBucketInformation() {
        assertThat(information.getBucketsProgress()).isNull();
        return this;
    }

    public ActivityProgressInformationAsserter<RA> assertNoItemsInformation() {
        assertThat(information.getItemsProgress()).isNull();
        return this;
    }

    public ItemsProgressInformationAsserter<ActivityProgressInformationAsserter<RA>> items() {
        ItemsProgressInformation items = information.getItemsProgress();
        assertThat(items).as("items progress information").isNotNull();
        ItemsProgressInformationAsserter<ActivityProgressInformationAsserter<RA>> asserter =
                new ItemsProgressInformationAsserter<>(items, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
