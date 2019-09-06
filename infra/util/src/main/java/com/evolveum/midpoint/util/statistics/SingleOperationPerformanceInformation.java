/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.statistics;

import com.evolveum.midpoint.util.ShortDumpable;

import java.util.Locale;

/**
 *  Experimental.
 */
public class SingleOperationPerformanceInformation implements ShortDumpable {

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

	public synchronized void register(OperationInvocationRecord operation) {
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
