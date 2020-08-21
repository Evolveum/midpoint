/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.backoff;

public abstract class RetryLimitedBackoffComputer implements BackoffComputer {

    private final int maxRetries;

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
