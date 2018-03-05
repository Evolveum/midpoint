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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketType;

import java.util.Comparator;
import java.util.List;

/**
 * @author mederly
 */
public class WorkBucketUtil {

	public static AbstractWorkBucketType findBucketByNumber(List<AbstractWorkBucketType> buckets, int sequentialNumber) {
		return buckets.stream()
				.filter(b -> b.getSequentialNumber() == sequentialNumber)
				.findFirst().orElse(null);
	}

	// beware: do not call this on prism structure directly (it does not support setting values)
	public static void sortBucketsBySequentialNumber(List<AbstractWorkBucketType> buckets) {
		buckets.sort(Comparator.comparingInt(AbstractWorkBucketType::getSequentialNumber));
	}

	public static Task findWorkerByBucketNumber(List<Task> workers, int sequentialNumber) {
		for (Task worker : workers) {
			if (worker.getWorkState() != null && findBucketByNumber(worker.getWorkState().getBucket(), sequentialNumber) != null) {
				return worker;
			}
		}
		return null;
	}
}
