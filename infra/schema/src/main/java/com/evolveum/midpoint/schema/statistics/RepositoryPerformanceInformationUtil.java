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

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryOperationPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryPerformanceInformationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
public class RepositoryPerformanceInformationUtil {

	public static void addTo(@NotNull RepositoryPerformanceInformationType aggregate, @Nullable RepositoryPerformanceInformationType part) {
		if (part == null) {
			return;
		}
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

	public static String format(RepositoryPerformanceInformationType i) {
		StringBuilder sb = new StringBuilder();
		List<RepositoryOperationPerformanceInformationType> operations = new ArrayList<>(i.getOperation());
		operations.sort(Comparator.comparing(RepositoryOperationPerformanceInformationType::getName));
		int max = operations.stream().mapToInt(op -> op.getName().length()).max().orElse(0);
		for (RepositoryOperationPerformanceInformationType op : operations) {
			long totalTime = defaultIfNull(op.getTotalTime(), 0L);
			long totalWastedTime = defaultIfNull(op.getTotalWastedTime(), 0L);
			int invocationCount = defaultIfNull(op.getInvocationCount(), 0);
			int executionCount = defaultIfNull(op.getExecutionCount(), 0);
			sb.append(String.format("  %-" + (max+2) + "s count:%7d, total time: %s", op.getName()+":", invocationCount,
					timeInfo(totalTime, op.getMinTime(), op.getMaxTime(), invocationCount)));
			if (totalTime > 0 && executionCount > invocationCount) {
				sb.append(String.format(Locale.US, ", wasted time for %4d retry/retries: %s (%s)", executionCount-invocationCount,
						timeInfo(totalWastedTime, op.getMinWastedTime(), op.getMaxWastedTime(), invocationCount), percent(totalWastedTime, totalTime)));
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private static String timeInfo(long total, Long min, Long max, int count) {
		return String.format(Locale.US, "%8d ms [min: %5d, max: %5d, avg: %7.1f]", total,
				defaultIfNull(min, 0L), defaultIfNull(max, 0L),
				count > 0 ? (float) total / count : 0);
	}

	private static String percent(long value, long base) {
		if (base != 0) {
			return String.format(Locale.US, "%6.2f%%", 100.0 * value / base);
		} else if (value == 0) {
			return String.format(Locale.US, "%6.2f%%", 0.0);
		} else {
			return "   NaN%";
		}
	}
}
