/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.api.perf;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryOperationPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryPerformanceInformationType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 *
 */
public class PerformanceInformationUtil {

	public static void addTo(@NotNull RepositoryPerformanceInformationType aggregate, @NotNull RepositoryPerformanceInformationType part) {
		for (RepositoryOperationPerformanceInformationType partOperation : part.getOperation()) {
			RepositoryOperationPerformanceInformationType matchingAggregateOperation = null;
			for (RepositoryOperationPerformanceInformationType aggregateOperation : aggregate.getOperation()) {
				if (Objects.equals(partOperation.getName(), aggregateOperation.getName())) {
					matchingAggregateOperation = aggregateOperation;
					break;
				}
			}
			if (matchingAggregateOperation != null) {
				addTo(matchingAggregateOperation, partOperation);
			} else {
				aggregate.getOperation().add(partOperation.clone());
			}
		}
	}

	private static void addTo(@NotNull RepositoryOperationPerformanceInformationType aggregate,
			@NotNull RepositoryOperationPerformanceInformationType part) {
		aggregate.setInvocationCount(aggregate.getInvocationCount() + part.getInvocationCount());
		aggregate.setExecutionCount(aggregate.getExecutionCount() + part.getExecutionCount());
		aggregate.setTotalTime(aggregate.getTotalTime() + part.getTotalTime());
		aggregate.setMinTime(min(aggregate.getMinTime(), part.getMinTime()));
		aggregate.setMaxTime(max(aggregate.getMaxTime(), part.getMaxTime()));
		aggregate.setTotalWastedTime(aggregate.getTotalWastedTime() + part.getTotalWastedTime());
		aggregate.setMinWastedTime(min(aggregate.getMinWastedTime(), part.getMinWastedTime()));
		aggregate.setMaxWastedTime(max(aggregate.getMaxWastedTime(), part.getMaxWastedTime()));
	}

	private static Long min(Long a, Long b) {
		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
		} else {
			return Math.min(a, b);
		}
	}

	private static Long max(Long a, Long b) {
		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
		} else {
			return Math.max(a, b);
		}
	}
}
