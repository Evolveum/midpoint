/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;

import org.jetbrains.annotations.Nullable;

/**
 * Creates content for new buckets.
 */
public interface BucketContentFactory {

    /**
     * Creates a content for the next bucket in a sequence.
     *
     * @return null if there is no next bucket
     */
    @Nullable AbstractWorkBucketContentType createNextBucketContent(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) throws SchemaException;

    /**
     * Estimates total number of buckets.
     *
     * @return null if the number cannot be determined
     */
    Integer estimateNumberOfBuckets();
}
