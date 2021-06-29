/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityPerformanceInformation;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.TreeNode;

/**
 *  Asserter that checks {@link ActivityPerformanceInformation} objects.
 */
public class ActivityPerformanceInformationAsserter<RA> extends AbstractAsserter<RA> {

    private final TreeNode<ActivityPerformanceInformation> node;
    private final ActivityPerformanceInformation information;

    public ActivityPerformanceInformationAsserter(TreeNode<ActivityPerformanceInformation> node,
            RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.node = node;
        this.information = node.getUserObject();
    }

    public ActivityPerformanceInformationAsserter<RA> assertChildren(int expected) {
        assertThat(node.getChildren().size()).as("# of children").isEqualTo(expected);
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertNoChildren() {
        return assertChildren(0);
    }

    public ActivityPerformanceInformationAsserter<RA> assertPath(ActivityPath expected) {
        assertThat(information.getActivityPath()).as("activity path").isEqualTo(expected);
        return this;
    }

    public ActivityPerformanceInformationAsserter<ActivityPerformanceInformationAsserter<RA>> child(String identifier) {
        TreeNode<ActivityPerformanceInformation> child = node.getChildren().stream()
                .filter(c -> Objects.equals(c.getUserObject().getActivityPath().last(), identifier))
                .findFirst().orElse(null);
        assertThat(child).as("child information for: " + identifier).isNotNull();
        ActivityPerformanceInformationAsserter<ActivityPerformanceInformationAsserter<RA>> asserter =
                new ActivityPerformanceInformationAsserter<>(child, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityPerformanceInformationAsserter<RA> assertApplicable() {
        assertThat(information.isApplicable()).as("applicable").isTrue();
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertNotApplicable() {
        assertThat(information.isApplicable()).as("applicable").isFalse();
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertItemsProcessed(int expected) {
        assertThat(information.getItemsProcessed()).as("items processed").isEqualTo(expected);
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertErrors(int expected) {
        assertThat(information.getErrors()).as("errors").isEqualTo(expected);
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertProgress(int expected) {
        assertThat(information.getProgress()).as("progress").isEqualTo(expected);
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertHasWallClockTime() {
        assertThat(information.getWallClockTime()).as("wall clock time").isNotNull();
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertHasThroughput() {
        assertThat(information.getThroughput()).as("throughput").isNotNull();
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertNoThroughput() {
        assertThat(information.getThroughput()).as("throughput").isNull();
        return this;
    }

    public ActivityPerformanceInformationAsserter<RA> assertHasEarliestStartTime() {
        assertThat(information.getEarliestStartTime()).as("earliest start time").isNotNull();
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityPerformanceInformationAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(node));
        return this;
    }

    public TreeNode<ActivityPerformanceInformation> get() {
        return node;
    }
}
