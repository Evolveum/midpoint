/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation.content;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringIntervalWorkBucketContentType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
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
