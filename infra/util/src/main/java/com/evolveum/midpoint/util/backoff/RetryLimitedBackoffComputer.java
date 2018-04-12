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
public abstract class RetryLimitedBackoffComputer implements BackoffComputer {

	private int maxRetries;

	RetryLimitedBackoffComputer(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	@Override
	public long computeDelay(int retryNumber) throws NoMoreRetriesException {
		if (retryNumber <= maxRetries) {
			return computeDelayWithinLimits(retryNumber);
		} else {
			throw new NoMoreRetriesException("Limit of " + maxRetries + " exceeded");
		}
	}

	protected abstract long computeDelayWithinLimits(int retryNumber);
}
