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

import com.evolveum.midpoint.util.statistics.SingleOperationPerformanceInformation;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleOperationPerformanceInformationType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
public class OperationsPerformanceInformationUtil {
	public static OperationsPerformanceInformationType toOperationsPerformanceInformationType(
			@NotNull OperationsPerformanceInformation methodsInfo) {
		OperationsPerformanceInformationType rv = new OperationsPerformanceInformationType();
		methodsInfo.getAllData().forEach((cache, info) -> rv.getOperation().add(toSingleMethodPerformanceInformationType(cache, info)));
		return rv;

	}

	private static SingleOperationPerformanceInformationType toSingleMethodPerformanceInformationType(String method,
			SingleOperationPerformanceInformation info) {
		SingleOperationPerformanceInformationType rv = new SingleOperationPerformanceInformationType();
		rv.setName(method);
		rv.setInvocationCount(info.getInvocationCount());
		rv.setTotalTime(info.getTotalTime());
		rv.setMinTime(info.getMinTime());
		rv.setMaxTime(info.getMaxTime());
		return rv;
	}

	public static void addTo(@NotNull OperationsPerformanceInformationType aggregate, @Nullable OperationsPerformanceInformationType part) {
		if (part == null) {
			return;
		}
		for (SingleOperationPerformanceInformationType partMethodInfo : part.getOperation()) {
			SingleOperationPerformanceInformationType matchingAggregateCacheInfo = null;
			for (SingleOperationPerformanceInformationType aggregateMethodInfo : aggregate.getOperation()) {
				if (Objects.equals(partMethodInfo.getName(), aggregateMethodInfo.getName())) {
					matchingAggregateCacheInfo = aggregateMethodInfo;
					break;
				}
			}
			if (matchingAggregateCacheInfo != null) {
				addTo(matchingAggregateCacheInfo, partMethodInfo);
			} else {
				aggregate.getOperation().add(partMethodInfo.clone());
			}
		}
	}

	private static void addTo(@NotNull SingleOperationPerformanceInformationType aggregate,
			@NotNull SingleOperationPerformanceInformationType part) {
		aggregate.setInvocationCount(aggregate.getInvocationCount() + part.getInvocationCount());
		aggregate.setTotalTime(aggregate.getTotalTime() + part.getTotalTime());
		aggregate.setMinTime(min(aggregate.getMinTime(), part.getMinTime()));
		aggregate.setMaxTime(max(aggregate.getMaxTime(), part.getMaxTime()));
	}

	private static Long min(Long a, Long b) {
		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
		} else return Math.min(a, b);
	}

	private static Long max(Long a, Long b) {
		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
		} else return Math.max(a, b);
	}

	public static String format(OperationsPerformanceInformationType i) {
		StringBuilder sb = new StringBuilder();
		List<SingleOperationPerformanceInformationType> methods = new ArrayList<>(i.getOperation());

		List<SingleOperationPerformanceInformationType> viaAspect = methods.stream()
				.filter(e -> e.getName().endsWith("#"))
				.collect(Collectors.toList());
		List<SingleOperationPerformanceInformationType> viaOpResult = methods.stream()
				.filter(e -> !e.getName().endsWith("#"))
				.collect(Collectors.toList());

		viaAspect.sort(Comparator.comparing(SingleOperationPerformanceInformationType::getName));
		viaOpResult.sort(Comparator.comparing(SingleOperationPerformanceInformationType::getName));
		int max = methods.stream().mapToInt(op -> op.getName().length()).max().orElse(0);

		if (!viaAspect.isEmpty()) {
			sb.append(" Data from OperationResult objects:\n");
		}
		format(sb, viaOpResult, max);
		if (!viaAspect.isEmpty()) {
			sb.append(" Data obtained using method interceptor:\n");
			format(sb, viaAspect, max);
		}
		return sb.toString();
	}

	private static void format(StringBuilder sb, List<SingleOperationPerformanceInformationType> viaOpResult, int max) {
		for (SingleOperationPerformanceInformationType op : viaOpResult) {
			long totalTime = defaultIfNull(op.getTotalTime(), 0L);
			int invocationCount = defaultIfNull(op.getInvocationCount(), 0);
			String name = StringUtils.stripEnd(op.getName(), "#");
			sb.append(String.format("  %-" + (max+2) + "s count:%7d, total time: %s", name +":", invocationCount,
					timeInfo(totalTime, op.getMinTime(), op.getMaxTime(), invocationCount)));
			sb.append("\n");
		}
	}

	private static String timeInfo(long total, Long min, Long max, int count) {
		return String.format(Locale.US, "%11.1f ms [min: %9.1f, max: %9.1f, avg: %9.1f]", total / 1000.0,
				defaultIfNull(min, 0L) / 1000.0, defaultIfNull(max, 0L) / 1000.0,
				count > 0 ? total/1000.0 / count : 0);
	}

}
