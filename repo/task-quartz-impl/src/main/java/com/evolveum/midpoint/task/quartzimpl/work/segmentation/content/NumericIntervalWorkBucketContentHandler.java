/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation.content;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericIntervalWorkBucketContentType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigInteger;

/**
 * @author mederly
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
        return ((NumericIntervalWorkBucketContentType) content).getFrom();
    }

    @Override
    protected Object getTo(AbstractWorkBucketContentType content) {
        return ((NumericIntervalWorkBucketContentType) content).getTo();
    }
}
