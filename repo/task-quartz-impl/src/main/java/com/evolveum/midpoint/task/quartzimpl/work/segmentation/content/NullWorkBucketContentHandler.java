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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * @author mederly
 */
@Component
public class NullWorkBucketContentHandler extends BaseWorkBucketContentHandler {

	@PostConstruct
	public void register() {
		registry.registerHandler(null, this);
	}

	@NotNull
	@Override
	public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket,
			AbstractWorkSegmentationType configuration, Class<? extends ObjectType> type,
			Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {
		return emptyList();
	}
}
