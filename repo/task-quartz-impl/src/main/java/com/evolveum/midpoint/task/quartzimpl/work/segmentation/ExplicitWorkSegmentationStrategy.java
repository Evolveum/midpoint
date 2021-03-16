/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkSegmentationStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO
 *
 * @author mederly
 */
public class ExplicitWorkSegmentationStrategy extends BaseWorkSegmentationStrategy {

    @NotNull private final ExplicitWorkSegmentationType bucketsConfiguration;

    public ExplicitWorkSegmentationStrategy(@NotNull TaskWorkManagementType configuration, PrismContext prismContext) {
        super(configuration, prismContext);
        this.bucketsConfiguration = (ExplicitWorkSegmentationType)
                TaskWorkStateUtil.getWorkSegmentationConfiguration(configuration);
    }

    @Override
    protected AbstractWorkBucketContentType createAdditionalBucket(AbstractWorkBucketContentType lastBucketContent, Integer lastBucketSequentialNumber) {
        int currentBucketNumber = lastBucketSequentialNumber != null ? lastBucketSequentialNumber : 0;
        if (currentBucketNumber < bucketsConfiguration.getContent().size()) {
            return bucketsConfiguration.getContent().get(currentBucketNumber);
        } else {
            return null;
        }
    }

    @Override
    public Integer estimateNumberOfBuckets(@Nullable TaskWorkStateType workState) {
        return bucketsConfiguration.getContent().size();
    }
}
