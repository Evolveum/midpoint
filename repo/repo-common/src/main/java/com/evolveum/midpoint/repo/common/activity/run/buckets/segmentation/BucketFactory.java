/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates buckets when needed. See {@link #createNewBuckets(List, int)} method.
 */
public class BucketFactory {

    @NotNull private final BucketContentFactory contentFactory;
    private final WorkAllocationDefinitionType allocationDefinition;

    private BucketFactory(@NotNull BucketContentFactory contentFactory, BucketsDefinitionType bucketing) {
        this.allocationDefinition = bucketing != null ? bucketing.getAllocation() : null;
        this.contentFactory = contentFactory;
    }

    public static BucketFactory create(@Nullable ActivityDistributionDefinition distributionDefinition,
            @Nullable ImplicitSegmentationResolver implicitSegmentationResolver, @NotNull CommonTaskBeans beans) {

        @Nullable BucketsDefinitionType bucketingConfig =
                distributionDefinition != null ? distributionDefinition.getBuckets() : null;

        return new BucketFactory(
                beans.contentFactoryCreator.createContentFactory(bucketingConfig, implicitSegmentationResolver),
                bucketingConfig);
    }

    /**
     * Creates new buckets.
     *
     * @param currentBuckets Current list of buckets. This is used to determine where to start creating new buckets
     * (sequential numbers, boundaries).
     *
     * @param bucketsNeeded How many buckets does the client need? We can create more, if we want to. But not less;
     * unless there is no more buckets available.
     *
     * @return Buckets created. We don't care about the state and worker reference.
     */
    public @NotNull List<WorkBucketType> createNewBuckets(@NotNull List<WorkBucketType> currentBuckets,
            int bucketsNeeded) throws SchemaException {
        List<? extends AbstractWorkBucketContentType> newBucketsContent = createNewBucketsContent(currentBuckets, bucketsNeeded);
        List<WorkBucketType> newBuckets = new ArrayList<>(newBucketsContent.size());
        WorkBucketType lastBucket = BucketingUtil.getLastBucket(currentBuckets);
        int sequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() + 1 : 1;
        for (AbstractWorkBucketContentType newBucketContent : newBucketsContent) {
            newBuckets.add(
                    new WorkBucketType(PrismContext.get())
                            .sequentialNumber(sequentialNumber++)
                            .content(newBucketContent));
        }
        return newBuckets;
    }

    @NotNull
    private List<? extends AbstractWorkBucketContentType> createNewBucketsContent(@NotNull List<WorkBucketType> currentBuckets,
            int bucketsNeeded) throws SchemaException {
        WorkBucketType lastBucket = BucketingUtil.getLastBucket(currentBuckets);
        AbstractWorkBucketContentType lastContent = lastBucket != null ? lastBucket.getContent() : null;
        Integer lastSequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() : null;
        int count = Math.max(getBucketCreationBatch(), bucketsNeeded);
        List<AbstractWorkBucketContentType> newContentList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            AbstractWorkBucketContentType newContent = contentFactory
                    .createNextBucketContent(lastContent, lastSequentialNumber);
            if (newContent == null) {
                break;
            }
            newContentList.add(newContent);
            lastContent = newContent;
            lastSequentialNumber = lastSequentialNumber != null ? lastSequentialNumber + 1 : 1;
        }
        return newContentList;
    }

    private int getBucketCreationBatch() {
        if (allocationDefinition != null && allocationDefinition.getBucketCreationBatch() != null) {
            return allocationDefinition.getBucketCreationBatch();
        } else {
            return 1;
        }
    }

    @NotNull public BucketContentFactory getContentFactory() {
        return contentFactory;
    }

    public Integer estimateNumberOfBuckets() {
        return contentFactory.estimateNumberOfBuckets();
    }
}
