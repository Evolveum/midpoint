/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import org.springframework.stereotype.Service;

import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

/**
 * @author semancik
 *
 * TODO: hostIdentifier
 */
@Service
public class LightweightIdentifierGeneratorImpl implements LightweightIdentifierGenerator {

    private static final long BACKWARD_TIME_ALLOWANCE = 10 * 1000L;

    private long lastTimestamp;     // monotonic increasing sequence
    private int lastSequence;       // incremented by 1, occasionally reset to 0
    private int hostIdentifier;     // currently unused

    public LightweightIdentifierGeneratorImpl() {
        lastTimestamp = 0;
        lastSequence = 0;
        hostIdentifier = 0;
    }

    @Override
    public synchronized LightweightIdentifier generate() {
        long timestamp = System.currentTimeMillis();
        if (timestamp > lastTimestamp) {
            // update the last timestamp and reset sequence counter
            lastTimestamp = timestamp;
            lastSequence = 0;
        } else if (timestamp < lastTimestamp - BACKWARD_TIME_ALLOWANCE) {
            throw new IllegalStateException("The time has moved back more than " + BACKWARD_TIME_ALLOWANCE
                    + " milliseconds, possible consistency violation. Current time = " + timestamp + ", last time = "
                    + lastTimestamp + ", difference is " + (lastTimestamp - timestamp) + ".");
        } else {
            // Usually timestamp == lastTimestamp here. But even if the time moved back a few seconds we stay calm
            // and simply keep lastTimestamp unchanged. We will probably get a few identifiers with increasing sequence
            // numbers and nothing wrong will happen.
        }
        return new LightweightIdentifier(timestamp, hostIdentifier, ++lastSequence);
    }
}
