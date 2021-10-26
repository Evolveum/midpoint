/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import java.math.BigInteger;

import com.evolveum.midpoint.repo.common.activity.run.buckets.BaseBucketContentFactory;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericIntervalWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericWorkSegmentationType;

/**
 * Implements work state management strategy based on numeric identifier intervals.
 */
public class NumericBucketContentFactory extends BaseBucketContentFactory<NumericWorkSegmentationType> {

    NumericBucketContentFactory(@NotNull NumericWorkSegmentationType segmentationConfig) {
        super(segmentationConfig);
    }

    @Override
    public NumericIntervalWorkBucketContentType createNextBucketContent(AbstractWorkBucketContentType lastBucketContent,
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
                return null; // no more buckets
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
        if (segmentationConfig.getBucketSize() != null) {
            return segmentationConfig.getBucketSize();
        } else if (segmentationConfig.getTo() != null && segmentationConfig.getNumberOfBuckets() != null) {
            BigInteger[] bi = segmentationConfig.getTo().subtract(getFrom())
                    .divideAndRemainder(BigInteger.valueOf(segmentationConfig.getNumberOfBuckets()));
            return bi[1].equals(BigInteger.ZERO) ? bi[0] : bi[0].add(BigInteger.ONE);
        } else {
            throw new IllegalStateException("Neither numberOfBuckets nor to + bucketSize is specified");
        }
    }

    @NotNull
    private BigInteger getFrom() {
        return segmentationConfig.getFrom() != null ? segmentationConfig.getFrom() : BigInteger.ZERO;
    }

    @NotNull
    private BigInteger getOrComputeTo() {
        if (segmentationConfig.getTo() != null) {
            return segmentationConfig.getTo();
        } else if (segmentationConfig.getBucketSize() != null && segmentationConfig.getNumberOfBuckets() != null) {
            return getFrom()
                    .add(segmentationConfig.getBucketSize()
                            .multiply(BigInteger.valueOf(segmentationConfig.getNumberOfBuckets())));
        } else {
            throw new IllegalStateException("Neither upper bound nor bucketSize + numberOfBucket specified");
        }
    }

    @NotNull
    private BigInteger computeIntervalSpan() {
        return getOrComputeTo().subtract(getFrom());
    }

    @Override
    public Integer estimateNumberOfBuckets() {
        if (segmentationConfig.getNumberOfBuckets() != null) {
            return segmentationConfig.getNumberOfBuckets();
        } else if (segmentationConfig.getTo() != null && segmentationConfig.getBucketSize() != null) {
            BigInteger[] divideAndRemainder = computeIntervalSpan().divideAndRemainder(segmentationConfig.getBucketSize());
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
