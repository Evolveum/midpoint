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

package com.evolveum.midpoint.task.quartzimpl.work.segmentation.content;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
public abstract class IntervalWorkBucketContentHandler extends BaseWorkBucketContentHandler {

	@NotNull
	@Override
	public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket, AbstractWorkSegmentationType configuration,
			Class<? extends ObjectType> type, Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {

		AbstractWorkBucketContentType content = bucket.getContent();

		if (hasNoBoundaries(content)) {
			return new ArrayList<>();
		}
		if (configuration == null) {
			throw new IllegalStateException("No buckets configuration but having defined bucket content: " + content);
		}
		ItemPath discriminator = getDiscriminator(configuration, content);
		ItemDefinition<?> discriminatorDefinition = itemDefinitionProvider != null ? itemDefinitionProvider.apply(discriminator) : null;

		QName matchingRuleName = configuration.getMatchingRule() != null
				? QNameUtil.uriToQName(configuration.getMatchingRule(), PrismConstants.NS_MATCHING_RULE)
				: null;

		List<ObjectFilter> filters = new ArrayList<>();
		if (getFrom(content) != null) {
			filters.add(QueryBuilder.queryFor(type, prismContext)
					.item(discriminator, discriminatorDefinition).ge(getFrom(content)).matching(matchingRuleName)
					.buildFilter());
		}
		if (getTo(content) != null) {
			filters.add(QueryBuilder.queryFor(type, prismContext)
					.item(discriminator, discriminatorDefinition).lt(getTo(content)).matching(matchingRuleName)
					.buildFilter());
		}
		return filters;
	}

	@NotNull
	private ItemPath getDiscriminator(AbstractWorkSegmentationType configuration, AbstractWorkBucketContentType content) {
		ItemPathType discriminatorPathType = configuration.getDiscriminator();
		if (discriminatorPathType != null) {
			return discriminatorPathType.getItemPath();
		} else if (configuration instanceof OidWorkSegmentationType) {
			return new ItemPath(PrismConstants.T_ID);
		} else {
			throw new IllegalStateException("No buckets discriminator defined; bucket content = " + content);
		}
	}

	protected abstract boolean hasNoBoundaries(AbstractWorkBucketContentType bucketContent);

	protected abstract Object getFrom(AbstractWorkBucketContentType content);

	protected abstract Object getTo(AbstractWorkBucketContentType content);
}
