/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.BucketContentFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for bucket content factories.
 *
 * @param <WSC> Work segmentation configuration type
 */
public abstract class BaseBucketContentFactory<WSC extends AbstractWorkSegmentationType> implements BucketContentFactory {

    @NotNull protected final WSC segmentationConfig;
    @NotNull protected final PrismContext prismContext;

    protected BaseBucketContentFactory(@NotNull WSC segmentationConfig) {
        this.segmentationConfig = segmentationConfig;
        this.prismContext = PrismContext.get();
    }
}
