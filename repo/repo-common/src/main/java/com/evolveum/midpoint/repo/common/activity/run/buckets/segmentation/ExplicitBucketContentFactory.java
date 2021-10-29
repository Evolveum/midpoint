/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import com.evolveum.midpoint.repo.common.activity.run.buckets.BaseBucketContentFactory;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExplicitWorkSegmentationType;

/**
 * Segmentation strategy based on explicit enumeration of buckets.
 */
public class ExplicitBucketContentFactory extends BaseBucketContentFactory<ExplicitWorkSegmentationType> {

    ExplicitBucketContentFactory(@NotNull ExplicitWorkSegmentationType segmentationConfig) {
        super(segmentationConfig);
    }

    @Override
    public AbstractWorkBucketContentType createNextBucketContent(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) {
        int currentBucketNumber = lastBucketSequentialNumber != null ? lastBucketSequentialNumber : 0;
        if (currentBucketNumber < segmentationConfig.getContent().size()) {
            return segmentationConfig.getContent().get(currentBucketNumber);
        } else {
            return null;
        }
    }

    @Override
    public Integer estimateNumberOfBuckets() {
        return segmentationConfig.getContent().size();
    }
}
