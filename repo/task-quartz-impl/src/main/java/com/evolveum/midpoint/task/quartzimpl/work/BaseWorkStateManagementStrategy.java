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
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategy.GetBucketResult.NothingFound;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base class for state management strategies.
 *
 * @author mederly
 */
public abstract class BaseWorkStateManagementStrategy<WBT extends AbstractWorkBucketType> implements WorkStateManagementStrategy {

	protected final PrismContext prismContext;

	protected BaseWorkStateManagementStrategy(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	/**
	 * Finds a ready (unallocated and not complete) bucket. If nothing can be found, creates one using strategy-specific means.
	 */
	@NotNull
	@Override
	public GetBucketResult getBucket(@NotNull TaskWorkStateType workState) throws SchemaException {
		boolean somethingDelegated = false;
		for (AbstractWorkBucketType bucket : workState.getBucket()) {
			if (bucket.getState() == WorkBucketStateType.READY) {
				return new GetBucketResult.FoundExisting(bucket);
			} else if (bucket.getState() == WorkBucketStateType.DELEGATED) {
				somethingDelegated = true;
			}
		}
		GetBucketResult.NewBuckets additionalBuckets = createAdditionalBuckets(workState);
		if (additionalBuckets != null) {
			return additionalBuckets;
		} else {
			return new NothingFound(!somethingDelegated);
		}
	}

	protected abstract GetBucketResult.NewBuckets createAdditionalBuckets(TaskWorkStateType workState) throws SchemaException;

	protected List<WBT> cast(List<AbstractWorkBucketType> buckets) {
		//noinspection unchecked
		return (List<WBT>) buckets;
	}
}
