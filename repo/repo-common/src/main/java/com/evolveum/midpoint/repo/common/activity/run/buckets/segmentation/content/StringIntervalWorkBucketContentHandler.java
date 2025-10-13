/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringIntervalWorkBucketContentType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class StringIntervalWorkBucketContentHandler extends IntervalWorkBucketContentHandler {

    @PostConstruct
    public void register() {
        registry.registerHandler(StringIntervalWorkBucketContentType.class, this);
    }

    @Override
    protected boolean hasNoBoundaries(AbstractWorkBucketContentType bucketContent) {
        StringIntervalWorkBucketContentType cnt = (StringIntervalWorkBucketContentType) bucketContent;
        return cnt == null || cnt.getFrom() == null && cnt.getTo() == null;
    }

    @Override
    protected Object getFrom(AbstractWorkBucketContentType content) {
        return ((StringIntervalWorkBucketContentType) content).getFrom();
    }

    @Override
    protected Object getTo(AbstractWorkBucketContentType content) {
        return ((StringIntervalWorkBucketContentType) content).getTo();
    }
}
