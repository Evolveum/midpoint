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

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericIntervalWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;

/**
 * @author mederly
 */
public class TaskTypeUtil {

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
		} else if (AbstractWorkBucketContentType.class.equals(bucket.getContent().getClass())) {
			return false;
		} else {
			throw new AssertionError("Unsupported bucket content: " + bucket.getContent());
		}
	}
}
