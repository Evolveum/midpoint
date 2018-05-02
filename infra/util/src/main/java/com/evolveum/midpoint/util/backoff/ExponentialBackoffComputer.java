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

package com.evolveum.midpoint.util.backoff;

/**
 * @author mederly
 */
public class ExponentialBackoffComputer extends RetryLimitedBackoffComputer {

	private long baseDelayInterval;
	private int exponentialThreshold;
	private Long delayIntervalLimit;

	public ExponentialBackoffComputer(int maxRetries, long baseDelayInterval, int exponentialThreshold, Long delayIntervalLimit) {
		super(maxRetries);
		this.baseDelayInterval = baseDelayInterval;
		this.exponentialThreshold = exponentialThreshold;
		this.delayIntervalLimit = delayIntervalLimit;
	}

	@Override
	public long computeDelayWithinLimits(int retryNumber) {
		//System.out.println("baseDelayInterval = " + baseDelayInterval + ", limits: " + exponentialThreshold + "/" + delayIntervalLimit + " (retry " + retryNumber + ")");
		double delayInterval = baseDelayInterval * Math.pow(2, Math.min(retryNumber, exponentialThreshold) - 1);
		if (delayIntervalLimit != null && delayInterval > delayIntervalLimit) {
			delayInterval = delayIntervalLimit;
		}
		return Math.round(Math.random() * delayInterval);
	}
}
