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
