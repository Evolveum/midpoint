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
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult.NothingFound;
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
public abstract class BaseWorkSegmentationStrategy implements WorkSegmentationStrategy {

	private final TaskWorkManagementType configuration;
	protected final PrismContext prismContext;

	protected BaseWorkSegmentationStrategy(TaskWorkManagementType configuration, PrismContext prismContext) {
		this.configuration = configuration;
		this.prismContext = prismContext;
	}

	/**
	 * Finds a ready (unallocated and not complete) bucket. If nothing can be found, creates one using strategy-specific means.
	 */
	@NotNull
	@Override
	public GetBucketResult getBucket(@NotNull TaskWorkStateType workState) throws SchemaException {
		boolean somethingDelegated = false;
		List<WorkBucketType> ready = new ArrayList<>();
		for (WorkBucketType bucket : workState.getBucket()) {
			if (bucket.getState() == WorkBucketStateType.READY) {
				ready.add(bucket);
			} else if (bucket.getState() == WorkBucketStateType.DELEGATED) {
				somethingDelegated = true;
			}
		}
		if (!ready.isEmpty()) {
			return new GetBucketResult.FoundExisting(ready.get(selectReadyBucket(ready.size())));
		}
		List<? extends AbstractWorkBucketContentType> newBucketsContent = createAdditionalBuckets(workState);
		if (!newBucketsContent.isEmpty()) {
			List<WorkBucketType> newBuckets = new ArrayList<>(newBucketsContent.size());
			WorkBucketType lastBucket = TaskWorkStateTypeUtil.getLastBucket(workState.getBucket());
			int sequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() + 1 : 1;
			for (AbstractWorkBucketContentType newBucketContent : newBucketsContent) {
				newBuckets.add(new WorkBucketType(prismContext)
						.sequentialNumber(sequentialNumber++)
						.content(newBucketContent)
						.state(WorkBucketStateType.READY));
			}
			return new GetBucketResult.NewBuckets(newBuckets, selectReadyBucket(newBuckets.size()));
		} else {
			return new NothingFound(!somethingDelegated);
		}
	}

	private int selectReadyBucket(int size) {
		if (isAllocateFirst()) {
			return 0;
		} else {
			return (int) (Math.random() * size);
		}
	}

	@NotNull
	protected List<? extends AbstractWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) throws SchemaException {
		WorkBucketType lastBucket = TaskWorkStateTypeUtil.getLastBucket(workState.getBucket());
		AbstractWorkBucketContentType lastContent = lastBucket != null ? lastBucket.getContent() : null;
		Integer lastSequentialNumber = lastBucket != null ? lastBucket.getSequentialNumber() : null;
		int count = getBucketCreationBatch();
		List<AbstractWorkBucketContentType> rv = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			AbstractWorkBucketContentType newContent = createAdditionalBucket(lastContent, lastSequentialNumber);
			if (newContent == null) {
				break;
			}
			rv.add(newContent);
			lastContent = newContent;
			lastSequentialNumber = lastSequentialNumber != null ? lastSequentialNumber + 1 : 1;
		}
		return rv;
	}

	private WorkAllocationConfigurationType getAllocationConfiguration() {
		return configuration != null && configuration.getBuckets() != null ? configuration.getBuckets().getAllocation() : null;
	}

	private int getBucketCreationBatch() {
		WorkAllocationConfigurationType ac = getAllocationConfiguration();
		if (ac != null && ac.getBucketCreationBatch() != null) {
			return ac.getBucketCreationBatch();
		} else {
			return 1;
		}
	}

	private boolean isAllocateFirst() {
		WorkAllocationConfigurationType ac = getAllocationConfiguration();
		if (ac != null && ac.isAllocateFirst() != null) {
			return ac.isAllocateFirst();
		} else {
			return true;
		}
	}

	// the issue with this method is that we cannot distinguish between returning null content and returning no content (no more buckets)
	protected abstract AbstractWorkBucketContentType createAdditionalBucket(AbstractWorkBucketContentType lastBucketContent, Integer lastBucketSequentialNumber) throws SchemaException;
}
