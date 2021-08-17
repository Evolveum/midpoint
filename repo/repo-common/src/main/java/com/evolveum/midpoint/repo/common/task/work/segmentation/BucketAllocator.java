/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work.segmentation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.work.GetBucketOperation;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator.Response.FoundExisting;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tries to find or create bucket suitable for processing from a given list of buckets.
 * See {@link #getBucket(List)} method.
 *
 * Although strongly coupled with {@link GetBucketOperation} class, these are kept separate for two reasons:
 *
 * 1. to keep them smaller,
 * 2. to allow simpler testing.
 *
 * The responsibilities are not divided cleanly, though. So we should re-think this eventually.
 */
public class BucketAllocator {

    @NotNull private final BucketContentFactory contentFactory;
    private final WorkAllocationConfigurationType allocationConfig;
    private final PrismContext prismContext;

    private BucketAllocator(@NotNull BucketContentFactory contentFactory, WorkBucketsManagementType bucketingConfig) {
        this.allocationConfig = bucketingConfig != null ? bucketingConfig.getAllocation() : null;
        this.contentFactory = contentFactory;
        this.prismContext = PrismContext.get();
    }

    public static BucketAllocator create(@Nullable WorkBucketsManagementType bucketingConfig,
            @NotNull CommonTaskBeans beans, @Nullable ImplicitSegmentationResolver implicitSegmentationResolver) {
        return new BucketAllocator(
                beans.contentFactoryCreator.createContentFactory(bucketingConfig, implicitSegmentationResolver),
                bucketingConfig);
    }

    public static BucketAllocator create(@NotNull ActivityDistributionDefinition distributionDefinition,
            @NotNull CommonTaskBeans beans, @Nullable ImplicitSegmentationResolver implicitSegmentationResolver) {
        return create(distributionDefinition.getBuckets(), beans, implicitSegmentationResolver);
    }

    /**
     * Tries to find a bucket suitable for processing from a given list of buckets.
     * If nothing can be found, creates bucket or buckets using strategy-specific means.
     */
    @NotNull
    public BucketAllocator.Response getBucket(@NotNull List<WorkBucketType> buckets) throws SchemaException {
        boolean somethingDelegated = false;
        List<WorkBucketType> readyBuckets = new ArrayList<>();
        for (WorkBucketType bucket : buckets) {
            if (bucket.getState() == WorkBucketStateType.READY) {
                readyBuckets.add(bucket);
            } else if (bucket.getState() == WorkBucketStateType.DELEGATED) {
                somethingDelegated = true;
            }
        }
        if (!readyBuckets.isEmpty()) {
            return new FoundExisting(selectReadyBucket(readyBuckets));
        }

        List<? extends AbstractWorkBucketContentType> newBucketsContent = createNewBucketsContent(buckets);
        if (!newBucketsContent.isEmpty()) {
            List<WorkBucketType> newBuckets = createNewBuckets(buckets, newBucketsContent);
            return new Response.NewBuckets(newBuckets, getReadyBucketIndex(newBuckets.size()));
        } else {
            return new Response.NothingFound(!somethingDelegated);
        }
    }

    @NotNull
    private List<WorkBucketType> createNewBuckets(@NotNull List<WorkBucketType> buckets, List<? extends AbstractWorkBucketContentType> newBucketsContent) {
        List<WorkBucketType> newBuckets = new ArrayList<>(newBucketsContent.size());
        WorkBucketType lastBucket = BucketingUtil.getLastBucket(buckets);
        int sequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() + 1 : 1;
        for (AbstractWorkBucketContentType newBucketContent : newBucketsContent) {
            newBuckets.add(new WorkBucketType(prismContext)
                    .sequentialNumber(sequentialNumber++)
                    .content(newBucketContent)
                    .state(WorkBucketStateType.READY));
        }
        return newBuckets;
    }

    private WorkBucketType selectReadyBucket(List<WorkBucketType> bucket) {
        return bucket.get(getReadyBucketIndex(bucket.size()));
    }

    private int getReadyBucketIndex(int size) {
        if (isAllocateFirst()) {
            return 0;
        } else {
            return (int) (Math.random() * size);
        }
    }

    @NotNull
    private List<? extends AbstractWorkBucketContentType> createNewBucketsContent(@NotNull List<WorkBucketType> buckets)
            throws SchemaException {
        WorkBucketType lastBucket = BucketingUtil.getLastBucket(buckets);
        AbstractWorkBucketContentType lastContent = lastBucket != null ? lastBucket.getContent() : null;
        Integer lastSequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() : null;
        int count = getBucketCreationBatch();
        List<AbstractWorkBucketContentType> rv = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            AbstractWorkBucketContentType newContent = contentFactory
                    .createNextBucketContent(lastContent, lastSequentialNumber);
            if (newContent == null) {
                break;
            }
            rv.add(newContent);
            lastContent = newContent;
            lastSequentialNumber = lastSequentialNumber != null ? lastSequentialNumber + 1 : 1;
        }
        return rv;
    }

    private int getBucketCreationBatch() {
        if (allocationConfig != null && allocationConfig.getBucketCreationBatch() != null) {
            return allocationConfig.getBucketCreationBatch();
        } else {
            return 1;
        }
    }

    private boolean isAllocateFirst() {
        if (allocationConfig != null && allocationConfig.isAllocateFirst() != null) {
            return allocationConfig.isAllocateFirst();
        } else {
            return true;
        }
    }

    @NotNull public BucketContentFactory getContentFactory() {
        return contentFactory;
    }

    public static class Response {

        public static class NothingFound extends Response {
            public final boolean definite;

            NothingFound(boolean definite) {
                this.definite = definite;
            }

            @Override
            public String toString() {
                return "NothingFound{" +
                        "definite=" + definite +
                        '}';
            }
        }
        /**
         * The getBucket() method found existing bucket.
         */
        public static class FoundExisting extends Response {
            /**
             * Free bucket that is provided as a result of the operation.
             */
            @NotNull public final WorkBucketType bucket;

            FoundExisting(@NotNull WorkBucketType bucket) {
                this.bucket = bucket;
            }

            @Override
            public String toString() {
                return "FoundExisting{" +
                        "bucket=" + bucket +
                        '}';
            }
        }
        /**
         * The getBucket() method created one or more buckets.
         */
        public static class NewBuckets extends Response {
            /**
             * New buckets.
             */
            @NotNull public final List<WorkBucketType> newBuckets;
            public final int selected;

            NewBuckets(@NotNull List<WorkBucketType> newBuckets, int selected) {
                this.newBuckets = newBuckets;
                this.selected = selected;
            }

            @Override
            public String toString() {
                return "NewBuckets{" +
                        "newBuckets=" + newBuckets +
                        ", selected=" + selected +
                        '}';
            }
        }
    }
}
