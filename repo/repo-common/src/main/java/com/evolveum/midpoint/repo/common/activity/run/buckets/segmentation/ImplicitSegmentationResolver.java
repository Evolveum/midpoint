/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImplicitWorkSegmentationType;

import org.jetbrains.annotations.NotNull;

/**
 * Resolves `ImplicitWorkSegmentationType` in a specific context.
 */
public interface ImplicitSegmentationResolver {

    /**
     * Converts `ImplicitWorkSegmentationType` into "real" segmentation configuration.
     */
    AbstractWorkSegmentationType resolveImplicitSegmentation(@NotNull ImplicitWorkSegmentationType segmentation);
}
