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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

/**
 * @author mederly
 */
public class TaskWorkStateTypeUtil {

	public static WorkBucketType findBucketByNumber(List<WorkBucketType> buckets, int sequentialNumber) {
		return buckets.stream()
				.filter(b -> b.getSequentialNumber() == sequentialNumber)
				.findFirst().orElse(null);
	}

	// beware: do not call this on prism structure directly (it does not support setting values)
	public static void sortBucketsBySequentialNumber(List<WorkBucketType> buckets) {
		buckets.sort(Comparator.comparingInt(WorkBucketType::getSequentialNumber));
	}

	public static AbstractWorkSegmentationType getWorkSegmentationConfiguration(TaskWorkManagementType cfg) {
		if (cfg != null && cfg.getBuckets() != null) {
			WorkBucketsManagementType buckets = cfg.getBuckets();
			return MiscUtil.getFirstNonNull(
					buckets.getNumericSegmentation(),
					buckets.getStringSegmentation(),
					buckets.getOidSegmentation(),
					buckets.getExplicitSegmentation(),
					buckets.getSegmentation());
		} else {
			return null;
		}
	}

	public static int getCompleteBucketsNumber(TaskType taskType) {
		if (taskType.getWorkState() == null) {
			return 0;
		}
		Integer max = null;
		int notComplete = 0;
		for (WorkBucketType bucket : taskType.getWorkState().getBucket()) {
			if (max == null || bucket.getSequentialNumber() > max) {
				max = bucket.getSequentialNumber();
			}
			if (bucket.getState() != WorkBucketStateType.COMPLETE) {
				notComplete++;
			}
		}
		if (max == null) {
			return 0;
		} else {
			// what is not listed is assumed to be complete
			return max - notComplete;
		}
	}

	private static Integer getFirstBucketNumber(@NotNull TaskWorkStateType workState) {
		return workState.getBucket().stream()
				.map(WorkBucketType::getSequentialNumber)
				.min(Integer::compareTo).orElse(null);
	}

	@Nullable
	public static WorkBucketType getLastBucket(List<WorkBucketType> buckets) {
		WorkBucketType lastBucket = null;
		for (WorkBucketType bucket : buckets) {
			if (lastBucket == null || lastBucket.getSequentialNumber() < bucket.getSequentialNumber()) {
				lastBucket = bucket;
			}
		}
		return lastBucket;
	}

	public static boolean hasLimitations(WorkBucketType bucket) {
		if (bucket == null || bucket.getContent() == null) {
			return false;
		}
		if (bucket.getContent() instanceof NumericIntervalWorkBucketContentType) {
			NumericIntervalWorkBucketContentType numInterval = (NumericIntervalWorkBucketContentType) bucket.getContent();
			return numInterval.getTo() != null || numInterval.getFrom() != null && !BigInteger.ZERO.equals(numInterval.getFrom());
		} else if (bucket.getContent() instanceof StringIntervalWorkBucketContentType) {
			StringIntervalWorkBucketContentType stringInterval = (StringIntervalWorkBucketContentType) bucket.getContent();
			return stringInterval.getTo() != null || stringInterval.getFrom() != null;
		} else if (bucket.getContent() instanceof StringPrefixWorkBucketContentType) {
			StringPrefixWorkBucketContentType stringPrefix = (StringPrefixWorkBucketContentType) bucket.getContent();
			return !stringPrefix.getPrefix().isEmpty();
		} else if (bucket.getContent() instanceof FilterWorkBucketContentType) {
			FilterWorkBucketContentType filtered = (FilterWorkBucketContentType) bucket.getContent();
			return !filtered.getFilter().isEmpty();
		} else if (AbstractWorkBucketContentType.class.equals(bucket.getContent().getClass())) {
			return false;
		} else {
			throw new AssertionError("Unsupported bucket content: " + bucket.getContent());
		}
	}
}
