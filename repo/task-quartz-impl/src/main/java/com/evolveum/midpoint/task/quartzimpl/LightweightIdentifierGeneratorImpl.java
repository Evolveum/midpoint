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

    long lastTimestamp;
    int lastSequence;
    int hostIdentifier;

    public LightweightIdentifierGeneratorImpl() {
        lastTimestamp = 0;
        lastSequence = 0;
        hostIdentifier = 0;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.task.api.LightweightIdentifierGenerator#generate()
     */
    @Override
    public synchronized LightweightIdentifier generate() {
        long timestamp = System.currentTimeMillis();
        if (timestamp == lastTimestamp) {
            // Nothing to do
        } else if (timestamp > lastTimestamp) {
            // reset the last timestamp and sequence conunter
            lastTimestamp = timestamp;
            lastSequence = 0;
        } else {
            throw new IllegalStateException("The time has moved back, possible consistency violation");
        }
        return new LightweightIdentifier(timestamp, hostIdentifier, ++lastSequence);
    }

}
