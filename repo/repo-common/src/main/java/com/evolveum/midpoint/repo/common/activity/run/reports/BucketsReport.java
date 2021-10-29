/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityReportsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BucketProcessingRecordType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BucketsProcessingReportDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Represents a report on individual buckets.
 */
public class BucketsReport extends AbstractReport {

    private static final ItemPath STATE_ITEM_PATH = ItemPath.create(ActivityStateType.F_REPORTS, ActivityReportsType.F_BUCKETS);

    public BucketsReport(@Nullable BucketsProcessingReportDefinitionType definition,
            @NotNull CurrentActivityState<?> activityState, @NotNull Kind kind) {
        super(definition, BucketProcessingRecordType.COMPLEX_TYPE, activityState, kind.itemsIncluded);
    }

    @Override
    String getReportType() {
        return "buckets";
    }

    /**
     * Writes a line about bucket that was just completed.
     *
     * No need to synchronize, because it is called from the main activity run thread only.
     */
    public void recordBucketCompleted(@NotNull BucketProcessingRecordType record, @NotNull RunningTask task,
            @NotNull OperationResult result) {

        if (isRejected(record, task, result)) {
            return;
        }

        openIfClosed(result);
        writeRecord(record);
    }

    @Override
    @NotNull ItemPath getStateItemPath() {
        return STATE_ITEM_PATH;
    }

    public enum Kind {

        EXECUTION(List.of()), // all items
        ANALYSIS(List.of(
                BucketProcessingRecordType.F_SEQUENTIAL_NUMBER,
                BucketProcessingRecordType.F_CONTENT,
                BucketProcessingRecordType.F_SIZE));

        @NotNull private final Collection<ItemName> itemsIncluded;

        Kind(@NotNull Collection<ItemName> itemsIncluded) {
            this.itemsIncluded = itemsIncluded;
        }

        public @NotNull Collection<ItemName> getItemsIncluded() {
            return itemsIncluded;
        }
    }
}
