/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import java.util.List;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Handles various aspects of work bucket content e.g. creating filters for work space segmentation.
 */
public interface WorkBucketContentHandler {

    // TODO experimental
    @NotNull
    List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket, AbstractWorkSegmentationType configuration,
            Class<? extends Containerable> type, ItemDefinitionProvider itemDefinitionProvider)
            throws SchemaException;

}
