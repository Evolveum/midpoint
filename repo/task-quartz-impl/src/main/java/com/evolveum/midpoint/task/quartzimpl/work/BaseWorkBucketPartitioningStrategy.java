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

package com.evolveum.midpoint.task.quartzimpl.work;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkBucketPartitioningStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkBucketPartitioningStrategy.GetBucketResult.NothingFound;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for state management strategies.
 *
 * @author mederly
 */
// <CNT extends AbstractWorkBucketContentType, CFG extends AbstractTaskWorkBucketsConfigurationType>
public abstract class BaseWorkBucketPartitioningStrategy implements WorkBucketPartitioningStrategy {

	protected final PrismContext prismContext;

	protected BaseWorkBucketPartitioningStrategy(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	/**
	 * Finds a ready (unallocated and not complete) bucket. If nothing can be found, creates one using strategy-specific means.
	 */
	@NotNull
	@Override
	public GetBucketResult getBucket(@NotNull TaskWorkStateType workState) throws SchemaException {
		boolean somethingDelegated = false;
		for (WorkBucketType bucket : workState.getBucket()) {
			if (bucket.getState() == WorkBucketStateType.READY) {
				return new GetBucketResult.FoundExisting(bucket);
			} else if (bucket.getState() == WorkBucketStateType.DELEGATED) {
				somethingDelegated = true;
			}
		}
		List<? extends AbstractWorkBucketContentType> newBucketsContent = createAdditionalBuckets(workState);
		if (!newBucketsContent.isEmpty()) {
			List<WorkBucketType> newBuckets = new ArrayList<>(newBucketsContent.size());
			WorkBucketType lastBucket = TaskTypeUtil.getLastBucket(workState.getBucket());
			int sequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() + 1 : 1;
			for (AbstractWorkBucketContentType newBucketContent : newBucketsContent) {
				newBuckets.add(new WorkBucketType(prismContext)
						.sequentialNumber(sequentialNumber++)
						.content(newBucketContent)
						.state(WorkBucketStateType.READY));
			}
			return new GetBucketResult.NewBuckets(newBuckets);
		} else {
			return new NothingFound(!somethingDelegated);
		}
	}

	@NotNull
	protected abstract List<? extends AbstractWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) throws SchemaException;
}
