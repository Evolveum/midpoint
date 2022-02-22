/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.events;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.cases.api.events.FutureNotificationEvent;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Maintains prepared notifications for given {@link CaseEngineOperationImpl}.
 *
 * Technically speaking, we do not maintain the events themselves, but only auxiliary objects that will
 * produce the events when needed. This preparation and sending to the notification service occurs
 * after successful commit, via the {@link #flush(OperationResult)} method.
 */
public class PendingNotificationEvents implements DebugDumpable {

//    private static final Trace LOGGER = TraceManager.getTrace(PendingNotificationEvents.class);

    private static final String OP_FLUSH = PendingNotificationEvents.class.getName() + ".sendPreparedNotifications";

    @NotNull private final CaseEngineOperationImpl operation;

    /** Sources for notification events to be fired after commit. */
    @NotNull private final List<FutureNotificationEvent> futureEvents = new ArrayList<>();

    public PendingNotificationEvents(@NotNull CaseEngineOperationImpl operation) {
        this.operation = operation;
    }

    @Override
    public String debugDump(int indent) {
        // Temporary implementation
        return DebugUtil.debugDump(futureEvents, indent);
    }

    public int size() {
        return futureEvents.size();
    }

    public void add(@NotNull FutureNotificationEvent supplier) {
        futureEvents.add(supplier);
    }

    public void flush(OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_FLUSH)
                .setMinor()
                .build();
        try {
            for (FutureNotificationEvent futureEvent : futureEvents) {
                operation.getBeans().notificationHelper
                        .send(futureEvent, operation.getTask(), result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }
}
