/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public abstract class IntervalWorkBucketContentHandler extends BaseWorkBucketContentHandler {

    @NotNull
    @Override
    public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket, AbstractWorkSegmentationType configuration,
            Class<? extends Containerable> type, ItemDefinitionProvider itemDefinitionProvider) {

        AbstractWorkBucketContentType content = bucket.getContent();

        if (hasNoBoundaries(content)) {
            return new ArrayList<>();
        }
        if (configuration == null) {
            throw new IllegalStateException("No buckets configuration but having defined bucket content: " + content);
        }
        ItemPath discriminator = getDiscriminator(configuration, content);
        ItemDefinition<?> discriminatorDefinition = itemDefinitionProvider != null ? itemDefinitionProvider.getItemDefinition(discriminator) : null;

        QName matchingRuleName = configuration.getMatchingRule() != null
                ? QNameUtil.uriToQName(configuration.getMatchingRule(), PrismConstants.NS_MATCHING_RULE)
                : null;

        List<ObjectFilter> filters = new ArrayList<>();
        if (getFrom(content) != null) {
            filters.add(prismContext.queryFor(type)
                    .item(discriminator, discriminatorDefinition).ge(getFrom(content)).matching(matchingRuleName)
                    .buildFilter());
        }
        if (getTo(content) != null) {
            filters.add(prismContext.queryFor(type)
                    .item(discriminator, discriminatorDefinition).lt(getTo(content)).matching(matchingRuleName)
                    .buildFilter());
        }
        return filters;
    }

    protected abstract boolean hasNoBoundaries(AbstractWorkBucketContentType bucketContent);

    protected abstract Object getFrom(AbstractWorkBucketContentType content);

    protected abstract Object getTo(AbstractWorkBucketContentType content);
}
