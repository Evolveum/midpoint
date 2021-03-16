/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkSegmentationStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 * Implements work state management strategy based on numeric identifier intervals.
 *
 * @author mederly
 */
//<NumericIntervalWorkBucketContentType, NumericIntervalWorkBucketsConfigurationType>
public class NumericWorkSegmentationStrategy extends BaseWorkSegmentationStrategy {

    @NotNull private final TaskWorkManagementType configuration;
    @NotNull private final NumericWorkSegmentationType bucketsConfiguration;

    public NumericWorkSegmentationStrategy(@NotNull TaskWorkManagementType configuration,
            PrismContext prismContext) {
        super(configuration, prismContext);
        this.configuration = configuration;
        this.bucketsConfiguration = (NumericWorkSegmentationType)
                TaskWorkStateUtil.getWorkSegmentationConfiguration(configuration);
    }

    @Override
    protected NumericIntervalWorkBucketContentType createAdditionalBucket(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) {
        BigInteger bucketSize = getOrComputeBucketSize();
        BigInteger from = getFrom();
        BigInteger to = getOrComputeTo();

        if (lastBucketSequentialNumber != null) {
            if (!(lastBucketContent instanceof NumericIntervalWorkBucketContentType)) {
                throw new IllegalStateException("Null or unsupported bucket content: " + lastBucketContent);
            }
            NumericIntervalWorkBucketContentType lastContent = (NumericIntervalWorkBucketContentType) lastBucketContent;
            if (lastContent.getTo() == null || lastContent.getTo().compareTo(to) >= 0) {
                return null;            // no more buckets
            }
            BigInteger newEnd = lastContent.getTo().add(bucketSize);
            if (newEnd.compareTo(to) > 0) {
                newEnd = to;
            }
            return new NumericIntervalWorkBucketContentType()
                    .from(lastContent.getTo())
                    .to(newEnd);
        } else {
            return new NumericIntervalWorkBucketContentType()
                    .from(from)
                    .to(from.add(bucketSize));
        }
    }

    @NotNull
    private BigInteger getOrComputeBucketSize() {
        if (bucketsConfiguration.getBucketSize() != null) {
            return bucketsConfiguration.getBucketSize();
        } else if (bucketsConfiguration.getTo() != null && bucketsConfiguration.getNumberOfBuckets() != null) {
            return bucketsConfiguration.getTo().subtract(getFrom()).divide(BigInteger.valueOf(bucketsConfiguration.getNumberOfBuckets()));
        } else {
            throw new IllegalStateException("Neither numberOfBuckets nor to + bucketSize is specified");
        }
    }

    @NotNull
    private BigInteger getFrom() {
        return bucketsConfiguration.getFrom() != null ? bucketsConfiguration.getFrom() : BigInteger.ZERO;
    }

    @NotNull
    private BigInteger getOrComputeTo() {
        if (bucketsConfiguration.getTo() != null) {
            return bucketsConfiguration.getTo();
        } else if (bucketsConfiguration.getBucketSize() != null && bucketsConfiguration.getNumberOfBuckets() != null) {
            return getFrom()
                    .add(bucketsConfiguration.getBucketSize()
                            .multiply(BigInteger.valueOf(bucketsConfiguration.getNumberOfBuckets())));
        } else {
            throw new IllegalStateException("Neither upper bound nor bucketSize + numberOfBucket specified");
        }
    }

    @NotNull
    private BigInteger computeIntervalSpan() {
        return getOrComputeTo().subtract(getFrom());
    }

    @Override
    public Integer estimateNumberOfBuckets(@Nullable TaskWorkStateType workState) {
        if (bucketsConfiguration.getNumberOfBuckets() != null) {
            return bucketsConfiguration.getNumberOfBuckets();
        } else if (bucketsConfiguration.getTo() != null && bucketsConfiguration.getBucketSize() != null) {
            BigInteger[] divideAndRemainder = computeIntervalSpan().divideAndRemainder(bucketsConfiguration.getBucketSize());
            if (BigInteger.ZERO.equals(divideAndRemainder[1])) {
                return divideAndRemainder[0].intValue();
            } else {
                return divideAndRemainder[0].intValue() + 1;
            }
        } else {
            throw new IllegalStateException("Neither numberOfBuckets nor to + bucketSize is specified");
        }
    }
}
