package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Keeps track of INDIVIDUAL pending (unacknowledged) events.
 *
 * We register each event as it is passed to the handler and deregister it when it is confirmed
 * (with any status). And, finally, we wait for all the pending events.
 *
 * The final wait is cancelled after specified time. All remaining events are negatively acknowledged then.
 *
 * This class can tolerate double acknowledgement of events (unlike {@link CountingEventsAcknowledgeGate}.
 */
class IndividualEventsAcknowledgeGate<E extends AcknowledgementSink> {

    private static final Trace LOGGER = TraceManager.getTrace(IndividualEventsAcknowledgeGate.class);

    /** Interval in which we check the pending requests set. */
    private static final int CHECK_INTERVAL = 1000;

    /** After this time we give up and nack all the pending events. */
    private static final int CHECK_TIMEOUT = 30000;

    /**
     * Events registered but not acknowledged. Null is a special value that denotes the synchronizer.
     * Guarded by: itself.
     */
    private final Set<E> pendingEvents = new HashSet<>();

    IndividualEventsAcknowledgeGate() {
        registerInternal(null);
    }

    void registerIssuedEvent(@NotNull E event) {
        registerInternal(event);
    }

    void acknowledgeIssuedEvent(@NotNull E event) {
        acknowledgeInternal(event);
    }

    private void registerInternal(E event) {
        synchronized (pendingEvents) {
            if (!pendingEvents.add(event)) {
                LOGGER.warn("Registering an event twice: {}", event);
            }
            LOGGER.trace("Registered an event: {}, pending items now: {}", event, pendingEvents.size());
        }
    }

    private void acknowledgeInternal(E event) {
        synchronized (pendingEvents) {
            if (!pendingEvents.remove(event)) {
                LOGGER.warn("Acknowledging an event twice: {}, pending items now: {}", event, pendingEvents.size());
            } else {
                LOGGER.trace("Acknowledged an event: {}, pending items now: {}", event, pendingEvents.size());
            }
            if (pendingEvents.isEmpty()) {
                pendingEvents.notify();
            }
        }
    }

    void waitForIssuedEventsAcknowledge(OperationResult result) {
        acknowledgeInternal(null);

        long started = System.currentTimeMillis();
        boolean interrupted = false;

        synchronized (pendingEvents) {
            while (!pendingEvents.isEmpty() && System.currentTimeMillis() - started < CHECK_TIMEOUT) {
                LOGGER.trace("Waiting for events to be acknowledged (remaining wait time: {} ms); pending events now: {}",
                        CHECK_TIMEOUT - (System.currentTimeMillis() - started), pendingEvents);
                try {
                    pendingEvents.wait(CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    LOGGER.info("Interrupted while waiting for issued events. Continuing waiting until specified time.");
                    interrupted = true;
                }
            }
        }

        Collection<E> events = getPendingEventsCopy();
        if (!events.isEmpty()) {
            LOGGER.info("Timed out waiting. Pending events: {}", events.size());
            nackEvents(events, result);
            LOGGER.trace("Exiting the gate (as timed out)");
        } else {
            LOGGER.trace("Exiting the gate (all events acknowledged)");
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void nackEvents(Collection<E> events, OperationResult result) {
        for (E event : events) {
            LOGGER.info("Acknowledging an event (negatively): {}", event);
            try {
                event.acknowledge(false, result);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't negatively acknowledge event {}", e, event);
            }
        }
    }

    private Collection<E> getPendingEventsCopy() {
        synchronized (pendingEvents) {
            return new ArrayList<>(pendingEvents);
        }
    }
}
