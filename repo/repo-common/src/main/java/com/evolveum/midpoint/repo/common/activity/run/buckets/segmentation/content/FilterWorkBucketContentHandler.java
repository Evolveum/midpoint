/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FilterWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Component
public class FilterWorkBucketContentHandler extends BaseWorkBucketContentHandler {

    @PostConstruct
    public void register() {
        registry.registerHandler(FilterWorkBucketContentType.class, this);
    }

    @NotNull
    @Override
    public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket,
            AbstractWorkSegmentationType configuration, Class<? extends Containerable> type,
            ItemDefinitionProvider itemDefinitionProvider) throws SchemaException {

        FilterWorkBucketContentType content = (FilterWorkBucketContentType) bucket.getContent();
        List<ObjectFilter> rv = new ArrayList<>();
        for (SearchFilterType filter : content.getFilter()) {
            rv.add(prismContext.getQueryConverter().createObjectFilter(type, filter));
        }
        return rv;
    }
}
