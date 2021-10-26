/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

public class NumericIntervalBucketUtil {

    public static @NotNull Interval getNarrowedInterval(@NotNull WorkBucketType bucket, @NotNull Interval defaultInterval) {
        AbstractWorkBucketContentType content = bucket.getContent();
        if (content instanceof NullWorkBucketContentType) {
            return defaultInterval;
        } else if (content instanceof NumericIntervalWorkBucketContentType) {
            NumericIntervalWorkBucketContentType numericContent = (NumericIntervalWorkBucketContentType) content;
            return Interval.of(
                    numericContent.getFrom().intValue(),
                    numericContent.getTo().intValue());
        } else {
            throw new IllegalStateException("Unexpected bucket content: " + content);
        }
    }

    public static AbstractWorkSegmentationType resolveImplicitSegmentation(@NotNull ImplicitWorkSegmentationType segmentation,
            @NotNull Interval wholeInterval) {
        argCheck(segmentation.getMatchingRule() == null, "Explicit matching rules are not supported");
        argCheck(segmentation.getDiscriminator() == null, "Explicit discriminator specification is not supported");
        argCheck(segmentation.getNumberOfBuckets() != null, "Number of buckets must be specified");

        return new NumericWorkSegmentationType(PrismContext.get())
                .from(BigInteger.valueOf(wholeInterval.from))
                .to(BigInteger.valueOf(wholeInterval.to))
                .numberOfBuckets(segmentation.getNumberOfBuckets());
    }

    /**
     * Represents an integer interval. Like in the case of numeric buckets, the upper boundary is EXCLUSIVE.
     */
    public static class Interval {

        /** Start of the interval (inclusive). */
        final public int from;

        /** End of the interval (exclusive). */
        final public int to;

        private Interval(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public static Interval of(int from, int to) {
            return new Interval(from, to);
        }

        public int getSize() {
            return to - from;
        }
    }
}
