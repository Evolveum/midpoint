/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;

@FunctionalInterface
public interface BucketContentFactorySupplier<ST extends AbstractWorkSegmentationType, CF extends BucketContentFactory> {

    /**
     * Creates bucket content factory with a given segmentation config in given context.
     * The context is currently taken into account only for implicit segmentation configuration.
     */
    @NotNull CF supply(ST segmentationConfig, @Nullable ImplicitSegmentationResolver implicitSegmentationResolver);
}
