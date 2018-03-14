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
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractTaskWorkBucketsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FilterWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
@Component
public class FilterWorkBucketContentHandler extends BaseWorkBucketContentHandler {

	@PostConstruct
	public void register() {
		registry.registerHandler(FilterWorkBucketContentType.class, this);
	}

	@NotNull
	@Override
	public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket,
			AbstractTaskWorkBucketsConfigurationType configuration, Class<? extends ObjectType> type,
			Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) throws SchemaException {

		FilterWorkBucketContentType content = (FilterWorkBucketContentType) bucket.getContent();
		List<ObjectFilter> rv = new ArrayList<>();
		for (SearchFilterType filter : content.getFilter()) {
			rv.add(QueryJaxbConvertor.createObjectFilter(type, filter, prismContext));
		}
		return rv;
	}
}
