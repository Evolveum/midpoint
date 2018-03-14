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
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkBucketPartitioningStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.WorkBucketUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * TODO
 *
 * @author mederly
 */
public class EnumeratedWorkBucketPartitioningStrategy extends BaseWorkBucketPartitioningStrategy {

	@NotNull private final TaskWorkStateConfigurationType configuration;
	@NotNull private final EnumeratedWorkBucketsConfigurationType bucketsConfiguration;

	public EnumeratedWorkBucketPartitioningStrategy(@NotNull TaskWorkStateConfigurationType configuration,
			PrismContext prismContext) {
		super(prismContext);
		this.configuration = configuration;
		this.bucketsConfiguration = (EnumeratedWorkBucketsConfigurationType)
				WorkBucketUtil.getWorkBucketsConfiguration(configuration);
	}

	@NotNull
	@Override
	protected List<AbstractWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) {
		WorkBucketType lastBucket = TaskTypeUtil.getLastBucket(workState.getBucket());
		int nextSequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() + 1 : 1;
		if (nextSequentialNumber > bucketsConfiguration.getBucket().size()) {
			return emptyList();
		} else {
			return singletonList(bucketsConfiguration.getBucket().get(nextSequentialNumber-1));
		}
	}
}
