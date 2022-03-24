/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.task.api.TaskManager;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

/**
 * @author semancik
 */
@Service
public class LightweightIdentifierGeneratorImpl implements LightweightIdentifierGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(LightweightIdentifierGeneratorImpl.class);

    @Autowired private TaskManager taskManager;

    private static final long BACKWARD_TIME_ALLOWANCE = 10 * 1000L;

    private static final int UNINITIALIZED = -1;

    /** Ensures monotonic increasing sequence. */
    private long lastTimestamp;

    /**
     * If we need to generate multiple identifiers in given {@link #lastTimestamp}.
     * It is incremented by 1, occasionally reset to 0.
     */
    private int lastSequence;

    /**
     * Used to avoid collision on timestamp + sequence# in the cluster.
     *
     * Currently very preliminary implementation: uses last 2 bytes of node object OID.
     * Collision is improbable but not impossible.
     *
     * If {@link #UNINITIALIZED}, the value of 0 is used in lightweight identifiers being generated.
     */
    private int hostIdentifier = UNINITIALIZED;

    @Override
    public synchronized @NotNull LightweightIdentifier generate() {
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
        return new LightweightIdentifier(timestamp, getHostIdentifier(), ++lastSequence);
    }

    private int getHostIdentifier() {
        int hostId = hostIdentifier;
        if (hostId != UNINITIALIZED) {
            return hostId;
        } else {
            int newHostId = getHostIdentifierFromNodeOid();
            if (newHostId != UNINITIALIZED) {
                hostIdentifier = newHostId;
                return newHostId;
            } else {
                return 0; // Fallback. The warning was already logged.
            }
        }
    }

    private int getHostIdentifierFromNodeOid() {
        NodeType localNode = taskManager.getLocalNode();
        if (localNode == null) {
            LOGGER.warn("Couldn't determine host identifier. No local node.");
            return UNINITIALIZED;
        }

        String localNodeOid = localNode.getOid();
        if (localNodeOid == null) {
            LOGGER.warn("Couldn't determine host identifier. No local node OID.");
            return UNINITIALIZED;
        }

        try {
            String last4digits = localNodeOid.substring(localNodeOid.length() - 4);
            return Integer.parseInt(last4digits, 16);
        } catch (RuntimeException e) {
            LOGGER.warn("Couldn't determine host identifier from local node OID: {}. Malformed OID?", localNodeOid);
            return UNINITIALIZED;
        }
    }
}
