/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation.content;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 *
 */
@Component
public class StringValueWorkBucketContentHandler extends BaseWorkBucketContentHandler {

    @PostConstruct
    public void register() {
        registry.registerHandler(StringValueWorkBucketContentType.class, this);
    }

    @SuppressWarnings("Duplicates")
    @NotNull
    @Override
    public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket, AbstractWorkSegmentationType configuration,
            Class<? extends ObjectType> type, Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {

        StringValueWorkBucketContentType content = (StringValueWorkBucketContentType) bucket.getContent();

        if (content == null || content.getValue().isEmpty()) {
            return emptyList();
        }
        if (configuration == null) {
            throw new IllegalStateException("No buckets configuration but having defined bucket content: " + content);
        }
        if (configuration.getDiscriminator() == null) {
            throw new IllegalStateException("No buckets discriminator defined; bucket content = " + content);
        }
        ItemPath discriminator = configuration.getDiscriminator().getItemPath();
        ItemDefinition<?> discriminatorDefinition = itemDefinitionProvider != null ? itemDefinitionProvider.apply(discriminator) : null;

        QName matchingRuleName = configuration.getMatchingRule() != null
                ? QNameUtil.uriToQName(configuration.getMatchingRule(), PrismConstants.NS_MATCHING_RULE)
                : null;

        List<ObjectFilter> prefixFilters = new ArrayList<>();
        for (String prefix : content.getValue()) {
            prefixFilters.add(prismContext.queryFor(type)
                    .item(discriminator, discriminatorDefinition).eq(prefix).matching(matchingRuleName)
                    .buildFilter());
        }
        assert !prefixFilters.isEmpty();
        if (prefixFilters.size() > 1) {
            return Collections.singletonList(prismContext.queryFactory().createOr(prefixFilters));
        } else {
            return prefixFilters;
        }
    }
}
