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

package com.evolveum.midpoint.task.quartzimpl.work.strategy;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkBucketPartitioningStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Implements work state management strategy based on numeric identifier intervals.
 *
 * @author mederly
 */
public class SingleWorkBucketPartitioningStrategy extends BaseWorkBucketPartitioningStrategy {

	private final TaskWorkStateConfigurationType configuration;

	public SingleWorkBucketPartitioningStrategy(TaskWorkStateConfigurationType configuration,
			PrismContext prismContext) {
		super(prismContext);
		this.configuration = configuration;
	}

	@NotNull
	@Override
	protected List<AbstractWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) {
		if (workState.getBucket().isEmpty()) {
			return singletonList(null);
		} else {
			return emptyList();
		}
	}

	// experimental implementation TODO
	@Override
	public List<ObjectFilter> createSpecificFilters(WorkBucketType bucket, Class<? extends ObjectType> type,
			Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {
		return new ArrayList<>();
	}
}
