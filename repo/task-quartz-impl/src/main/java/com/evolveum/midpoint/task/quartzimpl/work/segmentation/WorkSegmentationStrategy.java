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

package com.evolveum.midpoint.task.quartzimpl.work.segmentation;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Strategy related to work buckets segmentation. Deals with creation of buckets and with translation of buckets into object queries.
 *
 * @author mederly
 */
public interface WorkSegmentationStrategy {

	/**
	 * Finds or creates a free (unallocated and not complete) bucket.
	 */
	@NotNull
	GetBucketResult getBucket(@NotNull TaskWorkStateType workState) throws SchemaException;

	/**
	 * Estimates total number of buckets. Might utilize current work state, if available.
	 * @return null if the number cannot be determined
	 */
	default Integer estimateNumberOfBuckets(@Nullable TaskWorkStateType workState) {
		return null;
	}

	class GetBucketResult {
		public static class NothingFound extends GetBucketResult {
			public final boolean definite;

			public NothingFound(boolean definite) {
				this.definite = definite;
			}
		}
		/**
		 * The getBucket() method found existing bucket.
		 */
		public static class FoundExisting extends GetBucketResult {
			/**
			 * Free bucket that is provided as a result of the operation; or null if no bucket could be obtained.
			 */
			@NotNull public final WorkBucketType bucket;

			public FoundExisting(@NotNull WorkBucketType bucket) {
				this.bucket = bucket;
			}
		}
		/**
		 * The getBucket() method created one or more buckets.
		 */
		public static class NewBuckets extends GetBucketResult {
			/**
			 * New buckets. The first one is to be returned as the one to be processed.
			 */
			@NotNull public final List<WorkBucketType> newBuckets;

			public NewBuckets(@NotNull List<WorkBucketType> newBuckets) {
				this.newBuckets = newBuckets;
			}
		}
	}
}
