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

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractTaskWorkBucketsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import java.util.Comparator;
import java.util.List;

/**
 * @author mederly
 */
public class WorkBucketUtil {

	public static WorkBucketType findBucketByNumber(List<WorkBucketType> buckets, int sequentialNumber) {
		return buckets.stream()
				.filter(b -> b.getSequentialNumber() == sequentialNumber)
				.findFirst().orElse(null);
	}

	// beware: do not call this on prism structure directly (it does not support setting values)
	public static void sortBucketsBySequentialNumber(List<WorkBucketType> buckets) {
		buckets.sort(Comparator.comparingInt(WorkBucketType::getSequentialNumber));
	}

	public static Task findWorkerByBucketNumber(List<Task> workers, int sequentialNumber) {
		for (Task worker : workers) {
			if (worker.getWorkState() != null && findBucketByNumber(worker.getWorkState().getBucket(), sequentialNumber) != null) {
				return worker;
			}
		}
		return null;
	}

	public static AbstractTaskWorkBucketsConfigurationType getWorkBucketsConfiguration(TaskWorkStateConfigurationType cfg) {
		if (cfg == null) {
			return null;
		} else if (cfg.getNumericIntervalBuckets() != null) {
			return cfg.getNumericIntervalBuckets();
		} else if (cfg.getStringBuckets() != null) {
			return cfg.getStringBuckets();
		} else if (cfg.getEnumeratedBuckets() != null) {
			return cfg.getEnumeratedBuckets();
		} else {
			return cfg.getBuckets();
		}
	}
}
