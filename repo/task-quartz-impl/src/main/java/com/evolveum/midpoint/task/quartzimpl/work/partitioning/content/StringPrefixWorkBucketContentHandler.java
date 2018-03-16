/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.work.partitioning.content;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
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
 * @author mederly
 */
@Component
public class StringPrefixWorkBucketContentHandler extends BaseWorkBucketContentHandler {

	@PostConstruct
	public void register() {
		registry.registerHandler(StringPrefixWorkBucketContentType.class, this);
	}

	@NotNull
	@Override
	public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket, AbstractWorkSegmentationType configuration,
			Class<? extends ObjectType> type, Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {

		StringPrefixWorkBucketContentType content = (StringPrefixWorkBucketContentType) bucket.getContent();

		if (content == null || content.getPrefix().isEmpty()) {
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
		for (String prefix : content.getPrefix()) {
			prefixFilters.add(QueryBuilder.queryFor(type, prismContext)
					.item(discriminator, discriminatorDefinition).startsWith(prefix).matching(matchingRuleName)
					.buildFilter());
		}
		assert !prefixFilters.isEmpty();
		if (prefixFilters.size() > 1) {
			return Collections.singletonList(OrFilter.createOr(prefixFilters));
		} else {
			return prefixFilters;
		}
	}
}
