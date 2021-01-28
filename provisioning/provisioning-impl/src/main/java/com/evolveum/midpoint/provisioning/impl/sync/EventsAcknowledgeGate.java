/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.concurrent.Phaser;

/**
 * Keeps track of pending (unacknowledged) issued events. The parties are: synchronizer (1) plus each event issued.
 * So, we register each event as it is passed to the handler and deregister it when it is confirmed (with any status).
 * And, finally, the synchronizer arrives and waits for all the pending requests.
 */
class EventsAcknowledgeGate {

    private static final Trace LOGGER = TraceManager.getTrace(EventsAcknowledgeGate.class);

    private final Phaser issuedEventsGate = new Phaser(1);

    void registerIssuedEvent() {
        issuedEventsGate.register();
        LOGGER.trace("Registered an event");
    }

    void acknowledgeIssuedEvent() {
        issuedEventsGate.arriveAndDeregister();
        LOGGER.trace("Deregistered an event");
    }

    void waitForIssuedEventsAcknowledge() {
        LOGGER.trace("Waiting for events to be deregistered");
        issuedEventsGate.arriveAndAwaitAdvance();
        LOGGER.trace("Waiting done");
    }
}
