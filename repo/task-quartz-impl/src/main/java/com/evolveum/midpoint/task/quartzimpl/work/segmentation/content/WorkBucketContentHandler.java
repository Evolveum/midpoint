/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation.content;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Handles various aspects of work bucket content e.g. creating filters for work space segmentation.
 *
 * @author mederly
 */

public interface WorkBucketContentHandler {

    // TODO experimental
    @NotNull
    List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket, AbstractWorkSegmentationType configuration,
            Class<? extends ObjectType> type, Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider)
            throws SchemaException;

}
