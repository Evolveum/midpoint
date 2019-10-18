/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.logging;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LoggingEventSink {

    private final List<LoggedEvent> events = new ArrayList<>();

    private final LoggingEventCollector collector;
    private final LoggingEventSink parent;

    LoggingEventSink(LoggingEventCollector collector, LoggingEventSink parent) {
        this.collector = collector;
        this.parent = parent;
    }

    public List<LoggedEvent> getEvents() {
        return events;
    }

    public void add(String eventText) {
        events.add(new LoggedEvent(eventText));
    }

    void collectEvents() {
        if (collector != null) {
            collector.collect(this);
        }
    }

    public LoggingEventSink getParent() {
        return parent;
    }

    public void clearEvents() {
        events.clear();
    }
}
