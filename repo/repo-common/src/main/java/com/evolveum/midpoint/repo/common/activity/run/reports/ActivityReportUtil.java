/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;

public class ActivityReportUtil {

    /** Adds item-related information to a record that is related to processing of items. */
    public static void addItemInformation(@NotNull ItemRelatedRecordType record, @Nullable ItemProcessingRequest<?> request,
            @Nullable WorkBucketType bucket) {
        if (request != null) {
            IterationItemInformation iterationItemInformation = request.getIterationItemInformation();
            record.itemSequentialNumber(request.getSequentialNumber())
                    .itemName(iterationItemInformation.getObjectName())
                    .itemOid(iterationItemInformation.getObjectOid());
        }
        if (bucket != null) {
            record.bucketSequentialNumber(bucket.getSequentialNumber());
        }
    }

    public static @Nullable String getReportDataOid(@NotNull TaskActivityStateType taskActivityState,
            @NotNull ActivityPath path, @NotNull ItemName reportKind, @NotNull String nodeId) {
        ActivityStateType state = ActivityStateUtil.getActivityState(taskActivityState, path);
        if (state == null || state.getReports() == null) {
            return null;
        }
        //noinspection unchecked
        PrismContainer<ActivityReportCollectionType> collectionContainer =
                state.getReports().asPrismContainerValue().findContainer(reportKind);
        if (collectionContainer == null || collectionContainer.isEmpty()) {
            return null;
        }
        ActivityReportCollectionType collection = collectionContainer.getRealValue(ActivityReportCollectionType.class);
        return collection.getRawDataRef().stream()
                .filter(ref -> nodeId.equals(ref.getDescription()))
                .findFirst()
                .map(ObjectReferenceType::getOid)
                .orElse(null);
    }
}
