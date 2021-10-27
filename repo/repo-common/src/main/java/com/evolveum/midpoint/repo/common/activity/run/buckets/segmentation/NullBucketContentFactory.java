/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
