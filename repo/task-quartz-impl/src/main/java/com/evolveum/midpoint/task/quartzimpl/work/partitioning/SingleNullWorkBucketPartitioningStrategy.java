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

package com.evolveum.midpoint.task.quartzimpl.work.partitioning;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkBucketPartitioningStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Implements work state "partitioning" into single null work bucket.
 *
 * @author mederly
 */
public class SingleNullWorkBucketPartitioningStrategy extends BaseWorkBucketPartitioningStrategy {

	@SuppressWarnings("unused")
	public SingleNullWorkBucketPartitioningStrategy(TaskWorkManagementType configuration,
			PrismContext prismContext) {
		super(prismContext);
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

}
