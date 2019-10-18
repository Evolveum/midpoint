/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.backoff;

/**
 * @author mederly
 */
public class LinearBackoffComputer extends RetryLimitedBackoffComputer {

    private long delayInterval;

    public LinearBackoffComputer(int maxRetries, long delayInterval) {
        super(maxRetries);
        this.delayInterval = delayInterval;
    }

    @Override
    public long computeDelayWithinLimits(int retryNumber) {
        return Math.round(Math.random() * delayInterval);
    }
}
