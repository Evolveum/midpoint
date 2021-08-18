/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.statistics.ActivityBucketManagementStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.work.BucketingConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketManagementStatisticsType;

/**
 *  Asserter that checks bucket operation statistics.
 */
@SuppressWarnings("WeakerAccess")
public class ActivityBucketManagementStatisticsAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityBucketManagementStatisticsType information;

    ActivityBucketManagementStatisticsAsserter(ActivityBucketManagementStatisticsType information, RA returnAsserter,
            String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public ActivityBucketManagementStatisticsAsserter<RA> assertEntries(int expected) {
        assertThat(information.getOperation().size()).as("# of entries").isEqualTo(expected);
        return this;
    }

    public ActivityBucketManagementStatisticsAsserter<RA> assertCreatedNew(int expected) {
        assertThat(getCounter(BucketingConstants.GET_WORK_BUCKET_CREATED_NEW)).as("created new").isEqualTo(expected);
        return this;
    }

    public ActivityBucketManagementStatisticsAsserter<RA> assertComplete(int expected) {
        assertThat(getCounter(BucketingConstants.COMPLETE_WORK_BUCKET)).as("complete").isEqualTo(expected);
        return this;
    }

    public ActivityBucketManagementStatisticsAsserter<RA> assertNoMoreBucketsDefinite(int expected) {
        assertThat(getCounter(BucketingConstants.GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE))
                .as("no more buckets - definite")
                .isEqualTo(expected);
        return this;
    }

    private int getCounter(@NotNull String operationName) {
        return information.getOperation().stream()
                .filter(op -> operationName.equals(op.getName()))
                .map(op -> or0(op.getCount()))
                .findFirst()
                .orElse(0);
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityBucketManagementStatisticsAsserter<RA> display() {
        IntegrationTestTools.display(desc(), ActivityBucketManagementStatisticsUtil.format(information));
        return this;
    }
}
