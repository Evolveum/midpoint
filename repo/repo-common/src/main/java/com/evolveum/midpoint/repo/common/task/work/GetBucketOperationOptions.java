/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.task.work.segmentation.ImplicitSegmentationResolver;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public class GetBucketOperationOptions implements DebugDumpable {

    @NotNull private final ActivityDistributionDefinition distributionDefinition;
    @Nullable private final ImplicitSegmentationResolver implicitSegmentationResolver;
    @NotNull private final Supplier<Boolean> canRun;
    private final boolean isScavenger;
    private final long freeBucketWaitTime;
    private final boolean executeInitialWait;

    private GetBucketOperationOptions(@NotNull ActivityDistributionDefinition distributionDefinition,
            @Nullable ImplicitSegmentationResolver implicitSegmentationResolver,
            @NotNull Supplier<Boolean> canRun, boolean isScavenger,
            long freeBucketWaitTime, boolean executeInitialWait) {
        this.distributionDefinition = distributionDefinition;
        this.implicitSegmentationResolver = implicitSegmentationResolver;
        this.canRun = canRun;
        this.isScavenger = isScavenger;
        this.freeBucketWaitTime = freeBucketWaitTime;
        this.executeInitialWait = executeInitialWait;
    }

    public static GetBucketOperationOptions standard() {
        return GetBucketOperationOptionsBuilder.anOptions().build();
    }

    public ActivityDistributionDefinition getDistributionDefinition() {
        return distributionDefinition;
    }

    public ImplicitSegmentationResolver getImplicitSegmentationResolver() {
        return implicitSegmentationResolver;
    }

    public Supplier<Boolean> getCanRun() {
        return canRun;
    }

    public boolean isScavenger() {
        return isScavenger;
    }

    public boolean isExecuteInitialWait() {
        return executeInitialWait;
    }

    public long getFreeBucketWaitTime() {
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

        public GetBucketOperationOptions build() {
            return new GetBucketOperationOptions(distributionDefinition, implicitSegmentationResolver, canRun,
                    isScavenger, freeBucketWaitTime, executeInitialWait);
        }
    }
}
