/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NullWorkBucketContentType;

/**
 * Implements work state "segmentation" into single null work bucket.
 */
public class NullBucketContentFactory implements BucketContentFactory {

    @Override
    public AbstractWorkBucketContentType createNextBucketContent(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) throws SchemaException {
        if (lastBucketContent != null) {
            return null;
        } else {
            return new NullWorkBucketContentType();
        }
    }

    @Override
    public Integer estimateNumberOfBuckets() {
        return 1;
    }
}
