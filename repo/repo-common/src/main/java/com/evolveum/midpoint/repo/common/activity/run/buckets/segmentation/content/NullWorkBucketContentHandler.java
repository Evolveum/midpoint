/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import static java.util.Collections.emptyList;

import java.util.List;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NullWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

@Component
public class NullWorkBucketContentHandler extends BaseWorkBucketContentHandler {

    @PostConstruct
    public void register() {
        registry.registerHandler(null, this);
        registry.registerHandler(NullWorkBucketContentType.class, this);
    }

    @NotNull
    @Override
    public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket,
            AbstractWorkSegmentationType configuration, Class<? extends Containerable> type,
            ItemDefinitionProvider itemDefinitionProvider) {
        return emptyList();
    }
}
