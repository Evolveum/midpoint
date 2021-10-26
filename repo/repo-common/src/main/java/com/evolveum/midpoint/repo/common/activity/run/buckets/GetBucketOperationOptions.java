/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BucketProgressOverviewType;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.ImplicitSegmentationResolver;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public class GetBucketOperationOptions implements DebugDumpable {

    @Nullable private final ActivityDistributionDefinition distributionDefinition;
    @Nullable private final ImplicitSegmentationResolver implicitSegmentationResolver;
    @Nullable private final Supplier<Boolean> canRun;
    private final boolean isScavenger;
    private final long freeBucketWaitTime;
    private final boolean executeInitialWait;
    @Nullable private final Consumer<BucketProgressOverviewType> bucketProgressConsumer;

    private GetBucketOperationOptions(@Nullable ActivityDistributionDefinition distributionDefinition,
            @Nullable ImplicitSegmentationResolver implicitSegmentationResolver,
            @Nullable Supplier<Boolean> canRun, boolean isScavenger,
            long freeBucketWaitTime, boolean executeInitialWait,
            @Nullable Consumer<BucketProgressOverviewType> bucketProgressConsumer) {
        this.distributionDefinition = distributionDefinition;
        this.implicitSegmentationResolver = implicitSegmentationResolver;
        this.canRun = canRun;
        this.isScavenger = isScavenger;
        this.freeBucketWaitTime = freeBucketWaitTime;
        this.executeInitialWait = executeInitialWait;
        this.bucketProgressConsumer = bucketProgressConsumer;
    }

    public static GetBucketOperationOptions standard() {
        return GetBucketOperationOptionsBuilder.anOptions().build();
    }

    static @Nullable Consumer<BucketProgressOverviewType> getProgressConsumer(@Nullable GetBucketOperationOptions options) {
        return options != null ? options.bucketProgressConsumer : null;
    }

    public @Nullable ActivityDistributionDefinition getDistributionDefinition() {
        return distributionDefinition;
    }

    public @Nullable ImplicitSegmentationResolver getImplicitSegmentationResolver() {
        return implicitSegmentationResolver;
    }

    public @Nullable Supplier<Boolean> getCanRun() {
        return canRun;
    }

    public boolean isScavenger() {
        return isScavenger;
    }

    public boolean isExecuteInitialWait() {
        return executeInitialWait;
    }

    long getFreeBucketWaitTime() {
        return freeBucketWaitTime;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "distributionDefinition", distributionDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "isScavenger", isScavenger, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "freeBucketWaitTime", freeBucketWaitTime, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "executeInitialWait", executeInitialWait, indent + 1);
        return sb.toString();
    }

    public static final class GetBucketOperationOptionsBuilder {
        private ActivityDistributionDefinition distributionDefinition;
        private ImplicitSegmentationResolver implicitSegmentationResolver;
        private Supplier<Boolean> canRun = () -> true;
        private boolean isScavenger;
        private long freeBucketWaitTime;
        private boolean executeInitialWait;
        private Consumer<BucketProgressOverviewType> bucketProgressConsumer;

        private GetBucketOperationOptionsBuilder() {
        }

        public static GetBucketOperationOptionsBuilder anOptions() {
            return new GetBucketOperationOptionsBuilder();
        }

        public GetBucketOperationOptionsBuilder withDistributionDefinition(ActivityDistributionDefinition distributionDefinition) {
            this.distributionDefinition = distributionDefinition;
            return this;
        }

        public GetBucketOperationOptionsBuilder withImplicitSegmentationResolver(ImplicitSegmentationResolver implicitSegmentationResolver) {
            this.implicitSegmentationResolver = implicitSegmentationResolver;
            return this;
        }

        public GetBucketOperationOptionsBuilder withCanRun(Supplier<Boolean> canRun) {
            this.canRun = canRun;
            return this;
        }

        public GetBucketOperationOptionsBuilder withIsScavenger(boolean isScavenger) {
            this.isScavenger = isScavenger;
            return this;
        }

        public GetBucketOperationOptionsBuilder withFreeBucketWaitTime(long freeBucketWaitTime) {
            this.freeBucketWaitTime = freeBucketWaitTime;
            return this;
        }

        public GetBucketOperationOptionsBuilder withExecuteInitialWait(boolean executeInitialWait) {
            this.executeInitialWait = executeInitialWait;
            return this;
        }

        public GetBucketOperationOptionsBuilder withBucketProgressConsumer(Consumer<BucketProgressOverviewType> value) {
            this.bucketProgressConsumer = value;
            return this;
        }

        public GetBucketOperationOptions build() {
            return new GetBucketOperationOptions(distributionDefinition, implicitSegmentationResolver, canRun,
                    isScavenger, freeBucketWaitTime, executeInitialWait, bucketProgressConsumer);
        }
    }
}
