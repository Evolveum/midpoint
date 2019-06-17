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

package com.evolveum.midpoint.util.aspect;

import com.evolveum.midpoint.util.ShortDumpable;

import java.util.Locale;

/**
 *  Experimental.
 */
public class MethodPerformanceInformation implements ShortDumpable {

	private int invocationCount;
	private long totalTime;
	private Long minTime;
	private Long maxTime;

	public int getInvocationCount() {
		return invocationCount;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public Long getMinTime() {
		return minTime;
	}

	public Long getMaxTime() {
		return maxTime;
	}

	public synchronized void register(MethodInvocationRecord operation) {
		invocationCount++;
		addTotalTime(operation.getElapsedTimeMicros());
	}

	private void addTotalTime(long time) {
		totalTime += time;
		if (minTime == null || time < minTime) {
			minTime = time;
		}
		if (maxTime == null || time > maxTime) {
			maxTime = time;
		}
	}

	@Override
	public synchronized void shortDump(StringBuilder sb) {
		sb.append(invocationCount);
		sb.append(", total time: ");
		sb.append(totalTime/1000).append(" ms");
		if (invocationCount > 0) {
			sb.append(String.format(Locale.US, " (min/max/avg: %.2f/%.2f/%.2f)", minTime/1000.0, maxTime/1000.0,
					(float) totalTime / invocationCount / 1000.0));
		}
	}

}
