/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityItemProcessingStatisticsType;

/**
 *  Asserter that checks iterative task information.
 */
@SuppressWarnings("WeakerAccess")
public class ActivityItemProcessingStatisticsAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityItemProcessingStatisticsType information;

    ActivityItemProcessingStatisticsAsserter(ActivityItemProcessingStatisticsType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertRuns(int expected) {
        assertThat(information.getRun().size()).as("# of runs").isEqualTo(expected);
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertTotalCounts(int nonFailure, int failure) {
        assertNonFailureCount(nonFailure);
        assertFailureCount(failure);
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertTotalCounts(int success, int failure, int skip) {
        assertSuccessCount(success);
        assertFailureCount(failure);
        assertSkipCount(skip);
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertNonFailureCount(int success) {
        assertEquals("Wrong value of total 'non-failure' counter", success, getNonFailureCount());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertSuccessCount(int success) {
        assertEquals("Wrong value of total success counter", success, getSuccessCount());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertSkipCount(int skip) {
        assertEquals("Wrong value of total skip counter", skip, getSkipCount());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertSuccessCount(int min, int max) {
        assertMinMax("Total success counter", min, max, getSuccessCount());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertFailureCount(int failure) {
        assertEquals("Wrong value of total failure counter", failure, getFailureCount());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertFailureCount(int min, int max) {
        assertMinMax("Total failure counter", min, max, getFailureCount());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertLastSuccessObjectName(String expected) {
        assertEquals("Wrong 'last success' object name", expected, getLastSuccessObjectName());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertLastSuccessObjectOid(String expected) {
        assertEquals("Wrong 'last success' object oid", expected, getLastSuccessObjectOid());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertLastFailureObjectName(String expected) {
        assertEquals("Wrong 'last failure' object name", expected, getLastFailedObjectName());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertLastFailureMessage(String expected) {
        assertEquals("Wrong 'last failure' message", expected, getLastFailedItemMessage());
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<RA> assertLastFailureMessageContaining(String expected) {
        assertThat(getLastFailedItemMessage()).as("last failure message").contains(expected);
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityItemProcessingStatisticsAsserter<RA> display() {
        IntegrationTestTools.display(desc(), ActivityItemProcessingStatisticsUtil.format(information));
        return this;
    }

    private int getSuccessCount() {
        return ActivityItemProcessingStatisticsUtil.getItemsProcessedWithSuccessShallow(information);
    }

    private int getFailureCount() {
        return ActivityItemProcessingStatisticsUtil.getItemsProcessedWithFailureShallow(information);
    }

    private int getSkipCount() {
        return ActivityItemProcessingStatisticsUtil.getItemsProcessedWithSkipShallow(information);
    }

    private int getNonFailureCount() {
        return getSuccessCount() + getSkipCount();
    }

    private String getLastSuccessObjectName() {
        return ActivityItemProcessingStatisticsUtil.getLastProcessedObjectName(information, OutcomeKeyedCounterTypeUtil::isSuccess);
    }

    private String getLastSuccessObjectOid() {
        return ActivityItemProcessingStatisticsUtil.getLastProcessedObjectOid(information, OutcomeKeyedCounterTypeUtil::isSuccess);
    }

    private String getLastFailedObjectName() {
        return ActivityItemProcessingStatisticsUtil.getLastProcessedObjectName(information, OutcomeKeyedCounterTypeUtil::isFailure);
    }

    private String getLastFailedItemMessage() {
        return ActivityItemProcessingStatisticsUtil.getLastProcessedItemMessage(
                information, OutcomeKeyedCounterTypeUtil::isFailure);
    }
}
