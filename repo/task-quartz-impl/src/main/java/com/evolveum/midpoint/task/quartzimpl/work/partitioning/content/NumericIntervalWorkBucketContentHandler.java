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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractTaskWorkBucketsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericIntervalWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
@Component
public class NumericIntervalWorkBucketContentHandler extends BaseWorkBucketContentHandler {

	@PostConstruct
	public void register() {
		registry.registerHandler(NumericIntervalWorkBucketContentType.class, this);
	}

	@NotNull
	@Override
	public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket, AbstractTaskWorkBucketsConfigurationType configuration,
			Class<? extends ObjectType> type, Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {

		NumericIntervalWorkBucketContentType content = (NumericIntervalWorkBucketContentType) bucket.getContent();

		if (hasNoBoundaries(content)) {
			return new ArrayList<>();
		}
		if (configuration == null) {
			throw new IllegalStateException("No buckets configuration but having defined bucket content: " + content);
		}
		if (configuration.getDiscriminator() == null) {
			throw new IllegalStateException("No buckets discriminator defined; bucket content = " + content);
		}
		ItemPath identifier = configuration.getDiscriminator().getItemPath();
		ItemDefinition<?> identifierDefinition = itemDefinitionProvider != null ? itemDefinitionProvider.apply(identifier) : null;
		List<ObjectFilter> filters = new ArrayList<>();
		if (content.getFrom() != null) {
			if (identifierDefinition != null) {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier, identifierDefinition).ge(content.getFrom())
						.buildFilter());
			} else {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier).ge(content.getFrom())
						.buildFilter());
			}
		}
		if (content.getTo() != null) {
			if (identifierDefinition != null) {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier, identifierDefinition).lt(content.getTo())
						.buildFilter());
			} else {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier).lt(content.getTo())
						.buildFilter());
			}
		}
		return filters;
	}

	private boolean hasNoBoundaries(NumericIntervalWorkBucketContentType bucketContent) {
		return bucketContent == null || isNullOrZero(bucketContent.getFrom()) && bucketContent.getTo() == null;
	}

	private boolean isNullOrZero(BigInteger i) {
		return i == null || BigInteger.ZERO.equals(i);
	}

}
