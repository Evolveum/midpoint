/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericIntervalWorkBucketContentType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;

/**
 * Interprets numeric interval segmentation.
 *
 * Repository service currently does not support {@link BigInteger} values. Therefore we use a conversion to {@link Long};
 * hoping that it will be sufficient for current deployments.
 */
@Component
public class NumericIntervalWorkBucketContentHandler extends IntervalWorkBucketContentHandler {

    @PostConstruct
    public void register() {
        registry.registerHandler(NumericIntervalWorkBucketContentType.class, this);
    }

    @Override
    protected boolean hasNoBoundaries(AbstractWorkBucketContentType bucketContent) {
        NumericIntervalWorkBucketContentType cnt = (NumericIntervalWorkBucketContentType) bucketContent;
        return cnt == null || isNullOrZero(cnt.getFrom()) && cnt.getTo() == null;
    }

    private boolean isNullOrZero(BigInteger i) {
        return i == null || BigInteger.ZERO.equals(i);
    }

    @Override
    protected Object getFrom(AbstractWorkBucketContentType content) {
        return toLong(((NumericIntervalWorkBucketContentType) content).getFrom());
    }

    @Override
    protected Object getTo(AbstractWorkBucketContentType content) {
        return toLong(((NumericIntervalWorkBucketContentType) content).getTo());
    }

    private Long toLong(BigInteger value) {
        return value != null ? value.longValue() : null;
    }
}
