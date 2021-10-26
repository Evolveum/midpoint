/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
