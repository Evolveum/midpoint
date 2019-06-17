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

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementOperationPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementPerformanceInformationType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 *
 */
public class TaskWorkBucketManagementPerformanceInformationUtil {

	public static String format(WorkBucketManagementPerformanceInformationType i) {
		StringBuilder sb = new StringBuilder();
		List<WorkBucketManagementOperationPerformanceInformationType> operations = new ArrayList<>(i.getOperation());
		operations.sort(Comparator.comparing(WorkBucketManagementOperationPerformanceInformationType::getName));
		int max = operations.stream().mapToInt(op -> op.getName().length()).max().orElse(0);
		for (WorkBucketManagementOperationPerformanceInformationType op : operations) {
			long totalTime = or0(op.getTotalTime());
			long totalWastedTime = or0(op.getTotalWastedTime());
			long totalWaitTime = or0(op.getTotalWaitTime());
			int count = or0(op.getCount());
			int conflictCount = or0(op.getConflictCount());
			int waitCount = or0(op.getBucketWaitCount());
			sb.append(String.format("  %-" + (max+2) + "s count:%7d, total time: %s", op.getName()+":", count,
					timeInfo(totalTime, op.getMinTime(), op.getMaxTime(), count)));
			if (conflictCount > 0 || waitCount > 0) {
				sb.append(String.format(Locale.US, ", wasted time for %4d conflict(s): %s (%s)", conflictCount,
						timeInfo(totalWastedTime, op.getMinWastedTime(), op.getMaxWastedTime(), count), percent(totalWastedTime, totalTime)));
				if (waitCount > 0) {
					sb.append(String.format(Locale.US, ", waited %4d time(s): %s (%s)", waitCount,
							timeInfo(totalWaitTime, op.getMinWaitTime(), op.getMaxWaitTime(), count), percent(totalWaitTime, totalTime)));
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private static String timeInfo(long total, Long min, Long max, int count) {
		return String.format(Locale.US, "%8d ms [min: %5d, max: %5d, avg: %7.1f]", total, or0(min), or0(max),
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

	private static int or0(Integer n) {
		return n != null ? n : 0;
	}

	private static long or0(Long n) {
		return n != null ? n : 0;
	}

}
